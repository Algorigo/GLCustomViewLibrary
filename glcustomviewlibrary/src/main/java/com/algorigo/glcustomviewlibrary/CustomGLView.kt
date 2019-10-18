package com.algorigo.glcustomviewlibrary

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseArray
import com.algorigo.glcustomviewlibrary.ShaderHelper.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CustomGLView : GLSurfaceView {

    internal interface GLObject {
        fun setData(data: FloatArray)
        fun draw(positionAttribute: Int, normalAttribute: Int, colorAttribute: Int)
    }

    data class Vec3D(val x: Float, val y: Float, val z: Float)

    enum class Rotation {
        NORMAL,
        CW_90,
        CW_180,
        CW_270,
    }

    sealed class ColorMap {
        class RainbowColorMapRect(
            val centerPosition: Vec3D = Vec3D(0f, 0f, 0f),
            val vec1: Vec3D = Vec3D(22f, 0f, 0f),
            val vec2: Vec3D = Vec3D(0f, 22f, 0f),
            var rotation: Rotation = Rotation.NORMAL,
            val flip: Boolean = false,
            val sizePerWidth: Int = 57,
            val sizePerHeight: Int = 57
        ) : ColorMap()

        class RainbowColorMapCustom(
            val vertexData: List<PointF>,
            val heightMapIndexData: IntArray,
            val centerPosition: Vec3D = Vec3D(0f, 0f, 0f),
            val vec1: Vec3D = Vec3D(22f, 0f, 0f),
            val vec2: Vec3D = Vec3D(0f, 22f, 0f)
        ) : ColorMap()
    }

    private var rendererSurfaceCreated = false
    private val colorMaps = SparseArray<ColorMap>()
    private val colorMapObjects = SparseArray<GLObject>()
    private val dataMap = SparseArray<FloatArray>()

    inner class GLPressureRenderer : Renderer {

        private var program: Int = 0

        /** OpenGL handles to our program uniforms.  */
        private var mvpMatrixUniform: Int = 0
        private var mvMatrixUniform: Int = 0

        /** OpenGL handles to our program attributes.  */
        private var positionAttribute: Int = 0
        private var normalAttribute: Int = 0
        private var colorAttribute: Int = 0

        /**
         * Store the model matrix. This matrix is used to move models from object
         * space (where each model can be thought of being located at the center of
         * the universe) to world space.
         */
        private val modelMatrix = FloatArray(16)

        /**
         * Store the view matrix. This can be thought of as our camera. This matrix
         * transforms world space to eye space; it positions things relative to our
         * eye.
         */
        private val viewMatrix = FloatArray(16)

        /**
         * Store the projection matrix. This is used to project the scene onto a 2D
         * viewport.
         */
        private val projectionMatrix = FloatArray(16)

        /**
         * Allocate storage for the final combined matrix. This will be passed into
         * the shader program.
         */
        private val mvpMatrix = FloatArray(16)

        /** Additional matrices.  */
        private val accumulatedRotation = FloatArray(16)
        private val currentRotation = FloatArray(16)
        private val temporaryMatrix = FloatArray(16)

        internal var eyePoint = Vec3D(0f, 0f, 21.12f)
        internal var lookAt = Vec3D(0f, 0f, -1f)
        internal var eyeUp = Vec3D(0f, 1f, 0f)

        internal var centerX = 0f
        internal var centerY = 0f
        internal var size = 2f
        internal var near = 1.0f
        internal var far = 200.0f

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            rendererSurfaceCreated = true

            GLES20.glDisable(GLES20.GL_DITHER)

            GLES20.glClearColor(0f, 0f, 0f, 0f)

            // Enable depth testing
//            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)

            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDepthMask(false)

            for (index in 0 until colorMaps.size()) {
                val key = colorMaps.keyAt(index)
                val colorMap = colorMaps[key]
                addColorMapObject(key, colorMap).also {
                    val objIndex = colorMapObjects.indexOfValue(it)
                    val objKey = colorMapObjects.keyAt(objIndex)
                    if (dataMap.indexOfKey(objKey) >= 0) {
                        it.setData(dataMap[objKey])
                        dataMap.remove(objKey)
                    }
                }
            }

            setLookAt()

            val vertexShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.per_pixel_vertex_shader_no_tex)
            val fragmentShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.per_pixel_fragment_shader_no_tex)

            val vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
            val fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

            program = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, arrayOf(POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE))

            // Initialize the accumulated rotation matrix
            Matrix.setIdentityM(accumulatedRotation, 0)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            // Set the OpenGL viewport to the same size as the surface.
            GLES20.glViewport(0, 0, width, height)

            frustumM()
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // Set our per-vertex lighting program.
            GLES20.glUseProgram(program)

            // Set program handles for cube drawing.
            mvpMatrixUniform = GLES20.glGetUniformLocation(program, MVP_MATRIX_UNIFORM)
            mvMatrixUniform = GLES20.glGetUniformLocation(program, MV_MATRIX_UNIFORM)
            positionAttribute = GLES20.glGetAttribLocation(program, POSITION_ATTRIBUTE)
            normalAttribute = GLES20.glGetAttribLocation(program, NORMAL_ATTRIBUTE)
            colorAttribute = GLES20.glGetAttribLocation(program, COLOR_ATTRIBUTE)

            // Draw the heightmap.
            // Translate the heightmap into the screen.
            Matrix.setIdentityM(modelMatrix, 0)
//            Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -12f)

            // Set a matrix that contains the current rotation.
            Matrix.setIdentityM(currentRotation, 0)
//            Matrix.rotateM(currentRotation, 0, deltaX, 0.0f, 1.0f, 0.0f)
//            Matrix.rotateM(currentRotation, 0, deltaY, 1.0f, 0.0f, 0.0f)
//            deltaX = 0.0f
//            deltaY = 0.0f

            // Multiply the current rotation by the accumulated rotation, and then
            // set the accumulated rotation to the result.
            Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation, 0)
            System.arraycopy(temporaryMatrix, 0, accumulatedRotation, 0, 16)

            // Rotate the cube taking the overall rotation into account.
            Matrix.multiplyMM(temporaryMatrix, 0, modelMatrix, 0, accumulatedRotation, 0)
            System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16)

            // This multiplies the view matrix by the model matrix, and stores
            // the result in the MVP matrix
            // (which currently contains model * view).
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)

            // Pass in the modelview matrix.
            GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0)

            // This multiplies the modelview matrix by the projection matrix,
            // and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16)

            // Pass in the combined matrix.
            GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0)

            // Render the heightmap.
            for (index in 0 until colorMapObjects.size()) {
                val colorMapObject = colorMapObjects.valueAt(index)
                colorMapObject.draw(positionAttribute, normalAttribute, colorAttribute)
            }
        }

        internal fun setLookAt() {
            // Position the eye in front of the origin.
//            val eyeX = Math.cos(phi) * Math.sin(theta) * length
//            val eyeY = Math.sin(phi) * length
//            val eyeZ = Math.cos(phi) * Math.cos(theta) * length
//            val eyeX = 54.64
//            val eyeY = 66.81
//            val eyeZ = 89.09

            // We are looking toward the distance
//            val lookX = Math.cos(phi) * Math.sin(theta) * length * -1.2
//            val lookY = Math.sin(phi) * length * -1.2
//            val lookZ = Math.cos(phi) * Math.cos(theta) * length * -1.2
//            val lookX = -15.62
//            val lookY = -15.65
//            val lookZ = -13.32

            // Set our up vector. This is where our head would be pointing were we
            // holding the camera.
//            val upX = 0.0
//            val upY = Math.cos(angle)
//            val upZ = Math.sin(angle)

            // Set the view matrix. This matrix can be said to represent the camera
            // position.
            // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
            // of a model and view matrix. In OpenGL 2, we can keep track of these
            // matrices separately if we choose.
            if (rendererSurfaceCreated) {
                Matrix.setLookAtM(
                    viewMatrix,
                    0,
                    eyePoint.x,
                    eyePoint.y,
                    eyePoint.z,
                    lookAt.x,
                    lookAt.y,
                    lookAt.z,
                    eyeUp.x,
                    eyeUp.y,
                    eyeUp.z
                )
            }
        }

        internal fun frustumM() {
            // Create a new perspective projection matrix. The height will stay the
            // same while the width will vary as per aspect ratio.

            if (rendererSurfaceCreated) {
                Matrix.frustumM(
                    projectionMatrix,
                    0,
                    centerX - size / 2,
                    centerX + size / 2,
                    centerY - size / 2,
                    centerY + size / 2,
                    near,
                    far
                )
            }
        }
    }

    lateinit var renderer: GLPressureRenderer

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        initialize()
    }

    fun initialize() {
        // OpenGL ES 2.0 context를 생성합니다.
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        getHolder().setFormat(PixelFormat.RGBA_8888)
        setZOrderOnTop(true)

        // GLSurfaceView에 그래픽 객체를 그리는 처리를 하는 renderer를 설정합니다.
        renderer = GLPressureRenderer()
        setRenderer(renderer)

        //Surface가 생성될때와 GLSurfaceView클래스의 requestRender 메소드가 호출될때에만 화면을 다시 그리게 됩니다.
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
    }

    fun addColorMap(colorMap: ColorMap) {
        addColorMap(0, colorMap)
    }

    fun addColorMap(key: Int, colorMap: ColorMap) {
        colorMaps.put(key, colorMap)
        if (rendererSurfaceCreated) {
            addColorMapObject(key, colorMap)
        }
    }

    private fun addColorMapObject(key: Int, colorMap: ColorMap): GLObject {
        return when (colorMap) {
            is ColorMap.RainbowColorMapRect -> {
                Square(
                    colorMap.centerPosition,
                    colorMap.vec1,
                    colorMap.vec2,
                    colorMap.rotation,
                    colorMap.flip,
                    colorMap.sizePerWidth,
                    colorMap.sizePerHeight
                ).also {
                    colorMapObjects.put(key, it)
                }
            }
            is ColorMap.RainbowColorMapCustom -> {
                CustomObject(
                    colorMap.vertexData,
                    colorMap.heightMapIndexData,
                    colorMap.centerPosition,
                    colorMap.vec1,
                    colorMap.vec2
                ).also {
                    colorMapObjects.put(key, it)
                }
            }
        }
    }

    fun setData(data: FloatArray) {
        setData(0, data)
    }

    fun setData(key: Int, data: FloatArray) {
        if (colorMapObjects.indexOfKey(key) >= 0) {
            colorMapObjects[key].setData(data)
            Handler(Looper.getMainLooper()).post {
                requestRender()
            }
        } else {
            dataMap.put(key, data)
        }
    }

    fun setViewStatus(eyePoint: Vec3D? = null, lookAt: Vec3D? = null, eyeUp: Vec3D? = null) {
        var changed = false
        eyePoint?.let {
            changed = changed or (renderer.eyePoint.equals(it))
            renderer.eyePoint = it
        }
        lookAt?.let {
            changed = changed or (renderer.lookAt.equals(it))
            renderer.lookAt = it
        }
        eyeUp?.let {
            changed = changed or (renderer.eyeUp.equals(it))
            renderer.eyeUp = it
        }
        if (changed) {
            renderer.setLookAt()
            requestRender()
        }
    }

    fun setViewport(centerX: Float? = null, centerY: Float? = null, size: Float? = null) {
        var changed = false
        centerX?.let {
            changed = changed or (renderer.centerX == it)
            renderer.centerX = it
        }
        centerY?.let {
            changed = changed or (renderer.centerY == it)
            renderer.centerY = it
        }
        size?.let {
            changed = changed or (renderer.size == it)
            renderer.size = it
        }
        if (changed) {
            renderer.frustumM()
            requestRender()
        }
    }

    fun moveViewport(dx: Float, dy: Float) {
        renderer.centerX += dx
        renderer.centerY += dy

        renderer.frustumM()
        requestRender()
    }

    fun zoomViewport(zoom: Float) {
        renderer.size *= zoom

        renderer.frustumM()
        requestRender()
    }
}

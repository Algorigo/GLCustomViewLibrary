package com.algorigo.glcustomviewlibrary

import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class CustomObject(private val vertexData: List<PointF>,
                            private val heightMapIndexData: IntArray,
                            private val centerPosition: CustomGLView.Vec3D = CustomGLView.Vec3D(0f, 0f, 0f),
                            private val vec1: CustomGLView.Vec3D = CustomGLView.Vec3D(22f, 0f, 0f),
                            private val vec2: CustomGLView.Vec3D = CustomGLView.Vec3D(0f, 22f, 0f)) : CustomGLView.GLObject {

    private val vbo = IntArray(1)
    private val ibo = IntArray(1)

    private var heightMapVertexData: FloatArray

    init {
        heightMapVertexData = FloatArray(vertexData.size * FLOATS_PER_VERTEX)

        var offset = 0

        for (vertexDatum in vertexData) {
            val position = getPosition(vertexDatum.x, vertexDatum.y)

            heightMapVertexData[offset++] = position.x
            heightMapVertexData[offset++] = position.y
            heightMapVertexData[offset++] = position.z

            // Cheap normal using a derivative of the function.
            // The slope for X will be 2X, for Y will be 2Y.
            // Divide by 10 since the position's Z is also divided by 10.
            val xSlope = 2 * position.x / 10f
            val ySlope = 2 * position.y / 10f

            // Calculate the normal using the cross product of the slopes.
            val planeVectorX = floatArrayOf(1f, 0f, xSlope)
            val planeVectorY = floatArrayOf(0f, 1f, ySlope)
            val normalVector = floatArrayOf(planeVectorX[1] * planeVectorY[2] - planeVectorX[2] * planeVectorY[1], planeVectorX[2] * planeVectorY[0] - planeVectorX[0] * planeVectorY[2], planeVectorX[0] * planeVectorY[1] - planeVectorX[1] * planeVectorY[0])

            // Normalize the normal
            val length = Matrix.length(normalVector[0], normalVector[1], normalVector[2])

            heightMapVertexData[offset++] = normalVector[0] / length
            heightMapVertexData[offset++] = normalVector[1] / length
            heightMapVertexData[offset++] = normalVector[2] / length

            // Add some fancy colors.
            heightMapVertexData[offset++] = vertexDatum.x
            heightMapVertexData[offset++] = vertexDatum.y
            heightMapVertexData[offset++] = 0.5f
            heightMapVertexData[offset++] = 1.0f
        }
    }

    protected fun finalize() {
        releaseBuffer()
    }

    override fun draw(positionAttribute: Int, normalAttribute: Int, colorAttribute: Int) {
        releaseBuffer()

        val heightMapVertexDataBuffer = ByteBuffer
            .allocateDirect(heightMapVertexData.size * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        heightMapVertexDataBuffer.put(heightMapVertexData).position(0)

        val heightMapIndexDataBuffer = ByteBuffer
            .allocateDirect(heightMapIndexData.size * BYTES_PER_INT).order(ByteOrder.nativeOrder())
            .asIntBuffer()
        heightMapIndexDataBuffer.put(heightMapIndexData).position(0)

        GLES20.glGenBuffers(1, vbo, 0)
        GLES20.glGenBuffers(1, ibo, 0)

        if (vbo[0] > 0 && ibo[0] > 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER, heightMapVertexDataBuffer.capacity() * BYTES_PER_FLOAT,
                heightMapVertexDataBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, heightMapIndexDataBuffer.capacity() * BYTES_PER_INT, heightMapIndexDataBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])

            // Bind Attributes
            GLES20.glVertexAttribPointer(positionAttribute,
                POSITION_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                STRIDE, 0)
            GLES20.glEnableVertexAttribArray(positionAttribute)

            GLES20.glVertexAttribPointer(normalAttribute,
                NORMAL_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                STRIDE, POSITION_DATA_SIZE_IN_ELEMENTS * BYTES_PER_FLOAT
            )
            GLES20.glEnableVertexAttribArray(normalAttribute)

            GLES20.glVertexAttribPointer(colorAttribute,
                COLOR_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT
            )
            GLES20.glEnableVertexAttribArray(colorAttribute)

            // Draw
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, heightMapIndexData.size, GLES20.GL_UNSIGNED_INT, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        }
    }

    private fun releaseBuffer() {
        if (vbo[0] > 0) {
            GLES20.glDeleteBuffers(vbo.size, vbo, 0)
            vbo[0] = 0
        }

        if (ibo[0] > 0) {
            GLES20.glDeleteBuffers(ibo.size, ibo, 0)
            ibo[0] = 0
        }
    }

    override fun setData(data: FloatArray) {
        if (data.size != vertexData.size) {
            return
        }

        var offset = 6

        // First, build the data for the vertex buffer
        for (datum in data) {
            // Add some fancy colors.
            val color = ColorMapper.getColor(datum * 100)
            heightMapVertexData[offset] = color[0]/255f
            heightMapVertexData[offset+1] = color[1]/255f
            heightMapVertexData[offset+2] = color[2]/255f
            heightMapVertexData[offset+3] = color[3]/255f

            offset += FLOATS_PER_VERTEX
        }
    }

    private fun getPosition(x: Float, y: Float): CustomGLView.Vec3D {
        val xRatio = x * 2f - 1f
        val yRatio = 1f - y * 2f
        val xValue = centerPosition.x + xRatio * vec1.x + yRatio * vec2.x
        val yValue = centerPosition.y + xRatio * vec1.y + yRatio * vec2.y
        val zValue = centerPosition.z + xRatio * vec1.z + yRatio * vec2.z
        return CustomGLView.Vec3D(xValue, yValue, zValue)
    }

    companion object {
        private val POSITION_DATA_SIZE_IN_ELEMENTS = 3
        private val NORMAL_DATA_SIZE_IN_ELEMENTS = 3
        private val COLOR_DATA_SIZE_IN_ELEMENTS = 4
        private val FLOATS_PER_VERTEX = POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS

        private val BYTES_PER_FLOAT = 4
        private val BYTES_PER_SHORT = 2
        private val BYTES_PER_INT = 4

        private val STRIDE = (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT
    }
}
package com.algorigo.glcustomviewlibrary

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder


internal class Square(private val centerPosition: CustomGLView.Vec3D = CustomGLView.Vec3D(0f, 0f, 0f),
                      private val vec1: CustomGLView.Vec3D = CustomGLView.Vec3D(22f, 0f, 0f),
                      private val vec2: CustomGLView.Vec3D = CustomGLView.Vec3D(0f, 22f, 0f),
                      private var rotation: CustomGLView.Rotation = CustomGLView.Rotation.NORMAL,
                      flip: Boolean = false,
                      private val sizePerWidth: Int = 57,
                      private val sizePerHeight: Int = 57) {

    private val vbo = IntArray(1)
    private val ibo = IntArray(1)

    private var indexCount = 0

    private lateinit var heightMapVertexData: FloatArray
    private lateinit var heightMapIndexData: IntArray

    private var flip: Int

    init {
        this.flip = if (flip) -1 else 1

        try {
            val xLength = sizePerWidth
            val yLength = sizePerHeight

            heightMapVertexData = FloatArray(xLength * yLength * FLOATS_PER_VERTEX)

            var offset = 0

            // First, build the data for the vertex buffer
            for (y in 0 until yLength) {
                for (x in 0 until xLength) {
                    val position = getPosition(x, xLength, y, yLength)

                    // Position
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
                    heightMapVertexData[offset++] = x.toFloat() / xLength
                    heightMapVertexData[offset++] = y.toFloat() / yLength
                    heightMapVertexData[offset++] = 0.5f
                    heightMapVertexData[offset++] = 1.0f
                }
            }

            // Now build the index data
            val numStripsRequired = yLength - 1
            val numDegensRequired = 2 * (numStripsRequired - 1)
            val verticesPerStrip = 2 * xLength

            heightMapIndexData = IntArray(verticesPerStrip * numStripsRequired + numDegensRequired)

            offset = 0

            if (this.flip > 0) {
                for (y in 0 until yLength - 1) {
                    if (y > 0) {
                        // Degenerate begin: repeat first vertex
                        heightMapIndexData[offset++] = (y * xLength).toInt()
                    }

                    for (x in 0 until xLength) {
                        // One part of the strip
                        heightMapIndexData[offset++] = (y * xLength + x).toInt()
                        heightMapIndexData[offset++] = ((y + 1) * xLength + x).toInt()
                    }

                    if (y < yLength - 2) {
                        // Degenerate end: repeat last vertex
                        heightMapIndexData[offset++] = ((y + 1) * xLength + (xLength - 1)).toInt()
                    }
                }
            } else {
                for (y in 0 until yLength - 1) {
                    if (y > 0) {
                        // Degenerate begin: repeat first vertex
                        heightMapIndexData[offset++] = (y * xLength + (xLength - 1)).toInt()
                    }

                    for (x in xLength-1 downTo 0) {
                        // One part of the strip
                        heightMapIndexData[offset++] = (y * xLength + x).toInt()
                        heightMapIndexData[offset++] = ((y + 1) * xLength + x).toInt()
                    }

                    if (y < yLength - 2) {
                        // Degenerate end: repeat last vertex
                        heightMapIndexData[offset++] = ((y + 1) * xLength).toInt()
                    }
                }
            }

            indexCount = heightMapIndexData.size
        } catch (t: Throwable) {
            Log.w(LOG_TAG, t)
        }
    }

    protected fun finalize() {
        releaseBuffer()
    }

    fun draw(positionAttribute: Int, normalAttribute: Int, colorAttribute: Int) {
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
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, heightMapVertexDataBuffer.capacity() * BYTES_PER_FLOAT,
                    heightMapVertexDataBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, heightMapIndexDataBuffer.capacity() * BYTES_PER_INT, heightMapIndexDataBuffer, GLES20.GL_STATIC_DRAW)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])

            // Bind Attributes
            GLES20.glVertexAttribPointer(positionAttribute, POSITION_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                    STRIDE, 0)
            GLES20.glEnableVertexAttribArray(positionAttribute)

            GLES20.glVertexAttribPointer(normalAttribute, NORMAL_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                    STRIDE, POSITION_DATA_SIZE_IN_ELEMENTS * BYTES_PER_FLOAT)
            GLES20.glEnableVertexAttribArray(normalAttribute)

            GLES20.glVertexAttribPointer(colorAttribute, COLOR_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                    STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT)
            GLES20.glEnableVertexAttribArray(colorAttribute)

            // Draw
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indexCount, GLES20.GL_UNSIGNED_INT, 0)

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

    fun setData(data: Array<FloatArray>) {
        val xLength = data[0].size
        val yLength = data.size
        if (xLength != sizePerWidth && yLength != sizePerHeight) {
            return
        }

        var offset = 6

        // First, build the data for the vertex buffer
        for (y in 0 until yLength) {
            for (x in 0 until xLength) {
                // Add some fancy colors.
                var datum = data[y][x]
                val color = ColorMapper.getColor(datum * 100)
                heightMapVertexData[offset] = color[0]/255f
                heightMapVertexData[offset+1] = color[1]/255f
                heightMapVertexData[offset+2] = color[2]/255f
                heightMapVertexData[offset+3] = color[3]/255f

                offset += FLOATS_PER_VERTEX
            }
        }
    }

    private fun getPosition(x: Int, xLength: Int, y: Int, yLength: Int): CustomGLView.Vec3D {
        val xRatio: Float
        val yRatio: Float
        when (rotation) {
            CustomGLView.Rotation.NORMAL -> {
                xRatio = flip * (x * 2f / (xLength - 1f) - 1f)
                yRatio = 1f - y * 2f / (yLength - 1f)
            }
            CustomGLView.Rotation.CW_90 -> {
                xRatio = flip * (y * 2f / (yLength - 1f) - 1f)
                yRatio = x * 2f / (xLength - 1f) - 1f
            }
            CustomGLView.Rotation.CW_180 -> {
                xRatio = flip * (1f - x * 2f / (xLength - 1f))
                yRatio = y * 2f / (yLength - 1f) - 1f
            }
            CustomGLView.Rotation.CW_270 -> {
                xRatio = flip * (1f - y * 2f / (yLength - 1f))
                yRatio = 1f - x * 2f / (xLength - 1f)
            }
        }
        val xValue = centerPosition.x + xRatio * vec1.x + yRatio * vec2.x
        val yValue = centerPosition.y + xRatio * vec1.y + yRatio * vec2.y
        val zValue = centerPosition.z + xRatio * vec1.z + yRatio * vec2.z
        return CustomGLView.Vec3D(xValue, yValue, zValue)
    }

    companion object {

        private val LOG_TAG = Square::class.java.simpleName

        /** Additional constants.  */
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

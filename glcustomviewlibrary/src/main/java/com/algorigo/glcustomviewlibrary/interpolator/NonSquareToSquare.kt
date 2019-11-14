package com.algorigo.glcustomviewlibrary.interpolator

import android.graphics.PointF
import kotlin.math.*



object NonSquareToSquare {

    private val LOG_TAG = NonSquareToSquare::class.java.simpleName

    private const val LIFE_LENGTH = 0.2
    private const val ROUND_ORDER = -2

    class PointD(val x: Double, val y: Double) {
        constructor(pt: PointF) : this(pt.x.toDouble(), pt.y.toDouble())

        override fun toString(): String {
            return "$x,$y"
        }

        operator fun plus(value: PointD): PointD {
            return PointD(
                x + value.x,
                y + value.y
            )
        }

        fun scalar(value: Double): PointD {
            return PointD(
                x * value,
                y * value
            )
        }
    }
    private class Vec3D(val x: Double, val y: Double, val z: Double) {

        constructor(pt: PointD, z: Double) : this(pt.x, pt.y, z)

        operator fun plus(value: Vec3D): Vec3D {
            return Vec3D(
                x + value.x,
                y + value.y,
                z + value.z
            )
        }

        operator fun minus(value: Vec3D): Vec3D {
            return Vec3D(
                x - value.x,
                y - value.y,
                z - value.z
            )
        }

        fun scalar(value: Double): Vec3D {
            return Vec3D(
                x * value,
                y * value,
                z * value
            )
        }

        fun cross(value: Vec3D): Vec3D {
            return Vec3D(
                y * value.z - z * value.y,
                z * value.x - x * value.z,
                x * value.y - y * value.x
            )
        }

        fun dot(value: Vec3D): Double {
            return x*value.x + y*value.y + z*value.z
        }

        override fun toString(): String {
            return "$x,$y,$z"
        }
    }

    interface Shape {
        // Class 내부에서만 사용하는 method들
        fun isPointInside(pt: PointD): Boolean
        fun getHeightOfPosition(pt: PointD, intArray: IntArray): Double
    }

    class Triangle(pt1: PointF, pt2: PointF, pt3: PointF, val heightListDelegate: (IntArray) -> List<Double>) :
        Shape {

        private val pt1: PointD
        private val pt2: PointD
        private val pt3: PointD

        init {
            this.pt1 = PointD(pt1)
            this.pt2 = PointD(pt2)
            this.pt3 = PointD(pt3)
        }

        override fun isPointInside(pt: PointD): Boolean {
            return isInside(
                pt,
                pt1,
                pt2,
                pt3
            )
        }

        override fun getHeightOfPosition(pt: PointD, intArray: IntArray): Double {
            return heightListDelegate(intArray).let {
                getValueBy3Points(
                    pt,
                    listOf(Pair(pt1, it[0]), Pair(pt2, it[1]), Pair(pt3, it[2]))
                )
            }
        }

        override fun toString(): String {
            return "Triangle($pt1 - $pt2 - $pt3)"
        }
    }

    class Rectangle(pt1: PointF, pt2: PointF, pt3: PointF, pt4: PointF, val heightListDelegate: (IntArray) -> List<Double>) :
        Shape {

        private val pt1: PointD
        private val pt2: PointD
        private val pt3: PointD
        private val pt4: PointD

        init {
            this.pt1 = PointD(pt1)
            this.pt2 = PointD(pt2)
            this.pt3 = PointD(pt3)
            this.pt4 = PointD(pt4)
        }

        override fun isPointInside(pt: PointD): Boolean {
            return isInside(
                pt,
                pt1,
                pt2,
                pt3,
                pt4
            )
        }

        override fun getHeightOfPosition(pt: PointD, intArray: IntArray): Double {
            return heightListDelegate(intArray).let {
                getValueBy4Points(
                    pt,
                    listOf(Pair(pt1, it[0]), Pair(pt2, it[1]), Pair(pt3, it[2]), Pair(pt4, it[3]))
                )
            }
        }

        override fun toString(): String {
            return "Rectangle($pt1 - $pt2 - $pt3 - $pt4)"
        }
    }

    class OutsidePoints(val heightListDelegate: (IntArray) -> List<Double>, vararg points: PointF) :
        Shape {

        private val points: List<PointD>

        init {
            this.points = points.map {
                PointD(
                    it
                )
            }
        }

        override fun isPointInside(pt: PointD): Boolean {
            return true
        }

        override fun getHeightOfPosition(pt: PointD, intArray: IntArray): Double {
            if (pt.x == 0.0 || pt.y == 0.0 || pt.x == 1.0 || pt.y == 1.0) return 0.0

            var closestPoint = points.first().let { Pair(it,
                getDistance(it, pt)
            ) }
            for (index in 1 until points.size) {
                val point = points[index].let { Pair(it,
                    getDistance(
                        it,
                        pt
                    )
                ) }
                if (point.second < closestPoint.second) {
                    closestPoint = point
                }
            }

//            Log.e("!!!", "closest point:${closestPoint.second}:${closestPoint.first}")

            var closestLine = Pair(points.first(), points.last())
            var closestDistance =
                getDistanceToSegment(
                    pt,
                    closestLine.first,
                    closestLine.second
                )
            for (index in 0 until points.size - 1) {
                var distance =
                    getDistanceToSegment(
                        pt,
                        points[index],
                        points[index + 1]
                    )
                if (closestDistance > distance) {
                    closestLine = Pair(points[index], points[index+1])
                    closestDistance = distance
                }
            }
//            Log.e("!!!", "closest line:${closestDistance}:${closestLine.first} - ${closestLine.second}")

            if (closestDistance < LIFE_LENGTH) {
                val foot =
                    getFootInTheLineSegment(
                        pt,
                        closestLine.first,
                        closestLine.second
                    )
                val ratio = if (closestLine.second.x != closestLine.first.x) (foot.x-closestLine.first.x)/(closestLine.second.x-closestLine.first.x) else (foot.y-closestLine.first.y)/(closestLine.second.y-closestLine.first.y)
                val footValue = heightListDelegate(intArray).let {
                    val firstValue = it[points.indexOf(closestLine.first)]
                    val secondValue = it[points.indexOf(closestLine.second)]
                    firstValue * (1-ratio) + secondValue * ratio
                }
//                Log.e("!!!", "line : $foot, $ratio, $footValue, $closestDistance")
                return footValue * (LIFE_LENGTH - closestDistance) / LIFE_LENGTH
            }

            if (closestPoint.second < LIFE_LENGTH) {
                val value = heightListDelegate(intArray)[points.indexOf(closestPoint.first)]
//                Log.e("!!!", "point : $value, ${closestPoint.second}")
                return value * (LIFE_LENGTH - closestPoint.second) / LIFE_LENGTH
            }

            return 0.0
        }
    }

    fun map(intArray: IntArray, width: Int, height: Int, shapes: List<Shape>): IntArray {
//        val started = System.currentTimeMillis()
        val array = IntArray(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val index = y * width + x
                array[index] = getValue(
                    PointD(
                        x.toDouble() / (width - 1),
                        y.toDouble() / (height - 1)
                    ),
                    intArray,
                    shapes,
                    height.toDouble() / width
                ).roundToInt()
            }
        }
//        Log.i(LOG_TAG, "map time:${(System.currentTimeMillis()-started)/1000f}sec")
        return array
    }

    private fun getValue(pt: PointD, intArray: IntArray, shapes: List<Shape>, ratio: Double): Double {
        var shape: Shape? = null
        for (aShape in shapes) {
            if (aShape.isPointInside(pt)) {
                shape = aShape
                break
            }
        }
        Assert.check(
            shape != null,
            LOG_TAG,
            IllegalStateException("Shape is Not Found")
        )
        return (shape?.getHeightOfPosition(pt, intArray) ?: 0.0)
    }

    private fun getValueBy3Points(pt: PointD, pointList: List<Pair<PointD, Double>>): Double {
        val a = pointList[0].let { point ->
            Vec3D(
                point.first,
                point.second
            )
        }
        val b = pointList[1].let { point ->
            Vec3D(
                point.first,
                point.second
            )
        }
        val c = pointList[2].let { point ->
            Vec3D(
                point.first,
                point.second
            )
        }
        return getValueBy3Vec3Ds(
            pt,
            a,
            b,
            c
        )
    }

    private fun getValueBy3Vec3Ds(pt: PointD, a: Vec3D, b: Vec3D, c: Vec3D): Double {
        val vec1 = b.minus(a)
        val vec2 = c.minus(a)
        val vec = vec1.cross(vec2)
        val d = vec.dot(a)
        val value = if (vec.z != 0.0) (d - pt.x * vec.x - pt.y * vec.y) / vec.z else 0.0

        return value
    }

    private fun getValueBy4Points(pt: PointD, pointList: List<Pair<PointD, Double>>): Double {
        val pt1 = pointList[0].first
        val z1 = pointList[0].second
        val pt2 = pointList[1].first
        val z2 = pointList[1].second
        val pt3 = pointList[2].first
        val z3 = pointList[2].second
        val pt4 = pointList[3].first
        val z4 = pointList[3].second
        val ptCenter = PointD(
            (pt1.x + pt2.x + pt3.x + pt4.x) / 4,
            (pt1.y + pt2.y + pt3.y + pt4.y) / 4
        )
        val zCenter = (z1+z2+z3+z4)/4
        if (isInside(
                pt,
                pt1,
                pt2,
                ptCenter
            )
        ) {
            return getValueBy3Points(
                pt, listOf(
                    Pair(pt1, z1),
                    Pair(pt2, z2),
                    Pair(ptCenter, zCenter)
                )
            )
        } else if (isInside(
                pt,
                pt2,
                pt3,
                ptCenter
            )
        ) {
            return getValueBy3Points(
                pt, listOf(
                    Pair(pt2, z2),
                    Pair(pt3, z3),
                    Pair(ptCenter, zCenter)
                )
            )
        } else if (isInside(
                pt,
                pt3,
                pt4,
                ptCenter
            )
        ) {
            return getValueBy3Points(
                pt, listOf(
                    Pair(pt3, z3),
                    Pair(pt4, z4),
                    Pair(ptCenter, zCenter)
                )
            )
        } else if (isInside(
                pt,
                pt4,
                pt1,
                ptCenter
            )
        ) {
            return getValueBy3Points(
                pt, listOf(
                    Pair(pt4, z4),
                    Pair(pt1, z1),
                    Pair(ptCenter, zCenter)
                )
            )
        }
        throw IllegalStateException("Wrong $pt not inside $pt1 - $pt2 - $pt3 - $pt4, center:$ptCenter")
//        val Ax = pt1.x - pt2.x + pt3.x - pt4.x
//        val Ay = pt1.y - pt2.y + pt3.y - pt4.y
//        val Bx = -pt1.x + pt2.x
//        val By = -pt1.y + pt2.y
//        val Cx = -pt1.x + pt4.x
//        val Cy = -pt1.y + pt4.y
//        val Dx = pt1.x - pt.x
//        val Dy = pt1.y - pt.y
//        val a = Cx*Ay - Cy*Ax
//        val b = Cx*By + Dx*Ay - Bx*Cy - Ax*Dy
//        val c = Dx*By - Bx*Dy
//        val beta = if (a.round(ROUND_ORDER) == 0f) -c / b else -b / 2 / a + Math.sqrt((b.pow(2) - 4*a*c).round(ROUND_ORDER).toDouble()).toDouble()
//        val alpha = -(Cx*beta + Dx) / (Ax*beta + Bx)
//        Assert.check(beta < 0f || beta > 1f || alpha < 0f || alpha > 1f, LOG_TAG, IllegalArgumentException("alpha, beta is wrong : ${a}x2+${b}x+$c -> $alpha,$beta"))
//        val value = (1 - alpha) * (1 - beta) * pt1.z + alpha * (1 - beta) * pt2.z + alpha * beta * pt3.z + (1 - alpha) * beta * pt4.z
//        Log.e("!!!", "$pt1 - $pt2 - $pt3 - $pt4 -> $pt : ${a}x2+${b}x+$c -> $alpha,$beta:$value")
//        Assert.check(!value.isNaN(), LOG_TAG, IllegalArgumentException("value is NaN : ${a}x2+${b}x+$c -> $alpha,$beta"))
//        return if (value.isNaN()) 0f else value
    }

    private fun getAngle(pt1: PointD, pt2: PointD, pt3: PointD): Double {
        return Math.acos(
            (((pt1.x-pt2.x)*(pt3.x-pt2.x) + (pt1.y-pt2.y)*(pt3.y-pt2.y))
                / (Math.sqrt((pt1.x-pt2.x).pow(2)+(pt1.y-pt2.y).pow(2)) * Math.sqrt((pt3.x-pt2.x).pow(2)+(pt3.y-pt2.y).pow(2)))
                    ).let { if (it > 1.0) 1.0 else if (it < -1.0) -1.0 else it }
        ).also {
            Assert.check(
                !it.isNaN(),
                LOG_TAG,
                IllegalArgumentException(
                    "value is NaN:$pt1 - $pt2 - $pt3 -> ${(pt1.x - pt2.x) * (pt3.x - pt2.x) + (pt1.y - pt2.y) * (pt3.y - pt2.y)},${Math.sqrt(
                        ((pt1.x - pt2.x).pow(2) + (pt1.y - pt2.y).pow(2))
                    ) * Math.sqrt(((pt3.x - pt2.x).pow(2) + (pt3.y - pt2.y).pow(2)))}"
                )
            )
        }.let {
            if (it.round(ROUND_ORDER) == Math.PI.round(
                    ROUND_ORDER
                )) it else if ((pt1.x-pt2.x)*(pt3.y-pt2.y)-(pt1.y-pt2.y)*(pt3.x-pt2.x) > 0) it else -it
        }
    }

    private fun getDistance(pt1: PointD, pt2: PointD): Double {
        return Math.sqrt((pt1.x-pt2.x).pow(2)+(pt1.y-pt2.y).pow(2))
    }

    private fun isFootInTheLineSegment(pt: PointD, pt1: PointD, pt2: PointD): Boolean {
        return getAngle(
            pt,
            pt1,
            pt2
        ).absoluteValue < Math.PI/2 && getAngle(
            pt,
            pt2,
            pt1
        ).absoluteValue < Math.PI/2
    }

    private fun getFootInTheLineSegment(pt: PointD, pt1: PointD, pt2: PointD): PointD {
        return getFootInTheLineSegment(
            pt,
            pt2.y - pt1.y,
            pt1.x - pt2.x,
            pt1.y * pt2.x - pt1.x * pt2.y
        )
    }

    private fun getFootInTheLineSegment(pt: PointD, a: Double, b: Double, c: Double): PointD {
        return (-1 * (a * pt.x + b * pt.y + c) / (a * a + b * b)).let {
            PointD(
                it * a + pt.x,
                it * b + pt.y
            )
        }
    }

    private fun getDistanceToSegment(pt: PointD, pt1: PointD, pt2: PointD): Double {
        return if (isFootInTheLineSegment(
                pt,
                pt1,
                pt2
            )
        ) {
            getDistanceToLine(
                pt,
                pt1,
                pt2
            )
        } else { Double.MAX_VALUE }
    }

    private fun getDistanceToLine(pt: PointD, pt1: PointD, pt2: PointD): Double {
        return getDistanceToLine(
            pt,
            pt2.y - pt1.y,
            pt1.x - pt2.x,
            pt1.y * pt2.x - pt1.x * pt2.y
        )
    }

    // Function to find distance
    private fun getDistanceToLine(pt: PointD, a: Double, b: Double, c: Double): Double {
        return Math.abs((a * pt.x + b * pt.y + c) / Math.sqrt((a * a + b * b)))
    }

    private fun onSegment(p: PointD, q: PointD, r: PointD): Boolean {
        return q.x <= max(p.x, r.x) && q.x >= min(p.x, r.x)
                && q.y <= max(p.y, r.y) && q.y >= min(p.y, r.y)
    }

    // To find orientation of ordered triplet (p, q, r).
    // The function returns following values
    // 0 --> p, q and r are colinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    private fun orientation(p: PointD, q: PointD, r: PointD): Int {
        val value = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)

        if (value == 0.0) return 0  // colinear
        return if (value > 0) 1 else 2 // clock or counterclock wise
    }

    // The function that returns true if line segment 'p1q1'
    // and 'p2q2' intersect.
    private fun doIntersect(p1: PointD, q1: PointD, p2: PointD, q2: PointD): Boolean {
        // Find the four orientations needed for general and
        // special cases
        val o1 =
            orientation(p1, q1, p2)
        val o2 =
            orientation(p1, q1, q2)
        val o3 =
            orientation(p2, q2, p1)
        val o4 =
            orientation(p2, q2, q1)

        // General case
        if (o1 != o2 && o3 != o4)
            return true

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(
                p1,
                p2,
                q1
            )
        ) return true

        // p1, q1 and p2 are colinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(
                p1,
                q2,
                q1
            )
        ) return true

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(
                p2,
                p1,
                q2
            )
        ) return true

         // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(
                p2,
                q1,
                q2
            )
        ) return true

        return false // Doesn't fall in any of the above cases
    }

    // Returns true if the point p lies inside the polygon[] with n vertices
    private fun isInside(p: PointD, vararg polygon: PointD): Boolean {
        // There must be at least 3 vertices in polygon[]
        if (polygon.size < 3)  return false

        // Create a point for line segment from p to infinite
        val extreme = PointD(
            Double.MAX_VALUE,
            p.y
        )

        // Count intersections of the above line with sides of polygon
        var count = 0
        var i = 0
        do {
            val next = (i+1) % polygon.size

            // Check if the line segment from 'p' to 'extreme' intersects
            // with the line segment from 'polygon[i]' to 'polygon[next]'
            if (doIntersect(
                    polygon[i],
                    polygon[next],
                    p,
                    extreme
                )
            ) {
                // If the point 'p' is colinear with line segment 'i-next',
                // then check if it lies on segment. If it lies, return true,
                // otherwise false
                if (orientation(
                        polygon[i],
                        p,
                        polygon[next]
                    ) == 0)
                   return onSegment(
                       polygon[i],
                       p,
                       polygon[next]
                   )

                count++
            }
            i = next
        } while (i != 0)

        // Return true if count is odd, false otherwise
        return count%2 == 1
    }

    private fun Double.round(index: Int): Double {
        return 10.0.pow(-index).let {
            (this*it).roundToInt()/it
        }
    }
}
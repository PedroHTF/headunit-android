package com.headunit.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CameraModelTest {
    private val pose = CameraPose(heightM = 0.9, pitchDeg = 30.0)
    private val fisheye = CameraProjector(pose, Lens(150.0, 0.8), 640, 360)
    private val pinhole = CameraProjector(pose, Lens(100.0, 0.0), 640, 360)

    @Test
    fun pointStraightBehindIsOnCenterColumn() {
        val px = fisheye.project(Ground(0.0, 2.0))!!
        assertEquals(320.0, px.u, 1e-6)
    }

    @Test
    fun fartherPointsAreHigherInTheImage() {
        val near = pinhole.project(Ground(0.0, 1.0))!!
        val far = pinhole.project(Ground(0.0, 3.0))!!
        assertTrue(far.v < near.v)
    }

    @Test
    fun groundRoundTrip() {
        for (lens in listOf(fisheye, pinhole)) {
            val g = Ground(0.7, 2.3)
            val back = lens.toGround(lens.project(g)!!)!!
            assertEquals(g.x, back.x, 1e-6)
            assertEquals(g.y, back.y, 1e-6)
        }
    }

    @Test
    fun skyHasNoGround() {
        assertNull(pinhole.toGround(Pixel(320.0, 0.0)))
    }

    @Test
    fun straightLinesStayStraightWithoutFisheye() {
        assertTrue(maxDeviation(pinhole) < 0.5)
    }

    @Test
    fun fisheyeBendsStraightLines() {
        assertTrue(maxDeviation(fisheye) > 5.0)
    }

    @Test
    fun undistortMapsCenterToCenter() {
        val small = CameraProjector(pose, Lens(150.0, 0.8), 64, 36)
        val out = CameraProjector(pose, Lens(100.0, 0.0), 64, 36)
        val map = buildUndistortMap(small, out)
        val center = map[18 * 64 + 32]
        assertNotNull(center)
        assertEquals(32.0, (center % 64).toDouble(), 1.0)
        assertEquals(18.0, (center / 64).toDouble(), 1.0)
    }

    /** Distância máxima (px) dos pontos de uma reta do chão até a reta entre as pontas. */
    private fun maxDeviation(cam: CameraProjector): Double {
        val pts = (0..20).mapNotNull { cam.project(Ground(-1.5 + it * 0.15, 1.2)) }
        val a = pts.first()
        val b = pts.last()
        return pts.maxOf { p ->
            abs((b.v - a.v) * p.u - (b.u - a.u) * p.v + b.u * a.v - b.v * a.u) /
                Math.hypot(b.v - a.v, b.u - a.u)
        }
    }
}

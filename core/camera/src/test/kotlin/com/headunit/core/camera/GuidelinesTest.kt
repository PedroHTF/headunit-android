package com.headunit.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidelinesTest {
    private val car = VehicleDimensions(widthM = 1.8, lengthM = 4.0)

    @Test
    fun straightSteeringGivesStraightTrajectory() {
        val path = Guidelines.trajectory(0.9, car, 0.0, 3.0)
        assertTrue(path.all { it.x == 0.9 })
        assertEquals(3.0, path.last().y, 1e-9)
    }

    @Test
    fun steeringRightCurvesToPositiveX() {
        val path = Guidelines.trajectory(0.0, car, 20.0, 3.0)
        assertTrue(path.last().x > 0.2)
        assertTrue(path.zipWithNext().all { (a, b) -> b.y > a.y }) // sempre andando para trás
    }

    @Test
    fun steeringLeftIsMirrorOfRight() {
        val r = Guidelines.trajectory(0.9, car, 20.0, 3.0).last()
        val l = Guidelines.trajectory(-0.9, car, -20.0, 3.0).last()
        assertEquals(r.x, -l.x, 1e-9)
        assertEquals(r.y, l.y, 1e-9)
    }

    @Test
    fun trajectoryStartsAtBumperCorner() {
        val p = Guidelines.trajectory(-0.9, car, 15.0, 3.0).first()
        assertEquals(-0.9, p.x, 1e-9)
        assertEquals(0.0, p.y, 1e-9)
    }

    @Test
    fun wheelAngleUsesRatioAndLimit() {
        assertEquals(10.0, Guidelines.wheelAngleDeg(150.0, car), 1e-9)
        assertEquals(35.0, Guidelines.wheelAngleDeg(900.0, car), 1e-9)
    }

    @Test
    fun carLengthMarkersUseVehicleLength() {
        val m = Guidelines.markers(car, GuideOptions(distanceMarkersM = listOf(1.0), carLengthMarkers = listOf(1, 2)))
        assertEquals(listOf(1.0 to "1 m", 4.0 to "1 carro", 8.0 to "2 carros"), m)
    }

    @Test
    fun offsetsShiftLines() {
        val lines = Guidelines.build(car, GuideOptions(showDynamic = false, lineOffsetXM = 0.1), 0.0)
        assertTrue(lines.flatMap { it.points }.any { kotlin.math.abs(it.x - 1.0) < 1e-9 })
    }

    @Test
    fun invertSteeringFlipsCurve() {
        val opts = GuideOptions(showStatic = false, invertSteering = true)
        val lines = Guidelines.build(car, opts, 300.0)
        assertTrue(lines.all { it.points.last().x < it.points.first().x })
    }
}

package com.moneysearch

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class CoordinatesCalculatorTest {

    private val vaskaCenterLocation = Location(
        latitude = 59.941937,
        longitude = 30.257087
    )

    @Test
    fun test() {
        val coordinatesCalculator = CoordinatesCalculator()
        val bounds = coordinatesCalculator.getBounds(
            location = vaskaCenterLocation,
            distance = 1000
        )
        assertEquals(59.93290, bounds.bottomLeft.lat, 0.001, "Bottom left latitude assert")
        assertEquals(30.23920, bounds.bottomLeft.lng, 0.001, "Bottom left longitude assert")
        assertEquals(59.9509, bounds.topRight.lat, 0.001, "Right top latitude assert")
        assertEquals(30.2750, bounds.topRight.lng, 0.001, "Right top longitude assert")
    }
}

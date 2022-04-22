package com.moneysearch.services

import com.moneysearch.repositories.Location
import kotlin.math.cos
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// number of km per degree = ~111km (111.32 in google maps, but range varies
// between 110.567km at the equator and 111.699km at the poles)
// 1km in degree = 1 / 111.32km = 0.0089
// 1m in degree = 0.0089 / 1000 = 0.0000089
const val DEGREE_IN_METER = 0.0000089

//TODO this component should be improved, remove magic numbers, describe calculations
@Component
class CoordinatesCalculator {

    private val log = LoggerFactory.getLogger(CoordinatesCalculator::class.java)

    //distance has to be in meters
    fun getBounds(location: Location, distance: Long): Bounds {
        val bottomLeftLatitude = findLatitude(location.latitude, -distance)
        val bottomLeftLongitude = findLongitude(location.longitude, bottomLeftLatitude, -distance)
        val topRightLatitude = findLatitude(location.latitude, distance)
        val topRightLongitude = findLongitude(location.longitude, topRightLatitude, distance)
        val bounds = Bounds(
            bottomLeft = Bound(
                lat = bottomLeftLatitude,
                lng = bottomLeftLongitude
            ),
            topRight = Bound(
                lat = topRightLatitude,
                lng = topRightLongitude
            )
        )
        log.info(
            """
            Get bounds: locationLatitude=${location.latitude}, locationLongitude=${location.longitude},
            distance=$distance, bounds=$bounds
            """.trimIndent().replace("\n", " ")
        )
        return bounds
    }

    private fun findLatitude(oldLatitude: Double, distance: Long): Double =
        oldLatitude + distance * DEGREE_IN_METER

    private fun findLongitude(oldLongitude: Double, latitude: Double, distance: Long): Double =
        oldLongitude + distance * DEGREE_IN_METER / cos(latitude * 0.018)
}

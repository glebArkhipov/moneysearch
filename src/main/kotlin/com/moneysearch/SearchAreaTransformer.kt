package com.moneysearch

import com.moneysearch.SearchAreaType.CUSTOM
import com.moneysearch.SearchAreaType.VASKA
import com.moneysearch.SearchAreaType.WHOLE_SPB
import org.springframework.stereotype.Component

@Component
class SearchAreaTransformer(
    private val calculator: CoordinatesCalculator
) {
    fun searchAreaToBounds(searchArea: SearchArea): Bounds {
        return if (searchArea.type == CUSTOM) {
            calculator.getBounds(searchArea.location!!, searchArea.distanceFromLocation!!)
        } else {
            searchAreasToBoundsType[searchArea.type]!!
        }
    }
}

val searchAreasToBoundsType: Map<SearchAreaType, Bounds> = mapOf(
    WHOLE_SPB to Bounds(
        bottomLeft = Bound(
            lat = 59.87594110877944,
            lng = 30.189810644085755
        ),
        topRight = Bound(
            lat = 60.03428796417976,
            lng = 30.524737888104317
        )
    ),
    VASKA  to Bounds(
        bottomLeft = Bound(
            lat = 59.92063510877944,
            lng = 30.198010644085755
        ),
        topRight = Bound(
            lat = 59.96003996417976,
            lng = 30.282038888104317
        )
    )
)

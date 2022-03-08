package com.moneysearch

import com.moneysearch.SearchArea.VASKA
import com.moneysearch.SearchArea.WHOLE_SPB
import org.springframework.stereotype.Component

@Component
class BankPointsFinder(
    private val bankAdapter: BankApiAdapter
) {
    fun find(currencies: Set<String>, bounds: Bounds): List<BankPoint> =
        bankAdapter.findBankPoints(currencies, bounds)

    fun find(currencies: Set<String>, searchArea: SearchArea): List<BankPoint> =
        bankAdapter.findBankPoints(currencies, searchAreasToBounds[searchArea]!!)
}

data class BankPoint(
    val id: String,
    val address: String,
    val currenciesToAmount: Map<String, Long>
)

data class Bounds(
    val bottomLeft: Bound,
    val topRight: Bound
)

data class Bound(
    val lat: Double,
    val lng: Double
)

val searchAreasToBounds: Map<SearchArea, Bounds> = mapOf(
    VASKA to Bounds(
        bottomLeft = Bound(
            lat = 59.87594110877944,
            lng = 30.189810644085755
        ),
        topRight = Bound(
            lat = 60.03428796417976,
            lng = 30.524737888104317
        )
    ),
    WHOLE_SPB to Bounds(
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

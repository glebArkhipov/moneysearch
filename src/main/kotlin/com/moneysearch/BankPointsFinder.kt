package com.moneysearch

import org.springframework.stereotype.Component

@Component
class BankPointsFinder(
    private val bankAdapter: BankApiAdapter
) {
    fun find(currencies: Set<Currency>, bounds: Bounds): List<BankPoint> =
        bankAdapter.findBankPoints(currencies, bounds)
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

package com.moneysearch

import com.fasterxml.jackson.databind.json.JsonMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

val jsonMapper = JsonMapper()

@Component
class BankPointsFinder(
    private val bankAdapter: BankApiAdapter
) {
    private val log = LoggerFactory.getLogger(BankPointsFinder::class.java)

    fun find(currencies: Set<Currency>, bounds: Bounds): List<BankPoint> {
        val bankPoints = bankAdapter.findBankPoints(currencies, bounds)
        log.info("Bank points found: number=${bankPoints.size} bankPoints=${bankPoints}")
        return bankPoints
    }
}

data class BankPoint(
    val id: String,
    val address: String,
    val currenciesToAmount: Map<Currency, Long>
) {
    override fun toString(): String = jsonMapper.writeValueAsString(this)
}

data class Bounds(
    val bottomLeft: Bound,
    val topRight: Bound
)

data class Bound(
    val lat: Double,
    val lng: Double
)

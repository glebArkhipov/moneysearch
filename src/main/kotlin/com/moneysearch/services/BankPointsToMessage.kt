package com.moneysearch.services

import com.moneysearch.Currency
import org.springframework.stereotype.Component

@Component
class BankPointsToMessage {
    fun bankPointsToMessage(bankPoints: List<BankPoint>): String {
        return if (bankPoints.isEmpty()) {
            "Bank points are not found"
        } else {
            bankPoints
                .map { "Address: ${it.address}, currencies: ${currenciesToMessage(it.currenciesToAmount)}" }
                .joinToString("\n")
        }
    }

    private fun currenciesToMessage(currencyToAmount: Map<Currency, Long>) =
        currencyToAmount.entries.map { "${it.key} - ${it.value}" }.joinToString(", ")
}

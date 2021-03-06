package com.moneysearch

import com.moneysearch.repositories.Currency.EUR
import com.moneysearch.repositories.Currency.RUB
import com.moneysearch.services.BankPoint
import com.moneysearch.services.BankPointsToMessage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BankPointsToMessageTest {
    @Test
    fun notEmptyBankPoints() {
        val bankPoints = listOf(
            BankPoint("1", "address 1", mapOf(RUB to 100)),
            BankPoint("2", "address 2", mapOf(RUB to 150, EUR to 200)),
        )
        val bankPointsToMessage = BankPointsToMessage().bankPointsToMessage(bankPoints)
        val expectedResult = """
            Address: address 1, currencies: RUB - 100
            Address: address 2, currencies: RUB - 150, EUR - 200
        """.trimIndent()
        Assertions.assertEquals(expectedResult, bankPointsToMessage)
    }
}

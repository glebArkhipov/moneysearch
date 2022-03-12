package com.moneysearch

import java.lang.IllegalArgumentException
import org.springframework.stereotype.Component

@Component
class CurrencyMessageParser {
    fun parseCurrencyMessage(message: String): CurrencyParsingResult {
        val actionAndCurrency = message.split(" ")
        if (actionAndCurrency.size > 2) {
            return CurrencyParsingFailedResult("Too many arguments")
        }
        val actionAsString = actionAndCurrency.first()
        val action = Action.getActionByString(actionAsString)
        val currencyAsString = actionAndCurrency.last()
        val currency = Currency.getCurrencyByString(currencyAsString)
        return if (action == null || currency == null || actionAndCurrency.size > 2) {
            CurrencyParsingFailedResult("Bad arguments $actionAsString")
        } else {
            CurrencyParsingSuccessfulResult(currency, action)
        }
    }
}

interface CurrencyParsingResult

data class CurrencyParsingFailedResult(
    val errorMessage: String
): CurrencyParsingResult

data class CurrencyParsingSuccessfulResult(
    val currency: Currency,
    val action: Action
): CurrencyParsingResult

enum class Action(
    val string: String
) {
    ADD("Add"), REMOVE("Remove");
    companion object {
        fun getActionByString(string: String) = values().find { it.string == string }
    }
}

enum class Currency {
    EUR, USD, RUB;

    companion object {
        fun getCurrencyByString(string: String) = try {
            valueOf(string)
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
}

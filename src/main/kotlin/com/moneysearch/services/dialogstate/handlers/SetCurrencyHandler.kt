package com.moneysearch.services.dialogstate.handlers

import com.moneysearch.repositories.Currency
import com.moneysearch.repositories.User
import com.moneysearch.services.Action.ADD
import com.moneysearch.services.Action.REMOVE
import com.moneysearch.services.CurrencyMessageParser
import com.moneysearch.services.CurrencyParsingFailedResult
import com.moneysearch.services.CurrencyParsingSuccessfulResult
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogState.MAIN_MENU
import com.moneysearch.services.dialogstate.DialogStateHandler
import com.moneysearch.services.dialogstate.HandleResult
import com.moneysearch.services.dialogstate.Request
import com.moneysearch.services.dialogstate.SuggestedCommand
import com.moneysearch.services.dialogstate.SuggestedCommandDTO
import com.moneysearch.services.dialogstate.Suggestion
import com.moneysearch.services.dialogstate.toDto
import org.springframework.stereotype.Component

@Component
class SetCurrencyHandler(
    private val userService: UserService,
    private val currencyMessageParser: CurrencyMessageParser
) : DialogStateHandler {

    private val suggestedStaticCommands: List<SuggestedCommand> = listOf(
        SuggestedCommand(
            commandTxt = "Back",
            action = { _, _ -> HandleResult(nextDialogState = MAIN_MENU) }
        )
    )

    override fun handleRequest(request: Request, user: User): HandleResult {
        val suggestedCommand = suggestedStaticCommands.find { it.commandTxt == request.message }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(request, user)
        } else if (request.message != null) {
            when (val parsingResult = currencyMessageParser.parseCurrencyMessage(request.message)) {
                is CurrencyParsingFailedResult -> handleUnknownCommand()
                is CurrencyParsingSuccessfulResult -> changeCurrency(parsingResult, user)
            }
        } else {
            handleUnknownCommand()
        }
    }

    fun changeCurrency(parsingResult: CurrencyParsingSuccessfulResult, user: User): HandleResult {
        val (currency, action) = parsingResult
        when (action) {
            ADD -> userService.addCurrency(user, currency)
            REMOVE -> userService.removeCurrency(user, currency)
        }
        return HandleResult(txtResponse = "Currencies are ${user.currencies}")
    }

    override fun suggestionForUser(user: User): Suggestion {
        val currencyChangeSuggestions = getCurrencyChangeSuggestions(user)
        val suggestedCommandDTOS =
            currencyChangeSuggestions.map { SuggestedCommandDTO(it) } +
                suggestedStaticCommands.map { it.toDto() }
        return Suggestion("Add or remove currencies", suggestedCommandDTOS)
    }

    private fun getCurrencyChangeSuggestions(user: User): List<String> {
        val currencies = user.currencies
        val allCurrencies = Currency.values().toSet()
        val currenciesToRemove = currencies.intersect(allCurrencies)
        val currenciesToAdd = allCurrencies.minus(currencies)
        val removeSuggestions = currenciesToRemove.map { "${REMOVE.string} $it" }.toList()
        val addSuggestions = currenciesToAdd.map { "${ADD.string} $it" }.toList()
        return removeSuggestions + addSuggestions
    }
}

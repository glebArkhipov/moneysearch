package com.moneysearch.services.dialogstate.handlers

import com.moneysearch.Action.ADD
import com.moneysearch.Action.REMOVE
import com.moneysearch.Currency
import com.moneysearch.CurrencyMessageParser
import com.moneysearch.CurrencyParsingFailedResult
import com.moneysearch.CurrencyParsingSuccessfulResult
import com.moneysearch.repositories.User
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogState.MAIN_MENU
import com.moneysearch.services.dialogstate.DialogStateHandler
import com.moneysearch.services.dialogstate.HandleResult
import com.moneysearch.services.dialogstate.SuggestedCommand
import com.moneysearch.services.dialogstate.SuggestedCommandDTO
import com.moneysearch.services.dialogstate.Suggestion
import com.moneysearch.services.dialogstate.toDto
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

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

    override fun handleUpdate(update: Update, user: User): HandleResult {
        val messageTxt = update.message.text
        val suggestedCommand = suggestedStaticCommands.find { it.commandTxt == messageTxt }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(update, user)
        } else {
            val parsingResult = currencyMessageParser.parseCurrencyMessage(messageTxt)
            when (parsingResult) {
                is CurrencyParsingFailedResult -> handleUnknownCommand()
                is CurrencyParsingSuccessfulResult -> changeCurrency(parsingResult, user)
            }
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

    override fun suggestionForUser(update: Update, user: User): Suggestion {
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

package com.moneysearch.dialogstate.handler

import com.moneysearch.Action.ADD
import com.moneysearch.Action.REMOVE
import com.moneysearch.Currency
import com.moneysearch.CurrencyMessageParser
import com.moneysearch.CurrencyParsingFailedResult
import com.moneysearch.CurrencyParsingSuccessfulResult
import com.moneysearch.User
import com.moneysearch.UserService
import com.moneysearch.dialogstate.handler.DialogState.MAIN_MENU
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class SetCurrencyHandler(
    private val userService: UserService,
    private val currencyMessageParser: CurrencyMessageParser
) : DialogStateHandler {

    private val suggestedCommands: List<SuggestedCommand> = listOf(
        SuggestedCommand(
            commandTxt = "Back",
            action = { _, _ -> HandleResult(nextDialogState = MAIN_MENU) }
        )
    )

    override fun handleUpdate(update: Update, user: User): HandleResult {
        val messageTxt = update.message.text
        val suggestedCommand = suggestedCommands.find { it.commandTxt == messageTxt }
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

    override fun defaultDialogStateResponse(update: Update, user: User): SendMessage {
        val currencies = user.currencies
        val allCurrencies = Currency.values().toSet()
        val currenciesToRemove = currencies.intersect(allCurrencies)
        val currenciesToAdd = allCurrencies.minus(currencies)
        val removeButtons = currenciesToRemove.map { "${REMOVE.string} $it" }.toList()
        val addButtons = currenciesToAdd.map { "${ADD.string} $it" }.toList()
        val buttons = removeButtons + addButtons + suggestedCommands.map { it.commandTxt }.toList()
        val rows = buttons.map { KeyboardButton(it) }.chunked(2).map { KeyboardRow(it) }
        val keyboardMarkup = ReplyKeyboardMarkup(rows)
        keyboardMarkup.resizeKeyboard = true
        val message = SendMessage(update.message.chatId.toString(), "Add or remove currencies")
        message.replyMarkup = keyboardMarkup

        return message
    }
}

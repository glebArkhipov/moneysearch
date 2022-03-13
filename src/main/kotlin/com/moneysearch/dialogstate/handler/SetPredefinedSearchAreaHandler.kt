package com.moneysearch.dialogstate.handler

import com.moneysearch.SearchAreaType
import com.moneysearch.SearchAreaType.VASKA
import com.moneysearch.SearchAreaType.WHOLE_SPB
import com.moneysearch.User
import com.moneysearch.UserService
import com.moneysearch.dialogstate.handler.DialogState.MAIN_MENU
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class SetPredefinedSearchAreaHandler(
    private val userService: UserService
): DialogStateHandler {
    override fun handleUpdate(update: Update, user: User): HandleResult {
        val message = update.message
        return when (message.text) {
            "Vaska" -> setPredefinedSearchArea(update, user, VASKA)
            "Whole spb" -> setPredefinedSearchArea(update, user, WHOLE_SPB)
            "Back" -> HandleResult(nextDialogState = MAIN_MENU)
            else -> handleUnknownCommand()
        }
    }

    override fun defaultDialogStateResponse(update: Update, user: User): SendMessage {
        val row = KeyboardRow()
        row.add("Vaska")
        row.add("Whole spb")
        row.add("Back")
        val keyboardMarkup = ReplyKeyboardMarkup(listOf(row))
        keyboardMarkup.resizeKeyboard
        val message = SendMessage(update.message.chatId.toString(), "Choose predefined location")
        message.replyMarkup = keyboardMarkup

        return message
    }

    fun setPredefinedSearchArea(update: Update, user: User, searchAreaType: SearchAreaType): HandleResult {
        userService.setPredefinedSearchArea(user, searchAreaType)
        return HandleResult(
            txtResponse = "$searchAreaType is set as search area",
            nextDialogState = MAIN_MENU
        )
    }
}

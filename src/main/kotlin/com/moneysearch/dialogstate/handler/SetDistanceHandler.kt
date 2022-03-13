package com.moneysearch.dialogstate.handler

import com.moneysearch.User
import com.moneysearch.UserService
import com.moneysearch.dialogstate.handler.DialogState.SET_CUSTOM_SEARCH_AREA
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class SetDistanceHandler(
    private val userService: UserService
): DialogStateHandler {
    override fun handleUpdate(update: Update, user: User): HandleResult {
        val text = update.message.text
        if (text == "Back") {
            return HandleResult(nextDialogState = SET_CUSTOM_SEARCH_AREA)
        }
        return try {
            val distance = text.toLong()
            userService.setDistanceFromLocation(user, distance)
            HandleResult(
                txtResponse = "Distance is set",
                nextDialogState = SET_CUSTOM_SEARCH_AREA
            )
        } catch (ex: NumberFormatException) {
            HandleResult("Distance should be a number")
        }
    }

    override fun defaultDialogStateResponse(update: Update, user: User): SendMessage {
        val row = KeyboardRow()
        row.add("Back")
        val keyboardMarkup = ReplyKeyboardMarkup(listOf(row))
        keyboardMarkup.resizeKeyboard
        val message = SendMessage(update.message.chatId.toString(), "Please, provide a number")
        message.replyMarkup = keyboardMarkup

        return message
    }
}

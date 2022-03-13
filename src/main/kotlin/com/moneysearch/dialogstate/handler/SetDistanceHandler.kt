package com.moneysearch.dialogstate.handler

import com.moneysearch.User
import com.moneysearch.UserService
import com.moneysearch.dialogstate.handler.DialogState.SET_CUSTOM_SEARCH_AREA
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove

@Component
class SetDistanceHandler(
    private val userService: UserService
): DialogStateHandler {
    override fun handleUpdate(update: Update, user: User): HandleResult {
        return try {
            val distance = update.message.text.toLong()
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
        val message = SendMessage(update.message.chatId.toString(), "Please, provide a number")
        message.replyMarkup = ReplyKeyboardRemove(true)
        return message
    }
}

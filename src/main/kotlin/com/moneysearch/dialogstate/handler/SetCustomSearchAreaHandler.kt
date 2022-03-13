package com.moneysearch.dialogstate.handler

import com.moneysearch.Location
import com.moneysearch.User
import com.moneysearch.UserService
import com.moneysearch.dialogstate.handler.DialogState.MAIN_MENU
import com.moneysearch.dialogstate.handler.DialogState.SET_DISTANCE
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class SetCustomSearchAreaHandler(
    private val userService: UserService
): DialogStateHandler {
    override fun handleUpdate(update: Update, user: User): HandleResult {
        val message = update.message
        return if (message.hasLocation()) {
            setLocation(update, user)
        } else if (message.text == "Back") {
            HandleResult(nextDialogState = MAIN_MENU)
        } else if (message.text == "Set distance from location (meters)") {
            HandleResult(nextDialogState = SET_DISTANCE)
        } else {
            handleUnknownCommand()
        }
    }

    override fun defaultDialogStateResponse(update: Update, user: User): SendMessage {
        val row = KeyboardRow()
        val currentLocationButton = KeyboardButton("Set current location")
        currentLocationButton.requestLocation = true
        row.add(currentLocationButton)
        row.add("Set distance from location (meters)")
        row.add("Back")
        val keyboardMarkup = ReplyKeyboardMarkup(listOf(row))
        keyboardMarkup.resizeKeyboard
        val message = SendMessage(update.message.chatId.toString(), "You could set location by map")
        message.replyMarkup = keyboardMarkup

        return message
    }

    fun setLocation(update: Update, user: User): HandleResult {
        val location = Location(
            latitude = update.message.location.latitude,
            longitude = update.message.location.longitude
        )
        userService.setCustomLocation(user, location)
        return HandleResult("Custom location $location is set")
    }
}

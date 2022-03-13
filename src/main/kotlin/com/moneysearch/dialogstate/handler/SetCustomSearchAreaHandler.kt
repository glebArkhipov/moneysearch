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

    private val suggestedCommands: List<SuggestedCommand> = listOf(
        SuggestedCommand(
            commandTxt = "Set current location",
            action = { update, user -> setLocation(update, user) },
            requestCurrentLocation = true
        ),
        SuggestedCommand(
            commandTxt = "Set distance from location (meters)",
            action = { _, _ -> HandleResult(nextDialogState = SET_DISTANCE) },
        ),
        SuggestedCommand(
            commandTxt = "Back",
            action = { _, _ -> HandleResult(nextDialogState = MAIN_MENU) }
        )
    )

    override fun handleUpdate(update: Update, user: User): HandleResult {
        val message = update.message
        val suggestedCommand = suggestedCommands.find { it.commandTxt == message.text }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(update, user)
        } else if (message.hasLocation()) {
            setLocation(update, user)
        } else {
            handleUnknownCommand()
        }
    }

    override fun defaultDialogStateResponse(update: Update, user: User): SendMessage {
        val rows = suggestedCommands
            .map {
                KeyboardButton.builder()
                    .text(it.commandTxt)
                    .requestLocation(it.requestCurrentLocation)
                    .build()
            }
            .chunked(2)
            .map { KeyboardRow(it) }
        val keyboardMarkup = ReplyKeyboardMarkup(rows)
        keyboardMarkup.resizeKeyboard = true
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

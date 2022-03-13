package com.moneysearch.dialogstate.handler

import com.moneysearch.BankPointsFinder
import com.moneysearch.BankPointsToMessage
import com.moneysearch.SearchArea
import com.moneysearch.SearchAreaTransformer
import com.moneysearch.SearchAreaType.CUSTOM
import com.moneysearch.User
import com.moneysearch.UserService
import com.moneysearch.dialogstate.handler.DialogState.SET_CURRENCY
import com.moneysearch.dialogstate.handler.DialogState.SET_CUSTOM_SEARCH_AREA
import com.moneysearch.dialogstate.handler.DialogState.SET_PREDEFINED_SEARCH_AREA
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class MainMenuHandler(
    private val bankPointsFinder: BankPointsFinder,
    private val searchAreaTransformer: SearchAreaTransformer,
    private val userService: UserService,
    private val bankPointsToMessage: BankPointsToMessage
) : DialogStateHandler {
    override fun handleUpdate(update: Update, user: User): HandleResult {
        val message = update.message
        return when (message.text) {
            "User info" -> userInfo(user)
            "Get bank points with money" -> bankPointsWithMoney(user)
            "Turn notification on" -> turnNotificationOn(update, user)
            "Turn notification off" -> turnNotificationOff(update, user)
            "Set custom search area" -> HandleResult(nextDialogState = SET_CUSTOM_SEARCH_AREA)
            "Choose predefined location" -> HandleResult(nextDialogState = SET_PREDEFINED_SEARCH_AREA)
            "Add or remove currencies" -> HandleResult(nextDialogState = SET_CURRENCY)
            else -> handleUnknownCommand()
        }
    }

    override fun defaultDialogStateResponse(
        update: Update,
        user: User
    ): SendMessage {
        val buttons = listOf(
            "User info",
            "Get bank points with money",
            if (user.notificationsTurnOn) "Turn notification off" else "Turn notification on",
            "Set custom search area",
            "Choose predefined location",
            "Add or remove currencies"
        )
        val rows = buttons.map { KeyboardButton(it) }.chunked(2).map { KeyboardRow(it) }
        val keyboardMarkup = ReplyKeyboardMarkup(rows)
        keyboardMarkup.resizeKeyboard = true
        val txtMessage = "Main menu"
        val message = SendMessage(update.message.chatId.toString(), txtMessage)
        message.replyMarkup = keyboardMarkup
        return message
    }

    private fun userInfo(user: User) =
        HandleResult(
            """
            Currencies - ${user.currencies}
            ${searchAreaInfo(user.searchArea)}
            Notification - ${if (user.notificationsTurnOn) "on" else "off"}
            """.lines().joinToString(transform = String::trim, separator = "\n")
        )

    private fun searchAreaInfo(searchArea: SearchArea) = if (searchArea.type == CUSTOM) {
        """
        Custom location is set ${searchArea.location}
        Distance - ${searchArea.distanceFromLocation}
        """.trim()
    } else {
        "Search area - ${searchArea.type}"
    }

    private fun bankPointsWithMoney(user: User): HandleResult {
        val bounds = searchAreaTransformer.searchAreaToBounds(user.searchArea)
        val bankPoints = bankPointsFinder.find(user.currencies, bounds)
        return if (bankPoints.isNotEmpty()) {
            val message = bankPointsToMessage.bankPointsToMessage(bankPoints)
            HandleResult(message)
        } else {
            HandleResult("No bank points are found")
        }
    }

    fun turnNotificationOn(update: Update, user: User): HandleResult {
        userService.turnNotificationOn(user)
        val searchAreaMessage = searchAreaInfo(user.searchArea)
        val message = """
            Notification will be send every ~45 seconds
            Currencies ${user.currencies}
            $searchAreaMessage
        """.lines().joinToString(transform = String::trim, separator = "\n")
        return HandleResult(message)
    }

    fun turnNotificationOff(update: Update, user: User): HandleResult {
        userService.turnNotificationOff(user)
        return HandleResult("Notifications are turned off")
    }
}


package com.moneysearch.dialogstate.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.moneysearch.BankPointsFinder
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class MainMenuHandler(
    private val bankFinder: BankPointsFinder,
    private val searchAreaTransformer: SearchAreaTransformer,
    private val userService: UserService,
    private val jsonObjectMapper: ObjectMapper
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
        val row = KeyboardRow()
        row.addAll(buttons)
        val keyboardMarkup = ReplyKeyboardMarkup(listOf(row))
        keyboardMarkup.resizeKeyboard
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
        val banks = bankFinder.find(user.currencies, bounds)
        return if (banks.isNotEmpty()) {
            val banksAsString = jsonObjectMapper.writeValueAsString(banks)
            HandleResult(banksAsString)
        } else {
            HandleResult("No banks are found")
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


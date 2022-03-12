package com.moneysearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.moneysearch.SearchAreaType.CUSTOM
import com.moneysearch.SearchAreaType.VASKA
import com.moneysearch.SearchAreaType.WHOLE_SPB
import com.moneysearch.Step.MAIN_MENU
import com.moneysearch.Step.SET_CUSTOM_SEARCH_AREA_LOCATION
import com.moneysearch.Step.SET_DISTANCE
import com.moneysearch.Step.SET_PREDEFINED_SEARCH_AREA
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class Bot(
    private val bankFinder: BankPointsFinder,
    private val searchAreaTransformer: SearchAreaTransformer,
    private val authorityService: AuthorityService,
    private val userService: UserService,
    private val jsonObjectMapper: ObjectMapper,
    @Value("\${bot.token}")
    private val botToken: String,
    @Value("\${bot.username}")
    private val botUsername: String,
) : TelegramLongPollingBot() {
    override fun getBotToken(): String = botToken

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        if (!authorityService.checkAuthority(update)) {
            sendNotification(update.message.chatId, "You are not allowed to use this service")
            return
        }
        if (!update.hasMessage()) {
            println("update has no message")
            return
        }
        val userTelegramId = update.message.from.id
        val user = userService.findUserByTelegramId(userTelegramId)
        when (user.step) {
            MAIN_MENU -> handleMainMenuCommands(update, user)
            SET_PREDEFINED_SEARCH_AREA -> handleSetPredefinedSearchAreaCommands(update, user)
            SET_CUSTOM_SEARCH_AREA_LOCATION -> handleSetCustomCustomSearchAreaCommands(update, user)
            SET_DISTANCE -> handleSetDistanceCommands(update, user)
        }
    }

    fun handleMainMenuCommands(update: Update, user: User) {
        val message = update.message
        when (message.text) {
            "User info" -> sendUserInfo(update, user)
            "Get bank points with money" -> sendBankPointsWithMoney(update, user)
            "Turn notification on" -> turnNotificationOn(update, user)
            "Set custom search area" -> customSearchAreaKeyboard(update, user)
            "Choose predefined location" -> predefinedSearchAreasKeyboard(update, user)
            else -> sendNotification(update.message.chatId, "Unknown command")
        }
    }

    fun handleSetCustomCustomSearchAreaCommands(update: Update, user: User) {
        val message = update.message
        if (message.hasLocation()) {
            setLocation(update, user)
        } else if (message.text == "Back") {
            returnToMainMenu(update, user)
        } else if (message.text == "Set distance from location (meters)") {
            userService.setStep(user, SET_DISTANCE)
            removeKeyBoardAndSendNotification(update.message.chatId, "Please, enter a number")
        } else {
            sendNotification(update.message.chatId, "Unknown command")
        }
    }

    fun handleSetPredefinedSearchAreaCommands(update: Update, user: User) {
        val message = update.message
        when (message.text) {
            "Vaska" -> setSearchArea(update, user, VASKA)
            "Whole spb" -> setSearchArea(update, user, WHOLE_SPB)
            "Back" -> returnToMainMenu(update, user)
            else -> sendNotification(update.message.chatId, "Unknown command")
        }
    }

    fun handleSetDistanceCommands(update: Update, user: User) {
        setDistance(update, user)
        customSearchAreaKeyboard(update, user)
    }

    fun sendUserInfo(update: Update, user: User) {
        val message =
            """
            Currencies - ${user.currencies}
            ${searchAreaInfo(user.searchArea)}
            Notification - ${if (user.notificationsTurnOn) "on" else "off"}
            Current step - ${user.step}
            """.lines().joinToString(transform = String::trim, separator = "\n")
        sendNotification(update.message.chatId, message)
    }

    fun searchAreaInfo(searchArea: SearchArea) = if (searchArea.type == CUSTOM) {
        """
        Custom location is set ${searchArea.location}
        Distance - ${searchArea.distanceFromLocation}
        """.trim()
    } else {
        "Search area - ${searchArea.type}"
    }

    fun setLocation(update: Update, user: User) {
        val location = Location(
            latitude = update.message.location.latitude,
            longitude = update.message.location.longitude
        )
        userService.setCustomLocation(user, location)
        sendNotification(update.message.chatId, "Custom location is set")
        returnToMainMenu(update, user)
    }

    fun setSearchArea(update: Update, user: User, searchAreaType: SearchAreaType) {
        userService.setPredefinedSearchArea(user, searchAreaType)
        sendNotification(update.message.chatId, "$searchAreaType is set as search area")
        returnToMainMenu(update, user)
    }

    fun setDistance(update: Update, user: User) {
        try {
            val distance = update.message.text.toLong()
            userService.setDistanceFromLocation(user, distance)
            sendNotification(update.message.chatId, "Distance is set")
        } catch (ex: NumberFormatException) {
            sendNotification(update.message.chatId, "Distance should be a number")
        }
    }

    fun returnToMainMenu(update: Update, user: User) {
        userService.setStep(user, MAIN_MENU)
        val message = SendMessage(update.message.chatId.toString(), "Main menu")
        message.replyMarkup = ReplyKeyboardRemove(true)
        execute(message)
    }

    fun sendBankPointsWithMoney(update: Update, user: User) {
        val bounds = searchAreaTransformer.searchAreaToBounds(user.searchArea)
        val banks = bankFinder.find(user.currencies, bounds)
        sendNotificationAboutBanks(update.message.chatId, banks, true)
    }

    fun turnNotificationOn(update: Update, user: User) {
        userService.turnNotificationOn(user)
        val searchAreaMessage = searchAreaInfo(user.searchArea)
        val message = """
            Notification will be send every ~45 seconds
            Currencies ${user.currencies}
            $searchAreaMessage
        """.lines().joinToString(transform = String::trim, separator = "\n")
        sendNotification(update.message.chatId, message)
    }

    fun sendNotificationAboutBanks(
        chatId: Long,
        banks: List<BankPoint>,
        notifyWhenNoBanks: Boolean = false
    ) {
        if (banks.isNotEmpty()) {
            val banksAsString = jsonObjectMapper.writeValueAsString(banks)
            sendNotification(chatId, banksAsString)
        } else if (notifyWhenNoBanks) {
            sendNotification(chatId, "No banks are found")
        }
    }

    private fun removeKeyBoardAndSendNotification(chatId: Long, responseText: String) {
        val message = SendMessage(chatId.toString(), responseText)
        message.replyMarkup = ReplyKeyboardRemove(true)
        execute(message)
    }

    private fun sendNotification(chatId: Long, responseText: String) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        execute(responseMessage)
    }

    private fun customSearchAreaKeyboard(update: Update, user: User) {
        userService.setStep(user, SET_CUSTOM_SEARCH_AREA_LOCATION)
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

        execute(message)
    }

    private fun predefinedSearchAreasKeyboard(update: Update, user: User) {
        userService.setStep(user, SET_PREDEFINED_SEARCH_AREA)
        val row = KeyboardRow()
        row.add("Vaska")
        row.add("Whole spb")
        row.add("Back")
        val keyboardMarkup = ReplyKeyboardMarkup(listOf(row))
        keyboardMarkup.resizeKeyboard
        val message = SendMessage(update.message.chatId.toString(), "Predefined location:")
        message.replyMarkup = keyboardMarkup

        execute(message)
    }
}

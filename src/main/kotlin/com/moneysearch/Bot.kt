package com.moneysearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.moneysearch.SearchAreaType.CUSTOM
import com.moneysearch.SearchAreaType.VASKA
import com.moneysearch.SearchAreaType.WHOLE_SPB
import com.moneysearch.Step.MAIN_MENU
import com.moneysearch.Step.SET_CURRENCY
import com.moneysearch.Step.SET_CUSTOM_SEARCH_AREA
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
            SET_CUSTOM_SEARCH_AREA -> handleSetCustomCustomSearchAreaCommands(update, user)
            SET_DISTANCE -> handleSetDistanceCommands(update, user)
            SET_CURRENCY -> handleSetCurrencyCommands(update, user)
        }
        returnStepDefaultResponse(update, user)
    }

    fun handleMainMenuCommands(update: Update, user: User) {
        val message = update.message
        when (message.text) {
            "User info" -> sendUserInfo(update, user)
            "Get bank points with money" -> sendBankPointsWithMoney(update, user)
            "Turn notification on" -> turnNotificationOn(update, user)
            "Turn notification off" -> turnNotificationOff(update, user)
            "Set custom search area" -> userService.setStep(user, SET_CUSTOM_SEARCH_AREA)
            "Choose predefined location" -> userService.setStep(user, SET_PREDEFINED_SEARCH_AREA)
            "Add or remove currencies" -> userService.setStep(user, SET_CURRENCY)
            else -> handleUnknownCommand(update, user)
        }
    }

    fun handleSetCustomCustomSearchAreaCommands(update: Update, user: User) {
        val message = update.message
        if (message.hasLocation()) {
            setLocation(update, user)
        } else if (message.text == "Back") {
            userService.setStep(user, MAIN_MENU)
        } else if (message.text == "Set distance from location (meters)") {
            userService.setStep(user, SET_DISTANCE)
        } else {
            handleUnknownCommand(update, user)
        }
    }

    fun handleSetPredefinedSearchAreaCommands(update: Update, user: User) {
        val message = update.message
        when (message.text) {
            "Vaska" -> setPredefinedSearchArea(update, user, VASKA)
            "Whole spb" -> setPredefinedSearchArea(update, user, WHOLE_SPB)
            "Back" -> userService.setStep(user, MAIN_MENU)
            else -> handleUnknownCommand(update, user)
        }
    }

    fun handleSetDistanceCommands(update: Update, user: User) {
        setDistance(update, user)
        userService.setStep(user, SET_CUSTOM_SEARCH_AREA)
    }

    fun handleSetCurrencyCommands(update: Update, user: User) {
        val text = update.message.text
        if (text == "Back") {
            userService.setStep(user, MAIN_MENU)
            return
        }
        val actionAndCurrency = text.split(" ")
        val action = actionAndCurrency.first()
        val currency = actionAndCurrency.last()
        if (actionAndCurrency.size > 2) {
            handleUnknownCommand(update, user)
            return
        }
        if (!setOf("Add", "Remove").contains(action)) {
            handleUnknownCommand(update, user)
            return
        }
        if (!setOf("RUB", "USD", "EUR").contains(currency)) {
            handleUnknownCommand(update, user)
            return
        }
        if (action == "Add") {
            val currencies = user.currencies
            val newCurrencies = currencies.plus(currency)
            userService.setCurrencies(user, newCurrencies)
        }
        if (action == "Remove") {
            val currencies = user.currencies
            val newCurrencies = currencies.minus(currency)
            userService.setCurrencies(user, newCurrencies)
        }
    }

    fun handleUnknownCommand(update: Update, user: User) {
        sendNotification(update.message.chatId, "Unknown command")
    }

    fun returnStepDefaultResponse(update: Update, user: User) {
        when (user.step) {
            MAIN_MENU -> mainMenuKeyboard(update, user)
            SET_PREDEFINED_SEARCH_AREA -> predefinedSearchAreasKeyboard(update, user)
            SET_CUSTOM_SEARCH_AREA -> customSearchAreaKeyboard(update, user)
            SET_CURRENCY -> currencyKeyboard(update, user)
            else -> removeKeyBoardAndSendNotification(update.message.chatId, "Please, enter a number")
        }
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
    }

    fun setPredefinedSearchArea(update: Update, user: User, searchAreaType: SearchAreaType) {
        userService.setPredefinedSearchArea(user, searchAreaType)
        sendNotification(update.message.chatId, "$searchAreaType is set as search area")
        userService.setStep(user, MAIN_MENU)
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

    fun turnNotificationOff(update: Update, user: User) {
        userService.turnNotificationOff(user)
        sendNotification(update.message.chatId, "Notifications are turned off")
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

    @Suppress("SameParameterValue")
    private fun removeKeyBoardAndSendNotification(chatId: Long, responseText: String) {
        val message = SendMessage(chatId.toString(), responseText)
        message.replyMarkup = ReplyKeyboardRemove(true)
        execute(message)
    }

    private fun sendNotification(chatId: Long, responseText: String) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        execute(responseMessage)
    }

    @Suppress("SameParameterValue")
    private fun mainMenuKeyboard(update: Update, user: User) {
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
        val message = SendMessage(update.message.chatId.toString(), "Main menu")
        message.replyMarkup = keyboardMarkup

        execute(message)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun customSearchAreaKeyboard(update: Update, user: User) {
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

    @Suppress("UNUSED_PARAMETER")
    private fun predefinedSearchAreasKeyboard(update: Update, user: User) {
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

    fun currencyKeyboard(update: Update, user: User) {
        val currencies = user.currencies
        val allCurrencies = setOf("RUB", "USD", "EUR")
        val currenciesToRemove = currencies.intersect(allCurrencies)
        val currenciesToAdd = allCurrencies.minus(currencies)
        val removeButtons = currenciesToRemove.map { "Remove $it" }.toList()
        val addButtons = currenciesToAdd.map { "Add $it" }.toList()
        val row = KeyboardRow()
        row.addAll(addButtons)
        row.addAll(removeButtons)
        row.add("Back")
        val keyboardMarkup = ReplyKeyboardMarkup(listOf(row))
        keyboardMarkup.resizeKeyboard
        val message = SendMessage(update.message.chatId.toString(), "Add or remove currencies")
        message.replyMarkup = keyboardMarkup

        execute(message)
    }
}

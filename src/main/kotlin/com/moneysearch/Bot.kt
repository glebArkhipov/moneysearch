package com.moneysearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.moneysearch.SearchAreaType.CUSTOM
import com.moneysearch.SearchAreaType.VASKA
import com.moneysearch.SearchAreaType.WHOLE_SPB
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
        val message = update.message
        val userTelegramId = update.message.from.id
        val user = userService.findUserByTelegramId(userTelegramId)
        val lastCommand = user.lastCommand
        if (lastCommand == "Set custom search area") {
            if (message.hasLocation()) {
                setLocation(update, user)
            } else if (message.text == "Back") {
                handleBack(update, user)
            } else {
                sendNotification(update.message.chatId, "Unknown command")
            }
        } else if (lastCommand == "Set distance from location (meters)") {
            setDistance(update, user)
            handleBack(update, user)
        } else if (lastCommand == "Choose predefined location") {
            when (message.text) {
                "Vaska" -> setSearchArea(update, user, VASKA)
                "Whole spb" -> setSearchArea(update, user, WHOLE_SPB)
                "Back" -> handleBack(update, user)
                else -> sendNotification(update.message.chatId, "Unknown command")
            }
        } else if (message.hasText()) {
            when (message.text) {
                "User info" -> sendUserInfo(update, user)
                "Get bank points with money" -> sendBankPointsWithMoney(update, user)
                "Turn notification on" -> turnNotificationOn(update, user)
                "Set custom search area" -> customSearchAreaKeyboard(update, user)
                "Choose predefined location" -> definedSearchAreasKeyboard(update, user)
                "Set distance from location (meters)" -> {
                    userService.setLastCommand(user, "Set distance from location (meters)")
                    sendNotification(update.message.chatId, "Please, enter a number")
                }
            }
        }
    }

    fun sendUserInfo(update: Update, user: User) {
        val message =
            """
            Currencies - ${user.currencies}
            ${searchAreaInfo(user.searchArea)}
            Notification - ${if (user.notificationsTurnOn) "on" else "off"}
            Last command - ${user.lastCommand}
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
        sendNotification(update.message.chatId, "Location is set")
        handleBack(update, user)
    }

    fun setSearchArea(update: Update, user: User, searchAreaType: SearchAreaType) {
        userService.setPredefinedSearchArea(user, searchAreaType)
        sendNotification(update.message.chatId, "$searchAreaType is set as location area")
        handleBack(update, user)
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

    fun handleBack(update: Update, user: User) {
        userService.setLastCommand(user, null)
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

    private fun sendNotification(chatId: Long, responseText: String) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        execute(responseMessage)
    }

    private fun customSearchAreaKeyboard(update: Update, user: User) {
        userService.setLastCommand(user, "Set custom search area")
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

    private fun definedSearchAreasKeyboard(update: Update, user: User) {
        userService.setLastCommand(user, "Choose predefined location")
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

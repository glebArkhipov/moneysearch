package com.moneysearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.moneysearch.SearchArea.VASKA
import com.moneysearch.SearchArea.WHOLE_SPB
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class Bot(
    private val bankFinder: BankFinder,
    private val coordinatesCalculator: CoordinatesCalculator,
    private val userRepository: UserRepository,
    private val jsonObjectMapper: ObjectMapper,
    @Value("\${bot.token}")
    private val botToken: String,
    @Value("\${bot.username}")
    private val botUsername: String,
    @Value("\${auth.allowed.users}")
    private val allowedUsers: String,
) : TelegramLongPollingBot() {
    override fun getBotToken(): String = botToken

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        if (!checkAuthority(update)) {
            return
        }
        if (!update.hasMessage()) {
            println("update has no message")
            return
        }
        val message = update.message
        val chatId = message.chatId
        val userTelegramId = update.message.from.id
        if (message.hasText()) {
            when (message.text) {
                "ping" -> sendNotification(chatId, "pong")
                "check" -> handleCheck(chatId, userTelegramId)
                "notify" -> setNotification(chatId, userTelegramId)
            }
        } else if (message.hasLocation()) {
            handleLocation(update)
        }
    }

    private fun checkAuthority(update: Update): Boolean {
        val userName = update.message.from.userName
        val userTelegramId = update.message.from.id
        val findUserByTelegramId = userRepository.findUserByTelegramId(userTelegramId)
        if (findUserByTelegramId == null) {
            userRepository.save(User(userTelegramId, userName, emptySet()))
        }
        val allowedUsersSet = allowedUsers.split(",")
        val userAuthorized = allowedUsersSet.contains(userName)
        println("user $userName attempt to access service")
        if (!userAuthorized) {
            println("user $userName is not authorized")
            sendNotification(update.message.chatId, "You are not authorized")
        }
        return userAuthorized
    }

    fun handleLocation(update: Update) {
        val location = Location(
            latitude = update.message.location.latitude,
            longitude = update.message.location.longitude
        )
        val bounds = coordinatesCalculator.getBounds(location, 1000)
        val banks = bankFinder.findBanks(setOf("EUR", "USD"), bounds)
        sendNotificationAboutBanks(update.message.chatId, banks)
    }

    fun handleCheck(chatId: Long, userTelegramId: Long) {
        val banks = bankFinder.findBanks(setOf("EUR", "USD"), VASKA)
        sendNotificationAboutBanks(chatId, banks, true)
    }

    fun setNotification(chatId: Long, userTelegramId: Long) {
        val user = userRepository.findUserByTelegramId(userTelegramId)!!
        user.searchArea = WHOLE_SPB
        user.notificationsTurnOn = true
        user.currencies = setOf("EUR", "USD")
        userRepository.save(user)
        val message = """
            Notification will be send every ~45 seconds
            Currencies EUR and USD
            Location WHOLE_SPB
        """.trimIndent()
        sendNotification(chatId, message)
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
        responseMessage.enableMarkdown(true)
        execute(responseMessage)
    }
}

package com.moneysearch

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.math.roundToLong
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
        if (message.hasText()) {
            when (message.text) {
                "ping" -> sendNotification(chatId, "pong")
                "check" -> sendBanksWithEurAndUsdInWholeSpb(chatId)
                "notify" -> notifyRegularly(chatId)
            }
        } else if (message.hasLocation()) {
            val location = Location(
                latitude = update.message.location.latitude,
                longitude = update.message.location.longitude
            )
            val bounds = coordinatesCalculator.getBounds(location, 1000)
            sendBanksWithEurAndUsdInWholeSpb(chatId, bounds)
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

    fun notifyRegularly(chatId: Long) {
        Notifier(
            Bot(bankFinder, coordinatesCalculator, userRepository, jsonObjectMapper, botToken, botToken, allowedUsers),
            chatId
        ).start()
    }

    fun notifyAboutBanksWithEurAndUsdInWholeSpb(chatId: Long) {
        sendNotification(chatId, "Lets check for banks with eur and usd")
        while (true) {
            sendBanksWithEurAndUsdInWholeSpb(chatId)
            val randomPart = (Math.random() * 30000).roundToLong()
            val sleepFor = 30000 + randomPart
            println("Next check in $sleepFor")
            Thread.sleep(sleepFor)
        }
    }

    private fun sendBanksWithEurAndUsdInWholeSpb(chatId: Long, bounds: Bounds = WHOLE_SPB_BOUNDS) {
        val banks = bankFinder.findBanksWithCurrencies(
            setOf("EUR", "USD"), bounds
        )
        if (banks.isNotEmpty()) {
            val banksAsString = jsonObjectMapper.writeValueAsString(banks)
            sendNotification(chatId, banksAsString)
        }
    }

    private fun sendNotification(chatId: Long, responseText: String) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        responseMessage.enableMarkdown(true)
        execute(responseMessage)
    }
}

class Notifier(
    private val bot: Bot,
    private val chatId: Long
) : Thread() {
    override fun run() {
        bot.notifyAboutBanksWithEurAndUsdInWholeSpb(chatId)
    }
}

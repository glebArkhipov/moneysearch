package com.moneysearch

import com.moneysearch.repositories.User
import com.moneysearch.services.AuthCheckFailedResult
import com.moneysearch.services.AuthCheckSuccessfulResult
import com.moneysearch.services.AuthorityService
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogStateHandlerProvider
import com.moneysearch.services.dialogstate.Request
import com.moneysearch.services.dialogstate.Suggestion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Location
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Component
class Bot(
    private val authorityService: AuthorityService,
    private val userService: UserService,
    private val dialogStateHandlerProvider: DialogStateHandlerProvider,
    @Value("\${bot.token}")
    private val botToken: String,
    @Value("\${bot.username}")
    private val botUsername: String,
) : TelegramLongPollingBot() {

    private val log = LoggerFactory.getLogger(Bot::class.java)

    override fun getBotToken(): String = botToken

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        log.debug("update received: userName=${update.message.from.userName} message=${update.message.text}")
        when (val checkAuthorityResult = authorityService.checkAuthority(update)) {
            is AuthCheckFailedResult -> sendNotification(update.message.chatId, checkAuthorityResult.errorMessage)
            is AuthCheckSuccessfulResult -> {
                val user = checkAuthorityResult.user
                val initialDialogState = user.dialogState
                val handler = dialogStateHandlerProvider.getHandlerBy(initialDialogState)
                val (txtResponse, newDialogState) = handler.handleRequest(update.toRequest(), user)
                if (txtResponse != null) {
                    sendNotification(user, txtResponse)
                }
                if (newDialogState != null) {
                    userService.setDialogState(user, newDialogState)
                    log.debug(
                        "user dialog state changed: " +
                            "userName=${user.username} " +
                            "initialDialogState=${initialDialogState} " +
                            "newDialogState=${newDialogState}"
                    )
                }
                sendSuggestionNotification(update, user)
            }
        }
    }

    fun sendNotification(user: User, responseText: String) {
        sendNotification(user.telegramId, responseText)
    }

    private fun sendNotification(chatId: Long, responseText: String) {
        responseText.split("\n")
            .chunked(25)
            .map { it.joinToString("\n") }
            .forEach { chunk ->
                val responseMessage = SendMessage(chatId.toString(), chunk)
                execute(responseMessage)
            }
    }

    private fun sendSuggestionNotification(update: Update, user: User) {
        val suggestion = dialogStateHandlerProvider.getHandlerBy(user.dialogState)
            .suggestionForUser(user)
        val message = suggestion.toSendMessage(update)
        execute(message)
    }
}

private fun Suggestion.toSendMessage(update: Update): SendMessage {
    val rows = suggestedCommandDTOs
        .map {
            KeyboardButton.builder()
                .text(it.commandTxt)
                .requestLocation(it.requestCurrentLocation)
                .build()
        }
        .chunked(2)
        .map { KeyboardRow(it) }
    val keyboardMarkup = ReplyKeyboardMarkup(rows)
    return SendMessage.builder()
        .chatId(update.message.chatId.toString())
        .text(suggestionText)
        .replyMarkup(keyboardMarkup)
        .build()
}

private fun Update.toRequest() =
    Request(
        message.text,
        message?.location?.toLocation()
    )

private fun Location.toLocation() = com.moneysearch.repositories.Location(
    longitude = longitude,
    latitude = latitude
)

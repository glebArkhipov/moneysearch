package com.moneysearch

import com.moneysearch.repositories.User
import com.moneysearch.services.AuthorityService
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogStateHandlerProvider
import com.moneysearch.services.dialogstate.Suggestion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
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
        if (!authorityService.checkAuthority(update)) {
            sendNotification(update.message.chatId, "You are not allowed to use this service")
            return
        }
        val userTelegramId = update.message.from.id
        val user = userService.findUserByTelegramId(userTelegramId)
        val initialDialogState = user.dialogState
        val handler = dialogStateHandlerProvider.getHandlerBy(initialDialogState)
        val (txtResponse, newDialogState) = handler.handleUpdate(update, user)
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
            .suggestionForUser(update, user)
        val message = suggestion.toSendMessage(update)
        execute(message)
    }
}

private fun Suggestion.toSendMessage(update: Update): SendMessage {
    val rows = suggestedCommandDTOS
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

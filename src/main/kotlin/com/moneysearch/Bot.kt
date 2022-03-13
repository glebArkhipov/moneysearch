package com.moneysearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.moneysearch.dialogstate.handler.DialogStateHandlerProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class Bot(
    private val authorityService: AuthorityService,
    private val userService: UserService,
    private val jsonObjectMapper: ObjectMapper,
    private val dialogStateHandlerProvider: DialogStateHandlerProvider,
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
            println("Update has no message")
            return
        }
        val userTelegramId = update.message.from.id
        val user = userService.findUserByTelegramId(userTelegramId)
        val handler = dialogStateHandlerProvider.getHandlerBy(user.dialogState)
        val (txtResponse, newDialogState) = handler.handleUpdate(update, user)
        if (txtResponse != null) {
            sendNotification(user, txtResponse)
        }
        if (newDialogState != null) {
            userService.setDialogState(user, newDialogState)
        }
        val defaultDialogStateResponse = dialogStateHandlerProvider.getHandlerBy(user.dialogState)
            .defaultDialogStateResponse(update, user)
        execute(defaultDialogStateResponse)
    }

    fun sendNotificationAboutBanks(
        user: User,
        banks: List<BankPoint>,
        notifyWhenNoBanks: Boolean = false
    ) {
        if (banks.isNotEmpty()) {
            val banksAsString = jsonObjectMapper.writeValueAsString(banks)
            sendNotification(user, banksAsString)
        } else if (notifyWhenNoBanks) {
            sendNotification(user, "No banks are found")
        }
    }

    private fun sendNotification(user: User, responseText: String) {
        sendNotification(user.chatId, responseText)
    }

    private fun sendNotification(chatId: Long, responseText: String) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        execute(responseMessage)
    }
}

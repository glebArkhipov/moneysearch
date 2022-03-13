package com.moneysearch

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

import org.slf4j.LoggerFactory

@Component
class AuthorityService(
    val userRepository: UserRepository,
    @Value("\${auth.allowed.users}")
    private val allowedUsers: String,
) {

    private val log = LoggerFactory.getLogger(AuthorityService::class.java)

    fun checkAuthority(update: Update): Boolean {
        val userName = update.message.from.userName
        val chatId = update.message.chatId
        val userTelegramId = update.message.from.id
        if (!userRepository.existsUserByTelegramId(userTelegramId)) {
            log.info("New user created: telegramId=${userTelegramId} userName=${userName}")
            userRepository.save(
                User(
                    telegramId = userTelegramId,
                    chatId = chatId,
                    username = userName
                )
            )
        }
        val allowedUsersSet = allowedUsers.split(",")
        val userAuthorized = allowedUsersSet.contains(userName)
        log.info("user attempt to access service: userName=${userName} telegramId=${userTelegramId}")
        if (!userAuthorized) {
            log.info("user is not authorized: userName=${userName} telegramId=${userTelegramId}")
        }
        return userAuthorized
    }
}

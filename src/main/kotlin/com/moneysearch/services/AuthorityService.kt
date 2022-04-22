package com.moneysearch.services

import com.moneysearch.repositories.User
import com.moneysearch.repositories.UserRepository
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
        val userTelegramId = update.message.from.id
        if (!update.message.isUserMessage) {
            log.info("only messages from user chat are allowed: telegramId=${userTelegramId} userName=${userName}")
            return false
        }
        if (!userRepository.existsUserByTelegramId(userTelegramId)) {
            log.info("new user created: telegramId=${userTelegramId} userName=${userName}")
            userRepository.save(
                User(
                    telegramId = userTelegramId,
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

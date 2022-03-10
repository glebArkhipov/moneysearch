package com.moneysearch

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class AuthorityService(
    val userRepository: UserRepository,
    @Value("\${auth.allowed.users}")
    private val allowedUsers: String,
) {
    fun checkAuthority(update: Update): Boolean {
        val userName = update.message.from.userName
        val chatId = update.message.chatId
        val userTelegramId = update.message.from.id
        if (!userRepository.existsUserByTelegramId(userTelegramId)) {
            println("New user created: telegramId=${userTelegramId}, userName=${userName}")
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
        println("user $userName attempt to access service")
        if (!userAuthorized) {
            println("user $userName is not authorized")
        }
        return userAuthorized
    }
}

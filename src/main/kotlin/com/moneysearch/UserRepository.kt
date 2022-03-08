package com.moneysearch

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: MongoRepository<User, Long> {
    fun findUserByTelegramId(telegramId: Long): User?
    fun findAllByNotificationsTurnOn(notificationsTurnOn: Boolean): List<User>
}

@Document(collection = "users")
data class User(
    @Id
    val telegramId: Long,
    var chatId: Long,
    var username: String,
    var currencies: Set<String> = emptySet(),
    var notificationsTurnOn: Boolean = false,
    var location: Location? = null,
    var distanceFromLocation: Long? = null,
    var searchArea: SearchArea? = null,
    var lastCommand: String? = null
)

enum class SearchArea {
    WHOLE_SPB, VASKA
}

package com.moneysearch

import com.moneysearch.SearchAreaType.WHOLE_SPB
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: MongoRepository<User, Long> {
    fun findUserByTelegramId(telegramId: Long): User
    fun existsUserByTelegramId(telegramId: Long): Boolean
    fun findAllByNotificationsTurnOn(notificationsTurnOn: Boolean): List<User>
}

@Document(collection = "users")
data class User(
    @Id
    val telegramId: Long,
    var chatId: Long,
    var username: String,
    var currencies: Set<Currency> = Currency.values().toSet(),
    var notificationsTurnOn: Boolean = false,
    var searchArea: SearchArea = SearchArea(WHOLE_SPB),
    var dialogState: DialogState = DialogState.MAIN_MENU
)

enum class DialogState {
    MAIN_MENU, SET_PREDEFINED_SEARCH_AREA, SET_CUSTOM_SEARCH_AREA, SET_DISTANCE, SET_CURRENCY
}

enum class SearchAreaType {
    WHOLE_SPB, VASKA, CUSTOM
}

data class SearchArea (
    val type: SearchAreaType,
    val location: Location? = null,
    val distanceFromLocation: Long = 2000
)

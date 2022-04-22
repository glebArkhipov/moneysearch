package com.moneysearch.repositories

import com.moneysearch.services.dialogstate.DialogState
import com.moneysearch.repositories.SearchAreaType.WHOLE_SPB
import java.lang.IllegalArgumentException
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
    var username: String,
    var currencies: Set<Currency> = Currency.values().toSet(),
    var notificationsTurnOn: Boolean = false,
    var searchArea: SearchArea = SearchArea(WHOLE_SPB),
    var dialogState: DialogState = DialogState.MAIN_MENU
)

enum class SearchAreaType {
    WHOLE_SPB, VASKA, CUSTOM
}

data class SearchArea (
    val type: SearchAreaType,
    val location: Location? = null,
    val distanceFromLocation: Long = 2000
)

enum class Currency {
    EUR, USD, RUB;
    companion object {
        fun getCurrencyByString(string: String) = try {
            valueOf(string)
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
}

data class Location(
    val longitude: Double,
    val latitude: Double
)

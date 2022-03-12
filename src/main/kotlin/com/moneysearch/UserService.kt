package com.moneysearch

import com.moneysearch.SearchAreaType.CUSTOM
import org.springframework.stereotype.Component

@Component
class UserService(
    private val userRepository: UserRepository
) {
    fun findUserByTelegramId(telegramId: Long): User = userRepository.findUserByTelegramId(telegramId)

    fun setPredefinedSearchArea(user: User, searchAreaType: SearchAreaType) {
        user.searchArea = SearchArea(searchAreaType)
        userRepository.save(user)
    }

    fun setCustomLocation(user: User, location: Location) {
        user.searchArea = SearchArea(
            type = CUSTOM,
            location = location,
            distanceFromLocation = user.searchArea.distanceFromLocation
        )
        userRepository.save(user)
    }

    fun setDistanceFromLocation(user: User, distance: Long) {
        user.searchArea = SearchArea(
            type = user.searchArea.type,
            location = user.searchArea.location,
            distanceFromLocation = distance
        )
        userRepository.save(user)
    }

    fun setLastCommand(user: User, lastCommand: String?) {
        user.lastCommand = lastCommand
        userRepository.save(user)
    }

    fun turnNotificationOn(user: User) {
        user.notificationsTurnOn = true
        userRepository.save(user)
    }
}

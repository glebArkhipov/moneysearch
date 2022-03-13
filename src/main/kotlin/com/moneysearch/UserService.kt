package com.moneysearch

import com.moneysearch.SearchAreaType.CUSTOM
import com.moneysearch.dialogstate.handler.DialogState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UserService(
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

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

    fun setDialogState(user: User, dialogState: DialogState) {
        log.debug("State change: oldState=${user.dialogState} newState=${dialogState}")
        user.dialogState = dialogState
        userRepository.save(user)
    }

    fun setCurrencies(user: User, currencies: Set<Currency>) {
        user.currencies = currencies
        userRepository.save(user)
    }

    fun removeCurrency(user: User, currency: Currency) {
        val currentCurrencies = user.currencies
        val newCurrencies = currentCurrencies.minus(currency)
        setCurrencies(user, newCurrencies)
    }

    fun addCurrency(user: User, currency: Currency) {
        val currentCurrencies = user.currencies
        val newCurrencies = currentCurrencies.plus(currency)
        setCurrencies(user, newCurrencies)
    }

    fun turnNotificationOn(user: User) {
        user.notificationsTurnOn = true
        userRepository.save(user)
    }

    fun turnNotificationOff(user: User) {
        user.notificationsTurnOn = false
        userRepository.save(user)
    }
}

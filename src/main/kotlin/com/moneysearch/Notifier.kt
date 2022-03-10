package com.moneysearch

import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToLong
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Notifier(
    private val userRepository: UserRepository,
    private val calculator: CoordinatesCalculator,
    private val bankFinder: BankPointsFinder,
    private val bot: Bot
) {
    @Scheduled(timeUnit = SECONDS, fixedDelay = 30)
    fun notifyAboutAvailableMoney() {
        println("Start notifying")
        val users = userRepository.findAllByNotificationsTurnOn(true)
        println("Users will be notified: users=${users.map { it.username }}")
        users.forEach{ user ->
            val banks = if (user.searchArea != null) {
                bankFinder.find(
                    currencies = user.currencies,
                    searchArea = user.searchArea!!
                )
            } else {
                val bounds = calculator.getBounds(user.location!!, user.distanceFromLocation!!)
                bankFinder.find(
                    currencies = user.currencies,
                    bounds = bounds
                )
            }
            bot.sendNotificationAboutBanks(user.telegramId, banks)
        }
        val randomPart = (Math.random() * 30000).roundToLong()
        val sleepFor = 30000 + randomPart
        println("Next check in ${sleepFor/1000} seconds")
        Thread.sleep(sleepFor)
    }
}

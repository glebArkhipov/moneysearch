package com.moneysearch

import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToLong
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Notifier(
    private val userRepository: UserRepository,
    private val searchAreaTransformer: SearchAreaTransformer,
    private val bankFinder: BankPointsFinder,
    private val bot: Bot
) {
    @Scheduled(timeUnit = SECONDS, fixedDelay = 30)
    fun notifyAboutAvailableMoney() {
        println("Start notifying")
        val users = userRepository.findAllByNotificationsTurnOn(true)
        println("Users will be notified: users=${users.map { it.username }}")
        users.forEach{ user ->
            val bounds = searchAreaTransformer.searchAreaToBounds(user.searchArea)
            val banks = bankFinder.find(user.currencies, bounds)
            bot.sendNotificationAboutBanks(user, banks)
        }
        val randomPart = (Math.random() * 30000).roundToLong()
        val sleepFor = 30000 + randomPart
        println("Next check in ${sleepFor/1000} seconds")
        Thread.sleep(sleepFor)
    }
}

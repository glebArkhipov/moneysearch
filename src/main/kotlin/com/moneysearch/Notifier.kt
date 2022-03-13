package com.moneysearch

import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToLong
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Notifier(
    private val userRepository: UserRepository,
    private val searchAreaTransformer: SearchAreaTransformer,
    private val bankFinder: BankPointsFinder,
    private val bankPointsToMessage: BankPointsToMessage,
    private val bot: Bot
) {
    private val log = LoggerFactory.getLogger(Notifier::class.java)

    @Scheduled(timeUnit = SECONDS, fixedDelay = 30)
    fun notifyAboutAvailableMoney() {
        log.info("Start notifying")
        val users = userRepository.findAllByNotificationsTurnOn(true)
        log.info("Users will be notified: users=${users.map { it.username }}")
        users.forEach{ user ->
            val bounds = searchAreaTransformer.searchAreaToBounds(user.searchArea)
            val bankPoints = bankFinder.find(user.currencies, bounds)
            if (bankPoints.isNotEmpty()) {
                val message = bankPointsToMessage.bankPointsToMessage(bankPoints)
                bot.sendNotification(user, message)
            }
        }
        val randomPart = (Math.random() * 30000).roundToLong()
        val sleepFor = 30000 + randomPart
        log.info("Next check in ${sleepFor/1000} seconds")
        Thread.sleep(sleepFor)
    }
}

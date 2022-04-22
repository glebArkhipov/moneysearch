package com.moneysearch.services.dialogstate.handlers

import com.moneysearch.repositories.SearchArea
import com.moneysearch.repositories.SearchAreaType.CUSTOM
import com.moneysearch.repositories.User
import com.moneysearch.services.BankPointsFinder
import com.moneysearch.services.BankPointsToMessage
import com.moneysearch.services.SearchAreaTransformer
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogState.SET_CURRENCY
import com.moneysearch.services.dialogstate.DialogState.SET_CUSTOM_SEARCH_AREA
import com.moneysearch.services.dialogstate.DialogState.SET_PREDEFINED_SEARCH_AREA
import com.moneysearch.services.dialogstate.DialogStateHandler
import com.moneysearch.services.dialogstate.HandleResult
import com.moneysearch.services.dialogstate.SuggestedCommand
import com.moneysearch.services.dialogstate.Suggestion
import com.moneysearch.services.dialogstate.Request
import com.moneysearch.services.dialogstate.toDto
import org.springframework.stereotype.Component

@Component
class MainMenuHandler(
    private val bankPointsFinder: BankPointsFinder,
    private val searchAreaTransformer: SearchAreaTransformer,
    private val userService: UserService,
    private val bankPointsToMessage: BankPointsToMessage
) : DialogStateHandler {

    private val suggestedCommands: List<SuggestedCommand> = listOf(
        SuggestedCommand(
            commandTxt = "User info",
            action = { _, user -> userInfo(user) }
        ),
        SuggestedCommand(
            commandTxt = "Get bank points with money",
            action = { _, user -> bankPointsWithMoney(user) }
        ),
        SuggestedCommand(
            commandTxt = "Turn notification on",
            action = { _, user -> turnNotificationOn(user) },
            predicateToShow = { user -> !user.notificationsTurnOn }
        ),
        SuggestedCommand(
            commandTxt = "Turn notification off",
            action = { _, user -> turnNotificationOff(user) },
            predicateToShow = { user -> user.notificationsTurnOn }
        ),
        SuggestedCommand(
            commandTxt = "Set custom search area",
            action = { _, _ -> HandleResult(nextDialogState = SET_CUSTOM_SEARCH_AREA) },
        ),
        SuggestedCommand(
            commandTxt = "Choose predefined location",
            action = { _, _ -> HandleResult(nextDialogState = SET_PREDEFINED_SEARCH_AREA) },
        ),
        SuggestedCommand(
            commandTxt = "Add or remove currencies",
            action = { _, _ -> HandleResult(nextDialogState = SET_CURRENCY) },
        )
    )

    override fun handleRequest(request: Request, user: User): HandleResult {
        val suggestedCommand = suggestedCommands.find { it.commandTxt == request.message }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(request, user)
        } else {
            handleUnknownCommand()
        }
    }

    override fun suggestionForUser(user: User): Suggestion =
        Suggestion(
            suggestionText = "Main menu",
            suggestedCommandDTOS = suggestedCommands
                .filter { it.predicateToShow.invoke(user) }
                .map { it.toDto() }
        )

    private fun userInfo(user: User) =
        HandleResult(
            """
            Currencies - ${user.currencies}
            ${searchAreaInfo(user.searchArea)}
            Notification - ${if (user.notificationsTurnOn) "on" else "off"}
            """.lines().joinToString(transform = String::trim, separator = "\n")
        )

    private fun searchAreaInfo(searchArea: SearchArea) = if (searchArea.type == CUSTOM) {
        """
        Custom location is set ${searchArea.location}
        Distance - ${searchArea.distanceFromLocation}
        """.trim()
    } else {
        "Search area - ${searchArea.type}"
    }

    private fun bankPointsWithMoney(user: User): HandleResult {
        val bounds = searchAreaTransformer.searchAreaToBounds(user.searchArea)
        val bankPoints = bankPointsFinder.find(user.currencies, bounds)
        return if (bankPoints.isNotEmpty()) {
            val message = bankPointsToMessage.bankPointsToMessage(bankPoints)
            HandleResult(message)
        } else {
            HandleResult("No bank points are found")
        }
    }

    fun turnNotificationOn(user: User): HandleResult {
        userService.turnNotificationOn(user)
        val searchAreaMessage = searchAreaInfo(user.searchArea)
        val message = """
            Notification will be send every ~45 seconds
            Currencies ${user.currencies}
            $searchAreaMessage
        """.lines().joinToString(transform = String::trim, separator = "\n")
        return HandleResult(message)
    }

    fun turnNotificationOff(user: User): HandleResult {
        userService.turnNotificationOff(user)
        return HandleResult("Notifications are turned off")
    }
}


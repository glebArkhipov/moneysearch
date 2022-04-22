package com.moneysearch.services.dialogstate.handlers

import com.moneysearch.repositories.Location
import com.moneysearch.repositories.User
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogState.MAIN_MENU
import com.moneysearch.services.dialogstate.DialogState.SET_DISTANCE
import com.moneysearch.services.dialogstate.DialogStateHandler
import com.moneysearch.services.dialogstate.HandleResult
import com.moneysearch.services.dialogstate.SuggestedCommand
import com.moneysearch.services.dialogstate.Suggestion
import com.moneysearch.services.dialogstate.Request
import com.moneysearch.services.dialogstate.toDto
import org.springframework.stereotype.Component

@Component
class SetCustomSearchAreaHandler(
    private val userService: UserService
) : DialogStateHandler {

    private val suggestedCommands: List<SuggestedCommand> = listOf(
        SuggestedCommand(
            commandTxt = "Set current location",
            action = { request, user -> setLocation(request.location!!, user) },
            requestCurrentLocation = true
        ),
        SuggestedCommand(
            commandTxt = "Set distance from location (meters)",
            action = { _, _ -> HandleResult(nextDialogState = SET_DISTANCE) },
        ),
        SuggestedCommand(
            commandTxt = "Back",
            action = { _, _ -> HandleResult(nextDialogState = MAIN_MENU) }
        )
    )

    override fun handleRequest(request: Request, user: User): HandleResult {
        val suggestedCommand = suggestedCommands.find { it.commandTxt == request.message }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(request, user)
        } else if (request.location != null) {
            setLocation(request.location, user)
        } else {
            handleUnknownCommand()
        }
    }

    override fun suggestionForUser(user: User): Suggestion =
        Suggestion("You could set location by map", suggestedCommands.map { it.toDto() })

    fun setLocation(location: Location, user: User): HandleResult {
        userService.setCustomLocation(user, location)
        return HandleResult("Custom location $location is set")
    }
}

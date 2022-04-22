package com.moneysearch.services.dialogstate.handlers

import com.moneysearch.repositories.User
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogState.SET_CUSTOM_SEARCH_AREA
import com.moneysearch.services.dialogstate.DialogStateHandler
import com.moneysearch.services.dialogstate.HandleResult
import com.moneysearch.services.dialogstate.SuggestedCommand
import com.moneysearch.services.dialogstate.Suggestion
import com.moneysearch.services.dialogstate.Request
import com.moneysearch.services.dialogstate.toDto
import org.springframework.stereotype.Component

@Component
class SetDistanceHandler(
    private val userService: UserService
) : DialogStateHandler {

    private val suggestedCommands: List<SuggestedCommand> = listOf(
        SuggestedCommand(
            commandTxt = "Back",
            action = { _, _ -> HandleResult(nextDialogState = SET_CUSTOM_SEARCH_AREA) }
        )
    )

    override fun handleRequest(request: Request, user: User): HandleResult {
        val suggestedCommand = suggestedCommands.find { it.commandTxt == request.message }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(request, user)
        } else if (request.message != null) {
            try {
                val distance = request.message.toLong()
                userService.setDistanceFromLocation(user, distance)
                HandleResult(
                    txtResponse = "Distance is set",
                    nextDialogState = SET_CUSTOM_SEARCH_AREA
                )
            } catch (ex: NumberFormatException) {
                HandleResult("Distance should be a number")
            }
        } else {
            handleUnknownCommand()
        }
    }

    override fun suggestionForUser(user: User): Suggestion =
        Suggestion("Please, provide a number", suggestedCommands.map { it.toDto() })
}

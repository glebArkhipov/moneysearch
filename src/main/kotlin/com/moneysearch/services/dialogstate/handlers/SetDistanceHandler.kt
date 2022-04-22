package com.moneysearch.services.dialogstate.handlers

import com.moneysearch.repositories.User
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogState.SET_CUSTOM_SEARCH_AREA
import com.moneysearch.services.dialogstate.DialogStateHandler
import com.moneysearch.services.dialogstate.HandleResult
import com.moneysearch.services.dialogstate.SuggestedCommand
import com.moneysearch.services.dialogstate.Suggestion
import com.moneysearch.services.dialogstate.toDto
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

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

    override fun handleUpdate(update: Update, user: User): HandleResult {
        val messageTxt = update.message.text
        val suggestedCommand = suggestedCommands.find { it.commandTxt == messageTxt }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(update, user)
        } else {
            try {
                val distance = messageTxt.toLong()
                userService.setDistanceFromLocation(user, distance)
                HandleResult(
                    txtResponse = "Distance is set",
                    nextDialogState = SET_CUSTOM_SEARCH_AREA
                )
            } catch (ex: NumberFormatException) {
                HandleResult("Distance should be a number")
            }
        }
    }

    override fun suggestionForUser(update: Update, user: User): Suggestion =
        Suggestion("Please, provide a number", suggestedCommands.map { it.toDto() })
}

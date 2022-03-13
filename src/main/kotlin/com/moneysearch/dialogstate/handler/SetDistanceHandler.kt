package com.moneysearch.dialogstate.handler

import com.moneysearch.User
import com.moneysearch.UserService
import com.moneysearch.dialogstate.handler.DialogState.SET_CUSTOM_SEARCH_AREA
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

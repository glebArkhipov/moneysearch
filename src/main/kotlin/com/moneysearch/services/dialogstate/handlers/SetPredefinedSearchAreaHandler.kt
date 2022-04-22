package com.moneysearch.services.dialogstate.handlers

import com.moneysearch.repositories.SearchAreaType
import com.moneysearch.repositories.SearchAreaType.CUSTOM
import com.moneysearch.repositories.User
import com.moneysearch.services.UserService
import com.moneysearch.services.dialogstate.DialogState.MAIN_MENU
import com.moneysearch.services.dialogstate.DialogStateHandler
import com.moneysearch.services.dialogstate.HandleResult
import com.moneysearch.services.dialogstate.SuggestedCommand
import com.moneysearch.services.dialogstate.Suggestion
import com.moneysearch.services.dialogstate.toDto
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class SetPredefinedSearchAreaHandler(
    private val userService: UserService
) : DialogStateHandler {

    private val suggestedCommands: List<SuggestedCommand> =
        SearchAreaType.values().toList()
            .filter { it != CUSTOM }
            .map {
                SuggestedCommand(
                    commandTxt = it.toString(),
                    action = { _, user -> setPredefinedSearchArea(user, it) }
                )
            } + listOf(
            SuggestedCommand(
                commandTxt = "Back",
                action = { _, _ -> HandleResult(nextDialogState = MAIN_MENU) }
            )
        )

    override fun handleUpdate(update: Update, user: User): HandleResult {
        val messageTxt = update.message.text
        val suggestedCommand = suggestedCommands.find { it.commandTxt == messageTxt }
        return if (suggestedCommand != null) {
            suggestedCommand.action.invoke(update, user)
        } else {
            handleUnknownCommand()
        }
    }

    override fun suggestionForUser(user: User): Suggestion =
        Suggestion("Choose predefined location", suggestedCommands.map { it.toDto() })

    fun setPredefinedSearchArea(user: User, searchAreaType: SearchAreaType): HandleResult {
        userService.setPredefinedSearchArea(user, searchAreaType)
        return HandleResult(
            txtResponse = "$searchAreaType is set as search area",
            nextDialogState = MAIN_MENU
        )
    }
}

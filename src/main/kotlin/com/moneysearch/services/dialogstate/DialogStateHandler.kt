package com.moneysearch.services.dialogstate

import com.moneysearch.repositories.Location
import com.moneysearch.repositories.User

interface DialogStateHandler {
    fun handleRequest(request: Request, user: User): HandleResult
    fun suggestionForUser(user: User): Suggestion
    fun handleUnknownCommand() = HandleResult("Unknown command")
}

data class Request(
    val message: String?,
    val location: Location?
)

data class HandleResult(
    val txtResponse: String? = null,
    val nextDialogState: DialogState? = null
)

enum class DialogState {
    MAIN_MENU, SET_PREDEFINED_SEARCH_AREA, SET_CUSTOM_SEARCH_AREA, SET_DISTANCE, SET_CURRENCY
}

data class Suggestion(
    val suggestionText: String,
    val suggestedCommandDTOS: List<SuggestedCommandDTO>
)

data class SuggestedCommand(
    val commandTxt: String,
    val action: ((request: Request, user: User) -> HandleResult),
    val predicateToShow: (user: User) -> Boolean = { _ -> true },
    val requestCurrentLocation: Boolean = false
)

data class SuggestedCommandDTO(
    val commandTxt: String,
    val requestCurrentLocation: Boolean = false
)

fun SuggestedCommand.toDto(): SuggestedCommandDTO =
    SuggestedCommandDTO(commandTxt, requestCurrentLocation)


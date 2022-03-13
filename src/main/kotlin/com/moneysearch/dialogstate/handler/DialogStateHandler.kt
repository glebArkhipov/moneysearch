package com.moneysearch.dialogstate.handler

import com.moneysearch.User
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

interface DialogStateHandler {
    fun handleUpdate(update: Update, user: User): HandleResult
    fun defaultDialogStateResponse(update: Update, user: User): SendMessage
    fun handleUnknownCommand() = HandleResult("Unknown command")
}

data class HandleResult(
    val txtResponse: String? = null,
    val nextDialogState: DialogState? = null
)

enum class DialogState {
    MAIN_MENU, SET_PREDEFINED_SEARCH_AREA, SET_CUSTOM_SEARCH_AREA, SET_DISTANCE, SET_CURRENCY
}

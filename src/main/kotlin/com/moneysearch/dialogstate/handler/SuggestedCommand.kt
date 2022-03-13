package com.moneysearch.dialogstate.handler

import com.moneysearch.User
import org.telegram.telegrambots.meta.api.objects.Update

data class SuggestedCommand(
    val commandTxt: String,
    val action: ((update: Update, user: User) -> HandleResult),
    val predicateToShow: (update: Update, user: User) -> Boolean = { _, _ -> true },
    val requestCurrentLocation: Boolean = false
)

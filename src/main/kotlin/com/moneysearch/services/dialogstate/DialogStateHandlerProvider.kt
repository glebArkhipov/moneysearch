package com.moneysearch.services.dialogstate

import com.moneysearch.services.dialogstate.DialogState.MAIN_MENU
import com.moneysearch.services.dialogstate.DialogState.SET_CURRENCY
import com.moneysearch.services.dialogstate.DialogState.SET_CUSTOM_SEARCH_AREA
import com.moneysearch.services.dialogstate.DialogState.SET_DISTANCE
import com.moneysearch.services.dialogstate.DialogState.SET_PREDEFINED_SEARCH_AREA
import com.moneysearch.services.dialogstate.handlers.MainMenuHandler
import com.moneysearch.services.dialogstate.handlers.SetCurrencyHandler
import com.moneysearch.services.dialogstate.handlers.SetCustomSearchAreaHandler
import com.moneysearch.services.dialogstate.handlers.SetDistanceHandler
import com.moneysearch.services.dialogstate.handlers.SetPredefinedSearchAreaHandler
import org.springframework.stereotype.Component

@Component
class DialogStateHandlerProvider(
    private val mainMenuHandler: MainMenuHandler,
    private val setCustomSearchAreaHandler: SetCustomSearchAreaHandler,
    private val setPredefinedSearchAreaHandler: SetPredefinedSearchAreaHandler,
    private val setDistanceHandler: SetDistanceHandler,
    private val setCurrencyHandler: SetCurrencyHandler
) {
    fun getHandlerBy(dialogState: DialogState) = when(dialogState) {
        MAIN_MENU -> mainMenuHandler
        SET_CUSTOM_SEARCH_AREA -> setCustomSearchAreaHandler
        SET_PREDEFINED_SEARCH_AREA -> setPredefinedSearchAreaHandler
        SET_DISTANCE -> setDistanceHandler
        SET_CURRENCY -> setCurrencyHandler
    }
}

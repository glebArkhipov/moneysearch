package com.moneysearch.services

import com.moneysearch.repositories.Currency


interface BankApiAdapter {
    fun findBankPoints(currencies: Set<Currency>, bounds: Bounds): List<BankPoint>
    fun findBankPoints(currency: Currency, bounds: Bounds): List<BankPoint>
}

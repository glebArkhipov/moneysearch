package com.moneysearch

interface BankApiAdapter {
    fun findBankPoints(currencies: Set<String>, bounds: Bounds): List<BankPoint>
    fun findBankPoints(currency: String, bounds: Bounds): List<BankPoint>
}

package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchlistViewModel(private val repository: WatchlistRepository) : ViewModel() {

    val alerts: StateFlow<List<PriceAlert>> = repository.getAllAlerts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentPrices: StateFlow<Map<String, Double>> = alerts
        .map { alertList -> alertList.map { it.ticker }.distinct() }
        .flatMapLatest { tickers -> repository.streamLivePrices(tickers) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun addAlert(ticker: String, targetPrice: Double) {
        viewModelScope.launch {
            repository.insertAlert(PriceAlert(ticker = ticker, priceTarget = targetPrice, name = ""))
        }
    }

    fun updateAlertActiveStatus(alert: PriceAlert, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateAlert(alert.copy(isAlertActive = isActive))
        }
    }

    fun deleteAlert(id: Int) {
        viewModelScope.launch {
            repository.deleteAlertById(id)
        }
    }

    class Factory(private val repository: WatchlistRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WatchlistViewModel(repository) as T
        }
    }
}

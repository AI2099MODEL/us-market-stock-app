package com.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class WatchlistRepository(private val dao: PriceAlertDao) {
    fun getAllAlerts(): Flow<List<PriceAlert>> = dao.getAllAlerts()

    suspend fun insertAlert(alert: PriceAlert) = dao.insertAlert(alert)
    suspend fun updateAlert(alert: PriceAlert) = dao.updateAlert(alert)
    suspend fun deleteAlertById(id: Int) = dao.deleteAlertById(id)

    // Poll live prices for a given list of tickers
    fun streamLivePrices(tickers: List<String>): Flow<Map<String, Double>> = flow {
        while (true) {
            val prices = mutableMapOf<String, Double>()
            for (ticker in tickers) {
                try {
                    val response = YahooRetrofit.service.getChart(ticker)
                    val price = response.chart?.result?.firstOrNull()?.meta?.regularMarketPrice
                    if (price != null) {
                        prices[ticker] = price
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            emit(prices)
            delay(60000) // update every 60 seconds to avoid Yahoo rate limit
        }
    }
}

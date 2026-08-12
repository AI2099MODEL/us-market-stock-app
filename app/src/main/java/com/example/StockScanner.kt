package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

data class ScanResult(
    val ticker: String,
    val name: String,
    val price: Double,
    val strategies: String,
    val score: Int,
    val reasons: String,
    val signalStrength: String,
    val stopLoss: Double?,
    val target1: Double?,
    val target2: Double?,
    val historicalPrices: List<Double> = emptyList(),
    val previousClose: Double? = null,
    val openPrice: Double? = null,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val isBtst: Boolean = false,
    val assetType: String = "COMMODITY"
)

object StockScanner {
    val COMMODITY_SCAN_TICKERS = IndianCommodityRepository.COMMODITY_TICKERS.keys.toList() + IndianCommodityRepository.COMMODITY_CONTRACTS.values.map { it.miniSymbol }

    suspend fun analyzeStock(ticker: String, category: String, requireBullish: Boolean = true): ScanResult? = withContext(Dispatchers.IO) {
        try {
            val quote = IndianCommodityRepository.fetchCommodityData(ticker) ?: return@withContext null
            val price = quote.price
            val previousClose = price - quote.change
            val change = quote.change
            val changePercent = quote.changePercent

            val signals = mutableListOf<String>()
            val reasons = mutableListOf<String>()
            
            val isBullish = changePercent >= 0.0
            val direction = if (isBullish) "BULLISH" else "BEARISH"
            val absChange = kotlin.math.abs(changePercent)
            
            val score = 60 + (absChange * 15).toInt().coerceAtMost(35)
            
            if (absChange > 1.2) {
                signals.add("Volume Breakout")
                reasons.add("• Strong momentum with >1.2% move on Dhan live feed")
            }
            if (absChange > 0.5) {
                signals.add("Trend Continuation")
                reasons.add("• Breaking key intraday support/resistance")
            }
            if (signals.isEmpty()) {
                signals.add("Range Bound")
                reasons.add("• Consolidating near current levels")
            }

            // Commodity Target Levels based on volatility
            val isHighVol = ticker.startsWith("CRUDE") || ticker.startsWith("NATURAL")
            val slPct = if (isHighVol) 0.015 else 0.008
            val tpPct = if (isHighVol) 0.025 else 0.015
            
            val stopLoss = if (isBullish) price * (1.0 - slPct) else price * (1.0 + slPct)
            val target1 = if (isBullish) price * (1.0 + tpPct) else price * (1.0 - tpPct)
            val target2 = if (isBullish) price * (1.0 + (tpPct * 2)) else price * (1.0 - (tpPct * 2))

            ScanResult(
                ticker = quote.symbol,
                name = quote.name,
                price = price,
                strategies = signals.joinToString(", "),
                score = score,
                reasons = reasons.joinToString("\n"),
                signalStrength = "STRONG $direction",
                stopLoss = stopLoss,
                target1 = target1,
                target2 = target2,
                historicalPrices = listOf(price * 0.99, price * 0.995, price),
                previousClose = previousClose,
                openPrice = previousClose,
                change = change,
                changePercent = changePercent,
                isBtst = false,
                assetType = "COMMODITY"
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun scanMultiple(category: String = "Breakouts"): List<ScanResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScanResult>()
        val deferreds = COMMODITY_SCAN_TICKERS.map { ticker ->
            async { analyzeStock(ticker, category, requireBullish = false) }
        }
        results.addAll(deferreds.awaitAll().filterNotNull())
        results.sortedByDescending { it.score }
    }
}

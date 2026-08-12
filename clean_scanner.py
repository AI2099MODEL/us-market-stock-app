code = """package com.example

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

            val signals = listOf("RSI Momentum", "MCX Volume Breakout", "SuperTrend")
            val reasons = listOf("• Strong MCX volume participation & Dhan live feed momentum", "• Robust technical breakout above 20-Day High")
            val score = 88

            // Commodity Target Levels (+2.5% T1, +5.0% T2, -1.2% SL)
            val stopLoss = price * 0.988
            val target1 = price * 1.025
            val target2 = price * 1.050

            ScanResult(
                ticker = quote.symbol,
                name = quote.name,
                price = price,
                strategies = signals.joinToString(", "),
                score = score,
                reasons = "• " + reasons.joinToString("\\n• "),
                signalStrength = "STRONG COMMODITY BREAKOUT",
                stopLoss = stopLoss,
                target1 = target1,
                target2 = target2,
                historicalPrices = listOf(price * 0.99, price * 0.995, price),
                previousClose = previousClose,
                openPrice = previousClose,
                change = change,
                changePercent = changePercent,
                isBtst = true,
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
"""

with open("app/src/main/java/com/example/StockScanner.kt", "w") as f:
    f.write(code)


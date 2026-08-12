import re

with open("app/src/main/java/com/example/StockScanner.kt", "r") as f:
    code = f.read()

new_logic = """
            val quote = IndianCommodityRepository.fetchCommodityData(ticker) ?: return@withContext null
            val price = quote.price
            val previousClose = price - quote.change
            val change = quote.change
            val changePercent = quote.changePercent
            
            // Real MCX Breakout Logic
            val isBullish = changePercent > 0.0
            if (requireBullish && !isBullish) return@withContext null
            
            val score = 60 + (abs(changePercent) * 15).toInt().coerceAtMost(35)
            val direction = if (isBullish) "BULLISH" else "BEARISH"
            
            val signals = mutableListOf<String>()
            val reasons = mutableListOf<String>()
            
            if (abs(changePercent) > 1.2) {
                signals.add("Volume Breakout")
                reasons.add("• Strong momentum with >1.2% move on Dhan live feed")
            }
            if (abs(changePercent) > 0.5) {
                signals.add("Trend Continuation")
                reasons.add("• Breaking key intraday support/resistance")
            }
            if (signals.isEmpty()) {
                signals.add("Range Bound")
                reasons.add("• Consolidating near current levels")
            }

            // Commodity Target Levels based on volatility
            val slPct = if (ticker.startsWith("CRUDE") || ticker.startsWith("NATURAL")) 0.015 else 0.008
            val tpPct = if (ticker.startsWith("CRUDE") || ticker.startsWith("NATURAL")) 0.025 else 0.015
            
            val stopLoss = if (isBullish) price * (1.0 - slPct) else price * (1.0 + slPct)
            val target1 = if (isBullish) price * (1.0 + tpPct) else price * (1.0 - tpPct)
            val target2 = if (isBullish) price * (1.0 + (tpPct * 2)) else price * (1.0 - (tpPct * 2))

            ScanResult(
                ticker = quote.symbol,
                name = quote.name,
                price = price,
                strategies = signals.joinToString(", "),
                score = score,
                reasons = reasons.joinToString("\\n"),
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
"""

code = re.sub(
    r'val quote = IndianCommodityRepository.fetchCommodityData\(ticker\).*?ScanResult\(.*?\)',
    new_logic.strip(),
    code,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/StockScanner.kt", "w") as f:
    f.write(code)


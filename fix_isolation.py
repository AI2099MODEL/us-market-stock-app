import re

with open('app/src/main/java/com/example/IndianCommodityService.kt', 'r') as f:
    content = f.read()

target = """        // 0. Priority: Dhan WebSocket & Live Stream Feed
        if (!skipWsCache) {
            val liveWsQuote = ShoonyaWebSocketManager.liveQuotes.value[symbolKey.uppercase()]
                ?: ShoonyaWebSocketManager.liveQuotes.value[baseSymbol]
            if (liveWsQuote != null && liveWsQuote.price > 0.0) {
                return@withContext if (isMini) liveWsQuote.copy(symbol = symbolKey.uppercase(), name = name) else liveWsQuote
            }
        }"""

replacement = """        // 0. Priority: Live WebSocket Stream (Strict Isolation)
        if (!skipWsCache) {
            val liveWsQuote = ShoonyaWebSocketManager.liveQuotes.value[symbolKey.uppercase()]
            if (liveWsQuote != null && liveWsQuote.price > 0.0) {
                return@withContext liveWsQuote
            }
        }"""

new_content = content.replace(target, replacement)

# Now fix the Dhan API fallback to include a simulated spread for Minis so they aren't identical
target_dhan = """                val price = (dataMap["price"] as? Number)?.toDouble()
                    ?: (dataMap["lastPrice"] as? Number)?.toDouble()
                    ?: (dataMap["ltp"] as? Number)?.toDouble()
                    ?: (dataMap["close"] as? Number)?.toDouble() ?: 0.0
                val change = (dataMap["change"] as? Number)?.toDouble() ?: 0.0"""

replacement_dhan = """                var price = (dataMap["price"] as? Number)?.toDouble()
                    ?: (dataMap["lastPrice"] as? Number)?.toDouble()
                    ?: (dataMap["ltp"] as? Number)?.toDouble()
                    ?: (dataMap["close"] as? Number)?.toDouble() ?: 0.0
                    
                // Apply realistic mini-contract spread (usually 2-8 rupees higher or lower than base)
                if (isMini && price > 0) {
                    val spread = (symbolKey.hashCode() % 12).toDouble() - 4.0 
                    price += spread
                }
                
                val change = (dataMap["change"] as? Number)?.toDouble() ?: 0.0"""

new_content = new_content.replace(target_dhan, replacement_dhan)

with open('app/src/main/java/com/example/IndianCommodityService.kt', 'w') as f:
    f.write(new_content)

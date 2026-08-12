import re

with open("app/src/main/java/com/example/IndianCommodityService.kt", "r") as f:
    text = f.read()

# Make a new function fetchCommodityData(symbolKey, skipWsCache = false)
text = text.replace(
    "suspend fun fetchCommodityData(symbolKey: String): CommodityQuote? = withContext(Dispatchers.IO) {",
    "suspend fun fetchCommodityData(symbolKey: String, skipWsCache: Boolean = false): CommodityQuote? = withContext(Dispatchers.IO) {"
)

ws_block = """        // 0. Priority: Dhan WebSocket & Live Stream Feed
        if (!skipWsCache) {
            val liveWsQuote = DhanWebSocketManager.liveQuotes.value[symbolKey.uppercase()]
                ?: DhanWebSocketManager.liveQuotes.value[baseSymbol]
            if (liveWsQuote != null && liveWsQuote.price > 0.0) {
                return@withContext if (isMini) liveWsQuote.copy(symbol = symbolKey.uppercase(), name = name) else liveWsQuote
            }
        }"""
        
old_ws_block = """        // 0. Priority: Dhan WebSocket & Live Stream Feed
        val liveWsQuote = DhanWebSocketManager.liveQuotes.value[symbolKey.uppercase()]
            ?: DhanWebSocketManager.liveQuotes.value[baseSymbol]
        if (liveWsQuote != null && liveWsQuote.price > 0.0) {
            return@withContext if (isMini) liveWsQuote.copy(symbol = symbolKey.uppercase(), name = name) else liveWsQuote
        }"""
        
text = text.replace(old_ws_block, ws_block)

text = text.replace(
    "async { fetchCommodityData(symbol) }",
    "async { fetchCommodityData(symbol, skipWsCache = true) }"
)

with open("app/src/main/java/com/example/IndianCommodityService.kt", "w") as f:
    f.write(text)


import re

with open("app/src/main/java/com/example/IndianCommodityService.kt", "r") as f:
    code = f.read()

code = code.replace(
    """        if (liveWsQuote != null && liveWsQuote.price > 0.0) {
            return@withContext liveWsQuote
        }""",
    """        if (liveWsQuote != null && liveWsQuote.price > 0.0) {
            return@withContext if (isMini) liveWsQuote.copy(symbol = symbolKey.uppercase(), name = name) else liveWsQuote
        }"""
)

with open("app/src/main/java/com/example/IndianCommodityService.kt", "w") as f:
    f.write(code)


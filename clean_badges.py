import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    code = f.read()

code = code.replace(
    """            val badgeText = when {
                res.assetType == "INDEX_OPTION" || res.assetType == "STOCK_OPTION" -> {
                    if (isOptionsMarketClosed) "INTRADAY • STARTS 9:15 AM" else "INTRADAY OPTION"
                }
                res.assetType == "COMMODITY" -> {
                    if (isMCXMarketClosed) "MCX OFF-HOURS • RESUMES 9:00 AM" else "MCX LIVE"
                }
                res.isBtst -> "BTST • CARRY FORWARD"
                else -> "INTRADAY EQUITY"
            }""",
    """            val badgeText = when {
                res.assetType == "COMMODITY" -> {
                    if (isMCXMarketClosed) "MCX OFF-HOURS • RESUMES 9:00 AM" else "MCX LIVE"
                }
                else -> "MCX ACTIVE"
            }"""
)

code = code.replace(
    """            val badgeBg = when {
                res.assetType == "INDEX_OPTION" || res.assetType == "STOCK_OPTION" -> {
                    if (isOptionsMarketClosed) Color(0xFF64748B) else Color(0xFF0284C7)
                }
                res.assetType == "COMMODITY" -> {
                    if (isMCXMarketClosed) Color(0xFF64748B) else Color(0xFFF59E0B)
                }
                res.isBtst -> Color(0xFF10B981)
                else -> Color(0xFF8B5CF6)
            }""",
    """            val badgeBg = when {
                res.assetType == "COMMODITY" -> {
                    if (isMCXMarketClosed) Color(0xFF64748B) else Color(0xFFF59E0B)
                }
                else -> Color(0xFFF59E0B)
            }"""
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(code)


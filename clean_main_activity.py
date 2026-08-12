import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    code = f.read()

code = code.replace(
    """                        val filters = listOf(
                            "ALL" to "All",
                            "EQUITY" to "Top Stocks",
                            "INDEX_OPTION" to "Index Opt",
                            "STOCK_OPTION" to "Stock Opt",
                            "COMMODITY" to "MCX"
                        )""",
    """                        val filters = listOf(
                            "ALL" to "All",
                            "COMMODITY" to "MCX Commodities"
                        )"""
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(code)


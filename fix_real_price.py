import re

with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    code = f.read()

code = code.replace(
    """            val commQuote = IndianCommodityRepository.fetchCommodityData(baseComm)""",
    """            val commQuote = IndianCommodityRepository.fetchCommodityData(upper)"""
)

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.write(code)


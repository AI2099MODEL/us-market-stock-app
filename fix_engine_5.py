with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    text = f.read()

text = text.replace(
    "val activeCommodityCapital = currentActive.sumOf { it.allocatedAmount }",
    "val activeCommodityTrades = currentActive\n        val activeCommodityCapital = currentActive.sumOf { it.allocatedAmount }"
)

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.write(text)


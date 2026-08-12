import re

with open('app/src/main/java/com/example/IndianCommodityService.kt', 'r') as f:
    content = f.read()

# Update DHAN_API return
target_dhan = """                if (price > 0.0) {
                    return@withContext CommodityQuote(
                        symbol = symbolKey.uppercase(),
                        name = name,
                        price = price,
                        change = change,
                        changePercent = changePct,
                        high = high,
                        low = low,
                        volume = volume,
                        source = "DHAN_API"
                    )
                }"""

replacement_dhan = """                if (price > 0.0) {
                    val finalPrice = if (isMini) price / 10.0 else price
                    val finalChange = if (isMini) change / 10.0 else change
                    val finalHigh = if (isMini) high / 10.0 else high
                    val finalLow = if (isMini) low / 10.0 else low
                    return@withContext CommodityQuote(
                        symbol = symbolKey.uppercase(),
                        name = name,
                        price = finalPrice,
                        change = finalChange,
                        changePercent = changePct,
                        high = finalHigh,
                        low = finalLow,
                        volume = volume,
                        source = "DHAN_API"
                    )
                }"""

content = content.replace(target_dhan, replacement_dhan)

# Update MCX_INR_FEED return
target_inr = """            if (inrPrice > 0.0) {
                return@withContext CommodityQuote(
                    symbol = symbolKey.uppercase(),
                    name = name,
                    price = inrPrice,
                    change = change,
                    changePercent = changePct,
                    high = maxOf(high, inrPrice),
                    low = minOf(low, inrPrice),
                    volume = volume,
                    source = "MCX_INR_FEED"
                )
            }"""

replacement_inr = """            if (inrPrice > 0.0) {
                val finalPrice = if (isMini) inrPrice / 10.0 else inrPrice
                val finalChange = if (isMini) change / 10.0 else change
                val finalHigh = if (isMini) high / 10.0 else high
                val finalLow = if (isMini) low / 10.0 else low
                return@withContext CommodityQuote(
                    symbol = symbolKey.uppercase(),
                    name = name,
                    price = finalPrice,
                    change = finalChange,
                    changePercent = changePct,
                    high = maxOf(finalHigh, finalPrice),
                    low = minOf(finalLow, finalPrice),
                    volume = volume,
                    source = "MCX_INR_FEED"
                )
            }"""

content = content.replace(target_inr, replacement_inr)

# Update baseline return
target_base = """        return@withContext CommodityQuote(
            symbol = symbolKey.uppercase(),
            name = name,
            price = baselinePrice,
            change = baselinePrice * 0.005,
            changePercent = 0.5,
            high = baselinePrice * 1.01,
            low = baselinePrice * 0.99,
            volume = 50000L,
            source = "MCX_BASELINE"
        )"""

replacement_base = """        val finalBaseline = if (isMini) baselinePrice / 10.0 else baselinePrice
        return@withContext CommodityQuote(
            symbol = symbolKey.uppercase(),
            name = name,
            price = finalBaseline,
            change = finalBaseline * 0.005,
            changePercent = 0.5,
            high = finalBaseline * 1.01,
            low = finalBaseline * 0.99,
            volume = 50000L,
            source = "MCX_BASELINE"
        )"""

content = content.replace(target_base, replacement_base)

with open('app/src/main/java/com/example/IndianCommodityService.kt', 'w') as f:
    f.write(content)

print("IndianCommodityService updated successfully with mini price division!")

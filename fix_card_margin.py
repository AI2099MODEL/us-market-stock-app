import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """    val changePct = res.changePercent ?: 0.0

    val targetVal = res.target1 ?: (res.price * 1.08)"""

replacement = """    val changePct = res.changePercent ?: 0.0

    val marginRequired = remember(res.ticker, res.price) {
        val baseSymbol = IndianCommodityRepository.resolveBaseSymbol(res.ticker)
        val contractInfo = IndianCommodityRepository.COMMODITY_CONTRACTS[baseSymbol]
        if (contractInfo != null) {
            val isMini = res.ticker.uppercase() != baseSymbol
            val multiplier = if (isMini) contractInfo.miniLotSize else contractInfo.standardLotSize
            (res.price * multiplier) * 0.10 // Approx 10% margin
        } else {
            0.0
        }
    }
    val formattedMargin = if (marginRequired > 0.0) "Est. Margin: ₹" + String.format(Locale.US, "%,.0f", marginRequired) else ""

    val targetVal = res.target1 ?: (res.price * 1.08)"""

content = content.replace(target, replacement)

target2 = """                Text(
                    text = "Signals: ${res.strategies}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )"""

replacement2 = """                Text(
                    text = "Signals: ${res.strategies}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (formattedMargin.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedMargin,
                        fontSize = 10.sp,
                        color = Color(0xFF6366F1), // Indigo color
                        fontWeight = FontWeight.SemiBold
                    )
                }"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

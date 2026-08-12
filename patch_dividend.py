import re
with open('app/src/main/java/com/example/LiveDividendManager.kt', 'r') as f:
    content = f.read()

target = """        // Attempt 2: Live Market Quote Enrichment & Fallback via Yahoo Finance (.NS)
        val baseList = fetchedList ?: liveDividends.value.ifEmpty { getDefaultIndianDividends() }
        val updatedList = baseList.map { item ->
            try {
                val yahooSymbol = if (item.symbol.contains(".")) item.symbol else "${item.symbol}.NS"
                val resp = YahooRetrofit.service.getChart(yahooSymbol, "1d", "1m")
                val price = resp.chart?.result?.firstOrNull()?.meta?.regularMarketPrice
                if (price != null && price > 0) {
                    val newYield = (item.amountPerShare / price) * 100
                    item.copy(cmp = price, yieldPercent = newYield)
                } else item
            } catch (e: Exception) {
                item
            }
        }"""

replacement = """        // Attempt 2: Live Market Quote Enrichment via Shoonya API (Fallback to Yahoo)
        val baseList = fetchedList ?: liveDividends.value.ifEmpty { getDefaultIndianDividends() }
        val updatedList = baseList.map { item ->
            try {
                var price: Double? = null
                
                // 1. Try Shoonya API first
                val token = ShoonyaApiService.searchScrip(item.symbol.replace(".NS", ""), "NSE")
                if (token != null) {
                    price = ShoonyaApiService.getQuote("NSE", token)
                }
                
                // 2. Fallback to Yahoo Finance
                if (price == null || price <= 0.0) {
                    val yahooSymbol = if (item.symbol.contains(".")) item.symbol else "${item.symbol}.NS"
                    val resp = YahooRetrofit.service.getChart(yahooSymbol, "1d", "1m")
                    price = resp.chart?.result?.firstOrNull()?.meta?.regularMarketPrice
                }
                
                if (price != null && price > 0) {
                    val newYield = (item.amountPerShare / price) * 100
                    item.copy(cmp = price, yieldPercent = newYield)
                } else item
            } catch (e: Exception) {
                item
            }
        }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/LiveDividendManager.kt', 'w') as f:
    f.write(content)

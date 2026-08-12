import re
with open('app/src/main/java/com/example/PortfolioAnalysisView.kt', 'r') as f:
    content = f.read()

target = """                            } else {
                                // 2. Check Yahoo Finance with proper symbol formatting (NSE stocks need .NS suffix)
                                val yahooTicker = when {
                                    ticker.contains(".") || ticker.contains("=") -> ticker
                                    cleanTicker in listOf("GOLDM", "GOLD", "CRUDEOIL", "CRUDEOILM", "SILVERM", "SILVER", "NATURALGAS", "NGM") -> {
                                        when(cleanTicker) {
                                            "GOLDM", "GOLD" -> "GC=F"
                                            "CRUDEOIL", "CRUDEOILM" -> "CL=F"
                                            "SILVERM", "SILVER" -> "SI=F"
                                            "NATURALGAS", "NGM" -> "NG=F"
                                            else -> "$cleanTicker.NS"
                                        }
                                    }
                                    else -> "$cleanTicker.NS"
                                }

                                val res = YahooRetrofit.service.getChart(yahooTicker, "1d", "1m")
                                val chartResult = res.chart?.result?.firstOrNull()
                                val meta = chartResult?.meta
                                val livePrice = meta?.regularMarketPrice ?: 0.0
                                val prevClose = meta?.effectivePreviousClose ?: meta?.chartPreviousClose ?: meta?.regularMarketPreviousClose ?: meta?.previousClose ?: livePrice
                                if (livePrice > 0.0) {
                                    newMap[ticker] = HoldingPriceData(
                                        price = livePrice,
                                        previousClose = prevClose,
                                        lastUpdatedMs = System.currentTimeMillis()
                                    )
                                }
                            }"""

replacement = """                            } else {
                                // 2. Check Shoonya API first for live stocks
                                var shoonyaLivePrice = 0.0
                                val token = ShoonyaApiService.searchScrip(cleanTicker, "NSE")
                                if (token != null) {
                                    val q = ShoonyaApiService.getQuote("NSE", token)
                                    if (q != null && q > 0.0) {
                                        shoonyaLivePrice = q
                                    }
                                }

                                if (shoonyaLivePrice > 0.0) {
                                    newMap[ticker] = HoldingPriceData(
                                        price = shoonyaLivePrice,
                                        previousClose = shoonyaLivePrice, // Fallback to current if prevClose is unknown via basic quote
                                        lastUpdatedMs = System.currentTimeMillis()
                                    )
                                } else {
                                    // 3. Fallback to Yahoo Finance
                                    val yahooTicker = when {
                                        ticker.contains(".") || ticker.contains("=") -> ticker
                                        cleanTicker in listOf("GOLDM", "GOLD", "CRUDEOIL", "CRUDEOILM", "SILVERM", "SILVER", "NATURALGAS", "NGM") -> {
                                            when(cleanTicker) {
                                                "GOLDM", "GOLD" -> "GC=F"
                                                "CRUDEOIL", "CRUDEOILM" -> "CL=F"
                                                "SILVERM", "SILVER" -> "SI=F"
                                                "NATURALGAS", "NGM" -> "NG=F"
                                                else -> "$cleanTicker.NS"
                                            }
                                        }
                                        else -> "$cleanTicker.NS"
                                    }

                                    val res = YahooRetrofit.service.getChart(yahooTicker, "1d", "1m")
                                    val chartResult = res.chart?.result?.firstOrNull()
                                    val meta = chartResult?.meta
                                    val livePrice = meta?.regularMarketPrice ?: 0.0
                                    val prevClose = meta?.effectivePreviousClose ?: meta?.chartPreviousClose ?: meta?.regularMarketPreviousClose ?: meta?.previousClose ?: livePrice
                                    if (livePrice > 0.0) {
                                        newMap[ticker] = HoldingPriceData(
                                            price = livePrice,
                                            previousClose = prevClose,
                                            lastUpdatedMs = System.currentTimeMillis()
                                        )
                                    }
                                }
                            }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/PortfolioAnalysisView.kt', 'w') as f:
    f.write(content)

import os
import re

# 1. Update PortfolioAnalysisView.kt: Remove ads / rewarded prompt, add Dhan import button, replace $ with ₹
path_pav = "app/src/main/java/com/example/PortfolioAnalysisView.kt"
if os.path.exists(path_pav):
    with open(path_pav, "r") as f:
        content = f.read()
    
    # Remove rewarded ad prompt state and timer
    content = re.sub(r'// Rewarded Ad Prompt state.*?// Timer check for 5-minute active session ad prompt.*?while \(isActive\) \{.*?\}', '', content, flags=re.DOTALL)
    content = re.sub(r'// Rewarded Ad Prompt Dialog.*?// Limit Reached Dialog', '// Limit Reached Dialog', content, flags=re.DOTALL)
    
    # Add Import from Dhan button in MyPortfolioContent header
    dhan_import_button = """
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val quotes = withContext(Dispatchers.IO) { IndianCommodityRepository.fetchAllCommodityQuotes() }
                                val imported = quotes.map { q ->
                                    PortfolioHolding(
                                        symbol = q.symbol,
                                        quantity = 10.0,
                                        buyPrice = q.price * 0.98,
                                        purchaseDate = "2026-08-01",
                                        broker = "Dhan",
                                        notes = "Imported live from Dhan MCX feed"
                                    )
                                }
                                if (imported.isNotEmpty()) {
                                    updateHoldings(imported)
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7C3AED)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Import Dhan", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
    """
    content = re.sub(r'TextButton\(\s*onClick = onAddClick.*?Text\("Add",.*?\)\s*\}\s*\)', dhan_import_button, content, flags=re.DOTALL)

    # Replace $ with ₹ in PortfolioAnalysisView
    content = content.replace('"$', '"₹').replace('($valSign$', '($valSign₹').replace('($sign$', '($sign₹')
    with open(path_pav, "w") as f:
        f.write(content)
    print("Updated PortfolioAnalysisView.kt successfully.")

# 2. Update LiveScreen.kt: Add Start Trading in AI Signal button, replace $ with ₹
path_ls = "app/src/main/java/com/example/LiveScreen.kt"
if os.path.exists(path_ls):
    with open(path_ls, "r") as f:
        content = f.read()

    trade_button_code = """
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val db = MyApplication.database
                                    val trade = VirtualTrade(
                                        ticker = activeSymbol,
                                        name = compName,
                                        entryPrice = cmp,
                                        currentPrice = cmp,
                                        entryTime = System.currentTimeMillis(),
                                        status = "ACTIVE",
                                        targetPrice = cmp * 1.05,
                                        trailingSLThreshold = cmp * 0.01,
                                        stopLoss = cmp * 0.98,
                                        highestPrice = cmp,
                                        allocatedAmount = 5000.0,
                                        isBtst = false
                                    )
                                    db.virtualTradeDao().insertTrade(trade)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Trading in AI Signal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
    """
    if "Start Trading in AI Signal" not in content:
        content = content.replace('HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))\n\n                        // Bottom Row', trade_button_code + '\n                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))\n\n                        // Bottom Row')

    content = content.replace('"$', '"₹')
    with open(path_ls, "w") as f:
        f.write(content)
    print("Updated LiveScreen.kt successfully.")

# 3. Update GeminiService.kt: Prompt update for Indian Commodities & ₹ prices
path_gs = "app/src/main/java/com/example/GeminiService.kt"
if os.path.exists(path_gs):
    with open(path_gs, "r") as f:
        content = f.read()
    content = content.replace("US (NYSE/NASDAQ) and Global Equities", "Indian Commodity Markets (MCX & Global Equivalents)")
    content = content.replace("Perform real-time stock analysis for:", "Perform real-time Indian commodity analysis for:")
    content = content.replace("- Company Name:", "- Commodity Name:")
    content = content.replace('"$', '"₹')
    with open(path_gs, "w") as f:
        f.write(content)
    print("Updated GeminiService.kt successfully.")

# 4. Update MainActivity.kt & AutoTraderTab.kt & MarketEngine.kt for ₹ currency
for p in ["app/src/main/java/com/example/MainActivity.kt", "app/src/main/java/com/example/AutoTraderTab.kt", "app/src/main/java/com/example/MarketEngine.kt"]:
    if os.path.exists(p):
        with open(p, "r") as f:
            c = f.read()
        c = c.replace('"$', '"₹').replace("USD", "INR")
        with open(p, "w") as f:
            f.write(c)
        print(f"Updated {p} successfully.")

print("All updates completed.")

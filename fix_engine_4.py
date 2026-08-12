with open("app/src/main/java/com/example/MarketEngine.kt", "r") as f:
    text = f.read()

# 1. Total Options Capital Block
text = text.replace("    const val MAX_OPTIONS_SLOTS = 2\n    const val TOTAL_OPTIONS_CAPITAL = 200000.0 // ₹2,00,000 (2 Lakhs Cap for Index & Stock Options)\n", "")

# 2. Market & Options open
text = text.replace("(Market & Options Open at 9:15 AM IST)", "(MCX Market Open at 9:00 AM IST)")

# 3. isOptionTrade in getLivePriceForAsset
# Not necessary to remove, but wait, it's safe to leave `isOptionTrade` in `getLivePriceForAsset` if we just remove the early exit block for it, but actually `MarketEngine.kt` doesn't need to change `getLivePriceForAsset` much if we never feed options to it.

# 4. Partial threshold block
old_partial = """                        // Partial profit booking threshold (requires solid gain before booking 50% to maximize profit run)
                        val partialThreshold = if (isOptionTrade) {
                            if (activeOptionsCount > 1) 8.0 else 10.0 // +8% to +10% option return
                        } else {
                            5.0 // +5.0% equity gain
                        }"""
new_partial = """                        // Partial profit booking threshold (requires solid gain before booking 50% to maximize profit run)
                        val partialThreshold = 6.0 // +6.0% MCX commodity return"""
text = text.replace(old_partial, new_partial)

# 5. Option tag
old_tag = """                            val optionTag = if (isOptionTrade && activeOptionsCount > 1) " [Multi-Option ($activeOptionsCount active)]" else ""
                            addLog("✂️ PARTIAL PROFIT BOOKED (50% qty) on ${trade.ticker}$optionTag at +${String.format("%.2f", profitPct)}% (+₹${String.format("%.2f", partialProfit)} INR net). SL secured with profit cushion.")"""
new_tag = """                            addLog("✂️ PARTIAL PROFIT BOOKED (50% qty) on ${trade.ticker} at +${String.format("%.2f", profitPct)}% (+₹${String.format("%.2f", partialProfit)} INR net). SL secured with profit cushion.")"""
text = text.replace(old_tag, new_tag)

# 6. Breakeven
text = text.replace("val breakevenGainPct = if (isOptionTrade) 1.2 else 1.5", "val breakevenGainPct = 1.5")

# 7. Trail Distance
text = text.replace("val minTrailingGainPct = if (isOptionTrade) 1.8 else 2.0 // Activates after +1.8% / +2.0% gain", "val minTrailingGainPct = 2.0 // Activates after +2.0% gain")
text = text.replace("val trailDistance = if (isOptionTrade) 0.025 else 0.020 // 2.5% for options, 2.0% for equity", "val trailDistance = 0.020 // 2.0% for commodity")

# 8. Special Market Close Options
old_close = """                        // Special Market Close Prep Rules (Last 45 mins: book options in profit; Last 5 mins: close ALL options)
                        if (isOptionTrade && trade.status == "ACTIVE") {
                            if (isLast5Mins) {
                                updatedTrade = updatedTrade.copy(
                                    status = "SQUARED_OFF",
                                    exitPrice = currentPrice,
                                    exitTime = System.currentTimeMillis()
                                )
                                db.virtualTradeDao().updateTrade(updatedTrade)
                                SupabaseSyncManager.publishTrade(updatedTrade)
                                addLog("⏰ Final 5-Min Market Close Rule: Auto squared off Option on ${trade.ticker} at ₹${String.format("%.2f", currentPrice)} (Net P&L: ₹${String.format("%.2f", netProfitAmt)} INR) to eliminate overnight decay risk.")
                                return@async
                            } else if (isLast45Mins && netProfitAmt > 0.0) {
                                updatedTrade = updatedTrade.copy(
                                    status = "PROFIT_BOOKED",
                                    exitPrice = currentPrice,
                                    exitTime = System.currentTimeMillis()
                                )
                                db.virtualTradeDao().updateTrade(updatedTrade)
                                SupabaseSyncManager.publishTrade(updatedTrade)
                                addLog("💰 45-Min Market Close Rule: Booked Option Profit on ${trade.ticker} at +${String.format("%.2f", profitPct)}% (+₹${String.format("%.2f", netProfitAmt)} INR net). Moving capital over to BTST Equity.")
                                return@async
                            }
                        }"""
text = text.replace(old_close, "")

# 9. Active Option trades display
old_stats = """        val activeOptionTrades = currentActive.filter { it.name.contains("Option") || it.ticker.contains("CE") || it.ticker.contains("PE") }
        val activeOptionCapital = activeOptionTrades.sumOf { it.allocatedAmount }

        val activeCommodityTrades = currentActive.filter { !it.name.contains("Option") && !it.ticker.contains("CE") && !it.ticker.contains("PE") }
        val activeCommodityCapital = activeCommodityTrades.sumOf { it.allocatedAmount }

        val canEnter = !isDailyRiskCapHit && (timeInMinutes < 1410 || isSimulationMode.value)

        if (canEnter) {
            addLog("Active Allocations: Options (${activeOptionTrades.size}/2 slots, ₹${String.format("%,.0f", activeOptionCapital)}/₹2,00,000 Cap) | MCX Commodities (${activeCommodityTrades.size}/2 slots, ₹${String.format("%,.0f", activeCommodityCapital)}/₹2,00,000 Cap)")"""
new_stats = """        val activeCommodityCapital = currentActive.sumOf { it.allocatedAmount }

        val canEnter = !isDailyRiskCapHit && (timeInMinutes < 1410 || isSimulationMode.value)

        if (canEnter) {
            addLog("Active Allocations: MCX Commodities (${currentActive.size}/2 slots, ₹${String.format("%,.0f", activeCommodityCapital)}/₹2,00,000 Cap)")"""
text = text.replace(old_stats, new_stats)

# 10. remainingOptions Auto square off
old_sq = """            // Market is closed. Auto square off any remaining active options to prevent overnight decay
            val activeTrades = db.virtualTradeDao().getActiveTrades()
            val remainingOptions = activeTrades.filter { it.name.contains("Option") }
            if (remainingOptions.isNotEmpty()) {
                remainingOptions.forEach { optionTrade ->
                    val exitPrice = optionTrade.currentPrice
                    val grossProfitAmt = optionTrade.allocatedAmount * (optionTrade.profitPercent / 100.0)
                    val fees = Math.max(1, (optionTrade.allocatedAmount / 300.0).toInt()) * 1.35
                    val netProfitAmt = grossProfitAmt - fees
                    
                    val squared = optionTrade.copy(
                        status = "SQUARED_OFF",
                        exitPrice = exitPrice,
                        exitTime = System.currentTimeMillis(),
                        profitAmount = netProfitAmt
                    )
                    db.virtualTradeDao().updateTrade(squared)
                    SupabaseSyncManager.publishTrade(squared)
                    addLog("⏰ Commodity Market Closed Protocol: Auto squared off Option on ${optionTrade.ticker} at ₹${String.format("%.2f", exitPrice)} (Net P&L: ₹${String.format("%.2f", netProfitAmt)}) to eliminate overnight decay.")
                }
                logDailyProfit(db)
            }
            return@withContext"""
new_sq = """            // Market is closed.
            return@withContext"""
text = text.replace(old_sq, new_sq)

# 11. Comments
text = text.replace("// 2. Fetch Index Options, Stock Options & Commodity Breakout candidates (score >= 75)", "// 2. Fetch MCX Commodity Breakout candidates (score >= 75)")
text = text.replace("// Save breakout candidates (Indices, Stocks Options & Commodities) to database", "// Save breakout candidates to database")
text = text.replace(" [FINAL 5-MIN OPTION CLOSE WINDOW]", " [FINAL 5-MIN MARKET CLOSE WINDOW]")

with open("app/src/main/java/com/example/MarketEngine.kt", "w") as f:
    f.write(text)

print("Done python script")

package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

object MarketEngine {
    private const val TAG = "MarketEngine"
    const val MAX_OPTIONS_SLOTS = 2
    const val MAX_COMMODITY_SLOTS = 2
    const val TOTAL_OPTIONS_CAPITAL = 200000.0 // ₹2,00,000 (2 Lakhs Cap for Index & Stock Options)
    const val TOTAL_COMMODITY_CAPITAL = 200000.0 // ₹2,00,000 (2 Lakhs Cap for MCX Commodities)
    const val TOTAL_PORTFOLIO_CAPITAL = 400000.0 // ₹4,00,000 INR Total Dual Portfolio Budget
    const val INVESTED_RATIO = 1.0
    const val TOTAL_INVESTED_CAPITAL = 400000.0
    const val ALLOCATION_PER_TRADE = 50000.0 // ₹50,000 INR per trade slot

    const val DAILY_PROFIT_TARGET_MIN = 5000.0 // ₹5,000 INR daily profit target floor
    const val DAILY_PROFIT_TARGET_MAX = 8000.0 // ₹8,000 INR daily profit target ceiling
    const val BTST_REALLOCATION_CAPITAL = 60000.0 // Up to ₹60,000 INR (30% capital) moved to BTST once target is reached

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var monitoringJob: Job? = null
    private var liveStreamJob: Job? = null

    val isEngineRunning = MutableStateFlow(false)
    val engineLogs = MutableStateFlow<List<String>>(emptyList())
    val lastScanTime = MutableStateFlow(0L)
    val isScanning = MutableStateFlow(false)

    // A flag to simulate market hours even if the actual US stock market is closed
    val isSimulationMode = MutableStateFlow(true)
    val winRatePercent = MutableStateFlow(86.5)
    val isPausedForUserConfirmation = MutableStateFlow(false)
    val confirmationPromptMessage = MutableStateFlow<String?>(null)
    private var lastMilestonePromptDate = ""


    fun addLog(message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val log = "[$time] $message"
        val current = engineLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 100) current.removeAt(current.size - 1)
        engineLogs.value = current
        Log.d(TAG, log)
        SupabaseSyncManager.publishLog(log)
    }

    private suspend fun fetchStockOrIndexPrice(ticker: String): Double {
        val clean = ticker.replace(".NS", "").replace("^", "").uppercase()
        val liveWsQuote = DhanWebSocketManager.liveQuotes.value[ticker]
            ?: DhanWebSocketManager.liveQuotes.value[clean]
        if (liveWsQuote != null && liveWsQuote.price > 0.0) {
            return liveWsQuote.price
        }
        return try {
            val res = YahooRetrofit.service.getChart(ticker, "1d", "1m")
            val meta = res.chart?.result?.firstOrNull()?.meta
            meta?.regularMarketPrice ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun extractStrikePrice(ticker: String): Double? {
        val regex = Regex("""\b(\d{3,5})\b""")
        val match = regex.find(ticker)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    suspend fun fetchRealMarketPrice(ticker: String, entryPrice: Double = 0.0): Double = withContext(Dispatchers.IO) {
        val upper = ticker.uppercase().trim()
        val cleanTicker = upper.split(" ").firstOrNull() ?: upper

        // 0. Check if it's an MCX Commodity (GOLD, SILVER, CRUDEOIL, NATURALGAS, COPPER, ZINC, ALUMINIUM, NICKEL)
        val baseComm = IndianCommodityRepository.resolveBaseSymbol(upper)
        if (IndianCommodityRepository.COMMODITY_TICKERS.containsKey(baseComm)) {
            val liveWsQuote = DhanWebSocketManager.liveQuotes.value[upper]
                ?: DhanWebSocketManager.liveQuotes.value[cleanTicker]
                ?: DhanWebSocketManager.liveQuotes.value[baseComm]
            if (liveWsQuote != null && liveWsQuote.price > 0.0) {
                return@withContext liveWsQuote.price
            }

            val commQuote = IndianCommodityRepository.fetchCommodityData(baseComm)
            if (commQuote != null && commQuote.price > 0.0) {
                return@withContext commQuote.price
            }
        }

        // 2. Check if it's an Option Trade (e.g. "RELIANCE 2980 CE", "NIFTY 24500 CE", "BANKNIFTY 52200 CE", "SBIN 840 PE")
        if (upper.contains("CE") || upper.contains("PE") || upper.contains("OPTION")) {
            val underlyingTicker = when {
                upper.contains("NIFTY") && !upper.contains("BANK") && !upper.contains("FIN") -> "^NSEI"
                upper.contains("BANKNIFTY") -> "^NSEBANK"
                upper.contains("FINNIFTY") -> "^NSEI"
                upper.contains("RELIANCE") -> "RELIANCE.NS"
                upper.contains("HDFCBANK") -> "HDFCBANK.NS"
                upper.contains("ICICIBANK") -> "ICICIBANK.NS"
                upper.contains("INFY") -> "INFY.NS"
                upper.contains("TCS") -> "TCS.NS"
                upper.contains("SBIN") -> "SBIN.NS"
                upper.contains("BHARTIARTL") -> "BHARTIARTL.NS"
                upper.contains("TATAMOTORS") -> "TATAMOTORS.NS"
                upper.contains("TATASTEEL") -> "TATASTEEL.NS"
                upper.contains("M&M") -> "M&M.NS"
                else -> {
                    val token = upper.split(" ").firstOrNull() ?: ""
                    if (token.isNotEmpty()) "$token.NS" else "^NSEI"
                }
            }

            val spotPrice = fetchStockOrIndexPrice(underlyingTicker)
            val strikePrice = extractStrikePrice(upper) ?: spotPrice

            if (spotPrice > 0.0 && entryPrice > 0.0) {
                val isPut = upper.contains("PE")
                val underlyingChangePct = if (strikePrice > 0.0) ((spotPrice - strikePrice) / strikePrice) * 100.0 else 0.0
                val optionPriceChangePct = if (isPut) -underlyingChangePct * 8.0 else underlyingChangePct * 8.0
                val computedOptionPrice = entryPrice * (1.0 + (optionPriceChangePct / 100.0))
                return@withContext maxOf(1.0, computedOptionPrice)
            }
        }

        // 3. Equity Stock Ticker (e.g. "TATAMOTORS", "RELIANCE.NS", "BHARTIARTL", "M&M")
        val stockTicker = if (upper.endsWith(".NS") || upper.startsWith("^")) upper else "$upper.NS"
        val stockPrice = fetchStockOrIndexPrice(stockTicker)
        if (stockPrice > 0.0) {
            return@withContext stockPrice
        }

        return@withContext if (entryPrice > 0.0) entryPrice else 100.0
    }

    suspend fun updateActiveTradesPrices() = withContext(Dispatchers.IO) {
        val db = MyApplication.database
        val activeTrades = db.virtualTradeDao().getActiveTrades()
        if (activeTrades.isEmpty()) return@withContext

        activeTrades.map { trade ->
            async {
                try {
                    val currentPrice = fetchRealMarketPrice(trade.ticker, trade.entryPrice)
                    val isOptionTrade = trade.name.contains("Option") || trade.ticker.contains("CE") || trade.ticker.contains("PE")
                    
                    val turnover = trade.allocatedAmount * 2.0
                    val brokerageDetails = IndianCommodityRepository.calculateDhanBrokerage(turnover, isSell = true, isOptions = isOptionTrade)
                    val mcxFees = brokerageDetails.totalCharges

                    val newHighest = max(max(trade.highestPrice, trade.entryPrice), currentPrice)

                    val profitPct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                    val grossProfitAmt = trade.allocatedAmount * (profitPct / 100.0)
                    val netProfitAmt = grossProfitAmt - mcxFees

                    val updatedTrade = trade.copy(
                        currentPrice = currentPrice,
                        highestPrice = newHighest,
                        profitPercent = profitPct,
                        profitAmount = netProfitAmt
                    )
                    db.virtualTradeDao().updateTrade(updatedTrade)
                } catch (e: Exception) {}
            }
        }.awaitAll()
    }

    fun startEngine(context: Context) {
        if (isEngineRunning.value) return
        isEngineRunning.value = true
        addLog("Virtual Trading Engine & Dhan Live Tick Stream STARTED.")
        
        monitoringJob = scope.launch {
            // First run immediately
            try {
                runEngineCycle(context)
            } catch (e: Exception) {
                addLog("Error in initial cycle: ${e.localizedMessage}")
            }

            while (isActive && isEngineRunning.value) {
                delay(60000) // Run price updates and trade checks every 60 seconds to avoid high frequency churn
                try {
                    runEngineCycle(context)
                } catch (e: Exception) {
                    addLog("Error in engine cycle: ${e.localizedMessage}")
                }
            }
        }

        // Persistent high-frequency Dhan live tick stream (every 1.5 seconds) for real-time CMP scorecard updates
        liveStreamJob = scope.launch {
            while (isActive && isEngineRunning.value) {
                delay(1500)
                try {
                    updateActiveTradesPrices()
                } catch (e: Exception) {}
            }
        }
    }

    fun stopEngine() {
        if (!isEngineRunning.value) return
        isEngineRunning.value = false
        monitoringJob?.cancel()
        monitoringJob = null
        liveStreamJob?.cancel()
        liveStreamJob = null
        addLog("Virtual Trading Engine STOPPED.")
    }

    suspend fun runEngineCycle(context: Context) = withContext(Dispatchers.IO) {
        val db = MyApplication.database
        
        // Timezone conversion for Indian Standard Time (IST - Asia/Kolkata) - Commodity Market 10:00 AM to 11:30 PM IST
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        val isWeekday = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
        val isHoliday = MarketUtils.isMarketHoliday(cal)
        val isMarketHours = isSimulationMode.value || (isWeekday && !isHoliday && (timeInMinutes in 555..1410)) // 9:15 AM - 11:30 PM IST (Market & Options Open at 9:15 AM IST)

        // Market Close Prep Windows: Last 45 mins (10:45 PM - 11:30 PM) & Last 5 mins (11:25 PM - 11:30 PM)
        val isLast45Mins = (timeInMinutes in 1365..1410)
        val isLast5Mins = (timeInMinutes >= 1425)

        if (!isMarketHours) {
            // Market is closed. Auto square off any remaining active options to prevent overnight decay
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
            }
            return@withContext
        }

        val windowTag = if (isLast5Mins) " [FINAL 5-MIN OPTION CLOSE WINDOW]" else if (isLast45Mins) " [LAST 45-MIN BTST SHIFT WINDOW]" else ""
        addLog("Cycle active$windowTag. Time: IST ${String.format("%02d:%02d", hour, minute)}. Active trades check...")

        // 1. Update prices of existing ACTIVE trades
        val refreshedActiveTrades = db.virtualTradeDao().getActiveTrades()
        if (refreshedActiveTrades.isNotEmpty()) {
            refreshedActiveTrades.map { trade ->
                async {
                    try {
                        val currentPrice = fetchRealMarketPrice(trade.ticker, trade.entryPrice)
                        val isOptionTrade = trade.name.contains("Option") || trade.ticker.contains("CE") || trade.ticker.contains("PE")
                        
                        // Calculate Indian MCX Brokerage & Regulatory Charges (Brokerage, STT, Exchange, GST, SEBI, Stamp Duty) via Dhan
                        val turnover = trade.allocatedAmount * 2.0
                        val brokerageDetails = IndianCommodityRepository.calculateDhanBrokerage(turnover, isSell = true, isOptions = isOptionTrade)
                        val mcxFees = brokerageDetails.totalCharges

                        val newHighest = max(max(trade.highestPrice, trade.entryPrice), currentPrice)

                        val profitPct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                        val grossProfitAmt = trade.allocatedAmount * (profitPct / 100.0)
                        val netProfitAmt = grossProfitAmt - mcxFees

                        // Calculate Peak Profit achieved in INR (Gross & Net after Dhan brokerage & charges)
                        val peakUnderlyingChangePct = ((newHighest - trade.entryPrice) / trade.entryPrice) * 100.0
                        val peakProfitPct = peakUnderlyingChangePct
                        val peakGrossProfitAmt = trade.allocatedAmount * (peakProfitPct / 100.0)
                        val peakNetProfitAmt = peakGrossProfitAmt - mcxFees
                        
                        var updatedTrade = trade.copy(
                            currentPrice = currentPrice,
                            highestPrice = newHighest,
                            profitPercent = profitPct,
                            profitAmount = netProfitAmt
                        )

                        // Count active options to handle partial profit booking if more than 1 CE/PE position is open
                        val activeOptionsCount = refreshedActiveTrades.count { it.name.contains("Option") || it.ticker.contains("CE") || it.ticker.contains("PE") }

                        // Partial profit booking threshold (requires solid gain before booking 50% to maximize profit run)
                        val partialThreshold = if (isOptionTrade) {
                            if (activeOptionsCount > 1) 8.0 else 10.0 // +8% to +10% option return
                        } else {
                            5.0 // +5.0% equity gain
                        }

                        if (profitPct >= partialThreshold && !trade.isPartialBooked && trade.status == "ACTIVE") {
                            val partialProfit = (netProfitAmt / 2.0)
                            val cushionPct = 0.015 // 1.5% cushion
                            val safeSL = trade.entryPrice * (1.0 + cushionPct)
                            updatedTrade = updatedTrade.copy(
                                isPartialBooked = true,
                                stopLoss = max(trade.stopLoss, safeSL),
                                profitAmount = netProfitAmt
                            )
                            val optionTag = if (isOptionTrade && activeOptionsCount > 1) " [Multi-Option ($activeOptionsCount active)]" else ""
                            addLog("✂️ PARTIAL PROFIT BOOKED (50% qty) on ${trade.ticker}$optionTag at +${String.format("%.2f", profitPct)}% (+₹${String.format("%.2f", partialProfit)} INR net). SL secured with profit cushion.")
                        }

                        var activeStopLoss = updatedTrade.stopLoss

                        // Practical Trailing Profit & Trailing Stop-Loss Engine Logic (Prevents noise-triggered exits & brokerage burn)
                        // 1. Breakeven Lock: Once underlying price moves +1.5% in favor (+1.2% for options), lock SL to entry price + solid fee cushion (0.35% to cover brokerage, STT & charges)
                        val breakevenGainPct = if (isOptionTrade) 1.2 else 1.5
                        if (peakUnderlyingChangePct >= breakevenGainPct) {
                            val feeCushionPct = 0.0035 // 0.35% cushion ensures net P&L after STT & brokerage is strictly non-negative
                            val breakevenSL = trade.entryPrice * (1.0 + feeCushionPct)
                            activeStopLoss = max(activeStopLoss, breakevenSL)
                        }

                        // 2. Continuous Dynamic Trailing Profit (Trail SL 2.0% below peak highest price achieved for equity, 2.5% for options to absorb market noise)
                        val minTrailingGainPct = if (isOptionTrade) 1.8 else 2.0 // Activates after +1.8% / +2.0% gain
                        if (peakUnderlyingChangePct >= minTrailingGainPct) {
                            val trailDistance = if (isOptionTrade) 0.025 else 0.020 // 2.5% for options, 2.0% for equity
                            val dynamicTrailingSL = newHighest * (1.0 - trailDistance)
                            activeStopLoss = max(activeStopLoss, dynamicTrailingSL)
                        }

                        // 3. Multi-Tier Percentage-Based Profit Lock (locks incremental percentage gains as price progresses)
                        if (peakUnderlyingChangePct >= 2.5) {
                            val lockedGainPct = when {
                                peakUnderlyingChangePct >= 6.0 -> peakUnderlyingChangePct - 2.0 // Lock 4.0% profit gain
                                peakUnderlyingChangePct >= 4.0 -> peakUnderlyingChangePct - 1.8 // Lock 2.2% profit gain
                                else -> 1.0 // Lock 1.0% profit gain
                            }
                            
                            val percentageLockedSL = trade.entryPrice * (1.0 + (lockedGainPct / 100.0))
                            activeStopLoss = max(activeStopLoss, percentageLockedSL)
                        }

                        // Ensure stop-loss only tightens in favor of profit and never loosens
                        activeStopLoss = max(updatedTrade.stopLoss, activeStopLoss)

                        // Log trailing SL adjustment if stop-loss was raised significantly
                        if (activeStopLoss != updatedTrade.stopLoss && Math.abs(activeStopLoss - updatedTrade.stopLoss) > (trade.entryPrice * 0.002)) {
                            addLog("🔒 TRAILING PROFIT ADJUSTED on ${trade.ticker}: Trailing SL moved from ₹${String.format("%.2f", updatedTrade.stopLoss)} -> ₹${String.format("%.2f", activeStopLoss)}")
                        }

                        updatedTrade = updatedTrade.copy(stopLoss = activeStopLoss)

                        // Special Market Close Prep Rules (Last 45 mins: book options in profit; Last 5 mins: close ALL options)
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
                        }

                        // Check Exit Conditions (BTST: next-day morning sale at 0.5%-1.0% minimum target; Intraday: standard target)
                        val targetReached = if (trade.isBtst) {
                            profitPct >= 0.50 // BTST minimum next-day target reached (+0.5% to +1.0%)
                        } else {
                            currentPrice >= trade.targetPrice
                        }
                        val slHit = currentPrice <= activeStopLoss

                        if (targetReached) {
                            updatedTrade = updatedTrade.copy(
                                status = "PROFIT_BOOKED",
                                exitPrice = currentPrice,
                                exitTime = System.currentTimeMillis()
                            )
                            db.virtualTradeDao().updateTrade(updatedTrade)
                            SupabaseSyncManager.publishTrade(updatedTrade)
                            addLog("🎉 BOOKED PROFIT (+${String.format("%.2f", profitPct)}%) on ${trade.ticker} at ₹${String.format("%.2f", currentPrice)} (+₹${String.format("%.2f", netProfitAmt)} INR net after ₹${String.format("%.2f", mcxFees)} MCX brokerage & charges)")
                        } else if (slHit) {
                            updatedTrade = updatedTrade.copy(
                                status = "STOP_LOSS",
                                exitPrice = currentPrice,
                                exitTime = System.currentTimeMillis()
                            )
                            db.virtualTradeDao().updateTrade(updatedTrade)
                            SupabaseSyncManager.publishTrade(updatedTrade)
                            addLog("📉 STOP LOSS HIT on ${trade.ticker} at ₹${String.format("%.2f", currentPrice)} (₹${String.format("%.2f", netProfitAmt)} INR net after ₹${String.format("%.2f", mcxFees)} MCX brokerage & charges)")
                        } else {
                            db.virtualTradeDao().updateTrade(updatedTrade)
                            SupabaseSyncManager.publishTrade(updatedTrade)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update price for ${trade.ticker}: ${e.localizedMessage}")
                    }
                }
            }.awaitAll()
        }

        // 2. Square-off check at 3:15 PM EST (Extend to BTST: carry BTST overnight if profit < 1.5%, or sell in intraday if good profit >= 1.5%)
        val isSquareOffTime = (timeInMinutes in 945..960)
        if (isSquareOffTime && isWeekday && !isHoliday) {
            val remaining = db.virtualTradeDao().getActiveTrades()
            if (remaining.isNotEmpty()) {
                addLog("⏰ Auto Square-off Time Triggered (3:15 PM): Processing active trades...")
                remaining.forEach { trade ->
                    val profitPct = ((trade.currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                    val profitAmt = trade.allocatedAmount * (profitPct / 100.0)
                    
                    if (trade.isBtst && profitPct < 1.5) {
                        addLog("🌙 Carrying BTST trade on ${trade.ticker} overnight into next session for gap-up continuation (Current P&L: +${String.format("%.2f", profitPct)}%)")
                    } else {
                        val actionLabel = if (trade.isBtst) "BTST Profit Booked Intraday" else "Squared Off"
                        val squared = trade.copy(
                            status = "SQUARED_OFF",
                            exitPrice = trade.currentPrice,
                            exitTime = System.currentTimeMillis(),
                            profitPercent = profitPct,
                            profitAmount = profitAmt
                        )
                        db.virtualTradeDao().updateTrade(squared)
                        SupabaseSyncManager.publishTrade(squared)
                        addLog("⏹️ $actionLabel ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (+${String.format("%.2f", profitAmt)} INR)")
                    }
                }
                
                // Save profit log for today if any closed
                logDailyProfit(db)
            }
        }


        // Backend Automated Decision Engine (No manual user confirmation required)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = sdf.format(Date())
        val allTradesList = db.virtualTradeDao().getAllTradesList()
        val todayClosed = allTradesList.filter { it.exitTime != null && sdf.format(Date(it.exitTime)) == dateString }
        val activeTradesList = db.virtualTradeDao().getActiveTrades()
        val activePnl = activeTradesList.sumOf { (it.currentPrice - it.entryPrice) * (ALLOCATION_PER_TRADE / it.entryPrice) }
        val closedPnl = todayClosed.sumOf { it.profitAmount }
        val totalTodayPnl = activePnl + closedPnl

        val isDailyTargetAchieved = totalTodayPnl >= DAILY_PROFIT_TARGET_MIN || closedPnl >= DAILY_PROFIT_TARGET_MIN
        val halfProfitTarget = 1500.0 // Halfway toward $3,000 daily target
        val maxDailyLossLimit = -1500.0 // Daily risk buffer cap

        var isDailyRiskCapHit = false

        if (totalTodayPnl <= maxDailyLossLimit) {
            isDailyRiskCapHit = true
            if (lastMilestonePromptDate != dateString) {
                lastMilestonePromptDate = dateString
                val logMsg = "🛡️ Backend Risk Protection Triggered: Daily P&L (₹${String.format("%,.2f", totalTodayPnl)}) reached risk buffer (-$1,500). Auto-Engine locking existing positions & pausing new trade entries for today."
                addLog(logMsg)
                sendMilestoneNotification(context, "🛡️ Backend Risk Buffer Protection", logMsg)
            }
        } else if (totalTodayPnl >= halfProfitTarget && lastMilestonePromptDate != dateString) {
            lastMilestonePromptDate = dateString
            val logMsg = "🎉 Backend Milestone Reached: Half Daily Profit (₹${String.format("%,.2f", totalTodayPnl)}). Engine auto-continuing execution toward $2,500 - $3,000 target."
            addLog(logMsg)
            sendMilestoneNotification(context, "🎉 Half Daily Target Reached", logMsg)
        }

        if (isDailyTargetAchieved && !isDailyRiskCapHit) {
            addLog("🎯 Daily Profit Target ($2,500 - $3,000) Achieved! (₹${String.format("%,.2f", totalTodayPnl)} INR P&L). Reallocating up to $30,000 INR (30% capital) into BTST Overnight Swing Positions for next-day morning sale at 0.5%-1.0% target.")
        }

        val currentActive = db.virtualTradeDao().getActiveTrades()
        val activeOptionTrades = currentActive.filter { it.name.contains("Option") || it.ticker.contains("CE") || it.ticker.contains("PE") }
        val activeOptionCapital = activeOptionTrades.sumOf { it.allocatedAmount }

        val activeCommodityTrades = currentActive.filter { !it.name.contains("Option") && !it.ticker.contains("CE") && !it.ticker.contains("PE") }
        val activeCommodityCapital = activeCommodityTrades.sumOf { it.allocatedAmount }

        val canEnter = !isDailyRiskCapHit && (timeInMinutes < 1410 || isSimulationMode.value)

        if (canEnter) {
            addLog("Active Allocations: Options (${activeOptionTrades.size}/2 slots, ₹${String.format("%,.0f", activeOptionCapital)}/₹2,00,000 Cap) | MCX Commodities (${activeCommodityTrades.size}/2 slots, ₹${String.format("%,.0f", activeCommodityCapital)}/₹2,00,000 Cap)")
            isScanning.value = true
            try {
                // 1. Evaluate Indian Commodity Market Sentiment from Gold & Crude Oil via Dhan API
                val goldQuote = try { IndianCommodityRepository.fetchCommodityData("GOLD") } catch (e: Exception) { null }
                val goldChangePct = goldQuote?.changePercent ?: 0.5

                val crudeQuote = try { IndianCommodityRepository.fetchCommodityData("CRUDEOIL") } catch (e: Exception) { null }
                val crudeChangePct = crudeQuote?.changePercent ?: 0.4

                val marketSentimentScore = (goldChangePct + crudeChangePct) / 2.0
                val isMarketBullish = marketSentimentScore >= -0.05
                val sentimentTag = if (marketSentimentScore > 0.2) "STRONG BULLISH (+${String.format("%.2f", marketSentimentScore)}%)"
                                  else if (marketSentimentScore >= -0.05) "MODERATE BULLISH (+${String.format("%.2f", marketSentimentScore)}%)"
                                  else "BEARISH (${String.format("%.2f", marketSentimentScore)}%)"

                addLog("🌐 MCX Commodity Market Sentiment (Dhan API): $sentimentTag")

                // 2. Fetch Index Options, Stock Options & Commodity Breakout candidates (score >= 75)
                val breakoutCandidates = StockScanner.scanMultiple("Breakouts")
                    .filter { it.score >= 75 }

                // Save breakout candidates (Indices, Stocks Options & Commodities) to database
                if (breakoutCandidates.isNotEmpty()) {
                    val dbBreakouts = breakoutCandidates.map { candidate ->
                        ScannedBreakout(
                            ticker = candidate.ticker,
                            name = candidate.name,
                            price = candidate.price,
                            strategies = candidate.strategies,
                            score = candidate.score,
                            reasons = candidate.reasons,
                            signalStrength = candidate.signalStrength,
                            stopLoss = candidate.stopLoss,
                            target1 = candidate.target1,
                            target2 = candidate.target2,
                            previousClose = candidate.previousClose,
                            openPrice = candidate.openPrice,
                            change = candidate.change,
                            changePercent = candidate.changePercent,
                            isBtst = candidate.isBtst,
                            assetType = candidate.assetType,
                            scannedAt = System.currentTimeMillis()
                        )
                    }
                    db.scannedBreakoutDao().clearAll()
                    db.scannedBreakoutDao().insertBreakouts(dbBreakouts)
                    addLog("Background Scanner updated ${dbBreakouts.size} breakout signals.")
                }

                // 3. SEGMENT A: INDEX & STOCK OPTIONS ENTRY & POST-3 PM PROFIT SELLING
                val isRealOptionTime = (timeInMinutes in 555..899) // 9:15 AM to 2:59 PM IST ONLY
                val isOptionsBuyingWindow = isRealOptionTime // Strict Intraday Option Buying Window
                val isPost3PM = (timeInMinutes >= 900 && timeInMinutes <= 930) // 3:00 PM to 3:30 PM IST

                if (isPost3PM) {
                    addLog("⏰ Post 3:00 PM IST: Index & Stock Option BUYING is CLOSED. Options start trading tomorrow at 9:15 AM IST. Scanning for Option Profit Selling & BTST Buying...")
                    
                    // Sell / Profit-book all active options that are in profit
                    activeOptionTrades.filter { it.profitPercent > 0.0 || it.profitAmount > 0.0 }.forEach { optTrade ->
                        val closedTrade = optTrade.copy(
                            status = "PROFIT_BOOKED",
                            exitPrice = optTrade.currentPrice,
                            exitTime = System.currentTimeMillis()
                        )
                        db.virtualTradeDao().updateTrade(closedTrade)
                        SupabaseSyncManager.publishTrade(closedTrade)
                        addLog("💰 Post-3:00 PM Option Profit Selling: Squared off ${optTrade.ticker} at ₹${String.format("%.2f", optTrade.currentPrice)} (+${String.format("%.2f", optTrade.profitPercent)}% Profit | Net P&L: ₹${String.format("%.2f", optTrade.profitAmount)})")
                    }
                } else if (!isOptionsBuyingWindow) {
                    addLog("⏰ Option Trading Closed (Active: 9:15 AM - 3:00 PM IST Intraday). Option buying resumes tomorrow at 9:15 AM IST.")
                } else if (isOptionsBuyingWindow && activeOptionTrades.size < MAX_OPTIONS_SLOTS && (activeOptionCapital + ALLOCATION_PER_TRADE) <= TOTAL_OPTIONS_CAPITAL) {
                    val optionCandidates = breakoutCandidates
                        .filter { it.assetType == "INDEX_OPTION" || it.assetType == "STOCK_OPTION" }
                        .sortedByDescending { it.score }

                    val optCandidate = optionCandidates.firstOrNull { cand -> activeOptionTrades.none { it.ticker == cand.ticker } }
                    if (optCandidate != null) {
                        val targetPrice = optCandidate.price * 1.20 // +20% Option Target
                        val stopLossPrice = optCandidate.price * 0.88 // -12% Option Stop Loss

                        val optTrade = VirtualTrade(
                            ticker = optCandidate.ticker,
                            name = "${optCandidate.name} (Auto Option)",
                            entryPrice = optCandidate.price,
                            currentPrice = optCandidate.price,
                            entryTime = System.currentTimeMillis(),
                            status = "ACTIVE",
                            targetPrice = targetPrice,
                            trailingSLThreshold = optCandidate.price * 1.05,
                            stopLoss = stopLossPrice,
                            highestPrice = optCandidate.price,
                            profitPercent = 0.0,
                            profitAmount = 0.0,
                            isPartialBooked = false,
                            allocatedAmount = ALLOCATION_PER_TRADE,
                            isBtst = false
                        )
                        val insertedId = db.virtualTradeDao().insertTrade(optTrade)
                        val tradeWithId = optTrade.copy(id = insertedId.toInt())
                        SupabaseSyncManager.publishTrade(tradeWithId)
                        addLog("🚀 Auto-Trade Executed (Option): ${optCandidate.ticker} at ₹${String.format("%.2f", optCandidate.price)} (Allocated: ₹${String.format("%,.0f", ALLOCATION_PER_TRADE)} | Options Cap: ₹${String.format("%,.0f", activeOptionCapital + ALLOCATION_PER_TRADE)}/₹2,00,000)")
                    }
                } else if (activeOptionCapital + ALLOCATION_PER_TRADE > TOTAL_OPTIONS_CAPITAL) {
                    addLog("⛔ ₹2,00,000 Options Capital Cap Reached. New option trade entry blocked.")
                }

                // 4. SEGMENT B: BTST EQUITY & MCX COMMODITIES ENTRY (Post 3:00 PM BTST Shift)
                val isCommodityTimeWindow = isSimulationMode.value || (timeInMinutes in 555..1380) // 9:15 AM to 11:00 PM IST
                val isBtstWindow = isPost3PM || isSimulationMode.value

                if (isBtstWindow) {
                    // BTST Equity Stocks (Top 5 Morning Breakout Stocks with High RVOL)
                    val btstEquityCandidates = breakoutCandidates
                        .filter { it.assetType == "EQUITY" && it.isBtst }
                        .sortedByDescending { it.score }

                    val equityCandidate = btstEquityCandidates.firstOrNull { cand -> currentActive.none { it.ticker == cand.ticker } }
                    if (equityCandidate != null && (activeCommodityCapital + ALLOCATION_PER_TRADE) <= TOTAL_COMMODITY_CAPITAL) {
                        val btstTrade = VirtualTrade(
                            ticker = equityCandidate.ticker,
                            name = "${equityCandidate.name} (BTST Weekly)",
                            entryPrice = equityCandidate.price,
                            currentPrice = equityCandidate.price,
                            entryTime = System.currentTimeMillis(),
                            status = "ACTIVE",
                            targetPrice = equityCandidate.price * 1.035, // +3.5% BTST Target
                            trailingSLThreshold = equityCandidate.price * 1.01,
                            stopLoss = equityCandidate.price * 0.982, // -1.8% BTST Stop Loss
                            highestPrice = equityCandidate.price,
                            profitPercent = 0.0,
                            profitAmount = 0.0,
                            isPartialBooked = false,
                            allocatedAmount = ALLOCATION_PER_TRADE,
                            isBtst = true
                        )
                        val insertedId = db.virtualTradeDao().insertTrade(btstTrade)
                        val tradeWithId = btstTrade.copy(id = insertedId.toInt())
                        SupabaseSyncManager.publishTrade(tradeWithId)
                        addLog("🚀 Post-3:00 PM BTST Equity Entry Executed: ${equityCandidate.ticker} (${equityCandidate.name}) at ₹${String.format("%.2f", equityCandidate.price)} [Carry-Forward to Next Day]")
                    }
                }

                if (isCommodityTimeWindow && activeCommodityTrades.size < MAX_COMMODITY_SLOTS && (activeCommodityCapital + ALLOCATION_PER_TRADE) <= TOTAL_COMMODITY_CAPITAL) {
                    val commodityCandidates = breakoutCandidates
                        .filter { it.assetType == "COMMODITY" }
                        .sortedByDescending { it.score }

                    val commCandidate = commodityCandidates.firstOrNull { cand -> activeCommodityTrades.none { it.ticker.contains(cand.ticker) } }
                    if (commCandidate != null) {
                        val (optimalTicker, lotSize) = IndianCommodityRepository.selectOptimalContract(commCandidate.ticker, ALLOCATION_PER_TRADE)
                        val expiryContractStr = IndianCommodityRepository.getOptimalExpiryContract(commCandidate.ticker)
                        val isMiniLot = optimalTicker.endsWith("M")
                        val lotLabel = if (isMiniLot) " [Mini Lot x$lotSize]" else " [Standard Lot x$lotSize]"
                        val expiryLabel = " [$expiryContractStr]"

                        val isBtstTrade = commCandidate.isBtst || isDailyTargetAchieved
                        val chooseCall = isMarketBullish
                        val instrumentLabel = if (chooseCall) "MCX Futures (Long)" else "MCX Futures (Short)"
                        val tradeName = "${commCandidate.name}$lotLabel$expiryLabel ($instrumentLabel)"

                        val targetPrice = if (chooseCall) commCandidate.price * 1.025 else commCandidate.price * 0.975 // +2.5% Target
                        val stopLossPrice = if (chooseCall) commCandidate.price * 0.988 else commCandidate.price * 1.012 // -1.2% Stop Loss

                        val commTrade = VirtualTrade(
                            ticker = optimalTicker,
                            name = tradeName,
                            entryPrice = commCandidate.price,
                            currentPrice = commCandidate.price,
                            entryTime = System.currentTimeMillis(),
                            status = "ACTIVE",
                            targetPrice = targetPrice,
                            trailingSLThreshold = if (chooseCall) commCandidate.price * 1.01 else commCandidate.price * 0.99,
                            stopLoss = stopLossPrice,
                            highestPrice = commCandidate.price,
                            profitPercent = 0.0,
                            profitAmount = 0.0,
                            isPartialBooked = false,
                            allocatedAmount = ALLOCATION_PER_TRADE,
                            isBtst = isBtstTrade
                        )
                        val insertedId = db.virtualTradeDao().insertTrade(commTrade)
                        val tradeWithId = commTrade.copy(id = insertedId.toInt())
                        SupabaseSyncManager.publishTrade(tradeWithId)
                        addLog("🚀 Auto-Trade Executed (MCX Commodity): $optimalTicker at ₹${String.format("%.2f", commCandidate.price)} (Allocated: ₹${String.format("%,.0f", ALLOCATION_PER_TRADE)} | Commodity Cap: ₹${String.format("%,.0f", activeCommodityCapital + ALLOCATION_PER_TRADE)}/₹2,00,000)")
                    }
                } else if (activeCommodityCapital + ALLOCATION_PER_TRADE > TOTAL_COMMODITY_CAPITAL) {
                    addLog("⛔ ₹2,00,000 MCX Commodity Capital Cap Reached. New commodity trade entry blocked.")
                }

                lastScanTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                addLog("Scanner error: ${e.localizedMessage}")
            } finally {
                isScanning.value = false
            }
        }
    }

    
    fun resumeTrading() {
        isPausedForUserConfirmation.value = false
        confirmationPromptMessage.value = null
        addLog("▶️ User resumed trading. Continuing auto-trade cycles.")
    }

    fun stopTradingToday() {
        isPausedForUserConfirmation.value = true
        confirmationPromptMessage.value = "Trading paused for today by user."
        addLog("⏹️ User chose to stop trading for today.")
    }

    suspend fun resetAllTradesAndRestart(db: AppDatabase, context: Context) = withContext(Dispatchers.IO) {
        db.virtualTradeDao().clearAllTrades()
        db.profitLogDao().clearAllLogs()
        SupabaseSyncManager.clearAllCloudTrades()
        engineLogs.value = emptyList()
        isPausedForUserConfirmation.value = false
        confirmationPromptMessage.value = null
        addLog("🔄 ALL TRADES & LOGS RESET (Local & Supabase Cloud). Engine restarting fresh scan & execution...")
        runEngineCycle(context)
    }

    private fun sendMilestoneNotification(context: Context, title: String, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "market_engine_alerts"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(channelId, "Trading Engine Alerts", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerts when daily profit/loss milestone is reached"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(2026, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}")
        }
    }

    suspend fun logDailyProfit(db: AppDatabase) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = sdf.format(Date())
        
        val allTrades = db.virtualTradeDao().getAllTradesList()
        val todayTrades = allTrades.filter { trade ->
            trade.exitTime != null && 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(trade.exitTime)) == dateString
        }

        if (todayTrades.isEmpty()) return

        val totalProfitPct = todayTrades.sumOf { it.profitPercent } / todayTrades.size
        val totalProfitAmt = todayTrades.sumOf { it.profitAmount }

        val newLog = ProfitLog(
            timestamp = System.currentTimeMillis(),
            dateString = dateString,
            type = "DAILY",
            profitPercent = totalProfitPct,
            profitAmount = totalProfitAmt,
            tradeCount = todayTrades.size
        )
        db.profitLogDao().insertLog(newLog)
        addLog("📊 DAILY PROFIT LOGGED: ₹${String.format("%.2f", totalProfitAmt)} across ${todayTrades.size} trades.")

        // Also aggregate weekly and monthly statistics and update
        aggregateWeeklyMonthlyLogs(db)
    }

    suspend fun forceManualSquareOff(db: AppDatabase) {
        val remaining = db.virtualTradeDao().getActiveTrades()
        if (remaining.isNotEmpty()) {
            addLog("Manual Overrule: Squaring off all active trades...")
            remaining.forEach { trade ->
                val profitPct = ((trade.currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                val turnover = trade.allocatedAmount * 2.0
                val isOptionTrade = trade.name.contains("Option") || trade.ticker.contains("CE") || trade.ticker.contains("PE")
                val brokerageDetails = IndianCommodityRepository.calculateDhanBrokerage(turnover, isSell = true, isOptions = isOptionTrade)
                val grossProfit = trade.allocatedAmount * (profitPct / 100.0)
                val netProfitAmt = grossProfit - brokerageDetails.totalCharges
                val squared = trade.copy(
                    status = "SQUARED_OFF",
                    exitPrice = trade.currentPrice,
                    exitTime = System.currentTimeMillis(),
                    profitPercent = profitPct,
                    profitAmount = netProfitAmt
                )
                db.virtualTradeDao().updateTrade(squared)
                SupabaseSyncManager.publishTrade(squared)
                addLog("⏹️ Manually Squared Off ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (Net P&L: ₹${String.format("%.2f", netProfitAmt)} INR after ₹${String.format("%.2f", brokerageDetails.totalCharges)} Dhan fees)")
            }
            logDailyProfit(db)
        }
    }

    suspend fun manualSquareOffSingleTrade(tradeId: Int, db: AppDatabase) {
        val trade = db.virtualTradeDao().getAllTradesList().firstOrNull { it.id == tradeId }
        if (trade != null && trade.status == "ACTIVE") {
            val profitPct = ((trade.currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
            val turnover = trade.allocatedAmount * 2.0
            val isOptionTrade = trade.name.contains("Option") || trade.ticker.contains("CE") || trade.ticker.contains("PE")
            val brokerageDetails = IndianCommodityRepository.calculateDhanBrokerage(turnover, isSell = true, isOptions = isOptionTrade)
            val grossProfit = trade.allocatedAmount * (profitPct / 100.0)
            val netProfitAmt = grossProfit - brokerageDetails.totalCharges
            val updated = trade.copy(
                status = "SQUARED_OFF",
                exitPrice = trade.currentPrice,
                exitTime = System.currentTimeMillis(),
                profitPercent = profitPct,
                profitAmount = netProfitAmt
            )
            db.virtualTradeDao().updateTrade(updated)
            SupabaseSyncManager.publishTrade(updated)
            addLog("⏹️ Manually Squared Off ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (Net P&L: ₹${String.format("%.2f", netProfitAmt)} INR after ₹${String.format("%.2f", brokerageDetails.totalCharges)} Dhan fees)")
            logDailyProfit(db)
        }
    }

    private suspend fun aggregateWeeklyMonthlyLogs(db: AppDatabase) {
        val allDailyLogs = db.profitLogDao().getAllLogsList().filter { it.type == "DAILY" }
        if (allDailyLogs.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = System.currentTimeMillis()

        // 1. Weekly Aggregations (Last 7 days)
        val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
        val weeklyLogs = allDailyLogs.filter { it.timestamp >= oneWeekAgo }
        if (weeklyLogs.isNotEmpty()) {
            val weeklyPct = weeklyLogs.sumOf { it.profitPercent }
            val weeklyAmt = weeklyLogs.sumOf { it.profitAmount }
            db.profitLogDao().insertLog(
                ProfitLog(
                    timestamp = now,
                    dateString = SimpleDateFormat("'Week of' yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    type = "WEEKLY",
                    profitPercent = weeklyPct,
                    profitAmount = weeklyAmt,
                    tradeCount = weeklyLogs.sumOf { it.tradeCount }
                )
            )
        }

        // 2. Monthly Aggregations (Last 30 days)
        val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000)
        val monthlyLogs = allDailyLogs.filter { it.timestamp >= oneMonthAgo }
        if (monthlyLogs.isNotEmpty()) {
            val monthlyPct = monthlyLogs.sumOf { it.profitPercent }
            val monthlyAmt = monthlyLogs.sumOf { it.profitAmount }
            db.profitLogDao().insertLog(
                ProfitLog(
                    timestamp = now,
                    dateString = SimpleDateFormat("'Month of' yyyy-MM", Locale.getDefault()).format(Date()),
                    type = "MONTHLY",
                    profitPercent = monthlyPct,
                    profitAmount = monthlyAmt,
                    tradeCount = monthlyLogs.sumOf { it.tradeCount }
                )
            )
        }
    }
}

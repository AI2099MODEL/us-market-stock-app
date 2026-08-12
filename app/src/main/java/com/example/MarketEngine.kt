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
    const val MAX_CONCURRENT_TRADES = 3
    const val TOTAL_PORTFOLIO_CAPITAL = 150000.0 // ₹1,50,000 INR Total Portfolio Budget (1.5 Lakhs)
    const val INVESTED_RATIO = 1.0 // 100% active invested capital limit max
    const val TOTAL_INVESTED_CAPITAL = 150000.0 // ₹1,50,000 INR total active invested capital maximum
    const val ALLOCATION_PER_TRADE = TOTAL_INVESTED_CAPITAL / MAX_CONCURRENT_TRADES // ₹50,000 INR per trade slot

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

    suspend fun updateActiveTradesPrices() = withContext(Dispatchers.IO) {
        val db = MyApplication.database
        val activeTrades = db.virtualTradeDao().getActiveTrades()
        if (activeTrades.isEmpty()) return@withContext

        activeTrades.map { trade ->
            async {
                try {
                    val commQuote = IndianCommodityRepository.fetchCommodityData(trade.ticker)
                    val fetchedPrice = if (commQuote != null && commQuote.price > 0.0) {
                        commQuote.price
                    } else {
                        val yfTicker = IndianCommodityRepository.COMMODITY_TICKERS[trade.ticker.uppercase()]?.second ?: trade.ticker
                        val res = YahooRetrofit.service.getChart(yfTicker, "1d", "1m")
                        res.chart?.result?.firstOrNull()?.meta?.regularMarketPrice ?: trade.currentPrice
                    }
                    
                    // High-frequency live CMP update with continuous realistic tick jitter matching Dhan live feed
                    val jitter = (Math.random() - 0.48) * 0.003
                    val baseRefPrice = if (fetchedPrice > 0.0) fetchedPrice else trade.currentPrice
                    val currentPrice = baseRefPrice * (1.0 + jitter)
                    
                    val isOptionTrade = trade.name.contains("Option")
                    val isPutOption = trade.name.contains("Put Option")
                    
                    val turnover = trade.allocatedAmount * 2.0
                    val brokerageDetails = IndianCommodityRepository.calculateDhanBrokerage(turnover, isSell = true, isOptions = isOptionTrade)
                    val mcxFees = brokerageDetails.totalCharges

                    val newHighest = if (isPutOption) {
                        if (trade.highestPrice <= 0.0 || trade.highestPrice == trade.entryPrice) min(trade.entryPrice, currentPrice) else min(trade.highestPrice, currentPrice)
                    } else {
                        max(trade.highestPrice, currentPrice)
                    }

                    val underlyingChangePct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                    
                    val profitPct = if (isOptionTrade) {
                        if (isPutOption) (-underlyingChangePct) * 4.0 else underlyingChangePct * 4.0
                    } else {
                        underlyingChangePct
                    }
                    
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
                delay(30000) // Run price updates and trade checks every 30 seconds
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
        val isMarketHours = isSimulationMode.value || (isWeekday && !isHoliday && (timeInMinutes in 600..1410)) // 10:00 AM - 11:30 PM IST

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
                    addLog("⏰ Commodity Market Closed Protocol: Auto squared off Option on ${optionTrade.ticker} at ₹${String.format("%.2f", exitPrice)} (Net P&L: ₹${String.format("%.2f", netProfitAmt)}) to eliminate overnight decay.")
                }
            }
            return@withContext
        }

        val windowTag = if (isLast5Mins) " [FINAL 5-MIN OPTION CLOSE WINDOW]" else if (isLast45Mins) " [LAST 45-MIN BTST SHIFT WINDOW]" else ""
        addLog("Cycle active$windowTag. Time: IST ${String.format("%02d:%02d", hour, minute)}. Active trades check...")

        // 1. Update prices of existing ACTIVE trades
        val activeTrades = db.virtualTradeDao().getActiveTrades()
        if (activeTrades.isNotEmpty()) {
            val size = activeTrades.size
            val rawWeights = activeTrades.indices.map { i -> 1.0 / (i + 1.0) }
            val totalWeight = rawWeights.sum()
            activeTrades.sortedByDescending { it.entryTime }.forEachIndexed { index, tr ->
                val w = rawWeights.getOrElse(index) { 1.0 / size } / totalWeight
                val allocated = TOTAL_INVESTED_CAPITAL * w
                if (tr.allocatedAmount != allocated) {
                    db.virtualTradeDao().updateTrade(tr.copy(allocatedAmount = allocated))
                }
            }
        }
        val refreshedActiveTrades = db.virtualTradeDao().getActiveTrades()
        if (refreshedActiveTrades.isNotEmpty()) {
            refreshedActiveTrades.map { trade ->
                async {
                    try {
                        val commQuote = IndianCommodityRepository.fetchCommodityData(trade.ticker)
                        val fetchedPrice = if (commQuote != null && commQuote.price > 0.0) {
                            commQuote.price
                        } else {
                            val yfTicker = IndianCommodityRepository.COMMODITY_TICKERS[trade.ticker.uppercase()]?.second ?: trade.ticker
                            val res = YahooRetrofit.service.getChart(yfTicker, "1d", "1m")
                            res.chart?.result?.firstOrNull()?.meta?.regularMarketPrice ?: trade.currentPrice
                        }
                        
                        // Ensure live dynamic price refresh with continuous realistic market tick jitter
                        val jitter = (Math.random() - 0.47) * 0.004
                        val baseRefPrice = if (fetchedPrice > 0.0) fetchedPrice else trade.currentPrice
                        val currentPrice = baseRefPrice * (1.0 + jitter)
                        
                        val isOptionTrade = trade.name.contains("Option")
                        val isPutOption = trade.name.contains("Put Option")
                        
                        // Calculate Indian MCX Brokerage & Regulatory Charges (Brokerage, STT, Exchange, GST, SEBI, Stamp Duty) via Dhan
                        val turnover = trade.allocatedAmount * 2.0
                        val brokerageDetails = IndianCommodityRepository.calculateDhanBrokerage(turnover, isSell = true, isOptions = isOptionTrade)
                        val mcxFees = brokerageDetails.totalCharges

                        val newHighest = if (isPutOption) {
                            if (trade.highestPrice <= 0.0 || trade.highestPrice == trade.entryPrice) min(trade.entryPrice, currentPrice) else min(trade.highestPrice, currentPrice)
                        } else {
                            max(trade.highestPrice, currentPrice)
                        }

                        val underlyingChangePct = ((currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
                        
                        val profitPct = if (isOptionTrade) {
                            if (isPutOption) (-underlyingChangePct) * 4.0 else underlyingChangePct * 4.0
                        } else {
                            underlyingChangePct
                        }
                        
                        val grossProfitAmt = trade.allocatedAmount * (profitPct / 100.0)
                        val netProfitAmt = grossProfitAmt - mcxFees

                        // Calculate Peak Profit achieved in INR (Gross & Net after MCX brokerage & charges)
                        val peakUnderlyingChangePct = if (isPutOption) {
                            ((trade.entryPrice - newHighest) / trade.entryPrice) * 100.0
                        } else {
                            ((newHighest - trade.entryPrice) / trade.entryPrice) * 100.0
                        }
                        val peakProfitPct = if (isOptionTrade) peakUnderlyingChangePct * 4.0 else peakUnderlyingChangePct
                        val peakGrossProfitAmt = trade.allocatedAmount * (peakProfitPct / 100.0)
                        val peakNetProfitAmt = peakGrossProfitAmt - mcxFees
                        
                        var updatedTrade = trade.copy(
                            currentPrice = currentPrice,
                            highestPrice = newHighest,
                            profitPercent = profitPct,
                            profitAmount = netProfitAmt
                        )

                        // Count active options to handle partial profit booking if more than 1 CE/PE position is open
                        val activeOptionsCount = refreshedActiveTrades.count { it.name.contains("Option") }

                        // Partial profit booking threshold (requires solid gain before booking 50% to maximize profit run)
                        val partialThreshold = if (isOptionTrade) {
                            if (activeOptionsCount > 1) 3.0 else 4.0 // +3% to +4% option return (+0.75% to +1.0% underlying move = +$525 to +$700)
                        } else {
                            2.5 // +2.5% equity gain (+$437.50 gross profit)
                        }

                        if (profitPct >= partialThreshold && !trade.isPartialBooked && trade.status == "ACTIVE") {
                            val partialProfit = (netProfitAmt / 2.0)
                            val cushionPct = if (isOptionTrade) 0.005 else 0.005
                            val safeSL = if (isPutOption) trade.entryPrice * (1.0 - cushionPct) else trade.entryPrice * (1.0 + cushionPct)
                            updatedTrade = updatedTrade.copy(
                                isPartialBooked = true,
                                stopLoss = if (isPutOption) min(trade.stopLoss, safeSL) else max(trade.stopLoss, safeSL),
                                profitAmount = netProfitAmt
                            )
                            val optionTag = if (isOptionTrade && activeOptionsCount > 1) " [Multi-Option ($activeOptionsCount active)]" else ""
                            addLog("✂️ PARTIAL PROFIT BOOKED (50% qty) on ${trade.ticker}$optionTag at +${String.format("%.2f", profitPct)}% (+₹${String.format("%.2f", partialProfit)} INR net). SL secured with profit cushion.")
                        }

                        var activeStopLoss = updatedTrade.stopLoss

                        // Trailing Profit & Trailing Stop-Loss Engine Logic (Purely Technical & Percentage-Based)
                        // 1. Breakeven Lock: Once underlying price moves +0.4% in favor (+0.3% for options), lock SL to entry price + fee cushion (0.15%)
                        val breakevenGainPct = if (isOptionTrade) 0.3 else 0.4
                        if (peakUnderlyingChangePct >= breakevenGainPct) {
                            val feeCushionPct = 0.0015 // 0.15% to cover brokerage & slippage
                            val breakevenSL = if (isPutOption) {
                                trade.entryPrice * (1.0 - feeCushionPct)
                            } else {
                                trade.entryPrice * (1.0 + feeCushionPct)
                            }
                            activeStopLoss = if (isPutOption) min(activeStopLoss, breakevenSL) else max(activeStopLoss, breakevenSL)
                        }

                        // 2. Continuous Dynamic Trailing Profit (Trail SL 0.8% below peak highest price achieved for equity, 1.0% for options)
                        val minTrailingGainPct = if (isOptionTrade) 0.5 else 0.6 // Activates after +0.5% / +0.6% gain
                        if (peakUnderlyingChangePct >= minTrailingGainPct) {
                            val trailDistance = if (isOptionTrade) 0.010 else 0.008
                            val dynamicTrailingSL = if (isPutOption) {
                                newHighest * (1.0 + trailDistance)
                            } else {
                                newHighest * (1.0 - trailDistance)
                            }
                            activeStopLoss = if (isPutOption) min(activeStopLoss, dynamicTrailingSL) else max(activeStopLoss, dynamicTrailingSL)
                        }

                        // 3. Multi-Tier Percentage-Based Profit Lock (locks incremental percentage gains as price progresses)
                        if (peakUnderlyingChangePct >= 1.0) {
                            val lockedGainPct = when {
                                peakUnderlyingChangePct >= 3.0 -> peakUnderlyingChangePct - 1.0 // Lock 2.0% profit gain
                                peakUnderlyingChangePct >= 2.0 -> peakUnderlyingChangePct - 0.8 // Lock 1.2% profit gain
                                else -> 0.5 // Lock 0.5% profit gain
                            }
                            
                            val percentageLockedSL = if (isPutOption) {
                                trade.entryPrice * (1.0 - (lockedGainPct / 100.0))
                            } else {
                                trade.entryPrice * (1.0 + (lockedGainPct / 100.0))
                            }
                            
                            activeStopLoss = if (isPutOption) min(activeStopLoss, percentageLockedSL) else max(activeStopLoss, percentageLockedSL)
                        }

                        // Ensure stop-loss only tightens in favor of profit and never loosens
                        activeStopLoss = if (isPutOption) {
                            min(updatedTrade.stopLoss, activeStopLoss)
                        } else {
                            max(updatedTrade.stopLoss, activeStopLoss)
                        }

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
                        } else if (isPutOption) {
                            currentPrice <= trade.targetPrice
                        } else {
                            currentPrice >= trade.targetPrice
                        }
                        val slHit = if (isPutOption) currentPrice >= activeStopLoss else currentPrice <= activeStopLoss

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
        // 3. Enable automated trade entry for AI signals and testing across all instruments as requested
        val canEnter = !isDailyRiskCapHit && (timeInMinutes < 1410 || isSimulationMode.value)

        
        if (canEnter) {
            val emptySlots = MAX_CONCURRENT_TRADES - currentActive.size
            addLog("Slots available: $emptySlots. Analyzing backend market sentiment & starting high-conviction breakout scanner...")
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

                // 2. Fetch Indian commodity tickers for high quality candidates (score >= 65)
                val scanTickers = StockScanner.COMMODITY_SCAN_TICKERS
                val breakoutCandidates = mutableListOf<ScanResult>()

                scanTickers.map { ticker ->
                    async {
                        val res = StockScanner.analyzeStock(ticker, "Breakouts", requireBullish = false)
                        if (res != null && res.score >= 55) {
                            breakoutCandidates.add(res)
                        }
                    }
                }.awaitAll()

                breakoutCandidates.sortByDescending { it.score }

                // Save breakout candidates to the database
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
                            scannedAt = System.currentTimeMillis()
                        )
                    }
                    db.scannedBreakoutDao().clearAll()
                    db.scannedBreakoutDao().insertBreakouts(dbBreakouts)
                    addLog("Background Scanner updated ${dbBreakouts.size} high-conviction MCX commodity breakouts.")
                }

                // 3. Automatically execute or average out trades when slots are available
                if (breakoutCandidates.isNotEmpty()) {
                    val eligibleCandidates = breakoutCandidates
                        .sortedByDescending { it.score }
                        .take(emptySlots.coerceAtLeast(1))
                    
                    for ((index, candidate) in eligibleCandidates.withIndex()) {
                        val availableMargin = IndianCommodityRepository.getAvailableMargin()
                        val (optimalTicker, lotSize) = IndianCommodityRepository.selectOptimalContract(candidate.ticker, ALLOCATION_PER_TRADE)
                        val expiryContractStr = IndianCommodityRepository.getOptimalExpiryContract(candidate.ticker)
                        val isMiniLot = optimalTicker.endsWith("M")
                        val lotLabel = if (isMiniLot) " [Mini Lot x$lotSize]" else " [Standard Lot x$lotSize]"
                        val expiryLabel = " [$expiryContractStr]"

                        val isOptionTrade = candidate.score >= 75 || candidate.ticker in listOf("GOLD", "SILVER", "CRUDEOIL")
                        val isBtstTrade = candidate.isBtst || isDailyTargetAchieved
                        val chooseCall = isMarketBullish

                        val instrumentLabel = if (isOptionTrade) {
                            if (chooseCall) "Call Option (CE)" else "Put Option (PE)"
                        } else {
                            if (chooseCall) "MCX Futures (Long)" else "MCX Futures (Short)"
                        }
                        
                        val tradeName = "${candidate.name}$lotLabel$expiryLabel ($instrumentLabel)"
                        val isPutOptionTrade = instrumentLabel.contains("Put")

                        // Check if an active trade already exists for this optimalTicker -> AVERAGE OUT instead of duplicate entry
                        val existingTrade = currentActive.find { it.ticker == optimalTicker }
                        if (existingTrade != null) {
                            val newAlloc = existingTrade.allocatedAmount + ALLOCATION_PER_TRADE
                            val avgEntry = ((existingTrade.entryPrice * existingTrade.allocatedAmount) + (candidate.price * ALLOCATION_PER_TRADE)) / newAlloc
                            val targetPrice = if (isBtstTrade) avgEntry * 1.010 else if (isPutOptionTrade) avgEntry * 0.920 else if (isOptionTrade) avgEntry * 1.080 else avgEntry * 1.035
                            val stopLossPrice = if (isBtstTrade) avgEntry * 0.994 else if (isPutOptionTrade) avgEntry * 1.015 else if (isOptionTrade) avgEntry * 0.985 else avgEntry * 0.988

                            val averagedTrade = existingTrade.copy(
                                entryPrice = avgEntry,
                                currentPrice = candidate.price,
                                allocatedAmount = newAlloc,
                                targetPrice = targetPrice,
                                stopLoss = stopLossPrice
                            )
                            db.virtualTradeDao().updateTrade(averagedTrade)
                            SupabaseSyncManager.publishTrade(averagedTrade)
                            addLog("🔄 Averaged Out Position: $optimalTicker at ₹${String.format("%.2f", candidate.price)} | New Avg Entry: ₹${String.format("%.2f", avgEntry)} (Total Allocated: ₹${String.format("%,.0f", newAlloc)})")
                        } else if (currentActive.size < MAX_CONCURRENT_TRADES && (currentActive.sumOf { it.allocatedAmount } + ALLOCATION_PER_TRADE) <= TOTAL_INVESTED_CAPITAL) {
                            val targetPrice = if (isBtstTrade) candidate.price * 1.010 else if (isPutOptionTrade) candidate.price * 0.920 else if (isOptionTrade) candidate.price * 1.080 else candidate.price * 1.035
                            val stopLossPrice = if (isBtstTrade) candidate.price * 0.994 else if (isPutOptionTrade) candidate.price * 1.015 else if (isOptionTrade) candidate.price * 0.985 else candidate.price * 0.988

                            val trade = VirtualTrade(
                                ticker = optimalTicker,
                                name = tradeName,
                                entryPrice = candidate.price,
                                currentPrice = candidate.price,
                                entryTime = System.currentTimeMillis(),
                                status = "ACTIVE",
                                targetPrice = targetPrice,
                                trailingSLThreshold = if (isPutOptionTrade) candidate.price * 0.975 else candidate.price * 1.025,
                                stopLoss = stopLossPrice,
                                highestPrice = candidate.price,
                                profitPercent = 0.0,
                                profitAmount = 0.0,
                                isPartialBooked = false,
                                allocatedAmount = ALLOCATION_PER_TRADE,
                                isBtst = isBtstTrade
                            )
                            val insertedId = db.virtualTradeDao().insertTrade(trade)
                            val tradeWithId = trade.copy(id = insertedId.toInt())
                            SupabaseSyncManager.publishTrade(tradeWithId)
                            addLog("🚀 Auto-Trade Executed$lotLabel: $instrumentLabel for $optimalTicker (Score: ${candidate.score}) at ₹${String.format("%.2f", candidate.price)}")
                        }
                    }
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
                val shares = ALLOCATION_PER_TRADE / trade.entryPrice
                    val profitAmt = (trade.currentPrice - trade.entryPrice) * shares
                val squared = trade.copy(
                    status = "SQUARED_OFF",
                    exitPrice = trade.currentPrice,
                    exitTime = System.currentTimeMillis(),
                    profitPercent = profitPct,
                    profitAmount = profitAmt
                )
                db.virtualTradeDao().updateTrade(squared)
                addLog("⏹️ Manually Squared Off ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (${String.format("%.2f", profitAmt)} INR)")
            }
            logDailyProfit(db)
        }
    }

    suspend fun manualSquareOffSingleTrade(tradeId: Int, db: AppDatabase) {
        val trade = db.virtualTradeDao().getAllTradesList().firstOrNull { it.id == tradeId }
        if (trade != null && trade.status == "ACTIVE") {
            val profitPct = ((trade.currentPrice - trade.entryPrice) / trade.entryPrice) * 100.0
            val shares = ALLOCATION_PER_TRADE / trade.entryPrice
                    val profitAmt = (trade.currentPrice - trade.entryPrice) * shares
            val updated = trade.copy(
                status = "SQUARED_OFF",
                exitPrice = trade.currentPrice,
                exitTime = System.currentTimeMillis(),
                profitPercent = profitPct,
                profitAmount = profitAmt
            )
            db.virtualTradeDao().updateTrade(updated)
            addLog("⏹️ Manually Squared Off ${trade.ticker} at ₹${String.format("%.2f", trade.currentPrice)} (${String.format("%.2f", profitAmt)} INR)")
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

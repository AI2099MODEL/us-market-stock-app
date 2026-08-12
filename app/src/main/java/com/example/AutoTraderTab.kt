package com.example

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AutoTraderTabContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = MyApplication.database
    val scope = rememberCoroutineScope()
    val tradesFlow = remember { db.virtualTradeDao().getAllTradesFlow() }
    val logsFlow = remember { db.profitLogDao().getAllLogsFlow() }

    val virtualTrades by tradesFlow.collectAsState(initial = emptyList())
    val profitLogs by logsFlow.collectAsState(initial = emptyList())
    val liveEngineLogs by MarketEngine.engineLogs.collectAsState()
    val isPaused by MarketEngine.isPausedForUserConfirmation.collectAsState()
    val promptMessage by MarketEngine.confirmationPromptMessage.collectAsState()

    var marketTimeText by remember { mutableStateOf(getMarketCloseCountdownText()) }

    LaunchedEffect(Unit) {
        var cycleCounter = 0
        while (isActive) {
            marketTimeText = getMarketCloseCountdownText()
            if (cycleCounter % 15 == 0) { // Execute engine cycle every 15 seconds instead of every 1 second
                try {
                    withContext(Dispatchers.IO) {
                        MarketEngine.runEngineCycle(context)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            cycleCounter++
            delay(1000) // Update countdown timer every second
        }
    }

    val activeTrades by remember(virtualTrades) {
        derivedStateOf { virtualTrades.filter { it.status == "ACTIVE" } }
    }
    val closedTrades by remember(virtualTrades) {
        derivedStateOf { virtualTrades.filter { it.status != "ACTIVE" } }
    }

    // Metrics calculations
    val totalClosed = closedTrades
    val profitBookedCount = remember(totalClosed) { totalClosed.count { it.status == "PROFIT_BOOKED" || it.profitAmount > 0 } }
    val winRate = remember(totalClosed, profitBookedCount) { if (totalClosed.isNotEmpty()) (profitBookedCount.toDouble() / totalClosed.size * 100).toInt() else 0 }
    val netProfit = remember(totalClosed) { totalClosed.sumOf { it.profitAmount } }
    
    val netInvested = remember(activeTrades) { activeTrades.sumOf { it.allocatedAmount } }
    val totalInvestedClosed = remember(totalClosed) { totalClosed.sumOf { it.allocatedAmount } }
    val netProfitPercent = remember(totalInvestedClosed, netProfit) { if (totalInvestedClosed > 0) (netProfit / totalInvestedClosed) * 100.0 else 0.0 }

    var subTab by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "CLOSED", "PERFORMANCE"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dhan WebSocket Connection Debugger
        item {
            DhanWebSocketDebugCard()
        }

        // Clear all trade history button
        item {
            OutlinedButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        MarketEngine.resetAllTradesAndRestart(db, context)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All Trade History & Restart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Subtabs Selector & Reset Trades action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active Tab
                    val isActiveSel = subTab == "ACTIVE"
                    Surface(
                        onClick = { subTab = "ACTIVE" },
                        modifier = Modifier.weight(1.05f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActiveSel) Color(0xFFEDE9FE) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isActiveSel) Color(0xFFC084FC) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(10.5.dp),
                                tint = if (isActiveSel) Color(0xFF7C3AED) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Active (${activeTrades.size})",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActiveSel) Color(0xFF7C3AED) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Closed Tab - Increased weight to 1.35f to comfortably display "Closed (XX)" without truncation
                    val isClosedSel = subTab == "CLOSED"
                    Surface(
                        onClick = { subTab = "CLOSED" },
                        modifier = Modifier.weight(1.35f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isClosedSel) Color(0xFFEDE9FE) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isClosedSel) Color(0xFFC084FC) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(10.5.dp),
                                tint = if (isClosedSel) Color(0xFF7C3AED) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Closed (${closedTrades.size})",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isClosedSel) Color(0xFF7C3AED) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Logs Tab
                    val isPerfSel = subTab == "PERFORMANCE"
                    Surface(
                        onClick = { subTab = "PERFORMANCE" },
                        modifier = Modifier.weight(0.80f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPerfSel) Color(0xFFEDE9FE) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isPerfSel) Color(0xFFC084FC) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(10.5.dp),
                                tint = if (isPerfSel) Color(0xFF7C3AED) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Logs",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPerfSel) Color(0xFF7C3AED) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Market Time Left Badge - Reduced weight to 0.90f
                    Surface(
                        modifier = Modifier.weight(0.90f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(10.5.dp),
                                tint = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = marketTimeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Tab Specific Contents
        if (subTab == "ACTIVE") {
            if (activeTrades.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Active Trades Right Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("The automated system scans for breakouts and initiates virtual trades automatically during market hours starting at 9:30 AM.", fontSize = 11.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                item {
                    val investedAmount = activeTrades.sumOf { it.allocatedAmount }
                    val runningPnl = activeTrades.sumOf { it.profitAmount }
                    val realisedPnl = closedTrades.sumOf { it.profitAmount }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Invested", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format("%,.0f", investedAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Divider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp),
                                color = Color(0xFFE2E8F0)
                            )
                            Column(
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Running P&L", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                val rColor = if (runningPnl >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                Text(
                                    text = "${if (runningPnl >= 0) "+" else ""}₹${String.format("%,.2f", runningPnl)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rColor
                                )
                            }
                            Divider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp),
                                color = Color(0xFFE2E8F0)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("Realised P&L", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                val pColor = if (realisedPnl >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                Text(
                                    text = "${if (realisedPnl >= 0) "+" else ""}₹${String.format("%,.2f", realisedPnl)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pColor
                                )
                            }
                        }
                    }
                }
                items(activeTrades, key = { it.id }) { trade ->
                    ActiveTradeCardItem(trade = trade)
                }

            }
        } else if (subTab == "CLOSED") {
            if (closedTrades.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Trades Closed Yet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                }
            } else {
                items(closedTrades, key = { it.id }) { trade ->
                    ClosedTradeCardItem(trade = trade)
                }
            }
        } else if (subTab == "PERFORMANCE") {
            item {
                CalendarPerformanceView(
                    profitLogs = profitLogs,
                    closedTrades = closedTrades,
                    liveEngineLogs = liveEngineLogs
                )
            }
        }

        // Metrics Summary Grid (2x2) at bottom above disclaimer
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Net Profit & Win Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Net Profit Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Net Profit / Loss", fontSize = 9.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "₹${String.format("%,.2f", netProfit)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${if (netProfit >= 0) "+" else ""}${String.format("%.2f", netProfitPercent)}%",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                    }

                    // Win Rate Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Win Rate", fontSize = 9.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "₹winRate%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (winRate >= 50) Color(0xFF7C3AED) else Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "₹profitBookedCount Wins / ${totalClosed.size} Trades",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Row 2: Net Invested & Closed Trades
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Net Invested Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Net Invested", fontSize = 9.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "₹${String.format("%,.2f", netInvested)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Active exposure",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Closed Trades
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Closed Trades", fontSize = 9.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${totalClosed.size} Trades",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Total virtual completed",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Aligned active status below metrics row in small font
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auto Trading Status: Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }


    }
}

private fun getTradeSubtitle(trade: VirtualTrade): String {
    val baseSymbol = trade.ticker.uppercase()
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = trade.entryTime.takeIf { it > 0 } ?: System.currentTimeMillis()
    val month = cal.get(java.util.Calendar.MONTH)
    val year = cal.get(java.util.Calendar.YEAR)
    
    val fullMonths = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val expiryDay = when {
        baseSymbol.contains("GOLD") || baseSymbol.contains("SILVER") -> 28
        baseSymbol.contains("CRUDE") || baseSymbol.contains("GAS") -> 20
        else -> 28
    }
    val monthName = fullMonths[month]
    
    val lotText = if (trade.name.contains("x")) {
        val parts = trade.name.split("x")
        if (parts.size > 1) {
            val num = parts[1].takeWhile { it.isDigit() }
            "$num lot"
        } else "1 lot"
    } else "1 lot"
    
    return "$lotText $expiryDay $monthName $year"
}

@Composable
fun ActiveTradeCardItem(trade: VirtualTrade) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trade.ticker, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text(getTradeSubtitle(trade), fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                val pColor = if (trade.profitPercent >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%.2f", trade.currentPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${if (trade.profitPercent >= 0) "+" else ""}${String.format("%.2f", trade.profitPercent)}% (₹${String.format("%.0f", trade.profitAmount)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = pColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", trade.entryPrice)}", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STOP LOSS", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", trade.stopLoss)}", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TARGET (2%)", fontSize = 9.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", trade.targetPrice)}", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val lowerSL = trade.stopLoss
            val upperTarget = trade.targetPrice
            val totalSpan = upperTarget - lowerSL
            val pctVal = if (totalSpan > 0) ((trade.currentPrice - lowerSL) / totalSpan).toFloat() else 0.5f
            val boundedPct = pctVal.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(boundedPct)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFFFFD166), Color(0xFF22C55E))
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Trailing SL Bound", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
                Text("Target Peak (2%)", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
fun ClosedTradeCardItem(trade: VirtualTrade) {
    val exitLabel = when (trade.status) {
        "PROFIT_BOOKED" -> "PROFIT BOOKED"
        "STOP_LOSS" -> "STOP LOSS HIT"
        else -> "SQUARED OFF"
    }
    
    val badgeColor = when (trade.status) {
        "PROFIT_BOOKED" -> Color(0xFFDCFCE7)
        "STOP_LOSS" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF1F5F9)
    }

    val badgeTextColor = when (trade.status) {
        "PROFIT_BOOKED" -> Color(0xFF15803D)
        "STOP_LOSS" -> Color(0xFFB91C1C)
        else -> Color(0xFF475569)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trade.ticker, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(getTradeSubtitle(trade), fontSize = 10.5.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(exitLabel, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = badgeTextColor, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Buy: ₹${String.format("%.2f", trade.entryPrice)}", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("➔", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                        Text("Exit: ₹${String.format("%.2f", trade.exitPrice ?: trade.currentPrice)}", fontSize = 10.5.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    trade.exitTime?.let {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a 'IST'", Locale.ENGLISH).apply {
                            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                        }
                        Text("Closed: ${sdf.format(Date(it))}", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                val pColor = if (trade.profitPercent >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (trade.profitPercent >= 0) "+" else ""}${String.format("%.2f", trade.profitPercent)}%",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pColor
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "₹${String.format("%,.0f", trade.profitAmount)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = pColor
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceLogCardItem(log: ProfitLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (log.type) {
                                "DAILY" -> Color(0xFFEDE9FE)
                                "WEEKLY" -> Color(0xFFE0F2FE)
                                else -> Color(0xFFFEF3C7)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(log.type, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = when (log.type) {
                        "DAILY" -> Color(0xFF7C3AED)
                        "WEEKLY" -> Color(0xFF0284C7)
                        else -> Color(0xFFD97706)
                    })
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(log.dateString, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text("Aggregated over ${log.tradeCount} trades", fontSize = 10.sp, color = Color(0xFF64748B))
            }

            val pColor = if (log.profitAmount >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (log.profitAmount >= 0) "+" else ""}${String.format("%.2f", log.profitPercent)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = pColor
                )
                Text(
                    text = "₹${String.format("%,.2f", log.profitAmount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pColor
                )
            }
        }
    }
}

@Composable
fun EngineLogCardItem(logText: String) {
    val timestamp = if (logText.startsWith("[")) logText.substringAfter("[").substringBefore("]") else ""
    val rawMessage = if (logText.startsWith("[")) logText.substringAfter("] ").trim() else logText.trim()

    val isEntry = rawMessage.contains("🚀") || rawMessage.contains("Entry") || rawMessage.contains("High-Conviction")
    val isProfit = rawMessage.contains("💰") || rawMessage.contains("Profit") || rawMessage.contains("Target")
    val isSquareOff = rawMessage.contains("⏰") || rawMessage.contains("Squared Off") || rawMessage.contains("SQUARED_OFF") || rawMessage.contains("Closed")
    val isTrailing = rawMessage.contains("🔒") || rawMessage.contains("TRAILING")

    val bgColor = when {
        isEntry -> Color(0xFFFAF5FF)
        isProfit -> Color(0xFFF0FDF4)
        isSquareOff -> Color(0xFFFFF7ED)
        isTrailing -> Color(0xFFEFF6FF)
        else -> Color(0xFFF8FAFC)
    }
    val borderColor = when {
        isEntry -> Color(0xFFE9D5FF)
        isProfit -> Color(0xFFBBF7D0)
        isSquareOff -> Color(0xFFFED7AA)
        isTrailing -> Color(0xFFBFDBFE)
        else -> Color(0xFFE2E8F0)
    }
    val iconColor = when {
        isEntry -> Color(0xFF9333EA)
        isProfit -> Color(0xFF16A34A)
        isSquareOff -> Color(0xFFEA580C)
        isTrailing -> Color(0xFF2563EB)
        else -> Color(0xFF64748B)
    }
    val icon = when {
        isEntry -> Icons.Default.TrendingUp
        isProfit -> Icons.Default.MonetizationOn
        isSquareOff -> Icons.Default.Schedule
        isTrailing -> Icons.Default.Lock
        else -> Icons.Default.Memory
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.5.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(13.dp).padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                if (timestamp.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rawMessage,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 14.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = borderColor.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = timestamp,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = rawMessage,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        lineHeight = 14.5.sp
                    )
                }
            }
        }
    }
}

fun getMarketCloseCountdownText(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val currentSecs = cal.get(Calendar.SECOND)
    val openMinutes = 9 * 60      // 09:00 AM IST
    val closeMinutes = 23 * 60 + 30 // 11:30 PM IST (MCX Close)

    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // MCX is closed Saturday afternoon to Sunday evening. Saturday until 11:30 PM is closed or morning session closed. Let's consider Saturday & Sunday weekend closed.
    val isWeekend = dayOfWeek == Calendar.SUNDAY || (dayOfWeek == Calendar.SATURDAY && currentMinutes > closeMinutes)

    return if (isWeekend) {
        "Closed (Weekend)"
    } else if (currentMinutes < openMinutes) {
        val totalSecsLeft = (openMinutes - currentMinutes) * 60 - currentSecs
        if (totalSecsLeft <= 0) return "Opening..."
        val hrs = totalSecsLeft / 3600
        val mins = (totalSecsLeft % 3600) / 60
        if (hrs > 0) "${hrs}h ${mins}m to open" else "${mins}m to open"
    } else if (currentMinutes >= closeMinutes) {
        "Closed"
    } else {
        val totalSecsLeft = (closeMinutes - currentMinutes) * 60 - currentSecs
        if (totalSecsLeft <= 0) return "Closing..."
        val hrs = totalSecsLeft / 3600
        val mins = (totalSecsLeft % 3600) / 60
        if (hrs > 0) "${hrs}h ${mins}m left" else "${mins}m left"
    }
}

@Composable
fun EmptyStateCard(title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.EventNote, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 10.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ExecutedCommodityTradeCard(trade: VirtualTrade) {
    val isProfit = trade.profitAmount >= 0
    val pColor = if (isProfit) Color(0xFF16A34A) else Color(0xFFDC2626)
    val badgeColor = if (isProfit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    val badgeTextColor = if (isProfit) Color(0xFF15803D) else Color(0xFFB91C1C)
    val exitLabel = when (trade.status) {
        "PROFIT_BOOKED" -> "PROFIT BOOKED"
        "STOP_LOSS" -> "STOP LOSS HIT"
        else -> "SQUARED OFF"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trade.ticker, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text(trade.name, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(exitLabel, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = badgeTextColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Entry: ₹${String.format("%.2f", trade.entryPrice)}", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("➔", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text("Exit: ₹${String.format("%.2f", trade.exitPrice ?: trade.currentPrice)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    trade.exitTime?.let {
                        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a 'IST'", Locale.ENGLISH).apply {
                            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                        }
                        Text("Timestamp: ${sdf.format(Date(it))}", fontSize = 9.sp, color = Color(0xFF0284C7))
                    }
                    Text("Net P&L (After Dhan Brokerage & Govt Charges):", fontSize = 8.5.sp, color = Color(0xFF64748B))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format("%.2f", trade.profitPercent)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pColor
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${if (isProfit) "+" else ""}₹${String.format("%,.2f", trade.profitAmount)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = pColor
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarPerformanceView(
    profitLogs: List<ProfitLog>,
    closedTrades: List<VirtualTrade>,
    liveEngineLogs: List<String>
) {
    var calendarMode by remember { mutableStateOf("TRADES") } // "TRADES", "DAILY", "WEEKLY", "MONTHLY", "ENGINE_LOGS"
    var selectedCalendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDayNumber by remember { mutableStateOf<Int?>(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    val sdfYearMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val sdfDayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    val dailyPnlMap = remember(closedTrades, profitLogs) {
        val map = mutableMapOf<String, Double>()
        closedTrades.forEach { trade ->
            trade.exitTime?.let { time ->
                val dateStr = sdfDayKey.format(Date(time))
                map[dateStr] = (map[dateStr] ?: 0.0) + trade.profitAmount
            }
        }
        profitLogs.forEach { log ->
            map[log.dateString] = log.profitAmount
        }
        map
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Executed Commodity Trades & Performance Logs", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text("Net P&L calculated after Dhan brokerage and government charges", fontSize = 9.5.sp, color = Color(0xFF64748B))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F5F9))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "TRADES" to "Executed Trades",
                "DAILY" to "Daily Calendar",
                "WEEKLY" to "Weekly",
                "MONTHLY" to "Monthly",
                "ENGINE_LOGS" to "Live Logs"
            ).forEach { (key, label) ->
                val isSelected = calendarMode == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { calendarMode = key }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 8.5.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (calendarMode) {
            "TRADES" -> {
                if (closedTrades.isEmpty()) {
                    EmptyStateCard("No Executed Commodity Trades", "Executed trades history including entry price, exit price, timestamp, and net profit/loss after brokerage will appear here.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        closedTrades.sortedByDescending { it.exitTime ?: 0L }.forEach { trade ->
                            ExecutedCommodityTradeCard(trade = trade)
                        }
                    }
                }
            }
            "DAILY" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newCal = selectedCalendarDate.clone() as Calendar
                                    newCal.add(Calendar.MONTH, -1)
                                    selectedCalendarDate = newCal
                                    selectedDayNumber = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = Color(0xFF475569))
                            }

                            Text(
                                text = sdfYearMonth.format(selectedCalendarDate.time),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )

                            IconButton(
                                onClick = {
                                    val newCal = selectedCalendarDate.clone() as Calendar
                                    newCal.add(Calendar.MONTH, 1)
                                    selectedCalendarDate = newCal
                                    selectedDayNumber = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF475569))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { dayName ->
                                Text(
                                    text = dayName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val cal = selectedCalendarDate.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val totalCells = firstDayOfWeek + daysInMonth
                        val rows = (totalCells + 6) / 7

                        val year = cal.get(Calendar.YEAR)
                        val month = cal.get(Calendar.MONTH)

                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - firstDayOfWeek + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val dayCal = Calendar.getInstance()
                                        dayCal.set(year, month, dayNum)
                                        val dateStr = sdfDayKey.format(dayCal.time)
                                        val pnl = dailyPnlMap[dateStr]
                                        val isSelected = selectedDayNumber == dayNum

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .padding(1.5.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when {
                                                        isSelected -> Color(0xFFEDE9FE)
                                                        pnl != null && pnl > 0 -> Color(0xFFDCFCE7)
                                                        pnl != null && pnl < 0 -> Color(0xFFFEE2E2)
                                                        else -> Color(0xFFF8FAFC)
                                                    }
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = when {
                                                        isSelected -> Color(0xFF7C3AED)
                                                        pnl != null && pnl > 0 -> Color(0xFF86EFAC)
                                                        pnl != null && pnl < 0 -> Color(0xFFFCA5A5)
                                                        else -> Color(0xFFE2E8F0)
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { selectedDayNumber = dayNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "₹dayNum",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFF6D28D9) else Color(0xFF1E293B)
                                                )
                                                if (pnl != null) {
                                                    Text(
                                                        text = "${if (pnl >= 0) "+" else ""}₹${String.format("%.0f", pnl)}",
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (pnl >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedDayNumber != null) {
                    val dayCal = selectedCalendarDate.clone() as Calendar
                    dayCal.set(Calendar.DAY_OF_MONTH, selectedDayNumber!!)
                    val selectedDateStr = sdfDayKey.format(dayCal.time)
                    val pnlForDay = dailyPnlMap[selectedDateStr]

                    Text(
                        text = "Results for ${SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(dayCal.time)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (pnlForDay == null) {
                        EmptyStateCard("No Trades Executed On This Day", "Trading records are wiped clean and ready for tomorrow's live trading session.")
                    } else {
                        val matchingTrades = closedTrades.filter { trade ->
                            trade.exitTime?.let { sdfDayKey.format(Date(it)) == selectedDateStr } ?: false
                        }
                        matchingTrades.forEach { trade ->
                            ClosedTradeCardItem(trade = trade)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            "WEEKLY" -> {
                val weeklyGroups = remember(closedTrades) {
                    val map = mutableMapOf<String, MutableList<VirtualTrade>>()
                    val sdfWeek = SimpleDateFormat("'Week' w, yyyy", Locale.US)
                    closedTrades.forEach { trade ->
                        trade.exitTime?.let {
                            val weekStr = sdfWeek.format(Date(it))
                            map.getOrPut(weekStr) { mutableListOf() }.add(trade)
                        }
                    }
                    map
                }

                if (weeklyGroups.isEmpty()) {
                    EmptyStateCard("No Weekly Trade History", "Clean slate active. Weekly performance logs will accumulate automatically starting tomorrow.")
                } else {
                    weeklyGroups.forEach { (weekLabel, trades) ->
                        val netPnl = trades.sumOf { it.profitAmount }
                        val winCount = trades.count { it.status == "PROFIT_BOOKED" }
                        val winRatePct = if (trades.isNotEmpty()) (winCount * 100) / trades.size else 0

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(weekLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("${trades.size} Trades • $winRatePct% Win Rate", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                                Text(
                                    text = "${if (netPnl >= 0) "+" else ""}₹${String.format("%,.2f", netPnl)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netPnl >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            "MONTHLY" -> {
                val monthlyGroups = remember(closedTrades) {
                    val map = mutableMapOf<String, MutableList<VirtualTrade>>()
                    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.US)
                    closedTrades.forEach { trade ->
                        trade.exitTime?.let {
                            val monthStr = sdfMonth.format(Date(it))
                            map.getOrPut(monthStr) { mutableListOf() }.add(trade)
                        }
                    }
                    map
                }

                if (monthlyGroups.isEmpty()) {
                    EmptyStateCard("No Monthly Performance History", "Clean slate active. Monthly summary reports will accumulate automatically starting tomorrow.")
                } else {
                    monthlyGroups.forEach { (monthLabel, trades) ->
                        val netPnl = trades.sumOf { it.profitAmount }
                        val winCount = trades.count { it.status == "PROFIT_BOOKED" }
                        val winRatePct = if (trades.isNotEmpty()) (winCount * 100) / trades.size else 0

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(monthLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("${trades.size} Completed Trades • $winRatePct% Win Rate", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                                Text(
                                    text = "${if (netPnl >= 0) "+" else ""}₹${String.format("%,.2f", netPnl)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netPnl >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            "ENGINE_LOGS" -> {
                if (liveEngineLogs.isEmpty()) {
                    EmptyStateCard("Engine Active & Scanning", "Live signal scans, breakout entries, profit bookings, and auto option square-offs will be logged here in real time.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        liveEngineLogs.forEach { logText ->
                            EngineLogCardItem(logText = logText)
                        }
                    }
                }
            }
        }
    }
}
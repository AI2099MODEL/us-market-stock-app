package com.example

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// Data model for Portfolio Holding
import coil.compose.SubcomposeAsyncImage

data class PortfolioHolding(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val quantity: Double,
    val buyPrice: Double,
    val purchaseDate: String,
    val broker: String,
    val notes: String = "",
    val currentPrice: Double = 0.0,
    val previousClose: Double = 0.0
)

data class HoldingPriceData(
    val price: Double = 0.0,
    val previousClose: Double = 0.0,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

val BROKER_OPTIONS = listOf(
    "Dhan",
    "Charles Schwab",
    "Fidelity",
    "Robinhood",
    "E-Trade",
    "Interactive Brokers",
    "Vanguard",
    "TD Ameritrade",
    "Webull",
    "Merrill Edge",
    "Other"
)

fun getBrokerLogoUrl(broker: String): String {
    val domain = when (broker.trim().lowercase()) {
        "dhan", "dhan hq" -> "dhan.co"
        "schwab", "charles schwab" -> "schwab.com"
        "fidelity" -> "fidelity.com"
        "robinhood" -> "robinhood.com"
        "etrade", "e-trade" -> "etrade.com"
        "interactive brokers", "ibkr" -> "interactivebrokers.com"
        "vanguard" -> "vanguard.com"
        "td ameritrade", "tdameritrade" -> "tdameritrade.com"
        "webull" -> "webull.com"
        "merrill edge", "merrill" -> "merrilledge.com"
        else -> ""
    }
    return if (domain.isNotEmpty()) "https://www.google.com/s2/favicons?domain=$domain&sz=128" else ""
}

@Composable
fun BrokerBadge(broker: String, modifier: Modifier = Modifier) {
    val logoUrl = getBrokerLogoUrl(broker)
    val (letter, bg) = when (broker.trim().lowercase()) {
        "dhan", "dhan hq" -> Pair("D", Color(0xFF10B981))
        "schwab", "charles schwab" -> Pair("S", Color(0xFF00A0DF))
        "fidelity" -> Pair("F", Color(0xFF00875A))
        "robinhood" -> Pair("R", Color(0xFF00C805))
        "etrade", "e-trade" -> Pair("E", Color(0xFF663399))
        "interactive brokers", "ibkr" -> Pair("I", Color(0xFFE2231A))
        "vanguard" -> Pair("V", Color(0xFF990000))
        "td ameritrade", "tdameritrade" -> Pair("T", Color(0xFF007C32))
        "webull" -> Pair("W", Color(0xFF1E88E5))
        "merrill edge", "merrill" -> Pair("M", Color(0xFF002244))
        else -> Pair(broker.take(1).uppercase().ifBlank { "O" }, Color(0xFF64748B))
    }

    if (logoUrl.isNotEmpty()) {
        SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = "$broker Logo",
            modifier = modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit,
            error = {
                FallbackLetterBadge(letter = letter, bg = bg)
            },
            loading = {
                FallbackLetterBadge(letter = letter, bg = bg)
            }
        )
    } else {
        FallbackLetterBadge(letter = letter, bg = bg, modifier = modifier)
    }
}

@Composable
private fun FallbackLetterBadge(letter: String, bg: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bg
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

// Sector Mapping for US Tickers
fun getStockSector(ticker: String): String {
    val clean = ticker.uppercase()
    return when {
        clean in listOf("AAPL", "BPCL", "ONGC", "IOC", "GAIL", "ATGL") -> "Energy & Oil"
        clean in listOf("JPM", "WFC", "BAC", "KOTAKBANK", "AXISBANK", "INDUSINDBK", "BANKBARODA", "CANBK", "PNB", "AUBANK", "PFC", "RECLTD", "JIOFIN", "GOOGL", "BAJAJFINSV") -> "Banking & Financials"
        clean in listOf("TSLA", "MARUTI", "M&M", "BAJAJ-AUTO", "HEROMOTOCO", "EICHERMOT", "ASHOKLEY", "TVSMOTOR") -> "Automotive"
        clean in listOf("MSFT", "NVDA", "NVDA", "IBM", "HCLTECH", "TECHM", "PERSESTENT", "LTIM", "COFORGE", "MPHASIS", "CRM") -> "Information Technology"
        clean in listOf("PG", "HINDUNILVR", "HUL", "NESTLEIND", "BRITANNIA", "DABUR", "GODREJCP", "MARICO", "COLPAL", "VBL") -> "FMCG"
        clean in listOf("SUNPHARMA", "DRREDDY", "CIPLA", "DIVISLAB", "APOLLOHOSP", "TORNTPHARM", "LUPIN", "AURPHARMA", "ALKEM", "ZYDUSLIFE", "MANKIND") -> "Healthcare & Pharma"
        clean in listOf("LT", "META", "HAL", "BEL", "BHEL", "RVNL", "IRFC", "IRCTC", "CONCOR", "PNCINFRA") -> "Infra & Defence"
        clean in listOf("WMT", "JINDALSTEL", "JSWSTEEL", "HINDALCO", "COALINDIA", "VEDL", "NMDC", "SAIL", "NATIONALUM") -> "Metals & Mining"
        clean in listOf("NTPC", "POWERGRID", "NEE", "ADANIGREEN", "ADANIPOWER", "NHPC", "SUZLON", "JSWENERGY") -> "Utilities & Power"
        clean in listOf("AMZN", "AIRTEL", "IDEA", "INDUSTOWER") -> "Telecom"
        else -> "Diversified / Other"
    }
}

// Storage helpers using SharedPreferences
object PortfolioStorage {
    private const val PREFS_NAME = "StockBreakoutPortfolioPrefs"
    private const val KEY_HOLDINGS_BASE = "user_holdings_json"

    private fun getHoldingsKey(): String {
        val uid = GoogleAuthManager.currentUser.value?.uid
        return if (!uid.isNullOrBlank()) "${KEY_HOLDINGS_BASE}_$uid" else KEY_HOLDINGS_BASE
    }

    fun loadHoldings(context: Context): List<PortfolioHolding> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getHoldingsKey()
        val isPrunedKey = "dummy_holdings_pruned_$key"
        val isPruned = prefs.getBoolean(isPrunedKey, false)
        val jsonStr = prefs.getString(key, null)
        if (!isPruned || jsonStr.isNullOrBlank()) {
            val defaults = getDefaultHoldings()
            saveHoldings(context, defaults)
            prefs.edit().putBoolean(isPrunedKey, true).apply()
            return defaults
        }
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<PortfolioHolding>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PortfolioHolding(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        symbol = obj.optString("symbol", ""),
                        quantity = obj.optDouble("quantity", 0.0),
                        buyPrice = obj.optDouble("buyPrice", 0.0),
                        purchaseDate = obj.optString("purchaseDate", ""),
                        broker = obj.optString("broker", "Dhan"),
                        notes = obj.optString("notes", ""),
                        currentPrice = obj.optDouble("currentPrice", 0.0),
                        previousClose = obj.optDouble("previousClose", 0.0)
                    )
                )
            }
            if (list.isEmpty()) getDefaultHoldings() else list
        } catch (e: Exception) {
            getDefaultHoldings()
        }
    }

    fun saveHoldings(context: Context, holdings: List<PortfolioHolding>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getHoldingsKey()
        val jsonArray = JSONArray()
        holdings.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("symbol", h.symbol)
                put("quantity", h.quantity)
                put("buyPrice", h.buyPrice)
                put("purchaseDate", h.purchaseDate)
                put("broker", h.broker)
                put("notes", h.notes)
                put("currentPrice", h.currentPrice)
                put("previousClose", h.previousClose)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun getDefaultHoldings(): List<PortfolioHolding> {
        return emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioAnalysisView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Submenu state: "PORTFOLIO" or "ANALYZER"
    var activeSubTab by remember { mutableStateOf("PORTFOLIO") }

    // User & Holdings state
    val currentUser by GoogleAuthManager.currentUser.collectAsState()
    var holdings by remember { mutableStateOf(PortfolioStorage.loadHoldings(context)) }
    var priceMap by remember { mutableStateOf<Map<String, HoldingPriceData>>(emptyMap()) }

    LaunchedEffect(currentUser) {
        holdings = PortfolioStorage.loadHoldings(context)
    }

    // Modals & Dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var editingHolding by remember { mutableStateOf<PortfolioHolding?>(null) }
    var isAutoRefreshPaused by remember { mutableStateOf(false) }
    var isSyncingDhan by remember { mutableStateOf(false) }
    var dhanSyncMessage by remember { mutableStateOf<String?>(null) }

    // Persist holdings whenever changed
    fun updateHoldings(newList: List<PortfolioHolding>) {
        holdings = newList
        PortfolioStorage.saveHoldings(context, newList)
    }

    // Immediately populate priceMap from synced holdings if currentPrice exists
    LaunchedEffect(holdings) {
        if (holdings.isEmpty()) return@LaunchedEffect
        val updatedMap = priceMap.toMutableMap()
        var changed = false
        holdings.forEach { h ->
            if (h.currentPrice > 0.0 && (!updatedMap.containsKey(h.symbol) || updatedMap[h.symbol]?.price == 0.0)) {
                updatedMap[h.symbol] = HoldingPriceData(
                    price = h.currentPrice,
                    previousClose = if (h.previousClose > 0.0) h.previousClose else h.buyPrice,
                    lastUpdatedMs = System.currentTimeMillis()
                )
                changed = true
            }
        }
        if (changed) {
            priceMap = updatedMap
        }
    }

    // Batch Live Price Fetching with rate limit safety & exponential retry
    LaunchedEffect(holdings, isAutoRefreshPaused) {
        if (holdings.isEmpty()) return@LaunchedEffect

        while (isActive) {
            if (!isAutoRefreshPaused) {
                val tickers = holdings.map { it.symbol }.distinct()
                val newMap = priceMap.toMutableMap()

                withContext(Dispatchers.IO) {
                    tickers.forEach { ticker ->
                        try {
                            val cleanTicker = ticker.replace("-EQ", "").uppercase().trim()

                            // 1. First check Dhan live websocket feed for zero latency tick
                            val liveWsQuote = DhanWebSocketManager.liveQuotes.value[ticker]
                                ?: DhanWebSocketManager.liveQuotes.value[cleanTicker]

                            if (liveWsQuote != null && liveWsQuote.price > 0.0) {
                                newMap[ticker] = HoldingPriceData(
                                    price = liveWsQuote.price,
                                    previousClose = if (liveWsQuote.price - liveWsQuote.change > 0) liveWsQuote.price - liveWsQuote.change else liveWsQuote.price,
                                    lastUpdatedMs = System.currentTimeMillis()
                                )
                            } else {
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
                            }
                        } catch (e: Exception) {
                            // Retain existing price if fetch throttled or network fails
                        }
                    }
                }
                priceMap = newMap
            }
            delay(15000) // 15-second interval
        }
    }

    // Calculated portfolio metrics
    val totalInvested = remember(holdings) {
        holdings.sumOf { it.quantity * it.buyPrice }
    }

    val totalCurrentValue = remember(holdings, priceMap) {
        holdings.sumOf { h ->
            val p = priceMap[h.symbol]?.price?.takeIf { it > 0.0 }
                ?: h.currentPrice.takeIf { it > 0.0 }
                ?: h.buyPrice
            h.quantity * p
        }
    }

    val totalProfitLoss = totalCurrentValue - totalInvested
    val totalProfitLossPct = if (totalInvested > 0) (totalProfitLoss / totalInvested) * 100 else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP SUBMENU SELECTOR CARD (Matching Breakouts Home Tab Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // My Portfolio button
                val isPortfolioSelected = activeSubTab == "PORTFOLIO"
                Surface(
                    onClick = { activeSubTab = "PORTFOLIO" },
                    modifier = Modifier.weight(1f),
                    color = if (isPortfolioSelected) Color(0xFFEDE9FE) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isPortfolioSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "My Portfolio",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPortfolioSelected) Color(0xFF7C3AED) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Analyzer button
                val isAnalyzerSelected = activeSubTab == "ANALYZER"
                Surface(
                    onClick = { activeSubTab = "ANALYZER" },
                    modifier = Modifier.weight(1f),
                    color = if (isAnalyzerSelected) Color(0xFFEDE9FE) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = if (isAnalyzerSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Analyzer",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAnalyzerSelected) Color(0xFF7C3AED) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // Auto-refresh Paused Indicator Banner
        if (isAutoRefreshPaused) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isAutoRefreshPaused = false
                    },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Text("Live auto-refresh paused. Tap to resume live CMP updates.", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                    }
                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
            }
        }

        // Dhan Sync Status Banner
        dhanSyncMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dhanSyncMessage = null },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        Text(msg, fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { dhanSyncMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // SUBTAB CONTENT
        if (activeSubTab == "PORTFOLIO") {
            // PORTFOLIO TAB
            MyPortfolioContent(
                holdings = holdings,
                priceMap = priceMap,
                totalInvested = totalInvested,
                totalCurrentValue = totalCurrentValue,
                totalProfitLoss = totalProfitLoss,
                totalProfitLossPct = totalProfitLossPct,
                isSyncingDhan = isSyncingDhan,
                onAddClick = {
                    if (holdings.size >= 40) {
                        showLimitDialog = true
                    } else {
                        editingHolding = null
                        showAddDialog = true
                    }
                },
                onEditClick = { h ->
                    editingHolding = h
                    showAddDialog = true
                },
                onDeleteClick = { h ->
                    val newList = holdings.filter { it.id != h.id }
                    updateHoldings(newList)
                },
                onImportDhan = {
                    coroutineScope.launch {
                        isSyncingDhan = true
                        dhanSyncMessage = "Syncing portfolio with Dhan HQ API..."
                        val result = DhanPortfolioService.fetchDhanPortfolio(context)
                        isSyncingDhan = false
                        if (result.holdings.isNotEmpty()) {
                            updateHoldings(result.holdings)
                        }
                        dhanSyncMessage = result.message
                    }
                }
            )
        } else {
            // ANALYZER TAB
            PortfolioAnalyzerTabContent(
                holdings = holdings,
                priceMap = priceMap,
                totalInvested = totalInvested,
                totalCurrentValue = totalCurrentValue,
                totalProfitLossPct = totalProfitLossPct
            )
        }

        // SEC / FINRA US Regulatory Disclaimer Box (Shown at bottom of analysis page)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Disclaimer",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SEC & FINRA Regulatory Disclaimer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pursuant to U.S. SEC and FINRA regulatory policies, all portfolio analytics, stock valuation metrics, automated signals, virtual trades, and content displayed in StockBreak US are provided strictly for educational and informational paper-trading purposes only. This application is not a registered investment advisor or SEC broker-dealer. Paper trading / virtual trading involves no monetary risk, but actual trading of U.S. equities and options involves substantial risk of loss. Always consult a licensed financial advisor before making investment decisions.",
                    fontSize = 9.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 13.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Add / Edit Holding Dialog
    if (showAddDialog) {
        AddEditHoldingDialog(
            holding = editingHolding,
            currentTotalCount = holdings.size,
            onDismiss = { showAddDialog = false },
            onSave = { updatedHolding ->
                val existingIndex = holdings.indexOfFirst { it.id == updatedHolding.id }
                val newList = holdings.toMutableList()
                if (existingIndex != -1) {
                    newList[existingIndex] = updatedHolding
                } else {
                    newList.add(updatedHolding)
                }
                updateHoldings(newList)
                showAddDialog = false
            }
        )
    }

    // Limit Reached Dialog
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("Portfolio Limit Reached", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "You can add a maximum of 40 stock holdings in your portfolio. Please remove an existing stock holding to add a new share.",
                    fontSize = 12.5.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLimitDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun MyPortfolioContent(
    holdings: List<PortfolioHolding>,
    priceMap: Map<String, HoldingPriceData>,
    totalInvested: Double,
    totalCurrentValue: Double,
    totalProfitLoss: Double,
    totalProfitLossPct: Double,
    isSyncingDhan: Boolean = false,
    onAddClick: () -> Unit,
    onEditClick: (PortfolioHolding) -> Unit,
    onDeleteClick: (PortfolioHolding) -> Unit,
    onImportDhan: () -> Unit
) {
    // Sort holdings by loss% to profit% (ascending order of P&L percentage)
    val sortedHoldings = remember(holdings, priceMap) {
        holdings.sortedBy { h ->
            val curP = priceMap[h.symbol]?.price?.takeIf { it > 0.0 } ?: h.buyPrice
            if (h.buyPrice > 0) ((curP - h.buyPrice) / h.buyPrice) * 100 else 0.0
        }
    }

    // Top Summary Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PORTFOLIO VALUE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text("${holdings.size}/40 Shares", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("₹${String.format(Locale.US, "%,.2f", totalCurrentValue)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        val isValProfit = totalProfitLoss >= 0
                        val valSign = if (isValProfit) "+" else ""
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isValProfit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "$valSign${String.format(Locale.US, "%.2f", totalProfitLossPct)}% ($valSign₹${String.format(Locale.US, "%,.2f", totalProfitLoss)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isValProfit) Color(0xFF15803D) else Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .height(28.dp)
                            .clickable(enabled = !isSyncingDhan, onClick = onImportDhan),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSyncingDhan) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Sync Dhan HQ",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = if (isSyncingDhan) "Syncing..." else "Sync Dhan HQ",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    TextButton(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Invested", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    Text("₹${String.format(Locale.US, "%,.2f", totalInvested)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Overall P&L", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    val isProfit = totalProfitLoss >= 0
                    val sign = if (isProfit) "+" else ""
                    val pnlText = "$sign₹${String.format(Locale.US, "%,.2f", totalProfitLoss)} ($sign${String.format(Locale.US, "%.2f", totalProfitLossPct)}%)"
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isProfit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = pnlText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    if (holdings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(44.dp))
                Text("No Holdings Added Yet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Text("Fetch your live portfolio directly from Dhan HQ or add positions manually.", fontSize = 11.5.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onImportDhan,
                        enabled = !isSyncingDhan,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (isSyncingDhan) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connecting Dhan API...", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetch Portfolio from Dhan HQ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475569))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Manual", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    }
                }
            }
        }
    } else {
        // 2-Column Grid matching Dividend Card style (sorted from loss % to profit %)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val pairs = sortedHoldings.chunked(2)
            pairs.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { h ->
                        Box(modifier = Modifier.weight(1f)) {
                            HoldingGridCard(
                                holding = h,
                                liveData = priceMap[h.symbol],
                                onEdit = { onEditClick(h) },
                                onDelete = { onDeleteClick(h) }
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun HoldingGridCard(
    holding: PortfolioHolding,
    liveData: HoldingPriceData?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displaySymbol = holding.symbol
    val currentPrice = liveData?.price?.takeIf { it > 0.0 } ?: holding.currentPrice.takeIf { it > 0.0 } ?: holding.buyPrice
    val investedVal = holding.quantity * holding.buyPrice
    val currentVal = holding.quantity * currentPrice
    val pnlAmount = currentVal - investedVal
    val pnlPct = if (investedVal > 0) (pnlAmount / investedVal) * 100 else 0.0
    val isProfit = pnlAmount >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Broker Badge + Company Logo (Left) + Profit/Loss Pill Badge Tag (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    BrokerBadge(broker = holding.broker)
                    CompanyLogoView(symbol = holding.symbol, modifier = Modifier.size(22.dp))
                }

                val sign = if (isProfit) "▲" else "▼"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$sign ${if (isProfit) "PROFIT" else "LOSS"}",
                        color = Color.White,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            // Row 2: Ticker Symbol & Subtitle (Broker + Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displaySymbol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${holding.broker} · ${holding.purchaseDate.ifBlank { "Recent" }}",
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action icons
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                    }
                }
            }

            // Row 3: Live Price (Right-aligned) + Qty x BuyPrice (Left)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (holding.quantity % 1.0 == 0.0) holding.quantity.toInt().toString() else String.format(Locale.US, "%.2f", holding.quantity)} × ₹${String.format(Locale.US, "%,.2f", holding.buyPrice)}",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )

                Text(
                    text = "₹${String.format(Locale.US, "%.2f", currentPrice)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                )
            }

            // Row 4: Two-Column Stats (Invested vs Current)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Invested", fontSize = 8.5.sp, color = Color(0xFF64748B))
                    Text("₹${String.format(Locale.US, "%,.0f", investedVal)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current Value", fontSize = 8.5.sp, color = Color(0xFF64748B))
                    Text("₹${String.format(Locale.US, "%,.0f", currentVal)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }

            // Row 5: Bottom P&L Pill Band
            val pnlSign = if (isProfit) "+" else ""
            val bandBg = if (isProfit) Color(0xFFECFDF5) else Color(0xFFFFF0F1)
            val bandText = if (isProfit) Color(0xFF059669) else Color(0xFFDC2626)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(bandBg)
                    .padding(vertical = 3.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$pnlSign₹${String.format(Locale.US, "%,.0f", pnlAmount)} ($pnlSign${String.format(Locale.US, "%.1f", pnlPct)}%)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = bandText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ANALYZER TAB CONTENT
@Composable
fun PortfolioAnalyzerTabContent(
    holdings: List<PortfolioHolding>,
    priceMap: Map<String, HoldingPriceData>,
    totalInvested: Double,
    totalCurrentValue: Double,
    totalProfitLossPct: Double
) {
    if (holdings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Add holdings in 'My Portfolio' tab to view automated AI Analyzer diagnostics.", fontSize = 12.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
        }
        return
    }

    // HEALTH SCORE FORMULA CALCULATIONS (Section 4 & 5)
    // A. Diversification score (max 40 pts)
    val holdingCount = holdings.size
    val baseDiversificationPts = when {
        holdingCount >= 10 -> 40
        holdingCount >= 5 -> 25
        else -> 10
    }

    val hhi = if (totalCurrentValue > 0) {
        holdings.sumOf { h ->
            val curP = priceMap[h.symbol]?.price?.takeIf { it > 0.0 } ?: h.buyPrice
            val valHolding = h.quantity * curP
            val weight = valHolding / totalCurrentValue
            weight * weight
        }
    } else 0.0

    val hhiPenalty = when {
        hhi > 0.25 -> 15
        hhi in 0.15..0.25 -> 7
        else -> 0
    }
    val scoreA = (baseDiversificationPts - hhiPenalty).coerceIn(0, 40)

    // B. Sector spread score (max 30 pts)
    val sectorValueMap = mutableMapOf<String, Double>()
    holdings.forEach { h ->
        val sec = getStockSector(h.symbol)
        val curP = priceMap[h.symbol]?.price?.takeIf { it > 0.0 } ?: h.buyPrice
        val valHolding = h.quantity * curP
        sectorValueMap[sec] = (sectorValueMap[sec] ?: 0.0) + valHolding
    }

    val sectorCount = sectorValueMap.size
    val scoreB = when {
        sectorCount >= 5 -> 30
        sectorCount >= 3 -> 20
        else -> 10
    }

    // C. Risk discipline score (max 30 pts)
    var penaltyDownCount = 0
    holdings.forEach { h ->
        val curP = priceMap[h.symbol]?.price?.takeIf { it > 0.0 } ?: h.buyPrice
        val pnlPct = if (h.buyPrice > 0) ((curP - h.buyPrice) / h.buyPrice) * 100 else 0.0
        if (pnlPct < -15.0) {
            penaltyDownCount++
        }
    }
    val scoreC = (30 - (penaltyDownCount * 5)).coerceIn(0, 30)

    val healthScore = (scoreA + scoreB + scoreC).coerceIn(0, 100)

    val ratingLabel = when {
        healthScore >= 80 -> "Healthy"
        healthScore >= 50 -> "Moderate"
        else -> "Needs Attention"
    }

    val ratingColor = when {
        healthScore >= 80 -> Color(0xFF10B981)
        healthScore >= 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. HEALTH SCORE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PORTFOLIO HEALTH SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$healthScore", fontSize = 34.sp, fontWeight = FontWeight.Black, color = ratingColor)
                            Text("/100", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ratingColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, ratingColor)
                    ) {
                        Text(
                            text = ratingLabel.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ratingColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Score Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Diversification", fontSize = 9.5.sp, color = Color(0xFF64748B))
                        Text("$scoreA / 40", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    Column {
                        Text("Sector Spread", fontSize = 9.5.sp, color = Color(0xFF64748B))
                        Text("$scoreB / 30", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    Column {
                        Text("Risk Discipline", fontSize = 9.5.sp, color = Color(0xFF64748B))
                        Text("$scoreC / 30", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
            }
        }

        // 2. SECTOR ALLOCATION PROGRESS BAR CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("SECTOR ALLOCATION BREAKDOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(10.dp))

                val sortedSectors = sectorValueMap.toList().sortedByDescending { it.second }
                sortedSectors.forEach { (sec, valVal) ->
                    val pct = if (totalCurrentValue > 0) (valVal / totalCurrentValue) * 100 else 0.0
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sec, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            Text("${String.format(Locale.US, "%.1f", pct)}%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = (pct / 100.0).toFloat().coerceIn(0f, 1f),
                            color = if (pct > 25.0) Color(0xFFEF4444) else Color(0xFF7C3AED),
                            trackColor = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // 3. BENCHMARK COMPARISON CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                    Text("BENCHMARK COMPARISON (VS NIFTY 50)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                }
                Spacer(modifier = Modifier.height(8.dp))

                val sp500Return = 6.4 // Average market benchmark
                val alpha = totalProfitLossPct - sp500Return
                val isAlphaPositive = alpha >= 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Your Portfolio Return", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("${if (totalProfitLossPct >= 0) "+" else ""}${String.format(Locale.US, "%.2f", totalProfitLossPct)}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (totalProfitLossPct >= 0) Color(0xFF10B981) else Color(0xFFEF4444))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NIFTY 50", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("+6.40%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Alpha Outperformance", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("${if (isAlphaPositive) "+" else ""}${String.format(Locale.US, "%.2f", alpha)}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isAlphaPositive) Color(0xFF10B981) else Color(0xFFEF4444))
                    }
                }
            }
        }

        // 4. REBALANCING RECOMMENDATIONS CARDS
        Text("AUTOMATED REBALANCING RECOMMENDATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))

        val recommendations = mutableListOf<Triple<String, String, Color>>()

        // Sector concentration check
        sectorValueMap.forEach { (sec, valVal) ->
            val pct = if (totalCurrentValue > 0) (valVal / totalCurrentValue) * 100 else 0.0
            if (pct > 25.0) {
                recommendations.add(
                    Triple(
                        "High Concentration in $sec",
                        "$sec accounts for ${String.format(Locale.US, "%.1f", pct)}% of portfolio (exceeds 25% single sector safety threshold). Consider trimming or rebalancing into under-represented sectors.",
                        Color(0xFFDC2626)
                    )
                )
            }
        }

        // Stop loss discipline check
        holdings.forEach { h ->
            val curP = priceMap[h.symbol]?.price?.takeIf { it > 0.0 } ?: h.buyPrice
            val pnlPct = if (h.buyPrice > 0) ((curP - h.buyPrice) / h.buyPrice) * 100 else 0.0
            if (pnlPct < -15.0) {
                val sym = h.symbol
                recommendations.add(
                    Triple(
                        "Risk Warning: $sym",
                        "$sym is down ${String.format(Locale.US, "%.1f", pnlPct)}% from buy price. Review fundamentals or consider enforcing stop-loss discipline.",
                        Color(0xFFD97706)
                    )
                )
            }
        }

        if (sectorCount < 3) {
            recommendations.add(
                Triple(
                    "Low Sector Diversity",
                    "Portfolio is limited to only $sectorCount sectors. Add exposure to Pharma, IT, or FMCG to build resilience across market cycles.",
                    Color(0xFFD97706)
                )
            )
        } else {
            recommendations.add(
                Triple(
                    "Healthy Diversification",
                    "Holdings are distributed across $sectorCount growth sectors. Maintain current sector allocation balance.",
                    Color(0xFF059669)
                )
            )
        }

        recommendations.forEach { (title, desc, color) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (color == Color(0xFF059669)) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = color)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(desc, fontSize = 11.sp, color = Color(0xFF334155), lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

// Dialog for Adding / Editing Holding with Calendar DatePicker
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHoldingDialog(
    holding: PortfolioHolding?,
    currentTotalCount: Int = 0,
    onDismiss: () -> Unit,
    onSave: (PortfolioHolding) -> Unit
) {
    var symbol by remember { mutableStateOf(holding?.symbol ?: "") }
    var quantityStr by remember { mutableStateOf(holding?.quantity?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var buyPriceStr by remember { mutableStateOf(holding?.buyPrice?.toString() ?: "") }
    var purchaseDate by remember { mutableStateOf(holding?.purchaseDate ?: SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())) }
    var selectedBroker by remember { mutableStateOf(holding?.broker ?: "Robinhood") }
    var notes by remember { mutableStateOf(holding?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEdit = holding != null

    // Suggestions
    var showSuggestions by remember { mutableStateOf(false) }
    var aiSuggestions by remember { mutableStateOf<List<StockInfo>>(emptyList()) }
    var isAiFetchingSuggestions by remember { mutableStateOf(false) }

    LaunchedEffect(symbol) {
        val query = symbol.trim()
        if (query.length >= 2 && showSuggestions) {
            delay(300) // 300ms debounce
            isAiFetchingSuggestions = true
            try {
                val results = GeminiStockAutocompleter.fetchAiSuggestions(query)
                aiSuggestions = results.map { StockInfo(it.symbol, it.name) }
            } catch (e: Exception) {
                aiSuggestions = emptyList()
            } finally {
                isAiFetchingSuggestions = false
            }
        } else {
            aiSuggestions = emptyList()
            isAiFetchingSuggestions = false
        }
    }

    val suggestions = remember(symbol, aiSuggestions) {
        if (symbol.isBlank()) emptyList()
        else {
            val query = symbol.trim()
            val localMatches = STOCK_DICTIONARY.filter {
                it.symbol.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
            }
            val existingSymbols = localMatches.map { it.symbol.uppercase() }.toSet()
            val merged = localMatches + aiSuggestions.filter { !existingSymbols.contains(it.symbol.uppercase()) }
            merged.take(6)
        }
    }

    // Calculated total investment
    val qtyNum = quantityStr.toDoubleOrNull() ?: 0.0
    val priceNum = buyPriceStr.toDoubleOrNull() ?: 0.0
    val totalInvestment = qtyNum * priceNum

    // Material 3 Calendar DatePicker Dialog
    if (showDatePicker) {
        val initialMs = remember(purchaseDate) {
            try {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(purchaseDate)
                parsed?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMs
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            purchaseDate = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Select Date", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFF7C3AED),
                    todayDateBorderColor = Color(0xFF7C3AED),
                    todayContentColor = Color(0xFF7C3AED)
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF7C3AED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEdit) "Edit Portfolio Holding" else "Add Portfolio Holding",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Track buy price, broker, quantity & date",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Form Body Scrollable
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Ticker Symbol Input with Autocomplete
                        Column {
                            Text(
                                text = "Stock Symbol",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = symbol,
                                onValueChange = {
                                    symbol = it.uppercase().trim()
                                    showSuggestions = true
                                    errorMsg = null
                                },
                                placeholder = { Text("e.g. AAPL, TSLA", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED)
                                    )
                                },
                                trailingIcon = {
                                    if (symbol.isNotEmpty()) {
                                        IconButton(onClick = { symbol = ""; showSuggestions = false }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF7C3AED),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            // Autocomplete Dropdown List
                            if (showSuggestions && (suggestions.isNotEmpty() || isAiFetchingSuggestions)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 4.dp
                                ) {
                                    Column {
                                        if (isAiFetchingSuggestions && suggestions.isEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF7C3AED), strokeWidth = 2.dp)
                                            }
                                        }

                                        suggestions.forEach { item ->
                                            val clean = item.symbol
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        symbol = clean
                                                        showSuggestions = false
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = clean,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        Text(
                                                            text = item.name,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF64748B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFEDE9FE)
                                                ) {
                                                    Text(
                                                        text = "NYSE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF7C3AED),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                        }
                                    }
                                }
                            }
                        }

                        // Quantity & Buy Price Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Quantity",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = quantityStr,
                                    onValueChange = { quantityStr = it; errorMsg = null },
                                    placeholder = { Text("e.g. 50", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF7C3AED),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Buy Price (₹)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = buyPriceStr,
                                    onValueChange = { buyPriceStr = it; errorMsg = null },
                                    placeholder = { Text("e.g. 2450.50", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CurrencyRupee,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF7C3AED),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        // Purchase Date Selection (Calendar View)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Purchase Date",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "Tap to pick from Calendar",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    .clickable { showDatePicker = true },
                                color = Color(0xFFF8FAFC)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = purchaseDate.ifBlank { "Select Date" },
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEDE9FE)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = Color(0xFF7C3AED),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Calendar",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7C3AED)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Broker Selector Chips
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Broker Account",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "(${BROKER_OPTIONS.size} supported)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Horizontal scrollable list of brokers with logos
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BROKER_OPTIONS.forEach { brokerName ->
                                    val isSelected = selectedBroker == brokerName
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFFEDE9FE) else Color(0xFFF8FAFC),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier.clickable { selectedBroker = brokerName }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            BrokerBadge(broker = brokerName)
                                            Text(
                                                text = brokerName,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Notes Optional Text Field
                        Column {
                            Text(
                                text = "Notes (Optional)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("e.g. Long-term core investment, swing target $3,000", color = Color(0xFF94A3B8), fontSize = 12.5.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF7C3AED),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        // Real-Time Total Investment Summary Box
                        if (symbol.isNotBlank() || totalInvestment > 0) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "TOTAL INVESTMENT",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF64748B),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "₹${String.format(Locale.US, "%,.2f", totalInvestment)}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        BrokerBadge(broker = selectedBroker)
                                        Text(
                                            text = selectedBroker,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        // Error Banner
                        if (errorMsg != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = errorMsg ?: "",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFB91C1C),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Footer Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    if (!isEdit && currentTotalCount >= 40) {
                                        errorMsg = "Maximum limit reached! You can add up to 40 holdings."
                                        return@Button
                                    }

                                    val cleanSym = symbol.trim().uppercase()
                                    val qty = quantityStr.toDoubleOrNull()
                                    val price = buyPriceStr.toDoubleOrNull()

                                    if (cleanSym.isBlank()) {
                                        errorMsg = "Please enter stock symbol"
                                        return@Button
                                    }
                                    if (qty == null || qty <= 0) {
                                        errorMsg = "Please enter valid quantity"
                                        return@Button
                                    }
                                    if (price == null || price <= 0) {
                                        errorMsg = "Please enter valid buy price"
                                        return@Button
                                    }

                                    val formattedTicker = cleanSym

                                    val item = PortfolioHolding(
                                        id = holding?.id ?: UUID.randomUUID().toString(),
                                        symbol = formattedTicker,
                                        quantity = qty,
                                        buyPrice = price,
                                        purchaseDate = purchaseDate.ifBlank { "Recent" },
                                        broker = selectedBroker,
                                        notes = notes
                                    )
                                    onSave(item)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f).height(46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Save Holding", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

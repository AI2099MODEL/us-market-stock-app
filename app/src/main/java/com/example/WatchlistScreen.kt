package com.example

import android.Manifest
import java.util.Locale
import java.util.Calendar
import androidx.compose.foundation.border
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    modifier: Modifier = Modifier,
    onSymbolSelected: (String) -> Unit = {},
    viewModel: WatchlistViewModel = viewModel(
        factory = WatchlistViewModel.Factory(WatchlistRepository(MyApplication.database.priceAlertDao()))
    )
) {
    val context = LocalContext.current
    
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val currentPrices by viewModel.currentPrices.collectAsStateWithLifecycle()
    
    var showDialog by remember { mutableStateOf(false) }
    var ticker by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    // Start background worker automatically
    LaunchedEffect(Unit) {
        WorkerUtils.schedulePriceAlertWorker(context)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color(0xFFEDE9FE),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Watchlist & Alerts",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                letterSpacing = (-0.1).sp
                            )
                        }
                    }

                    Button(
                        onClick = { showDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Stock Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Stats Pills Row
            val activeCount = alerts.count { it.isAlertActive }
            val reachedCount = alerts.count { alert ->
                val cmp = currentPrices[alert.ticker]
                cmp != null && if (alert.priceTarget > 0) cmp >= alert.priceTarget else cmp <= alert.priceTarget
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text("Active Alerts", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text("$activeCount / ${alerts.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text("Triggered", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text("$reachedCount Reached", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                OutlinedButton(
                    onClick = { sendTestNotification(context) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2563EB))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alert", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            }

            if (alerts.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { showDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to add your first stock to Watchlist",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(alerts) { alert ->
                        val cmp = currentPrices[alert.ticker]
                        val isTargetReached = if (cmp != null) {
                            if (alert.priceTarget > 0) cmp >= alert.priceTarget else cmp <= alert.priceTarget
                        } else false

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSymbolSelected(alert.ticker) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isTargetReached) Color(0xFF10B981) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val badgeBg = when {
                                        !alert.isAlertActive -> Color(0xFF94A3B8)
                                        isTargetReached -> Color(0xFF10B981)
                                        else -> Color(0xFF2563EB)
                                    }
                                    val badgeText = when {
                                        !alert.isAlertActive -> "INACTIVE"
                                        isTargetReached -> "TARGET HIT"
                                        else -> "ACTIVE ALERT"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = Color.White,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.2.sp,
                                            maxLines = 1
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteAlert(alert.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CompanyLogoView(symbol = alert.ticker, modifier = Modifier.size(18.dp))

                                    val displaySymbol = alert.ticker
                                    Text(
                                        text = displaySymbol,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFFF8F9FA))
                                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Target ",
                                                color = Color(0xFF64748B),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                            Text(
                                                text = "$${String.format(Locale.US, "%.1f", alert.priceTarget)}",
                                                color = Color(0xFF0F172A),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    val cmpText = if (cmp != null) "$${String.format(Locale.US, "%.1f", cmp)}" else "--"
                                    val cmpColor = if (isTargetReached) Color(0xFF10B981) else Color(0xFF0F172A)

                                    Text(
                                        text = cmpText,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cmpColor,
                                        maxLines = 1
                                    )
                                }

                                // Dividend alert banner
                                val matchedDividend = MASTER_DIVIDEND_LEST.find {
                                    it.symbol.equals(alert.ticker, ignoreCase = true) ||
                                    it.symbol.equals(alert.ticker, ignoreCase = true)
                                }
                                if (matchedDividend != null) {
                                    val calTomorrow = Calendar.getInstance()
                                    calTomorrow.add(Calendar.DAY_OF_YEAR, 1)
                                    val tomorrowDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calTomorrow.time)

                                    val isExDateTomorrow = matchedDividend.exDate == tomorrowDateStr
                                    val isPayoutTomorrow = getPayoutDate(matchedDividend.exDate) == tomorrowDateStr

                                    if (isExDateTomorrow) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFF0F1))
                                                .border(0.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(4.dp))
                                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "🔔 EX-DIVIDEND TOMORROW\nAmount: $${matchedDividend.amountPerShare}",
                                                fontSize = 8.5.sp,
                                                lineHeight = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }

                                    if (isPayoutTomorrow) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFECFDF5))
                                                .border(0.5.dp, Color(0xFF6EE7B7), RoundedCornerShape(4.dp))
                                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "💰 PAYOUT TOMORROW\nAmount: $${matchedDividend.amountPerShare}",
                                                fontSize = 8.5.sp,
                                                lineHeight = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isTargetReached) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                    border = BorderStroke(0.5.dp, if (isTargetReached) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isTargetReached) "Target Hit! 🎉" else "Alert Active",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTargetReached) Color(0xFF047857) else Color(0xFF2563EB)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AddPriceAlertAiDialog(
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onDismiss = { showDialog = false },
                    onAddAlert = { formattedTicker, targetPrice ->
                        viewModel.addAlert(formattedTicker, targetPrice)
                        showDialog = false
                    }
                )
            }
        }
    }
}

private fun resolveStockTicker(
    query: String,
    currentSelectedTicker: String,
    aiSuggestions: List<StockInfo>
): String {
    if (currentSelectedTicker.isNotBlank()) return currentSelectedTicker
    val clean = query.trim()
    if (clean.isEmpty()) return ""

    // 1. Match in local STOCK_DICTIONARY
    val dictExact = STOCK_DICTIONARY.find {
        it.name.equals(clean, ignoreCase = true) ||
        it.symbol.equals(clean, ignoreCase = true) ||
        it.symbol.equals(clean, ignoreCase = true)
    }
    if (dictExact != null) return dictExact.symbol

    val dictPartial = STOCK_DICTIONARY.find {
        it.name.contains(clean, ignoreCase = true) ||
        clean.contains(it.name, ignoreCase = true) ||
        it.symbol.contains(clean, ignoreCase = true) ||
        clean.contains(it.symbol, ignoreCase = true)
    }
    if (dictPartial != null) return dictPartial.symbol

    // 2. Match in AI suggestions
    val aiMatch = aiSuggestions.find {
        it.name.equals(clean, ignoreCase = true) ||
        it.symbol.equals(clean, ignoreCase = true) ||
        it.name.contains(clean, ignoreCase = true) ||
        clean.contains(it.name, ignoreCase = true)
    }
    if (aiMatch != null) {
        val sym = aiMatch.symbol
        return sym
    }

    // 3. Clean up input, strip non-alphanumeric/period/carat characters
    val sanitized = clean.replace("[^a-zA-Z0-9.^]".toRegex(), "").uppercase(Locale.ROOT)
    if (sanitized.isEmpty()) return ""

    return if (sanitized.contains(".") || sanitized.startsWith("^")) {
        sanitized
    } else {
        sanitized
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPriceAlertAiDialog(
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDismiss: () -> Unit,
    onAddAlert: (String, Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTicker by remember { mutableStateOf("") }
    var selectedStockName by remember { mutableStateOf("") }
    var targetPriceInput by remember { mutableStateOf("") }

    var fetchedPrice by remember { mutableStateOf<Double?>(null) }
    var isFetchingPrice by remember { mutableStateOf(false) }

    var aiSuggestions by remember { mutableStateOf<List<StockInfo>>(emptyList()) }
    var isAiFetching by remember { mutableStateOf(false) }

    val quickPresets = listOf("AAPL", "TSLA", "MSFT", "NVDA", "JPM", "WFC", "ZOMATO")

    // AI Autocomplete Debounced Search
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length >= 2) {
            delay(300)
            isAiFetching = true
            try {
                val results = GeminiStockAutocompleter.fetchAiSuggestions(query)
                aiSuggestions = results.map { StockInfo(it.symbol, it.name) }
            } catch (e: Exception) {
                aiSuggestions = emptyList()
            } finally {
                isAiFetching = false
            }
        } else {
            aiSuggestions = emptyList()
            isAiFetching = false
        }
    }

    val combinedSuggestions = remember(searchQuery, aiSuggestions) {
        val query = searchQuery.trim()
        if (query.isEmpty()) emptyList()
        else {
            val localMatches = STOCK_DICTIONARY.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.symbol.contains(query, ignoreCase = true)
            }
            val existingSymbols = localMatches.map { it.symbol.uppercase() }.toSet()
            val newAiMatches = aiSuggestions.filter { !existingSymbols.contains(it.symbol.uppercase()) }
            (localMatches + newAiMatches).take(6)
        }
    }

    // Effective Ticker to query for live price
    val effectiveTicker = remember(selectedTicker, searchQuery, combinedSuggestions) {
        if (selectedTicker.isNotBlank()) selectedTicker
        else if (combinedSuggestions.isNotEmpty()) combinedSuggestions.first().symbol
        else resolveStockTicker(searchQuery, "", aiSuggestions)
    }

    // Fetch live market price when effective stock ticker changes
    LaunchedEffect(effectiveTicker) {
        if (effectiveTicker.isNotBlank()) {
            isFetchingPrice = true
            try {
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    YahooRetrofit.service.getChart(effectiveTicker, "1d", "1m")
                }
                val price = response.chart?.result?.firstOrNull()?.meta?.regularMarketPrice
                if (price != null && price > 0.0) {
                    fetchedPrice = price
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingPrice = false
            }
        } else {
            fetchedPrice = null
        }
    }

    fun selectStock(symbol: String, name: String? = null) {
        val formatted = symbol
        selectedTicker = formatted
        val matchedName = name ?: STOCK_DICTIONARY.find { it.symbol.equals(formatted, ignoreCase = true) }?.name ?: symbol
        selectedStockName = matchedName
        searchQuery = "$matchedName (${formatted})"
        aiSuggestions = emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text("Add Price Alert", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("AI-assisted stock lookup — type any name or symbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Notifications Control Card inside Add Stock Dialog
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasNotificationPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (hasNotificationPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasNotificationPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (hasNotificationPermission) MaterialTheme.colorScheme.primary else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (hasNotificationPermission) "Push Alerts Enabled" else "Enable Notifications",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasNotificationPermission) MaterialTheme.colorScheme.onSurface else Color(0xFF92400E)
                                )
                                Text(
                                    text = if (hasNotificationPermission) "Instant alert when price hits target" else "Tap Enable to get push alert on phone",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Presets Row
                Text("Quick Tickers:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickPresets) { preset ->
                        FilterChip(
                            selected = selectedTicker == preset,
                            onClick = { selectStock(preset) },
                            label = { Text(preset, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Stock Name or Ticker Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedTicker = ""
                        selectedStockName = ""
                    },
                    label = { Text("Stock Name or Symbol") },
                    placeholder = { Text("e.g. Apple, Tesla, Meta, Google") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (isAiFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // AI & Local Search Suggestions Overlay
                if (combinedSuggestions.isNotEmpty() && selectedTicker.isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI & Local Matches:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (isAiFetching) {
                                    Text("Searching...", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            combinedSuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectStock(suggestion.symbol, suggestion.name) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(suggestion.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(
                                            suggestion.symbol,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "Select",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Market Price Badge & Target Price Calculator
                if (effectiveTicker.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Resolved Stock: ${effectiveTicker}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                if (isFetchingPrice) {
                                    Text("Fetching Live Price...", fontSize = 10.sp, color = Color(0xFF15803D))
                                } else if (fetchedPrice != null) {
                                    Text("Live CMP: $${String.format(Locale.US, "%.2f", fetchedPrice)}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                                }
                            }

                            // Quick Target Price Presets (+2%, +5%, +10%, -2%, -5%)
                            if (fetchedPrice != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Quick Target Presets:", fontSize = 10.sp, color = Color(0xFF166534), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val price = fetchedPrice!!
                                    listOf(
                                        "+2%" to price * 1.02,
                                        "+5%" to price * 1.05,
                                        "+10%" to price * 1.10,
                                        "-2%" to price * 0.98,
                                        "-5%" to price * 0.95
                                    ).forEach { (label, calculatedPrice) ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                            modifier = Modifier.clickable {
                                                targetPriceInput = String.format("%.2f", calculatedPrice)
                                            }
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (label.startsWith("+")) Color(0xFF15803D) else Color(0xFFB91C1C),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Target Price Input
                OutlinedTextField(
                    value = targetPriceInput,
                    onValueChange = { targetPriceInput = it },
                    label = { Text("Target Price ($)") },
                    placeholder = { Text("e.g. 1050.00") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val targetVal = targetPriceInput.replace(",", "").toDoubleOrNull()
            val isInputValid = effectiveTicker.isNotBlank() && targetVal != null && targetVal > 0

            Button(
                onClick = {
                    if (isInputValid) {
                        onAddAlert(effectiveTicker, targetVal!!)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = isInputValid
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Alert", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper extension for scaling switch
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

@android.annotation.SuppressLint("MissingPermission")
private fun sendTestNotification(context: Context) {
    try {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "PRICE_ALERTS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("StockBreak Price Alert Engine")
            .setContentText("Background price monitoring & push notifications are ACTIVE!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(9999, builder.build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

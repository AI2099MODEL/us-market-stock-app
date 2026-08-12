package com.example
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ShowChart

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan


import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextOverflow

import kotlinx.coroutines.*

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.ripple.rememberRipple
import com.google.android.gms.ads.AdRequest
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.nativead.MediaView
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.View
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.Color as AndroidColor


@Composable
fun MiniSparkline(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF16A34A)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.75f)
            lineTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.75f, h * 0.25f)
            lineTo(w, h * 0.05f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color = color,
            radius = 2.5.dp.toPx(),
            center = Offset(w, h * 0.05f)
        )
    }
}



data class BreakoutAsset(
    val name: String,
    val trendPercentage: String,
    val targetValue: String,
    val currentPrice: String,
    val stopLossPercentage: String
)


enum class Screen {
    HOME, AUTO_TRADER, LIVE, DIVIDENDS, WATCHLEST, NEWS, PREMIUM
}

@Composable
fun AppMainHeaderRow() {
    val tradesFlow = remember { MyApplication.database.virtualTradeDao().getAllTradesFlow() }
    val virtualTrades by tradesFlow.collectAsState(initial = emptyList())

    val closedPnl = virtualTrades.filter { it.status != "ACTIVE" }.sumOf { it.profitAmount }
    val activePnl = virtualTrades.filter { it.status == "ACTIVE" }.sumOf { it.profitAmount }
    val totalAiPnl = closedPnl + activePnl

    val displayPnl = totalAiPnl
    val isPositive = displayPnl >= 0.0
    val formattedPnl = if (isPositive) {
        "+₹${String.format(Locale.US, "%,.2f", displayPnl)}"
    } else {
        "-₹${String.format(Locale.US, "%,.2f", kotlin.math.abs(displayPnl))}"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Branding Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF1E1B4B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "StockBreak India Logo",
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "StockBreak India",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "AI Stock Signals Generator",
                    fontSize = 9.5.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right: Single row on black background showing live animation dot + amount only
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Animated Live Radar Pulsing Dot
            Box(
                modifier = Modifier.size(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (isPositive) Color(0xFF34D399).copy(alpha = pulseAlpha) else Color(0xFFFB7185).copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isPositive) Color(0xFF10B981) else Color(0xFFF43F5E))
                )
            }

            Text(
                text = formattedPnl,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) Color(0xFF34D399) else Color(0xFFFB7185)
            )
        }
    }
}

@Composable
fun TopScrollingTickerBanner() {
    val scrollState = rememberScrollState()
    val isDark = LocalThemeMode.current.value

    val tradesFlow = remember { MyApplication.database.virtualTradeDao().getAllTradesFlow() }
    val virtualTrades by tradesFlow.collectAsState(initial = emptyList())

    val activeTrades = virtualTrades.filter { it.status == "ACTIVE" }
    val closedTrades = virtualTrades.filter { it.status != "ACTIVE" }

    val closedPnl = closedTrades.sumOf { it.profitAmount }
    val activePnl = activeTrades.sumOf { it.profitAmount }
    val totalAutoTraderPnl = closedPnl + activePnl

    val autoTraderPnlFormatted = if (totalAutoTraderPnl >= 0.0) {
        "+₹${String.format(Locale.US, "%,.2f", totalAutoTraderPnl)}"
    } else {
        "-₹${String.format(Locale.US, "%,.2f", kotlin.math.abs(totalAutoTraderPnl))}"
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val maxScroll = scrollState.maxValue
            if (maxScroll > 0) {
                if (scrollState.value >= maxScroll) {
                    scrollState.scrollTo(0)
                } else {
                    scrollState.animateScrollTo(
                        value = scrollState.value + 100,
                        animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
                    )
                }
            }
            delay(120)
        }
    }

    Surface(
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
        modifier = Modifier.fillMaxWidth().height(30.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                color = if (isDark) Color(0xFF7C3AED) else MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LIVE TICKER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TickerItem(
                    tag = "AUTOTRADER P&L",
                    text = "Daily P&L: $autoTraderPnlFormatted (${activeTrades.size} Active)",
                    isUp = totalAutoTraderPnl >= 0.0
                )
                TickerItem(tag = "SP500", text = "S&P 500 Index Trading Near Historic Highs", isUp = true)
                TickerItem(tag = "AAPL", text = "AAPL Tech Signal: EMA 20/50 Crossover", isUp = true)
                TickerItem(tag = "MSFT", text = "MSFT RSI Neutral-Bullish • Cloud Momentum", isUp = true)
                TickerItem(tag = "XLF", text = "Financial Sector ETF (XLF) Holding Support", isUp = true)
                TickerItem(tag = "DISCLAIMER", text = "Educational & Informational Purpose Only • SEC/FINRA Compliant Educational Notice", isUp = null)
                TickerItem(tag = "JPM", text = "Earnings & Dividend Catalyst Focus", isUp = true)
                TickerItem(tag = "NVDA", text = "AI Data Center Demand Surge Continues", isUp = true)
            }
        }
    }
}

@Composable
fun TickerItem(tag: String, text: String, isUp: Boolean?) {
    val isDark = LocalThemeMode.current.value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = when (isUp) {
                true -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                false -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                else -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            },
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = tag,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = when (isUp) {
                    true -> if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                    false -> if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                    else -> if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            maxLines = 1
        )
    }
}



@Composable
fun GlobalTopBar(
    goldPrice: String,
    goldChange: String,
    goldIsPositive: Boolean,
    silverPrice: String,
    silverChange: String,
    silverIsPositive: Boolean,
    crudePrice: String,
    crudeChange: String,
    crudeIsPositive: Boolean,
    natgasPrice: String,
    natgasChange: String,
    natgasIsPositive: Boolean
) {
    val tradesFlow = remember { MyApplication.database.virtualTradeDao().getAllTradesFlow() }
    val virtualTrades by tradesFlow.collectAsState(initial = emptyList())
    val activeTrades = virtualTrades.filter { it.status == "ACTIVE" }
    val closedTrades = virtualTrades.filter { it.status != "ACTIVE" }
    val closedPnl = closedTrades.sumOf { it.profitAmount }
    val activePnl = activeTrades.sumOf { it.profitAmount }
    val totalAutoTraderPnl = closedPnl + activePnl

    val autoTraderPnlFormatted = if (totalAutoTraderPnl >= 0.0) {
        "+₹${String.format(Locale.US, "%,.2f", totalAutoTraderPnl)}"
    } else {
        "-₹${String.format(Locale.US, "%,.2f", kotlin.math.abs(totalAutoTraderPnl))}"
    }

    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            while (true) {
                val maxScroll = scrollState.maxValue
                if (maxScroll > 0) {
                    if (scrollState.value >= maxScroll) {
                        scrollState.scrollTo(0)
                    } else {
                        scrollState.animateScrollTo(
                            value = scrollState.value + 100,
                            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
                        )
                    }
                }
                delay(120)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) {
                // AUTOTRADER P&L
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xFF7C3AED),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "AI SIGNALS P&L",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = autoTraderPnlFormatted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalAutoTraderPnl >= 0.0) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))

                // GOLD
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "GOLD",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = goldPrice,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = goldChange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (goldIsPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))

                // SILVER
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SILVER",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = silverPrice,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = silverChange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (silverIsPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))

                // CRUDE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CRUDE",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF97316),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = crudePrice,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = crudeChange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (crudeIsPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))

                // NATGAS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NATGAS",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0EA5E9),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = natgasPrice,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = natgasChange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (natgasIsPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text("•", fontSize = 9.sp, color = Color(0xFF475569))
            }
        }
    }
}

val TopNavLightBg = Color(0xFFF1F5F9)
val NavActiveBlue = Color(0xFF7C3AED)

@Composable
fun AppTopNavigation(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = NavActiveBlue
        )
    ) {
        Surface(
            color = Color(0xFFF8FAFC),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1508433957232-3107f5fd5995?q=80&w=2000&auto=format&fit=crop",
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.30f
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_usa_menu_bg),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                    alpha = 0.45f
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                val navItems = listOf(
                    Triple(Screen.HOME, Icons.Default.Home, "Breakouts"),
                    Triple(Screen.DIVIDENDS, Icons.Default.AttachMoney, "Dividends"),
                    Triple(Screen.WATCHLEST, Icons.Default.Favorite, "Watchlist"),
                    Triple(Screen.NEWS, Icons.Default.Newspaper, "News"),
                    Triple(Screen.PREMIUM, Icons.Default.CardMembership, "Premium")
                )

                navItems.forEach { (screen, icon, label) ->
                    val isSelected = currentScreen == screen
                    
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.18f else 1.0f,
                        animationSpec = tween(durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "IconScale"
                    )

                    val iconOffsetY by animateDpAsState(
                        targetValue = if (isSelected) (-4).dp else 0.dp,
                        animationSpec = tween(durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "IconOffsetY"
                    )

                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 52.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "PillWidth"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) NavActiveBlue else Color(0xFF1E293B).copy(alpha = 0.7f),
                        animationSpec = tween(durationMillis = 250),
                        label = "ContentColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (currentScreen != screen) {
                                    InterstitialAdManager.showAd(context) {
                                        onScreenSelected(screen)
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(pillWidth)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NavActiveBlue.copy(alpha = 0.24f))
                        )
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier
                                .offset(y = iconOffsetY)
                                .graphicsLayer(
                                    scaleX = iconScale,
                                    scaleY = iconScale
                                )
                                .size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.1.sp
                    )
                }
            }
        }
    }
}
}
}

@Composable
fun AppBottomNavigation(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    AppTopNavigation(currentScreen = currentScreen, onScreenSelected = onScreenSelected)
}

@Composable
fun LoginAuthScreen(
    onMobileLoginSuccess: (String) -> Unit = {},
    onContinueAsGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var authError by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Light StockBreak India background icon & theme watermark
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(320.dp)
                    .graphicsLayer(alpha = 0.12f),
                contentScale = ContentScale.Fit
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Subtle light background watermark accent inside card header
                Image(
                    painter = painterResource(id = R.drawable.ic_usa_menu_bg),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .graphicsLayer(alpha = 0.15f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = Color(0xFF1E1B4B),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "StockBreak India Logo",
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Welcome to StockBreak India",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "Sign in to access AI Breakout Signals, Real-time Market Analytics & Portfolio Intelligence",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Button(
                        onClick = {
                            isSigningIn = true
                            authError = null
                            coroutineScope.launch {
                                try {
                                    GoogleAuthManager.signInWithGoogle(
                                        context = context,
                                        onSuccess = {
                                            isSigningIn = false
                                        },
                                        onError = { err ->
                                            isSigningIn = false
                                            authError = err
                                        }
                                    )
                                } catch (e: Throwable) {
                                    isSigningIn = false
                                    authError = e.localizedMessage ?: "Google Sign-In unavailable"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSigningIn
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF4285F4)
                                    )
                                }
                            }
                            Text(
                                text = if (isSigningIn) "Signing in with Google..." else "Continue with Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (!authError.isNullOrBlank()) {
                        Text(
                            text = authError!!,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "By signing in, you agree to StockBreak Terms of Service and Privacy Policy.",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    TextButton(
                        onClick = { onContinueAsGuest() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip & Continue to App →",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val currentUser by GoogleAuthManager.currentUser.collectAsState()
    val prefs = remember { context.getSharedPreferences("user_auth_prefs", Context.MODE_PRIVATE) }
    var isMobileLoggedIn by remember { mutableStateOf(prefs.getBoolean("is_mobile_logged_in", false)) }
    var isGuestUser by remember { mutableStateOf(prefs.getBoolean("is_guest_user", false)) }

    if (currentUser == null && !isMobileLoggedIn && !isGuestUser) {
        LoginAuthScreen(
            onMobileLoginSuccess = { phone ->
                prefs.edit().putBoolean("is_mobile_logged_in", true).putString("mobile_number", phone).apply()
                isMobileLoggedIn = true
            },
            onContinueAsGuest = {
                prefs.edit().putBoolean("is_guest_user", true).apply()
                isGuestUser = true
            }
        )
    } else {
        StockBreakMainScreen()
    }
}

@Composable
fun StockBreakMainScreen() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedSymbol by remember { mutableStateOf<String?>(null) }
    
    var goldPrice by remember { mutableStateOf("₹71,450") }
    var goldChange by remember { mutableStateOf("+1.25%") }
    var goldIsPositive by remember { mutableStateOf(true) }

    var silverPrice by remember { mutableStateOf("₹84,200") }
    var silverChange by remember { mutableStateOf("+0.85%") }
    var silverIsPositive by remember { mutableStateOf(true) }

    var crudePrice by remember { mutableStateOf("₹5,890") }
    var crudeChange by remember { mutableStateOf("+1.45%") }
    var crudeIsPositive by remember { mutableStateOf(true) }

    var natgasPrice by remember { mutableStateOf("₹215.40") }
    var natgasChange by remember { mutableStateOf("-0.45%") }
    var natgasIsPositive by remember { mutableStateOf(false) }

    // Persistent Live Commodity Tickers polling across ALL 5 tabs
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val g = IndianCommodityRepository.fetchCommodityData("GOLD")
                    val s = IndianCommodityRepository.fetchCommodityData("SILVER")
                    val c = IndianCommodityRepository.fetchCommodityData("CRUDEOIL")
                    val n = IndianCommodityRepository.fetchCommodityData("NATURALGAS")

                    if (g != null && g.price > 0) {
                        goldPrice = String.format(Locale.US, "₹%,.0f", g.price)
                        goldChange = String.format(Locale.US, "%+.2f%%", g.changePercent)
                        goldIsPositive = g.changePercent >= 0
                    }
                    if (s != null && s.price > 0) {
                        silverPrice = String.format(Locale.US, "₹%,.0f", s.price)
                        silverChange = String.format(Locale.US, "%+.2f%%", s.changePercent)
                        silverIsPositive = s.changePercent >= 0
                    }
                    if (c != null && c.price > 0) {
                        crudePrice = String.format(Locale.US, "₹%,.0f", c.price)
                        crudeChange = String.format(Locale.US, "%+.2f%%", c.changePercent)
                        crudeIsPositive = c.changePercent >= 0
                    }
                    if (n != null && n.price > 0) {
                        natgasPrice = String.format(Locale.US, "₹%,.2f", n.price)
                        natgasChange = String.format(Locale.US, "%+.2f%%", n.changePercent)
                        natgasIsPositive = n.changePercent >= 0
                    }
                }
            } catch (e: Exception) {}
            delay(4000)
        }
    }

    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = WindowInsets(0.dp),
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    AppMainHeaderRow()
                    GlobalTopBar(
                        goldPrice = goldPrice,
                        goldChange = goldChange,
                        goldIsPositive = goldIsPositive,
                        silverPrice = silverPrice,
                        silverChange = silverChange,
                        silverIsPositive = silverIsPositive,
                        crudePrice = crudePrice,
                        crudeChange = crudeChange,
                        crudeIsPositive = crudeIsPositive,
                        natgasPrice = natgasPrice,
                        natgasChange = natgasChange,
                        natgasIsPositive = natgasIsPositive
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF0F172A))
                        .navigationBarsPadding()
                ) {
                    AdBannerView()
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Handle Bar pill at top of rounded light grey container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp),
                            shape = CircleShape,
                            color = Color(0xFFCBD5E1)
                        ) {}
                    }

                    // Top Navigation bar inside the rounded sheet
                    AppTopNavigation(currentScreen) { currentScreen = it }

                    // Active screen content
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentScreen) {
                            Screen.HOME -> DashboardScreen(onSymbolSelected = { symbol -> 
                                selectedSymbol = symbol
                                currentScreen = Screen.LIVE
                            })
                            Screen.AUTO_TRADER -> AutoTraderTabContent()
                            Screen.LIVE -> LiveScreen(initialSymbol = selectedSymbol, forceMode = 1)
                            Screen.PREMIUM -> PremiumScreen(onSymbolSelected = { symbol ->
                                selectedSymbol = symbol
                                currentScreen = Screen.LIVE
                            })
                            Screen.DIVIDENDS -> DividendsScreen(onSymbolSelected = { symbol ->
                                selectedSymbol = symbol
                                currentScreen = Screen.LIVE
                            })
                            Screen.WATCHLEST -> WatchlistScreen(onSymbolSelected = { symbol ->
                                selectedSymbol = symbol
                                currentScreen = Screen.LIVE
                            })
                            Screen.NEWS -> NewsScreen()
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

fun buildNonPersonalizedAdRequest(): AdRequest {
    return AdRequest.Builder()
        .addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply {
            putString("npa", "1")
        })
        .build()
}

@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = ""
) {
    // Ads removed as requested
}

@Composable
fun NativeAdViewComposable(
    modifier: Modifier = Modifier,
    adUnitId: String = ""
) {
    // Ads removed as requested
}

object InterstitialAdManager {
    fun loadAd(context: Context, adUnitId: String = "") {
        // Ads removed as requested
    }

    fun showAd(context: Context, onAdDismissed: () -> Unit = {}) {
        onAdDismissed()
    }
}

fun loadAndShowAppOpenAd(
    activity: android.app.Activity,
    adUnitId: String = ""
) {
    // Ads removed as requested
}

object RewardedVideoAdManager {
    fun loadAd(context: Context, adUnitId: String = "") {}

    fun showAd(context: Context, onUserEarnedReward: () -> Unit = {}, onAdDismissed: () -> Unit = {}) {
        onUserEarnedReward()
        onAdDismissed()
    }
}

val LocalThemeMode = compositionLocalOf<androidx.compose.runtime.MutableState<Boolean>> { error("No theme provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            GoogleAuthManager.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "GoogleAuthManager init error", e)
        }
        try {
            MarketEngine.startEngine(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "MarketEngine start error", e)
        }
        /*
        try {
            MobileAds.initialize(this) {
                try {
                    InterstitialAdManager.loadAd(this)
                    RewardedVideoAdManager.loadAd(this)
                } catch (e: Throwable) {}
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "MobileAds initialize error", e)
        }
        */
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE) }
            val isDarkTheme = remember { mutableStateOf(prefs.getBoolean("is_dark_theme", false)) }

            CompositionLocalProvider(LocalThemeMode provides isDarkTheme) {
                MyApplicationTheme(darkTheme = isDarkTheme.value) {
                    MainApp()
                }
            }
        }
    }
}

suspend fun fetchRealTimeData(ticker: String): String {
    return try {
        val response = YahooRetrofit.service.getChart(ticker, "1mo", "1d")
        val result = response.chart?.result?.firstOrNull()
        val price = result?.meta?.regularMarketPrice
        val closePrices = result?.indicators?.quote?.firstOrNull()?.close?.filterNotNull()
        
        if (price != null && closePrices != null && closePrices.isNotEmpty()) {
             "Real-time Data for $ticker:\nCurrent Price: ₹price\nLast 5 days close: ${closePrices.takeLast(5)}"
        } else {
             "Could not fetch real data for $ticker"
        }
    } catch (e: Exception) {
        "Error fetching data: ${e.message}"
    }
}

@Composable
fun StockBreakoutCard(
    res: ScanResult,
    onSymbolSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false
) {
    var isFavorite by remember { mutableStateOf(false) }

    val displaySymbol = res.ticker
    val changePct = res.changePercent ?: 0.0

    val targetVal = res.target1 ?: (res.price * 1.08)
    val formattedTarget = String.format(Locale.US, "%.2f", targetVal)

    val stopLossVal = res.stopLoss ?: (res.price * 0.95)
    val stopLossPct = if (res.price > 0) ((res.price - stopLossVal) / res.price) * 100 else 5.0
    val formattedStopLossPct = String.format(Locale.US, "%.1f", stopLossPct)
    val formattedPrice = "₹" + String.format(Locale.US, "%.2f", res.price)

    val morningOpen = res.openPrice ?: res.previousClose ?: res.price
    val isBelowMorningOpen = res.price < morningOpen
    val cmpColor = if (isBelowMorningOpen) Color(0xFFEF4444) else Color(0xFF10B981)

    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onSymbolSelected(res.ticker)
            },
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
            // Row 1: Strong Breakout Badge Tag + Heart Icon
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            val timeInMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val isOptionsMarketClosed = (timeInMins >= 900 || timeInMins < 555)

            val badgeText = when {
                res.assetType == "INDEX_OPTION" || res.assetType == "STOCK_OPTION" -> {
                    if (isOptionsMarketClosed) "INTRADAY • STARTS 9:15 AM" else "INTRADAY OPTION"
                }
                res.isBtst && res.assetType == "EQUITY" -> "BTST ELIGIBLE (EQUITY)"
                res.assetType == "COMMODITY" -> "MCX COMMODITY"
                else -> res.signalStrength.ifBlank { "STRONG BREAKOUT" }.uppercase()
            }

            val badgeBg = when {
                res.assetType == "INDEX_OPTION" || res.assetType == "STOCK_OPTION" -> {
                    if (isOptionsMarketClosed) Color(0xFF64748B) else Color(0xFF0284C7)
                }
                res.isBtst && res.assetType == "EQUITY" -> Color(0xFF7C3AED)
                res.assetType == "COMMODITY" -> Color(0xFFD97706)
                else -> StrongBreakoutGreen
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isFavorite) StopLossRedText else TextMutedGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2: Company Icon + Ticker Symbol + Technical Scorecard Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CompanyLogoView(symbol = res.ticker, modifier = Modifier.size(18.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = displaySymbol,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val expiryStr = remember(res.ticker) { IndianCommodityRepository.getExpiryDateDisplay(res.ticker) }
                        Text(
                            text = "Exp: $expiryStr",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMutedGray,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF3E8FF),
                    border = BorderStroke(0.5.dp, Color(0xFFDDD6FE))
                ) {
                    Text(
                        text = "Score ${res.score}/100",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Row 3: Target Box (left) + Percentage Increase above CMP (right)
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
                            color = TextMutedGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = formattedTarget,
                            color = TextPrimaryDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val sign = if (changePct > 0) "+" else ""
                    Text(
                        text = "$sign${String.format(Locale.US, "%.2f", changePct)}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (changePct >= 0) TrendTextGreen else StopLossRedText
                    )
                    Text(
                        text = formattedPrice,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = cmpColor,
                        maxLines = 1
                    )
                }
            }

            // Row 4: Stop Loss Red Pill Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFF0F1))
                    .padding(vertical = 3.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STOP-LOSS (-$formattedStopLossPct%)",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    asset: BreakoutAsset,
    onClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            // Row 1: Strong Breakout Badge Tag + Heart Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StrongBreakoutGreen)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STRONG BREAKOUT",
                        color = Color.White,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isFavorite) StopLossRedText else TextMutedGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2: Company Icon + Asset Title + Green Trend Metric Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CompanyLogoView(symbol = asset.name, modifier = Modifier.size(18.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = asset.name,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val expiryStr = remember(asset.name) { IndianCommodityRepository.getExpiryDateDisplay(asset.name) }
                        Text(
                            text = "Exp: $expiryStr",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMutedGray,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(TextGreenBadge)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = asset.trendPercentage,
                            color = TrendTextGreen,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Row 3: Target Box & Current Price
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
                            color = TextMutedGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = asset.targetValue,
                            color = TextPrimaryDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "₹${asset.currentPrice}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriceNumericRed,
                    maxLines = 1
                )
            }

            // Row 4: Stop-Loss Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(StopLossRedBg)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STOP-LOSS (${asset.stopLossPercentage})",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StopLossRedText,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// Helper extensions for database caching
fun ScannedBreakout.toScanResult() = ScanResult(
    ticker = ticker,
    name = name,
    price = price,
    strategies = strategies,
    score = score,
    reasons = reasons,
    signalStrength = signalStrength,
    stopLoss = stopLoss,
    target1 = target1,
    target2 = target2,
    historicalPrices = emptyList(),
    previousClose = previousClose,
    openPrice = openPrice,
    change = change,
    changePercent = changePercent,
    isBtst = isBtst,
    assetType = assetType
)

fun ScanResult.toScannedBreakout() = ScannedBreakout(
    ticker = ticker,
    name = name,
    price = price,
    strategies = strategies,
    score = score,
    reasons = reasons,
    signalStrength = signalStrength,
    stopLoss = stopLoss,
    target1 = target1,
    target2 = target2,
    previousClose = previousClose,
    openPrice = openPrice,
    change = change,
    changePercent = changePercent,
    isBtst = isBtst,
    assetType = assetType
)

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, onSymbolSelected: (String) -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    var activeSubTab by remember { mutableStateOf("BREAKOUTS") } // "BREAKOUTS", "AUTOTRADER", or "ANALYSIS"
    var selectedStockForAnalysis by remember { mutableStateOf<String?>(null) }
    var scanResults by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    var loadingPercent by remember { mutableIntStateOf(0) }
    var lastFetchedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            currentTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            loadingPercent = 0
            while (loadingPercent < 95) {
                delay((150..350).random().toLong())
                loadingPercent += (1..2).random()
                if (loadingPercent > 95) loadingPercent = 95
            }
        } else {
            loadingPercent = 100
        }
    }

    // Load from cache first, then run a periodic background refresh to keep it fresh
    LaunchedEffect(Unit) {
        // Load cached immediately to prevent empty/stale display
        try {
            val cached = withContext(Dispatchers.IO) {
                MyApplication.database.scannedBreakoutDao().getAllScannedBreakoutsList()
            }
            if (cached.isNotEmpty()) {
                scanResults = cached.map { it.toScanResult() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        while (isActive) {
            try {
                isScanning = true
                val fresh = withContext(Dispatchers.IO) { StockScanner.scanMultiple("Breakouts") }
                if (fresh.isNotEmpty()) {
                    scanResults = fresh
                    lastFetchedTime = System.currentTimeMillis()
                    withContext(Dispatchers.IO) {
                        MyApplication.database.scannedBreakoutDao().clearAll()
                        MyApplication.database.scannedBreakoutDao().insertBreakouts(fresh.map { it.toScannedBreakout() })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isScanning = false
            }
            // Refresh breakouts in background every 3 minutes
            delay(180000)
        }
    }

    val liveQuotes by ShoonyaWebSocketManager.liveQuotes.collectAsState()

    // Real-time CMP Refresh for Breakout Contracts from Dhan WebSocket live quotes
    LaunchedEffect(liveQuotes) {
        if (liveQuotes.isNotEmpty() && scanResults.isNotEmpty() && activeSubTab == "BREAKOUTS") {
            scanResults = scanResults.map { item ->
                val baseComm = IndianCommodityRepository.resolveBaseSymbol(item.ticker)
                val cleanTicker = item.ticker.split(" ").firstOrNull() ?: item.ticker
                val q = liveQuotes[item.ticker] ?: liveQuotes[cleanTicker] ?: liveQuotes[baseComm]
                
                if (q != null && q.price > 0.0) {
                    val finalLivePrice = q.price
                    val prevClose = finalLivePrice - q.change
                    val dayOpen = item.openPrice ?: finalLivePrice
                    item.copy(
                        price = finalLivePrice,
                        openPrice = dayOpen,
                        previousClose = prevClose,
                        change = q.change,
                        changePercent = q.changePercent
                    )
                } else {
                    item
                }
            }
            lastFetchedTime = System.currentTimeMillis()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F6))
    ) {
        // Submenu selector: Top Breakouts, Auto Trader, and Single Stock AI side-by-side
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
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
                    // Breakouts button
                    val isBreakoutsSelected = activeSubTab == "BREAKOUTS"
                    Surface(
                        onClick = { activeSubTab = "BREAKOUTS" },
                        modifier = Modifier.weight(1f),
                        color = if (isBreakoutsSelected) Color(0xFFEDE9FE) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isBreakoutsSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Top Breakouts",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBreakoutsSelected) Color(0xFF7C3AED) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Auto Trader button
                    val isAutoTraderSelected = activeSubTab == "AUTOTRADER"
                    Surface(
                        onClick = { activeSubTab = "AUTOTRADER" },
                        modifier = Modifier.weight(1f),
                        color = if (isAutoTraderSelected) Color(0xFFEDE9FE) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoGraph,
                                    contentDescription = null,
                                    tint = if (isAutoTraderSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "AI Signals",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoTraderSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Analysis button
                    val isSingleStockSelected = activeSubTab == "ANALYSIS"
                    Surface(
                        onClick = { activeSubTab = "ANALYSIS" },
                        modifier = Modifier.weight(1f),
                        color = if (isSingleStockSelected) Color(0xFFEDE9FE) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = null,
                                    tint = if (isSingleStockSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Analysis",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSingleStockSelected) Color(0xFF7C3AED) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
        }

        if (activeSubTab == "AUTOTRADER") {
            AutoTraderTabContent(modifier = Modifier.weight(1f))
        } else if (activeSubTab == "ANALYSIS") {
            LiveScreen(modifier = Modifier.weight(1f), initialSymbol = selectedStockForAnalysis, forceMode = 1)
        } else {
            if (isScanning && scanResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { loadingPercent / 100f },
                                color = TabActiveBlue,
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 5.dp
                            )
                            Text(
                                text = "₹loadingPercent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TabActiveBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scanning SP500 200 for breakout signals...", fontSize = 11.sp, color = TextMutedGray)
                    }
                }
            } else {
                val filteredResults = remember(scanResults) {
                    scanResults.filter { it.assetType == "COMMODITY" }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 2.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredResults, key = { it.ticker }) { res ->
                            StockBreakoutCard(
                                res = res,
                                onSymbolSelected = { ticker ->
                                    selectedStockForAnalysis = ticker
                                    activeSubTab = "ANALYSIS"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationsScreen(modifier: Modifier = Modifier, onSymbolSelected: (String) -> Unit = {}) {
    var isLoading by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (results.isEmpty()) {
            isLoading = true
            withContext(Dispatchers.IO) {
                results = StockScanner.scanMultiple("Breakouts")
            }
            isLoading = false
        }
        
        while (isActive) {
            delay(60000) // update every 60 seconds to avoid Yahoo rate limit
            if (results.isNotEmpty()) {
                val updated = withContext(Dispatchers.IO) {
                    results.map { res ->
                        async {
                            try {
                                val response = YahooRetrofit.service.getChart(res.ticker, "1d", "1m")
                                val price = response.chart?.result?.firstOrNull()?.meta?.regularMarketPrice ?: res.price
                                res.copy(price = price)
                            } catch (e: Exception) {
                                res
                            }
                        }
                    }.awaitAll()
                }
                results = updated
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Text("Algorithmic Tech-Tips", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning S&P 500 for technical signals...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(results.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (res in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                StockBreakoutCard(res = res, onSymbolSelected = onSymbolSelected)
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
}





@Composable
fun PortfolioScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Portfolio - Coming Soon", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfigScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("InvestifyPrefs", android.content.Context.MODE_PRIVATE)
    
    var activeProvider by remember { mutableStateOf(sharedPrefs.getString("active_provider", "E-Trade") ?: "E-Trade") }
    
    // E-Trade
    var etradeClientCode by remember { mutableStateOf(sharedPrefs.getString("etrade_client_code", "") ?: "") }
    var etradeApiKey by remember { mutableStateOf(sharedPrefs.getString("etrade_api_key", "") ?: "") }
    
    // Fyers
    var ibkrAppId by remember { mutableStateOf(sharedPrefs.getString("ibkr_app_id", "") ?: "") }
    var fyersToken by remember { mutableStateOf(sharedPrefs.getString("fyers_token", "") ?: "") }
    
    // Dhan
    var schwabClientId by remember { mutableStateOf(sharedPrefs.getString("schwab_client_id", "") ?: "") }
    var dhanToken by remember { mutableStateOf(sharedPrefs.getString("dhan_token", "") ?: "") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Broker Configuration", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Text("Select and configure your broker API for live CMP and Indices.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Active Data Provider", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("E-Trade", "Interactive Brokers", "Charles Schwab").forEach { provider ->
                val selected = activeProvider == provider
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clickable { 
                        activeProvider = provider 
                        sharedPrefs.edit().putString("active_provider", provider).apply()
                    }
                ) {
                    Text(provider, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (activeProvider == "E-Trade") {
            OutlinedTextField(
                value = etradeClientCode,
                onValueChange = { etradeClientCode = it; sharedPrefs.edit().putString("etrade_client_code", it).apply() },
                label = { Text("E-Trade Client ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = etradeApiKey,
                onValueChange = { etradeApiKey = it; sharedPrefs.edit().putString("etrade_api_key", it).apply() },
                label = { Text("E-Trade API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else if (activeProvider == "Interactive Brokers") {
            OutlinedTextField(
                value = ibkrAppId,
                onValueChange = { ibkrAppId = it; sharedPrefs.edit().putString("ibkr_app_id", it).apply() },
                label = { Text("IBKR App ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = fyersToken,
                onValueChange = { fyersToken = it; sharedPrefs.edit().putString("fyers_token", it).apply() },
                label = { Text("Fyers Access Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else if (activeProvider == "Charles Schwab") {
            OutlinedTextField(
                value = schwabClientId,
                onValueChange = { schwabClientId = it; sharedPrefs.edit().putString("schwab_client_id", it).apply() },
                label = { Text("Schwab Client ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = dhanToken,
                onValueChange = { dhanToken = it; sharedPrefs.edit().putString("dhan_token", it).apply() },
                label = { Text("Schwab Access Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Security Notice", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Your API keys are stored locally on your device in SharedPreferences. Do not share your API keys.", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun PremiumScreen(
    modifier: Modifier = Modifier,
    onSymbolSelected: (String) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        LiveScreen(initialSymbol = "", forceMode = 0)
    }
}

@Composable
private fun PremiumFeatureRow(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF34D399),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}




package com.example

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.Manrope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Shared Cache for Video feeds
object VideoCache {
    var cachedVideos: List<VideoItem> = emptyList()
}

data class VideoItem(
    val id: String,
    val title: String,
    val channel: String,
    val tag: String,
    val tagBgColor: Color,
    val videoId: String,
    val directUrl: String,
    val timeAgo: String,
    val category: String,
    val isLive: Boolean = false,
    val isAvailable: Boolean = true,
    val pubDateMs: Long = 0L
)

fun parsePubDateToMillis(pubDateStr: String): Long {
    if (pubDateStr.isBlank()) return 0L
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = sdf.parse(pubDateStr)
        date?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

suspend fun fetchYouTubeChannelVideos(
    channelId: String,
    channelName: String,
    tag: String,
    tagColor: Color
): List<VideoItem> = withContext(Dispatchers.IO) {
    val result = mutableListOf<VideoItem>()
    try {
        val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        val apiUrl = "https://api.rss2json.com/v1/api.json?rss_url=" + URLEncoder.encode(rssUrl, "UTF-8")
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode == 200) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonStr)
            if (jsonObj.optString("status") == "ok") {
                val items = jsonObj.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val rawTitle = item.optString("title", "")
                    val title = android.text.Html.fromHtml(rawTitle, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    val link = item.optString("link", "")
                    val guid = item.optString("guid", "")
                    val videoId = when {
                        guid.startsWith("yt:video:") -> guid.removePrefix("yt:video:")
                        link.contains("watch?v=") -> link.substringAfter("watch?v=").substringBefore("&")
                        link.contains("shorts/") -> link.substringAfter("shorts/").substringBefore("?")
                        else -> ""
                    }
                    if (videoId.isNotBlank()) {
                        val pubDate = item.optString("pubDate", "")
                        val timeAgo = parsePubDateToTimeAgo(pubDate)
                        val pubDateMs = parsePubDateToMillis(pubDate)
                        result.add(
                            VideoItem(
                                id = "yt_${channelId}_$videoId",
                                title = title,
                                channel = channelName,
                                tag = tag,
                                tagBgColor = tagColor,
                                videoId = videoId,
                                directUrl = if (link.isNotBlank()) link else "https://www.youtube.com/watch?v=$videoId",
                                timeAgo = timeAgo,
                                category = channelName.uppercase(),
                                pubDateMs = pubDateMs
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    result
}

fun parsePubDateToTimeAgo(pubDateStr: String): String {
    if (pubDateStr.isBlank()) return "Recently"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = sdf.parse(pubDateStr) ?: return pubDateStr
        val now = System.currentTimeMillis()
        val diffMs = now - date.time
        val diffMins = diffMs / (1000 * 60)
        val diffHours = diffMins / 60
        val diffDays = diffHours / 24

        when {
            diffMins < 1 -> "Just now"
            diffMins < 60 -> "${diffMins}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.US).format(date)
        }
    } catch (e: Exception) {
        "Recently"
    }
}

@Composable
fun NewsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = News, 1 = Videos
    var selectedNewsCategory by remember { mutableStateOf("ALL") }
    var selectedVideoCategory by remember { mutableStateOf("ALL") }

    var isNewsLoading by remember { mutableStateOf(false) }
    var newsList by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }

    var isRefreshingVideos by remember { mutableStateOf(false) }
    var videoList by remember { mutableStateOf<List<VideoItem>>(VideoCache.cachedVideos) }
    var removedVideoIds by remember { mutableStateOf(setOf<String>()) }

    // Fetch News Articles
    fun loadNews() {
        isNewsLoading = true
        scope.launch {
            try {
                newsList = NewsTickerService.fetchNewsArticles(selectedNewsCategory)
            } finally {
                isNewsLoading = false
            }
        }
    }

    // Fetch Video Feeds
    fun refreshVideos() {
        scope.launch {
            isRefreshingVideos = true
            try {
                val fetched = withContext(Dispatchers.IO) {
                    val d1 = async { fetchYouTubeChannelVideos("UCEAZeUIeJs0ijQiqTC5Wg6w", "CNBC", "CNBC LATEST", Color(0xFF0284C7)) }
                    val d2 = async { fetchYouTubeChannelVideos("UCw5TLrz3qADabwezTEcOmgQ", "Fidelity", "FIDELITY LATEST", Color(0xFF00D09C)) }
                    val d3 = async { fetchYouTubeChannelVideos("UCvJJ_dzjViJCoLf5uKUTwoA", "Bloomberg Tech", "BLOOMBERG TECH", Color(0xFF2563EB)) }
                    val d4 = async { fetchYouTubeChannelVideos("UC43vP6323_y7x_96Lw4L75A", "Yahoo Finance", "YAHOO FINANCE", Color(0xFF7C3AED)) }
                    val d5 = async { fetchYouTubeChannelVideos("UC16niRr50-MSBwiO3YDb3RA", "WSJ Markets", "WSJ MARKETS", Color(0xFFD97706)) }
                    val all = listOf(d1.await(), d2.await(), d3.await(), d4.await(), d5.await()).flatten()
                    all.distinctBy { it.videoId }.sortedByDescending { it.pubDateMs }
                }
                if (fetched.isNotEmpty()) {
                    videoList = fetched
                    VideoCache.cachedVideos = fetched
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshingVideos = false
            }
        }
    }

    LaunchedEffect(selectedNewsCategory) {
        loadNews()
    }

    LaunchedEffect(Unit) {
        refreshVideos()
        while (isActive) {
            delay(30 * 60 * 1000L)
            refreshVideos()
        }
    }

    // Filtered lists
    val filteredNews = remember(newsList, selectedNewsCategory) {
        when (selectedNewsCategory) {
            "S&P 500 & Dow" -> newsList.filter { it.category.contains("S&P 500", ignoreCase = true) || it.title.contains("S&P 500", ignoreCase = true) || it.title.contains("Dow", ignoreCase = true) }
            "Corporate & Q3" -> newsList.filter { it.category.contains("Corporate", ignoreCase = true) || it.category.contains("Q3", ignoreCase = true) || it.title.contains("Corporate", ignoreCase = true) || it.title.contains("Earnings", ignoreCase = true) }
            "FII/DII", "FII / DII" -> newsList.filter { it.category.contains("Institutional", ignoreCase = true) || it.category.contains("FII", ignoreCase = true) || it.title.contains("Institutional", ignoreCase = true) || it.title.contains("Flows", ignoreCase = true) }
            else -> newsList
        }
    }

    val visibleVideos = remember(videoList, selectedVideoCategory, removedVideoIds) {
        videoList.filter { video ->
            video.id !in removedVideoIds &&
            when (selectedVideoCategory) {
                "CNBC" -> video.channel.contains("CNBC", ignoreCase = true) || video.tag.contains("CNBC", ignoreCase = true)
                "FIDELITY" -> video.channel.contains("Fidelity", ignoreCase = true) || video.tag.contains("FIDELITY", ignoreCase = true)
                "BLOOMBERG" -> video.channel.contains("Bloomberg", ignoreCase = true) || video.tag.contains("BLOOMBERG", ignoreCase = true)
                "YAHOO" -> video.channel.contains("Yahoo", ignoreCase = true) || video.tag.contains("YAHOO", ignoreCase = true)
                "WSJ" -> video.channel.contains("WSJ", ignoreCase = true) || video.tag.contains("WSJ", ignoreCase = true)
                else -> true
            }
        }
    }

    fun openUrlDirectly(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isNewsSelected = activeTab == 0
                Surface(
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f),
                    color = if (isNewsSelected) Color(0xFFEDE9FE) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Market News",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNewsSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                val isVideosSelected = activeTab == 1
                Surface(
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f),
                    color = if (isVideosSelected) Color(0xFFEDE9FE) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Videos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVideosSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                val isBrokersSelected = activeTab == 2
                Surface(
                    onClick = { activeTab = 2 },
                    modifier = Modifier.weight(1f),
                    color = if (isBrokersSelected) Color(0xFFEDE9FE) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Open Account",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBrokersSelected) Color(0xFF7C3AED) else Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Tab Content
        if (activeTab == 0) {
            // NEWS TAB CONTENT
            if (filteredNews.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No News Articles Found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNews, key = { it.id }) { article ->
                        NewsCardItem(
                            article = article,
                            onOpenUrl = { openUrlDirectly(it) }
                        )
                    }
                }
            }
        } else if (activeTab == 1) {
            // VIDEOS TAB CONTENT
            if (visibleVideos.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Videos Available",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleVideos, key = { it.id }) { video ->
                        VideoCardRowItem(
                            video = video,
                            onOpenUrl = { openUrlDirectly(video.directUrl) },
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Watch '${video.title}' on YouTube: ${video.directUrl}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                            },
                            onHide = {
                                removedVideoIds = removedVideoIds + video.id
                            }
                        )
                    }
                }
            }
        } else {
            // BROKER ACCOUNTS REFERRAL TAB CONTENT
            BrokerAccountsTabContent(onOpenUrl = { openUrlDirectly(it) })
        }
    }
}

fun getPublisherDomain(source: String, articleUrl: String = ""): String {
    val s = source.lowercase(Locale.ROOT)
    return when {
        s.contains("wall street") || s.contains("wsj") -> "wsj.com"
        s.contains("cnbc") -> "cnbc.com"
        s.contains("bloomberg") -> "bloomberg.com"
        s.contains("reuters") -> "reuters.com"
        s.contains("marketwatch") -> "marketwatch.com"
        s.contains("barron") -> "barrons.com"
        s.contains("yahoo") -> "finance.yahoo.com"
        s.contains("investor") || s.contains("ibd") -> "investors.com"
        s.contains("seeking") -> "seekingalpha.com"
        s.contains("fool") || s.contains("motley") -> "fool.com"
        s.contains("forbes") -> "forbes.com"
        else -> {
            if (articleUrl.isNotBlank()) {
                try {
                    val host = java.net.URI(articleUrl).host
                    if (!host.isNullOrBlank() && !host.contains("google")) host else "wsj.com"
                } catch (e: Exception) {
                    "wsj.com"
                }
            } else {
                "wsj.com"
            }
        }
    }
}

fun getPublisherLogoUrl(source: String, articleUrl: String = ""): String {
    val domain = getPublisherDomain(source, articleUrl)
    return "https://www.google.com/s2/favicons?domain=$domain&sz=128"
}

fun getPublisherBadgeInfo(source: String): Pair<Color, String> {
    val s = source.lowercase(Locale.ROOT)
    return when {
        s.contains("wall street") || s.contains("wsj") -> Pair(Color(0xFF0F172A), "WSJ")
        s.contains("cnbc") -> Pair(Color(0xFF0284C7), "CNBC")
        s.contains("bloomberg") -> Pair(Color(0xFF1E293B), "BBG")
        s.contains("reuters") -> Pair(Color(0xFFD97706), "RTRS")
        s.contains("marketwatch") -> Pair(Color(0xFF166534), "MW")
        s.contains("barron") -> Pair(Color(0xFF854D0E), "BRN")
        s.contains("yahoo") -> Pair(Color(0xFF7C3AED), "YHOO")
        s.contains("investor") || s.contains("ibd") -> Pair(Color(0xFF0284C7), "IBD")
        s.contains("seeking") -> Pair(Color(0xFFC2410C), "SA")
        s.contains("fool") || s.contains("motley") -> Pair(Color(0xFF4338CA), "TMF")
        else -> Pair(Color(0xFF475569), source.take(2).uppercase(Locale.ROOT))
    }
}

fun getArticleTickerTag(article: NewsArticle): String {
    val text = (article.title + " " + article.source + " " + article.category).uppercase(Locale.ROOT)
    return when {
        text.contains("SP500") || text.contains("S&P 500") -> "S&P 500"
        text.contains("DOW") -> "DOW"
        text.contains("NASDAQ") -> "NASDAQ"
        text.contains("AAPL") || text.contains("APPLE") -> "AAPL"
        text.contains("MSFT") || text.contains("MICROSOFT") -> "MSFT"
        text.contains("NVDA") || text.contains("NVIDIA") -> "NVDA"
        text.contains("TSLA") || text.contains("TESLA") -> "TSLA"
        text.contains("AMZN") || text.contains("AMAZON") -> "AMZN"
        text.contains("GOOG") || text.contains("ALPHABET") -> "GOOGL"
        text.contains("FED") || text.contains("RESERVE") -> "US FED"
        else -> "NYSE"
    }
}

@Composable
fun NewsCardItem(
    article: NewsArticle,
    onOpenUrl: (String) -> Unit
) {
    val sentimentColor = when (article.sentiment.uppercase(Locale.ROOT)) {
        "BULLISH" -> Color(0xFF16A34A)
        "BEARISH" -> Color(0xFFDC2626)
        else -> Color(0xFF2563EB)
    }

    val (pubBgColor, pubInitial) = getPublisherBadgeInfo(article.source)
    val tickerTag = getArticleTickerTag(article)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(article.url) },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Content Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Publisher Logo + Publisher Name + Green Solid Sentiment Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = getPublisherLogoUrl(article.source, article.url),
                        contentDescription = article.source,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(0.5.dp, Color(0xFFCBD5E1), CircleShape),
                        error = {
                            Surface(
                                shape = CircleShape,
                                color = pubBgColor,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = pubInitial,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        },
                        loading = {
                            Surface(
                                shape = CircleShape,
                                color = pubBgColor,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = pubInitial,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    )

                    Text(
                        text = article.source,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Manrope,
                        color = Color(0xFF334155),
                        letterSpacing = (-0.1).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Headline: Bold Black Title (max 2 lines)
                Text(
                    text = article.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Manrope,
                    color = Color(0xFF0F172A),
                    lineHeight = 19.sp,
                    letterSpacing = (-0.15).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle / Date line e.g. "2 hours ago · Feb 9, 2023"
                Text(
                    text = article.timeAgo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Manrope,
                    color = Color(0xFF64748B),
                    letterSpacing = (-0.1).sp
                )
            }

            // Right Side Image Container with rounded corners & ticker overlay on image bottom-right
            val hasRealImage = article.imageUrl.isNotBlank() && !isGoogleNewsOrDefaultLogo(article.imageUrl)

            var imageLoadFailed by remember(article.id) { mutableStateOf(false) }
            var logoLoadFailed by remember(article.id) { mutableStateOf(false) }

            val imageModel = remember(article.imageUrl, hasRealImage, imageLoadFailed, logoLoadFailed) {
                if (hasRealImage && !imageLoadFailed) {
                    article.imageUrl
                } else if (!logoLoadFailed) {
                    getPublisherLogoUrl(article.source, article.url)
                } else {
                    NewsTickerService.getCategoryImage(article.category, article.title)
                }
            }

            val isShowingLogo = !logoLoadFailed && (!hasRealImage || imageLoadFailed)

            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = article.title,
                    contentScale = if (isShowingLogo) ContentScale.Fit else ContentScale.Crop,
                    modifier = if (isShowingLogo) Modifier.size(36.dp) else Modifier.fillMaxSize(),
                    onError = {
                        if (hasRealImage && !imageLoadFailed) {
                            imageLoadFailed = true
                        } else {
                            logoLoadFailed = true
                        }
                    }
                )

                if (hasRealImage) {
                    // Dark gradient overlay at bottom of thumbnail image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )

                    // News Channel Favicon Overlay inside top-left of thumbnail picture
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .border(0.5.dp, Color(0xFFE2E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = getPublisherLogoUrl(article.source, article.url),
                            contentDescription = article.source,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // White ticker badge tag overlay at bottom-right corner of thumbnail image
                    Text(
                        text = tickerTag,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 5.dp, bottom = 4.dp),
                        maxLines = 1
                    )
                } else {
                    // Minimalist pill for ticker tag on a clean light container
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = tickerTag,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCardRowItem(
    video: VideoItem,
    onOpenUrl: () -> Unit,
    onShare: () -> Unit,
    onHide: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Side-by-Side Row Layout: Text LEFT, Thumbnail RIGHT (as requested)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Text Block to the LEFT of Thumbnail
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Headline Above: Bold Black, Max 2 Lines, Ellipsis if longer
                    Text(
                        text = video.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )

                    // Source Icon and Name Below the headline (slightly bigger)
                    val s = video.channel.lowercase(Locale.ROOT)
                    val domain = when {
                        s.contains("dhan") -> "dhan.co"
                        s.contains("fidelity") -> "fidelity.com"
                        s.contains("zee") -> "zeebiz.com"
                        s.contains("cnbc") -> "cnbctv18.com"
                        s.contains("et now") || s.contains("swadesh") -> "economictimes.indiatimes.com"
                        else -> "youtube.com"
                    }
                    val logoUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=128"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .border(0.5.dp, Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val context = LocalContext.current
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.channel,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape),
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = video.channel.take(1).uppercase(Locale.ROOT),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            )
                        }

                        Text(
                            text = video.channel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Right Thumbnail Container (110x70px)
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    val context = LocalContext.current
                    val primaryUrl = remember(video.videoId) { "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg" }
                    val fallbackUrl = remember(video.videoId) { "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg" }

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(primaryUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fallbackUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )

                    // Red Play Button Icon Centered
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                        shadowElevation = 3.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(3.dp)
                                .fillMaxSize()
                        )
                    }

                    // Small Dark Time Badge in Bottom-Right Corner
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = video.timeAgo,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Below Each Row: 3 Icons Aligned Right (open/external-link, share, hide)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onOpenUrl() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Link",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onShare() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onHide() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Hide Video",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

data class BrokerReferralItem(
    val id: String,
    val name: String,
    val logoDomain: String,
    val tagline: String,
    val primaryColor: Color,
    val referralUrl: String,
    val features: List<String>,
    val isRecommended: Boolean = false
)

val BROKER_REFERRAL_LEST = listOf(
    BrokerReferralItem(
        id = "dhan",
        name = "Dhan (Primary MCX Partner)",
        logoDomain = "dhan.co",
        tagline = "Lightning Fast API Trading & Advanced MCX Commodity Charts",
        primaryColor = Color(0xFFEF4444),
        referralUrl = "https://dhan.co",
        features = listOf(
            "₹20 Flat Brokerage per MCX Commodity Order",
            "Direct API Integration & Live Margin Reporting",
            "Advanced Options Chain & TradingView Charting"
        ),
        isRecommended = true
    ),
    BrokerReferralItem(
        id = "zerodha",
        name = "Zerodha (Kite)",
        logoDomain = "zerodha.com",
        tagline = "India's Largest Retail Broker for Commodities & Futures",
        primaryColor = Color(0xFF38BDF8),
        referralUrl = "https://zerodha.com",
        features = listOf(
            "₹20 per executed MCX Futures & Options order",
            "Kite Connect API & Robust Order Routing",
            "Console Analytics & Tax Reporting"
        )
    ),
    BrokerReferralItem(
        id = "angelone",
        name = "Angel One",
        logoDomain = "angelone.in",
        tagline = "Full-Service Broking & Commodity Intelligence",
        primaryColor = Color(0xFFFF7700),
        referralUrl = "https://www.angelone.in",
        features = listOf(
            "Smart API & Commodity Advisory",
            "Zero AMC for First Year",
            "Dedicated MCX Margin Support"
        )
    ),
    BrokerReferralItem(
        id = "upstox",
        name = "Upstox",
        logoDomain = "upstox.com",
        tagline = "High-Speed Trading Platform for Derivatives",
        primaryColor = Color(0xFF7C3AED),
        referralUrl = "https://upstox.com",
        features = listOf(
            "Super Fast MCX Order Execution",
            "Flat ₹20 Intra-day & F&O Brokerage",
            "Advanced Portfolio Analytics"
        )
    ),
    BrokerReferralItem(
        id = "groww",
        name = "Groww",
        logoDomain = "groww.in",
        tagline = "Simple & Transparent Investment Platform",
        primaryColor = Color(0xFF16A34A),
        referralUrl = "https://groww.in",
        features = listOf(
            "Clean & Intuitive Interface",
            "Transparent MCX Charges & P&L Statements",
            "Instant Fund Transfers via UPI"
        )
    ),
    BrokerReferralItem(
        id = "icicidirect",
        name = "ICICI Direct",
        logoDomain = "icicidirect.com",
        tagline = "Bank-Grade Security & Institutional Commodity Research",
        primaryColor = Color(0xFF003366),
        referralUrl = "https://www.icicidirect.com",
        features = listOf(
            "3-in-1 Bank Account Integration",
            "In-depth MCX Research & Commodity Reports",
            "Robust Risk Management Controls"
        )
    )
)

@Composable
fun BrokerAccountsTabContent(
    onOpenUrl: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header Info Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "OPEN US BROKERAGE ACCOUNT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Top US Brokerage Houses",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Choose a US broker below to open an account with $0 commissions, fractional share trading, and advanced trading platforms.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        items(BROKER_REFERRAL_LEST, key = { it.id }) { broker ->
            BrokerReferralCard(
                broker = broker,
                onOpenUrl = onOpenUrl
            )
        }

        item {
            // Bottom Info Note
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "More broker referral links can be updated anytime upon request.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun BrokerReferralCard(
    broker: BrokerReferralItem,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (broker.isRecommended) 1.5.dp else 1.dp,
                color = if (broker.isRecommended) broker.primaryColor else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onOpenUrl(broker.referralUrl) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (broker.isRecommended) 2.dp else 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Logo + Broker Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    SubcomposeAsyncImage(
                        model = "https://www.google.com/s2/favicons?domain=${broker.logoDomain}&sz=128",
                        contentDescription = "${broker.name} Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                        error = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(broker.primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = broker.name.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = broker.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            if (broker.isRecommended) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = broker.primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = broker.tagline,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Features list
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                broker.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = broker.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = feature,
                            fontSize = 11.5.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Clickable Action CTA Button
            Button(
                onClick = { onOpenUrl(broker.referralUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = broker.primaryColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Open Free ${broker.name} Account",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}



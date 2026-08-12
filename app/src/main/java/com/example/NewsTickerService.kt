package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class NewsArticle(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val source: String = "Market News",
    val timeAgo: String = "Just now",
    val category: String = "General",
    val sentiment: String = "NEUTRAL", // BULLISH, BEARISH, NEUTRAL
    val url: String = "",
    val imageUrl: String = ""
)

fun isGoogleNewsOrDefaultLogo(url: String?): Boolean {
    if (url.isNullOrBlank()) return true
    val lower = url.lowercase(Locale.ROOT)
    return lower.contains("googleusercontent.com") ||
           lower.contains("gstatic.com") ||
           (lower.contains("google.com") && !lower.contains("favicons")) ||
           lower.contains("favicon.ico") ||
           (lower.contains("logo") && (lower.contains("google") || lower.contains("g-") || lower.contains("/g_")))
}

object NewsTickerService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val DEFAULT_NEWS = listOf(
        "S&P 500 surges past 5,200 mark driven by Big Tech & Nvidia AI rally",
        "Dow Jones gains 450 points following strong institutional buying on Wall Street",
        "US Federal Reserve maintains benchmark interest rate with dovish inflation outlook",
        "Microsoft, Apple & Alphabet report record quarterly cloud and AI revenues",
        "Nvidia & Semiconductor stocks lead broad tech market rally",
        "Tesla announces new gigafactory expansion and autonomous driving milestone",
        "Berkshire Hathaway increases cash reserves to record high amid market volatility",
        "US Treasury yields ease as inflation metrics align with Fed target"
    )

    fun getCategoryImage(category: String, title: String): String {
        val lower = (category + " " + title).lowercase(Locale.ROOT)
        return when {
            // Auto / EV
            lower.contains("auto") || lower.contains("ev") || lower.contains("motor") || lower.contains("maruti") || lower.contains("mahindra") || lower.contains("tata motor") ->
                "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=400&q=80"
            // Green Energy / Oil / Power / Capex
            lower.contains("energy") || lower.contains("green") || lower.contains("power") || lower.contains("oil") || lower.contains("solar") || lower.contains("reliance") ->
                "https://images.unsplash.com/photo-1466611653911-95081537e5b7?w=400&q=80"
            // Banking / Financial / RBI / Vault / JPM / BAC / WFC
            lower.contains("bank") || lower.contains("rbi") || lower.contains("fii") || lower.contains("dii") || lower.contains("jpm") || lower.contains("bac") || lower.contains("wfc") || lower.contains("repo") ->
                "https://images.unsplash.com/photo-1541354329998-f4d9a9f9297f?w=400&q=80"
            // Tech / IT / AI / Cloud / MSFT / GOOGL / AMZN
            lower.contains("tech") || lower.contains("tcs") || lower.contains("infosys") || lower.contains("wipro") || lower.contains("cloud") || lower.contains("ai ") ->
                "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400&q=80"
            // Global / Fed / US / Wall Street
            lower.contains("global") || lower.contains("fed") || lower.contains("us ") || lower.contains("wall street") ->
                "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?w=400&q=80"
            // IPO / Earnings / Dividend / Result / Allotment
            lower.contains("ipo") || lower.contains("dividend") || lower.contains("earning") || lower.contains("q1") || lower.contains("q2") || lower.contains("q3") || lower.contains("q4") ->
                "https://images.unsplash.com/photo-1535320903710-d993d3d77d29?w=400&q=80"
            // Metals / Steel / Mining / Vedanta
            lower.contains("metal") || lower.contains("steel") || lower.contains("vedanta") || lower.contains("coal") ->
                "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=400&q=80"
            // Pharma / Healthcare / Lab
            lower.contains("pharma") || lower.contains("cipla") || lower.contains("health") || lower.contains("drug") ->
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=400&q=80"
            // Real Estate / Infrastructure / Skyline
            lower.contains("realty") || lower.contains("infra") || lower.contains("dlf") || lower.contains("cement") ->
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=400&q=80"
            // S&P 500 / Dow / Market Bull
            else -> "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400&q=80"
        }
    }

    val SAMPLE_ARTICLES = listOf(
        NewsArticle(
            id = "1",
            title = "S&P 500 Surges Past 5,200 Mark Driven by Big Tech & Semiconductor AI Rally",
            source = "The Wall Street Journal",
            timeAgo = "15 mins ago",
            category = "S&P 500 & Dow",
            sentiment = "BULLISH",
            url = "https://www.wsj.com/news/markets",
            imageUrl = "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400&q=80"
        ),
        NewsArticle(
            id = "2",
            title = "Dow Jones Gains 450 Points Following Strong Institutional Wall Street Capital Inflows",
            source = "CNBC",
            timeAgo = "32 mins ago",
            category = "FII / DII",
            sentiment = "BULLISH",
            url = "https://www.cnbc.com/markets",
            imageUrl = "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=400&q=80"
        ),
        NewsArticle(
            id = "3",
            title = "Federal Reserve Signals Data-Dependent Monetary Stance as Inflation Metrics Moderate",
            source = "Bloomberg",
            timeAgo = "1 hour ago",
            category = "Global Markets",
            sentiment = "NEUTRAL",
            url = "https://www.bloomberg.com/markets",
            imageUrl = "https://images.unsplash.com/photo-1541354329998-f4d9a9f9297f?w=400&q=80"
        ),
        NewsArticle(
            id = "4",
            title = "Microsoft & Alphabet Lead Tech Rally After Record High Quarterly Cloud & Enterprise AI Earnings",
            source = "MarketWatch",
            timeAgo = "2 hours ago",
            category = "Corporate & Q3",
            sentiment = "BULLISH",
            url = "https://www.marketwatch.com",
            imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400&q=80"
        ),
        NewsArticle(
            id = "5",
            title = "Apple Inc. Board Approves $110 Billion Share Buyback Program & Capital Return Strategy",
            source = "Barron's",
            timeAgo = "3 hours ago",
            category = "Corporate & Q3",
            sentiment = "BULLISH",
            url = "https://www.barrons.com",
            imageUrl = "https://images.unsplash.com/photo-1466611653911-95081537e5b7?w=400&q=80"
        ),
        NewsArticle(
            id = "6",
            title = "US Treasury Yields Ease Below 4.2% as Economic Activity Data Confirms Soft Landing",
            source = "Reuters",
            timeAgo = "4 hours ago",
            category = "Global Markets",
            sentiment = "BULLISH",
            url = "https://www.reuters.com/markets",
            imageUrl = "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?w=400&q=80"
        ),
        NewsArticle(
            id = "7",
            title = "Crude Oil Futures Ease Relieving Margin Pressures on Airline & Transport Stocks",
            source = "Yahoo Finance",
            timeAgo = "5 hours ago",
            category = "Global Markets",
            sentiment = "NEUTRAL",
            url = "https://finance.yahoo.com",
            imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&q=80"
        ),
        NewsArticle(
            id = "8",
            title = "Tesla Electric Vehicle Delivery Figures Exceed Wall Street Estimates Driven by Global Demand",
            source = "Investor's Business Daily",
            timeAgo = "6 hours ago",
            category = "Corporate & Q3",
            sentiment = "BULLISH",
            url = "https://www.investors.com",
            imageUrl = "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=400&q=80"
        ),
        NewsArticle(
            id = "9",
            title = "Tech IPO Pipeline Expands as SEC Filings Reveal High Growth Enterprise Software Prospects",
            source = "Seeking Alpha",
            timeAgo = "7 hours ago",
            category = "IPO & Earnings",
            sentiment = "NEUTRAL",
            url = "https://seekingalpha.com",
            imageUrl = "https://images.unsplash.com/photo-1535320903710-d993d3d77d29?w=400&q=80"
        ),
        NewsArticle(
            id = "10",
            title = "Nvidia Expands AI Chip Partnerships Across Major US Defense & Healthcare Enterprise Clients",
            source = "The Motley Fool",
            timeAgo = "8 hours ago",
            category = "Corporate & Q3",
            sentiment = "BULLISH",
            url = "https://www.fool.com",
            imageUrl = "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=400&q=80"
        )
    )

    suspend fun fetchLatestNews(): List<String> = withContext(Dispatchers.IO) {
        val articles = fetchNewsArticles("All")
        if (articles.isNotEmpty()) {
            articles.map { "${it.title} (${it.source})" }
        } else {
            DEFAULT_NEWS
        }
    }

    suspend fun fetchNewsForQuery(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://news.google.com/rss/search?q=$encoded+when:2d&hl=en-US&gl=US&ceid=US:en"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val xml = response.body?.string() ?: return@withContext emptyList()
            parseRSSXml(xml, "All")
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchNewsArticles(category: String = "All"): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val queryParam = when (category) {
                "S&P 500 & Dow" -> "S%26P+500+Dow+Jones+Stock+Market+US"
                "Corporate & Q3" -> "US+Company+Quarterly+Earnings+Corporate+Stock+News"
                "FII / DII" -> "Wall+Street+Institutional+Investors+Flows+US"
                "Global Markets" -> "US+Federal+Reserve+Interest+Rates+Wall+Street"
                "IPO & Earnings" -> "US+Tech+IPOs+Securities+Exchange+Commission+SEC"
                else -> "NYSE+SP500+Dow+Jones+Wall+Street+Stock+Market"
            }

            val url = "https://news.google.com/rss/search?q=$queryParam+when:2d&hl=en-US&gl=US&ceid=US:en"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val xml = response.body?.string() ?: return@withContext SAMPLE_ARTICLES

            val articles = parseRSSXml(xml, category)
            if (articles.isNotEmpty()) articles else SAMPLE_ARTICLES
        } catch (e: Exception) {
            SAMPLE_ARTICLES
        }
    }

    private fun fetchRealImageUrlFromPage(url: String): String? {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val html = response.body?.string() ?: return null

            // Find meta tag containing property="og:image" or name="twitter:image" or name="thumbnail"
            val ogImageRegex = """<meta\s+[^>]*property=["']og:image["']\s+[^>]*content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
            val ogImageMatch = ogImageRegex.find(html)
            if (ogImageMatch != null) {
                val candidate = ogImageMatch.groupValues[1].replace("&amp;", "&")
                if (candidate.startsWith("http") && !isGoogleNewsOrDefaultLogo(candidate)) return candidate
            }

            val ogImageRegexAlt = """<meta\s+[^>]*content=["']([^"']+)["']\s+[^>]*property=["']og:image["']""".toRegex(RegexOption.IGNORE_CASE)
            val ogImageMatchAlt = ogImageRegexAlt.find(html)
            if (ogImageMatchAlt != null) {
                val candidate = ogImageMatchAlt.groupValues[1].replace("&amp;", "&")
                if (candidate.startsWith("http") && !isGoogleNewsOrDefaultLogo(candidate)) return candidate
            }

            val twitterImageRegex = """<meta\s+[^>]*name=["']twitter:image["']\s+[^>]*content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
            val twitterImageMatch = twitterImageRegex.find(html)
            if (twitterImageMatch != null) {
                val candidate = twitterImageMatch.groupValues[1].replace("&amp;", "&")
                if (candidate.startsWith("http") && !isGoogleNewsOrDefaultLogo(candidate)) return candidate
            }

            val thumbRegex = """<meta\s+[^>]*name=["']thumbnail["']\s+[^>]*content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
            val thumbMatch = thumbRegex.find(html)
            if (thumbMatch != null) {
                val candidate = thumbMatch.groupValues[1].replace("&amp;", "&")
                if (candidate.startsWith("http") && !isGoogleNewsOrDefaultLogo(candidate)) return candidate
            }
        } catch (e: Exception) {
            // Silently fall back
        }
        return null
    }

    private suspend fun parseRSSXml(xml: String, fallbackCategory: String): List<NewsArticle> = coroutineScope {
        val itemRegex = "<item>(.*?)</item>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val matches = itemRegex.findAll(xml).take(15).toList()

        val deferredArticles = matches.mapIndexed { index, match ->
            async(Dispatchers.IO) {
                try {
                    val itemXml = match.groupValues[1]

                    val rawTitle = extractTagContent(itemXml, "title") ?: return@async null
                    val link = extractTagContent(itemXml, "link") ?: ""
                    val pubDate = extractTagContent(itemXml, "pubDate") ?: ""
                    val sourceFromTag = extractTagContent(itemXml, "source")

                    if (rawTitle.isBlank() || rawTitle.equals("Google News", ignoreCase = true)) return@async null

                    // Unescape HTML entities & clean CDATA
                    val cleanTitle = cleanHtmlEntities(rawTitle)

                    // Split title and source correctly (source is after the LAST ' - ')
                    val lastDashIndex = cleanTitle.lastIndexOf(" - ")
                    val headline: String
                    val source: String

                    if (lastDashIndex != -1) {
                        headline = cleanTitle.substring(0, lastDashIndex).trim()
                        val parsedSource = cleanTitle.substring(lastDashIndex + 3).trim()
                        source = if (parsedSource.isNotBlank()) parsedSource else (sourceFromTag ?: "Market News")
                    } else {
                        headline = cleanTitle
                        source = sourceFromTag ?: "Economic Times"
                    }

                    if (headline.isBlank() || headline.equals("Google News", ignoreCase = true)) return@async null

                    val (timeAgo, isTooOld) = calculateTimeAgoWithAgeCheck(pubDate, index)
                    // Strictly exclude any news older than 2 days (48 hours)
                    if (isTooOld) return@async null

                    val sentiment = determineSentiment(headline)
                    val articleCat = determineCategory(headline, fallbackCategory)

                    // Extract image URL from item XML if present
                    var extractedImg = ""
                    val unescapedXml = cleanHtmlEntities(itemXml)

                    val mediaRegex = "(?:media:content|media:thumbnail|enclosure)[^>]+url=[\"'](https?://[^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
                    val mediaMatch = mediaRegex.find(itemXml) ?: mediaRegex.find(unescapedXml)
                    if (mediaMatch != null) {
                        val candidate = mediaMatch.groupValues[1].replace("&amp;", "&")
                        if (candidate.startsWith("http") && !isGoogleNewsOrDefaultLogo(candidate)) {
                            extractedImg = candidate
                        }
                    }

                    if (extractedImg.isBlank()) {
                        val imgRegex = "<img[^>]+src=[\"'](https?://[^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
                        val imgMatch = imgRegex.find(itemXml) ?: imgRegex.find(unescapedXml)
                        if (imgMatch != null) {
                            val candidate = imgMatch.groupValues[1].replace("&amp;", "&")
                            if (candidate.startsWith("http") && !isGoogleNewsOrDefaultLogo(candidate)) {
                                extractedImg = candidate
                            }
                        }
                    }

                    // Try fetching from the actual page if we still don't have an image
                    if (extractedImg.isBlank() && link.isNotBlank()) {
                        val fetchedUrl = fetchRealImageUrlFromPage(link.trim())
                        if (!fetchedUrl.isNullOrBlank() && !isGoogleNewsOrDefaultLogo(fetchedUrl)) {
                            extractedImg = fetchedUrl
                        }
                    }

                    if (extractedImg.isNotBlank() && isGoogleNewsOrDefaultLogo(extractedImg)) {
                        extractedImg = ""
                    }

                    NewsArticle(
                        id = "rss_$index",
                        title = headline,
                        source = source,
                        timeAgo = timeAgo,
                        category = articleCat,
                        sentiment = sentiment,
                        url = link.trim(),
                        imageUrl = extractedImg
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }

        deferredArticles.awaitAll().filterNotNull()
    }

    private fun extractTagContent(xml: String, tagName: String): String? {
        val regex = "<$tagName.*?>(.*?)</$tagName>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val match = regex.find(xml) ?: return null
        return match.groupValues[1]
            .replace("<![CDATA[", "")
            .replace("]]>", "")
            .trim()
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8211;", "-")
            .replace("&#8212;", "—")
            .trim()
    }

    private fun calculateTimeAgoWithAgeCheck(pubDateStr: String, fallbackIndex: Int): Pair<String, Boolean> {
        val todayFormatted = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
        if (pubDateStr.isBlank()) return Pair("${(fallbackIndex + 1) * 15} mins ago · $todayFormatted", false)
        return try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            val date = sdf.parse(pubDateStr)
            if (date != null) {
                val diffMs = System.currentTimeMillis() - date.time
                val diffMins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
                val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)

                // Enforce maximum 2 days (48 hours) threshold
                val isTooOld = diffHours > 48

                val timeStr = when {
                    diffMins <= 0 -> "Just now"
                    diffMins < 60 -> "₹diffMins mins ago"
                    diffHours < 24 -> "₹diffHours hours ago"
                    diffHours < 48 -> "1 day ago"
                    else -> "${diffHours / 24} days ago"
                }
                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
                Pair("₹timeStr · $dateStr", isTooOld)
            } else {
                Pair("${(fallbackIndex + 1) * 15} mins ago · $todayFormatted", false)
            }
        } catch (e: Exception) {
            Pair("${(fallbackIndex + 1) * 15} mins ago · $todayFormatted", false)
        }
    }

    private fun determineSentiment(headline: String): String {
        val text = headline.lowercase(Locale.ROOT)
        return when {
            text.contains("surge") || text.contains("gain") || text.contains("rally") ||
            text.contains("rise") || text.contains("high") || text.contains("jump") ||
            text.contains("bull") || text.contains("record") || text.contains("profit") ||
            text.contains("boost") || text.contains("soar") || text.contains("up ") -> "BULLISH"

            text.contains("fall") || text.contains("drop") || text.contains("plunge") ||
            text.contains("slide") || text.contains("down") || text.contains("cut") ||
            text.contains("bear") || text.contains("loss") || text.contains("slump") ||
            text.contains("sink") || text.contains("crash") -> "BEARISH"

            else -> "NEUTRAL"
        }
    }

    private fun determineCategory(headline: String, fallback: String): String {
        if (fallback != "All") return fallback

        val text = headline.lowercase(Locale.ROOT)
        return when {
            text.contains("sp500") || text.contains("dow") || text.contains("market live") || text.contains("share market") -> "S&P 500 & Dow"
            text.contains("fii") || text.contains("dii") || text.contains("inflow") || text.contains("outflow") || text.contains("fund") -> "FII / DII"
            text.contains("ipo") || text.contains("earnings") || text.contains("q1") || text.contains("q2") || text.contains("q3") || text.contains("q4") || text.contains("allotment") -> "IPO & Earnings"
            text.contains("fed") || text.contains("global") || text.contains("us ") || text.contains("wall street") || text.contains("crude") || text.contains("oil") -> "Global Markets"
            else -> "Corporate & Q3"
        }
    }
}



package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest


fun cleanStockSymbol(input: String): String {
    val upper = input.trim().uppercase()
    val noSuffix = upper
    var cleaned = noSuffix
    for (word in listOf(" LIMITED", " LTD", " IND", " INDUSTRIES", " ENTERPRISES", " PHARMA", " SERVICES", " CORP", " CORPORATION", " BANK")) {
        cleaned = cleaned.replace(word, "")
    }
    val alphaNumAndAnd = cleaned.replace(Regex("[^A-Z0-9&]"), "")
    if (alphaNumAndAnd.isNotEmpty()) return alphaNumAndAnd
    return noSuffix.replace(Regex("[^A-Z0-9]"), "")
}

data class UpcomingDividend(
    val symbol: String,
    val companyName: String,
    val amountPerShare: Double,
    val dividendType: String, // Interim, Final, Special
    val exDate: String, // "YYYY-MM-DD"
    val recordDate: String, // "YYYY-MM-DD"
    val cmp: Double,
    val yieldPercent: Double
)

val STOCK_DOMAIN_MAP = mapOf(
    "360ONE" to "360.one",
    "ABB" to "abb.com",
    "ABCAPITAL" to "adityabirlacapital.com",
    "ACC" to "acclimited.com",
    "ADANI" to "adani.com",
    "ADANIENSOL" to "adanienergysolutions.com",
    "ADANIENT" to "adani.com",
    "ADANIGREEN" to "adanigreenenergy.com",
    "ADANIPORTS" to "adaniports.com",
    "ADANIPOWER" to "adanipower.com",
    "AIRTEL" to "airtel.in",
    "ALKEM" to "alkemlabs.com",
    "AMBUJACEM" to "ambujacement.com",
    "APLAPOLLO" to "aplapollo.com",
    "APOLLOHOSP" to "apollohospitals.com",
    "ASHOKLEY" to "ashokleyland.com",
    "ASIANPAINT" to "asianpaints.com",
    "ASTRAL" to "astralpipes.com",
    "ATGL" to "adanigas.com",
    "AUBANK" to "aubank.in",
    "AUROPHARMA" to "aurobindo.com",
    "AXISBANK" to "axisbank.com",
    "BAJAJAUTO" to "bajajauto.com",
    "BAJAJFINSV" to "bajajfinserv.in",
    "BAJAJHFL" to "bajajhousingfinance.in",
    "BAJAJHLDNG" to "bajajauto.com",
    "BANKBARODA" to "bankofbaroda.in",
    "BANKINDIA" to "bankofindia.co.in",
    "BDL" to "bdl-india.in",
    "BEL" to "bel-india.in",
    "BHARATFORG" to "bharatforge.com",
    "BHARTIHEXA" to "airtel.in",
    "BHEL" to "bhel.in",
    "BIOCON" to "biocon.com",
    "BLUESTARCO" to "bluestarindia.com",
    "BOSCHLTD" to "bosch.in",
    "BPCL" to "bharatpetroleum.in",
    "BRITANNIA" to "britannia.co.in",
    "BSE" to "bseindia.com",
    "CANBK" to "canarabank.com",
    "CGPOWER" to "cgpower.com",
    "CHOLAFIN" to "cholamandalam.com",
    "CIPLA" to "cipla.com",
    "COALINDIA" to "coalindia.in",
    "COCHINSHIP" to "cochinshipyard.in",
    "COFORGE" to "coforge.com",
    "COLPAL" to "colgatepalmolive.co.in",
    "CONCOR" to "concorindia.com",
    "COROMANDEL" to "coromandel.biz",
    "CUMMINSIND" to "cummins.com",
    "DABUR" to "dabur.com",
    "DIVISLAB" to "divislabs.com",
    "DIXON" to "dixoninfo.com",
    "DLF" to "dlf.in",
    "DMART" to "dmart.in",
    "DRREDDY" to "drreddys.com",
    "EICHERMOT" to "eichermotors.com",
    "EXIDEIND" to "exideindustries.com",
    "FEDERALBNK" to "federalbank.co.in",
    "FORTIS" to "fortishealthcare.com",
    "GAIL" to "gailonline.com",
    "GLENMARK" to "glenmarkpharma.com",
    "GODFRYPHLP" to "godfreyphillips.com",
    "GODREJCP" to "godrejcp.com",
    "GODREJPROP" to "godrejproperties.com",
    "GRASIM" to "grasim.com",
    "HAL" to "hal-india.co.in",
    "HAVELLS" to "havells.com",
    "HCLTECH" to "hcltech.com",
    "HEROMOTOCO" to "heromotocorp.com",
    "HINDALCO" to "hindalco.com",
    "HINDPETRO" to "hindustanpetroleum.com",
    "HINDUNILVR" to "hul.co.in",
    "HINDZINC" to "hzlindia.com",
    "HUDCO" to "hudco.org",
    "HUL" to "hul.co.in",
    "HYUNDAI" to "hyundai.com",
    "ICICIBANK" to "icicibank.com",
    "IDEA" to "myvi.in",
    "IDFCFIRSTB" to "idfcfirstbank.com",
    "IGL" to "iglonline.net",
    "INDHOTEL" to "ihcltata.com",
    "INDIGO" to "goindigo.in",
    "INDUSTOWER" to "industowers.com",
    "INDUSINDBK" to "indusind.com",
    "INFOSYS" to "infosys.com",
    "INFY" to "infosys.com",
    "IOC" to "iocl.com",
    "IRB" to "irb.co.in",
    "IRCTC" to "irctc.co.in",
    "IREDA" to "ireda.in",
    "IRFC" to "irfc.co.in",
    "ITC" to "itcportal.com",
    "ITCHOTELS" to "itchotels.com",
    "JINDALSTEL" to "jindalsteelpower.com",
    "JIOFIN" to "jiofinancial.com",
    "JSWENERGY" to "jsw.in",
    "JSWSTEEL" to "jsw.in",
    "JUBLFOOD" to "jubilantfoodworks.com",
    "KALYANKJIL" to "kalyanjewellers.net",
    "KEI" to "kei-ind.com",
    "KOTAK" to "kotak.com",
    "KOTAKBANK" to "kotak.com",
    "KPITTECH" to "kpit.com",
    "LIC" to "licindia.in",
    "LICI" to "licindia.in",
    "LICHSGFIN" to "lichousing.com",
    "LODHA" to "macrotechdevelopers.com",
    "LT" to "larsentoubro.com",
    "LTF" to "ltfs.com",
    "LTIM" to "ltimindtree.com",
    "LUPIN" to "lupin.com",
    "M&M" to "mahindra.com",
    "MAHINDRA" to "mahindra.com",
    "MANKIND" to "mankindpharma.com",
    "MARICO" to "marico.com",
    "MARUTI" to "marutisuzuki.com",
    "MAXHEALTH" to "maxhealthcare.in",
    "MAZDOCK" to "mazagondock.in",
    "MFSL" to "maxfinancialservices.com",
    "MOTHERSON" to "motherson.com",
    "MOTILALOFS" to "motilaloswalgroup.com",
    "MPHASIS" to "mphasis.com",
    "MRF" to "mrftyres.com",
    "MUTHOOTFIN" to "muthootfinance.com",
    "NATIONALUM" to "nalcoindia.com",
    "NAUKRI" to "infoedge.in",
    "NESTLEIND" to "nestle.in",
    "NHPC" to "nhpcindia.com",
    "NMDC" to "nmdc.co.in",
    "NTPC" to "ntpc.co.in",
    "NYKAA" to "nykaa.com",
    "OBEROIRLTY" to "oberoirealty.com",
    "OFSS" to "oracle.com",
    "OIL" to "oil-india.com",
    "ONGC" to "ongcindia.com",
    "PAGEIND" to "jockey.in",
    "PATANJALI" to "patanjaliayurved.org",
    "PAYTM" to "paytm.com",
    "PFC" to "pfcindia.in",
    "PIDILITIND" to "pidilite.com",
    "PNB" to "pnbindia.in",
    "POLICYBZR" to "policybazaar.com",
    "POLYCAB" to "polycab.com",
    "POWERGRID" to "powergrid.in",
    "PRESTIGE" to "prestigeconstructions.com",
    "REC" to "recindia.nic.in",
    "RECLTD" to "recindia.nic.in",
    "RVNL" to "rvnl.org",
    "SAIL" to "sail.co.in",
    "SBIN" to "sbi.co.in",
    "SHREECEM" to "shreecement.com",
    "SHRIRAMFIN" to "shriramfinance.in",
    "SIEMENS" to "siemens.com",
    "SRF" to "srf.com",
    "SUNPHARMA" to "sunpharma.com",
    "SUZLON" to "suzlon.com",
    "SWIGGY" to "swiggy.com",
    "TATAMOTORS" to "tatamotors.com",
    "TCS" to "tcs.com",
    "TECHM" to "techmahindra.com",
    "TITAN" to "titancompany.in",
    "TORNTPHARM" to "torrentpharma.com",
    "TRENT" to "trentlimited.com",
    "TVSMOTOR" to "tvsmotor.com",
    "ULTRACEMCO" to "ultratechcement.com",
    "UNIONBANK" to "unionbankofindia.co.in",
    "VEDL" to "vedantalimited.com",
    "VOLTAS" to "voltas.com",
    "YESBANK" to "yesbank.in",
    "ZOMATO" to "zomato.com",
    "ZYDUSLIFE" to "zyduslife.com"
)

data class StockBrandInfo(
    val shortName: String,
    val bgColor: Color,
    val textColor: Color = Color.White
)

val STOCK_BRAND_MAP = mapOf(
    "TCS" to StockBrandInfo("TCS", Color(0xFF003366)),
    "INFY" to StockBrandInfo("INFY", Color(0xFF007CC3)),
    "ITC" to StockBrandInfo("ITC", Color(0xFF0056B3)),
    "COALINDIA" to StockBrandInfo("CIL", Color(0xFF2E7D32)),
    "VEDL" to StockBrandInfo("VEDL", Color(0xFFD84315)),
    "HINDUNILVR" to StockBrandInfo("HUL", Color(0xFF002244)),
    "BPCL" to StockBrandInfo("BPCL", Color(0xFF006699)),
    "IOC" to StockBrandInfo("IOC", Color(0xFFE65100)),
    "ONGC" to StockBrandInfo("ONGC", Color(0xFF8B0000)),
    "NTPC" to StockBrandInfo("NTPC", Color(0xFF0288D1)),
    "POWERGRID" to StockBrandInfo("PGCIL", Color(0xFF00796B)),
    "PFC" to StockBrandInfo("PFC", Color(0xFF004080)),
    "RECLTD" to StockBrandInfo("REC", Color(0xFF1B5E20)),
    "HCLTECH" to StockBrandInfo("HCL", Color(0xFF0056B3)),
    "LICI" to StockBrandInfo("LIC", Color(0xFF1E293B)),
    "TATAMOTORS" to StockBrandInfo("TATA", Color(0xFF0A2540)),
    "SBIN" to StockBrandInfo("SBI", Color(0xFF0083CA)),
    "GAIL" to StockBrandInfo("GAIL", Color(0xFFE53935)),
    "NMDC" to StockBrandInfo("NMDC", Color(0xFF1B5E20)),
    "AIRTEL" to StockBrandInfo("ARTL", Color(0xFFE40000)),
    "KOTAKBANK" to StockBrandInfo("KOTAK", Color(0xFFD32F2F)),
    "LT" to StockBrandInfo("L&T", Color(0xFF005A9C)),
    "AXISBANK" to StockBrandInfo("AXIS", Color(0xFF800020)),
    "ASIANPAINT" to StockBrandInfo("AP", Color(0xFFE53935)),
    "MARUTI" to StockBrandInfo("MSIL", Color(0xFF002F6C)),
    "SUNPHARMA" to StockBrandInfo("SUN", Color(0xFFF57C00)),
    "TITAN" to StockBrandInfo("TITAN", Color(0xFF1E293B)),
    "ULTRACEMCO" to StockBrandInfo("ULTRA", Color(0xFFE65100)),
    "BAJAJFINSV" to StockBrandInfo("BAJAJ", Color(0xFF004080)),
    "NESTLEIND" to StockBrandInfo("NESTLE", Color(0xFF7B1FA2)),
    "ADANIENT" to StockBrandInfo("ADANI", Color(0xFF1A237E)),
    "JSWSTEEL" to StockBrandInfo("JSW", Color(0xFF1565C0)),
    "GRASIM" to StockBrandInfo("GRASIM", Color(0xFFC62828)),
    "HEROMOTOCO" to StockBrandInfo("HERO", Color(0xFFD32F2F)),
    "CIPLA" to StockBrandInfo("CIPLA", Color(0xFF0288D1)),
    "DRREDDY" to StockBrandInfo("REDDY", Color(0xFFC62828)),
    "BRITANNIA" to StockBrandInfo("BRIT", Color(0xFFD32F2F))
)

val AVATAR_PALETTE = listOf(
    Color(0xFF003366),
    Color(0xFF004C8F),
    Color(0xFF1B365D),
    Color(0xFF007CC3),
    Color(0xFFF37021),
    Color(0xFF0083CA),
    Color(0xFF003D7A),
    Color(0xFFE40000),
    Color(0xFF34113B),
    Color(0xFFC8102E),
    Color(0xFF1E293B),
    Color(0xFF0F766E),
    Color(0xFF4338CA),
    Color(0xFF6D28D9)
)

private val LOGO_URL_CACHE = mutableMapOf<String, Int>()

fun getInitialsBgColor(symbol: String): Color {
    val brandInfo = STOCK_BRAND_MAP[symbol]
    if (brandInfo != null) return brandInfo.bgColor
    val hash = kotlin.math.abs(symbol.hashCode())
    return AVATAR_PALETTE[hash % AVATAR_PALETTE.size]
}

fun getLogoCandidateUrls(cleanSymbol: String): List<String> {
    val urls = mutableListOf<String>()
    
    // 1. Direct SVG logo from Indian Listed Company Logos dataset (dharunashokkumar/us-listed-company-logos)
    urls.add("https://dharunashokkumar.github.io/us-listed-company-logos/nse/NSE_${cleanSymbol}.svg")
    urls.add("https://dharunashokkumar.github.io/us-listed-company-logos/bse/BSE_${cleanSymbol}.svg")
    
    val domain = STOCK_DOMAIN_MAP[cleanSymbol]
    if (domain != null) {
        // 2. Google Favicon Service - extremely reliable sz=128
        urls.add("https://www.google.com/s2/favicons?domain=$domain&sz=128")
        // 3. Clearbit logo API
        urls.add("https://logo.clearbit.com/$domain")
        // 4. Unavatar API
        urls.add("https://unavatar.io/$domain?fallback=false")
        // 5. IconHorse API
        urls.add("https://icon.horse/icon/$domain")
    } else {
        val lower = cleanSymbol.lowercase().replace("&", "")
        urls.add("https://www.google.com/s2/favicons?domain=${lower}.com&sz=128")
        urls.add("https://www.google.com/s2/favicons?domain=${lower}.in&sz=128")
        urls.add("https://www.google.com/s2/favicons?domain=${lower}.co.in&sz=128")
    }
    
    return urls
}

@Composable
fun CompanyLogoView(
    symbol: String,
    modifier: Modifier = Modifier.size(38.dp)
) {
    val cleanSymbol = remember(symbol) { cleanStockSymbol(symbol) }
    val candidates = remember(cleanSymbol) { getLogoCandidateUrls(cleanSymbol) }
    var currentUrlIndex by remember(cleanSymbol) { mutableIntStateOf(LOGO_URL_CACHE[cleanSymbol] ?: 0) }

    val brandInfo = STOCK_BRAND_MAP[cleanSymbol]
    val shortName = brandInfo?.shortName ?: if (cleanSymbol.length > 4) cleanSymbol.take(3) else cleanSymbol
    val fallbackBgColor = brandInfo?.bgColor ?: getInitialsBgColor(cleanSymbol)

    val context = LocalContext.current

    if (currentUrlIndex < candidates.size) {
        val currentUrl = candidates[currentUrlIndex]
        val imageRequest = remember(currentUrl) {
            ImageRequest.Builder(context)
                .data(currentUrl)
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build()
        }
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = " logo",
            contentScale = ContentScale.Fit,
            modifier = modifier
                .clip(CircleShape)
                .background(Color.White),
            onError = {
                val nextIndex = currentUrlIndex + 1
                LOGO_URL_CACHE[cleanSymbol] = nextIndex
                currentUrlIndex = nextIndex
            },
            onSuccess = {
                LOGO_URL_CACHE[cleanSymbol] = currentUrlIndex
            },
            loading = {
                Box(
                    modifier = modifier
                        .clip(CircleShape)
                        .background(fallbackBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            error = {
                Box(
                    modifier = modifier
                        .clip(CircleShape)
                        .background(fallbackBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    } else {
        // Brand avatar with authentic brand color & abbreviation
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(fallbackBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = shortName,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun generateUpcomingDividends(): List<UpcomingDividend> {
    val cal = Calendar.getInstance()
    fun getFutureDate(daysAhead: Int): String {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, daysAhead)
        val year = c.get(Calendar.YEAR)
        val month = String.format(Locale.US, "%02d", c.get(Calendar.MONTH) + 1)
        val day = String.format(Locale.US, "%02d", c.get(Calendar.DAY_OF_MONTH))
        return "$year-$month-$day"
    }

    return listOf(
        UpcomingDividend("TCS", "Tata Consultancy Services Ltd.", 10.00, "Interim Dividend", getFutureDate(3), getFutureDate(5), 3850.00, 0.26),
        UpcomingDividend("INFY", "Infosys Limited", 20.00, "Final Dividend", getFutureDate(5), getFutureDate(7), 1820.00, 1.10),
        UpcomingDividend("ITC", "ITC Limited", 7.50, "Final Dividend", getFutureDate(8), getFutureDate(10), 485.00, 1.55),
        UpcomingDividend("COALINDIA", "Coal India Limited", 15.25, "Interim Dividend", getFutureDate(10), getFutureDate(12), 512.00, 2.98),
        UpcomingDividend("VEDL", "Vedanta Limited", 20.50, "Special Dividend", getFutureDate(12), getFutureDate(14), 435.60, 4.71),
        UpcomingDividend("HINDUNILVR", "Hindustan Unilever Ltd.", 24.00, "Interim Dividend", getFutureDate(15), getFutureDate(17), 2720.00, 0.88),
        UpcomingDividend("BPCL", "Bharat Petroleum Corp. Ltd.", 10.50, "Final Dividend", getFutureDate(18), getFutureDate(20), 345.80, 3.04),
        UpcomingDividend("IOC", "Indian Oil Corporation Ltd.", 7.00, "Final Dividend", getFutureDate(21), getFutureDate(23), 175.20, 3.99),
        UpcomingDividend("ONGC", "Oil & Natural Gas Corp. Ltd.", 6.00, "Interim Dividend", getFutureDate(24), getFutureDate(26), 320.40, 1.87),
        UpcomingDividend("NTPC", "NTPC Limited", 3.25, "Interim Dividend", getFutureDate(27), getFutureDate(29), 410.90, 0.79),
        UpcomingDividend("POWERGRID", "Power Grid Corp. of India", 4.50, "Interim Dividend", getFutureDate(30), getFutureDate(32), 340.10, 1.32),
        UpcomingDividend("PFC", "Power Finance Corporation", 3.50, "Interim Dividend", getFutureDate(33), getFutureDate(35), 520.10, 0.67),
        UpcomingDividend("RECLTD", "REC Limited", 4.50, "Interim Dividend", getFutureDate(36), getFutureDate(38), 585.30, 0.77),
        UpcomingDividend("HCLTECH", "HCL Technologies Ltd.", 12.00, "Interim Dividend", getFutureDate(40), getFutureDate(42), 1580.00, 0.76),
        UpcomingDividend("LICI", "Life Insurance Corp. of India", 6.00, "Final Dividend", getFutureDate(44), getFutureDate(46), 1020.00, 0.59),
        UpcomingDividend("TATAMOTORS", "Tata Motors Limited", 6.00, "Final Dividend", getFutureDate(48), getFutureDate(50), 1080.00, 0.56),
        UpcomingDividend("SBIN", "State Bank of India", 13.70, "Final Dividend", getFutureDate(52), getFutureDate(54), 840.00, 1.63),
        UpcomingDividend("GAIL", "GAIL (India) Limited", 5.50, "Interim Dividend", getFutureDate(56), getFutureDate(58), 230.00, 2.39),
        UpcomingDividend("NMDC", "NMDC Limited", 5.75, "Interim Dividend", getFutureDate(60), getFutureDate(62), 260.00, 2.21)
    )
}

val MASTER_DIVIDEND_LIST = generateUpcomingDividends()
val MASTER_DIVIDEND_LEST = MASTER_DIVIDEND_LIST

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividendsScreen(
    modifier: Modifier = Modifier,
    onSymbolSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, HIGH_YIELD, PSU, IT

    val liveDividends by LiveDividendManager.liveDividends.collectAsState()
    val isLoading by LiveDividendManager.isLoading.collectAsState()
    val lastSync by LiveDividendManager.lastSyncTimestamp.collectAsState()
    val statusMsg by LiveDividendManager.syncStatusMessage.collectAsState()

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

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        LiveDividendManager.initialize(context)
        if (liveDividends.isEmpty()) {
            scope.launch {
                LiveDividendManager.fetchLiveDividendsFromInternet(context)
            }
        }
    }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val validUpcomingDividends = remember(todayDateStr, searchQuery, selectedFilter, liveDividends) {
        liveDividends.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.symbol.contains(searchQuery, ignoreCase = true) ||
                    item.companyName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "HIGH_YIELD" -> item.yieldPercent >= 2.0
                "PSU" -> listOf("COALINDIA", "VEDL", "BPCL", "IOC", "ONGC", "NTPC", "POWERGRID", "PFC", "RECLTD", "LICI", "GAIL", "NMDC").contains(item.symbol)
                "IT" -> listOf("TCS", "INFY", "HCLTECH", "WIPRO", "TECHM", "LTIM").contains(item.symbol)
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedBy { it.exDate }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        // Header & Live Sync Controls
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isLoading) Color(0xFFEAB308) else Color(0xFF16A34A))
                        )
                        Text(
                            text = "Indian Corporate Dividends (NSE / BSE)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (lastSync > 0) "$statusMsg • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSync))}" else statusMsg,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            LiveDividendManager.fetchLiveDividendsFromInternet(context)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF7C3AED),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Sync Internet Dividends",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Indian stock or ticker...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                "ALL" to "All Announced (${liveDividends.size})",
                "HIGH_YIELD" to "High Yield (>2%)",
                "PSU" to "PSUs & Energy",
                "IT" to "IT Majors"
            )

            filters.forEach { (key, label) ->
                val isSelected = selectedFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEDE9FE),
                        selectedLabelColor = Color(0xFF6D28D9),
                        containerColor = Color.White,
                        labelColor = Color(0xFF475569)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFFE2E8F0),
                        selectedBorderColor = Color(0xFFC4B5FD)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (validUpcomingDividends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No Upcoming Dividends Found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Try adjusting your search query.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(validUpcomingDividends, key = { it.symbol }) { item ->
                    DividendCard(item = item, onSymbolClick = { onSymbolSelected(item.symbol) })
                }
            }
        }
    }
}

fun formatDividendDate(dateStr: String): String {
    try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdfInput.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance()
        cal.time = date
        
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        
        val monthSdf = SimpleDateFormat("MMMM", Locale.US)
        val monthName = monthSdf.format(date) // e.g. "July"
        val year = cal.get(Calendar.YEAR)
        
        val dayOfWeekSdf = SimpleDateFormat("EEEE", Locale.US)
        val dayOfWeek = dayOfWeekSdf.format(date) // e.g. "Friday"
        
        return "$dayOfWeek ${day}$suffix $monthName $year"
    } catch (e: Exception) {
        return dateStr
    }
}

fun getPayoutDate(exDateStr: String): String {
    try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdfInput.parse(exDateStr) ?: return exDateStr
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, 15) // Payout is typically ~15 days after ex-date
        val year = cal.get(Calendar.YEAR)
        val month = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
        val day = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        return "$year-$month-$day"
    } catch (e: Exception) {
        return exDateStr
    }
}

@Composable
fun DividendCard(
    item: UpcomingDividend,
    onSymbolClick: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }

    val displaySymbol = item.symbol
    val formattedPayout = String.format(Locale.US, "%.2f", item.amountPerShare)
    val formattedPrice = "₹" + String.format(Locale.US, "%.2f", item.cmp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSymbolClick() },
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
            // Row 1: Dividend Type Badge Tag + Heart Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.dividendType.ifBlank { "DIVIDEND" }.uppercase(),
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )

                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isFavorite) Color(0xFFDC2626) else Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2: Company Icon + Company Name & Ticker Symbol (Left), and Yield (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    CompanyLogoView(symbol = item.symbol, modifier = Modifier.size(24.dp))

                    Column {
                        Text(
                            text = item.companyName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = displaySymbol,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = "Yield: ${String.format(Locale.US, "%.2f", item.yieldPercent)}%",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            // Row 3: Payout Box (left) + Current Price (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF8F9FA))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Payout: ",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "₹$formattedPayout",
                            color = Color(0xFF1F2937),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = formattedPrice,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            
            // Ex-date Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFF0F1))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EX-DATE",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626),
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = formatDividendDate(item.exDate),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.End
                    )
                }
            }

            // Payout Date Banner
            val payoutDateStr = getPayoutDate(item.exDate)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFECFDF5))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAYOUT DATE",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669),
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = formatDividendDate(payoutDateStr),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

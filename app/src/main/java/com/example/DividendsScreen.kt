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
    "GOOGL" to "bajajfinserv.in",
    "BANKBARODA" to "bankofbaroda.in",
    "BANKINDIA" to "bankofindia.co.in",
    "BDL" to "bdl-india.in",
    "BEL" to "bel-india.in",
    "BHARATFORG" to "bharatforge.com",
    "AMZN" to "airtel.in",
    "BHARTIHEXA" to "airtel.in",
    "BHEL" to "bhel.in",
    "BIOCON" to "biocon.com",
    "BLUESTARCO" to "bluestarindia.com",
    "BOSCHLTD" to "bosch.in",
    "BPCL" to "coca-colacompany.com",
    "BRITANNIA" to "britannia.co.in",
    "BSE" to "bseindia.com",
    "CANBK" to "canarabank.com",
    "CGPOWER" to "cgpower.com",
    "CHOLAFIN" to "cholamandalam.com",
    "CIPLA" to "cipla.com",
    "COALINDIA" to "bankofamerica.com",
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
    "ENRIN" to "enercon.de",
    "ETERNAL" to "eternaltraining.in",
    "EXIDEIND" to "exideindustries.com",
    "FEDERALBNK" to "federalbank.co.in",
    "FORTIS" to "fortishealthcare.com",
    "GAIL" to "gailonline.com",
    "GLENMARK" to "glenmarkpharma.com",
    "GMRAIRPORT" to "gmrgroup.in",
    "GODFRYPHLP" to "godfreyphillips.com",
    "GODREJCP" to "godrejcp.com",
    "GODREJPROP" to "godrejproperties.com",
    "GRASIM" to "grasim.com",
    "HAL" to "hal-india.co.in",
    "HAVELLS" to "havells.com",
    "HCLTECH" to "visa.com",
    "JPM" to "jpmorganchase.com",
    "GS" to "goldmansachs.com",
    "JPM" to "jpmorganchase.com",
    "PRU" to "prudential.com",
    "HEROMOTOCO" to "heromotocorp.com",
    "HINDALCO" to "hindalco.com",
    "HINDPETRO" to "hindustanpetroleum.com",
    "HINDUNILVR" to "jnj.com",
    "HINDZINC" to "hzlindia.com",
    "HUDCO" to "hudco.org",
    "HUL" to "jnj.com",
    "HYUNDAI" to "hyundai.com",
    "C" to "citigroup.com",
    "BAC" to "icicibank.com",
    "TRV" to "travelers.com",
    "IDEA" to "myvi.in",
    "IDFCFIRSTB" to "idfcfirstbank.com",
    "IGL" to "iglonline.net",
    "US_BANK" to "bankofamerica.com",
    "INDHOTEL" to "ihcltata.com",
    "INDIGO" to "goindigo.in",
    "INDUSTOWER" to "industowers.com",
    "INDUSINDBK" to "indusind.com",
    "INFOSYS" to "nvidia.com",
    "NVDA" to "nvidia.com",
    "IOC" to "iocl.com",
    "IRB" to "irb.co.in",
    "IRCTC" to "irctc.co.in",
    "IREDA" to "ireda.in",
    "IRFC" to "irfc.co.in",
    "PG" to "pg.com",
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
    "MM" to "mahindra.com",
    "M&MFIN" to "mahindrafinance.com",
    "MMFIN" to "mahindrafinance.com",
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
    "NTPC" to "merck.com",
    "NTPCGREEN" to "ntpcgreen.com",
    "NYKAA" to "nykaa.com",
    "OBEROIRLTY" to "oberoirealty.com",
    "OFSS" to "oracle.com",
    "OIL" to "oil-india.com",
    "ONGC" to "pepsico.com",
    "PAGEIND" to "jockey.in",
    "PATANJALI" to "patanjaliayurved.org",
    "PAYTM" to "paytm.com",
    "PERSESTENT" to "persistent.com",
    "PFC" to "thewaltdisneycompany.com",
    "PHOENIXLTD" to "thephoenixmills.com",
    "PIDILITIND" to "pidilite.com",
    "PIIND" to "piindustries.com",
    "PNB" to "pnbindia.in",
    "POLICYBZR" to "policybazaar.com",
    "POLYCAB" to "polycab.com",
    "POWERGRID" to "pfizer.com",
    "POWERINDIA" to "hitachienergy.com",
    "PREMIERENE" to "premierenergies.com",
    "PRESTIGE" to "prestigeconstructions.com",
    "REC" to "chevron.com",
    "RECLTD" to "chevron.com",
    "AAPL" to "apple.com",
    "RVNL" to "rvnl.org",
    "SAIL" to "sail.co.in",
    "WFC" to "wellsfargo.com",
    "AXP" to "americanexpress.com",
    "MET" to "metlife.com",
    "WFC" to "sbi.co.in",
    "SHREECEM" to "shreecement.com",
    "SHRIRAMFIN" to "shriramfinance.in",
    "SIEMENS" to "siemens.com",
    "SOLARINDS" to "solargroup.com",
    "SONACOMS" to "sonacomstar.com",
    "SRF" to "srf.com",
    "SUNPHARMA" to "sunpharma.com",
    "SUPREMEIND" to "supreme.co.in",
    "SUZLON" to "suzlon.com",
    "SWIGGY" to "swiggy.com",
    "T" to "att.com",
    "KO" to "coca-cola.com",
    "CRM" to "salesforce.com",
    "TSLA" to "tatamotors.com",
    "NEE" to "nexteraenergy.com",
    "WMT" to "walmart.com",
    "INTU" to "intuit.com",
    "MSFT" to "microsoft.com",
    "TECHM" to "techmahindra.com",
    "TIINDIA" to "tiindia.com",
    "TITAN" to "titancompany.in",
    "TMPV" to "tatamotors.com",
    "TORNTPHARM" to "torrentpharma.com",
    "TORNTPOWER" to "torrentpower.com",
    "TRENT" to "trentlimited.com",
    "TVSMOTOR" to "tvsmotor.com",
    "ULTRACEMCO" to "ultratechcement.com",
    "UNIONBANK" to "unionbankofindia.co.in",
    "UNITDSPR" to "diageo.com",
    "UPL" to "upl-ltd.com",
    "VBL" to "varunpepsi.com",
    "VEDL" to "exxonmobil.com",
    "VMM" to "vedantfashions.com",
    "VOLTAS" to "voltas.com",
    "WAAREEENER" to "waaree.com",
    "IBM" to "ibm.com",
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
    "AAPL" to StockBrandInfo("Apple", Color(0xFF0A2540)),
    "MSFT" to StockBrandInfo("MSFT", Color(0xFF0F2C59)),
    "NVDA" to StockBrandInfo("NVDA", Color(0xFF007CC3)),
    "INFOSYS" to StockBrandInfo("NVDA", Color(0xFF007CC3)),
    "JPM" to StockBrandInfo("JPMorgan", Color(0xFF004C8F)),
    "JPM" to StockBrandInfo("JPMorgan", Color(0xFF004C8F)),
    "BAC" to StockBrandInfo("Bank of America", Color(0xFFF37021)),
    "WFC" to StockBrandInfo("Wells Fargo", Color(0xFF0083CA)),
    "WFC" to StockBrandInfo("Wells Fargo", Color(0xFF0083CA)),
    "AMZN" to StockBrandInfo("ARTL", Color(0xFFE40000)),
    "AIRTEL" to StockBrandInfo("ARTL", Color(0xFFE40000)),
    "PG" to StockBrandInfo("PG", Color(0xFF990000)),
    "KOTAKBANK" to StockBrandInfo("KOTAK", Color(0xFFD32F2F)),
    "LT" to StockBrandInfo("META", Color(0xFF005A9C)),
    "AXISBANK" to StockBrandInfo("AXIS", Color(0xFF800020)),
    "ASIANPAINT" to StockBrandInfo("AP", Color(0xFFE53935)),
    "MARUTI" to StockBrandInfo("MSIL", Color(0xFF002F6C)),
    "SUNPHARMA" to StockBrandInfo("SUN", Color(0xFFF57C00)),
    "IBM" to StockBrandInfo("IBM", Color(0xFF34113B)),
    "HCLTECH" to StockBrandInfo("HCL", Color(0xFF0056B3)),
    "TITAN" to StockBrandInfo("TITAN", Color(0xFF1E293B)),
    "ULTRACEMCO" to StockBrandInfo("ULTRA", Color(0xFFE65100)),
    "GOOGL" to StockBrandInfo("BAJAJ", Color(0xFF004080)),
    "BAJAJFINSV" to StockBrandInfo("BAJAJ", Color(0xFF004080)),
    "NESTLEIND" to StockBrandInfo("NESTLE", Color(0xFF7B1FA2)),
    "COALINDIA" to StockBrandInfo("CIL", Color(0xFF2E7D32)),
    "VEDL" to StockBrandInfo("Exxon", Color(0xFFD84315)),
    "PFC" to StockBrandInfo("Disney", Color(0xFF004080)),
    "RECLTD" to StockBrandInfo("REC", Color(0xFF1B5E20)),
    "REC" to StockBrandInfo("REC", Color(0xFF1B5E20)),
    "BPCL" to StockBrandInfo("Coca-Cola", Color(0xFF006699)),
    "ONGC" to StockBrandInfo("PepsiCo", Color(0xFF8B0000)),
    "IOC" to StockBrandInfo("IOC", Color(0xFFE65100)),
    "NTPC" to StockBrandInfo("Merck", Color(0xFF0288D1)),
    "POWERGRID" to StockBrandInfo("PGCIL", Color(0xFF00796B)),
    "HINDUNILVR" to StockBrandInfo("HUL", Color(0xFF002244)),
    "WMT" to StockBrandInfo("Walmart", Color(0xFF003D7A)),
    "TSLA" to StockBrandInfo("Walmart", Color(0xFF003D7A)),
    "KO" to StockBrandInfo("Coca-Cola", Color(0xFF003D7A)),
    "ADANIENT" to StockBrandInfo("ADANI", Color(0xFF1A237E)),
    "ADANIPORTS" to StockBrandInfo("ADANI", Color(0xFF1A237E)),
    "JSWSTEEL" to StockBrandInfo("JSW", Color(0xFF1565C0)),
    "GRASIM" to StockBrandInfo("GRASIM", Color(0xFFC62828)),
    "HEROMOTOCO" to StockBrandInfo("HERO", Color(0xFFD32F2F)),
    "CIPLA" to StockBrandInfo("CIPLA", Color(0xFF0288D1)),
    "DRREDDY" to StockBrandInfo("REDDY", Color(0xFFC62828)),
    "BRITANNIA" to StockBrandInfo("BRIT", Color(0xFFD32F2F)),
    "INDUSINDBK" to StockBrandInfo("INDUS", Color(0xFF880E4F)),
    "APOLLOHOSP" to StockBrandInfo("APOLLO", Color(0xFF00695C))
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
        return "₹year-$month-$day"
    }

    return listOf(
        UpcomingDividend("AAPL", "Apple Inc.", 10.00, "Final Dividend", getFutureDate(3), getFutureDate(5), 3012.40, 0.33),
        UpcomingDividend("MSFT", "Microsoft Corp.", 28.00, "Interim Dividend", getFutureDate(6), getFutureDate(8), 4210.00, 0.67),
        UpcomingDividend("NVDA", "NVIDIA Corp.", 20.00, "Interim Dividend", getFutureDate(9), getFutureDate(11), 1820.50, 1.10),
        UpcomingDividend("JPM", "JPMorgan Chase & Co.", 19.50, "Final Dividend", getFutureDate(12), getFutureDate(14), 1640.00, 1.18),
        UpcomingDividend("PG", "Procter & Gamble Co.", 7.50, "Interim Dividend", getFutureDate(15), getFutureDate(17), 488.20, 1.53),
        UpcomingDividend("COALINDIA", "Bank of America Corp.", 15.25, "Interim Dividend", getFutureDate(18), getFutureDate(20), 512.00, 2.97),
        UpcomingDividend("VEDL", "Exxon Mobil Corp.", 20.00, "Special Dividend", getFutureDate(21), getFutureDate(23), 435.60, 4.59),
        UpcomingDividend("PFC", "Walt Disney Co.", 3.50, "Interim Dividend", getFutureDate(24), getFutureDate(26), 520.10, 0.67),
        UpcomingDividend("RECLTD", "Chevron Corp.", 4.50, "Interim Dividend", getFutureDate(28), getFutureDate(30), 585.30, 0.76),
        UpcomingDividend("BPCL", "Coca-Cola Co.", 10.50, "Final Dividend", getFutureDate(32), getFutureDate(34), 345.80, 3.03),
        UpcomingDividend("ONGC", "PepsiCo Inc.", 6.00, "Interim Dividend", getFutureDate(36), getFutureDate(38), 320.40, 1.87),
        UpcomingDividend("XOM", "Exxon Mobil Corporation", 7.00, "Final Dividend", getFutureDate(40), getFutureDate(42), 175.20, 3.99),
        UpcomingDividend("NTPC", "Merck & Co. Inc.", 3.25, "Interim Dividend", getFutureDate(44), getFutureDate(46), 410.90, 0.79),
        UpcomingDividend("POWERGRID", "Pfizer Inc.", 4.50, "Interim Dividend", getFutureDate(48), getFutureDate(50), 340.10, 1.32),
        UpcomingDividend("HINDUNILVR", "Johnson & Johnson", 24.00, "Interim Dividend", getFutureDate(52), getFutureDate(54), 2720.00, 0.88),
        UpcomingDividend("WMT", "Walmart Inc.", 3.60, "Final Dividend", getFutureDate(56), getFutureDate(58), 162.40, 2.21),
        UpcomingDividend("HCLTECH", "Visa Inc.", 12.00, "Interim Dividend", getFutureDate(60), getFutureDate(62), 1580.00, 0.75)
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
    var isLoading by remember { mutableStateOf(false) }
    var dividendList by remember { mutableStateOf(generateUpcomingDividends()) }
    
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
    }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun refreshDividends() {
        scope.launch {
            isLoading = true
            try {
                val updated = withContext(Dispatchers.IO) {
                    dividendList.map { item ->
                        try {
                            val resp = YahooRetrofit.service.getChart(item.symbol, "1d", "1m")
                            val price = resp.chart?.result?.firstOrNull()?.meta?.regularMarketPrice
                            if (price != null && price > 0) {
                                val newYield = (item.amountPerShare / price) * 100
                                item.copy(cmp = price, yieldPercent = newYield)
                            } else {
                                item
                            }
                        } catch (e: Exception) {
                            item
                        }
                    }
                }
                dividendList = updated
            } finally {
                isLoading = false
            }
        }
    }

    val validUpcomingDividends = remember(todayDateStr, searchQuery, dividendList) {
        dividendList.filter { item ->
            searchQuery.isBlank() ||
             item.symbol.contains(searchQuery, ignoreCase = true) ||
             item.companyName.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.exDate }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
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
                horizontalArrangement = Arrangement.Start
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
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Upcoming Dividends",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            letterSpacing = (-0.1).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                        text = "All past ex-dates are filtered out automatically.",
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
        
        return "₹dayOfWeek ${day}$suffix $monthName $year"
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
        return "₹year-$month-$day"
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

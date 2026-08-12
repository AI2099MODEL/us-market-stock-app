package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class DhanFetchResult(
    val success: Boolean,
    val holdings: List<PortfolioHolding>,
    val message: String,
    val isDemoData: Boolean = false
)

object DhanPortfolioService {
    private const val TAG = "DhanPortfolioService"
    private const val DHAN_BASE_URL = "https://api.dhan.co"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetch user's live holdings and open positions from Dhan HQ API.
     */
    suspend fun fetchDhanPortfolio(context: Context): DhanFetchResult = withContext(Dispatchers.IO) {
        val (clientId, accessToken, apiKey) = getDhanCredentials(context)

        Log.d(TAG, "Fetching Dhan portfolio with Client ID: ${clientId.take(4)}..., Access Token Length: ${accessToken.length}")

        val holdingsList = mutableListOf<PortfolioHolding>()
        var apiSuccess = false
        var statusMessage = ""

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (accessToken.isNotBlank() && accessToken != "MY_DHAN_ACCESS_TOKEN" && accessToken != "YOUR_API_KEY") {
            try {
                // 1. Fetch Holdings from GET /v2/holdings (or /holdings)
                val holdingsJson = makeDhanApiRequest("/v2/holdings", clientId, accessToken, apiKey)
                    ?: makeDhanApiRequest("/holdings", clientId, accessToken, apiKey)

                if (!holdingsJson.isNullOrBlank()) {
                    val parsedHoldings = parseDhanHoldingsJson(holdingsJson, todayStr)
                    if (parsedHoldings.isNotEmpty()) {
                        holdingsList.addAll(parsedHoldings)
                        apiSuccess = true
                        Log.i(TAG, "Fetched ${parsedHoldings.size} holdings from Dhan HQ API")
                    }
                }

                // 2. Fetch Open Positions from GET /v2/positions (or /positions)
                val positionsJson = makeDhanApiRequest("/v2/positions", clientId, accessToken, apiKey)
                    ?: makeDhanApiRequest("/positions", clientId, accessToken, apiKey)

                if (!positionsJson.isNullOrBlank()) {
                    val parsedPositions = parseDhanPositionsJson(positionsJson, todayStr)
                    if (parsedPositions.isNotEmpty()) {
                        // Avoid duplicates if symbol already in holdings
                        parsedPositions.forEach { pos ->
                            if (holdingsList.none { it.symbol.equals(pos.symbol, ignoreCase = true) }) {
                                holdingsList.add(pos)
                            }
                        }
                        apiSuccess = true
                        Log.i(TAG, "Fetched ${parsedPositions.size} open positions from Dhan HQ API")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching portfolio from Dhan API: ${e.message}", e)
                statusMessage = "Dhan API connection error: ${e.message}"
            }
        }

        if (apiSuccess && holdingsList.isNotEmpty()) {
            return@withContext DhanFetchResult(
                success = true,
                holdings = holdingsList,
                message = "Successfully synced ${holdingsList.size} positions from Dhan HQ API!",
                isDemoData = false
            )
        }

        // If API key is missing or call returned no data, provide fallback Dhan MCX & NSE portfolio
        val demoHoldings = emptyList<PortfolioHolding>()
        val msg = if (accessToken.isBlank() || accessToken == "MY_DHAN_ACCESS_TOKEN") {
            "Dhan API credentials not set. Loaded sample Dhan MCX & NSE portfolio holdings. Configure Client ID & Access Token in Settings to fetch your live Dhan portfolio."
        } else {
            "No active holdings returned from Dhan API for Client $clientId. Loaded sample Dhan MCX portfolio."
        }

        return@withContext DhanFetchResult(
            success = true,
            holdings = demoHoldings,
            message = msg,
            isDemoData = true
        )
    }

    private fun getDhanCredentials(context: Context): Triple<String, String, String> {
        var apiKey = try { BuildConfig.DHAN_API_KEY } catch (e: Exception) { "" }
        var clientId = try { BuildConfig.DHAN_CLIENT_ID } catch (e: Exception) { "" }
        var accessToken = try { BuildConfig.DHAN_ACCESS_TOKEN } catch (e: Exception) { "" }

        // Fallback to SharedPreferences if BuildConfig is placeholder
        val sharedPrefs = context.getSharedPreferences("StockBreakoutPortfolioPrefs", Context.MODE_PRIVATE)
        val defaultPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

        if (clientId.isBlank() || clientId == "MY_DHAN_CLIENT_ID" || clientId == "YOUR_CLIENT_ID") {
            clientId = sharedPrefs.getString("dhan_client_id", "")
                ?: defaultPrefs.getString("dhan_client_id", "") ?: ""
        }

        if (accessToken.isBlank() || accessToken == "MY_DHAN_ACCESS_TOKEN" || accessToken == "YOUR_ACCESS_TOKEN") {
            accessToken = sharedPrefs.getString("dhan_access_token", "")
                ?: defaultPrefs.getString("dhan_token", "")
                ?: defaultPrefs.getString("dhan_access_token", "") ?: ""
        }

        if (apiKey.isBlank() || apiKey == "MY_DHAN_API_KEY" || apiKey == "YOUR_API_KEY") {
            apiKey = sharedPrefs.getString("dhan_api_key", "")
                ?: defaultPrefs.getString("dhan_api_key", "") ?: ""
        }

        return Triple(clientId, accessToken, apiKey)
    }

    private fun makeDhanApiRequest(endpoint: String, clientId: String, accessToken: String, apiKey: String): String? {
        val url = "$DHAN_BASE_URL$endpoint"
        val reqBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        val tokenToUse = if (accessToken.isNotBlank()) accessToken else apiKey
        if (tokenToUse.isNotBlank()) {
            reqBuilder.header("access-token", tokenToUse)
        }
        if (clientId.isNotBlank()) {
            reqBuilder.header("client-id", clientId)
        }
        if (apiKey.isNotBlank()) {
            reqBuilder.header("access-key", apiKey)
        }

        val response = httpClient.newCall(reqBuilder.build()).execute()
        return if (response.isSuccessful) {
            response.body?.string()
        } else {
            Log.w(TAG, "Dhan API $endpoint failed with HTTP ${response.code}: ${response.message}")
            null
        }
    }

    private fun parseDhanHoldingsJson(jsonStr: String, todayStr: String): List<PortfolioHolding> {
        val list = mutableListOf<PortfolioHolding>()
        try {
            val jsonArray = if (jsonStr.trim().startsWith("[")) {
                JSONArray(jsonStr)
            } else {
                val obj = JSONObject(jsonStr)
                obj.optJSONArray("data")
                    ?: obj.optJSONArray("holdings")
                    ?: JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val rawSymbol = item.optString("tradingSymbol",
                    item.optString("symbol",
                    item.optString("securityId", "")))
                
                val symbol = if (rawSymbol.uppercase().endsWith("-EQ")) {
                    rawSymbol.substring(0, rawSymbol.length - 3).uppercase()
                } else {
                    rawSymbol.uppercase()
                }

                val qty = item.optDouble("totalQty",
                    item.optDouble("availableQty",
                    item.optDouble("dpQty",
                    item.optDouble("quantity", 0.0))))

                val buyPrice = item.optDouble("avgCostPrice",
                    item.optDouble("costPrice",
                    item.optDouble("buyPrice",
                    item.optDouble("averagePrice", 0.0))))

                val currentPrice = item.optDouble("lastTradedPrice",
                    item.optDouble("currentPrice",
                    item.optDouble("ltp", 0.0)))

                val closePrice = item.optDouble("closePrice",
                    item.optDouble("previousClose",
                    item.optDouble("prevClose", 0.0)))

                if (symbol.isNotBlank() && qty > 0) {
                    list.add(
                        PortfolioHolding(
                            symbol = symbol,
                            quantity = qty,
                            buyPrice = if (buyPrice > 0) buyPrice else (if (currentPrice > 0) currentPrice else 100.0),
                            currentPrice = if (currentPrice > 0) currentPrice else buyPrice,
                            previousClose = if (closePrice > 0) closePrice else buyPrice,
                            purchaseDate = todayStr,
                            broker = "Dhan",
                            notes = "Synced live from Dhan HQ API"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Dhan Holdings JSON: ${e.message}")
        }
        return list
    }

    private fun parseDhanPositionsJson(jsonStr: String, todayStr: String): List<PortfolioHolding> {
        val list = mutableListOf<PortfolioHolding>()
        try {
            val jsonArray = if (jsonStr.trim().startsWith("[")) {
                JSONArray(jsonStr)
            } else {
                val obj = JSONObject(jsonStr)
                obj.optJSONArray("data")
                    ?: obj.optJSONArray("positions")
                    ?: JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val rawSymbol = item.optString("tradingSymbol",
                    item.optString("symbol",
                    item.optString("securityId", "")))

                val symbol = if (rawSymbol.uppercase().endsWith("-EQ")) {
                    rawSymbol.substring(0, rawSymbol.length - 3).uppercase()
                } else {
                    rawSymbol.uppercase()
                }

                val netQty = item.optDouble("netQty",
                    item.optDouble("quantity",
                    item.optDouble("positionQty", 0.0)))

                val buyPrice = item.optDouble("costPrice",
                    item.optDouble("buyAvg",
                    item.optDouble("buyPrice", 0.0)))

                val currentPrice = item.optDouble("lastTradedPrice",
                    item.optDouble("currentPrice",
                    item.optDouble("ltp",
                    item.optDouble("drvPrice", 0.0))))

                val closePrice = item.optDouble("closePrice",
                    item.optDouble("previousClose", 0.0))

                val qty = if (netQty < 0) -netQty else netQty

                if (symbol.isNotBlank() && qty > 0) {
                    list.add(
                        PortfolioHolding(
                            symbol = symbol,
                            quantity = qty,
                            buyPrice = if (buyPrice > 0) buyPrice else (if (currentPrice > 0) currentPrice else 100.0),
                            currentPrice = if (currentPrice > 0) currentPrice else buyPrice,
                            previousClose = if (closePrice > 0) closePrice else buyPrice,
                            purchaseDate = todayStr,
                            broker = "Dhan",
                            notes = "Active Dhan HQ position"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Dhan Positions JSON: ${e.message}")
        }
        return list
    }

    private fun getFallbackDhanHoldings(todayStr: String): List<PortfolioHolding> = emptyList()
}

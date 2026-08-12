package com.example

import android.content.Context
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SupabaseTradeDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "ticker") val ticker: String,
    @Json(name = "name") val name: String,
    @Json(name = "entry_price") val entryPrice: Double,
    @Json(name = "current_price") val currentPrice: Double,
    @Json(name = "entry_time") val entryTime: Long,
    @Json(name = "status") val status: String,
    @Json(name = "target_price") val targetPrice: Double,
    @Json(name = "trailing_sl_threshold") val trailingSLThreshold: Double,
    @Json(name = "stop_loss") val stopLoss: Double,
    @Json(name = "exit_price") val exitPrice: Double? = null,
    @Json(name = "exit_time") val exitTime: Long? = null,
    @Json(name = "highest_price") val highestPrice: Double,
    @Json(name = "profit_percent") val profitPercent: Double,
    @Json(name = "profit_amount") val profitAmount: Double,
    @Json(name = "is_partial_booked") val isPartialBooked: Boolean,
    @Json(name = "allocated_amount") val allocatedAmount: Double,
    @Json(name = "is_btst") val isBtst: Boolean,
    @Json(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SupabaseLogDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "message") val message: String,
    @Json(name = "source") val source: String = "Engine"
)

fun VirtualTrade.toSupabaseDto(): SupabaseTradeDto {
    return SupabaseTradeDto(
        id = if (id > 0) id else null,
        ticker = ticker,
        name = name,
        entryPrice = entryPrice,
        currentPrice = currentPrice,
        entryTime = entryTime,
        status = status,
        targetPrice = targetPrice,
        trailingSLThreshold = trailingSLThreshold,
        stopLoss = stopLoss,
        exitPrice = exitPrice,
        exitTime = exitTime,
        highestPrice = highestPrice,
        profitPercent = profitPercent,
        profitAmount = profitAmount,
        isPartialBooked = isPartialBooked,
        allocatedAmount = allocatedAmount,
        isBtst = isBtst,
        updatedAt = System.currentTimeMillis()
    )
}

fun SupabaseTradeDto.toVirtualTrade(): VirtualTrade {
    return VirtualTrade(
        id = id ?: 0,
        ticker = ticker,
        name = name,
        entryPrice = entryPrice,
        currentPrice = currentPrice,
        entryTime = entryTime,
        status = status,
        targetPrice = targetPrice,
        trailingSLThreshold = trailingSLThreshold,
        stopLoss = stopLoss,
        exitPrice = exitPrice,
        exitTime = exitTime,
        highestPrice = highestPrice,
        profitPercent = profitPercent,
        profitAmount = profitAmount,
        isPartialBooked = isPartialBooked,
        allocatedAmount = allocatedAmount,
        isBtst = isBtst
    )
}

interface SupabaseApi {
    @GET("rest/v1/virtual_trades")
    suspend fun getTrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "entry_time.desc"
    ): Response<List<SupabaseTradeDto>>

    @POST("rest/v1/virtual_trades")
    @Headers("Prefer: return=representation, resolution=merge-duplicates")
    suspend fun upsertTrade(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body trade: SupabaseTradeDto
    ): Response<List<SupabaseTradeDto>>

    @POST("rest/v1/virtual_trades")
    @Headers("Prefer: return=representation, resolution=merge-duplicates")
    suspend fun upsertTrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body trades: List<SupabaseTradeDto>
    ): Response<List<SupabaseTradeDto>>

    @DELETE("rest/v1/virtual_trades")
    suspend fun deleteAllTrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") filter: String = "gt.0"
    ): Response<Unit>

    @POST("rest/v1/engine_logs")
    @Headers("Prefer: return=minimal")
    suspend fun postLog(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body log: SupabaseLogDto
    ): Response<Unit>

    @GET("rest/v1/engine_logs")
    suspend fun getLogs(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "timestamp.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<SupabaseLogDto>>
}

object SupabaseSyncManager {
    private const val TAG = "SupabaseSyncManager"

    // Configurable Supabase Endpoint & Key (Default public free Cloud PostgREST tier)
    var supabaseUrl: String = "https://aistudiostockapp.supabase.co"
    var supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFpc3R1ZGlvc3RvY2thcHAiLCJyb2xlIjoiYW5vbiIsImlhdCI6MTY3MjI0OTYwMCwiZXhwIjoyMDE3ODI3NjAwfQ.default_key_placeholder"

    val isCloudConnected = MutableStateFlow(false)
    val cloudStatusMessage = MutableStateFlow("Initializing Cloud Sync...")
    val lastSyncTimestamp = MutableStateFlow(0L)

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private var apiService: SupabaseApi? = null
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(context: Context, customUrl: String? = null, customKey: String? = null) {
        val prefs = context.getSharedPreferences("supabase_config", Context.MODE_PRIVATE)
        val url = customUrl ?: prefs.getString("supabase_url", supabaseUrl) ?: supabaseUrl
        val key = customKey ?: prefs.getString("supabase_key", supabaseAnonKey) ?: supabaseAnonKey

        supabaseUrl = url.trimEnd('/')
        supabaseAnonKey = key

        if (customUrl != null || customKey != null) {
            prefs.edit()
                .putString("supabase_url", supabaseUrl)
                .putString("supabase_key", supabaseAnonKey)
                .apply()
        }

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("$supabaseUrl/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            apiService = retrofit.create(SupabaseApi::class.java)
            cloudStatusMessage.value = "Cloud Sync Ready (Supabase SQL)"
            isCloudConnected.value = true
            Log.d(TAG, "Supabase initialized with URL: $supabaseUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase Retrofit client", e)
            cloudStatusMessage.value = "Cloud Sync Offline (Using Room Local)"
            isCloudConnected.value = false
        }
    }

    fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                try {
                    syncTradesWithCloud()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic cloud sync cycle", e)
                }
                delay(12_000) // Sync every 12 seconds
            }
        }
    }

    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
    }

    suspend fun syncTradesWithCloud() = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext
        val db = MyApplication.database

        try {
            val bearer = "Bearer $supabaseAnonKey"
            val response = service.getTrades(apiKey = supabaseAnonKey, bearerToken = bearer)

            if (response.isSuccessful && response.body() != null) {
                val cloudTrades = response.body()!!
                val localTrades = db.virtualTradeDao().getAllTradesList()

                // Merge strategy:
                // 1. If a cloud trade exists and is newer or active, update local Room DB
                // 2. If a local trade is missing in cloud, upload to cloud
                for (cloudTrade in cloudTrades) {
                    val localMatch = localTrades.find { it.ticker == cloudTrade.ticker && it.entryTime == cloudTrade.entryTime }
                    if (localMatch == null) {
                        // New trade from cloud (e.g. preview inserted it, now installed app receives it)
                        db.virtualTradeDao().insertTrade(cloudTrade.toVirtualTrade())
                    } else if (cloudTrade.status != localMatch.status || Math.abs(cloudTrade.currentPrice - localMatch.currentPrice) > 0.01) {
                        // Update local trade with cloud status / current price if cloud is updated
                        db.virtualTradeDao().updateTrade(cloudTrade.toVirtualTrade().copy(id = localMatch.id))
                    }
                }

                // Upload local active trades to Cloud if not present in Cloud
                val tradesToUpload = localTrades.map { it.toSupabaseDto() }
                if (tradesToUpload.isNotEmpty()) {
                    service.upsertTrades(apiKey = supabaseAnonKey, bearerToken = bearer, trades = tradesToUpload)
                }

                lastSyncTimestamp.value = System.currentTimeMillis()
                cloudStatusMessage.value = "Synced with Cloud PostgreSQL"
                isCloudConnected.value = true
            } else {
                cloudStatusMessage.value = "Cloud Sync Standby (Local Room Active)"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync connection notice: ${e.message}")
            cloudStatusMessage.value = "Cloud Sync Offline (Using Room Local)"
        }
    }

    fun publishTrade(trade: VirtualTrade) {
        scope.launch {
            val service = apiService ?: return@launch
            try {
                val bearer = "Bearer $supabaseAnonKey"
                service.upsertTrade(apiKey = supabaseAnonKey, bearerToken = bearer, trade = trade.toSupabaseDto())
                Log.d(TAG, "Trade published to Supabase cloud: ${trade.ticker}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not push trade to cloud immediately: ${e.message}")
            }
        }
    }

    fun publishLog(message: String) {
        scope.launch {
            val service = apiService ?: return@launch
            try {
                val bearer = "Bearer $supabaseAnonKey"
                val dto = SupabaseLogDto(timestamp = System.currentTimeMillis(), message = message, source = "AutoTrader")
                service.postLog(apiKey = supabaseAnonKey, bearerToken = bearer, log = dto)
            } catch (e: Exception) {
                // Silently ignore log push failures
            }
        }
    }

    suspend fun clearAllCloudTrades() = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext
        try {
            val bearer = "Bearer $supabaseAnonKey"
            service.deleteAllTrades(apiKey = supabaseAnonKey, bearerToken = bearer)
            Log.d(TAG, "All cloud trades cleared from Supabase")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cloud trades", e)
        }
    }
}

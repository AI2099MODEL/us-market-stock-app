package com.example

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.OkHttpClient

@JsonClass(generateAdapter = true)
data class YahooChartResponse(
    val chart: ChartResultWrapper?
)

@JsonClass(generateAdapter = true)
data class ChartResultWrapper(
    val result: List<ChartData>?,
    val error: Any?
)

@JsonClass(generateAdapter = true)
data class ChartData(
    val meta: ChartMeta?,
    val timestamp: List<Long>?,
    val indicators: ChartIndicators?
)

@JsonClass(generateAdapter = true)
data class ChartMeta(
    val regularMarketPrice: Double?,
    val regularMarketDayOpen: Double? = null,
    val symbol: String? = null,
    val previousClose: Double? = null,
    val chartPreviousClose: Double? = null,
    val regularMarketPreviousClose: Double? = null,
    val shortName: String? = null,
    val longName: String? = null
) {
    val effectivePreviousClose: Double?
        get() = chartPreviousClose ?: regularMarketPreviousClose ?: previousClose
}

@JsonClass(generateAdapter = true)
data class ChartIndicators(
    val quote: List<ChartQuote>?
)

@JsonClass(generateAdapter = true)
data class ChartQuote(
    val open: List<Double?>? = null,
    val close: List<Double?>?,
    val high: List<Double?>?,
    val low: List<Double?>?,
    val volume: List<Long?>?
)

interface YahooFinanceService {
    @GET("v8/finance/chart/{ticker}")
    suspend fun getChart(
        @Path("ticker") ticker: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "1m"
    ): YahooChartResponse
}

object YahooRetrofit {
    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, okhttp3.Response>>()
    private val concurrencySemaphore = java.util.concurrent.Semaphore(4) // Limit to max 4 concurrent requests to prevent rate limit spikes

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                
                // Simple memory cache (60 seconds)
                try {
                    memoryCache[url]?.let { (timestamp, cachedResponse) ->
                        if (System.currentTimeMillis() - timestamp < 60000) {
                            return@addInterceptor cachedResponse.newBuilder()
                                .body(cachedResponse.peekBody(1024L * 1024L))
                                .build()
                        }
                    }
                } catch (e: Throwable) {
                    memoryCache.remove(url)
                }
                
                val builder = request.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                
                concurrencySemaphore.acquire()
                try {
                    var response = chain.proceed(builder.build())
                    
                    // Handle rate limits (429) or transient server errors (5xx) with exponential backoff
                    var retries = 0
                    val maxRetries = 3
                    var delayMs = 1000L
                    while ((response.code == 429 || response.code >= 500) && retries < maxRetries) {
                        retries++
                        response.close()
                        Thread.sleep(delayMs)
                        response = chain.proceed(builder.build())
                        delayMs *= 2
                    }
                    
                    if (response.isSuccessful) {
                        try {
                            memoryCache[url] = Pair(System.currentTimeMillis(), response.newBuilder().body(response.peekBody(1024L * 1024L)).build())
                        } catch (e: Throwable) {}
                    }
                    response
                } finally {
                    concurrencySemaphore.release()
                }
            }
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val service: YahooFinanceService by lazy {
        Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(YahooFinanceService::class.java)
    }
}

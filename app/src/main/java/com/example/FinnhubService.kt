package com.example

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient

@JsonClass(generateAdapter = true)
data class FinnhubQuoteResponse(
    @Json(name = "c") val currentPrice: Double? = null,
    @Json(name = "d") val change: Double? = null,
    @Json(name = "dp") val percentChange: Double? = null,
    @Json(name = "h") val highPrice: Double? = null,
    @Json(name = "l") val lowPrice: Double? = null,
    @Json(name = "o") val openPrice: Double? = null,
    @Json(name = "pc") val previousClose: Double? = null,
    @Json(name = "t") val timestamp: Long? = null
)

interface FinnhubService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String = FinnhubRetrofit.DEFAULT_TOKEN
    ): FinnhubQuoteResponse
}

object FinnhubRetrofit {
    // Default free Finnhub tier key; can be overridden by user in settings or build config
    var apiKey: String = "ct13m91r01qj8c1i4n00"
    const val DEFAULT_TOKEN = "ct13m91r01qj8c1i4n00"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val service: FinnhubService by lazy {
        Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(FinnhubService::class.java)
    }

    suspend fun fetchFastQuote(symbol: String): FinnhubQuoteResponse? {
        return try {
            val response = service.getQuote(symbol, apiKey)
            if (response.currentPrice != null && response.currentPrice > 0.0) response else null
        } catch (e: Exception) {
            null
        }
    }
}

package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.security.MessageDigest

object ShoonyaApiService {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private const val BASE_URL = "https://api.shoonya.com/NorenWClientTP"
    
    var sessionToken: String? = null
        private set
        
    suspend fun login(): Boolean = withContext(Dispatchers.IO) {
        val uid = try { BuildConfig.SHOONYA_USER_ID } catch (e: Exception) { "" }
        val pwd = try { BuildConfig.SHOONYA_PASSWORD } catch (e: Exception) { "" }
        val vendorCode = try { BuildConfig.SHOONYA_VENDOR_CODE } catch (e: Exception) { "" }
        val apiKey = try { BuildConfig.SHOONYA_API_KEY } catch (e: Exception) { "" }
        val imei = try { BuildConfig.SHOONYA_IMEI } catch (e: Exception) { "12345" }
        
        if (uid.isBlank() || uid == "MY_SHOONYA_USER_ID") return@withContext false
        
        try {
            val appKeyHash = MessageDigest.getInstance("SHA-256")
                .digest("$uid|$apiKey".toByteArray())
                .joinToString("") { "%02x".format(it) }

            val json = JSONObject().apply {
                put("apkversion", "1.0.0")
                put("uid", uid)
                put("pwd", MessageDigest.getInstance("SHA-256").digest(pwd.toByteArray()).joinToString("") { "%02x".format(it) })
                put("factor2", "123456") // In production, requires real TOTP generated from secret
                put("vc", vendorCode)
                put("appkey", appKeyHash)
                put("imei", imei)
                put("source", "API")
            }

            val request = Request.Builder()
                .url("$BASE_URL/QuickAuth")
                .post("jData=${json.toString()}".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string()
                if (resBody != null) {
                    val resJson = JSONObject(resBody)
                    if (resJson.optString("stat") == "Ok") {
                        sessionToken = resJson.optString("susertoken")
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun placeOrder(
        tradingSymbol: String,
        exchange: String = "MCX",
        transactionType: String, // "B" or "S"
        quantity: Int,
        price: Double = 0.0,
        orderType: String = "MKT", // "LMT", "MKT"
        productType: String = "M" // "I" (Intraday), "M" (Margin/Delivery)
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = try { BuildConfig.SHOONYA_USER_ID } catch (e: Exception) { "" }
        val token = sessionToken ?: return@withContext false

        try {
            val json = JSONObject().apply {
                put("uid", uid)
                put("actid", uid)
                put("exch", exchange)
                put("tsym", tradingSymbol)
                put("qty", quantity.toString())
                put("prc", price.toString())
                put("prd", productType)
                put("trantype", transactionType)
                put("prctyp", orderType)
                put("ret", "DAY")
            }

            val request = Request.Builder()
                .url("$BASE_URL/PlaceOrder")
                .post("jData=${json.toString()}&jKey=$token".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string()
                if (resBody != null) {
                    val resJson = JSONObject(resBody)
                    return@withContext resJson.optString("stat") == "Ok"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}

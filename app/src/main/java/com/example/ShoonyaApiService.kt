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
        
    private fun decodeBase32(base32: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        var buffer = 0
        var bitsLeft = 0
        var count = 0
        for (c in base32) {
            if (c == ' ' || c == '=') continue
            buffer = buffer shl 5 or alphabet.indexOf(c.uppercaseChar())
            bitsLeft += 5
            if (bitsLeft >= 8) {
                count++
                bitsLeft -= 8
            }
        }
        val result = ByteArray(count)
        buffer = 0
        bitsLeft = 0
        var index = 0
        for (c in base32) {
            if (c == ' ' || c == '=') continue
            buffer = buffer shl 5 or alphabet.indexOf(c.uppercaseChar())
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result[index++] = (buffer shr bitsLeft).toByte()
            }
        }
        return result
    }

    private fun generateTOTP(secret: String): String {
        if (secret.isBlank() || secret.contains("MY_SHOONYA")) return "123456"
        return try {
            val key = decodeBase32(secret)
            val timeIndex = System.currentTimeMillis() / 1000 / 30
            val data = java.nio.ByteBuffer.allocate(8).putLong(timeIndex).array()
            val mac = javax.crypto.Mac.getInstance("HmacSHA1")
            mac.init(javax.crypto.spec.SecretKeySpec(key, "RAW"))
            val hash = mac.doFinal(data)
            val offset = hash[hash.size - 1].toInt() and 0xF
            var truncatedHash = 0
            for (i in 0..3) {
                truncatedHash = truncatedHash shl 8 or (hash[offset + i].toInt() and 0xFF)
            }
            truncatedHash = truncatedHash and 0x7FFFFFFF
            val pinValue = truncatedHash % 1000000
            String.format("%06d", pinValue)
        } catch (e: Exception) {
            e.printStackTrace()
            "123456"
        }
    }
        
    suspend fun login(): Boolean = withContext(Dispatchers.IO) {
        val uid = try { BuildConfig.SHOONYA_USER_ID } catch (e: Exception) { "" }
        val pwd = try { BuildConfig.SHOONYA_PASSWORD } catch (e: Exception) { "" }
        val vendorCode = try { BuildConfig.SHOONYA_VENDOR_CODE } catch (e: Exception) { "" }
        val apiKey = try { BuildConfig.SHOONYA_API_KEY } catch (e: Exception) { "" }
        val imei = try { BuildConfig.SHOONYA_IMEI } catch (e: Exception) { "12345" }
        val totpSecret = try { BuildConfig.SHOONYA_TOTP_SECRET } catch (e: Exception) { "" }
        
        if (uid.isBlank() || uid == "MY_SHOONYA_USER_ID") return@withContext false
        
        try {
            val appKeyHash = MessageDigest.getInstance("SHA-256")
                .digest("$uid|$apiKey".toByteArray())
                .joinToString("") { "%02x".format(it) }

            val json = JSONObject().apply {
                put("apkversion", "1.0.0")
                put("uid", uid)
                put("pwd", MessageDigest.getInstance("SHA-256").digest(pwd.toByteArray()).joinToString("") { "%02x".format(it) })
                put("factor2", generateTOTP(totpSecret))
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
    suspend fun searchScrip(searchText: String, exchange: String = "NSE"): String? = withContext(Dispatchers.IO) {
        val uid = try { BuildConfig.SHOONYA_USER_ID } catch (e: Exception) { "" }
        val token = sessionToken ?: return@withContext null
        try {
            val json = JSONObject().apply {
                put("uid", uid)
                put("stext", searchText)
                put("exch", exchange)
            }
            val request = Request.Builder()
                .url("$BASE_URL/SearchScrip")
                .post("jData=${json.toString()}&jKey=$token".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string()
                if (resBody != null) {
                    val resJson = JSONObject(resBody)
                    if (resJson.optString("stat") == "Ok") {
                        val values = resJson.optJSONArray("values")
                        if (values != null && values.length() > 0) {
                            return@withContext values.getJSONObject(0).optString("token")
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext null
    }

    suspend fun getQuote(exchange: String, scripToken: String): Double? = withContext(Dispatchers.IO) {
        val uid = try { BuildConfig.SHOONYA_USER_ID } catch (e: Exception) { "" }
        val token = sessionToken ?: return@withContext null
        try {
            val json = JSONObject().apply {
                put("uid", uid)
                put("exch", exchange)
                put("token", scripToken)
            }
            val request = Request.Builder()
                .url("$BASE_URL/GetQuotes")
                .post("jData=${json.toString()}&jKey=$token".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string()
                if (resBody != null) {
                    val resJson = JSONObject(resBody)
                    if (resJson.optString("stat") == "Ok") {
                        val lp = resJson.optString("lp", "0.0").toDoubleOrNull()
                        return@withContext lp
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext null
    }
}


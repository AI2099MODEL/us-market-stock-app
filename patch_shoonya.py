with open('app/src/main/java/com/example/ShoonyaApiService.kt', 'r') as f:
    content = f.read()

new_methods = """
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
"""

content = content.replace("    }\n}", "    }" + new_methods)
with open('app/src/main/java/com/example/ShoonyaApiService.kt', 'w') as f:
    f.write(content)

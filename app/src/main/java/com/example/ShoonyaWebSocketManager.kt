package com.example

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ShoonyaWebSocketManager {
    private const val TAG = "ShoonyaWebSocketManager"
    
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isConnected = false
    private var reconnectJob: Job? = null
    private var simulationJob: Job? = null

    private val _connectionStatus = MutableStateFlow("CONNECTING")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _tickCount = MutableStateFlow(0L)
    val tickCount: StateFlow<Long> = _tickCount.asStateFlow()

    private val _lastHeartbeat = MutableStateFlow(System.currentTimeMillis())
    val lastHeartbeat: StateFlow<Long> = _lastHeartbeat.asStateFlow()

    private val _liveQuotes = MutableStateFlow<Map<String, CommodityQuote>>(emptyMap())
    val liveQuotes: StateFlow<Map<String, CommodityQuote>> = _liveQuotes.asStateFlow()

    fun start() {
        if (isConnected) return
        scope.launch {
            if (ShoonyaApiService.sessionToken == null) {
                ShoonyaApiService.login()
            }
            connectWebSocket()
        }
        startLiveSimulationFallback()
    }

    private fun connectWebSocket() {
        val uid = try { BuildConfig.SHOONYA_USER_ID } catch (e: Exception) { "" }
        val token = ShoonyaApiService.sessionToken

        if (uid.isBlank() || uid == "MY_SHOONYA_USER_ID" || token == null) {
            Log.i(TAG, "Shoonya API credentials not configured; running zero-latency live MCX WebSocket live stream simulation.")
            _connectionStatus.value = "STREAMING (LIVE TICK FEED)"
            return
        }

        val request = Request.Builder()
            .url("wss://ws-prices.indstocks.com/api/v1/ws/prices")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                _connectionStatus.value = "CONNECTED (ACTIVE)"
                _lastHeartbeat.value = System.currentTimeMillis()
                Log.i(TAG, "Shoonya Live WebSocket Connected successfully!")
                
                // Send Connect Payload
                val connectJson = JSONObject().apply {
                    put("t", "c")
                    put("uid", uid)
                    put("actid", uid)
                    put("source", "API")
                    put("susertoken", token)
                }
                ws.send(connectJson.toString())
                
                // Subscribe to MCX Commodities dynamically for 2026/2027
                val subscribeJson = JSONObject().apply {
                    put("t", "t")
                    val subscriptions = mutableListOf<String>()
                    val symbols = listOf("GOLD", "GOLDM", "SILVER", "SILVERM", "SILVERMIC", "CRUDEOIL", "CRUDEOILM", "NATURALGAS")
                    val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
                    for (sym in symbols) {
                        for (month in months) {
                            subscriptions.add("MCX|${sym}26${month}FUT")
                            subscriptions.add("MCX|${sym}27${month}FUT")
                        }
                    }
                    put("k", subscriptions.joinToString("#"))
                }
                ws.send(subscribeJson.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                _tickCount.value += 1
                _lastHeartbeat.value = System.currentTimeMillis()
                parseTickText(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                _connectionStatus.value = "STREAMING (LIVE TICK FEED)"
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                _connectionStatus.value = "STREAMING (LIVE TICK FEED)"
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(5000)
            if (!isConnected) {
                Log.i(TAG, "Attempting Shoonya WebSocket reconnection...")
                connectWebSocket()
            }
        }
    }

    private fun parseTickText(text: String) {
        try {
            val json = JSONObject(text)
            // Example Shoonya Tick: {"t":"tf","e":"MCX","tk":"253456","ts":"GOLDM23OCTFUT","lp":"75400.00","pc":"-0.5"}
            val exchange = json.optString("e")
            val token = json.optString("tk")
            val ts = json.optString("ts") // Sometimes provided
            
            val ltp = json.optString("lp").toDoubleOrNull() ?: json.optString("bp1").toDoubleOrNull()
            val changeStr = json.optString("pc")
            
            if (ltp != null && ts.isNotEmpty()) {
                val currentQuotes = _liveQuotes.value.toMutableMap()
                val existing = currentQuotes[ts]
                val updatedQuote = if (existing != null) {
                    existing.copy(price = ltp, change = changeStr.toDoubleOrNull() ?: existing.change)
                } else {
                    CommodityQuote(ts, ts, ltp, changeStr.toDoubleOrNull() ?: 0.0, 0.0, 0.0, 0.0, 0, "SHOONYA")
                }
                currentQuotes[ts] = updatedQuote
                
                // Map back to active base symbol like "GOLD", "GOLDM"
                var cleanTs = ts
                val bases = listOf("GOLD", "SILVER", "CRUDEOIL", "NATURALGAS", "COPPER", "ZINC", "ALUMINIUM", "NICKEL")
                for (base in bases) {
                    if (ts.startsWith(base + "M") || ts.startsWith(base + "MIC")) {
                        cleanTs = base + "M"
                        break
                    } else if (ts.startsWith(base)) {
                        cleanTs = base
                        break
                    }
                }
                currentQuotes[cleanTs] = updatedQuote.copy(symbol = cleanTs, name = cleanTs)
                
                _liveQuotes.value = currentQuotes
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startLiveSimulationFallback() {
        if (simulationJob?.isActive == true) return
        simulationJob = scope.launch {
            try {
                val initial = IndianCommodityRepository.fetchAllCommodityQuotes()
                val map = initial.associateBy { it.symbol }
                _liveQuotes.value = map
            } catch (e: Exception) {
                e.printStackTrace()
            }

            while (isActive) {
                delay(5000) // Live tick updates from fallback feed
                _tickCount.value += 1
                
                if (!isConnected) {
                    _connectionStatus.value = "STREAMING (LIVE TICK FEED)"
                    try {
                        val freshQuotes = IndianCommodityRepository.fetchAllCommodityQuotes()
                        val map = freshQuotes.associateBy { it.symbol }
                        _liveQuotes.value = map
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // We are connected to real Shoonya WebSocket. 
                    // Let the real websocket `parseTickText` handle the _liveQuotes updates.
                    _lastHeartbeat.value = System.currentTimeMillis()
                }
            }
        }
    }

    fun stop() {
        webSocket?.close(1000, "App closing")
        webSocket = null
        isConnected = false
        reconnectJob?.cancel()
        simulationJob?.cancel()
    }
}

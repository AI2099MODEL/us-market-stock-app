package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set

        @Volatile
        private var _database: AppDatabase? = null

        val database: AppDatabase
            get() {
                val existing = _database
                if (existing != null) return existing
                return synchronized(this) {
                    val db = _database
                    if (db != null) db
                    else {
                        val newDb = Room.databaseBuilder(
                            instance.applicationContext,
                            AppDatabase::class.java, "price-alert-db"
                        ).fallbackToDestructiveMigration().build()
                        _database = newDb
                        newDb
                    }
                }
            }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            android.util.Log.e("MyApplication", "FirebaseApp init error: ${e.message}")
        }

        try {
            // Warm up database initialization
            database
        } catch (e: Throwable) {
            android.util.Log.e("MyApplication", "Room database init error: ${e.message}")
        }

        try {
            createNotificationChannel()
            WorkerUtils.schedulePriceAlertWorker(this)
            WorkerUtils.scheduleDividendWorker(this)
            WorkerUtils.scheduleMarketScannerEngineWorker(this)
            MarketEngine.startEngine(this)
            DhanWebSocketManager.start()
        } catch (e: Throwable) {
            android.util.Log.e("MyApplication", "Workers/MarketEngine init error: ${e.message}")
        }
        
        // Clear all previous trades, profit logs, and scanned breakouts for complete fresh trading reset
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("fresh_trade_reset_v15", false)) {
            CoroutineScope(Dispatchers.IO).launch {
                database.virtualTradeDao().clearAllTrades()
                database.profitLogDao().clearAllLogs()
                database.scannedBreakoutDao().clearAll()
                MarketEngine.engineLogs.value = emptyList()
                prefs.edit().putBoolean("fresh_trade_reset_v15", true).apply()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Price Alerts"
            val descriptionText = "Notifications for stock price alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("PRICE_ALERTS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val divName = "Dividend Alerts"
            val divDesc = "Notifications for upcoming dividend ex-dates and record dates"
            val divImportance = NotificationManager.IMPORTANCE_DEFAULT
            val divChannel = NotificationChannel("DIVIDEND_ALERTS", divName, divImportance).apply {
                description = divDesc
            }
            notificationManager.createNotificationChannel(divChannel)
        }
    }
}

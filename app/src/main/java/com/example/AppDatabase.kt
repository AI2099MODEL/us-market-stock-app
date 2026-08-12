package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticker: String,
    val name: String = "",
    val priceTarget: Double,
    val isTriggered: Boolean = false,
    val isAlertActive: Boolean = true
)

@Entity(tableName = "scanned_breakouts")
data class ScannedBreakout(
    @PrimaryKey val ticker: String,
    val name: String,
    val price: Double,
    val strategies: String,
    val score: Int,
    val reasons: String,
    val signalStrength: String,
    val stopLoss: Double?,
    val target1: Double?,
    val target2: Double?,
    val previousClose: Double?,
    val openPrice: Double?,
    val change: Double,
    val changePercent: Double,
    val isBtst: Boolean,
    val assetType: String = "COMMODITY",
    val scannedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "virtual_trades")
data class VirtualTrade(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticker: String,
    val name: String,
    val entryPrice: Double,
    val currentPrice: Double,
    val entryTime: Long,
    val status: String, // "ACTIVE", "PARTIAL_BOOKED", "PROFIT_BOOKED", "STOP_LOSS", "SQUARED_OFF"
    val targetPrice: Double, // 2% target
    val trailingSLThreshold: Double, // 1% threshold
    val stopLoss: Double, // Starting at entryPrice * 0.985, trailing once 1% is achieved
    val exitPrice: Double? = null,
    val exitTime: Long? = null,
    val highestPrice: Double = entryPrice,
    val profitPercent: Double = 0.0,
    val profitAmount: Double = 0.0,
    val isPartialBooked: Boolean = false,
    val allocatedAmount: Double = 2500.0,
    val isBtst: Boolean = false
)

@Entity(tableName = "profit_logs")
data class ProfitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val dateString: String, // "YYYY-MM-DD"
    val type: String, // "DAILY", "WEEKLY", "MONTHLY"
    val profitPercent: Double,
    val profitAmount: Double,
    val tradeCount: Int
)

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts")
    fun getAllAlerts(): Flow<List<PriceAlert>>
    
    @Query("SELECT * FROM price_alerts WHERE isAlertActive = 1")
    suspend fun getActiveAlerts(): List<PriceAlert>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert)
    
    @Update
    suspend fun updateAlert(alert: PriceAlert)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)
}

@Dao
interface ScannedBreakoutDao {
    @Query("SELECT * FROM scanned_breakouts ORDER BY score DESC")
    fun getAllScannedBreakouts(): Flow<List<ScannedBreakout>>

    @Query("SELECT * FROM scanned_breakouts ORDER BY score DESC")
    suspend fun getAllScannedBreakoutsList(): List<ScannedBreakout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakouts(breakouts: List<ScannedBreakout>)

    @Query("DELETE FROM scanned_breakouts")
    suspend fun clearAll()
}

@Dao
interface VirtualTradeDao {
    @Query("SELECT * FROM virtual_trades ORDER BY entryTime DESC")
    fun getAllTradesFlow(): Flow<List<VirtualTrade>>

    @Query("SELECT * FROM virtual_trades ORDER BY entryTime DESC")
    suspend fun getAllTradesList(): List<VirtualTrade>

    @Query("SELECT * FROM virtual_trades WHERE status = 'ACTIVE'")
    suspend fun getActiveTrades(): List<VirtualTrade>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: VirtualTrade): Long

    @Update
    suspend fun updateTrade(trade: VirtualTrade)

    @Query("DELETE FROM virtual_trades")
    suspend fun clearAllTrades()
}

@Dao
interface ProfitLogDao {
    @Query("SELECT * FROM profit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<ProfitLog>>

    @Query("SELECT * FROM profit_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<ProfitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ProfitLog)

    @Query("DELETE FROM profit_logs")
    suspend fun clearAllLogs()
}

@Database(entities = [PriceAlert::class, ScannedBreakout::class, VirtualTrade::class, ProfitLog::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun scannedBreakoutDao(): ScannedBreakoutDao
    abstract fun virtualTradeDao(): VirtualTradeDao
    abstract fun profitLogDao(): ProfitLogDao
}

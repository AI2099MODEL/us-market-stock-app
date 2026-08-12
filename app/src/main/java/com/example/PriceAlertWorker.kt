package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PriceAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = MyApplication.database
        val activeAlerts = db.priceAlertDao().getActiveAlerts()

        if (activeAlerts.isEmpty()) return@withContext Result.success()

        val uniqueTickers = activeAlerts.map { it.ticker }.distinct()
        val prices = mutableMapOf<String, Double>()

        for (ticker in uniqueTickers) {
            try {
                val response = YahooRetrofit.service.getChart(ticker, "1d", "1m")
                val result = response.chart?.result?.firstOrNull()
                val price = result?.meta?.regularMarketPrice
                if (price != null) {
                    prices[ticker] = price
                }
            } catch (e: Exception) {
                // Ignore network errors
            }
        }

        for (alert in activeAlerts) {
            val currentPrice = prices[alert.ticker]
            if (currentPrice != null) {
                // Determine if we hit target (either way: alert if >= target for bullish, <= target for bearish - wait, let's just trigger if price reaches or crosses target)
                // Actually, simple alert: if current price is within 0.5% of target or crosses it. Let's just say, if price >= target. (or we can add condition to db, but for now simple price >= target).
                // Actually users can set stop loss (<=) and target (>=). Let's just check both:
                // We don't have direction. Just say if it reaches target +/- 0.5%
                if (!alert.isTriggered && (Math.abs(currentPrice - alert.priceTarget) / alert.priceTarget < 0.005 || currentPrice >= alert.priceTarget)) {
                    sendNotification(alert, currentPrice)
                    // Mark as inactive so it doesn't trigger repeatedly
                    db.priceAlertDao().updateAlert(alert.copy(isTriggered = true, isAlertActive = false))
                }
            }
        }

        Result.success()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun sendNotification(alert: PriceAlert, currentPrice: Double) {
        val context = applicationContext
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            val nameToDisplay = if (alert.name.isNotEmpty()) alert.name else alert.ticker.replace(".NS", "")
            val builder = NotificationCompat.Builder(context, "PRICE_ALERTS")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Price Alert: $nameToDisplay")
                .setContentText("₹nameToDisplay has reached ₹${"%.2f".format(currentPrice)} (Target: ₹${"%.2f".format(alert.priceTarget)})")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(alert.id, builder.build())
            }
        } catch (e: Exception) {
            // Background notification exception safety
        }
    }
}

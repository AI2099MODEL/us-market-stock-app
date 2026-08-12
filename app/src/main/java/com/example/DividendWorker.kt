package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DividendWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val today = Calendar.getInstance()
        today.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tomorrowDateString = tomorrowDateFormat.format(today.time)

        MASTER_DIVIDEND_LIST.forEach { dividend ->
            if (dividend.exDate == tomorrowDateString) {
                sendNotification(
                    dividend.hashCode(),
                    "Upcoming Ex-Dividend Tomorrow",
                    "${dividend.companyName} (${dividend.symbol}) goes ex-dividend tomorrow. Amount: ₹${dividend.amountPerShare}"
                )
            }
            val payoutDateStr = getPayoutDate(dividend.exDate)
            if (payoutDateStr == tomorrowDateString) {
                sendNotification(
                    dividend.hashCode() + 2,
                    "Upcoming Dividend Payout Tomorrow",
                    "${dividend.companyName} (${dividend.symbol}) dividend payout is tomorrow. Amount: ₹${dividend.amountPerShare}"
                )
            }
        }
        
        Result.success()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun sendNotification(id: Int, title: String, message: String) {
        val context = applicationContext
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, "DIVIDEND_ALERTS")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        } catch (e: Exception) {
            // Background notification exception safety
        }
    }
}

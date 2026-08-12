package com.example

import java.util.Calendar
import java.util.TimeZone

object MarketUtils {
    /**
     * Checks whether the Indian stock market (NSE/BSE) is currently open for live trading.
     * Market hours: Monday to Friday, 09:15 AM IST to 03:30 PM IST (555 to 930 minutes).
     */
    fun isMarketOpen(): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute
        return timeInMinutes in 555..930
    }

    fun getMarketStatusText(): String {
        return if (isMarketOpen()) "LIVE MARKET" else "MARKET CLOSED"
    }
}

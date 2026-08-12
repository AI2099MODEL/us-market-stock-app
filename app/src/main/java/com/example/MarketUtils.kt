package com.example

import java.util.Calendar
import java.util.TimeZone

object MarketUtils {
    /**
     * Checks whether the Indian Commodity Market (MCX) is currently open for live trading.
     * Market hours: Monday to Friday, 09:00 AM IST to 11:30 PM IST (540 to 1410 minutes).
     */
    fun isMarketOpen(): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        if (isMarketHoliday(cal)) {
            return false
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute
        // MCX hours: 09:00 AM to 11:30 PM (540 to 1410 minutes)
        return timeInMinutes in 540..1410
    }

    fun isMarketHoliday(cal: Calendar): Boolean {
        val month = cal.get(Calendar.MONTH) // 0-indexed (0 is Jan, 11 is Dec)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        
        // Indian Market Holidays (Fixed & Major Festivals approximation for 2026)
        if (month == Calendar.JANUARY && day == 26) return true // Republic Day
        if (month == Calendar.AUGUST && day == 15) return true // Independence Day
        if (month == Calendar.OCTOBER && day == 2) return true // Gandhi Jayanti
        if (month == Calendar.MAY && day == 1) return true // Maharashtra Day / May Day
        if (month == Calendar.DECEMBER && day == 25) return true // Christmas
        if (month == Calendar.NOVEMBER && day == 8) return true // Diwali (approx)
        if (month == Calendar.MARCH && day == 30) return true // Holi (approx)
        
        return false
    }

    fun getMarketStatusText(): String {
        return if (isMarketOpen()) "MCX LIVE" else "MCX CLOSED"
    }
}


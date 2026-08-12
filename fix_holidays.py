import os
import re

filepath = 'app/src/main/java/com/example/MarketUtils.kt'
with open(filepath, 'r') as f:
    content = f.read()

# Replace the holiday section
replacement = """    fun isMarketHoliday(cal: Calendar): Boolean {
        val month = cal.get(Calendar.MONTH) // 0-indexed (0 is Jan, 11 is Dec)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        
        // US Market Holidays (Standard Fixed)
        if (month == Calendar.JANUARY && day == 1) return true // New Year's Day
        if (month == Calendar.JUNE && day == 19) return true // Juneteenth
        if (month == Calendar.JULY && day == 4) return true // Independence Day
        if (month == Calendar.DECEMBER && day == 25) return true // Christmas Day
        
        // Example dynamic/observed holidays for 2026 (approximation)
        if (month == Calendar.JANUARY && day == 19) return true // Martin Luther King Jr. Day
        if (month == Calendar.FEBRUARY && day == 16) return true // Washington's Birthday
        if (month == Calendar.APRIL && day == 3) return true // Good Friday
        if (month == Calendar.MAY && day == 25) return true // Memorial Day
        if (month == Calendar.JULY && day == 3) return true // Independence Day (Observed)
        if (month == Calendar.SEPTEMBER && day == 7) return true // Labor Day
        if (month == Calendar.NOVEMBER && day == 26) return true // Thanksgiving Day
        
        return false
    }"""

# Extract everything before `fun isMarketHoliday` and after `fun getMarketStatusText`
start_idx = content.find('fun isMarketHoliday')
end_idx = content.find('fun getMarketStatusText')

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + replacement + "\n\n    " + content[end_idx:]
    with open(filepath, 'w') as f:
        f.write(new_content)

import re

with open("app/src/main/java/com/example/LiveDividendManager.kt", "r") as f:
    code = f.read()

new_logic = """
    suspend fun fetchLiveDividendsFromInternet(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastSync = lastSyncTimestamp.value
        // Only fetch if forced, or if it's been more than 4 hours since the last successful sync
        if (!force && (now - lastSync) < 4 * 60 * 60 * 1000L && liveDividends.value.isNotEmpty()) {
            isLoading.value = false
            return@withContext
        }

        isLoading.value = true
        syncStatusMessage.value = "Scanning Internet for NSE Corporate Announcements..."
"""

code = code.replace("suspend fun fetchLiveDividendsFromInternet(context: Context) = withContext(Dispatchers.IO) {", new_logic.strip())

with open("app/src/main/java/com/example/LiveDividendManager.kt", "w") as f:
    f.write(code)


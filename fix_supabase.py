import re

with open("app/src/main/java/com/example/SupabaseService.kt", "r") as f:
    code = f.read()

code = code.replace(
    """    fun initialize(context: Context, customUrl: String? = null, customKey: String? = null) {
        val prefs = context.getSharedPreferences("supabase_config", Context.MODE_PRIVATE)
        val url = customUrl ?: prefs.getString("supabase_url", supabaseUrl) ?: supabaseUrl
        val key = customKey ?: prefs.getString("supabase_key", supabaseAnonKey) ?: supabaseAnonKey""",
    """    fun initialize(context: Context, customUrl: String? = null, customKey: String? = null) {
        val prefs = context.getSharedPreferences("supabase_config", Context.MODE_PRIVATE)
        val envUrl = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
        val envKey = try { BuildConfig.SUPABASE_KEY } catch (e: Exception) { "" }
        val finalEnvUrl = if (!envUrl.isNullOrBlank() && envUrl != "MY_SUPABASE_URL") envUrl else supabaseUrl
        val finalEnvKey = if (!envKey.isNullOrBlank() && envKey != "MY_SUPABASE_KEY") envKey else supabaseAnonKey
        val url = customUrl ?: prefs.getString("supabase_url", finalEnvUrl) ?: finalEnvUrl
        val key = customKey ?: prefs.getString("supabase_key", finalEnvKey) ?: finalEnvKey"""
)

with open("app/src/main/java/com/example/SupabaseService.kt", "w") as f:
    f.write(code)

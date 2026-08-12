import java.net.URL
import java.net.HttpURLConnection

fun main() {
    val url = URL("https://api.dhan.co/v2/marketfeed/quote/gold")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    try {
        println(conn.responseCode)
    } catch(e: Exception) {
        println(e)
    }
}

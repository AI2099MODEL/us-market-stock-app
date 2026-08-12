import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main() {
    // Simulated Dhan 8-byte Header + 44 byte Payload
    val buffer = ByteBuffer.allocate(52)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    // Header
    buffer.putShort(52.toShort()) // Length
    buffer.put(2.toByte()) // Feed Response Code (Ticker)
    buffer.put(0.toByte()) // Exchange Segment
    buffer.putInt(12345) // Security ID
    
    // Payload (LTP is at some offset)
    // Let's pretend LTP is at offset 8 (right after header)
    buffer.putFloat(75432.50f)
    
    buffer.flip()
    
    val len = buffer.short
    val code = buffer.get()
    val segment = buffer.get()
    val secId = buffer.int
    val ltp = buffer.float
    
    println("Len: $len, Code: $code, LTP: $ltp")
}

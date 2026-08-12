package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShoonyaWebSocketDebugCard(modifier: Modifier = Modifier) {
    val status by ShoonyaWebSocketManager.connectionStatus.collectAsState()
    val tickCount by ShoonyaWebSocketManager.tickCount.collectAsState()
    val lastHeartbeat by ShoonyaWebSocketManager.lastHeartbeat.collectAsState()
    val liveQuotes by ShoonyaWebSocketManager.liveQuotes.collectAsState()

    var isExpanded by remember { mutableStateOf(true) }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val heartbeatStr = timeFormat.format(Date(lastHeartbeat))

    val isConnected = status.contains("CONNECTED") || status.contains("STREAMING")
    val statusColor = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = "Shoonya WebSocket Debugger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            ShoonyaWebSocketManager.stop()
                            ShoonyaWebSocketManager.start()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart WebSocket",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(2.dp))

                    DebugRow(label = "Connection Status", value = status, valueColor = statusColor)
                    DebugRow(label = "Total Ticks Streamed", value = tickCount.toString())
                    DebugRow(label = "Last Heartbeat", value = heartbeatStr, fontFamily = FontFamily.Monospace)
                    DebugRow(label = "Active Symbols Tracked", value = "${liveQuotes.size} MCX Instruments")

                    if (liveQuotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sample Tick: ${liveQuotes.entries.firstOrNull()?.key} @ ₹${String.format("%.2f", liveQuotes.entries.firstOrNull()?.value?.price ?: 0.0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    fontFamily: FontFamily = FontFamily.Default
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontFamily = fontFamily
        )
    }
}

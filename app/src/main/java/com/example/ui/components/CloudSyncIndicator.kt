package com.example.ui.components

import android.content.Context
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SyncState
import com.example.ui.WorkLogViewModel
import com.example.ui.theme.SarabunFontFamily
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CloudSyncIndicator(
    viewModel: WorkLogViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()
    val simulateError by viewModel.simulateSyncError.collectAsState()

    var showDetailDialog by remember { mutableStateOf(false) }

    // Spin animation for retrieving/syncing
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_angle"
    )

    // Pulsing animation for uploading or error
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sync_pulse"
    )

    // Visual configurations based on state
    val config = when (syncState) {
        SyncState.SYNCED -> SyncUIConfig(
            icon = Icons.Default.CloudDone,
            color = Color(0xFF10B981), // Emerald green
            textTh = "เชื่อมต่อแล้ว",
            textEn = "Synced",
            pulsing = false,
            spinning = false
        )
        SyncState.UPLOADING -> SyncUIConfig(
            icon = Icons.Default.CloudUpload,
            color = Color(0xFFF59E0B), // Amber orange
            textTh = "กำลังอัปโหลด...",
            textEn = "Uploading...",
            pulsing = true,
            spinning = false
        )
        SyncState.RETRIEVING -> SyncUIConfig(
            icon = Icons.Default.Refresh,
            color = Color(0xFF3B82F6), // Blue
            textTh = "กำลังดึงข้อมูล...",
            textEn = "Retrieving...",
            pulsing = false,
            spinning = true
        )
        SyncState.ERROR -> SyncUIConfig(
            icon = Icons.Default.CloudOff,
            color = Color(0xFFEF4444), // Red
            textTh = "การเชื่อมต่อขัดข้อง",
            textEn = "Sync Error",
            pulsing = true,
            spinning = false
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDetailDialog = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("cloud_sync_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp)
        ) {
            // Pulsing background for active actions
            if (config.pulsing) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(config.color.copy(alpha = 0.25f * pulseAlpha))
                )
            }
            Icon(
                imageVector = config.icon,
                contentDescription = config.textTh,
                tint = config.color,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (config.spinning) angle else 0f)
            )
        }

        // Small indicator text
        Text(
            text = config.textTh,
            color = config.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SarabunFontFamily
        )
    }

    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            icon = {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    tint = config.color,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(if (config.spinning) angle else 0f)
                )
            },
            title = {
                Text(
                    text = "สถานะคลาวด์ซิงค์ (Cloud Sync)",
                    fontWeight = FontWeight.Bold,
                    fontFamily = SarabunFontFamily,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Status row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(config.color.copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(config.color)
                        )
                        Column {
                            Text(
                                text = config.textTh,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = config.color,
                                fontFamily = SarabunFontFamily
                            )
                            Text(
                                text = when (syncState) {
                                    SyncState.SYNCED -> "ข้อมูลทั้งหมดถูกบันทึกบน Cloud เรียบร้อย"
                                    SyncState.UPLOADING -> "กำลังอัปโหลดการเปลี่ยนแปลงล่าสุด..."
                                    SyncState.RETRIEVING -> "กำลังตรวจสอบและอัปเดตฐานข้อมูล..."
                                    SyncState.ERROR -> "การเชื่อมต่อล้มเหลว กรุณาตรวจสอบการตั้งค่า"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }

                    // Last Synced Time
                    val formattedTime = remember(lastSyncedTime) {
                        val sdf = SimpleDateFormat("HH:mm:ss น. (dd/MM/yyyy)", Locale("th", "TH"))
                        sdf.format(Date(lastSyncedTime))
                    }
                    val relativeTime = remember(lastSyncedTime) {
                        DateUtils.getRelativeTimeSpanString(
                            lastSyncedTime,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ข้อมูลอัปเดตล่าสุด:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SarabunFontFamily
                        )
                        Text(
                            text = "$formattedTime ($relativeTime)",
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SarabunFontFamily
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Error Simulation Notice if enabled
                    if (simulateError) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "เปิดโหมดจำลองปัญหาเน็ตขัดข้องอยู่",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.syncWithCloud()
                        Toast.makeText(context, "เริ่มรีเฟรชการซิงค์...", Toast.LENGTH_SHORT).show()
                    },
                    enabled = syncState != SyncState.UPLOADING && syncState != SyncState.RETRIEVING,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ซิงค์ด่วน (Sync Now)",
                        fontFamily = SarabunFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = false }) {
                    Text(
                        text = "ปิด",
                        fontFamily = SarabunFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

private data class SyncUIConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val textTh: String,
    val textEn: String,
    val pulsing: Boolean,
    val spinning: Boolean
)

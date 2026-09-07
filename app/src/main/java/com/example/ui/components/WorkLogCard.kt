package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.WorkLog
import com.example.ui.theme.BentoBlueBadgeBg
import com.example.ui.theme.BentoBlueBadgeText
import com.example.ui.theme.BentoBorderSubtle
import com.example.ui.theme.BentoGreenBadgeBg
import com.example.ui.theme.BentoGreenBadgeText
import com.example.ui.theme.BentoOrangeBadgeBg
import com.example.ui.theme.BentoOrangeBadgeText
import com.example.ui.theme.BentoPinkBadgeBg
import com.example.ui.theme.BentoPinkBadgeText
import com.example.ui.theme.BentoPurpleBadgeBg
import com.example.ui.theme.BentoPurpleBadgeText
import com.example.ui.theme.StatusClosedLight
import com.example.ui.theme.StatusOpenLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Event
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.ai.CustomerInfoExtractor
import com.example.ui.theme.SarabunFontFamily

import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import android.widget.Toast
import com.example.util.ImageExportUtils
import com.example.util.SnUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PriorityStyle(
    val bg: Color,
    val text: Color,
    val borderColor: Color,
    val label: String
)

fun getPriorityStyle(priority: String): PriorityStyle {
    return when (priority) {
        WorkLog.PRIORITY_HIGH -> PriorityStyle(
            bg = Color(0xFF451A1A),
            text = Color(0xFFEF4444),
            borderColor = Color(0xFFEF4444),
            label = "🔥 High"
        )
        WorkLog.PRIORITY_LOW -> PriorityStyle(
            bg = Color(0xFF1E293B),
            text = Color(0xFF60A5FA),
            borderColor = Color(0xFF3B82F6),
            label = "🔹 Low"
        )
        else -> PriorityStyle(
            bg = Color(0xFF3A2E14),
            text = Color(0xFFF59E0B),
            borderColor = Color(0xFFF59E0B),
            label = "⚡ Medium"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkLogCard(
    workLog: WorkLog,
    onCardClick: () -> Unit,
    onCloseCaseClick: () -> Unit,
    onCheckInClick: ((WorkLog) -> Unit)? = null,
    onCheckOutClick: ((WorkLog) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isClosed = workLog.status == WorkLog.STATUS_CLOSED
    val extractedInfo = remember(workLog.rawText) { CustomerInfoExtractor.extract(workLog.rawText) }

    val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm น.", Locale("th", "TH")) }
    val formattedTime = dateFormat.format(Date(workLog.timestamp))

    val (badgeBg, badgeText, categoryIcon) = getBentoCategoryStyle(workLog.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCardClick() }
            .testTag("work_log_card_${workLog.id}"),
        colors = CardDefaults.cardColors(
            containerColor = com.example.ui.theme.BentoSurfaceDark
        ),
        border = BorderStroke(
            1.dp,
            Color(0xFF374151)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Bento Badge & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left FlowRow: Category Badge & Hashtag Case Tag
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bento Badge Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = badgeText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = workLog.category.uppercase(),
                            color = badgeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Hashtag Case Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2D3748))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = workLog.formattedCaseNumber,
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Cloud Sync Indicator Tag
                    val hasAttachments = workLog.imageUriList.isNotEmpty()
                    when (workLog.syncStatus) {
                        WorkLog.SYNC_SYNCED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF064E3B).copy(alpha = 0.6f))
                                    .border(BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.7f)), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("cloud_synced_badge_${workLog.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = if (hasAttachments) "รูปแนบและข้อมูลซิงค์ขึ้น Google Drive แล้ว" else "ข้อมูลซิงค์ขึ้น Cloud แล้ว",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (hasAttachments) "Drive Synced" else "Synced",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = SarabunFontFamily,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        WorkLog.SYNC_PENDING -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1F2937).copy(alpha = 0.6f))
                                    .border(BorderStroke(0.5.dp, Color(0xFF6B7280).copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("cloud_pending_badge_${workLog.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "รอซิงค์ข้อมูล",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Pending",
                                    color = Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    fontFamily = SarabunFontFamily,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        WorkLog.SYNC_FAILED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.5f))
                                    .border(BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("cloud_failed_badge_${workLog.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "ซิงค์ไม่สำเร็จ",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Error",
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = SarabunFontFamily,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Raw Text Snippet
            Text(
                text = workLog.cleanRawText.ifBlank { workLog.rawText ?: "" }.ifBlank { "(ไม่มีข้อความ)" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Extracted Info Quick Action Chips (Phone & Location)
            if (extractedInfo.phone != null || extractedInfo.buildingOrAddress != null || extractedInfo.customerName != null || extractedInfo.onsiteTime != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    extractedInfo.customerName?.let { name ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp)
                            )
                        }
                    }

                    extractedInfo.phone?.let { phoneNum ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                .clickable { CustomerInfoExtractor.openDialer(context, phoneNum) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "โทร $phoneNum",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phoneNum,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontFamily = SarabunFontFamily,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    extractedInfo.buildingOrAddress?.let { address ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1976D2).copy(alpha = 0.15f))
                                .clickable { CustomerInfoExtractor.openGoogleMaps(context, address) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "แผนที่ $address",
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "แผนที่",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2),
                                    fontFamily = SarabunFontFamily,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    extractedInfo.onsiteTime?.let { onsiteTime ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE65100).copy(alpha = 0.15f))
                                .clickable {
                                    CustomerInfoExtractor.openCalendarEvent(
                                        context = context,
                                        customerName = extractedInfo.customerName,
                                        location = extractedInfo.buildingOrAddress,
                                        rawText = workLog.rawText,
                                        onsiteTimeStr = onsiteTime,
                                        customDateTimestamp = workLog.timestamp
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = "เวลานัด $onsiteTime",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = onsiteTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    fontFamily = SarabunFontFamily,
                                    lineHeight = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Check IN & Check OUT Status Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (workLog.isCheckedIn) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF064E3B))
                                .border(BorderStroke(0.5.dp, Color(0xFF10B981)), RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Check IN",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "In: ${workLog.formattedCheckInTime}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    fontFamily = SarabunFontFamily
                                )
                            }
                        }
                    } else if (!isClosed) {
                        Button(
                            onClick = { onCheckInClick?.invoke(workLog) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp).testTag("btn_card_checkin_${workLog.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Check IN หน้างาน", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                        }
                    }

                    if (workLog.isCheckedOut) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF78350F))
                                .border(BorderStroke(0.5.dp, Color(0xFFF59E0B)), RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Check OUT",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Out: ${workLog.formattedCheckOutTime}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = SarabunFontFamily
                                )
                            }
                        }
                    } else if (workLog.isCheckedIn && !isClosed) {
                        Button(
                            onClick = { onCheckOutClick?.invoke(workLog) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp).testTag("btn_card_checkout_${workLog.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Check OUT หน้างาน", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                        }
                    } else if (!workLog.isCheckedIn && !workLog.isCheckedOut && isClosed) {
                        Text(
                            text = "ไม่ได้บันทึก Check IN/OUT",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            fontFamily = SarabunFontFamily
                        )
                    }
                }
            }

            // Closed Details Snippet if Closed (Highlighting Solutions = )
            if (isClosed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val snInfo = remember(workLog.solutions) { SnUtils.extractSerialNumbers(workLog.solutions) }
                    val cleanSolutions = remember(workLog.solutions) { SnUtils.cleanSolutionsText(workLog.solutions) }
                    
                    if (cleanSolutions.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Solutions = $cleanSolutions",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (snInfo.hasSerialNumbers) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            snInfo.oldSn?.let { old ->
                                if (old.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFEF2F2))
                                            .border(BorderStroke(0.5.dp, Color(0xFFFCA5A5)), RoundedCornerShape(4.dp))
                                            .clickable { SnUtils.copySnToClipboard(context, "S/N เก่า", old) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "S/N เก่า: $old",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "คัดลอก",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }

                            snInfo.newSn?.let { new ->
                                if (new.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFECFDF5))
                                            .border(BorderStroke(0.5.dp, Color(0xFF6EE7B7)), RoundedCornerShape(4.dp))
                                            .clickable { SnUtils.copySnToClipboard(context, "S/N ใหม่", new) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "S/N ใหม่: $new",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "คัดลอก",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (workLog.technician.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ช่าง = ${workLog.technician}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val images = workLog.imageUriList
                    if (images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📷 รูปหลักฐาน (${images.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SarabunFontFamily
                            )
                            if (workLog.syncStatus == WorkLog.SYNC_SYNCED) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF064E3B).copy(alpha = 0.8f))
                                        .border(BorderStroke(0.5.dp, Color(0xFF10B981)), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "รูปซิงค์ลง Google Drive เรียบร้อย",
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Drive Synced",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        fontFamily = SarabunFontFamily
                                    )
                                }
                            }
                        }
                        if (images.size == 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                            ) {
                                AsyncImage(
                                    model = images.first(),
                                    contentDescription = "ภาพหลักฐานงาน",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(images) { imgUri ->
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        AsyncImage(
                                            model = imgUri,
                                            contentDescription = "ภาพหลักฐานงาน",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer Row: Status Indicator Dot & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Indicator Dot with Label
                val statusColor = if (isClosed) {
                    if (isSystemInDarkTheme()) Color(0xFF8AF062) else Color(0xFF2E7D32)
                } else {
                    Color(0xFFD65151)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = workLog.status,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = statusColor
                    )
                }

                if (!isClosed) {
                    Button(
                        onClick = onCloseCaseClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.DarkOrangeAccent,
                            contentColor = Color(0xFF111827)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("card_close_case_btn_${workLog.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ปิดเคส", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontFamily = SarabunFontFamily)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var isCardSavingImage by remember { mutableStateOf(false) }
                        val cardScope = rememberCoroutineScope()

                        OutlinedButton(
                            onClick = {
                                if (!isCardSavingImage) {
                                    isCardSavingImage = true
                                    Toast.makeText(context, "กำลังสร้างรูปภาพสรุปงาน...", Toast.LENGTH_SHORT).show()
                                    cardScope.launch(Dispatchers.Default) {
                                        try {
                                            val snInfo = SnUtils.extractSerialNumbers(workLog.solutions + "\n" + workLog.rawText)
                                            val summaryBitmap = ImageExportUtils.generateSummaryBitmap(
                                                context = context,
                                                workLog = workLog,
                                                solutions = workLog.solutions,
                                                oldSn = snInfo.oldSn ?: "",
                                                newSn = snInfo.newSn ?: "",
                                                technician = workLog.technician
                                            )
                                            try {
                                                val savedUri = ImageExportUtils.saveBitmapToGallery(
                                                    context = context,
                                                    bitmap = summaryBitmap,
                                                    title = "CaseSummary_${workLog.formattedCaseNumber}_${workLog.id}"
                                                )
                                                withContext(Dispatchers.Main) {
                                                    if (savedUri != null) {
                                                        Toast.makeText(context, "บันทึกภาพสรุปปิดเคสลงแกลเลอรี่สำเร็จ! 🎉", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, "ล้มเหลวในการบันทึกภาพ", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } finally {
                                                summaryBitmap.recycle()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } finally {
                                            isCardSavingImage = false
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF38BDF8)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("card_save_summary_btn_${workLog.id}")
                        ) {
                            if (isCardSavingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "บันทึกภาพสรุป",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "บันทึกภาพสรุป",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = onCardClick,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF374151)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("ดูสรุป", fontSize = 11.sp, color = com.example.ui.theme.DarkOrangeAccent, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = com.example.ui.theme.DarkOrangeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactWorkLogCard(
    workLog: WorkLog,
    onCardClick: () -> Unit,
    onCloseCaseClick: () -> Unit,
    onCheckInClick: ((WorkLog) -> Unit)? = null,
    onCheckOutClick: ((WorkLog) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isClosed = workLog.status == WorkLog.STATUS_CLOSED
    val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale("th", "TH")) }
    val formattedTime = dateFormat.format(Date(workLog.timestamp))
    val (badgeBg, badgeText, categoryIcon) = getBentoCategoryStyle(workLog.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCardClick() }
            .testTag("compact_work_log_card_${workLog.id}"),
        colors = CardDefaults.cardColors(
            containerColor = com.example.ui.theme.BentoSurfaceDark
        ),
        border = BorderStroke(
            1.dp,
            if (isClosed) Color(0xFF374151)
            else com.example.ui.theme.DarkOrangeAccent.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Case Number & Status Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workLog.formattedCaseNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = com.example.ui.theme.DarkOrangeAccent,
                    fontFamily = SarabunFontFamily,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (workLog.syncStatus == WorkLog.SYNC_SYNCED) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF064E3B).copy(alpha = 0.8f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Synced to Drive/Cloud",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = if (workLog.imageUriList.isNotEmpty()) "Drive" else "Synced",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    fontFamily = SarabunFontFamily,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else if (workLog.syncStatus == WorkLog.SYNC_FAILED) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Sync Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isClosed) Color(0xFF1E293B) else Color(0xFF7F1D1D))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isClosed) "ปิดแล้ว" else "เปิดอยู่",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isClosed) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontFamily = SarabunFontFamily,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Category Badge Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = badgeText,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = workLog.category,
                    color = badgeText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    fontFamily = SarabunFontFamily
                )
            }

            // Raw Text Preview (Max 2 lines)
            Text(
                text = workLog.cleanRawText.ifBlank { workLog.rawText ?: "" }.ifBlank { "(ไม่มีข้อความ)" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                fontFamily = SarabunFontFamily,
                modifier = Modifier.heightIn(min = 32.dp)
            )

            // Solution or Technician if available
            if (isClosed && workLog.solutions.isNotBlank()) {
                Text(
                    text = "แก้: ${workLog.solutions}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = SarabunFontFamily
                )
            }

            // Check IN / Check OUT Compact Action Buttons
            if (!isClosed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!workLog.isCheckedIn) {
                        Button(
                            onClick = { onCheckInClick?.invoke(workLog) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(26.dp)
                        ) {
                            Text("Check IN", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                        }
                    } else if (!workLog.isCheckedOut) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF064E3B))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("In: ${workLog.formattedCheckInTime}", fontSize = 9.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onCheckOutClick?.invoke(workLog) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.weight(1f).height(26.dp)
                        ) {
                            Text("Check OUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF78350F))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Out: ${workLog.formattedCheckOutTime}", fontSize = 9.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Timestamp Row
            Text(
                text = formattedTime,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                fontFamily = SarabunFontFamily
            )

            // Action Button
            if (!isClosed) {
                Button(
                    onClick = onCloseCaseClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.DarkOrangeAccent,
                        contentColor = Color(0xFF111827)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("compact_close_case_btn_${workLog.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("ปิดเคส", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontFamily = SarabunFontFamily)
                }
            } else {
                OutlinedButton(
                    onClick = onCardClick,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF374151)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    Text("รายละเอียด", fontSize = 10.sp, color = com.example.ui.theme.DarkOrangeAccent, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                }
            }
        }
    }
}

private fun getBentoCategoryStyle(category: String): Triple<Color, Color, ImageVector> {
    return when (category) {
        "ซ่อมบำรุง" -> Triple(BentoPinkBadgeBg, BentoPinkBadgeText, Icons.Default.Build)
        "ติดตั้ง" -> Triple(BentoBlueBadgeBg, BentoBlueBadgeText, Icons.Default.Build)
        "ส่งสินค้า" -> Triple(BentoOrangeBadgeBg, BentoOrangeBadgeText, Icons.Default.DirectionsBus)
        "บริการลูกค้า" -> Triple(BentoGreenBadgeBg, BentoGreenBadgeText, Icons.Default.SupportAgent)
        else -> Triple(BentoPurpleBadgeBg, BentoPurpleBadgeText, Icons.Default.Work)
    }
}

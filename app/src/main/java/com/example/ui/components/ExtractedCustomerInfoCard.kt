package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.CustomerInfoExtractor
import com.example.ui.theme.SarabunFontFamily

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun ExtractedCustomerInfoCard(
    rawText: String,
    modifier: Modifier = Modifier,
    title: String = "ข้อมูลติดต่อและสถานที่ที่ตรวจพบ",
    customDateTimestamp: Long? = null,
    onManualInfoEdited: ((name: String, phone: String, location: String, onsiteTime: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val extractedInfo = remember(rawText) { CustomerInfoExtractor.extract(rawText) }

    var isEditing by remember { mutableStateOf(false) }
    var editedNameState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var editedPhoneState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var editedLocationState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var editedOnsiteTimeState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    var manualOverrideInfo by remember(rawText) { mutableStateOf<com.example.ai.ExtractedCustomerInfo?>(null) }
    val info = manualOverrideInfo ?: extractedInfo

    LaunchedEffect(isEditing) {
        if (isEditing) {
            editedNameState = androidx.compose.ui.text.input.TextFieldValue(info.customerName ?: "")
            editedPhoneState = androidx.compose.ui.text.input.TextFieldValue(info.phone ?: "")
            editedLocationState = androidx.compose.ui.text.input.TextFieldValue(info.buildingOrAddress ?: "")
            editedOnsiteTimeState = androidx.compose.ui.text.input.TextFieldValue(info.onsiteTime ?: "")
        }
    }

    // Always show if onManualInfoEdited is present or if content exists
    val hasContent = info.customerName != null || info.phone != null || info.buildingOrAddress != null || info.onsiteTime != null || onManualInfoEdited != null

    if (!hasContent) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = SarabunFontFamily,
                            lineHeight = 20.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Edit Toggle Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { isEditing = !isEditing }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "แก้ไขข้อมูลติดต่อ",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEditing) "ยกเลิก" else "แก้ไขข้อมูลเอง",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = SarabunFontFamily,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (isEditing) {
                // Manual Edit Input Form
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    OutlinedTextField(
                        value = editedNameState,
                        onValueChange = { editedNameState = com.example.util.ThaiTextFilter.processThaiBackspace(editedNameState, it) },
                        label = { Text("ชื่อลูกค้า / ผู้ติดต่อ", fontFamily = SarabunFontFamily, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            autoCorrect = false
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedPhoneState,
                        onValueChange = { editedPhoneState = com.example.util.ThaiTextFilter.processThaiBackspace(editedPhoneState, it) },
                        label = { Text("เบอร์โทรศัพท์", fontFamily = SarabunFontFamily, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                            autoCorrect = false
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedLocationState,
                        onValueChange = { editedLocationState = com.example.util.ThaiTextFilter.processThaiBackspace(editedLocationState, it) },
                        label = { Text("ตำแหน่ง / อาคาร / สถานที่", fontFamily = SarabunFontFamily, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            autoCorrect = false
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedOnsiteTimeState,
                        onValueChange = { editedOnsiteTimeState = com.example.util.ThaiTextFilter.processThaiBackspace(editedOnsiteTimeState, it) },
                        label = { Text("เวลานัดหมาย / เข้า Onsite", fontFamily = SarabunFontFamily, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            autoCorrect = false
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val name = editedNameState.text
                                val phone = editedPhoneState.text
                                val loc = editedLocationState.text
                                val time = editedOnsiteTimeState.text
                                val newOverride = com.example.ai.ExtractedCustomerInfo(
                                    customerName = name.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    buildingOrAddress = loc.ifBlank { null },
                                    onsiteTime = time.ifBlank { null }
                                )
                                manualOverrideInfo = newOverride
                                onManualInfoEdited?.invoke(
                                    name.trim(),
                                    phone.trim(),
                                    loc.trim(),
                                    time.trim()
                                )
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("บันทึกการแก้ไข", fontFamily = SarabunFontFamily, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { isEditing = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ยกเลิก", fontFamily = SarabunFontFamily, fontSize = 12.sp)
                        }
                    }
                }
            } else {

            // Customer Name Item
            info.customerName?.let { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(end = 6.dp)) {
                            Text(
                                text = "ชื่อลูกค้า",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { CustomerInfoExtractor.copyToClipboard(context, "ชื่อลูกค้า", name) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "คัดลอกชื่อ",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "คัดลอก",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }
                }
            }

            // Phone Number Item
            info.phone?.let { phoneNumber ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(end = 6.dp)) {
                            Text(
                                text = "เบอร์โทรศัพท์",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = phoneNumber,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { CustomerInfoExtractor.openDialer(context, phoneNumber) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Phone Dial Action Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.example.ui.theme.CustomAccentBlue.copy(alpha = 0.15f))
                                .clickable { CustomerInfoExtractor.openDialer(context, phoneNumber) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "โทรออก",
                                    modifier = Modifier.size(12.dp),
                                    tint = com.example.ui.theme.CustomAccentBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "โทรออก",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.CustomAccentBlue,
                                    fontFamily = SarabunFontFamily
                                )
                            }
                        }

                        // Copy Phone Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { CustomerInfoExtractor.copyToClipboard(context, "เบอร์โทร", phoneNumber) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "คัดลอกเบอร์โทร",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Building / Location / Address Item
            info.buildingOrAddress?.let { address ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(end = 6.dp)) {
                            Text(
                                text = "ตำแหน่ง / อาคาร / สถานที่",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = address,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { CustomerInfoExtractor.openGoogleMaps(context, address) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Google Maps Action Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.example.ui.theme.CustomAccentBlue.copy(alpha = 0.15f))
                                .clickable { CustomerInfoExtractor.openGoogleMaps(context, address) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Google Maps",
                                    modifier = Modifier.size(12.dp),
                                    tint = com.example.ui.theme.CustomAccentBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "แผนที่",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.CustomAccentBlue,
                                    fontFamily = SarabunFontFamily
                                )
                            }
                        }

                        // Copy Address Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { CustomerInfoExtractor.copyToClipboard(context, "สถานที่/ที่อยู่", address) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "คัดลอกสถานที่",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Onsite / Appointment Time Item
            info.onsiteTime?.let { onsiteTime ->
                val formattedDateStr = remember(customDateTimestamp) {
                    if (customDateTimestamp != null) {
                        val thaiLocale = java.util.Locale("th", "TH")
                        val df = java.text.SimpleDateFormat("EEEE ที่ d MMMM yyyy", thaiLocale)
                        df.format(java.util.Date(customDateTimestamp))
                    } else null
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(end = 6.dp)) {
                            Text(
                                text = "เวลาเข้า Onsite / นัดหมาย",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 16.sp
                            )
                            val displayTimeText = if (formattedDateStr != null) {
                                "$onsiteTime ($formattedDateStr)"
                            } else {
                                onsiteTime
                            }
                            Text(
                                text = displayTimeText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SarabunFontFamily,
                                lineHeight = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Confirm & Add to Calendar Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.example.ui.theme.CustomAccentBlue.copy(alpha = 0.15f))
                                .clickable {
                                    CustomerInfoExtractor.openCalendarEvent(
                                        context = context,
                                        customerName = info.customerName,
                                        location = info.buildingOrAddress,
                                        rawText = rawText,
                                        onsiteTimeStr = onsiteTime,
                                        customDateTimestamp = customDateTimestamp
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "บันทึกปฏิทิน",
                                    modifier = Modifier.size(12.dp),
                                    tint = com.example.ui.theme.CustomAccentBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "บันทึกปฏิทิน",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.CustomAccentBlue,
                                    fontFamily = SarabunFontFamily
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}


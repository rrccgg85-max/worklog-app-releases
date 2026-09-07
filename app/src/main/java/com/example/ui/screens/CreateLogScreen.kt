package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveCaseStatus
import com.example.ui.WorkLogViewModel
import com.example.ui.components.ExtractedCustomerInfoCard
import com.example.ui.components.OcrTextScannerDialog
import com.example.ui.components.QuickFabSpeedDial
import com.example.ui.theme.BentoBackgroundDark
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.DarkOrangeAccent
import com.example.ui.theme.DarkOrangeBorder
import com.example.ui.theme.SarabunFontFamily

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.components.TechGridBackground
import com.example.ui.theme.SciFiBackgroundDark
import com.example.ui.theme.SciFiBorderDark
import com.example.ui.theme.SciFiCyanAccent
import com.example.ui.theme.SciFiCyanGlow
import com.example.ui.theme.SciFiOrangeAccent
import com.example.ui.theme.SciFiOrangeBorder
import com.example.ui.theme.SciFiSurfaceDark
import com.example.ui.theme.SciFiSurfaceRecessed
import com.example.ui.theme.PromptFontFamily
import com.example.ui.theme.SarabunFontFamily
import com.example.data.Technician
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateLogScreen(
    viewModel: WorkLogViewModel,
    currentTechnician: Technician,
    onLogCreated: () -> Unit
) {
    val context = LocalContext.current
    var rawTextFieldState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    val rawText = rawTextFieldState.text
    var manualCategory by remember { mutableStateOf<String?>(null) }
    var selectedPriority by remember { mutableStateOf(com.example.data.WorkLog.PRIORITY_MEDIUM) }

    var selectedDateTime by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    var useCurrentTime by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val currentCaseStatus by viewModel.currentCaseStatus.collectAsState()

    var showOcrScannerInScreen by remember { mutableStateOf(false) }

    val attachedImageUris = remember { mutableStateListOf<String>() }

    // Multi-image Gallery Picker
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val str = uri.toString()
            if (str !in attachedImageUris) {
                attachedImageUris.add(str)
            }
        }
    }

    // Single-image Gallery Picker
    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val str = it.toString()
            if (str !in attachedImageUris) {
                attachedImageUris.add(str)
            }
        }
    }

    // Camera Capture Launcher
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraUri
        val file = tempCameraFile
        if (uri != null && (success || (file != null && file.exists() && file.length() > 0))) {
            val str = uri.toString()
            if (str !in attachedImageUris) {
                attachedImageUris.add(str)
            }
        }
    }

    val launchActualCamera: () -> Unit = {
        try {
            val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val photoFile = File(cacheDir, "create_case_${System.currentTimeMillis()}.jpg")
            if (!photoFile.exists()) {
                photoFile.createNewFile()
            }
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraFile = photoFile
            tempCameraUri = photoUri
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(context, "ไม่สามารถเปิดกล้องได้: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchActualCamera()
        } else {
            Toast.makeText(context, "ต้องการสิทธิ์การใช้งานกล้องเพื่อถ่ายภาพ", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchActualCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-update selectedDateTime if AI extracts a valid time from rawText
    val extractedInfo = remember(rawText) { com.example.ai.CustomerInfoExtractor.extract(rawText) }
    LaunchedEffect(extractedInfo.onsiteTime) {
        extractedInfo.onsiteTime?.let { onsiteTime ->
            val parsedMs = com.example.ai.CustomerInfoExtractor.parseBeginTimeMillis(onsiteTime)
            val newCal = java.util.Calendar.getInstance().apply {
                timeInMillis = parsedMs
            }
            selectedDateTime = newCal
            useCurrentTime = false
        }
    }

    // Voice Speech-to-Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!spokenText.isNullOrBlank()) {
                val newCombined = if (rawText.isBlank()) spokenText else "$rawText $spokenText"
                rawTextFieldState = androidx.compose.ui.text.input.TextFieldValue(
                    text = newCombined,
                    selection = androidx.compose.ui.text.TextRange(newCombined.length)
                )
            }
        }
    }

    val startVoiceInput = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "พูดบันทึกรายละเอียดงาน...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "อุปกรณ์ไม่รองรับการพิมพ์ด้วยเสียง", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(rawText) {
        if (rawText.length >= 4) {
            kotlinx.coroutines.delay(400)
            viewModel.analyzeTextForCategory(rawText)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TechGridBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .padding(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Screen Header Title (Reference style)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ลงบันทึกงานใหม่",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFamily = SarabunFontFamily
                    )
                    Text(
                        text = "บันทึกการทำงาน · เลือกหมวดหมู่ · ปิดเคส",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        fontFamily = SarabunFontFamily
                    )
                }



            // Section 0 Card: TECHNICIAN_ASSIGNMENT (Read-only Logged-in Technician)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                border = BorderStroke(1.dp, SciFiBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SciFiCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "▸  TECHNICIAN_ASSIGNMENT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SciFiCyanAccent,
                            fontFamily = PromptFontFamily,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SciFiSurfaceRecessed)
                            .border(BorderStroke(1.dp, SciFiBorderDark), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "ช่างผู้รับผิดชอบ: ${currentTechnician.name}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                            if (currentTechnician.id.isNotBlank() || currentTechnician.role.isNotBlank()) {
                                    val mappedRole = if (currentTechnician.role.isBlank() || currentTechnician.role == "ช่างบริการภาคสนาม") "IT Support Onsite" else currentTechnician.role
                                    Text(
                                        text = "$mappedRole · ID: ${currentTechnician.id.ifBlank { "N/A" }}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontFamily = SarabunFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Section 1 Card: INPUT_LOG (Matching Reference Image)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                border = BorderStroke(1.dp, SciFiBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section Header Label: ▸ INPUT_LOG with instant encryption badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▸  INPUT_CASE / INPUT_LOG",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SciFiCyanAccent,
                            fontFamily = PromptFontFamily,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SciFiCyanAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "เข้ารหัสทันที (AES-256)",
                                color = SciFiCyanAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PromptFontFamily
                            )
                        }
                    }

                    // Input Box
                    OutlinedTextField(
                        value = rawTextFieldState,
                        onValueChange = { rawTextFieldState = com.example.util.ThaiTextFilter.processThaiBackspace(rawTextFieldState, it) },
                        placeholder = {
                            Text(
                                text = "พิมพ์บันทึกการทำงานของคุณที่นี่...",
                                fontFamily = SarabunFontFamily,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            autoCorrect = false
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SciFiSurfaceRecessed,
                            unfocusedContainerColor = SciFiSurfaceRecessed,
                            focusedBorderColor = SciFiCyanAccent,
                            unfocusedBorderColor = SciFiBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_log_input_field")
                    )

                    // Voice Recording Button (Matching Reference Image)
                    OutlinedButton(
                        onClick = { startVoiceInput() },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SciFiOrangeBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SciFiSurfaceRecessed,
                            contentColor = SciFiOrangeAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = SciFiOrangeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "บันทึกด้วยเสียง",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SciFiOrangeAccent,
                            fontFamily = SarabunFontFamily
                        )
                    }
                }
            }

            // Section: ATTACH_IMAGES (Gallery Picker, Camera & Firebase Storage Upload)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                border = BorderStroke(1.dp, SciFiBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "▸  ATTACH_IMAGES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SciFiCyanAccent,
                                fontFamily = PromptFontFamily,
                                letterSpacing = 1.sp
                            )
                            if (attachedImageUris.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SciFiCyanAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${attachedImageUris.size} รูป",
                                        color = SciFiCyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons: Gallery & Camera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { multipleImagePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SciFiCyanGlow),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SciFiSurfaceRecessed,
                                contentColor = SciFiCyanAccent
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                                .testTag("pick_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                modifier = Modifier.size(16.dp),
                                tint = SciFiCyanAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "แนบไฟล์ / รูปภาพ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = { launchCameraCapture() },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SciFiOrangeBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SciFiSurfaceRecessed,
                                contentColor = SciFiOrangeAccent
                            ),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(42.dp)
                                .testTag("take_camera_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Camera",
                                modifier = Modifier.size(16.dp),
                                tint = SciFiOrangeAccent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ถ่ายรูป",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }

                    if (attachedImageUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            itemsIndexed(attachedImageUris) { index, uriStr ->
                                val isImg = isImageUri(uriStr, context)
                                val displayName = getFileDisplayName(uriStr, context)
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(BorderStroke(1.dp, SciFiCyanAccent.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                ) {
                                    if (isImg) {
                                        AsyncImage(
                                            model = uriStr,
                                            contentDescription = "Attached Image $index",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Non-image file card
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = "ไฟล์แนบ",
                                                tint = SciFiCyanAccent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = displayName,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }

                                    // Index badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(topEnd = 6.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { attachedImageUris.removeAt(index) },
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Sync Notice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = SciFiCyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "รูปจะถูกอัปโหลดขึ้น Firebase Storage และบันทึก URL ลงใน Firestore ทันทีเมื่อกดบันทึก",
                                color = SciFiCyanAccent.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    } else {
                        Text(
                            text = "ยังไม่ได้แนบรูปภาพ (สามารถเลือกภาพจากแกลเลอรีหรือถ่ายภาพหน้างานได้)",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontFamily = SarabunFontFamily
                        )
                    }
                }
            }


            // Section 2 Card: CATEGORY_SELECT (Matching Reference Image)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                border = BorderStroke(1.dp, SciFiBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▸  CATEGORY_SELECT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SciFiCyanAccent,
                            fontFamily = PromptFontFamily,
                            letterSpacing = 1.sp
                        )

                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SciFiOrangeAccent
                            )
                        }
                    }

                    val detectedCategory = manualCategory ?: aiResult?.category ?: "รอตรวจสอบ"

                    // Active Status Tag Pill: ⦿ ACTIVE: รอตรวจสอบ
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SciFiSurfaceRecessed)
                            .border(BorderStroke(1.dp, SciFiOrangeBorder), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SciFiOrangeAccent)
                            )
                            Text(
                                text = "ACTIVE: $detectedCategory",
                                color = SciFiOrangeAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PromptFontFamily
                            )
                        }
                    }

                    // Grid of Categories (2x2 Grid + extra items matching ref layout)
                    val categories = listOf("ส่งสินค้า", "ซ่อมบำรุง", "ติดตั้ง", "บริการลูกค้า", "งานทั่วไป")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Row 1: 2 items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.take(2).forEach { cat ->
                                val isSelected = (manualCategory ?: aiResult?.category) == cat
                                CategoryGridButton(
                                    title = cat,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = { manualCategory = cat }
                                )
                            }
                        }

                        // Row 2: 2 items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.drop(2).take(2).forEach { cat ->
                                val isSelected = (manualCategory ?: aiResult?.category) == cat
                                CategoryGridButton(
                                    title = cat,
                                    isSelected = isSelected,
                                    modifier = Modifier.weight(1f),
                                    onClick = { manualCategory = cat }
                                )
                            }
                        }

                        // Row 3: Full width item if odd number
                        categories.lastOrNull()?.let { cat ->
                            val isSelected = (manualCategory ?: aiResult?.category) == cat
                            CategoryGridButton(
                                title = cat,
                                isSelected = isSelected,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { manualCategory = cat }
                            )
                        }
                    }
                }
            }

            // Section 3 Card: CASE_TIMESTAMP (Matching Reference Image)
            val thaiLocale = remember { java.util.Locale("th", "TH") }
            val dateFormatter = remember { java.text.SimpleDateFormat("dd.MM.yyyy", thaiLocale) }
            val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", thaiLocale) }

            // Convert calendar year to Thai Buddhist Era if desired (e.g., 2569)
            val currentYearBE = selectedDateTime.get(java.util.Calendar.YEAR) + 543
            val formattedDateStr = remember(selectedDateTime.timeInMillis, useCurrentTime) {
                if (useCurrentTime) {
                    val cal = java.util.Calendar.getInstance()
                    val day = String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH))
                    val month = String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)
                    val yearBE = cal.get(java.util.Calendar.YEAR) + 543
                    "$day.$month.$yearBE"
                } else {
                    val day = String.format("%02d", selectedDateTime.get(java.util.Calendar.DAY_OF_MONTH))
                    val month = String.format("%02d", selectedDateTime.get(java.util.Calendar.MONTH) + 1)
                    "$day.$month.$currentYearBE"
                }
            }

            val formattedTimeStr = remember(selectedDateTime.timeInMillis, useCurrentTime) {
                if (useCurrentTime) {
                    val cal = java.util.Calendar.getInstance()
                    String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
                } else {
                    timeFormatter.format(selectedDateTime.time)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                border = BorderStroke(1.dp, SciFiBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "▸  CASE_TIMESTAMP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SciFiCyanAccent,
                        fontFamily = PromptFontFamily,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Display Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SciFiSurfaceRecessed)
                                .border(BorderStroke(1.dp, SciFiBorderDark), RoundedCornerShape(8.dp))
                                .clickable {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val newCal = java.util.Calendar.getInstance().apply {
                                                timeInMillis = selectedDateTime.timeInMillis
                                                set(java.util.Calendar.YEAR, year)
                                                set(java.util.Calendar.MONTH, month)
                                                set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                            }
                                            selectedDateTime = newCal
                                            useCurrentTime = false
                                        },
                                        selectedDateTime.get(java.util.Calendar.YEAR),
                                        selectedDateTime.get(java.util.Calendar.MONTH),
                                        selectedDateTime.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "DATE",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PromptFontFamily,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = formattedDateStr,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = PromptFontFamily
                                )
                            }
                        }

                        // Time Display Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SciFiSurfaceRecessed)
                                .border(BorderStroke(1.dp, SciFiBorderDark), RoundedCornerShape(8.dp))
                                .clickable {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val newCal = java.util.Calendar.getInstance().apply {
                                                timeInMillis = selectedDateTime.timeInMillis
                                                set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                                set(java.util.Calendar.MINUTE, minute)
                                            }
                                            selectedDateTime = newCal
                                            useCurrentTime = false
                                        },
                                        selectedDateTime.get(java.util.Calendar.HOUR_OF_DAY),
                                        selectedDateTime.get(java.util.Calendar.MINUTE),
                                        true
                                    ).show()
                                }
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "TIME",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PromptFontFamily,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = formattedTimeStr,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = PromptFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Live Extracted Contact Card
            if (rawText.isNotBlank()) {
                ExtractedCustomerInfoCard(
                    rawText = rawText,
                    title = "ข้อมูลติดต่อและสถานที่ที่ตรวจพบ",
                    customDateTimestamp = if (useCurrentTime) null else selectedDateTime.timeInMillis,
                    onManualInfoEdited = { name, phone, location, onsiteTime ->
                        val updated = com.example.ai.CustomerInfoExtractor.updateRawTextWithManualInfo(
                            currentRawText = rawText,
                            name = name,
                            phone = phone,
                            location = location,
                            onsiteTime = onsiteTime
                        )
                        rawTextFieldState = androidx.compose.ui.text.input.TextFieldValue(
                            text = updated,
                            selection = androidx.compose.ui.text.TextRange(updated.length)
                        )
                    }
                )
            }

            // Action Submit Button
            Button(
                onClick = {
                    if (currentCaseStatus == ActiveCaseStatus.CLOSED) {
                        Toast.makeText(context, "ไม่สามารถบันทึกงานใหม่ได้ เนื่องจากสถานะเคสอยู่ในสถานะ ปิด (Closed)", Toast.LENGTH_LONG).show()
                    } else if (rawText.isBlank()) {
                        Toast.makeText(context, "กรุณากรอกข้อความลงงานก่อนบันทึก", Toast.LENGTH_SHORT).show()
                    } else if (!isSaving) {
                        isSaving = true
                        val finalCategory = manualCategory ?: aiResult?.category ?: "งานทั่วไป"
                        val imageUriStr = if (attachedImageUris.isNotEmpty()) attachedImageUris.joinToString(" | ") else null
                        
                        val prefsManager = com.example.util.EncryptedPrefsManager(context)
                        val techId = currentTechnician.id.trim().ifBlank {
                            prefsManager.getSavedTechnicianId() ?: prefsManager.getTechnicianId()
                        }
                        val techName = currentTechnician.name.trim().ifBlank {
                            prefsManager.getSavedTechnicianName() ?: prefsManager.getTechnicianName() ?: "ช่างประจำเคส"
                        }

                        viewModel.createCase(
                            rawText = rawText,
                            customCategory = finalCategory,
                            customTimestamp = if (useCurrentTime) null else selectedDateTime.timeInMillis,
                            imageUri = imageUriStr,
                            priority = selectedPriority,
                            technicianId = techId,
                            technicianName = techName,
                            createdBy = "technician",
                            onError = { errorMsg ->
                                isSaving = false
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            },
                            onComplete = {
                                isSaving = false
                                Toast.makeText(context, "บันทึกเคสใหม่สำเร็จ!", Toast.LENGTH_SHORT).show()
                                rawTextFieldState = androidx.compose.ui.text.input.TextFieldValue("")
                                manualCategory = null
                                selectedPriority = com.example.data.WorkLog.PRIORITY_MEDIUM
                                useCurrentTime = true
                                attachedImageUris.clear()
                                selectedDateTime = java.util.Calendar.getInstance()
                                onLogCreated()
                            }
                        )
                    }
                },
                enabled = !isSaving && currentCaseStatus == ActiveCaseStatus.ACTIVE,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_work_log_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentCaseStatus == ActiveCaseStatus.ACTIVE) SciFiOrangeAccent else Color(0xFF64748B),
                    contentColor = if (currentCaseStatus == ActiveCaseStatus.ACTIVE) Color(0xFF080E17) else Color.White,
                    disabledContainerColor = Color(0xFF334155),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF080E17)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "กำลังอัปโหลดและบันทึกเคส...",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF080E17),
                        fontSize = 14.sp,
                        fontFamily = SarabunFontFamily
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (currentCaseStatus == ActiveCaseStatus.ACTIVE) Color(0xFF080E17) else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentCaseStatus == ActiveCaseStatus.ACTIVE) "บันทึกการทำงาน (เปิดเคส)" else "ระงับการบันทึก (สถานะเคส Closed)",
                        fontWeight = FontWeight.Bold,
                        color = if (currentCaseStatus == ActiveCaseStatus.ACTIVE) Color(0xFF080E17) else Color.White,
                        fontSize = 15.sp,
                        fontFamily = SarabunFontFamily
                    )
                }
            }
        }
    }

    // Floating Action Button Shortcuts (Speed Dial for Quick Urgent Case, Timer, Voice, Scan)
    QuickFabSpeedDial(
        viewModel = viewModel,
        onVoiceInputRequested = { startVoiceInput() },
        onOcrScanRequested = { showOcrScannerInScreen = true },
        onQuickUrgentCreated = { onLogCreated() },
        modifier = Modifier.fillMaxSize()
    )

    // Modal OCR Scanner Dialog triggered from FAB
    if (showOcrScannerInScreen) {
        OcrTextScannerDialog(
            viewModel = viewModel,
            onDismiss = { showOcrScannerInScreen = false }
        )
    }
}
}

@Composable
private fun CategoryGridButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0B1724) else SciFiSurfaceRecessed)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) SciFiCyanGlow else SciFiBorderDark
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) SciFiCyanAccent else Color(0xFFCBD5E1),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = SarabunFontFamily
        )
    }
}

private fun isImageUri(uriStr: String, context: android.content.Context? = null): Boolean {
    val lower = uriStr.lowercase()
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")) {
        return true
    }
    if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".txt") || lower.endsWith(".zip") || lower.endsWith(".rar")) {
        return false
    }
    if (context != null && (uriStr.startsWith("content://") || uriStr.startsWith("file://"))) {
        try {
            val type = context.contentResolver.getType(Uri.parse(uriStr))
            if (type != null) {
                return type.startsWith("image/")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return !lower.contains(".pdf") && !lower.contains(".doc") && !lower.contains(".xls")
}

private fun getFileDisplayName(uriStr: String, context: android.content.Context? = null): String {
    if (context != null && (uriStr.startsWith("content://") || uriStr.startsWith("file://"))) {
        try {
            val uri = Uri.parse(uriStr)
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        return it.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    try {
        val uri = Uri.parse(uriStr)
        val path = uri.path ?: ""
        val cut = path.lastIndexOf('/')
        if (cut != -1) {
            val name = path.substring(cut + 1)
            if (name.isNotBlank()) return name
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "ไฟล์แนบ"
}


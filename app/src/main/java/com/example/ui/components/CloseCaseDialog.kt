package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.WorkLog

import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tag
import com.example.util.SnUtils
import com.example.ui.theme.SarabunFontFamily

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.util.ImageExportUtils
import java.io.File
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ElevatedCard
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.RadioButton

@Composable
fun CloseCaseDialog(
    workLog: WorkLog,
    initialTechnician: String,
    onDismiss: () -> Unit,
    onConfirmClose: (solutions: String, technician: String, imageUri: String?) -> Unit
) {
    val context = LocalContext.current

    // Auto-detect S/N from workLog rawText or solutions if available
    val initialSnInfo = remember { SnUtils.extractSerialNumbers(workLog.solutions + "\n" + workLog.rawText) }
    var oldSn by remember { androidx.compose.runtime.mutableStateOf(initialSnInfo.oldSn ?: "") }
    var newSn by remember { androidx.compose.runtime.mutableStateOf(initialSnInfo.newSn ?: "") }
    var activeScannerField by remember { androidx.compose.runtime.mutableStateOf<String?>(null) } // "OLD" or "NEW"
    var showTextExtractorDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var textExtractorInput by remember { androidx.compose.runtime.mutableStateOf("") }

    val initialCleanSolutions = remember { SnUtils.cleanSolutionsText(workLog.solutions) }
    var solutionsState by remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(initialCleanSolutions)) }
    val solutions = solutionsState.text
    var technicianState by remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(initialTechnician)) }
    val technician = technicianState.text
    val attachedImageUris = remember { mutableStateListOf<String>().apply { addAll(workLog.imageUriList) } }
    var isError by remember { androidx.compose.runtime.mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var showWatermarkSelector by remember { androidx.compose.runtime.mutableStateOf(false) }
    val sharedPrefs = remember { context.getSharedPreferences("WatermarkPrefs", android.content.Context.MODE_PRIVATE) }
    var watermarkPresets by remember {
        val saved = sharedPrefs.getStringSet("watermarks", null)
        val list = if (saved != null) saved.toList().sorted() else listOf("ของเก่า 1", "ของใหม่ 1", "ของเก่า 2", "ของใหม่ 2")
        androidx.compose.runtime.mutableStateOf(list)
    }
    var newPresetInput by remember { androidx.compose.runtime.mutableStateOf("") }
    var selectedWatermarkText by remember { androidx.compose.runtime.mutableStateOf("ของเก่า 1") }
    var customWatermarkText by remember { androidx.compose.runtime.mutableStateOf("") }

    var tempPhotoFile by remember { androidx.compose.runtime.mutableStateOf<File?>(null) }
    var tempPhotoUri by remember { androidx.compose.runtime.mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val file = tempPhotoFile
            val uri = tempPhotoUri
            if (file != null && uri != null) {
                Toast.makeText(context, "กำลังประทับลายน้ำ...", Toast.LENGTH_SHORT).show()
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        val options = BitmapFactory.Options()
                        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                        if (originalBitmap != null) {
                            var rotatedBitmap = originalBitmap
                            try {
                                val exifInterface = ExifInterface(file.absolutePath)
                                val orientation = exifInterface.getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL
                                )
                                val matrix = Matrix()
                                when (orientation) {
                                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                                }
                                if (!matrix.isIdentity) {
                                    rotatedBitmap = Bitmap.createBitmap(
                                        originalBitmap, 0, 0,
                                        originalBitmap.width, originalBitmap.height, matrix, true
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            val label = if (selectedWatermarkText == "กำหนดเอง") {
                                customWatermarkText.ifBlank { "งานซ่อมบำรุง" }
                            } else {
                                selectedWatermarkText
                            }
                            val watermarkedBitmap = ImageExportUtils.applyWatermark(context, rotatedBitmap, label)
                            try {
                                val savedUri = ImageExportUtils.saveBitmapToGallery(context, watermarkedBitmap, "Watermark_${label}")

                                withContext(Dispatchers.Main) {
                                    if (savedUri != null) {
                                        attachedImageUris.add(savedUri.toString())
                                        Toast.makeText(context, "บันทึกรูปลายน้ำ \"$label\" ลงแกลเลอรี่และแนบสำเร็จ!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "บันทึกรูปลงแกลเลอรี่ไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } finally {
                                if (originalBitmap != null) {
                                    originalBitmap.recycle()
                                }
                                if (rotatedBitmap != originalBitmap) {
                                    rotatedBitmap.recycle()
                                }
                                watermarkedBitmap.recycle()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "เกิดข้อผิดพลาดในการใส่ลายน้ำ: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val launchActualWatermarkCamera: () -> Unit = {
        try {
            val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val file = File(cacheDir, "worklog_watermark_${System.currentTimeMillis()}.jpg")
            if (!file.exists()) {
                file.createNewFile()
            }
            tempPhotoFile = file
            val authority = "${context.packageName}.fileprovider"
            tempPhotoUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            tempPhotoUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ล้มเหลวในการจัดเตรียมกล้อง: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchActualWatermarkCamera()
        } else {
            Toast.makeText(context, "ต้องการสิทธิ์การใช้งานกล้องเพื่อถ่ายภาพ", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerWatermarkPhotoCapture = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchActualWatermarkCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Multiple Image Picker Launcher for unlimited photo attachments
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

    // Speech-to-Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!spokenText.isNullOrBlank()) {
                val newCombined = if (solutions.isBlank()) spokenText else "$solutions $spokenText"
                solutionsState = androidx.compose.ui.text.input.TextFieldValue(
                    text = newCombined,
                    selection = androidx.compose.ui.text.TextRange(newCombined.length)
                )
                isError = false
            }
        }
    }

    val startVoiceInput = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "พูดข้อความ Solutions...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "อุปกรณ์ไม่รองรับการพิมพ์ด้วยเสียง", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "ปิดเคส",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "ปิดเคสการทำงาน",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!workLog.isCheckedOut) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ เคสนี้ยังไม่ได้ Check OUT หน้างาน!\nกรุณากด Check OUT หน้างานก่อนทำการปิดเคส",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }
                }
                // Original Case Raw Text Preview Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ข้อความดิบเดิม:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = workLog.cleanRawText.ifBlank { "(ไม่มีข้อความรายละเอียดเดิม)" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Voice Input Helper Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(com.example.ui.theme.CustomAccentBlue.copy(alpha = 0.2f))
                        .clickable { startVoiceInput() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "พูดด้วยเสียง",
                            tint = com.example.ui.theme.SlateDarkText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "กดตรงนี้เพื่อพูดด้วยเสียง (Voice Input Solutions)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.SlateDarkText
                        )
                    }
                }

                // SERIAL NUMBER MANAGEMENT CARD (S/N เก่า & S/N ใหม่ + Camera Scanner)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, com.example.ui.theme.CustomAccentBlue.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ระบุ Serial Number (S/N) อุปกรณ์",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // S/N เก่า (Old Serial) Input
                        OutlinedTextField(
                            value = oldSn,
                            onValueChange = { oldSn = it },
                            label = { Text("S/N เก่า (อุปกรณ์เดิม/ถอดออก)") },
                            placeholder = { Text("พิมพ์ หรือ กดปุ่มสแกน...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = Color(0xFFEF4444))
                            },
                            trailingIcon = {
                                IconButton(onClick = { activeScannerField = "OLD" }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "สแกน S/N เก่า",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                                autoCorrect = false
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("old_sn_input")
                        )

                        // S/N ใหม่ (New Serial) Input
                        OutlinedTextField(
                            value = newSn,
                            onValueChange = { newSn = it },
                            label = { Text("S/N ใหม่ (อุปกรณ์เปลี่ยนใหม่/ใส่แทน)") },
                            placeholder = { Text("พิมพ์ หรือ กดปุ่มสแกน...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = Color(0xFF10B981))
                            },
                            trailingIcon = {
                                IconButton(onClick = { activeScannerField = "NEW" }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "สแกน S/N ใหม่",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                                autoCorrect = false
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("new_sn_input")
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )

                        OutlinedButton(
                            onClick = { showTextExtractorDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_extract_sn_text"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("สแกน S/N จากข้อความ / แชท", fontSize = 12.sp, fontFamily = SarabunFontFamily)
                        }
                    }
                }

                // Solutions Input Field (Explicitly Solutions = )
                OutlinedTextField(
                    value = solutionsState,
                    onValueChange = {
                        val filtered = com.example.util.ThaiTextFilter.processThaiBackspace(solutionsState, it)
                        solutionsState = filtered
                        if (filtered.text.isNotBlank()) isError = false
                    },
                    label = { Text("Solutions = (วิธีการแก้ไข) *") },
                    placeholder = { Text("พิมพ์ หรือ กดปุ่มไมค์เพื่อพูดด้วยเสียง...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput() }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "พูดด้วยเสียง",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                        autoCorrect = false
                    ),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("กรุณาระบุ Solutions = ก่อนปิดเคส", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("พิมพ์แก้ไข หรือพูดด้วยเสียงเพื่อระบุ Solutions = ")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solutions_input")
                )

                // Technician Input Field
                OutlinedTextField(
                    value = technicianState,
                    onValueChange = { technicianState = com.example.util.ThaiTextFilter.processThaiBackspace(technicianState, it) },
                    label = { Text("ช่าง = (ผู้ปฏิบัติงาน)") },
                    placeholder = { Text("ชื่อ-นามสกุล ช่างผู้ดูแล") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                        autoCorrect = false
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("technician_input")
                )

                // Image Attachment Section (Unlimited Photos)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (attachedImageUris.isNotEmpty()) {
                                        "แนบไฟล์ / รูปหลักฐาน (${attachedImageUris.size} ไฟล์ - ไม่จำกัด)"
                                    } else {
                                        "แนบไฟล์เอกสาร / รูปถ่าย (ไม่จำกัดจำนวน)"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (attachedImageUris.isNotEmpty()) {
                                IconButton(
                                    onClick = { attachedImageUris.clear() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "ลบทั้งหมด",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (attachedImageUris.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemsIndexed(attachedImageUris) { index, uriStr ->
                                    val isImg = isImageUri(uriStr, context)
                                    val displayName = getFileDisplayName(uriStr, context)
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black.copy(alpha = 0.1f))
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                                    ) {
                                        if (isImg) {
                                            AsyncImage(
                                                model = uriStr,
                                                contentDescription = "หลักฐานที่ ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            // Show generic file card
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachFile,
                                                    contentDescription = "ไฟล์แนบ",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = displayName,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }

                                        // Individual Remove Button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .clickable { attachedImageUris.removeAt(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "ลบไฟล์นี้",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                            .clickable { multipleImagePickerLauncher.launch("*/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = "เพิ่มไฟล์",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "+ เพิ่มไฟล์",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { multipleImagePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("แนบไฟล์ / รูปภาพ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showWatermarkSelector = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ถ่ายรูป + ลายน้ำ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // CASE SUMMARY IMAGE EXPORT CARD
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "บันทึกภาพสรุปงานปิดเคส",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "ประมวลผลรายละเอียดทั้งหมด (S/N, Solutions, ช่าง) ออกเป็นการ์ดรูปภาพสรุปงาน 1 ใบและเซฟลงแกลเลอรี่ทันที",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Button(
                            onClick = {
                                Toast.makeText(context, "กำลังสร้างรูปภาพสรุปงาน...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch(Dispatchers.Default) {
                                    try {
                                        val tempLogWithAttachedImages = workLog.copy(
                                            imageUri = if (attachedImageUris.isNotEmpty()) attachedImageUris.joinToString("|") else null
                                        )
                                        val summaryBitmap = ImageExportUtils.generateSummaryBitmap(
                                            context = context,
                                            workLog = tempLogWithAttachedImages,
                                            solutions = solutions,
                                            oldSn = oldSn,
                                            newSn = newSn,
                                            technician = technician
                                        )
                                        try {
                                            val savedUri = ImageExportUtils.saveBitmapToGallery(
                                                context = context,
                                                bitmap = summaryBitmap,
                                                title = "CaseSummary_${workLog.id}"
                                            )
                                            withContext(Dispatchers.Main) {
                                                if (savedUri != null) {
                                                    Toast.makeText(context, "บันทึกรูปสรุปงานปิดเคสลงแกลเลอรี่สำเร็จ! 🎉", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "ล้มเหลวในการบันทึกรูปภาพ", Toast.LENGTH_SHORT).show()
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
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("บันทึกภาพสรุปงานปิดเคสลงแกลเลอรี่", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!workLog.isCheckedOut) {
                        Toast.makeText(context, "⚠️ ไม่สามารถปิดเคสได้: กรุณากด Check OUT หน้างานก่อน", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (solutions.isBlank() && oldSn.isBlank() && newSn.isBlank()) {
                        isError = true
                    } else {
                        val baseSolutions = if (solutions.isBlank()) "ดำเนินการแก้ไขอุปกรณ์เรียบร้อย" else solutions
                        val finalFormattedSolutions = SnUtils.formatSolutionsWithSn(
                            mainSolutions = baseSolutions,
                            oldSn = oldSn,
                            newSn = newSn
                        )
                        val finalImageUris = if (attachedImageUris.isNotEmpty()) attachedImageUris.joinToString("|") else null
                        onConfirmClose(finalFormattedSolutions, technician, finalImageUris)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.ui.theme.CustomAccentBlue,
                    contentColor = com.example.ui.theme.SlateDarkText
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_close_case_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = com.example.ui.theme.SlateDarkText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("ยืนยันปิดเคส", fontWeight = FontWeight.Bold, color = com.example.ui.theme.SlateDarkText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("ยกเลิก")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    // Camera Barcode / S/N Scanner Dialog
    activeScannerField?.let { fieldType ->
        SnScannerDialog(
            title = if (fieldType == "OLD") "สแกน S/N เก่า (อุปกรณ์เดิม)" else "สแกน S/N ใหม่ (อุปกรณ์ใส่แทน)",
            excludeSns = if (fieldType == "OLD") {
                listOf(newSn).filter { it.isNotBlank() }
            } else {
                listOf(oldSn).filter { it.isNotBlank() }
            },
            onDismiss = { activeScannerField = null },
            onSnScanned = { scannedCode ->
                if (fieldType == "OLD") {
                    oldSn = scannedCode
                } else {
                    newSn = scannedCode
                }
                Toast.makeText(context, "สแกนบันทึก $scannedCode สำเร็จ", Toast.LENGTH_SHORT).show()
                activeScannerField = null
            }
        )
    }

    // S/N Text Extractor Dialog (สแกน S/N จากข้อความ)
    if (showTextExtractorDialog) {
        AlertDialog(
            onDismissRequest = { showTextExtractorDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "ดึงข้อมูล S/N จากข้อความ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SarabunFontFamily
                        )
                    }
                    IconButton(onClick = { showTextExtractorDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ปิด",
                            tint = Color.Gray
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "วางหรือพิมพ์ข้อความแชท/รายงานที่มีข้อมูล Serial Number เพื่อให้ระบบช่วยดึงข้อมูล S/N เก่า และ S/N ใหม่ให้อัตโนมัติ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SarabunFontFamily
                    )

                    OutlinedTextField(
                        value = textExtractorInput,
                        onValueChange = { textExtractorInput = it },
                        label = { Text("ข้อความแชท / รายงานงาน", fontFamily = SarabunFontFamily) },
                        placeholder = { Text("ตัวอย่าง: เปลี่ยนอุปกรณ์แทนตัวเก่า S/N: SN12345 ด้วยตัวใหม่ S/N: SN99887", fontSize = 12.sp, fontFamily = SarabunFontFamily) },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("text_extractor_input")
                    )

                    // Paste from Clipboard Button
                    OutlinedButton(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clipData = clipboard.primaryClip
                                if (clipData != null && clipData.itemCount > 0) {
                                    val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                                    if (pastedText.isNotBlank()) {
                                        textExtractorInput = pastedText
                                        Toast.makeText(context, "วางข้อความจากคลิปบอร์ดแล้ว", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "คลิปบอร์ดไม่มีข้อความ", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "คลิปบอร์ดว่างเปล่า", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "ไม่สามารถดึงข้อมูลจากคลิปบอร์ดได้", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End).testTag("btn_paste_clipboard")
                    ) {
                        Text("📋 วางจากคลิปบอร์ด", fontSize = 11.sp, fontFamily = SarabunFontFamily)
                    }

                    // Live extraction preview
                    val detectedInfo = remember(textExtractorInput) {
                        SnUtils.extractSerialNumbers(textExtractorInput)
                    }

                    if (detectedInfo.hasSerialNumbers) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🔍 ตรวจพบข้อมูล S/N อัตโนมัติ:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = SarabunFontFamily
                                )
                                detectedInfo.oldSn?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("S/N เก่า: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                                        Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                }
                                detectedInfo.newSn?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("S/N ใหม่: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                                        Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    } else if (textExtractorInput.isNotBlank()) {
                        Text(
                            text = "❌ ไม่พบรูปแบบ S/N ในข้อความนี้ (ลองพิมพ์ในรูปแบบ S/N เก่า: ... S/N ใหม่: ...)",
                            fontSize = 11.sp,
                            color = Color(0xFFF87171),
                            fontFamily = SarabunFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val detectedInfo = SnUtils.extractSerialNumbers(textExtractorInput)
                        if (detectedInfo.hasSerialNumbers) {
                            detectedInfo.oldSn?.let { oldSn = it }
                            detectedInfo.newSn?.let { newSn = it }
                            Toast.makeText(context, "ดึงข้อมูล S/N สำเร็จ!", Toast.LENGTH_SHORT).show()
                            showTextExtractorDialog = false
                        } else {
                            Toast.makeText(context, "กรุณาใส่ข้อความที่มีข้อมูล S/N ก่อน", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_extracted_sn")
                ) {
                    Text("ยืนยันการใช้ S/N", fontFamily = SarabunFontFamily)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showTextExtractorDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ยกเลิก", fontFamily = SarabunFontFamily)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Beautiful Custom Watermark Option Dialog
    if (showWatermarkSelector) {
        AlertDialog(
            onDismissRequest = { showWatermarkSelector = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "เลือกข้อความประทับลายน้ำ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "จัดการและเลือกลายน้ำเพื่อแสดงบนรูปถ่ายหลักฐานหน้างาน:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Input section to add new preset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPresetInput,
                            onValueChange = { newPresetInput = it },
                            placeholder = { Text("เพิ่มลายน้ำใหม่...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Button(
                            onClick = {
                                if (newPresetInput.isNotBlank()) {
                                    val trimmed = newPresetInput.trim()
                                    if (trimmed !in watermarkPresets) {
                                        val updated = watermarkPresets + trimmed
                                        watermarkPresets = updated
                                        sharedPrefs.edit().putStringSet("watermarks", updated.toSet()).apply()
                                        selectedWatermarkText = trimmed
                                    }
                                    newPresetInput = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("เพิ่ม", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Display all presets with radio buttons and delete buttons
                    val displayPresets = watermarkPresets + "กำหนดเอง"
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayPresets.forEach { preset ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedWatermarkText = preset }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = (selectedWatermarkText == preset),
                                        onClick = { selectedWatermarkText = preset }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedWatermarkText == preset) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                // Show delete option only for presets (not for "กำหนดเอง")
                                if (preset != "กำหนดเอง") {
                                    IconButton(
                                        onClick = {
                                            val updated = watermarkPresets.filter { it != preset }
                                            watermarkPresets = updated
                                            sharedPrefs.edit().putStringSet("watermarks", updated.toSet()).apply()
                                            if (selectedWatermarkText == preset) {
                                                selectedWatermarkText = updated.firstOrNull() ?: "กำหนดเอง"
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "ลบ",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedWatermarkText == "กำหนดเอง") {
                        OutlinedTextField(
                            value = customWatermarkText,
                            onValueChange = { customWatermarkText = it },
                            label = { Text("ข้อความลายน้ำกำหนดเอง") },
                            placeholder = { Text("เช่น อุปกรณ์สวิตช์เสีย, จุดติดตั้งชั้น 2") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWatermarkSelector = false
                        triggerWatermarkPhotoCapture()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("เปิดกล้องถ่ายภาพ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWatermarkSelector = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ยกเลิก")
                }
            },
            shape = RoundedCornerShape(16.dp)
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

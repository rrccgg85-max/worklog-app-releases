package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkOrangeAccent
import com.example.ui.theme.SarabunFontFamily
import com.example.ui.theme.SciFiBorderDark
import com.example.ui.theme.SciFiCyanAccent
import com.example.ui.theme.SciFiCyanGlow
import com.example.ui.theme.SciFiSurfaceDark
import com.example.ui.theme.SciFiSurfaceRecessed
import com.example.util.SnUtils
import java.util.concurrent.Executors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SnScannerDialog(
    title: String = "สแกน Serial Number (S/N)",
    excludeSns: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSnScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var manualSnInput by remember { mutableStateOf("") }
    var detectedCandidates = remember { mutableStateListOf<String>() }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }

    // Initialize ML Kit clients
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Helper to add unique and non-excluded candidates only (case-insensitive deduplication)
    val addCandidateUnique = { code: String ->
        val trimmedCode = code.trim()
        val isExcluded = excludeSns.any { it.trim().equals(trimmedCode, ignoreCase = true) }
        val isAlreadyInList = detectedCandidates.any { it.trim().equals(trimmedCode, ignoreCase = true) }
        if (!isExcluded && !isAlreadyInList && trimmedCode.isNotBlank()) {
            detectedCandidates.add(trimmedCode)
        }
    }

    // Launcher for Camera Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "ต้องการสิทธิ์เปิดกล้องเพื่อสแกน S/N", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo Capture Launcher (Take Photo of S/N label for OCR/candidate extraction)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val codes = barcodes.mapNotNull { it.rawValue?.trim() }.filter { it.isNotBlank() }
                            if (codes.isNotEmpty()) {
                                codes.forEach { code ->
                                    addCandidateUnique(code)
                                }
                            }
                        }
                        .addOnCompleteListener {
                            textRecognizer.process(inputImage)
                                .addOnSuccessListener { visionText ->
                                    val extracted = SnUtils.extractSnCandidatesFromText(visionText.text)
                                    if (extracted.isNotEmpty()) {
                                        extracted.forEach { code ->
                                            addCandidateUnique(code)
                                        }
                                    }
                                    if (detectedCandidates.isNotEmpty()) {
                                        Toast.makeText(context, "สแกนพบ S/N จากรูปภาพสำเร็จ!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "ไม่พบข้อมูลบาร์โค้ดหรือตัวอักษร S/N ในรูปภาพนี้", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "ไม่สามารถจำแนกตัวอักษรจากรูปภาพได้", Toast.LENGTH_SHORT).show()
                                }
                        }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ไม่สามารถอ่านรูปภาพได้", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
            textRecognizer.close()
        }
    }

    // Laser scan bar animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffsetY"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = SciFiSurfaceDark,
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
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = SciFiCyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = SarabunFontFamily
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ปิด",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CAMERA PREVIEW SCANNER BOX
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(BorderStroke(1.5.dp, SciFiCyanGlow), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        var lastAnalysisTime = 0L

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastAnalysisTime < 1800) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            lastAnalysisTime = currentTime

                                            var originalBitmap: Bitmap? = null
                                            var rotatedBitmap: Bitmap? = null
                                            var croppedBitmap: Bitmap? = null
                                            try {
                                                originalBitmap = imageProxy.toBitmap()
                                                val rotation = imageProxy.imageInfo.rotationDegrees
                                                rotatedBitmap = if (rotation != 0) {
                                                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                                    Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                                } else {
                                                    originalBitmap
                                                }

                                                val w = rotatedBitmap.width
                                                val h = rotatedBitmap.height
                                                
                                                // Crop the central scanning region corresponding directly to our UI visual band
                                                val cropWidth = (w * 0.65f).toInt().coerceAtMost(w)
                                                val cropHeight = (h * 0.20f).toInt().coerceAtMost(h)
                                                val startX = (w - cropWidth) / 2
                                                val startY = (h - cropHeight) / 2

                                                croppedBitmap = Bitmap.createBitmap(rotatedBitmap, startX, startY, cropWidth, cropHeight)
                                                val image = InputImage.fromBitmap(croppedBitmap, 0)

                                                barcodeScanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        val codes = barcodes.mapNotNull { it.rawValue?.trim() }.filter { it.isNotBlank() }
                                                        if (codes.isNotEmpty()) {
                                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                codes.forEach { code ->
                                                                    addCandidateUnique(code)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        it.printStackTrace()
                                                    }
                                                    .addOnCompleteListener {
                                                        textRecognizer.process(image)
                                                            .addOnSuccessListener { visionText ->
                                                                val text = visionText.text
                                                                if (text.isNotBlank()) {
                                                                    val extracted = SnUtils.extractSnCandidatesFromText(text)
                                                                    if (extracted.isNotEmpty()) {
                                                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                            extracted.forEach { code ->
                                                                                addCandidateUnique(code)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            .addOnFailureListener {
                                                                it.printStackTrace()
                                                            }
                                                            .addOnCompleteListener {
                                                                originalBitmap?.recycle()
                                                                if (rotatedBitmap != originalBitmap) {
                                                                    rotatedBitmap?.recycle()
                                                                }
                                                                croppedBitmap?.recycle()
                                                                imageProxy.close()
                                                            }
                                                    }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                originalBitmap?.recycle()
                                                if (rotatedBitmap != originalBitmap) {
                                                    rotatedBitmap?.recycle()
                                                }
                                                croppedBitmap?.recycle()
                                                imageProxy.close()
                                            }
                                        }

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                        cameraControl = camera.cameraControl
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Reticle Overlay Box with semi-transparent scan band
                        Box(
                            modifier = Modifier
                                .size(width = 220.dp, height = 110.dp)
                                .border(BorderStroke(2.dp, SciFiCyanAccent), RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Semi-transparent scan region band (แทบสีใส แสดงตำแหน่งที่สแกนจริง)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(SciFiCyanAccent.copy(alpha = 0.25f))
                            )

                            // Laser Scan Indicator Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.TopCenter)
                                    .padding(top = (108 * laserOffsetY).dp)
                                    .background(Color(0xFFEF4444))
                            )
                        }

                        // Torch & Gallery Controls Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isFlashOn) SciFiCyanAccent else Color.Black.copy(alpha = 0.6f))
                                    .clickable {
                                        isFlashOn = !isFlashOn
                                        cameraControl?.enableTorch(isFlashOn)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "ไฟฉาย",
                                    tint = if (isFlashOn) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "เลือกจากอัลบั้ม",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Instruction overlay
                        Text(
                            text = "วางบาร์โค้ด หรือ S/N ให้อยู่ในกรอบสแกน",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SarabunFontFamily,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    } else {
                        // Permission Request Placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = SciFiCyanAccent,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "กรุณาอนุญาตสิทธิ์การใช้กล้อง",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = SarabunFontFamily
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = SciFiCyanAccent, contentColor = Color.Black)
                            ) {
                                Text("เปิดกล้อง", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // DETECTED CANDIDATES FROM SCAN / OCR
                if (detectedCandidates.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "🎯 ผลการสแกน / พบ S/N ที่เป็นไปได้:",
                            fontSize = 11.sp,
                            color = SciFiCyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SarabunFontFamily
                        )
                        if (excludeSns.isNotEmpty()) {
                            Text(
                                text = "*(ระบบทำการคัดกรอง S/N ที่ซ้ำกับอีกส่วนออกโดยอัตโนมัติ)",
                                fontSize = 9.sp,
                                color = Color(0xFFF87171),
                                fontFamily = SarabunFontFamily
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            detectedCandidates.forEach { code ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SciFiCyanGlow.copy(alpha = 0.25f))
                                        .border(BorderStroke(1.dp, SciFiCyanAccent), RoundedCornerShape(6.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clickable {
                                                onSnScanned(code)
                                                onDismiss()
                                            }
                                            .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 6.dp)
                                    ) {
                                        Text(
                                            text = code,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "เลือก",
                                            tint = SciFiCyanAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(20.dp)
                                            .background(SciFiCyanAccent.copy(alpha = 0.4f))
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clickable { detectedCandidates.remove(code) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "ลบ",
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // MANUAL ENTRY OR QUICK SCAN GENERATOR
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "หรือ พิมพ์ / สแกนแบบจำลอง:",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = SarabunFontFamily
                    )

                    OutlinedTextField(
                        value = manualSnInput,
                        onValueChange = { manualSnInput = it },
                        keyboardOptions = KeyboardOptions(autoCorrect = false),
                        placeholder = {
                            Text("พิมพ์ S/N ด้วยตนเอง...", fontSize = 12.sp, color = Color(0xFF64748B))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SciFiSurfaceRecessed,
                            unfocusedContainerColor = SciFiSurfaceRecessed,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SciFiCyanAccent,
                            unfocusedBorderColor = SciFiBorderDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_sn_input")
                    )

                    // Quick Test Scan Presets for testing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val testCodes = listOf("SN-2026-8819", "HW-99827361", "NET-334411")
                        testCodes.forEach { sample ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SciFiSurfaceRecessed)
                                    .border(BorderStroke(1.dp, SciFiBorderDark), RoundedCornerShape(6.dp))
                                    .clickable { manualSnInput = sample }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sample,
                                    fontSize = 10.sp,
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val codeToUse = manualSnInput.ifBlank { detectedCandidates.firstOrNull() ?: "" }
                    if (codeToUse.isNotBlank()) {
                        onSnScanned(codeToUse)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "กรุณาสแกนหรือพิมพ์ S/N ก่อนตกลง", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SciFiCyanAccent, contentColor = Color.Black)
            ) {
                Text("ยืนยัน S/N", fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SciFiBorderDark),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text("ยกเลิก", fontFamily = SarabunFontFamily)
            }
        }
    )
}

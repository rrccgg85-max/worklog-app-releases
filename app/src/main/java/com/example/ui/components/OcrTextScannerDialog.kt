package com.example.ui.components

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.WorkLogViewModel
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.KanitFontFamily
import com.example.ui.theme.SciFiCyanAccent
import com.example.ui.theme.SciFiCyanGlow
import com.example.ui.theme.SciFiSurfaceDark
import com.example.ui.theme.SciFiBackgroundDark
import com.example.ui.theme.SciFiBorderDark
import com.example.ui.theme.SciFiSurfaceRecessed
import com.example.ui.theme.DarkOrangeAccent
import com.example.ui.theme.DarkOrangeBorder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Utility function to extract URLs and web links from text.
 */
fun extractUrlsFromText(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val urlPattern = Regex(
        """\b(?:https?://[^\s/$.?#].[^\s]*|www\.[a-zA-Z0-9.\-_~:/?#\[\]@!$&'()*+,;=]+|[a-zA-Z0-9.\-_~]+(?:\.com|\.co\.th|\.net|\.org|\.io|\.info|\.xyz|\.dev|\.me|\.link|\.online|\.cc)(?:/[^\s]*)?)\b""",
        RegexOption.IGNORE_CASE
    )
    return urlPattern.findAll(text).map { it.value.trim() }.distinct().toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrTextScannerDialog(
    viewModel: WorkLogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    val history by viewModel.allScannedTexts.collectAsStateWithLifecycle()
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }

    // Detected Text State
    var recognizedText by remember { mutableStateOf("") }
    var isAnalyzingRealtime by remember { mutableStateOf(true) }
    var isFromGallery by remember { mutableStateOf(false) }

    // AI OCR and Bitmap tracking states
    var isAiOcrLoading by remember { mutableStateOf(false) }
    val latestCameraFrameRef = remember { java.util.concurrent.atomic.AtomicReference<Bitmap?>(null) }
    var latestGalleryBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Initialize ML Kit Text Recognizer
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val coroutineScope = rememberCoroutineScope()

    // Helper to Copy Text
    val copyTextToClipboard = { textToCopy: String, customSource: String? ->
        if (textToCopy.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Scanned Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "คัดลอกข้อความลงคลิปบอร์ดแล้ว! 📋", Toast.LENGTH_SHORT).show()
            val finalSource = customSource ?: (if (isFromGallery) "แกลเลอรี่" else "กล้องถ่ายรูป")
            viewModel.insertScannedText(textToCopy, finalSource)
        } else {
            Toast.makeText(context, "ไม่มีข้อความให้คัดลอก", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for Camera Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "กรุณาอนุญาตการเข้าถึงกล้องเพื่อสแกนข้อความ", Toast.LENGTH_LONG).show()
        }
    }

    // Photo/Gallery Picker Launcher with Unified AI OCR (Focused on Links, URLs & English)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    latestGalleryBitmap = bitmap
                    isFromGallery = true
                    isAiOcrLoading = true
                    coroutineScope.launch {
                        try {
                            val ocrResult = com.example.ai.AiCategorizer.performOcr(bitmap)
                            if (!ocrResult.isNullOrBlank()) {
                                recognizedText = ocrResult
                                Toast.makeText(context, "AI สแกนดึงลิงก์เว็บและอังกฤษสำเร็จ! 🔗🇬🇧✨", Toast.LENGTH_LONG).show()
                            } else {
                                // Fallback to local ML Kit OCR
                                val inputImage = InputImage.fromBitmap(bitmap, 0)
                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val text = visionText.text
                                        if (text.isNotBlank()) {
                                            recognizedText = text
                                            Toast.makeText(context, "สแกนข้อความภาษาอังกฤษสำเร็จ (ออฟไลน์) 🇬🇧", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "ไม่พบตัวอักษรหรือข้อความในรูปภาพนี้", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "เกิดข้อผิดพลาดในการสแกน: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isAiOcrLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ไม่สามารถอ่านไฟล์ภาพได้", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Request camera permission on launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            textRecognizer.close()
            val oldBitmap = latestCameraFrameRef.getAndSet(null)
            oldBitmap?.recycle()
        }
    }

    // Scanning Line Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_ocr")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffsetY"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "สแกนดึงลิงก์ & ข้อความอังกฤษ",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = KanitFontFamily,
                                color = Color.White
                            )
                            Text(
                                text = "AI Links & English OCR",
                                fontSize = 11.sp,
                                fontFamily = KanitFontFamily,
                                color = SciFiCyanAccent.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "กลับ", tint = Color.White)
                        }
                    },
                    actions = {
                        // Toggle Flashlight
                        if (hasCameraPermission) {
                            IconButton(onClick = {
                                isFlashOn = !isFlashOn
                                cameraControl?.enableTorch(isFlashOn)
                            }) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "เปิด/ปิดไฟฉาย",
                                    tint = if (isFlashOn) SciFiCyanAccent else Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F172A)
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = Color(0xFF0B0F19),
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFF1E293B).copy(alpha = 0.5f)),
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1.0f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ปิด", fontFamily = KanitFontFamily)
                        }

                        Button(
                            onClick = { copyTextToClipboard(recognizedText, null) },
                            modifier = Modifier.weight(1.5f).testTag("btn_ocr_copy_all"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SciFiCyanAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "คัดลอกข้อความทั้งหมด",
                                fontFamily = KanitFontFamily,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            containerColor = Color(0xFF0B0F19)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                
                // Guidance Tips Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, SciFiCyanAccent.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SciFiCyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "💡 เล็งหน้าจอไปที่ บอร์ด, บิล, รหัส S/N หรือหน้าจอคอมพิวเตอร์เพื่อสแกนภาษาอังกฤษและดึงลิงก์เว็บ (URLs) ออกมาทันทีเพื่อความรวดเร็วและแม่นยำ (ข้ามภาษาไทยเพื่อให้คัดลอกง่ายที่สุด)",
                            fontSize = 11.sp,
                            fontFamily = KanitFontFamily,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }

                // SECTION 1: Camera Live Scan Box
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(BorderStroke(1.5.dp, SciFiCyanGlow), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAnalyzingRealtime) {
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
                                                if (!isAnalyzingRealtime) {
                                                    imageProxy.close()
                                                    return@setAnalyzer
                                                }
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastAnalysisTime < 1500) {
                                                    imageProxy.close()
                                                    return@setAnalyzer
                                                }
                                                lastAnalysisTime = currentTime

                                                var originalBitmap: Bitmap? = null
                                                var rotatedBitmap: Bitmap? = null
                                                try {
                                                    originalBitmap = imageProxy.toBitmap()
                                                    val rotation = imageProxy.imageInfo.rotationDegrees
                                                    rotatedBitmap = if (rotation != 0) {
                                                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                                        Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                                    } else {
                                                        originalBitmap
                                                    }

                                                    if (rotatedBitmap != null) {
                                                        val copiedBitmap = rotatedBitmap.copy(rotatedBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                                                        val oldBitmap = latestCameraFrameRef.getAndSet(copiedBitmap)
                                                        oldBitmap?.recycle()
                                                    }

                                                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                                    val mediaImage = imageProxy.image
                                                    if (mediaImage != null) {
                                                        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                                                        textRecognizer.process(inputImage)
                                                            .addOnSuccessListener { visionText ->
                                                                val text = visionText.text
                                                                if (text.isNotBlank()) {
                                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                        // Real-time offline English/Latin text update (no auto-pauses, extremely responsive)
                                                                        recognizedText = text
                                                                        isFromGallery = false
                                                                    }
                                                                }
                                                            }
                                                            .addOnCompleteListener {
                                                                originalBitmap?.recycle()
                                                                if (rotatedBitmap != originalBitmap) {
                                                                    rotatedBitmap?.recycle()
                                                                }
                                                                imageProxy.close()
                                                            }
                                                    } else {
                                                        val inputImage = InputImage.fromBitmap(rotatedBitmap, 0)
                                                        textRecognizer.process(inputImage)
                                                            .addOnSuccessListener { visionText ->
                                                                val text = visionText.text
                                                                if (text.isNotBlank()) {
                                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                        recognizedText = text
                                                                        isFromGallery = false
                                                                    }
                                                                }
                                                            }
                                                            .addOnCompleteListener {
                                                                originalBitmap?.recycle()
                                                                if (rotatedBitmap != originalBitmap) {
                                                                    rotatedBitmap?.recycle()
                                                                }
                                                                imageProxy.close()
                                                            }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    originalBitmap?.recycle()
                                                    if (rotatedBitmap != originalBitmap) {
                                                        rotatedBitmap?.recycle()
                                                    }
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
                                    }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Animated Laser Line overlay
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val maxHeightPx = constraints.maxHeight.toFloat()
                                val laserY = laserOffsetY * maxHeightPx
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .offset(y = (laserY / 2.75f).dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    SciFiCyanAccent,
                                                    SciFiCyanAccent,
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

                            // Guide target overlay box
                            Box(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = "🔴 สแกนเรียลไทม์กำลังทำงาน...",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = KanitFontFamily,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            // Realtime analyzer is paused
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = "หยุดสแกนชั่วคราว",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "สแกนเรียลไทม์หยุดชั่วคราว",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    fontFamily = KanitFontFamily
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "โปรดอนุญาตสิทธิ์การใช้กล้องถ่ายรูป",
                                color = Color.White,
                                fontFamily = KanitFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("เปิดตั้งค่าสิทธิ์", fontFamily = KanitFontFamily)
                            }
                        }
                    }
                }

                // CONTROLS BAR: Play/Pause, Upload Picture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasCameraPermission) {
                        Button(
                            onClick = { isAnalyzingRealtime = !isAnalyzingRealtime },
                            modifier = Modifier.weight(1f).testTag("btn_toggle_realtime_scan"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAnalyzingRealtime) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isAnalyzingRealtime) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAnalyzingRealtime) "หยุดสแกนกล้อง" else "เปิดสแกนกล้อง",
                                fontFamily = KanitFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).testTag("btn_ocr_upload_photo"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = SciFiCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "เลือกจากแกลเลอรี่",
                            fontFamily = KanitFontFamily,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // AI Scanner Action Button: Highly reliable, accurate, triggers on click to prevent blurry scans
                Button(
                    onClick = {
                        if (isAiOcrLoading) return@Button
                        
                        val bitmapToProcess = if (isFromGallery) {
                            latestGalleryBitmap
                        } else {
                            latestCameraFrameRef.get()
                        }
                        
                        if (bitmapToProcess == null) {
                            Toast.makeText(context, "กรุณาเปิดสแกนกล้องให้เห็นข้อความ หรือเลือกรูปจากแกลเลอรี่ก่อนกดสแกน", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        
                        isAnalyzingRealtime = false // Pause live scan during processing
                        isAiOcrLoading = true
                        coroutineScope.launch {
                            try {
                                val ocrResult = com.example.ai.AiCategorizer.performOcr(bitmapToProcess)
                                if (!ocrResult.isNullOrBlank()) {
                                    recognizedText = ocrResult
                                    Toast.makeText(context, "AI ดึงภาษาอังกฤษและลิงก์เว็บสแกนสำเร็จ! 🔗🇬🇧✨", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "ไม่สามารถดึงข้อความได้ หรือยังไม่ได้ตั้งค่าคีย์ Gemini API", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isAiOcrLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_ocr_ai_action"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAiOcrLoading) Color(0xFF334155) else Color(0xFF0F766E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isAiOcrLoading) Color.Transparent else SciFiCyanAccent.copy(alpha = 0.6f))
                ) {
                    if (isAiOcrLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SciFiCyanAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI กำลังดึงลิงก์และคัดลอกรหัส...",
                            fontFamily = KanitFontFamily,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SciFiCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val scanBtnText = if (isFromGallery) "✨ สแกนดึงลิงก์แกลเลอรี่ด้วย AI 🔗" else "📸 สแกนดึงลิงก์ภาพกล้องด้วย AI 🔗"
                        Text(
                            text = scanBtnText,
                            fontFamily = KanitFontFamily,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // SECTION 2: Text Recognition Output Box & Editing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isAiOcrLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = SciFiCyanAccent,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "AI กำลังสแกนหาลิงก์ (Ignore Thai)...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = KanitFontFamily,
                                        color = SciFiCyanAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Subject,
                                        contentDescription = null,
                                        tint = SciFiCyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "ข้อความที่สแกนพบ (แก้ไขเพิ่มเติมได้)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = KanitFontFamily,
                                        color = Color.White
                                    )
                                }
                            }

                            // Copy button
                            IconButton(
                                onClick = { copyTextToClipboard(recognizedText, null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "คัดลอกทั้งหมด",
                                    tint = SciFiCyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = recognizedText,
                            onValueChange = {
                                recognizedText = it
                                isAnalyzingRealtime = false // Pause scanner when user starts manually editing the text field
                            },
                            placeholder = {
                                Text(
                                    "สแกนเพื่ออ่านลิงก์ URLs หรือข้อความภาษาอังกฤษด้านบน...",
                                    fontFamily = KanitFontFamily,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 260.dp)
                                .testTag("ocr_scanned_text_field"),
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SciFiCyanAccent,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ตัวอักษร: ${recognizedText.length} ตัว",
                                fontSize = 11.sp,
                                fontFamily = KanitFontFamily,
                                color = Color.Gray
                            )

                            TextButton(
                                onClick = {
                                    if (recognizedText.isNotBlank()) {
                                        val finalSource = if (isFromGallery) "แกลเลอรี่" else "กล้องถ่ายรูป"
                                        viewModel.insertScannedText(recognizedText, finalSource)
                                        Toast.makeText(context, "บันทึกข้อมูลในประวัติเรียบร้อย! 💾", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "กรุณาสแกนหรือป้อนข้อความก่อนบันทึก", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("btn_ocr_save_history"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = SciFiCyanAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "บันทึกในประวัติ",
                                    fontFamily = KanitFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SciFiCyanAccent
                                )
                            }
                        }

                        // HIGH VALUE EXTRACTION 1: Clickable Web URLs / Links in scanned text
                        val urls = remember(recognizedText) {
                            extractUrlsFromText(recognizedText)
                        }

                        if (urls.isNotEmpty()) {
                            Divider(color = Color(0xFF1E293B), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "🔗 ลิงก์ที่ตรวจพบ (แตะที่ชิปเพื่อคัดลอกลิงก์ทันที):",
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily,
                                color = SciFiCyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                urls.forEach { url ->
                                    SuggestionChip(
                                        onClick = { 
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("URL Link", url)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "🔗 คัดลอกลิงก์: $url เรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                                            viewModel.insertScannedText(url, "ลิงก์ตรวจพบ")
                                        },
                                        label = {
                                            Text(
                                                text = url,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFF0C4A6E)
                                        ),
                                        border = BorderStroke(1.dp, SciFiCyanAccent.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }

                        // HIGH VALUE EXTRACTION 2: Highlight S/N matches inside scanned text
                        val sNs = remember(recognizedText) {
                            com.example.util.SnUtils.extractSnCandidatesFromText(recognizedText)
                        }

                        if (sNs.isNotEmpty()) {
                            Divider(color = Color(0xFF1E293B), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "💡 รหัสสินค้า / S/N ที่ตรวจพบ (แตะที่ชิปเพื่อคัดลอกรหัส):",
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily,
                                color = DarkOrangeAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sNs.forEach { sn ->
                                    SuggestionChip(
                                        onClick = { 
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Serial Number", sn)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "📋 คัดลอกรหัส: $sn เรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                                            viewModel.insertScannedText(sn, "ตรวจจับ S/N")
                                        },
                                        label = {
                                            Text(
                                                text = sn,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFF3B2304)
                                        ),
                                        border = BorderStroke(1.dp, DarkOrangeAccent.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 3: SCAN HISTORY LOGS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = SciFiCyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ประวัติเก็บข้อมูลล่าสุด (${history.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = KanitFontFamily,
                                    color = Color.White
                                )
                            }

                            if (history.isNotEmpty()) {
                                if (showClearAllConfirmation) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "ยืนยันล้าง?",
                                            fontSize = 11.sp,
                                            fontFamily = KanitFontFamily,
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(
                                            onClick = {
                                                viewModel.clearAllScannedTexts()
                                                showClearAllConfirmation = false
                                                Toast.makeText(context, "ล้างประวัติทั้งหมดแล้ว", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                        ) {
                                            Text("ใช่", fontFamily = KanitFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = { showClearAllConfirmation = false },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("ไม่", fontFamily = KanitFontFamily, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                } else {
                                    IconButton(
                                        onClick = { showClearAllConfirmation = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "ล้างประวัติทั้งหมด",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (history.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    "ไม่มีข้อมูลประวัติการสแกน",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontFamily = KanitFontFamily
                                )
                                Text(
                                    "เมื่อสแกนพบลิงก์หรือรหัสผ่านกล้อง ข้อมูลจะจัดเก็บที่นี่โดยอัตโนมัติ",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontFamily = KanitFontFamily,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                history.take(20).forEach { item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val badgeColor = when (item.scannedFrom) {
                                                    "กล้องถ่ายรูป" -> Color(0xFF0EA5E9)
                                                    "แกลเลอรี่" -> Color(0xFFF59E0B)
                                                    "ลิงก์ตรวจพบ" -> Color(0xFF38BDF8)
                                                    "ตรวจจับ S/N" -> Color(0xFF10B981)
                                                    else -> Color(0xFF8B5CF6)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(badgeColor.copy(alpha = 0.15f))
                                                        .border(BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f)), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = item.scannedFrom,
                                                        color = badgeColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = KanitFontFamily
                                                    )
                                                }

                                                val sdf = remember { java.text.SimpleDateFormat("dd/MM/yy HH:mm น.", java.util.Locale("th", "TH")) }
                                                val timeStr = remember(item.timestamp) { sdf.format(java.util.Date(item.timestamp)) }
                                                Text(
                                                    text = timeStr,
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    fontFamily = KanitFontFamily
                                                )
                                            }

                                            Text(
                                                text = item.text,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 3,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        recognizedText = item.text
                                                        isFromGallery = item.scannedFrom == "แกลเลอรี่"
                                                        Toast.makeText(context, "ดึงข้อความเข้ากล่องแก้ไขแล้ว", Toast.LENGTH_SHORT).show()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "แก้ไข",
                                                        tint = SciFiCyanAccent,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("แก้ไขในกล่อง", fontFamily = KanitFontFamily, fontSize = 10.sp, color = SciFiCyanAccent)
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("Scanned Text", item.text)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "คัดลอกเรียบร้อย! 📋", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "คัดลอก",
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(4.dp))

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteScannedText(item)
                                                        Toast.makeText(context, "ลบประวัตินี้แล้ว", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "ลบ",
                                                        tint = Color.Red.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(13.dp)
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


            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

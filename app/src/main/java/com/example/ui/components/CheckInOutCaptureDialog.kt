package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.WorkLog
import com.example.ui.theme.SarabunFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CheckInOutCaptureDialog(
    workLog: WorkLog,
    isCheckIn: Boolean, // true for Check IN, false for Check OUT
    isProcessingExternal: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val showLoading = isProcessing || isProcessingExternal
    var isTakingInAppPhoto by remember { mutableStateOf(false) }

    // Live In-App Camera state
    var showLiveCamera by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // Helper to save a Bitmap to cache and set selected URI
    val saveBitmapAndSetUri: (Bitmap) -> Unit = { bitmap ->
        scope.launch(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val photoFile = File(cacheDir, "checkin_snap_${System.currentTimeMillis()}.jpg")
                FileOutputStream(photoFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                val uri = Uri.fromFile(photoFile)
                withContext(Dispatchers.Main) {
                    selectedImageUri = uri
                    showLiveCamera = false
                    isTakingInAppPhoto = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "บันทึกรูปไม่สำเร็จ: ${e.message}", Toast.LENGTH_SHORT).show()
                    isTakingInAppPhoto = false
                }
            }
        }
    }

    // System Camera Intent Launcher
    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = tempCameraFile
        val uri = tempCameraUri
        if (success && uri != null) {
            selectedImageUri = uri
            showLiveCamera = false
        } else if (file != null && file.exists() && file.length() > 0 && uri != null) {
            selectedImageUri = uri
            showLiveCamera = false
        }
    }

    // Fallback Thumbnail Bitmap Camera Launcher
    val takePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            saveBitmapAndSetUri(bitmap)
        }
    }

    // File Picker Launcher (Supports any file/image/document)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            showLiveCamera = false
        }
    }

    // Function to launch external system camera app
    val launchSystemCameraApp = {
        try {
            val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val photoFile = File(cacheDir, "checkin_temp_${System.currentTimeMillis()}.jpg")
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
            systemCameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            // Fallback to TakePicturePreview if FileProvider fails
            try {
                takePreviewLauncher.launch(null)
            } catch (e2: Exception) {
                Toast.makeText(context, "ไม่สามารถเปิดกล้องได้: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showLiveCamera = true
        } else {
            Toast.makeText(context, "ต้องการสิทธิ์เปิดกล้องเพื่อถ่ายภาพ Check IN / OUT", Toast.LENGTH_SHORT).show()
        }
    }

    val requestCameraAndOpen = {
        if (hasCameraPermission) {
            showLiveCamera = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isImage = remember(selectedImageUri) {
        if (selectedImageUri == null) false
        else {
            val type = context.contentResolver.getType(selectedImageUri!!)
            type?.startsWith("image/") == true || 
            selectedImageUri!!.path?.lowercase()?.endsWith(".jpg") == true ||
            selectedImageUri!!.path?.lowercase()?.endsWith(".jpeg") == true ||
            selectedImageUri!!.path?.lowercase()?.endsWith(".png") == true ||
            selectedImageUri!!.path?.lowercase()?.endsWith(".webp") == true ||
            selectedImageUri!!.path?.lowercase()?.endsWith(".gif") == true
        }
    }

    val fileName = remember(selectedImageUri) {
        if (selectedImageUri == null) ""
        else {
            var result: String? = null
            if (selectedImageUri!!.scheme == "content") {
                val cursor = context.contentResolver.query(selectedImageUri!!, null, null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                } finally {
                    cursor?.close()
                }
            }
            if (result == null) {
                result = selectedImageUri!!.path
                val cut = result?.lastIndexOf('/') ?: -1
                if (cut != -1) {
                    result = result?.substring(cut + 1)
                }
            }
            result ?: "file"
        }
    }

    val actionTitle = if (isCheckIn) "Check IN - เข้าสถานที่ทำงาน" else "Check OUT - ออกจากสถานที่ทำงาน"
    val actionColor = if (isCheckIn) Color(0xFF10B981) else Color(0xFFF59E0B)
    val actionHeaderLabel = if (isCheckIn) "CHECK IN - เข้าสถานที่ทำงาน" else "CHECK OUT - ออกจากสถานที่ทำงาน"

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss น.", Locale("th", "TH")) }
    val currentTimeStr = remember { sdf.format(Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
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
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = actionTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        fontFamily = SarabunFontFamily
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ปิด",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            if (showLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = actionColor,
                        modifier = Modifier.size(50.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isCheckIn) "กำลังบันทึกและประทับเวลา..." else "กำลังอัปโหลดและปิดงาน...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = SarabunFontFamily
                    )
                }
            } else if (!isCheckIn) {
                // Check OUT layout
                if (showLiveCamera && hasCameraPermission) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "กล้องถ่ายภาพหลักฐาน",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = SarabunFontFamily
                            )
                            Text(
                                text = "ย้อนกลับ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                fontFamily = SarabunFontFamily,
                                modifier = Modifier.clickable { showLiveCamera = false }
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(2.dp, actionColor),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx).apply {
                                            scaleType = PreviewView.ScaleType.FILL_CENTER
                                        }
                                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                        cameraProviderFuture.addListener({
                                            try {
                                                val cameraProvider = cameraProviderFuture.get()
                                                val preview = Preview.Builder().build().also {
                                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                                }
                                                val imageCapture = ImageCapture.Builder()
                                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                    .build()
                                                imageCaptureUseCase = imageCapture

                                                val cameraSelector = if (useFrontCamera) {
                                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                                } else {
                                                    CameraSelector.DEFAULT_BACK_CAMERA
                                                }

                                                cameraProvider.unbindAll()
                                                val cam = cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    cameraSelector,
                                                    preview,
                                                    imageCapture
                                                )
                                                cameraControl = cam.cameraControl
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Flash & Camera Flip Controls
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .clickable {
                                                isFlashOn = !isFlashOn
                                                cameraControl?.enableTorch(isFlashOn)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Flash",
                                            tint = if (isFlashOn) Color(0xFFFBBF24) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .clickable {
                                                useFrontCamera = !useFrontCamera
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cameraswitch,
                                            contentDescription = "Flip",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Bottom Shutter Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isTakingInAppPhoto) {
                                        CircularProgressIndicator(
                                            color = actionColor,
                                            modifier = Modifier.size(36.dp),
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                val capture = imageCaptureUseCase
                                                if (capture != null && !isTakingInAppPhoto) {
                                                    isTakingInAppPhoto = true
                                                    val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                                                    val photoFile = File(cacheDir, "checkin_snap_${System.currentTimeMillis()}.jpg")
                                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                                    capture.takePicture(
                                                        outputOptions,
                                                        ContextCompat.getMainExecutor(context),
                                                        object : ImageCapture.OnImageSavedCallback {
                                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                                selectedImageUri = Uri.fromFile(photoFile)
                                                                showLiveCamera = false
                                                                isTakingInAppPhoto = false
                                                            }

                                                            override fun onError(exception: ImageCaptureException) {
                                                                Toast.makeText(context, "ถ่ายภาพไม่สำเร็จ: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                                isTakingInAppPhoto = false
                                                            }
                                                        }
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = actionColor,
                                                contentColor = Color(0xFF0F172A)
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "กดถ่ายรูปทันที",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = SarabunFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedImageUri == null) {
                    // Requirement 1: Options to Select Photo
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "แนบรูปหลักฐานการปิดงาน",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = SarabunFontFamily,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Text(
                            text = "กรุณาแนบรูปภาพหรือหลักฐานเพื่อใช้ในการปิดงานหน้างาน",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = SarabunFontFamily,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // ถ่ายรูปใหม่ (เปิดกล้อง)
                        Button(
                            onClick = { requestCameraAndOpen() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = actionColor,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_checkout_take_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ถ่ายรูปใหม่ (เปิดกล้อง)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                        }

                        // เลือกจากแกลเลอรี
                        OutlinedButton(
                            onClick = { fileLauncher.launch("image/*") },
                            border = BorderStroke(1.5.dp, Color(0xFF475569)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_checkout_pick_gallery")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "เลือกจากแกลเลอรี",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }
                } else {
                    // Requirement 2: Image Preview
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "รูปภาพหลักฐานการปิดงาน",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = SarabunFontFamily
                        )

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, actionColor),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "ภาพหลักฐานปิดงาน",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Watermark overlay at bottom
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color(0xE00F172A))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(actionColor)
                                        )
                                        Text(
                                            text = "ปิดงาน - " + workLog.formattedCaseNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontFamily = SarabunFontFamily
                                        )
                                        Text(
                                            text = "เวลาปิดงาน: $currentTimeStr",
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }
                                }
                            }
                        }

                        // Re-select options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { requestCameraAndOpen() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ถ่ายใหม่", fontSize = 12.sp, fontFamily = SarabunFontFamily)
                            }
                            OutlinedButton(
                                onClick = { fileLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("เลือกใหม่", fontSize = 12.sp, fontFamily = SarabunFontFamily)
                            }
                        }
                    }
                }
            } else {
                // Existing Check IN layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "เคส ${workLog.formattedCaseNumber}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8),
                                fontFamily = SarabunFontFamily
                            )
                            Text(
                                text = "ถ่ายภาพหน้างานเพื่อสแตมป์เวลา เข้า สถานที่ทำงานลงในรูปภาพ",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                fontFamily = SarabunFontFamily,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { requestCameraAndOpen() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = actionColor,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(38.dp)
                                .testTag("btn_capture_checkin_camera"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedImageUri == null) "เปิดกล้องถ่ายรูป" else "ถ่ายรูปใหม่",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    launchSystemCameraApp()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "แอปกล้อง",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SarabunFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = { fileLauncher.launch("*/*") },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "แนบไฟล์",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }

                    if (showLiveCamera && hasCameraPermission) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(2.dp, actionColor),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx).apply {
                                            scaleType = PreviewView.ScaleType.FILL_CENTER
                                        }
                                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                        cameraProviderFuture.addListener({
                                            try {
                                                val cameraProvider = cameraProviderFuture.get()
                                                val preview = Preview.Builder().build().also {
                                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                                }
                                                val imageCapture = ImageCapture.Builder()
                                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                    .build()
                                                imageCaptureUseCase = imageCapture

                                                val cameraSelector = if (useFrontCamera) {
                                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                                } else {
                                                    CameraSelector.DEFAULT_BACK_CAMERA
                                                }

                                                cameraProvider.unbindAll()
                                                val cam = cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    cameraSelector,
                                                    preview,
                                                    imageCapture
                                                )
                                                cameraControl = cam.cameraControl
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .clickable {
                                                isFlashOn = !isFlashOn
                                                cameraControl?.enableTorch(isFlashOn)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Flash",
                                            tint = if (isFlashOn) Color(0xFFFBBF24) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .clickable {
                                                useFrontCamera = !useFrontCamera
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cameraswitch,
                                            contentDescription = "Flip",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isTakingInAppPhoto) {
                                        CircularProgressIndicator(
                                            color = actionColor,
                                            modifier = Modifier.size(36.dp),
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                val capture = imageCaptureUseCase
                                                if (capture != null && !isTakingInAppPhoto) {
                                                    isTakingInAppPhoto = true
                                                    val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                                                    val photoFile = File(cacheDir, "checkin_snap_${System.currentTimeMillis()}.jpg")
                                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                                    capture.takePicture(
                                                        outputOptions,
                                                        ContextCompat.getMainExecutor(context),
                                                        object : ImageCapture.OnImageSavedCallback {
                                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                                selectedImageUri = Uri.fromFile(photoFile)
                                                                showLiveCamera = false
                                                                isTakingInAppPhoto = false
                                                            }

                                                            override fun onError(exception: ImageCaptureException) {
                                                                Toast.makeText(context, "ถ่ายภาพไม่สำเร็จ: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                                isTakingInAppPhoto = false
                                                            }
                                                        }
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = actionColor,
                                                contentColor = Color(0xFF0F172A)
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "กดถ่ายรูปทันที",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = SarabunFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedImageUri != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, actionColor),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isImage) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "ภาพถ่ายหน้างาน",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AttachFile,
                                            contentDescription = "เอกสารแนบ",
                                            tint = actionColor,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = fileName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = SarabunFontFamily,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "ไฟล์ที่เลือกเพื่อทำการบันทึก",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = SarabunFontFamily,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color(0xE00F172A))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(actionColor)
                                                .padding(bottom = 2.dp)
                                        )
                                        Text(
                                            text = actionHeaderLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontFamily = SarabunFontFamily
                                        )
                                        Text(
                                            text = "เวลา: $currentTimeStr",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFCBD5E1),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(10.dp))
                                .clickable { requestCameraAndOpen() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = actionColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "กดที่นี่เพื่อเปิดกล้องถ่ายภาพหน้างาน",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = SarabunFontFamily
                                )
                                Text(
                                    text = "รองรับกล้องในแอป, แอปกล้องหลักของเครื่อง และคลังภาพ",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = SarabunFontFamily
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showLoading) {
                if (isCheckIn) {
                    Button(
                        onClick = {
                            val uri = selectedImageUri
                            if (uri != null && !isProcessing) {
                                isProcessing = true
                                onConfirm(uri)
                            } else if (uri == null) {
                                Toast.makeText(context, "กรุณาถ่ายภาพหน้างานก่อนทำรายการ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = selectedImageUri != null && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = actionColor,
                            contentColor = Color(0xFF0F172A),
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_confirm_checkin")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ยืนยัน Check IN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = SarabunFontFamily
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val uri = selectedImageUri
                            if (uri != null && !isProcessing) {
                                isProcessing = true
                                onConfirm(uri)
                            } else if (uri == null) {
                                Toast.makeText(context, "กรุณาแนบรูปหลักฐานการปิดงานก่อนทำรายการ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = selectedImageUri != null && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = actionColor,
                            contentColor = Color(0xFF0F172A),
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_confirm_checkout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ยืนยันปิดงาน",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = SarabunFontFamily
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (!showLoading) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = if (isCheckIn) Modifier.testTag("btn_cancel_checkin") else Modifier.testTag("btn_cancel_checkout")
                ) {
                    Text("ยกเลิก", fontSize = 13.sp, fontFamily = SarabunFontFamily)
                }
            }
        }
    )
}

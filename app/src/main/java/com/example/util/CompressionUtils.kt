package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility for local data and image compression to prevent storage bloat
 * and ensure lightweight Base64/Cloud payloads for Google Sheets sync.
 */
object CompressionUtils {

    /**
     * Downscales and compresses an image from Uri or local path, saving it to internal app storage.
     * Reduces image dimensions to a max of [maxDimension] and JPEG quality to [quality].
     */
    suspend fun compressAndSaveImage(
        context: Context,
        imageUri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): String? = withContext(Dispatchers.Default) {
        try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds first to prevent OutOfMemoryError
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            var inputStream: InputStream? = try {
                contentResolver.openInputStream(imageUri)
            } catch (e: Exception) {
                if (imageUri.scheme == null || imageUri.scheme == "file") {
                    File(imageUri.path ?: return@withContext null).inputStream()
                } else null
            }

            if (inputStream == null) return@withContext null

            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

            // 2. Calculate inSampleSize for optimal downsampling
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension * 2 ||
                options.outHeight / sampleSize > maxDimension * 2
            ) {
                sampleSize *= 2
            }

            // 3. Decode bitmap with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            inputStream = try {
                contentResolver.openInputStream(imageUri)
            } catch (e: Exception) {
                if (imageUri.scheme == null || imageUri.scheme == "file") {
                    File(imageUri.path ?: return@withContext null).inputStream()
                } else null
            }
            var bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) return@withContext null

            // 4. Handle EXIF rotation
            try {
                val exifStream = try {
                    contentResolver.openInputStream(imageUri)
                } catch (e: Exception) {
                    if (imageUri.scheme == null || imageUri.scheme == "file") {
                        File(imageUri.path ?: "").inputStream()
                    } else null
                }
                exifStream?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
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
                        bitmap = Bitmap.createBitmap(
                            bitmap!!, 0, 0,
                            bitmap!!.width, bitmap!!.height, matrix, true
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 5. Scale to exact max dimension if needed
            val width = bitmap!!.width
            val height = bitmap!!.height
            val maxCurrent = maxOf(width, height)
            val finalBitmap = if (maxCurrent > maxDimension) {
                val scale = maxDimension.toFloat() / maxCurrent
                Bitmap.createScaledBitmap(
                    bitmap!!,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                bitmap!!
            }

            // 6. Compress JPEG to file in cache
            val outputDir = File(context.filesDir, "compressed_images").apply { mkdirs() }
            val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")

            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            // Clean up bitmap memory
            if (finalBitmap != bitmap) {
                bitmap?.recycle()
            }
            finalBitmap.recycle()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun serveNull(): String? = null

    /**
     * Compresses multiple pipe-separated image URIs or file paths, returning a clean pipe-separated string
     * of compressed local file paths.
     */
    fun compressMultipleImages(
        context: Context,
        imageUriString: String?,
        maxDimension: Int = 1024,
        quality: Int = 80
    ): String? {
        if (imageUriString.isNullOrBlank()) return null
        val uriList = imageUriString.split("|").map { it.trim() }.filter { it.isNotBlank() }
        if (uriList.isEmpty()) return null

        val compressedList = mutableListOf<String>()

        for (item in uriList) {
            val file = File(item)
            if (file.exists() && item.contains("compressed_images")) {
                // Already a compressed local file
                compressedList.add(item)
            } else if (item.startsWith("http://") || item.startsWith("https://")) {
                // Remote URL
                compressedList.add(item)
            } else {
                // Uri or raw file path requiring compression
                val targetUri = if (item.startsWith("content://") || item.startsWith("file://")) {
                    Uri.parse(item)
                } else {
                    Uri.fromFile(File(item))
                }
                val compressedPath = runBlocking {
                    compressAndSaveImage(context, targetUri, maxDimension, quality)
                }
                if (!compressedPath.isNullOrBlank()) {
                    compressedList.add(compressedPath)
                } else if (file.exists()) {
                    compressedList.add(item)
                }
            }
        }

        return if (compressedList.isNotEmpty()) compressedList.joinToString(" | ") else null
    }

    /**
     * Sanitizes and cleans string metadata, stripping unnecessary blank lines
     * and reducing redundant spaces to minimize storage size.
     */
    fun cleanAndCompressText(rawText: String): String {
        return rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /**
     * Converts a file path or Uri string to lightweight Base64 string for cloud image sync.
     * Ensures image is compressed first to prevent payload bloat.
     */
    fun convertUriOrPathToBase64(context: Context, uriOrPath: String?): String {
        if (uriOrPath.isNullOrBlank()) return ""
        return try {
            val file = File(uriOrPath)
            if (file.exists() && uriOrPath.contains("compressed_images")) {
                // Direct read for already compressed files
                val bytes = file.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else if (file.exists()) {
                // Compress file on-the-fly first
                val compressedPath = runBlocking {
                    compressAndSaveImage(context, Uri.fromFile(file))
                }
                val targetFile = if (!compressedPath.isNullOrBlank()) File(compressedPath) else file
                val bytes = targetFile.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else if (uriOrPath.startsWith("content://")) {
                // Compress content Uri on-the-fly first
                val compressedPath = runBlocking {
                    compressAndSaveImage(context, Uri.parse(uriOrPath))
                }
                if (!compressedPath.isNullOrBlank()) {
                    val bytes = File(compressedPath).readBytes()
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                } else {
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(uriOrPath))
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP) else ""
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Formats a file path or Uri string into a full Data URI scheme (data:image/jpeg;base64,...)
     * suitable for direct rendering or Google Sheets IMAGE formulas.
     */
    fun getDataUri(context: Context, uriOrPath: String?): String {
        val base64 = convertUriOrPathToBase64(context, uriOrPath)
        return if (base64.isNotBlank()) "data:image/jpeg;base64,$base64" else ""
    }
}

fun compressImageToBase64(
    context: Context,
    imageUri: String,
    maxWidth: Int = 800,
    maxHeight: Int = 800,
    quality: Int = 60
): String {
    if (imageUri.isBlank()) return ""
    try {
        val finalUri = if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
            Uri.parse(imageUri)
        } else {
            Uri.fromFile(File(imageUri))
        }

        // Calculate original size
        var originalSizeKb = 0.0
        try {
            context.contentResolver.openAssetFileDescriptor(finalUri, "r")?.use {
                originalSizeKb = it.length / 1024.0
            }
        } catch (e: Exception) {
            try {
                context.contentResolver.openInputStream(finalUri)?.use {
                    originalSizeKb = it.available() / 1024.0
                }
            } catch (ex: Exception) {}
        }

        // Load Bitmap
        val inputStream = context.contentResolver.openInputStream(finalUri) ?: return ""
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return ""

        // Resize bitmap preserving aspect ratio
        val width = originalBitmap.width
        val height = originalBitmap.height
        var newWidth = width
        var newHeight = height

        if (width > maxWidth || height > maxHeight) {
            val aspectRatio = width.toFloat() / height.toFloat()
            if (width > height) {
                newWidth = maxWidth
                newHeight = (maxWidth / aspectRatio).toInt()
            } else {
                newHeight = maxHeight
                newWidth = (maxHeight * aspectRatio).toInt()
            }
        }

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        if (resizedBitmap != originalBitmap) {
            originalBitmap.recycle()
        }

        // Compress
        var compressedBytes = ByteArray(0)
        val qualities = if (quality == 60) listOf(60, 40, 20) else listOf(quality, 40, 20).distinct().filter { it <= quality }
        
        for (q in qualities) {
            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, q, baos)
            compressedBytes = baos.toByteArray()
            baos.close()
            
            val sizeInKb = compressedBytes.size / 1024.0
            if (sizeInKb < 700.0) {
                break
            }
        }

        resizedBitmap.recycle()

        val originalSize = originalSizeKb.toInt()
        val compressedSize = (compressedBytes.size / 1024.0).toInt()
        Log.d("ImageCompress", "Original: ${originalSize}KB → Compressed: ${compressedSize}KB")

        return android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e("ImageCompress", "Failed to compress image: ${e.message}", e)
        return ""
    }
}



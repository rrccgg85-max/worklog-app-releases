package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.WorkLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageExportUtils {

    /**
     * Loads a Bitmap safely from a Content URI or local File path.
     */
    fun loadBitmapFromUriOrPath(context: Context, uriOrPath: String, maxDim: Int = 1024): Bitmap? {
        if (uriOrPath.isBlank()) return null
        return try {
            val uri = if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                Uri.parse(uriOrPath)
            } else {
                val file = File(uriOrPath)
                if (file.exists()) Uri.fromFile(file) else null
            } ?: return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            var inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                if (uri.scheme == "file") File(uri.path ?: return null).inputStream() else null
            }
            if (inputStream == null) return null
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDim * 2 || options.outHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                if (uri.scheme == "file") File(uri.path ?: return null).inputStream() else null
            }
            var bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) return null

            // Handle EXIF rotation
            val exifStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                if (uri.scheme == "file") File(uri.path ?: "").inputStream() else null
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
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads all attached bitmaps from a WorkLog's imageUriList.
     */
    fun getAttachedBitmaps(context: Context, workLog: WorkLog): List<Bitmap> {
        val list = mutableListOf<Bitmap>()
        workLog.imageUriList.forEach { uriStr ->
            val bmp = loadBitmapFromUriOrPath(context, uriStr, maxDim = 1200)
            if (bmp != null) {
                list.add(bmp)
            }
        }
        return list
    }

    /**
     * Saves a Bitmap to the public Gallery (DCIM/Pictures/WorkLogPhotos)
     * so it shows up immediately in the system gallery app.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val filename = "${title}_${System.currentTimeMillis()}.jpg"
        val contentResolver = context.contentResolver

        val imageDetails = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WorkLogPhotos")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails)
        if (imageUri != null) {
            try {
                contentResolver.openOutputStream(imageUri).use { out ->
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    imageDetails.clear()
                    imageDetails.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(imageUri, imageDetails, null, null)
                }
                return imageUri
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    contentResolver.delete(imageUri, null, null)
                } catch (ignored: Exception) {}
            }
        }
        return null
    }

    /**
     * Applies a professional-looking job watermark to a given bitmap using the custom Kanit font.
     * Includes a slate dark background banner, primary blue accent, large custom label, and a timestamp.
     */
    fun applyWatermark(context: Context, bitmap: Bitmap, labelText: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // Calculate dynamic dimensions for high-res images
        val bannerHeight = (height * 0.16f).coerceAtLeast(140f).coerceAtMost(300f)
        val bannerRect = RectF(0f, height - bannerHeight, width.toFloat(), height.toFloat())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Semi-transparent dark slate background
        paint.color = Color.parseColor("#E00F172A") // Deep slate dark (#0f172a with 224/255 alpha)
        paint.style = Paint.Style.FILL
        canvas.drawRect(bannerRect, paint)

        // 2. Primary blue/cyan accent bar at the top of the watermark
        val accentHeight = bannerHeight * 0.08f
        val accentRect = RectF(0f, height - bannerHeight, width.toFloat(), height - bannerHeight + accentHeight)
        paint.color = Color.parseColor("#3B82F6") // Accent Blue (#3b82f6)
        canvas.drawRect(accentRect, paint)

        // Load System Fonts (Watermark)
        val kanitBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val kanitRegular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // 3. Draw Watermark Label (e.g. ของเก่า 1 / ของใหม่ 1)
        paint.color = Color.WHITE
        paint.textSize = (bannerHeight * 0.35f).coerceAtLeast(24f)
        paint.typeface = kanitBold
        val textX = 40f
        val textY = height - (bannerHeight * 0.45f)
        canvas.drawText(labelText, textX, textY, paint)

        // 4. Draw timestamp (short format, e.g. 11/08/2026 15:45)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("th", "TH"))
        val dateStr = sdf.format(Date())
        paint.color = Color.parseColor("#94A3B8") // Slate gray (#94a3b8)
        paint.textSize = (bannerHeight * 0.22f).coerceAtLeast(16f)
        paint.typeface = kanitRegular
        val subTextY = height - (bannerHeight * 0.18f)
        canvas.drawText(dateStr, textX, subTextY, paint)

        return output
    }

    /**
     * Applies a high-tech "CHECK IN • ถึงสถานที่ทำงาน" watermark to a bitmap with date/time and case number.
     */
    fun applyCheckInWatermark(
        context: Context,
        bitmap: Bitmap,
        caseNumber: String,
        timestamp: Long = System.currentTimeMillis()
    ): Bitmap {
        return createCheckInOutStampBitmap(
            context = context,
            bitmap = bitmap,
            title = "CHECK IN - เข้าสถานที่ทำงาน",
            accentColorHex = "#10B981", // Emerald Green
            caseNumber = caseNumber,
            timestamp = timestamp
        )
    }

    /**
     * Applies a high-tech "CHECK OUT - ออกจากสถานที่ทำงาน" watermark to a bitmap with date/time and case number.
     */
    fun applyCheckOutWatermark(
        context: Context,
        bitmap: Bitmap,
        caseNumber: String,
        timestamp: Long = System.currentTimeMillis()
    ): Bitmap {
        return createCheckInOutStampBitmap(
            context = context,
            bitmap = bitmap,
            title = "CHECK OUT - ออกจากสถานที่ทำงาน",
            accentColorHex = "#F59E0B", // Amber / Orange
            caseNumber = caseNumber,
            timestamp = timestamp
        )
    }

    private fun createCheckInOutStampBitmap(
        context: Context,
        bitmap: Bitmap,
        title: String,
        accentColorHex: String,
        caseNumber: String,
        timestamp: Long
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val bannerHeight = (height * 0.18f).coerceAtLeast(160f).coerceAtMost(340f)
        val bannerRect = RectF(0f, height - bannerHeight, width.toFloat(), height.toFloat())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Semi-transparent dark slate background banner
        paint.color = Color.parseColor("#E00F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRect(bannerRect, paint)

        // 2. Top accent stripe
        val accentHeight = bannerHeight * 0.10f
        val accentRect = RectF(0f, height - bannerHeight, width.toFloat(), height - bannerHeight + accentHeight)
        paint.color = Color.parseColor(accentColorHex)
        canvas.drawRect(accentRect, paint)

        // Load System Fonts
        val kanitBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val kanitMedium = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // Title (e.g. 📍 CHECK IN • ถึงสถานที่ทำงาน)
        paint.color = Color.WHITE
        paint.textSize = (bannerHeight * 0.32f).coerceAtLeast(26f)
        paint.typeface = kanitBold
        val textX = 40f
        val textY = height - (bannerHeight * 0.48f)
        canvas.drawText(title, textX, textY, paint)

        // Subtitle: Formatted timestamp (Case number removed as requested)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss น.", Locale("th", "TH"))
        val dateStr = sdf.format(Date(timestamp))
        val subText = "เวลา: $dateStr"

        paint.color = Color.parseColor("#CBD5E1") // Slate 300
        paint.textSize = (bannerHeight * 0.22f).coerceAtLeast(18f)
        paint.typeface = kanitMedium
        val subTextY = height - (bannerHeight * 0.18f)
        canvas.drawText(subText, textX, subTextY, paint)

        return output
    }

    /**
     * Stamps an image Uri with Check IN or Check OUT watermark and saves it to cache/compressed directory.
     */
    fun stampAndSaveCheckInOutImage(
        context: Context,
        imageUri: Uri,
        isCheckIn: Boolean,
        caseNumber: String,
        timestamp: Long = System.currentTimeMillis()
    ): String? {
        return try {
            val originalBmp = loadBitmapFromUriOrPath(context, imageUri.toString(), maxDim = 1200) ?: return null
            val stampedBmp = if (isCheckIn) {
                applyCheckInWatermark(context, originalBmp, caseNumber, timestamp)
            } else {
                applyCheckOutWatermark(context, originalBmp, caseNumber, timestamp)
            }
            if (stampedBmp != originalBmp) {
                originalBmp.recycle()
            }

            // Save stamped photo to device public gallery immediately (Pictures/WorkLogPhotos)
            val title = if (isCheckIn) "CHECK_IN_$caseNumber" else "CHECK_OUT_$caseNumber"
            saveBitmapToGallery(context, stampedBmp, title)

            val outputDir = File(context.filesDir, "compressed_images").apply { mkdirs() }
            val prefix = if (isCheckIn) "checkin" else "checkout"
            val outputFile = File(outputDir, "${prefix}_${System.currentTimeMillis()}.jpg")

            java.io.FileOutputStream(outputFile).use { out ->
                stampedBmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            stampedBmp.recycle()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a beautiful pixel-perfect card image containing the entire case closing summary,
     * including attached close-case photos if present.
     */
    fun generateSummaryBitmap(
        context: Context,
        workLog: WorkLog,
        solutions: String = workLog.solutions,
        oldSn: String = "",
        newSn: String = "",
        technician: String = workLog.technician
    ): Bitmap {
        val snInfo = SnUtils.extractSerialNumbers(workLog.rawText + "\n" + solutions)
        val finalOldSn = if (oldSn.isNotBlank()) oldSn else (snInfo.oldSn ?: "")
        val finalNewSn = if (newSn.isNotBlank()) newSn else (snInfo.newSn ?: "")

        val width = 1080

        // Load System Fonts (Summary Image)
        val kanitBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val kanitRegular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val kanitMedium = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        // Pre-create StaticLayout for Raw text (Section 1)
        val boxInnerPadding = 24f
        val maxTextWidth = width - 100 - (boxInnerPadding * 2).toInt() // 1080 - 100 - 48 = 932

        val rawTextToDisplay = workLog.cleanRawText.ifBlank { "(ไม่มีข้อความรายละเอียดเพิ่มเติม)" }
        val rawTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 24f
            typeface = kanitRegular
        }
        val rawStaticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(rawTextToDisplay, 0, rawTextToDisplay.length, rawTextPaint, maxTextWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(rawTextToDisplay, rawTextPaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false)
        }
        val rawBoxHeight = rawStaticLayout.height + (boxInnerPadding * 2)

        // Pre-create StaticLayout for Solutions (Section 3)
        val solutionsToDisplay = solutions.ifBlank { "ดำเนินการแก้ไขอุปกรณ์เรียบร้อย" }
        val solutionsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E40AF")
            textSize = 24f
            typeface = kanitRegular
        }
        val solutionsStaticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(solutionsToDisplay, 0, solutionsToDisplay.length, solutionsPaint, maxTextWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(solutionsToDisplay, solutionsPaint, maxTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false)
        }
        val solutionsBoxHeight = solutionsStaticLayout.height + (boxInnerPadding * 2)

        // Pre-load attached close-case images (Section 5)
        val attachedBitmaps = getAttachedBitmaps(context, workLog)
        val hasPhotos = attachedBitmaps.isNotEmpty()

        val photosSectionHeight = when {
            !hasPhotos -> 0f
            attachedBitmaps.size == 1 -> 45f + 560f + 30f // Title + single large photo + spacing
            attachedBitmaps.size == 2 -> 45f + 380f + 30f // Title + 2 side-by-side photos + spacing
            else -> 45f + 680f + 30f // Title + 2x2 grid photos + spacing
        }

        // Calculate dynamic total height required for all sections
        val headerHeight = 160f
        val topSpacing = 35f
        val section1HeaderHeight = 45f + 36f // title + category text
        val section1Total = section1HeaderHeight + rawBoxHeight + 30f
        val section2Total = 45f + 120f + 30f // title + SN cards + spacing
        val section3Total = 45f + solutionsBoxHeight + 30f // title + solutions box + spacing
        val section4Total = 80f + 30f // technician card + spacing
        val section5PhotosTotal = photosSectionHeight
        val footerTotal = 100f

        val calculatedHeight = headerHeight + topSpacing + section1Total + section2Total + section3Total + section4Total + section5PhotosTotal + footerTotal
        val height = calculatedHeight.toInt().coerceAtLeast(1350)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Soft premium off-white
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Draw top brand header bar (Slate Dark #0F172A)
        paint.color = Color.parseColor("#0F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, paint)

        // Cyan-blue border indicator for Material 3 look
        paint.color = Color.parseColor("#3B82F6")
        canvas.drawRect(0f, headerHeight - 8f, width.toFloat(), headerHeight, paint)

        // Header Title (Th)
        textPaint.color = Color.WHITE
        textPaint.textSize = 38f
        textPaint.typeface = kanitBold
        val caseDisplayNo = workLog.formattedCaseNumber.ifBlank { "ปิดเคสปฏิบัติงาน" }
        canvas.drawText("ใบสรุปรายละเอียดการปิดเคส ($caseDisplayNo)", 50f, 65f, textPaint)

        // Header Timestamp
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm น.", Locale("th", "TH"))
        val closureTime = "วันเวลาปิดเคส: ${sdf.format(Date())}"
        textPaint.color = Color.parseColor("#60A5FA")
        textPaint.textSize = 24f
        textPaint.typeface = kanitRegular
        canvas.drawText(closureTime, 50f, 110f, textPaint)

        var currentY = headerHeight + topSpacing

        // Helper function to draw a section header
        val drawSectionHeader = { title: String ->
            paint.color = Color.parseColor("#1E293B")
            paint.style = Paint.Style.FILL
            canvas.drawRect(50f, currentY, 62f, currentY + 26f, paint)

            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 26f
            textPaint.typeface = kanitBold
            canvas.drawText(title, 75f, currentY + 23f, textPaint)
            currentY += 45f
        }

        // 2. Section 1: อาการ / ข้อมูลดิบเดิม
        drawSectionHeader("รายละเอียดงาน / อาการดั้งเดิม")
        val categoryText = "หมวดหมู่: ${workLog.category}  |  ความสำคัญ: ${workLog.priority}"
        textPaint.color = Color.parseColor("#475569")
        textPaint.textSize = 22f
        textPaint.typeface = kanitBold
        canvas.drawText(categoryText, 50f, currentY + 6f, textPaint)
        currentY += 36f

        // Draw RawText Box
        val rawRect = RectF(50f, currentY, (width - 50).toFloat(), currentY + rawBoxHeight)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rawRect, 12f, 12f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rawRect, 12f, 12f, paint)

        canvas.save()
        canvas.clipRect(rawRect)
        canvas.translate(50f + boxInnerPadding, currentY + boxInnerPadding)
        rawStaticLayout.draw(canvas)
        canvas.restore()

        currentY += rawBoxHeight + 30f

        // 3. Section 2: ข้อมูล Serial Number (S/N) - Side by Side Layout
        drawSectionHeader("ข้อมูลอุปกรณ์ที่ทำการสลับเปลี่ยน (S/N)")

        val cardHeight = 120f
        val halfWidth = (width - 100 - 20) / 2f // 480f

        // Left Card: Old SN (ถอดออก)
        val leftRect = RectF(50f, currentY, 50f + halfWidth, currentY + cardHeight)
        paint.color = Color.parseColor("#FEF2F2") // soft red
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(leftRect, 12f, 12f, paint)
        paint.color = Color.parseColor("#FCA5A5")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(leftRect, 12f, 12f, paint)

        textPaint.color = Color.parseColor("#991B1B")
        textPaint.textSize = 21f
        textPaint.typeface = kanitBold
        canvas.drawText("S/N เดิม (ถอดออก):", 70f, currentY + 38f, textPaint)

        val oldSnStr = finalOldSn.ifBlank { "ไม่ระบุ" }
        textPaint.color = Color.parseColor("#B91C1C")
        textPaint.textSize = if (oldSnStr.length > 20) 20f else if (oldSnStr.length > 15) 22f else 24f
        canvas.drawText(oldSnStr, 70f, currentY + 84f, textPaint)

        // Right Card: New SN (ใส่แทน)
        val rightX = 50f + halfWidth + 20f
        val rightRect = RectF(rightX, currentY, rightX + halfWidth, currentY + cardHeight)
        paint.color = Color.parseColor("#ECFDF5") // soft green
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rightRect, 12f, 12f, paint)
        paint.color = Color.parseColor("#A7F3D0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rightRect, 12f, 12f, paint)

        textPaint.color = Color.parseColor("#065F46")
        textPaint.textSize = 21f
        textPaint.typeface = kanitBold
        canvas.drawText("S/N ใหม่ (ใส่แทน):", rightX + 20f, currentY + 38f, textPaint)

        val newSnStr = finalNewSn.ifBlank { "ไม่ระบุ" }
        textPaint.color = Color.parseColor("#047857")
        textPaint.textSize = if (newSnStr.length > 20) 20f else if (newSnStr.length > 15) 22f else 24f
        canvas.drawText(newSnStr, rightX + 20f, currentY + 84f, textPaint)

        currentY += cardHeight + 30f

        // 4. Section 3: แนวทางการแก้ไข (Solutions)
        drawSectionHeader("แนวทางการแก้ไข / บันทึกการซ่อม (Solutions)")

        val solRect = RectF(50f, currentY, (width - 50).toFloat(), currentY + solutionsBoxHeight)
        paint.color = Color.parseColor("#EFF6FF") // soft blue
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(solRect, 12f, 12f, paint)
        paint.color = Color.parseColor("#BFDBFE")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(solRect, 12f, 12f, paint)

        canvas.save()
        canvas.clipRect(solRect)
        canvas.translate(50f + boxInnerPadding, currentY + boxInnerPadding)
        solutionsStaticLayout.draw(canvas)
        canvas.restore()

        currentY += solutionsBoxHeight + 30f

        // 5. Section 4: ช่างผู้ปฏิบัติงาน (Technician)
        val technicianName = technician.ifBlank { "ไม่ระบุช่างผู้ดูแล" }
        val bottomRect = RectF(50f, currentY, (width - 50).toFloat(), currentY + 74f)
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(bottomRect, 12f, 12f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(bottomRect, 12f, 12f, paint)

        textPaint.color = Color.parseColor("#1E293B")
        textPaint.textSize = 24f
        textPaint.typeface = kanitBold
        canvas.drawText("ช่างผู้ปิดงาน (Technician):", 75f, currentY + 46f, textPaint)

        textPaint.color = Color.parseColor("#0F172A")
        textPaint.textSize = 24f
        textPaint.typeface = kanitBold
        canvas.drawText(technicianName, 400f, currentY + 46f, textPaint)

        currentY += 74f + 30f

        // 6. Section 5: รูปภาพการปฏิบัติงาน / ภาพถ่ายปิดเคส (Attached Close Case Photos)
        if (hasPhotos) {
            val countLabel = "(${attachedBitmaps.size} รูปภาพ)"
            drawSectionHeader("รูปภาพประกอบ / ภาพถ่ายปิดเคส $countLabel")

            val drawBitmapInRect = { targetRect: RectF, srcBitmap: Bitmap ->
                paint.color = Color.parseColor("#0F172A")
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(targetRect, 12f, 12f, paint)

                canvas.save()
                canvas.clipRect(targetRect)

                // Scale center crop
                val srcRatio = srcBitmap.width.toFloat() / srcBitmap.height.toFloat()
                val targetRatio = targetRect.width() / targetRect.height()
                val srcRect = if (srcRatio > targetRatio) {
                    val cropW = (srcBitmap.height * targetRatio).toInt()
                    val left = (srcBitmap.width - cropW) / 2
                    Rect(left, 0, left + cropW, srcBitmap.height)
                } else {
                    val cropH = (srcBitmap.width / targetRatio).toInt()
                    val top = (srcBitmap.height - cropH) / 2
                    Rect(0, top, srcBitmap.width, top + cropH)
                }
                canvas.drawBitmap(srcBitmap, srcRect, targetRect, paint)
                canvas.restore()

                // Border
                paint.color = Color.parseColor("#38BDF8")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawRoundRect(targetRect, 12f, 12f, paint)
            }

            if (attachedBitmaps.size == 1) {
                val photoRect = RectF(50f, currentY, (width - 50).toFloat(), currentY + 560f)
                drawBitmapInRect(photoRect, attachedBitmaps[0])
                currentY += 560f + 30f
            } else if (attachedBitmaps.size == 2) {
                val photoH = 380f
                val photoW = halfWidth
                val rect1 = RectF(50f, currentY, 50f + photoW, currentY + photoH)
                val rect2 = RectF(50f + photoW + 20f, currentY, width - 50f, currentY + photoH)

                drawBitmapInRect(rect1, attachedBitmaps[0])
                drawBitmapInRect(rect2, attachedBitmaps[1])

                // Small pill captions
                val drawPill = { x: Float, y: Float, label: String ->
                    paint.color = Color.parseColor("#CC0F172A")
                    paint.style = Paint.Style.FILL
                    val pillRect = RectF(x, y - 24f, x + 160f, y + 10f)
                    canvas.drawRoundRect(pillRect, 6f, 6f, paint)
                    textPaint.color = Color.WHITE
                    textPaint.textSize = 18f
                    textPaint.typeface = kanitBold
                    canvas.drawText(label, x + 12f, y - 2f, textPaint)
                }
                drawPill(65f, currentY + 40f, "รูปที่ 1 (ของเดิม)")
                drawPill(50f + photoW + 35f, currentY + 40f, "รูปที่ 2 (ของใหม่)")

                currentY += photoH + 30f
            } else {
                // 3 or 4 photos in 2x2 grid
                val photoH = 320f
                val photoW = halfWidth
                val row1Y = currentY
                val row2Y = currentY + photoH + 16f

                val rect1 = RectF(50f, row1Y, 50f + photoW, row1Y + photoH)
                val rect2 = RectF(50f + photoW + 20f, row1Y, width - 50f, row1Y + photoH)
                drawBitmapInRect(rect1, attachedBitmaps[0])
                drawBitmapInRect(rect2, attachedBitmaps[1])

                if (attachedBitmaps.size >= 3) {
                    val rect3 = RectF(50f, row2Y, 50f + photoW, row2Y + photoH)
                    drawBitmapInRect(rect3, attachedBitmaps[2])
                }
                if (attachedBitmaps.size >= 4) {
                    val rect4 = RectF(50f + photoW + 20f, row2Y, width - 50f, row2Y + photoH)
                    drawBitmapInRect(rect4, attachedBitmaps[3])
                }

                currentY += (photoH * 2) + 16f + 30f
            }
        }

        // 7. Draw footer at bottom of the canvas
        val footerY = height - 45f
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawLine(50f, footerY - 30f, (width - 50).toFloat(), footerY - 30f, paint)

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 20f
        textPaint.typeface = kanitRegular
        canvas.drawText("เอกสารสรุปรายละเอียดการทำงานระบบอัตโนมัติ", 50f, footerY, textPaint)

        val appSignature = "WorkLog System"
        val appSigWidth = textPaint.measureText(appSignature)
        canvas.drawText(appSignature, width - appSigWidth - 50f, footerY, textPaint)

        return bitmap
    }
}

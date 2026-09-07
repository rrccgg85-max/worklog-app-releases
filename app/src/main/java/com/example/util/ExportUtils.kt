package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ai.CustomerInfoExtractor
import com.example.data.WorkLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun exportToCsv(context: Context, logs: List<WorkLog>) {
        if (logs.isEmpty()) {
            Toast.makeText(context, "ไม่มีข้อมูลที่จะส่งออก", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "WorkLog_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            // UTF-8 BOM for Microsoft Excel Thai language compatibility
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val csvContent = StringBuilder()
            // Header line
            csvContent.append("ลำดับ,วันที่-เวลา,สถานะ,หมวดหมู่,ช่างผู้ปฏิบัติงาน,รายละเอียดงาน,วิธีแก้ไข (Solutions),ลูกค้า,เบอร์โทร,สถานที่\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("th", "TH"))

            logs.forEachIndexed { index, log ->
                val dateStr = dateFormat.format(Date(log.timestamp))
                val extracted = CustomerInfoExtractor.extract(log.rawText)

                val row = listOf(
                    (index + 1).toString(),
                    escapeCsv(dateStr),
                    escapeCsv(log.status),
                    escapeCsv(log.category),
                    escapeCsv(log.technician),
                    escapeCsv(log.cleanRawText.ifBlank { log.rawText }),
                    escapeCsv(log.solutions),
                    escapeCsv(extracted.customerName ?: "-"),
                    escapeCsv(extracted.phone ?: "-"),
                    escapeCsv(extracted.buildingOrAddress ?: "-")
                ).joinToString(",")

                csvContent.append(row).append("\n")
            }

            outputStream.write(csvContent.toString().toByteArray(Charsets.UTF_8))
            outputStream.close()

            shareFile(context, file, "text/csv", "แชร์ไฟล์ Excel / CSV")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "เกิดข้อผิดพลาดในการสร้างไฟล์ Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportToPdf(context: Context, logs: List<WorkLog>) {
        if (logs.isEmpty()) {
            Toast.makeText(context, "ไม่มีข้อมูลที่จะส่งออก", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595x842 pt)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subTitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
                typeface = Typeface.DEFAULT
            }
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.DEFAULT
            }
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var y = 40f

            // Header Title
            canvas.drawText("รายงานสรุปบันทึกการทำงาน (Work Log Report)", 40f, y, titlePaint)
            y += 20f
            val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm น.", Locale("th", "TH")).format(Date())
            canvas.drawText("วันที่ออกรายงาน: $dateStr | ทั้งหมด ${logs.size} รายการ", 40f, y, subTitlePaint)
            y += 15f

            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            // Metrics Summary Box
            val openCount = logs.count { it.status == WorkLog.STATUS_OPEN }
            val closedCount = logs.count { it.status == WorkLog.STATUS_CLOSED }
            val headerBoxPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9")
            }
            canvas.drawRect(40f, y, 555f, y + 35f, headerBoxPaint)
            canvas.drawText("สถานะเคส:  เปิดเคส $openCount รายการ  |  ปิดเคสแล้ว $closedCount รายการ", 50f, y + 22f, boldPaint)
            y += 50f

            val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale("th", "TH"))

            logs.take(20).forEachIndexed { index, log ->
                if (y > 780f) return@forEachIndexed

                val logDate = dateFormat.format(Date(log.timestamp))
                val statusColor = if (log.status == WorkLog.STATUS_CLOSED) Color.parseColor("#15803D") else Color.parseColor("#B91C1C")
                val statusPaint = Paint().apply {
                    color = statusColor
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.drawText("${index + 1}. [${log.category}] - $logDate", 40f, y, boldPaint)
                canvas.drawText(log.status, 480f, y, statusPaint)
                y += 15f

                val cleanText = log.cleanRawText.ifBlank { log.rawText }
                val logText = if (cleanText.length > 70) cleanText.take(67) + "..." else cleanText
                canvas.drawText("   รายละเอียด: $logText", 40f, y, textPaint)
                y += 15f

                if (log.solutions.isNotBlank()) {
                    val solText = if (log.solutions.length > 70) log.solutions.take(67) + "..." else log.solutions
                    canvas.drawText("   วิธีแก้ไข: $solText", 40f, y, textPaint)
                    y += 15f
                }

                val extracted = CustomerInfoExtractor.extract(log.rawText)
                val infoParts = mutableListOf<String>()
                extracted.customerName?.let { infoParts.add("ลูกค้า: $it") }
                extracted.phone?.let { infoParts.add("โทร: $it") }
                extracted.buildingOrAddress?.let { infoParts.add("สถานที่: $it") }

                if (infoParts.isNotEmpty()) {
                    canvas.drawText("   ${infoParts.joinToString(" | ")}", 40f, y, subTitlePaint)
                    y += 15f
                }

                canvas.drawLine(40f, y, 555f, y, linePaint)
                y += 12f
            }

            if (logs.size > 20) {
                canvas.drawText("...และอีก ${logs.size - 20} รายการ (ส่งออกครบถ้วนผ่านระบบ CSV / Excel)", 40f, y + 10f, subTitlePaint)
            }

            pdfDocument.finishPage(page)

            val fileName = "WorkLog_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            shareFile(context, file, "application/pdf", "แชร์ไฟล์ PDF รายงาน")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "เกิดข้อผิดพลาดในการสร้างไฟล์ PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(value: String): String {
        var clean = value.replace("\"", "\"\"")
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
            clean = "\"$clean\""
        }
        return clean
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "เกิดข้อผิดพลาดในการแชร์ไฟล์: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

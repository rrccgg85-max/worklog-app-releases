package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

data class SerialNumberInfo(
    val oldSn: String? = null,
    val newSn: String? = null
) {
    val hasSerialNumbers: Boolean
        get() = !oldSn.isNullOrBlank() || !newSn.isNullOrBlank()
}

object SnUtils {

    /**
     * Extracts Old S/N and New S/N from rawText, solutions, or combined log text.
     */
    fun extractSerialNumbers(text: String): SerialNumberInfo {
        if (text.isBlank()) return SerialNumberInfo()

        var oldSn: String? = null
        var newSn: String? = null

        // 1. Regex for Old S/N
        val oldRegexes = listOf(
            Regex("""(?:S/N\s*เก่า|S/N\s*เดิม|Old\s*S/N|SN\s*เก่า|S/N\s*\(เก่า\)|S/N\s*\[เก่า\])[\s:=]+([A-Za-z0-9\-_#]{4,35})""", RegexOption.IGNORE_CASE),
            Regex("""(?:S/N|SN|Serial)[\s:=]*([A-Za-z0-9\-_#]{4,35})[\s]+(?:เก่า|เดิม)""", RegexOption.IGNORE_CASE)
        )
        for (regex in oldRegexes) {
            val match = regex.find(text)
            if (match != null) {
                oldSn = match.groupValues.getOrNull(1)?.trim()
                if (!oldSn.isNullOrBlank()) break
            }
        }

        // 2. Regex for New S/N
        val newRegexes = listOf(
            Regex("""(?:S/N\s*ใหม่|New\s*S/N|SN\s*ใหม่|S/N\s*\(ใหม่\)|S/N\s*\[ใหม่\])[\s:=]+([A-Za-z0-9\-_#]{4,35})""", RegexOption.IGNORE_CASE),
            Regex("""(?:S/N|SN|Serial)[\s:=]*([A-Za-z0-9\-_#]{4,35})[\s]+(?:ใหม่)""", RegexOption.IGNORE_CASE)
        )
        for (regex in newRegexes) {
            val match = regex.find(text)
            if (match != null) {
                newSn = match.groupValues.getOrNull(1)?.trim()
                if (!newSn.isNullOrBlank()) break
            }
        }

        // 3. Fallback generic S/N matching if no explicit Old/New label found
        if (oldSn == null && newSn == null) {
            val genericRegex = Regex("""(?:S/N|SN|Serial|SerNo|S/N:)\s*([A-Za-z0-9\-_#]{5,35})""", RegexOption.IGNORE_CASE)
            val matches = genericRegex.findAll(text).map { it.groupValues[1].trim() }.distinct().toList()
            if (matches.isNotEmpty()) {
                oldSn = matches[0]
                if (matches.size > 1) {
                    newSn = matches[1]
                }
            }
        }

        return SerialNumberInfo(oldSn = oldSn, newSn = newSn)
    }

    /**
     * Formats solutions string with structured S/N section at the end.
     */
    fun formatSolutionsWithSn(
        mainSolutions: String,
        oldSn: String,
        newSn: String
    ): String {
        val cleanBase = cleanSolutionsText(mainSolutions)
        val snLines = mutableListOf<String>()
        if (oldSn.isNotBlank()) snLines.add("S/N เก่า = ${oldSn.trim()}")
        if (newSn.isNotBlank()) snLines.add("S/N ใหม่ = ${newSn.trim()}")

        if (snLines.isEmpty()) return cleanBase

        val snBlock = snLines.joinToString("\n")
        return if (cleanBase.isBlank()) {
            snBlock
        } else {
            "$cleanBase\n$snBlock"
        }
    }

    /**
     * Removes the auto-generated S/N block from solutions text to expose only the raw solutions description.
     */
    fun cleanSolutionsText(solutions: String): String {
        val blockRegex = Regex("""\n*---------------------------\n(?:🏷️\s*)?S/N[\s\S]*$""", RegexOption.IGNORE_CASE)
        val snLineRegex = Regex("""(?m)^\s*(?:🏷️\s*)?S/N\s*(?:เก่า|ใหม่|เดิม)[\s:=]+.*$""", RegexOption.IGNORE_CASE)
        var result = solutions.replace(blockRegex, "").trim()
        result = result.replace(snLineRegex, "").trim()
        return result
    }

    /**
     * Extracts potential S/N candidate tokens from camera OCR, voice input, or raw text labels.
     */
    fun extractSnCandidatesFromText(rawInput: String): List<String> {
        if (rawInput.isBlank()) return emptyList()

        // Match typical serial number formats (alphanumeric, hyphens, slashes, dots, min length 5)
        val candidateRegex = Regex("""\b([A-Za-z0-9\-_#/.]{5,32})\b""")
        val blacklistedWords = setOf(
            "SOLUTIONS", "SOLUTION", "SERIAL", "NUMBER", "MODEL", "STATUS", "CLOSED",
            "OPENED", "TECHNICIAN", "CASE", "UPDATE", "DEVICE", "ROUTER", "SWITCH",
            "MADE", "CHINA", "INPUT", "OUTPUT", "POWER", "VOLTAGE", "RATING", "CURRENT",
            "ADMIN", "PASSWORD", "SSID", "WIFI", "WLAN", "GATEWAY", "WAN", "LAN", "DEFAULT",
            "RESET", "BUTTON", "PORT", "PORTS", "CABLE", "CABLES", "GREEN", "BLUE", "RED",
            "YELLOW", "LIGHT", "LIGHTS", "INDICATOR", "PRODUCT", "BRAND", "VERSION", "REV",
            "YEAR", "MONTH", "DATE", "TIME", "MADE_IN", "PATENT", "PENDING", "WARRANTY",
            "VOID", "REMOVED", "BROKEN", "SEAL", "STICKER", "LABEL", "INFO", "DETAIL",
            "WIFI6", "CAT5E", "CAT6", "RJ45", "802.11AC", "802.11BGN", "802.11N", "12VDC", "24VDC", "220VAC", "5V/2A"
        )

        val blacklistedThai = setOf(
            "เครื่อง", "รุ่น", "ผลิต", "ไฟ", "แรงดัน", "กระแส", "ระบบ", "รหัส", "หมายเลข",
            "ข้อมูล", "ผู้ผลิต", "วันที่", "เดือน", "ปี", "อุปกรณ์", "สัญญาณ", "ช่อง", "ปุ่ม",
            "เข้า", "ออก", "สาย", "เสียบ", "ทดสอบ", "ผ่าน", "ไม่ผ่าน", "ซ่อม", "แก้ไข"
        )

        val ipRegex = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
        val dateRegex1 = Regex("""^\d{2}/\d{2}/\d{4}$""")
        val dateRegex2 = Regex("""^\d{4}/\d{2}/\d{2}$""")
        val dateRegex3 = Regex("""^\d{2}-\d{2}-\d{4}$""")
        val dateRegex4 = Regex("""^\d{4}-\d{2}-\d{2}$""")
        val unitRegex1 = Regex("""^\d+V(AC|DC|)?$""", RegexOption.IGNORE_CASE)
        val unitRegex2 = Regex("""^\d+(\.\d+)?(A|MA|W|HZ)$""", RegexOption.IGNORE_CASE)
        val unitRegex3 = Regex("""^\d+/\d+HZ$""", RegexOption.IGNORE_CASE)
        val unitRegex4 = Regex("""^\d+V\d+A$""", RegexOption.IGNORE_CASE)
        val unitRegex5 = Regex("""^\d+V-\d+A$""", RegexOption.IGNORE_CASE)

        return candidateRegex.findAll(rawInput)
            .map { it.groupValues[1].trim() }
            .filter { token ->
                val upper = token.uppercase()
                token.length >= 5 &&
                upper !in blacklistedWords &&
                upper !in blacklistedThai &&
                token.any { it.isDigit() } && // Has at least one digit
                !token.startsWith("http", ignoreCase = true) &&
                !token.startsWith("www", ignoreCase = true) &&
                !ipRegex.matches(token) &&
                !dateRegex1.matches(token) &&
                !dateRegex2.matches(token) &&
                !dateRegex3.matches(token) &&
                !dateRegex4.matches(token) &&
                !unitRegex1.matches(token) &&
                !unitRegex2.matches(token) &&
                !unitRegex3.matches(token) &&
                !unitRegex4.matches(token) &&
                !unitRegex5.matches(token) &&
                token != "2020" && token != "2021" && token != "2022" && token != "2023" && token != "2024" && token != "2025" && token != "2026"
            }
            .distinct()
            .toList()
    }

    /**
     * Copies S/N to clipboard with Toast confirmation.
     */
    fun copySnToClipboard(context: Context, label: String, snValue: String) {
        if (snValue.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, snValue)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📋 คัดลอก $label: $snValue เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
    }
}

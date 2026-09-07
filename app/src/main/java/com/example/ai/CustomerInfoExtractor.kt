package com.example.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

data class ExtractedCustomerInfo(
    val customerName: String? = null,
    val phone: String? = null,
    val buildingOrAddress: String? = null,
    val onsiteTime: String? = null
)

object CustomerInfoExtractor {

    /**
     * Gemini API System Prompt specifically engineered to parse contact information
     * with high-priority recognition for Thai mobile (06, 08, 09) and landline (02, 03, 04, 05, 07) numbers,
     * as well as onsite / appointment time entry parsing.
     */
    val GEMINI_EXTRACTION_SYSTEM_PROMPT = """
        You are an intelligent Thai entity extraction AI for field work logs and customer service requests.
        Analyze the provided Thai text and extract customer contact details into structured JSON fields.

        CRITICAL PHONE NUMBER RECOGNITION & PRIORITIZATION RULES:
        1. Recognize and PRIORITIZE Thai phone number formats:
           - Thai Mobile Numbers: 10 digits starting with 06, 08, or 09 (e.g., 081-234-5678, 062 345 6789, 0912345678, +66812345678).
           - Bangkok Landline Numbers: 9 digits starting with 02 (e.g., 02-123-4567, 029876543, 02 123 4567, +6621234567).
           - Provincial Landline Numbers: 9 digits starting with 03, 04, 05, or 07 (e.g., 038-123456, 053-987654).
        2. EXCLUDE non-phone numeric strings such as order numbers, tracking numbers, invoice IDs, item codes, prices, dates, or timestamps.
        3. If prefix keywords like "โทร", "เบอร์", "เบอร์โทร", "ติดต่อ", "มือถือ", "tel", "phone", "mobile", "call" are present, prioritize the phone number immediately following them.
        4. Format extracted Thai phone numbers cleanly with hyphens:
           - Mobile (starting 06, 08, 09): "0XX-XXX-XXXX"
           - Bangkok Landline (starting 02): "02-XXX-XXXX"
           - Provincial Landline (starting 03, 04, 05, 07): "0XX-XXX-XXX"

        CUSTOMER NAME RULES:
        - Extract customer or caller name prefixed by "ผู้แจ้ง", "ผู้ติดต่อ", "คุณ", "ลูกค้า", "ชื่อ", or "ติดต่อ" (e.g., "ผู้แจ้ง : กฤษณ์" -> customerName: "คุณกฤษณ์", "ลูกค้า: คุณวิภา" -> customerName: "คุณวิภา").
        - CRITICAL: NEVER return the field label or keyword itself (such as "ผู้แจ้ง", "ลูกค้า", "ชื่อ", "ผู้ติดต่อ", "ผู้รับเรื่อง") as the customer name. If input says "ผู้แจ้ง : กฤษณ์", extract "คุณกฤษณ์" or "กฤษณ์", NEVER "ผู้แจ้ง".

        LOCATION & ADDRESS RULES:
        - Extract location, building name, floor, room number, village, condo, or street address.

        ONSITE / APPOINTMENT TIME RULES:
        - Extract scheduled onsite time, entry time, or appointment date/time (e.g., "14:30 น.", "เข้า onsite 10:00", "เวลานัด 25/07 14:00").

        OUTPUT FORMAT:
        Return ONLY a JSON object:
        {
          "customerName": "...",
          "phone": "...",
          "buildingOrAddress": "...",
          "onsiteTime": "..."
        }
    """.trimIndent()

    fun buildGeminiExtractionPrompt(rawText: String): String {
        return "$GEMINI_EXTRACTION_SYSTEM_PROMPT\n\nInput Work Log Text:\n\"\"\"\n$rawText\n\"\"\""
    }

    fun cleanRawText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val lines = text.lines()
        val cleanLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if ((trimmed.startsWith("---") || trimmed.startsWith("[")) &&
                (trimmed.contains("ข้อมูลติดต่อแก้ไข") || trimmed.contains("ข้อมูลแก้ไข") ||
                 trimmed.contains("ข้อมูลลูกค้าแก้ไข") || trimmed.contains("ข้อมูลติดต่อ") ||
                 trimmed.contains("ข้อมูลลูกค้า"))
            ) {
                break
            }
            cleanLines.add(line)
        }
        return cleanLines.joinToString("\n").trim()
    }

    fun updateRawTextWithManualInfo(
        currentRawText: String?,
        name: String,
        phone: String,
        location: String,
        onsiteTime: String
    ): String {
        val baseText = cleanRawText(currentRawText)

        val lines = mutableListOf<String>()
        if (name.isNotBlank()) lines.add("ชื่อ: $name")
        if (phone.isNotBlank()) lines.add("โทร: $phone")
        if (location.isNotBlank()) lines.add("สถานที่: $location")
        if (onsiteTime.isNotBlank()) lines.add("เวลานัด: $onsiteTime")

        if (lines.isEmpty()) return baseText

        val overrideBlock = "--- [ข้อมูลติดต่อแก้ไข] ---\n" + lines.joinToString("\n")
        return if (baseText.isBlank()) overrideBlock else "$baseText\n\n$overrideBlock"
    }

    fun extract(text: String?): ExtractedCustomerInfo {
        if (text.isNullOrBlank()) return ExtractedCustomerInfo()

        return try {
            var manualName: String? = null
            var manualPhone: String? = null
            var manualAddress: String? = null
            var manualOnsiteTime: String? = null

            val lines = text.lines()
            var overrideStartIndex = -1
            for (i in lines.indices) {
                val trimmed = lines[i].trim()
                if ((trimmed.startsWith("---") || trimmed.startsWith("[")) &&
                    (trimmed.contains("ข้อมูลติดต่อแก้ไข") || trimmed.contains("ข้อมูลแก้ไข") ||
                     trimmed.contains("ข้อมูลลูกค้าแก้ไข") || trimmed.contains("ข้อมูลติดต่อ") ||
                     trimmed.contains("ข้อมูลลูกค้า"))
                ) {
                    overrideStartIndex = i
                    break
                }
            }

            val baseText = if (overrideStartIndex != -1) {
                for (i in (overrideStartIndex + 1) until lines.size) {
                    val trimmed = lines[i].trim()
                    when {
                        trimmed.startsWith("ชื่อ:") || trimmed.startsWith("ชื่อ :") || trimmed.startsWith("ลูกค้า:") || trimmed.startsWith("ผู้แจ้ง:") || trimmed.startsWith("ผู้แจ้ง :") || trimmed.startsWith("ผู้ติดต่อ:") -> {
                            val v = trimmed.substringAfter(":").trim()
                            if (v.isNotBlank()) manualName = v
                        }
                        trimmed.startsWith("โทร:") || trimmed.startsWith("โทร :") || trimmed.startsWith("เบอร์:") || trimmed.startsWith("ติดต่อ:") -> {
                            val v = trimmed.substringAfter(":").trim()
                            if (v.isNotBlank()) manualPhone = v
                        }
                        trimmed.startsWith("สถานที่:") || trimmed.startsWith("สถานที่ :") || trimmed.startsWith("ที่อยู่:") || trimmed.startsWith("อาคาร:") -> {
                            val v = trimmed.substringAfter(":").trim()
                            if (v.isNotBlank()) manualAddress = v
                        }
                        trimmed.startsWith("เวลานัด:") || trimmed.startsWith("เวลานัด :") || trimmed.startsWith("เวลา:") || trimmed.startsWith("นัดหมาย:") -> {
                            val v = trimmed.substringAfter(":").trim()
                            if (v.isNotBlank()) manualOnsiteTime = v
                        }
                    }
                }
                lines.subList(0, overrideStartIndex).joinToString("\n").trim()
            } else {
                text
            }

            var phone = manualPhone ?: extractThaiPhoneNumber(baseText)

            var name = manualName
            if (name == null) {
                val reservedLabels = setOf(
                    "ผู้แจ้ง", "ผู้ติดต่อ", "ลูกค้า", "ชื่อ", "ติดต่อ", "ผู้แจ้งเรื่อง", "แจ้งโดย",
                    "ชื่อผู้แจ้ง", "ชื่อผู้ติดต่อ", "รายละเอียด", "งาน", "เคส", "อาการ", "สถานที่",
                    "ที่อยู่", "เวลา", "เวลานัด", "โทร", "เบอร์", "เบอร์โทร", "ผู้รับเรื่อง"
                )

                // 1) Try explicit line prefixes like "ผู้แจ้ง : กฤษณ์" or "ชื่อ : สมชาย"
                val linePrefixRegex = Regex("""^(?:ผู้แจ้ง|ผู้ติดต่อ|ชื่อผู้แจ้ง|ชื่อผู้ติดต่อ|ชื่อลูกค้า|ลูกค้านาม|ชื่อ|ลูกค้า|คุณ|แจ้งโดย|ผู้แจ้งเรื่อง)\s*[:=]?\s*(.+)""", RegexOption.IGNORE_CASE)
                for (line in baseText.lines()) {
                    val trimmed = line.trim()
                    val match = linePrefixRegex.find(trimmed)
                    if (match != null && match.groupValues.size > 1) {
                        var valCandidate = match.groupValues[1].trim()
                        valCandidate = valCandidate.removePrefix(":").removePrefix("=").trim()
                        if (valCandidate.isNotBlank()) {
                            val cleanCandidate = valCandidate.replace(Regex("""^(คุณ|ลูกค้า)\s*""", RegexOption.IGNORE_CASE), "").trim()
                            if (cleanCandidate.isNotBlank() && cleanCandidate !in reservedLabels && !reservedLabels.any { cleanCandidate.equals(it, ignoreCase = true) }) {
                                name = if (valCandidate.startsWith("คุณ")) valCandidate else "คุณ $valCandidate"
                                break
                            }
                        }
                    }
                }

                // 2) Fallback name regex
                if (name == null) {
                    val nameRegex = Regex("""(?:คุณ|ลูกค้า|ชื่อ|ติดต่อ|ผู้แจ้ง|ผู้ติดต่อ|ชื่อผู้แจ้ง|ชื่อผู้ติดต่อ|แจ้งโดย|ผู้แจ้งเรื่อง)\s*[:=]?\s*(คุณ\s*[ก-๙a-zA-Z]+|[ก-๙a-zA-Z]{2,}(?:\s+[ก-๙a-zA-Z]+)?)""", RegexOption.IGNORE_CASE)
                    val nameMatch = nameRegex.find(baseText)
                    if (nameMatch != null && nameMatch.groupValues.size > 1) {
                        val matchedName = nameMatch.groupValues[1].trim()
                        val cleanCandidate = matchedName.replace(Regex("""^คุณ\s*""", RegexOption.IGNORE_CASE), "").trim()
                        if (cleanCandidate.isNotBlank() && cleanCandidate !in reservedLabels && !reservedLabels.any { cleanCandidate.equals(it, ignoreCase = true) }) {
                            name = if (matchedName.startsWith("คุณ")) matchedName else "คุณ $matchedName"
                        }
                    }
                }
            }

            var address = manualAddress
            if (address == null) {
                val locationKeywords = listOf(
                    "อาคาร", "ตึก", "ชั้น", "ห้อง", "หมู่บ้าน", "คอนโด", "สถานที่", "ที่อยู่",
                    "โครงการ", "ซอย", "ถนน", "แขวง", "เขต", "ตำบล", "อำเภอ", "จังหวัด", "โรงพยาบาล",
                    "ศูนย์", "นิคม", "บริษัท", "ห้าง", "ม. ", "บ้านเลขที่"
                )

                val lines = baseText.split("\n", ",", ";")
                val addressParts = mutableListOf<String>()

                for (line in lines) {
                    val trimmed = line.trim()
                    if (locationKeywords.any { trimmed.contains(it, ignoreCase = true) }) {
                        val cleaned = trimmed.replace(Regex("""^(สถานที่|ที่อยู่|สถานที่ส่ง|สถานที่ทำงาน|อาคาร/สถานที่)\s*[:=]?\s*""", RegexOption.IGNORE_CASE), "")
                        if (cleaned.isNotBlank() && !addressParts.contains(cleaned)) {
                            addressParts.add(cleaned)
                        }
                    }
                }

                if (addressParts.isNotEmpty()) {
                    address = addressParts.joinToString(", ")
                } else {
                    val addressRegex = Regex("""(?:อาคาร|ตึก|คอนโด|หมู่บ้าน|ที่อยู่|สถานที่)\s*[:=]?\s*([ก-๙a-zA-Z0-9\s/.-]+)""")
                    val addrMatch = addressRegex.find(baseText)
                    if (addrMatch != null && addrMatch.groupValues.size > 1) {
                        val matchedAddr = addrMatch.groupValues[1].trim()
                        if (matchedAddr.isNotBlank()) {
                            address = matchedAddr
                        }
                    }
                }
            }

            val onsiteTime = manualOnsiteTime ?: extractOnsiteTime(baseText)

            ExtractedCustomerInfo(
                customerName = name,
                phone = phone,
                buildingOrAddress = address,
                onsiteTime = onsiteTime
            )
        } catch (e: Exception) {
            ExtractedCustomerInfo()
        }
    }

    private fun extractOnsiteTime(text: String): String? {
        if (text.isBlank()) return null

        // 1. Check for time range pattern first, e.g. "10:00-11:00", "10.00 - 11.00 น."
        val rangeRegex = Regex("""(\d{1,2})[:.](\d{2})\s*-\s*(\d{1,2})[:.](\d{2})(?:\s*น\.)?""")
        val rangeMatch = rangeRegex.find(text)
        if (rangeMatch != null) {
            val h1 = rangeMatch.groupValues[1].toIntOrNull()
            val m1 = rangeMatch.groupValues[2].toIntOrNull()
            val h2 = rangeMatch.groupValues[3].toIntOrNull()
            val m2 = rangeMatch.groupValues[4].toIntOrNull()
            if (h1 != null && m1 != null && h2 != null && m2 != null && h1 in 0..23 && m1 in 0..59 && h2 in 0..23 && m2 in 0..59) {
                return rangeMatch.value.trim()
            }
        }

        // 2. Check for time with Thai keyword or standard keywords prefix
        val keywordTimeRegex = Regex("""(?:เวลา|ช่วง|ตอน|นัดหมาย|นัด|onsite|ออนไซต์|เข้างาน|เข้าพื้นที่|สแตนบาย)\s*[:=]?\s*(\d{1,2}[:.]\d{2}\s*(?:น\.|นาฬิกา|am|pm)?|\d{1,2}\s*โมง)""", RegexOption.IGNORE_CASE)
        val keywordMatch = keywordTimeRegex.find(text)
        if (keywordMatch != null) {
            return keywordMatch.groupValues[1].trim()
        }

        // 3. Fallback to matching lines with relevant keywords and a time
        val timeRegex = Regex("""(\d{1,2})[:.](\d{2})\s*(?:น\.|นาฬิกา|am|pm)?""", RegexOption.IGNORE_CASE)
        val lines = text.split("\n", ",", ";")
        for (line in lines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()
            if (lower.contains("onsite") ||
                lower.contains("ออนไซต์") ||
                lower.contains("นัด") ||
                lower.contains("เข้างาน") ||
                lower.contains("เข้าพื้นที่") ||
                lower.contains("สแตนบาย") ||
                lower.contains("เวลานัด") ||
                lower.contains("เวลา")) {

                val timeMatch = timeRegex.find(trimmed)
                if (timeMatch != null) {
                    val h = timeMatch.groupValues[1].toIntOrNull()
                    val m = timeMatch.groupValues[2].toIntOrNull()
                    if (h != null && m != null && h in 0..23 && m in 0..59) {
                        val cleaned = trimmed.replace(Regex("""^(เข้า\s*onsite|เข้า\s*ออนไซต์|onsite|ออนไซต์|เวลานัดหมาย|เวลานัด|นัดหมาย|นัด|เวลา)\s*[:=]?\s*""", RegexOption.IGNORE_CASE), "").trim()
                        if (cleaned.isNotBlank()) {
                            return cleaned
                        }
                    }
                }
            }
        }

        // 4. Any standalone time pattern with "น." suffix (e.g., "10:00 น.")
        val standaloneSuffixRegex = Regex("""\b(\d{1,2})[:.](\d{2})\s*(?:น\.|นาฬิกา|โมง)\b""", RegexOption.IGNORE_CASE)
        val suffixMatch = standaloneSuffixRegex.find(text)
        if (suffixMatch != null) {
            val h = suffixMatch.groupValues[1].toIntOrNull()
            val m = suffixMatch.groupValues[2].toIntOrNull()
            if (h != null && m != null && h in 0..23 && m in 0..59) {
                return suffixMatch.value.trim()
            }
        }

        // 5. Standalone valid HH:MM or HH.MM time
        val standaloneTimeRegex = Regex("""\b(\d{1,2})[:.](\d{2})\b""")
        val standaloneMatches = standaloneTimeRegex.findAll(text)
        for (m in standaloneMatches) {
            val h = m.groupValues[1].toIntOrNull()
            val min = m.groupValues[2].toIntOrNull()
            if (h != null && min != null && h in 0..23 && min in 0..59) {
                return m.value.trim()
            }
        }

        return null
    }

    private fun extractThaiPhoneNumber(text: String): String? {
        if (text.isBlank()) return null

        // Priority 1: Match keyword prefixes followed by phone number (e.g. "โทร 081-234-5678", "เบอร์: 0912345678", "tel: 021234567")
        val prefixRegex = Regex(
            """(?:โทร|เบอร์|เบอร์โทร|เบอร์ติดต่อ|ติดต่อ|มือถือ|tel|phone|mobile|call)\s*[:=]?\s*(\+?66[-\s]?|0)[23456789][0-9\s-]{7,12}""",
            RegexOption.IGNORE_CASE
        )
        val prefixMatch = prefixRegex.find(text)
        if (prefixMatch != null) {
            val rawMatch = prefixMatch.value
            val digitsOnly = rawMatch.replace(Regex("""[^\d+]"""), "")
            val normalized = normalizeThaiPhoneDigits(digitsOnly)
            if (normalized != null) return normalized
        }

        // Priority 2: Thai Mobile Numbers starting with 06, 08, 09 (10 digits)
        val mobileRegex = Regex("""(?:\+66|0)[689]\d{1}[-\s]?\d{3}[-\s]?\d{4}\b""")
        val mobileMatches = mobileRegex.findAll(text)
        for (match in mobileMatches) {
            val digits = match.value.replace(Regex("""[^\d+]"""), "")
            val normalized = normalizeThaiPhoneDigits(digits)
            if (normalized != null) return normalized
        }

        // Priority 3: Thai Landline Numbers starting with 02 (Bangkok 9 digits) or 03, 04, 05, 07 (Provincial 9 digits)
        val landlineRegex = Regex("""(?:\+66|0)[23457]\d{1}[-\s]?\d{3}[-\s]?\d{3,4}\b""")
        val landlineMatches = landlineRegex.findAll(text)
        for (match in landlineMatches) {
            val digits = match.value.replace(Regex("""[^\d+]"""), "")
            val normalized = normalizeThaiPhoneDigits(digits)
            if (normalized != null) return normalized
        }

        // Priority 4: Fallback sequence scan for 9 or 10 digit sequences starting with 06, 08, 09, 02, 03, 04, 05, 07
        val generalDigitRegex = Regex("""(?<!\d)(0[2345689]\d{7,8})(?!\d)""")
        val generalMatches = generalDigitRegex.findAll(text)
        for (match in generalMatches) {
            val normalized = normalizeThaiPhoneDigits(match.value)
            if (normalized != null) return normalized
        }

        return null
    }

    private fun normalizeThaiPhoneDigits(rawDigits: String): String? {
        var cleaned = rawDigits.replace(" ", "").replace("-", "")
        if (cleaned.startsWith("+66")) {
            cleaned = "0" + cleaned.substring(3)
        }

        // Thai Mobile: 10 digits starting with 06, 08, 09
        if (cleaned.length == 10 && (cleaned.startsWith("06") || cleaned.startsWith("08") || cleaned.startsWith("09"))) {
            return "${cleaned.substring(0, 3)}-${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
        }

        // Bangkok Landline: 9 digits starting with 02
        if (cleaned.length == 9 && cleaned.startsWith("02")) {
            return "${cleaned.substring(0, 2)}-${cleaned.substring(2, 5)}-${cleaned.substring(5)}"
        }

        // Provincial Landline: 9 digits starting with 03, 04, 05, 07
        if (cleaned.length == 9 && (cleaned.startsWith("03") || cleaned.startsWith("04") || cleaned.startsWith("05") || cleaned.startsWith("07"))) {
            return "${cleaned.substring(0, 3)}-${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
        }

        return null
    }

    fun openDialer(context: Context, phoneNumber: String) {
        try {
            val cleanPhone = phoneNumber.replace("-", "").replace(" ", "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ไม่สามารถเปิดแอปโทรศัพท์ได้", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGoogleMaps(context: Context, locationQuery: String) {
        try {
            val encodedQuery = Uri.encode(locationQuery)
            val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedQuery")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery"))
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(locationQuery)))
            context.startActivity(webIntent)
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "คัดลอก $label เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
    }

    fun openCalendarEvent(
        context: Context,
        customerName: String?,
        location: String?,
        rawText: String,
        onsiteTimeStr: String?,
        customDateTimestamp: Long? = null
    ) {
        try {
            val title = if (!customerName.isNullOrBlank()) "นัดหมายเข้า Onsite - $customerName" else "นัดหมายเข้า Onsite"
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                putExtra(android.provider.CalendarContract.Events.TITLE, title)
                if (!location.isNullOrBlank()) {
                    putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, location)
                }
                putExtra(android.provider.CalendarContract.Events.DESCRIPTION, rawText)

                val beginTime = parseBeginTimeMillis(onsiteTimeStr, customDateTimestamp)
                val endTime = parseEndTimeMillis(onsiteTimeStr, beginTime)
                putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ไม่สามารถเปิดแอปปฏิทินได้", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseEndTimeMillis(timeStr: String?, beginTime: Long): Long {
        if (timeStr.isNullOrBlank()) {
            return beginTime + (60 * 60 * 1000) // Default 1 hour later
        }

        try {
            val timeRegex = Regex("""(\d{1,2})[:.](\d{2})""")
            val matches = timeRegex.findAll(timeStr).toList()
            if (matches.size >= 2) {
                // There is a second time match, e.g. "11:00" in "10:00-11:00"
                val secondMatch = matches[1]
                val hour = secondMatch.groupValues[1].toIntOrNull()
                val min = secondMatch.groupValues[2].toIntOrNull()
                if (hour != null && min != null && hour in 0..23 && min in 0..59) {
                    val calendar = java.util.Calendar.getInstance().apply {
                        timeInMillis = beginTime
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, min)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    if (calendar.timeInMillis <= beginTime) {
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                    return calendar.timeInMillis
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        return beginTime + (60 * 60 * 1000) // Default 1 hour later
    }

    fun parseBeginTimeMillis(timeStr: String?, customDateTimestamp: Long? = null): Long {
        val calendar = java.util.Calendar.getInstance()
        if (customDateTimestamp != null) {
            calendar.timeInMillis = customDateTimestamp
        }
        if (timeStr.isNullOrBlank()) {
            if (customDateTimestamp == null) {
                calendar.add(java.util.Calendar.HOUR_OF_DAY, 1)
            }
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        try {
            val timeRegex = Regex("""(\d{1,2})[:.](\d{2})""")
            val match = timeRegex.find(timeStr)
            if (match != null) {
                val hour = match.groupValues[1].toIntOrNull()
                val min = match.groupValues[2].toIntOrNull()
                if (hour != null && min != null && hour in 0..23 && min in 0..59) {
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                    calendar.set(java.util.Calendar.MINUTE, min)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    return calendar.timeInMillis
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        if (customDateTimestamp == null) {
            calendar.add(java.util.Calendar.HOUR_OF_DAY, 1)
        }
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

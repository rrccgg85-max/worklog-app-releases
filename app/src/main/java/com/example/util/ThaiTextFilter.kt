package com.example.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object ThaiTextFilter {

    /**
     * Checks if a character is a Thai combining mark (vowels above/below, tone marks, diacritics).
     * Range: U+0E31 (ั), U+0E34..U+0E3A (ิ ี ึ ื ุ ู ฺ), U+0E47..U+0E4E (็ ่ ้ ๊ ๋ ์ ํ ๎)
     */
    fun isThaiCombiningMark(ch: Char): Boolean {
        val code = ch.code
        return code == 0x0E31 || (code in 0x0E34..0x0E3A) || (code in 0x0E47..0x0E4E)
    }

    /**
     * Intercepts TextFieldValue changes to prevent Android IME / Gboard from deleting
     * both base consonant and tone mark together (e.g. deleting "ม้" when pressing backspace on "ไม้").
     *
     * Ensures backspace deletes character-by-character (deleting tone mark first, then consonant).
     */
    fun processThaiBackspace(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        // Only process if single cursor selection (no text selection block highlighted)
        if (!oldValue.selection.collapsed) return newValue

        val oldText = oldValue.text
        val newText = newValue.text
        val oldCursor = oldValue.selection.start
        val newCursor = newValue.selection.start

        val lengthDiff = oldText.length - newText.length
        if (lengthDiff >= 2 && oldCursor > 0 && newCursor < oldCursor) {
            val prefixLen = newCursor
            val suffixLen = newText.length - newCursor

            val matchesPrefix = prefixLen <= oldText.length && oldText.startsWith(newText.substring(0, prefixLen))
            val matchesSuffix = suffixLen == 0 || (suffixLen <= oldText.length - oldCursor && oldText.endsWith(newText.substring(newCursor)))

            if (matchesPrefix && matchesSuffix) {
                val deletedStart = prefixLen
                val deletedEnd = oldText.length - suffixLen
                if (deletedStart < deletedEnd && deletedEnd <= oldText.length) {
                    val deletedSubstring = oldText.substring(deletedStart, deletedEnd)
                    if (deletedSubstring.isNotEmpty()) {
                        val lastDeletedChar = deletedSubstring.last()
                        if (isThaiCombiningMark(lastDeletedChar)) {
                            // Only delete the last character (the Thai combining mark)
                            val adjustedText = oldText.substring(0, deletedEnd - 1) + oldText.substring(deletedEnd)
                            val adjustedCursor = (deletedEnd - 1).coerceAtLeast(0)
                            return TextFieldValue(
                                text = adjustedText,
                                selection = TextRange(adjustedCursor),
                                composition = null
                            )
                        }
                    }
                }
            }
        }
        return newValue
    }

    /**
     * Helper for String state inputs.
     */
    fun processThaiBackspace(oldText: String, newText: String): String {
        val lengthDiff = oldText.length - newText.length
        if (lengthDiff >= 2 && oldText.isNotEmpty()) {
            val lastChar = oldText.last()
            if (isThaiCombiningMark(lastChar)) {
                return oldText.substring(0, oldText.length - 1)
            }
        }
        return newText
    }
}

package com.example.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ThaiTextFilterTest {

    @Test
    fun testThaiBackspace_Mai_DeletesOnlyToneMark() {
        // "ไม้" -> old len 3, selection at 3
        val oldValue = TextFieldValue("ไม้", selection = TextRange(3))
        // IME tries to delete "ม้" (len 2) leaving "ไ" (len 1, selection 1)
        val newValue = TextFieldValue("ไ", selection = TextRange(1))

        val result = ThaiTextFilter.processThaiBackspace(oldValue, newValue)

        // Expected: only '้' is deleted, leaving "ไม", selection at 2
        assertEquals("ไม", result.text)
        assertEquals(TextRange(2), result.selection)
    }

    @Test
    fun testThaiBackspace_SecondTime_DeletesConsonant() {
        // "ไม" -> old len 2, selection at 2
        val oldValue = TextFieldValue("ไม", selection = TextRange(2))
        // IME deletes "ม" leaving "ไ", selection at 1
        val newValue = TextFieldValue("ไ", selection = TextRange(1))

        val result = ThaiTextFilter.processThaiBackspace(oldValue, newValue)

        // Expected: normal delete of 'ม', leaving "ไ", selection at 1
        assertEquals("ไ", result.text)
        assertEquals(TextRange(1), result.selection)
    }

    @Test
    fun testThaiBackspace_StringHelper() {
        val oldText = "ไม้"
        val newText = "ไ"

        val result = ThaiTextFilter.processThaiBackspace(oldText, newText)

        assertEquals("ไม", result)
    }
}

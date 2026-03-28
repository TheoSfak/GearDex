package com.geardex.app.ui.logs

import java.text.SimpleDateFormat
import java.util.Locale

data class OcrResult(
    val odometer: Int?,
    val liters: Double?,
    val cost: Double?,
    val date: Long?
)

object OcrReceiptParser {

    private val kmPattern = Regex(
        """(\d{4,6})\s*(?:km|χλμ|klm|KM|ΧΛΜ)""",
        RegexOption.IGNORE_CASE
    )
    private val litersPattern = Regex(
        """(\d{1,3}[.,]\d{1,3})\s*(?:L|lt|ltr|litr|λίτ|lit)\b""",
        RegexOption.IGNORE_CASE
    )
    private val costPattern = Regex(
        """(?:€|EUR|TOTAL|ΣΥΝΟΛΟ|ευρώ)\s*[:\s]?\s*(\d{1,5}[.,]\d{1,2})""",
        RegexOption.IGNORE_CASE
    )
    private val costPatternAlt = Regex(
        """(\d{1,5}[.,]\d{2})\s*(?:€|EUR)""",
        RegexOption.IGNORE_CASE
    )
    private val datePattern = Regex(
        """(\d{2})[/\-.](\d{2})[/\-.](\d{4})"""
    )
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun parse(rawText: String): OcrResult {
        val odometer = kmPattern.find(rawText)?.groupValues?.get(1)?.toIntOrNull()

        val litersStr = litersPattern.find(rawText)?.groupValues?.get(1)
            ?.replace(',', '.')
        val liters = litersStr?.toDoubleOrNull()

        val costStr = (costPattern.find(rawText) ?: costPatternAlt.find(rawText))
            ?.groupValues?.get(1)
            ?.replace(',', '.')
        val cost = costStr?.toDoubleOrNull()

        val dateMatch = datePattern.find(rawText)
        val date = dateMatch?.let {
            val (day, month, year) = it.destructured
            runCatching { dateFormat.parse("$day/$month/$year")?.time }.getOrNull()
        }

        return OcrResult(
            odometer = odometer,
            liters = liters,
            cost = cost,
            date = date
        )
    }
}

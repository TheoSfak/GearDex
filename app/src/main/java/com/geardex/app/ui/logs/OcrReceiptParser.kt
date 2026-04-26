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

    // ── Keyword-based patterns (work when ML Kit reads the label) ────────

    private val kmPattern = Regex(
        """(\d{4,6})\s*(?:km|χλμ|klm|KM|ΧΛΜ)""",
        RegexOption.IGNORE_CASE
    )
    private val litersPattern = Regex(
        """(\d{1,3}[.,]\d{1,3})\s*(?:L|lt|ltr|litr|λίτ|lit)\b""",
        RegexOption.IGNORE_CASE
    )
    private val costPattern = Regex(
        """(?:€|EUR|TOTAL|ΣΥΝΟΛΟ|ευρώ|ΑΞΙΑ|ΠΛΗΡΩΤ|CASH|CARD|ΚΑΡΤΑ|ΜΕΤΡΗΤ)\s*[:\s]?\s*(\d{1,5}[.,]\d{1,2})""",
        RegexOption.IGNORE_CASE
    )
    private val costPatternAlt = Regex(
        """(\d{1,5}[.,]\d{2})\s*(?:€|EUR)""",
        RegexOption.IGNORE_CASE
    )
    private val datePattern = Regex(
        """(\d{2})[/\-.](\d{2})[/\-.](\d{4})"""
    )
    // ── Fallback: extract ALL decimal numbers from the text ──────────────

    private val allNumbersPattern = Regex("""(\d{1,5}[.,]\d{1,3})""")

    // Fuel price per liter in the 1.0–3.5 range (€/L) — skip these
    private fun isPricePerLiter(v: Double) = v in 1.0..3.5

    // Reasonable liters range for a single fill-up
    private fun isLikelyLiters(v: Double) = v in 3.0..150.0

    // Reasonable total cost range
    private fun isLikelyCost(v: Double) = v in 5.0..500.0

    fun parse(rawText: String): OcrResult {
        val odometer = kmPattern.find(rawText)?.groupValues?.get(1)?.toIntOrNull()

        // 1) Try keyword-based patterns first
        val litersStr = litersPattern.find(rawText)?.groupValues?.get(1)?.replace(',', '.')
        var liters = litersStr?.toDoubleOrNull()

        val costStr = (costPattern.find(rawText) ?: costPatternAlt.find(rawText))
            ?.groupValues?.get(1)?.replace(',', '.')
        var cost = costStr?.toDoubleOrNull()

        // 2) Fallback: if either is missing, use number heuristics
        //    Greek gas station receipts have 3 key numbers:
        //    price/L (~1.5-2.2), volume (~10-80 L), total cost (~20-150 €)
        if (liters == null || cost == null) {
            val candidates = allNumbersPattern.findAll(rawText)
                .map { it.groupValues[1].replace(',', '.') }
                .mapNotNull { it.toDoubleOrNull() }
                .filter { it > 0 }
                .toList()

            // Remove price-per-liter values and duplicates
            val useful = candidates.filter { !isPricePerLiter(it) }.distinct()

            if (cost == null) {
                // Cost is typically the largest value with exactly 2 decimal places
                cost = useful
                    .filter { isLikelyCost(it) }
                    .maxOrNull()
            }
            if (liters == null) {
                // Liters is typically in the 3-150 range but smaller than cost
                liters = useful
                    .filter { isLikelyLiters(it) && it != cost }
                    .let { possibles ->
                        // If we know cost, liters should be smaller
                        if (cost != null) possibles.filter { it < cost } else possibles
                    }
                    .maxOrNull()
            }
        }

        val dateMatch = datePattern.find(rawText)
        val date = dateMatch?.let {
            val (day, month, year) = it.destructured
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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

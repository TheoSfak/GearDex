package com.geardex.app.ui.logs

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class OcrResult(
    val odometer: Int?,
    val liters: Double?,
    val cost: Double?,
    val date: Long?,
    val unitPrice: Double?,
    val merchantName: String?,
    val serviceChecks: Set<String>
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
    private val shortDatePattern = Regex(
        """(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})"""
    )
    private val unitPricePattern = Regex(
        """(\d{1}[.,]\d{2,3})\s*(?:€/l|eur/l|e/l|/l|lt|liter|litre)""",
        RegexOption.IGNORE_CASE
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
        val normalizedText = rawText.lowercase(Locale.ROOT)

        // 1) Try keyword-based patterns first
        val litersStr = litersPattern.find(rawText)?.groupValues?.get(1)?.replace(',', '.')
        var liters = litersStr?.toDoubleOrNull()

        val unitPrice = unitPricePattern.find(rawText)
            ?.groupValues?.get(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()

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

            // Remove price-per-liter values and duplicates. Keep exact parsed unit price out too.
            val useful = candidates
                .filter { !isPricePerLiter(it) && it != unitPrice }
                .distinct()

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
        } ?: shortDatePattern.find(rawText)?.let {
            val (day, month, shortYear) = it.destructured
            val year = 2000 + (shortYear.toIntOrNull() ?: return@let null)
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, (month.toIntOrNull() ?: 1) - 1)
                set(Calendar.DAY_OF_MONTH, day.toIntOrNull() ?: 1)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val merchantName = extractMerchantName(rawText)
        val serviceChecks = buildSet {
            if (containsAny(normalizedText, "oil", "λάδι", "λαδι", "λιπαντικ", "lube")) add("oilChange")
            if (containsAny(normalizedText, "air filter", "φίλτρο αέρα", "φιλτρο αερα")) add("airFilter")
            if (containsAny(normalizedText, "brake", "φρέν", "φρεν", "τακάκ", "τακακ")) add("brakePads")
            if (containsAny(normalizedText, "timing", "ιμάντα", "ιμαντα", "belt")) add("timingBelt")
            if (containsAny(normalizedText, "cabin", "καμπίνας", "καμπινας", "pollen")) add("cabinFilter")
            if (containsAny(normalizedText, "chain", "αλυσίδ", "αλυσιδ")) add("chainLube")
            if (containsAny(normalizedText, "valve", "βαλβίδ", "βαλβιδ")) add("valveClearance")
            if (containsAny(normalizedText, "fork", "πιρούν", "πιρουν")) add("forkOil")
            if (containsAny(normalizedText, "tyre", "tire", "ελαστικ")) add("tireCheck")
        }

        return OcrResult(
            odometer = odometer,
            liters = liters,
            cost = cost,
            date = date,
            unitPrice = unitPrice,
            merchantName = merchantName,
            serviceChecks = serviceChecks
        )
    }

    private fun containsAny(text: String, vararg needles: String): Boolean {
        return needles.any { text.contains(it.lowercase(Locale.ROOT)) }
    }

    private fun extractMerchantName(rawText: String): String? {
        val ignored = listOf(
            "receipt", "invoice", "αποδειξη", "απόδειξη", "τιμολογιο", "τιμολόγιο",
            "date", "ημερομηνια", "ημερομηνία", "total", "συνολο", "σύνολο",
            "eur", "€", "vat", "φπα", "αφμ", "δελτιο", "δελτίο"
        )
        return rawText
            .lineSequence()
            .map { it.trim() }
            .filter { it.length in 3..40 }
            .filter { line -> ignored.none { line.lowercase(Locale.ROOT).contains(it) } }
            .firstOrNull { line -> line.any { it.isLetter() } }
    }
}

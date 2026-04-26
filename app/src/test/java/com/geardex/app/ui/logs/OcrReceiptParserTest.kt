package com.geardex.app.ui.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class OcrReceiptParserTest {

    @Test
    fun parsesFuelReceiptValues() {
        val result = OcrReceiptParser.parse(
            """
            AEGEAN STATION
            DATE 12/04/2026
            ODOMETER 126000 km
            PRICE 1.789 €/L
            VOLUME 42.18 L
            TOTAL EUR 75.45
            """.trimIndent()
        )

        assertEquals(126000, result.odometer)
        assertEquals(42.18, result.liters!!, 0.001)
        assertEquals(75.45, result.cost!!, 0.001)
        assertEquals(1.789, result.unitPrice!!, 0.001)
        assertEquals("AEGEAN STATION", result.merchantName)
        assertEquals(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse("12/04/2026")?.time,
            result.date
        )
    }

    @Test
    fun parsesGreekServiceReceiptValuesAndChecks() {
        val result = OcrReceiptParser.parse(
            """
            Συνεργείο Γιώργος
            Ημερομηνία 05-03-26
            88000 χλμ
            Αλλαγή λάδι
            Φίλτρο αέρα
            Τακάκια φρένων
            ΣΥΝΟΛΟ 180,00
            """.trimIndent()
        )

        assertEquals(88000, result.odometer)
        assertEquals(180.0, result.cost!!, 0.001)
        assertEquals("Συνεργείο Γιώργος", result.merchantName)
        assertTrue("oilChange" in result.serviceChecks)
        assertTrue("airFilter" in result.serviceChecks)
        assertTrue("brakePads" in result.serviceChecks)
    }
}

package com.geardex.app.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfReportGenerator @Inject constructor(@ApplicationContext private val context: Context) {

    private val pageWidth = 595  // A4 in points
    private val pageHeight = 842
    private val margin = 40f
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private val titlePaint = Paint().apply {
        color = Color.parseColor("#1976D2")
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        color = Color.parseColor("#333333")
        textSize = 16f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#444444")
        textSize = 11f
        isAntiAlias = true
    }

    private val smallPaint = Paint().apply {
        color = Color.parseColor("#666666")
        textSize = 9f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#DDDDDD")
        strokeWidth = 1f
    }

    private val accentPaint = Paint().apply {
        color = Color.parseColor("#1976D2")
        textSize = 11f
        isFakeBoldText = true
        isAntiAlias = true
    }

    fun generateVehicleReport(
        vehicle: Vehicle,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        healthScore: Int,
        costPerKm: Double?,
        totalFuelCost: Double?,
        monthlyAvg: Double?
    ): File {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 30f

        // Title
        canvas.drawText("GearDex — Vehicle Report", margin, y, titlePaint)
        y += 14f
        canvas.drawText(dateTimeFormat.format(Date()), margin, y + 12f, smallPaint)
        y += 30f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        // Vehicle info
        canvas.drawText("Vehicle Information", margin, y, headerPaint)
        y += 22f
        val typeStr = when (vehicle.type) {
            VehicleType.CAR -> "Car"
            VehicleType.MOTORCYCLE -> "Motorcycle"
            VehicleType.ATV -> "ATV"
        }
        val vehicleInfo = listOf(
            "Name" to "${vehicle.make} ${vehicle.model}",
            "Type" to typeStr,
            "Year" to vehicle.year.toString(),
            "License Plate" to vehicle.licensePlate,
            "Current KM" to "%,d km".format(vehicle.currentKm),
            "Health Score" to "$healthScore / 100"
        )
        for ((label, value) in vehicleInfo) {
            canvas.drawText("$label:", margin + 8f, y, accentPaint)
            canvas.drawText(value, margin + 120f, y, textPaint)
            y += 16f
        }

        // Cost summary
        if (costPerKm != null || totalFuelCost != null || monthlyAvg != null) {
            y += 10f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 20f
            canvas.drawText("Cost Summary", margin, y, headerPaint)
            y += 22f
            if (costPerKm != null) {
                canvas.drawText("Cost/KM:", margin + 8f, y, accentPaint)
                canvas.drawText("€%.4f".format(costPerKm), margin + 120f, y, textPaint)
                y += 16f
            }
            if (totalFuelCost != null) {
                canvas.drawText("Total Fuel:", margin + 8f, y, accentPaint)
                canvas.drawText("€%.2f".format(totalFuelCost), margin + 120f, y, textPaint)
                y += 16f
            }
            if (monthlyAvg != null) {
                canvas.drawText("Monthly Avg:", margin + 8f, y, accentPaint)
                canvas.drawText("€%.2f".format(monthlyAvg), margin + 120f, y, textPaint)
                y += 16f
            }
        }

        // Fuel logs table
        if (fuelLogs.isNotEmpty()) {
            y += 10f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 20f
            canvas.drawText("Fuel Logs (${fuelLogs.size})", margin, y, headerPaint)
            y += 22f

            // Table header
            val cols = listOf("Date", "Odometer", "Liters", "Cost", "L/100km")
            val colX = listOf(margin + 8f, margin + 108f, margin + 208f, margin + 298f, margin + 388f)
            cols.forEachIndexed { i, col ->
                canvas.drawText(col, colX[i], y, accentPaint)
            }
            y += 4f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 14f

            val sortedFuel = fuelLogs.sortedByDescending { it.date }
            for (log in sortedFuel) {
                if (y > pageHeight - margin - 30f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 20f
                }
                canvas.drawText(dateFormat.format(Date(log.date)), colX[0], y, textPaint)
                canvas.drawText("%,d km".format(log.odometer), colX[1], y, textPaint)
                canvas.drawText("%.2f L".format(log.liters), colX[2], y, textPaint)
                canvas.drawText("€%.2f".format(log.cost), colX[3], y, textPaint)
                canvas.drawText(log.fuelEconomy?.let { "%.1f".format(it) } ?: "—", colX[4], y, textPaint)
                y += 14f
            }
        }

        // Service logs table
        if (serviceLogs.isNotEmpty()) {
            y += 10f
            if (y > pageHeight - margin - 60f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 20f
            }
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 20f
            canvas.drawText("Service Logs (${serviceLogs.size})", margin, y, headerPaint)
            y += 22f

            val sCols = listOf("Date", "Odometer", "Cost", "Services")
            val sColX = listOf(margin + 8f, margin + 108f, margin + 208f, margin + 298f)
            sCols.forEachIndexed { i, col ->
                canvas.drawText(col, sColX[i], y, accentPaint)
            }
            y += 4f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 14f

            val sortedService = serviceLogs.sortedByDescending { it.date }
            for (log in sortedService) {
                if (y > pageHeight - margin - 30f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 20f
                }
                canvas.drawText(dateFormat.format(Date(log.date)), sColX[0], y, textPaint)
                canvas.drawText("%,d km".format(log.odometer), sColX[1], y, textPaint)
                canvas.drawText("€%.2f".format(log.cost), sColX[2], y, textPaint)
                canvas.drawText(getServiceTypes(log), sColX[3], y, textPaint)
                y += 14f
                if (log.notes.isNotEmpty()) {
                    canvas.drawText("  ${log.notes}", sColX[0], y, smallPaint)
                    y += 12f
                }
            }
        }

        // Footer
        y = pageHeight - margin
        canvas.drawLine(margin, y - 14f, pageWidth - margin, y - 14f, linePaint)
        canvas.drawText("Generated by GearDex", margin, y, smallPaint)

        document.finishPage(page)

        val fileName = "GearDex_${vehicle.make}_${vehicle.model}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    fun generateAllVehiclesReport(
        vehicles: List<Vehicle>,
        fuelLogsByVehicle: Map<Long, List<FuelLog>>,
        serviceLogsByVehicle: Map<Long, List<ServiceLog>>
    ): File {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 30f

        // Title page
        canvas.drawText("GearDex — Full Report", margin, y, titlePaint)
        y += 14f
        canvas.drawText(dateTimeFormat.format(Date()), margin, y + 12f, smallPaint)
        y += 14f
        canvas.drawText("${vehicles.size} vehicles", margin, y + 12f, smallPaint)
        y += 30f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        for (vehicle in vehicles) {
            val fuelLogs = fuelLogsByVehicle[vehicle.id] ?: emptyList()
            val serviceLogs = serviceLogsByVehicle[vehicle.id] ?: emptyList()

            // Check if we need a new page
            if (y > pageHeight - margin - 120f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 20f
            }

            // Vehicle header
            canvas.drawText("${vehicle.make} ${vehicle.model} (${vehicle.year})", margin, y, headerPaint)
            y += 18f
            canvas.drawText("${vehicle.licensePlate}  ·  %,d km".format(vehicle.currentKm), margin + 8f, y, textPaint)
            y += 20f

            // Fuel summary
            if (fuelLogs.isNotEmpty()) {
                val totalFuelCost = fuelLogs.sumOf { it.cost }
                val totalLiters = fuelLogs.sumOf { it.liters }
                val avgEconomy = fuelLogs.mapNotNull { it.fuelEconomy }.average().takeIf { !it.isNaN() }
                canvas.drawText(
                    "Fuel: ${fuelLogs.size} entries · %.1f L · €%.2f".format(totalLiters, totalFuelCost) +
                            (avgEconomy?.let { " · Avg: %.1f L/100km".format(it) } ?: ""),
                    margin + 8f, y, textPaint
                )
                y += 16f
            }

            // Service summary
            if (serviceLogs.isNotEmpty()) {
                val totalServiceCost = serviceLogs.sumOf { it.cost }
                canvas.drawText(
                    "Service: ${serviceLogs.size} entries · €%.2f total".format(totalServiceCost),
                    margin + 8f, y, textPaint
                )
                y += 16f
            }

            if (fuelLogs.isEmpty() && serviceLogs.isEmpty()) {
                canvas.drawText("No logs recorded", margin + 8f, y, smallPaint)
                y += 16f
            }

            y += 6f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 16f
        }

        // Footer
        if (y > pageHeight - margin - 20f) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
        }
        val footerY = pageHeight - margin
        canvas.drawLine(margin, footerY - 14f, pageWidth - margin, footerY - 14f, linePaint)
        canvas.drawText("Generated by GearDex", margin, footerY.toFloat(), smallPaint)

        document.finishPage(page)

        val fileName = "GearDex_Full_Report_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun getServiceTypes(log: ServiceLog): String {
        val types = mutableListOf<String>()
        if (log.oilChange) types.add("Oil")
        if (log.airFilter) types.add("Air Filter")
        if (log.brakePads) types.add("Brakes")
        if (log.timingBelt) types.add("Timing Belt")
        if (log.cabinFilter) types.add("Cabin Filter")
        if (log.chainLube) types.add("Chain")
        if (log.valveClearance) types.add("Valves")
        if (log.forkOil) types.add("Fork Oil")
        if (log.tireCheck) types.add("Tires")
        return types.joinToString(", ").ifEmpty { "General" }
    }
}

package com.geardex.app.notifications

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.di.WidgetEntryPoint
import com.geardex.app.ui.garage.HealthScoreCalculator
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GearDexWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val vehicles = entryPoint.vehicleRepository().getAllVehicles().first()
        val activeReminders = entryPoint.reminderRepository().getActiveRemindersFlow().first()
        val allServiceLogs = entryPoint.logRepository().getAllServiceLogs().first()

        val vehicle = vehicles.firstOrNull()

        val score = vehicle?.let { v ->
            val lastLog = allServiceLogs
                .filter { it.vehicleId == v.id }
                .maxByOrNull { it.odometer }
            val vehicleReminders = activeReminders.filter { it.vehicleId == v.id }
            HealthScoreCalculator.compute(v, lastLog, vehicleReminders)
        }

        val nearestReminder = vehicle?.let { v ->
            activeReminders
                .filter { it.vehicleId == v.id }
                .minByOrNull { it.targetDate ?: Long.MAX_VALUE }
        }

        provideContent {
            WidgetContent(vehicle, score, nearestReminder)
        }
    }

    @Composable
    private fun WidgetContent(
        vehicle: Vehicle?,
        score: Int?,
        nearestReminder: MaintenanceReminder?
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF242424))
                .padding(12.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (vehicle == null) {
                Text(
                    text = "No vehicles added",
                    style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 14.sp)
                )
            } else {
                Column(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()) {

                    // Vehicle name
                    Text(
                        text = "${vehicle.make} ${vehicle.model}",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color.White),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Score row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val scoreColor = when {
                            (score ?: 100) >= 80 -> Color(0xFF4CAF50)
                            (score ?: 100) >= 50 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                        Text(
                            text = "Health: ${score ?: "—"}",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(scoreColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Reminder row
                    val reminderText = if (nearestReminder != null) {
                        val dateStr = nearestReminder.targetDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it))
                        }
                        "${nearestReminder.title}${if (dateStr != null) " · $dateStr" else ""}"
                    } else {
                        "All clear ✓"
                    }

                    Text(
                        text = reminderText,
                        modifier = GlanceModifier.padding(top = 6.dp),
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color(0xFFB0B0B0)),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

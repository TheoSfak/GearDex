package com.geardex.app.ui.garage

import android.content.Context
import com.geardex.app.R
import com.geardex.app.data.local.entity.ServicePlan
import com.geardex.app.data.local.entity.ServicePlanType
import com.geardex.app.data.repository.ServicePlanStatus
import java.util.Locale
import kotlin.math.abs

fun Context.servicePlanTypeLabel(type: ServicePlanType): String = when (type) {
    ServicePlanType.OIL_CHANGE -> getString(R.string.service_oil_change)
    ServicePlanType.AIR_FILTER -> getString(R.string.service_air_filter)
    ServicePlanType.BRAKE_PADS -> getString(R.string.service_brake_pads)
    ServicePlanType.TIMING_BELT -> getString(R.string.service_timing_belt)
    ServicePlanType.CABIN_FILTER -> getString(R.string.service_cabin_filter)
    ServicePlanType.CHAIN_LUBE -> getString(R.string.service_chain_lube)
    ServicePlanType.VALVE_CLEARANCE -> getString(R.string.service_valve_clearance)
    ServicePlanType.FORK_OIL -> getString(R.string.service_fork_oil)
    ServicePlanType.TIRE_CHECK -> getString(R.string.service_tire_check)
    ServicePlanType.CUSTOM -> getString(R.string.service_plan_custom)
}

fun Context.servicePlanTitle(plan: ServicePlan): String =
    if (plan.type == ServicePlanType.CUSTOM) plan.title else servicePlanTypeLabel(plan.type)

fun Context.servicePlanStatusLabel(status: ServicePlanStatus): String = when (status) {
    ServicePlanStatus.OK -> getString(R.string.service_plan_status_ok)
    ServicePlanStatus.SOON -> getString(R.string.service_plan_status_soon)
    ServicePlanStatus.DUE -> getString(R.string.service_plan_status_due)
    ServicePlanStatus.OVERDUE -> getString(R.string.service_plan_status_overdue)
}

fun Context.servicePlanDistanceText(kmRemaining: Int?): String? = kmRemaining?.let {
    if (it < 0) {
        getString(R.string.service_plan_overdue_km, abs(it).formatNumber())
    } else {
        getString(R.string.service_plan_remaining_km, it.formatNumber())
    }
}

fun Context.servicePlanDateText(daysRemaining: Long?): String? = daysRemaining?.let {
    if (it < 0) {
        getString(R.string.service_plan_overdue_days, abs(it))
    } else {
        getString(R.string.service_plan_remaining_days, it)
    }
}

private fun Int.formatNumber(): String =
    String.format(Locale.getDefault(), "%,d", this)

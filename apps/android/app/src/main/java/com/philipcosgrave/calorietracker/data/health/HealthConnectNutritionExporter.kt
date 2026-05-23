package com.philipcosgrave.calorietracker.data.health

import android.content.Context
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.philipcosgrave.calorietracker.domain.scale
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.Meal
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class HealthConnectAvailability {
    Available,
    UpdateRequired,
    Unavailable,
}

class HealthConnectNutritionExporter(private val context: Context) {
    companion object {
        private const val HEALTH_CONNECT_PACKAGE_NAME = "com.google.android.apps.healthdata"

        val writeNutritionPermission: String =
            HealthPermission.getWritePermission(NutritionRecord::class)

        val requestedPermissions: Set<String> = setOf(writeNutritionPermission)

        fun onboardingUri(): Uri =
            Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE_NAME&url=healthconnect%3A%2F%2Fonboarding")
    }

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE_NAME)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UpdateRequired
            else -> HealthConnectAvailability.Unavailable
        }

    suspend fun grantedPermissions(): Set<String> =
        if (availability() == HealthConnectAvailability.Available) {
            client.permissionController.getGrantedPermissions()
        } else {
            emptySet()
        }

    suspend fun hasWriteNutritionPermission(): Boolean =
        writeNutritionPermission in grantedPermissions()

    suspend fun exportEntry(record: DiaryEntryRecord) {
        if (!hasWriteNutritionPermission()) return

        val entry = record.entry
        val nutrients = entry.food.nutrients.scale(entry.servingMultiplier)
        val zoneId = ZoneId.systemDefault()
        val startTime = ZonedDateTime.of(entry.date, LocalTime.NOON, zoneId)
        val endTime = startTime.plusMinutes(15)
        val mealType = when (entry.meal) {
            Meal.Breakfast -> MealType.MEAL_TYPE_BREAKFAST
            Meal.Lunch -> MealType.MEAL_TYPE_LUNCH
            Meal.Dinner -> MealType.MEAL_TYPE_DINNER
            Meal.Snack -> MealType.MEAL_TYPE_SNACK
        }

        val healthConnectRecord = NutritionRecord(
            startTime = startTime.toInstant(),
            startZoneOffset = startTime.offset,
            endTime = endTime.toInstant(),
            endZoneOffset = endTime.offset,
            metadata = Metadata.manualEntry(entry.id, record.sync.version.toLong()),
            mealType = mealType,
            name = entry.food.name,
            energy = Energy.calories(nutrients.calories),
            protein = Mass.grams(nutrients.proteinGrams),
            totalCarbohydrate = Mass.grams(nutrients.carbohydrateGrams),
            totalFat = Mass.grams(nutrients.fatGrams),
        )

        client.insertRecords(listOf(healthConnectRecord))
    }

    suspend fun deleteEntry(entryId: String) {
        if (!hasWriteNutritionPermission()) return
        client.deleteRecords(
            NutritionRecord::class,
            emptyList(),
            listOf(entryId),
        )
    }
}

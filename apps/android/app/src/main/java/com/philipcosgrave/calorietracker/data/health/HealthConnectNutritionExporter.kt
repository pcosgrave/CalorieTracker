package com.philipcosgrave.calorietracker.data.health

import android.content.Context
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.philipcosgrave.calorietracker.domain.scale
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.HealthDashboardMetrics
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.WeightEntry
import java.time.LocalTime
import java.time.LocalDate
import java.time.ZoneOffset
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
        val readNutritionPermission: String =
            HealthPermission.getReadPermission(NutritionRecord::class)
        val readStepsPermission: String =
            HealthPermission.getReadPermission(StepsRecord::class)
        val readHeartRatePermission: String =
            HealthPermission.getReadPermission(HeartRateRecord::class)
        val readCaloriesBurnedPermission: String =
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        val writeWeightPermission: String =
            HealthPermission.getWritePermission(WeightRecord::class)
        val readWeightPermission: String =
            HealthPermission.getReadPermission(WeightRecord::class)

        val requestedPermissions: Set<String> = setOf(
            writeNutritionPermission,
            readNutritionPermission,
            writeWeightPermission,
            readWeightPermission,
            readStepsPermission,
            readHeartRatePermission,
            readCaloriesBurnedPermission,
        )

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

    suspend fun hasWriteWeightPermission(): Boolean =
        writeWeightPermission in grantedPermissions()

    suspend fun hasReadNutritionPermission(): Boolean =
        readNutritionPermission in grantedPermissions()

    suspend fun hasRequestedPermissions(): Boolean =
        requestedPermissions.all { it in grantedPermissions() }

    suspend fun readTodayMetrics(): HealthDashboardMetrics {
        if (!hasRequestedPermissions()) return HealthDashboardMetrics()

        val zoneId = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
        val end = java.time.Instant.now()
        val aggregateResponse = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        val heartRateResponse = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 10,
            ),
        )
        val latestHeartRate = heartRateResponse.records
            .flatMap { it.samples }
            .maxByOrNull { it.time }
            ?.beatsPerMinute
            ?.toLong()

        return HealthDashboardMetrics(
            steps = aggregateResponse[StepsRecord.COUNT_TOTAL],
            heartRateBpm = latestHeartRate,
            caloriesBurned = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inCalories,
        )
    }

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
            startZoneOffset = startTime.offset ?: ZoneOffset.UTC,
            endTime = endTime.toInstant(),
            endZoneOffset = endTime.offset ?: ZoneOffset.UTC,
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

    suspend fun exportWeightEntry(entry: WeightEntry) {
        if (!hasWriteWeightPermission()) return

        val zoneId = ZoneId.systemDefault()
        val loggedAt = ZonedDateTime.of(entry.date, LocalTime.NOON, zoneId)
        val record = WeightRecord(
            time = loggedAt.toInstant(),
            zoneOffset = loggedAt.offset ?: ZoneOffset.UTC,
            metadata = Metadata.manualEntry(entry.id, entry.date.toEpochDay()),
            weight = Mass.kilograms(entry.weightKg),
        )

        client.insertRecords(listOf(record))
    }

    suspend fun deleteWeightEntry(entryId: String) {
        if (!hasWriteWeightPermission()) return
        client.deleteRecords(
            WeightRecord::class,
            emptyList(),
            listOf(entryId),
        )
    }

    suspend fun importWeightEntries(): List<WeightEntry> {
        if (readWeightPermission !in grantedPermissions()) return emptyList()

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    java.time.Instant.now(),
                ),
                ascendingOrder = true,
                pageSize = 1000,
            ),
        )
        return response.records.map { record ->
            WeightEntry(
                id = record.metadata.clientRecordId ?: "hc-weight-${record.metadata.id}",
                date = record.time.atZone(ZoneId.systemDefault()).toLocalDate(),
                weightKg = record.weight.inKilograms,
            )
        }
    }

    suspend fun importNutritionEntries(): List<DiaryEntry> {
        if (!hasReadNutritionPermission()) return emptyList()

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = NutritionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    java.time.Instant.now(),
                ),
                ascendingOrder = true,
                pageSize = 1000,
            ),
        )
        return response.records.map { record ->
            DiaryEntry(
                id = record.metadata.clientRecordId ?: "hc-nutrition-${record.metadata.id}",
                food = FoodItem(
                    id = "hc-food-${record.metadata.clientRecordId ?: record.metadata.id}",
                    kind = FoodKind.Ingredient,
                    name = record.name.orEmpty().ifBlank { "Imported meal" },
                    servingQuantity = 1.0,
                    servingUnit = "entry",
                    nutrients = Nutrients(
                        calories = record.energy?.inCalories ?: 0.0,
                        proteinGrams = record.protein?.inGrams ?: 0.0,
                        carbohydrateGrams = record.totalCarbohydrate?.inGrams ?: 0.0,
                        fatGrams = record.totalFat?.inGrams ?: 0.0,
                    ),
                ),
                date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                meal = when (record.mealType) {
                    MealType.MEAL_TYPE_BREAKFAST -> Meal.Breakfast
                    MealType.MEAL_TYPE_LUNCH -> Meal.Lunch
                    MealType.MEAL_TYPE_DINNER -> Meal.Dinner
                    else -> Meal.Snack
                },
                servingMultiplier = 1.0,
            )
        }
    }
}

package com.philipcosgrave.calorietracker.data.health

import android.content.Context
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_HISTORY
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
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
import java.time.Duration
import kotlin.reflect.KClass

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

        val baseRequestedPermissions: Set<String> = setOf(
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
        requestedPermissions().all { it in grantedPermissions() }

    fun isHistoryPermissionAvailable(): Boolean =
        availability() == HealthConnectAvailability.Available &&
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    fun requestedPermissions(): Set<String> =
        buildSet {
            addAll(baseRequestedPermissions)
            if (isHistoryPermissionAvailable()) {
                add(PERMISSION_READ_HEALTH_DATA_HISTORY)
            }
        }

    suspend fun readTodayMetrics(): HealthDashboardMetrics {
        val granted = grantedPermissions()
        if (granted.isEmpty()) return HealthDashboardMetrics()

        val zoneId = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
        val end = java.time.Instant.now()
        val heartRateLookbackStart = end.minus(Duration.ofHours(48))
        val aggregateMetrics = buildSet {
            if (readStepsPermission in granted) add(StepsRecord.COUNT_TOTAL)
        }
        val aggregateResponse =
            if (aggregateMetrics.isNotEmpty()) {
                client.aggregate(
                    AggregateRequest(
                        metrics = aggregateMetrics,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                )
            } else {
                null
            }
        val caloriesBurnedResponse =
            if (readCaloriesBurnedPermission in granted) {
                readAllRecords(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        start.minusSeconds(3 * 24 * 60 * 60),
                        end,
                    ),
                    ascendingOrder = false,
                    pageSize = 100,
                )
            } else {
                null
            }
        val heartRateResponse =
            if (readHeartRatePermission in granted) {
                readAllRecords(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(heartRateLookbackStart, end),
                    ascendingOrder = false,
                    pageSize = 100,
                )
            } else {
                null
            }
        val allHeartRateSamples = heartRateResponse
            ?.flatMap { it.samples }
            .orEmpty()
        val latestHeartRate = allHeartRateSamples
            ?.maxByOrNull { it.time }
            ?.beatsPerMinute
            ?.toLong()
        val todayHeartRateSamples = allHeartRateSamples.filter { sample ->
            sample.time.atZone(zoneId).toLocalDate() == LocalDate.now(zoneId)
        }
        val averageHeartRateToday = todayHeartRateSamples.takeIf { it.isNotEmpty() }
            ?.map { it.beatsPerMinute }
            ?.average()
            ?.toLong()
        val minHeartRateToday = todayHeartRateSamples.minOfOrNull { it.beatsPerMinute }?.toLong()
        val maxHeartRateToday = todayHeartRateSamples.maxOfOrNull { it.beatsPerMinute }?.toLong()
        val newestHeartRateSampleTime = allHeartRateSamples.maxByOrNull { it.time }?.time?.atZone(zoneId)
        val oldestHeartRateSampleTime = allHeartRateSamples.minByOrNull { it.time }?.time?.atZone(zoneId)
        val heartRateOrigins = heartRateResponse
            ?.map { it.metadata.dataOrigin.packageName }
            ?.distinct()
            ?.sorted()
            .orEmpty()
        val todayCaloriesBurnedRecords = caloriesBurnedResponse
            ?.filter { record ->
                record.endTime.atZone(zoneId).toLocalDate() == LocalDate.now(zoneId)
            }
            .orEmpty()
        val totalCaloriesBurnedToday = todayCaloriesBurnedRecords
            .sumOf { it.energy.inKilocalories }
            .takeIf { it > 0.0 }
        val latestRecentTotalCalories = caloriesBurnedResponse?.firstOrNull()?.energy?.inKilocalories

        return HealthDashboardMetrics(
            steps = aggregateResponse?.get(StepsRecord.COUNT_TOTAL),
            heartRateBpm = latestHeartRate ?: averageHeartRateToday,
            caloriesBurned = totalCaloriesBurnedToday ?: latestRecentTotalCalories,
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
            energy = Energy.kilocalories(nutrients.calories),
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
        check(hasWriteWeightPermission()) { "Health Connect WRITE_WEIGHT permission not granted" }

        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        val loggedAt =
            if (entry.date == now.toLocalDate()) {
                now.minusMinutes(1)
            } else {
                ZonedDateTime.of(entry.date, LocalTime.NOON, zoneId)
            }
        val record = WeightRecord(
            time = loggedAt.toInstant(),
            zoneOffset = loggedAt.offset ?: ZoneOffset.UTC,
            metadata = Metadata.manualEntry(entry.id, System.currentTimeMillis()),
            weight = Mass.kilograms(entry.weightKg),
        )

        client.insertRecords(listOf(record))
    }

    suspend fun deleteWeightEntry(entryId: String) {
        check(hasWriteWeightPermission()) { "Health Connect WRITE_WEIGHT permission not granted" }
        client.deleteRecords(
            WeightRecord::class,
            emptyList(),
            listOf(entryId),
        )
    }

    suspend fun importWeightEntries(): List<WeightEntry> {
        if (readWeightPermission !in grantedPermissions()) return emptyList()

        val records = readAllRecords(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                java.time.Instant.now(),
            ),
        )
        return records.map { record ->
            WeightEntry(
                id = record.metadata.clientRecordId ?: "hc-weight-${record.metadata.id}",
                date = record.time.atZone(ZoneId.systemDefault()).toLocalDate(),
                weightKg = record.weight.inKilograms,
            )
        }
    }

    suspend fun importNutritionEntries(): List<DiaryEntry> {
        if (!hasReadNutritionPermission()) return emptyList()

        val records = readAllRecords(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                java.time.Instant.now(),
            ),
        )
        return records.map { record ->
            DiaryEntry(
                id = record.metadata.clientRecordId ?: "hc-nutrition-${record.metadata.id}",
                food = FoodItem(
                    id = "hc-food-${record.metadata.clientRecordId ?: record.metadata.id}",
                    kind = FoodKind.Ingredient,
                    name = record.name.orEmpty().ifBlank { "Imported meal" },
                    servingQuantity = 1.0,
                    servingUnit = "entry",
                    nutrients = Nutrients(
                        calories = record.energy?.inKilocalories ?: 0.0,
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
                loggedAmount = 1.0,
                loggedUnit = "entry",
            )
        }
    }

    private suspend fun <T : Record> readAllRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        ascendingOrder: Boolean = true,
        pageSize: Int = 1000,
    ): List<T> {
        val allRecords = mutableListOf<T>()
        var pageToken: String? = null

        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = timeRangeFilter,
                    ascendingOrder = ascendingOrder,
                    pageSize = pageSize,
                    pageToken = pageToken,
                ),
            )
            allRecords.addAll(response.records)
            pageToken = response.pageToken
        } while (pageToken != null)

        return allRecords
    }
}

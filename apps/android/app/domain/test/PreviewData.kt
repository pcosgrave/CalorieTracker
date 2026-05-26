package com.philipcosgrave.calorietracker.ui.preview

import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.domain.createSyncMetadata
import com.philipcosgrave.calorietracker.domain.nowIsoString
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.domain.totalsForEntries
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object PreviewData {

    val totals = Totals(
        calories = 1650.0,
        protein = 120.0,
        carbs = 180.0,
        fat = 60.0
    )

    val healthMetrics = HealthDashboardMetrics(
        steps = 8500L,
        heartRateBpm = 72L,
        caloriesBurned = 450.0
    )

    val weightEntries = listOf(
        WeightEntry(
            id = "weight-1",
            date = LocalDate.now().minusDays(7),
            weightKg = 70.5
        ),
        WeightEntry(
            id = "weight-2",
            date = LocalDate.now().minusDays(5),
            weightKg = 70.2
        ),
        WeightEntry(
            id = "weight-3",
            date = LocalDate.now().minusDays(3),
            weightKg = 70.0
        ),
        WeightEntry(
            id = "weight-4",
            date = LocalDate.now().minusDays(1),
            weightKg = 69.8
        ),
        WeightEntry(
            id = "weight-5",
            date = LocalDate.now(),
            weightKg = 69.5
        )
    )

    val diaryEntries = listOf(
        DiaryEntry(
            id = createId("entry"),
            food = FoodItem(
                id = createId("food"),
                kind = FoodKind.Ingredient,
                name = "Oatmeal with Berries",
                servingQuantity = 1.0,
                servingUnit = "serving",
                nutrients = Nutrients(320.0, 8.0, 55.0, 5.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        ),
        DiaryEntry(
            id = createId("entry"),
            food = FoodItem(
                id = createId("food"),
                kind = FoodKind.Ingredient,
                name = "Scrambled Eggs",
                servingQuantity = 2.0,
                servingUnit = "large",
                nutrients = Nutrients(140.0, 12.0, 1.0, 10.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        ),
        DiaryEntry(
            id = createId("entry"),
            food = FoodItem(
                id = createId("food"),
                kind = FoodKind.Ingredient,
                name = "Grilled Chicken Breast",
                servingQuantity = 200.0,
                servingUnit = "gram",
                nutrients = Nutrients(250.0, 45.0, 0.0, 5.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        ),
        DiaryEntry(
            id = createId("entry"),
            food = FoodItem(
                id = createId("food"),
                kind = FoodKind.Ingredient,
                name = "Quinoa Salad",
                servingQuantity = 150.0,
                servingUnit = "gram",
                nutrients = Nutrients(210.0, 6.0, 35.0, 4.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Dinner,
            servingMultiplier = 1.0
        ),
        DiaryEntry(
            id = createId("entry"),
            food = FoodItem(
                id = createId("food"),
                kind = FoodKind.Ingredient,
                name = "Greek Yogurt",
                servingQuantity = 170.0,
                servingUnit = "gram",
                nutrients = Nutrients(130.0, 15.0, 9.0, 2.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Snack,
            servingMultiplier = 1.0
        ),
        DiaryEntry(
            id = createId("entry"),
            food = FoodItem(
                id = createId("food"),
                kind = FoodKind.Ingredient,
                name = "Apple",
                servingQuantity = 150.0,
                servingUnit = "gram",
                nutrients = Nutrients(52.0, 0.3, 14.0, 0.3)
            ),
            date = LocalDate.now(),
            meal = Meal.Snack,
            servingMultiplier = 1.0
        )
    )

    val foods = listOf(
        FoodItem(
            id = createId("food"),
            kind = FoodKind.Ingredient,
            name = "Apple",
            barcode = "093469000000",
            servingQuantity = 150.0,
            servingUnit = "gram",
            nutrients = Nutrients(52.0, 0.3, 14.0, 0.3),
            frequency = 5,
            lastUsedDaysAgo = 2
        ),
        FoodItem(
            id = createId("food"),
            kind = FoodKind.Ingredient,
            name = "Banana",
            barcode = "072049100000",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(89.0, 1.1, 23.0, 0.3),
            frequency = 8,
            lastUsedDaysAgo = 1
        ),
        FoodItem(
            id = createId("food"),
            kind = FoodKind.Ingredient,
            name = "Chicken Breast",
            barcode = "073495000000",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(165.0, 31.0, 0.0, 3.6),
            frequency = 12,
            lastUsedDaysAgo = 0
        ),
        FoodItem(
            id = createId("food"),
            kind = FoodKind.Ingredient,
            name = "Rice",
            barcode = "072049100001",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(130.0, 2.6, 28.0, 0.3),
            frequency = 10,
            lastUsedDaysAgo = 1
        ),
        FoodItem(
            id = createId("food"),
            kind = FoodKind.Ingredient,
            name = "Broccoli",
            barcode = "004129100000",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(34.0, 2.8, 7.0, 0.4),
            frequency = 6,
            lastUsedDaysAgo = 3
        ),
        FoodItem(
            id = createId("food"),
            kind = FoodKind.Ingredient,
            name = "Salmon",
            barcode = "073495000001",
            servingQuantity = 100.0,
            servingUnit = "gram",
            nutrients = Nutrients(208.0, 20.0, 0.0, 13.0),
            frequency = 3,
            lastUsedDaysAgo = 5
        )
    )

    val syncSettings = SyncSettings(
        syncEnabled = true,
        backupMode = SyncSettings.BackupMode.AutomaticBackup,
        calorieTargetMin = 1800,
        calorieTargetMax = 2200,
        weightUnit = WeightUnit.Kilograms,
        goalWeightKg = 70.0,
        lastSuccessfulSyncAt = nowIsoString()
    )

    val syncMetadata = SyncMetadata(
        recordId = "test-id",
        deviceId = "test-device",
        updatedAt = nowIsoString(),
        version = 1,
        syncStatus = SyncStatus.Synced
    )

    val barcodeAlias = BarcodeAliasRecord(
        barcode = "093469000000",
        productId = "apple-product-id",
        ownerUserId = "user-123",
        visibility = "private",
        createdAt = nowIsoString(),
        sync = syncMetadata
    )

    val barcodeAliases = listOf(
        barcodeAlias.copy(
            barcode = "072049100000",
            productId = "banana-product-id",
            ownerUserId = "user-123",
            createdAt = nowIsoString()
        ),
        barcodeAlias.copy(
            barcode = "073495000000",
            productId = "chicken-product-id",
            ownerUserId = "user-123",
            createdAt = nowIsoString()
        )
    )

    val recipeComponents = listOf(
        RecipeComponent(
            item = FoodItem(
                id = createId("recipe-ingredient"),
                kind = FoodKind.Ingredient,
                name = "Eggs",
                servingQuantity = 50.0,
                servingUnit = "gram",
                nutrients = Nutrients(155.0, 13.0, 1.1, 11.0)
            ),
            amount = 3.0,
            unit = "large"
        ),
        RecipeComponent(
            item = FoodItem(
                id = createId("recipe-ingredient"),
                kind = FoodKind.Ingredient,
                name = "Milk",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(52.0, 3.4, 5.0, 3.6)
            ),
            amount = 100.0,
            unit = "ml"
        ),
        RecipeComponent(
            item = FoodItem(
                id = createId("recipe-ingredient"),
                kind = FoodKind.Ingredient,
                name = "Flour",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(340.0, 13.0, 75.0, 0.6)
            ),
            amount = 80.0,
            unit = "gram"
        )
    )

    val recipeDraft = RecipeDraft(
        name = "Pancakes",
        brand = "My Recipes",
        servingQuantity = "1",
        servingUnit = "serving",
        components = recipeComponents
    )

    val recipe = FoodItem(
        id = createId("recipe"),
        kind = FoodKind.Recipe,
        name = "Pancakes",
        brand = "My Recipes",
        servingQuantity = 1.0,
        servingUnit = "serving",
        nutrients = Nutrients(450.0, 18.0, 65.0, 12.0),
        components = recipeComponents
    )

    val allFoods = foods + listOf(recipe)

    enum class Theme(val value: String) {
        Light("light"),
        Dark("dark")
    }

    /**
     * A composable builder function that takes a theme block and applies it.
     * This is a placeholder that will be removed when the actual theme composable is available.
     */
    @Deprecated("This is a preview-only helper. Remove when actual theme composable is implemented.")
    inline fun Theme(crossinline block: Theme.() -> Unit) = block(this)
}

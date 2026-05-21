package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.model.Totals
import java.util.UUID
import kotlin.math.round

val measurementUnits = listOf(
    "bar",
    "bottle",
    "box",
    "can",
    "container",
    "cup",
    "fl oz",
    "gram",
    "jar",
    "kg",
    "lb",
    "liter",
    "milligram",
    "ml",
    "oz",
    "package",
    "pint",
    "quart",
    "service",
    "serving",
    "tbsp",
    "tsp",
)

private val conversionGroups = listOf(
    mapOf("tsp" to 1.0, "tbsp" to 3.0, "fl oz" to 6.0, "cup" to 48.0, "pint" to 96.0, "quart" to 192.0, "ml" to 0.202884, "liter" to 202.884),
    mapOf("milligram" to 0.001, "gram" to 1.0, "kg" to 1000.0, "oz" to 28.3495, "lb" to 453.592),
)

val FoodItem.servingLabel: String
    get() = "${formatNumber(servingQuantity)} $servingUnit"

fun createId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

fun roundOne(value: Double): Double = round(value * 10.0) / 10.0

fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else roundOne(value).toString()

fun convertAmount(amount: Double, fromUnit: String, toUnit: String): Double? {
    if (fromUnit == toUnit) return amount
    val group = conversionGroups.firstOrNull { it.containsKey(fromUnit) && it.containsKey(toUnit) } ?: return null
    return amount * (group[fromUnit] ?: return null) / (group[toUnit] ?: return null)
}

fun totalComponents(components: List<RecipeComponent>): Totals =
    components.fold(Totals()) { total, component ->
        val converted = convertAmount(component.amount, component.unit, component.item.servingUnit) ?: component.amount
        val nutrients = component.item.nutrients.scale(converted / component.item.servingQuantity.coerceAtLeast(0.1))
        Totals(
            calories = total.calories + nutrients.calories,
            protein = total.protein + nutrients.proteinGrams,
            carbs = total.carbs + nutrients.carbohydrateGrams,
            fat = total.fat + nutrients.fatGrams,
        )
    }.rounded()

fun totalsForEntries(entries: List<DiaryEntry>): Totals =
    entries.fold(Totals()) { total, entry ->
        val nutrients = entry.food.nutrients.scale(entry.servingMultiplier)
        Totals(
            calories = total.calories + nutrients.calories,
            protein = total.protein + nutrients.proteinGrams,
            carbs = total.carbs + nutrients.carbohydrateGrams,
            fat = total.fat + nutrients.fatGrams,
        )
    }.rounded()

fun Nutrients.scale(multiplier: Double): Nutrients = Nutrients(
    calories = roundOne(calories * multiplier),
    proteinGrams = roundOne(proteinGrams * multiplier),
    carbohydrateGrams = roundOne(carbohydrateGrams * multiplier),
    fatGrams = roundOne(fatGrams * multiplier),
)

fun Totals.rounded(): Totals = Totals(
    calories = roundOne(calories),
    protein = roundOne(protein),
    carbs = roundOne(carbs),
    fat = roundOne(fat),
)

fun FoodItem.componentSummary(): String =
    components.joinToString(", ") { "${it.item.name} - ${formatNumber(it.amount)} ${it.unit}" }

fun RecipeDraft.toFoodItem(existingId: String? = null): FoodItem {
    val nutrients = totalComponents(components)
    val quantity = servingQuantity.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
    return FoodItem(
        id = existingId ?: createId("recipe"),
        kind = FoodKind.Recipe,
        name = name.trim(),
        brand = brand.trim(),
        servingQuantity = quantity,
        servingUnit = servingUnit,
        nutrients = Nutrients(nutrients.calories, nutrients.protein, nutrients.carbs, nutrients.fat),
        components = components,
    )
}

fun FoodItem.toRecipeDraft(): RecipeDraft = RecipeDraft(
    name = name,
    brand = brand,
    servingQuantity = formatNumber(servingQuantity),
    servingUnit = servingUnit,
    components = components,
)

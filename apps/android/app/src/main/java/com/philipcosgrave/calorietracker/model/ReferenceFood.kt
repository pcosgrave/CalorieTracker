package com.philipcosgrave.calorietracker.model

data class ReferenceServing(
    val id: String,
    val description: String,
    val grams: Double,
    /** Original equivalent amount when a serving is based on volume rather than mass. */
    val amount: Double? = null,
    val unit: String? = null,
)

data class ReferenceFood(
    val id: String,
    val source: String,
    val sourceFoodId: String,
    val name: String,
    val normalizedName: String,
    val foodGroup: String,
    val nutritionPer100g: Nutrients,
    val servingOptions: List<ReferenceServing>,
    val sourceVersion: String = "2026",
) {
    fun toLocalFood(id: String) = FoodItem(
        id = id, kind = FoodKind.Ingredient, name = name, servingQuantity = 100.0,
        servingUnit = "g", nutrients = nutritionPer100g, source = source,
        sourceId = sourceFoodId, sourceVersion = sourceVersion, servingOptions = servingOptions,
    )
}

/** Keys are stable identifiers; units never depend on display text. Missing values stay absent. */
val additionalNutrientFields = linkedMapOf(
    "saturatedFat" to ("Saturated fat" to "g"), "transFat" to ("Trans fat" to "g"),
    "fiber" to ("Fiber" to "g"), "sugar" to ("Sugar" to "g"),
    "sodium" to ("Sodium" to "mg"), "cholesterol" to ("Cholesterol" to "mg"),
    "potassium" to ("Potassium" to "mg"), "calcium" to ("Calcium" to "mg"),
    "iron" to ("Iron" to "mg"), "magnesium" to ("Magnesium" to "mg"),
    "zinc" to ("Zinc" to "mg"), "vitaminA" to ("Vitamin A (RAE)" to "µg"),
    "vitaminC" to ("Vitamin C" to "mg"), "vitaminD" to ("Vitamin D" to "µg"),
    "vitaminB12" to ("Vitamin B12" to "µg"), "folate" to ("Folate" to "µg"),
)

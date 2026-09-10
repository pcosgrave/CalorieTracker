package com.philipcosgrave.calorietracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.philipcosgrave.calorietracker.ui.components.ReviewSwipeActions
import androidx.core.content.ContextCompat
import com.philipcosgrave.calorietracker.domain.*
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.ui.components.DatePillsRow
import com.philipcosgrave.calorietracker.ui.components.MealPicker
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.LocalTime

val DraftsSaver = listSaver<List<PhotoFoodDraft>, String>(
    save = { drafts -> drafts.flatMap { listOf(it.id, it.name, it.grams, it.weightSource, it.notes, it.calories, it.protein, it.carbs, it.fat, it.matchedFoodId.orEmpty(), it.amountUnit) } },
    restore = { values -> values.chunked(11).map { PhotoFoodDraft(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], it[9].ifBlank { null }, it[10]) } },
)

@Composable
fun PhotoFoodScreen(
    date: LocalDate,
    onAnalyzePhoto: suspend (Bitmap, List<FoodItem>, (String) -> Unit) -> List<PhotoFoodDraft>,
    onAnalyzeLabel: suspend (Bitmap, (String) -> Unit) -> NutritionLabel,
    foods: List<FoodItem>,
    onBack: () -> Unit,
    onSave: suspend (List<DiaryEntry>) -> Unit,
    onSaved: () -> Unit,
    onCreateFood: suspend (FoodItem) -> Unit,
    onLookupBarcode: suspend (String) -> FoodItem?,
    initialEntries: List<DiaryEntry> = emptyList(),
    initialDrafts: List<PhotoFoodDraft> = emptyList(),
    reviewOnly: Boolean = false,
    destinationMeal: Meal? = null,
    recipeDestination: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var drafts by rememberSaveable(stateSaver = DraftsSaver) { mutableStateOf(initialDrafts.ifEmpty { initialEntries.map {
        PhotoFoodDraft(id = it.id, name = foodTitle(it.food.name), grams = formatNumber(it.loggedAmount),
            weightSource = "Dictated amount", matchedFoodId = it.food.id, amountUnit = it.loggedUnit)
    } }) }
    var creatingFoodId by rememberSaveable { mutableStateOf<String?>(null) }
    var createError by remember { mutableStateOf<String?>(null) }
    var addingFoodId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedFoodId by rememberSaveable { mutableStateOf<String?>(null) }
    // Review-only image: never put a bitmap in saved state, the database, or a file.
    var reviewPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var selectedDate by rememberSaveable { mutableStateOf(date.toString()) }
    var mealName by rememberSaveable { mutableStateOf((destinationMeal ?: initialEntries.firstOrNull()?.meal ?: inferMealForTime(LocalTime.now())).name) }
    var busy by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var analysisJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun goBack() {
        if (!saving) {
            analysisJob?.cancel()
            reviewPhoto = null
            onBack()
        }
    }
    BackHandler(busy) { goBack() }

    fun analyze(bitmap: Bitmap) {
        if (busy) { bitmap.recycle(); return }
        drafts = emptyList()
        reviewPhoto = null
        expandedFoodId = null
        error = null
        busy = true
        analysisJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                drafts = withTimeout(300_000) { onAnalyzePhoto(bitmap, foods) { status = it } }
                val ratio = minOf(1f, 640f / maxOf(bitmap.width, bitmap.height))
                val small = Bitmap.createScaledBitmap(bitmap,
                    maxOf(1, (bitmap.width * ratio).toInt()), maxOf(1, (bitmap.height * ratio).toInt()), true)
                // createScaledBitmap may return its source; capture cleanup must not recycle the preview.
                reviewPhoto = if (small === bitmap) bitmap.copy(Bitmap.Config.ARGB_8888, false) else small
            } catch (cancelled: CancellationException) {
                if (cancelled is kotlinx.coroutines.TimeoutCancellationException) {
                    error = "Photo analysis took too long. Please try again."
                } else throw cancelled
            } catch (exception: Exception) {
                error = exception.message ?: "Unable to analyze this photo. Please try again."
            } finally {
                bitmap.recycle()
                analysisJob = null
                busy = false
                status = ""
            }
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) error = "Allow camera access to photograph your meal."
    }
    LaunchedEffect(Unit) {
        if (!reviewOnly && !hasCameraPermission && drafts.isEmpty()) permission.launch(Manifest.permission.CAMERA)
    }

    val creating = drafts.firstOrNull { it.id == creatingFoodId }
    if (creating != null) {
        BackHandler { if (!saving) creatingFoodId = null }
        key(creating.id) {
            var lookup by remember { mutableStateOf<FoodItem?>(null) }
            var lookingUp by remember { mutableStateOf(false) }
            val lookupScope = rememberCoroutineScope()
            AddIngredientScreen(knownFoods = foods,
                onAnalyzeLabel = onAnalyzeLabel,                barcodeLookupResult = lookup, isLookingUpBarcode = lookingUp,
                onLookupBarcode = { barcode ->
                    lookupScope.launch {
                        lookingUp = true
                        createError = null
                        try {
                            lookup = onLookupBarcode(barcode)
                            if (lookup == null) createError = "No barcode match found. Enter the food or scan its label."
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (exception: Exception) {
                            createError = "Barcode lookup failed. Try again or scan the nutrition label."
                        } finally { lookingUp = false }
                    }
                },
                existing = null, initialName = creating.name, autoSearchNutrition = true,
                initialBarcode = if (creating.weightSource == "Barcode") creating.notes.removePrefix("UPC: ") else "",
                initialLabel = if (creating.weightSource == "Nutrition label") NutritionLabel(creating.name, null, creating.grams, creating.amountUnit, creating.calories, creating.protein, creating.carbs, creating.fat) else null,
                isSaving = saving, saveError = createError,
                onBack = { if (!saving) creatingFoodId = null },
                onSave = { food ->
                    if (!saving) {
                        saving = true
                        createError = null
                        scope.launch {
                            try {
                                onCreateFood(food)
                                drafts = drafts.map { if (it.id == creating.id) linkReviewFood(it, food) else it }
                                expandedFoodId = creating.id
                                creatingFoodId = null
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (exception: Exception) {
                                createError = "Could not save the food. Please retry."
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
            )
        }
        return
    }

    drafts.firstOrNull { it.id == addingFoodId }?.let { added ->
        Dialog(onDismissRequest = { addingFoodId = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PageHeader("Add missed food", onBack = { addingFoodId = null })
                    reviewPhoto?.let { photo ->
                        Image(photo.asImageBitmap(), "Full meal photo", Modifier.fillMaxWidth().height(280.dp), contentScale = ContentScale.Fit)
                    }
                    PhotoFoodEditor(added, foods, true, true, {},
                        onChange = { changed -> drafts = drafts.map { if (it.id == changed.id) changed else it } },
                        onRemove = { drafts = drafts.filterNot { it.id == added.id }; addingFoodId = null },
                        showThumbnail = false,
                        onAddFood = { addingFoodId = null; createError = null; creatingFoodId = added.id },
                    )
                    Button(onClick = { addingFoodId = null }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader(if (reviewOnly || drafts.isNotEmpty()) "Food review" else "Add food from photo", onBack = { goBack() })
        Text(
            if (reviewOnly) "Review what you dictated. Tap a food to edit."
            else if (drafts.isEmpty()) "Photos are analyzed on your phone without being saved."
            else "Review the items you’ve added. Tap a food to edit.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!busy && drafts.isEmpty() && !reviewOnly) {
            if (hasCameraPermission) {
                MemoryPhotoCapture(onCaptured = ::analyze, onError = { error = it })
            } else {
                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
            }
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(status)
            if (!saving) TextButton(onClick = { analysisJob?.cancel() }) { Text("Cancel analysis") }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (reviewOnly && drafts.isEmpty()) Text("No foods left to save. Go back to dictate another food.")
        if (drafts.isNotEmpty()) {
            drafts.forEach { draft ->
                key(draft.id) {
                    PhotoFoodEditor(draft, foods, !busy,
                        expanded = expandedFoodId == draft.id,
                        photo = reviewPhoto,
                        showThumbnail = !reviewOnly,
                        onToggle = {
                            if (draft.matchedFood(foods) == null) { createError = null; creatingFoodId = draft.id }
                            else expandedFoodId = if (expandedFoodId == draft.id) null else draft.id
                        },
                        onAddFood = { createError = null; creatingFoodId = draft.id },
                        onChange = { changed -> drafts = drafts.map { if (it.id == changed.id) changed else it } },
                        onRemove = {
                            drafts = drafts.filterNot { it.id == draft.id }
                            expandedFoodId = null
                            if (drafts.isEmpty()) reviewPhoto = null
                        },
                    )
                }
            }
            if (!busy) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        val added = PhotoFoodDraft(name = "", grams = "")
                        drafts = drafts + added
                        addingFoodId = added.id
                    }) { Text("Add missed food") }
                    if (!reviewOnly) TextButton(onClick = { drafts = emptyList(); reviewPhoto = null; expandedFoodId = null; error = null }) { Text("Retake photo") }
                }
                if (!recipeDestination) MealPicker(Meal.valueOf(mealName), { mealName = it.name })
                if (!recipeDestination) DatePillsRow(LocalDate.parse(selectedDate), LocalDate.now(), { if (!it.isAfter(LocalDate.now())) selectedDate = it.toString() })
            }
            Button(enabled = !busy && drafts.all { it.isValid(foods) }, modifier = Modifier.fillMaxWidth(), onClick = {
                busy = true
                saving = true
                error = null
                status = "Saving foods and meal…"
                scope.launch {
                    try {
                        onSave(photoDiaryEntries(drafts, foods, LocalDate.parse(selectedDate), Meal.valueOf(mealName)))
                        reviewPhoto = null
                        onSaved()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        error = "Could not save the meal. Your list is still here; please retry."
                    } finally {
                        busy = false
                        saving = false
                    }
                }
            }) { Text(if (recipeDestination) "Add ingredients" else "Add") }
        }
    }
}

@Composable
private fun PhotoFoodEditor(
    draft: PhotoFoodDraft, foods: List<FoodItem>, enabled: Boolean,
    expanded: Boolean, onToggle: () -> Unit,
    onChange: (PhotoFoodDraft) -> Unit, onRemove: () -> Unit,
    photo: Bitmap? = null,
    showThumbnail: Boolean = true,
    onAddFood: () -> Unit = {},
) {
    val matched = draft.matchedFood(foods)
    var choosing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val grams = draft.grams.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val portionCalories = grams?.let { amount ->
        if (matched != null) matched.amountInBaseUnits(amount, draft.amountUnit)?.let {
            it / matched.servingQuantity * matched.nutrients.calories
        } else null
    }?.takeIf { it.isFinite() && it >= 0 }
    val needsReview = !draft.isValid(foods)
    ReviewSwipeActions(enabled = enabled, onEdit = { if (!expanded) onToggle() }, onDelete = onRemove) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (expanded) colors.primary else colors.outline.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClickLabel = if (expanded) "Collapse food" else "Edit food", onClick = onToggle)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showThumbnail) Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    Image(photo.asImageBitmap(), contentDescription = "Captured meal photo",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("🍽️", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(foodTitle(draft.name).ifBlank { "New food" }, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (matched == null) "Unknown food · tap to add" else if (needsReview) "Needs review" else "${formatNumber(grams!!)} ${draft.amountUnit} · " +
                        if (draft.weightSource.startsWith("Scale")) "Scale" else if (draft.weightSource.startsWith("Edited")) "Edited" else if (draft.weightSource.startsWith("Dictated")) "Dictated" else "Estimated",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (needsReview) colors.error else colors.onSurfaceVariant,
                )
            }
            Text(portionCalories?.let { "${formatNumber(it)} kcal" } ?: "— kcal",
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(if (expanded) "⌃" else "›", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfaceVariant)
        }
        if (expanded) {
        HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))
        Column(Modifier.background(colors.surfaceVariant.copy(alpha = 0.3f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(draft.name, { onChange(draft.copy(name = it, matchedFoodId = null, amountUnit = "g", calories = "", protein = "", carbs = "", fat = "")) }, enabled = enabled, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
            Text(draft.weightSource, style = MaterialTheme.typography.labelMedium)
            if (draft.notes.isNotBlank()) Text(draft.notes, style = MaterialTheme.typography.bodySmall)
            PhotoNumberField("Amount (${draft.amountUnit})", draft.grams, enabled) { onChange(draft.copy(grams = it, weightSource = "Edited amount")) }
            TextButton(enabled = enabled, onClick = { choosing = !choosing }) { Text(matched?.let { "Saved food: ${it.name} — change" } ?: "Match a saved food") }
            if (choosing) {
                OutlinedTextField(query, { query = it }, enabled = enabled, label = { Text("Find saved food") })
                foods.filter { it.name.contains(query, true) }.take(8).forEach { food ->
                    TextButton(enabled = enabled, onClick = { onChange(linkReviewFood(draft, food)); choosing = false }) { Text("${foodTitle(food.name)} ${food.brand}") }
                }
            }
            if (matched == null) {
                Text("Add this food to supply its nutrition, or match a saved food.", style = MaterialTheme.typography.bodySmall)
                Button(enabled = enabled, onClick = onAddFood) { Text("Add food") }
            } else {
                val amount = draft.grams.toDoubleOrNull()
                val calories = amount?.let { matched.amountInBaseUnits(it, draft.amountUnit) }?.let { it / matched.servingQuantity * matched.nutrients.calories }
                calories?.let { Text("${formatNumber(it)} calories · saved nutrition") }
            }
            if (!draft.isValid(foods)) Text("Choose or add a saved food and enter a positive amount.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        }
    }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun CondensedPhotoReviewPreview() {
    MaterialTheme {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                PhotoFoodDraft(name = "Apple", grams = "180", calories = "52", protein = "0.3", carbs = "14", fat = "0.2"),
                PhotoFoodDraft(name = "Grilled chicken breast", grams = "150", calories = "165", protein = "31", carbs = "0", fat = "3.6"),
                PhotoFoodDraft(name = "Oatmeal (cooked)", grams = "200", calories = "79", protein = "3", carbs = "14", fat = "1.5"),
                PhotoFoodDraft(name = "Unidentified food", grams = ""),
            ).forEach { draft ->
                PhotoFoodEditor(draft, emptyList(), true, false, {}, {}, {})
            }
        }
    }
}

@Composable
private fun PhotoNumberField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value, { text ->
        val normalized = text.replace(',', '.')
        if (normalized.matches(Regex("[0-9]*[.]?[0-9]*"))) onChange(normalized)
    }, enabled = enabled, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

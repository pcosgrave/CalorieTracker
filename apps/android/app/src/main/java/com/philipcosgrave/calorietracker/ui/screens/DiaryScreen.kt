package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.totalsForEntries
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.MealCopyOptions
import com.philipcosgrave.calorietracker.ui.components.AppBlue
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.DatePillsRow
import com.philipcosgrave.calorietracker.ui.components.DiaryEntryRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.appBorderColor
import com.philipcosgrave.calorietracker.ui.components.appBorderStrongColor
import com.philipcosgrave.calorietracker.ui.components.appSoftColor
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

@Composable
fun DiaryScreen(
    selectedDate: LocalDate,
    entries: List<DiaryEntry>,
    targetRangeMin: Int,
    targetRangeMax: Int,
    onDateChange: (LocalDate) -> Unit,
    onCopyDay: suspend (LocalDate) -> Unit = {},
    onOpenCopyMeals: () -> Unit = {},
    onBack: () -> Unit,
    onAddFood: () -> Unit,
    onAddMealFood: (Meal) -> Unit = { onAddFood() },
    onVoiceLog: () -> Unit,
    onPhotoLog: () -> Unit = {},
    onOpenLeftovers: () -> Unit = {},
    leftoverCount: Int = 0,
    onCreateLeftover: suspend (List<DiaryEntry>, Double, String) -> Unit = { _, _, _ -> },
    onOpenSyncSettings: () -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    mealCopyOptions: Map<Meal, MealCopyOptions> = emptyMap(),
    onCopyMealFromDate: (Meal, LocalDate) -> Unit = { _, _ -> },
    onCopyMealToToday: (Meal) -> Unit = {},
    voiceTranscript: String? = null,
    voiceStatusLabel: String? = null,
    voiceStatusMessage: String? = null,
    voiceRetryVisible: Boolean = false,
    voiceCandidateMatches: List<FoodItem> = emptyList(),
    onRetryVoiceLog: () -> Unit = {},
    onDismissVoiceFeedback: () -> Unit = {},
    onSelectVoiceCandidate: (FoodItem) -> Unit = {},
) {
    var choosingAddMeal by remember { mutableStateOf(false) }
    var choosingCopyDay by remember { mutableStateOf(false) }
    var copySource by remember { mutableStateOf<LocalDate?>(null) }
    var copying by remember { mutableStateOf(false) }
    var copyError by remember { mutableStateOf<String?>(null) }
    val copyScope = androidx.compose.runtime.rememberCoroutineScope()
    if (choosingCopyDay) MealCopyCalendarDialog(Meal.Breakfast,
        entries.map { it.date }.distinct().filter { it != selectedDate },
        { choosingCopyDay = false }, { choosingCopyDay = false; copySource = it }, title = "Copy day from")
    copySource?.let { source -> AlertDialog(onDismissRequest = { if (!copying) copySource = null },
        title = { Text("Copy day?") }, text = { Text(copyError ?: "Add all foods from $source to $selectedDate? Existing foods will stay.") },
        confirmButton = { TextButton(enabled = !copying, onClick = { copying = true; copyScope.launch {
            try { onCopyDay(source); copySource = null; copyError = null }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { copyError = "Could not copy day. Please retry." }
            finally { copying = false }
        } }) { Text(if (copying) "Copying…" else "Copy") } },
        dismissButton = { TextButton(enabled = !copying, onClick = { copySource = null; copyError = null }) { Text("Cancel") } }) }
    var choosingLeftoverMeal by remember { mutableStateOf(false) }
    var leftoverEntries by remember(selectedDate) { mutableStateOf<List<DiaryEntry>?>(null) }
    leftoverEntries?.let { CreateLeftoverDialog(it, { leftoverEntries = null }, onCreateLeftover) }
    val pageScrollState = rememberScrollState()
    var expandedEntryId by remember(selectedDate) { mutableStateOf<String?>(null) }
    var pendingDeleteEntry by remember(selectedDate) { mutableStateOf<DiaryEntry?>(null) }
    val today = LocalDate.now()
    val selectedEntries = entries.filter { it.date == selectedDate }
    val totals = totalsForEntries(selectedEntries)
    LaunchedEffect(pageScrollState.isScrollInProgress, selectedDate) {
        if (pageScrollState.isScrollInProgress) {
            expandedEntryId = null
        }
    }
    Page(scrollState = pageScrollState) {
        Text("Food Log", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        DatePillsRow(selectedDate, today, onDateChange, Modifier.fillMaxWidth())

        com.philipcosgrave.calorietracker.ui.components.ExpandableNutritionSummary(totals, targetRangeMin, targetRangeMax)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Button(onClick = onAddFood, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
                DropdownMenu(choosingAddMeal, { choosingAddMeal = false }) { Meal.entries.forEach { meal ->
                    DropdownMenuItem(text = { Text(meal.label) }, onClick = { choosingAddMeal = false; onAddMealFood(meal) })
                } }
            }
            OutlinedButton(onClick = onOpenCopyMeals, enabled = entries.isNotEmpty(), modifier = Modifier.weight(1f).height(56.dp).semantics { contentDescription = "Copy" }, shape = RoundedCornerShape(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                com.philipcosgrave.calorietracker.ui.components.BiteWiseIcon("Copy", modifier = Modifier.size(32.dp))
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { choosingLeftoverMeal = true }, enabled = selectedEntries.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "Create leftover" }, shape = RoundedCornerShape(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                    com.philipcosgrave.calorietracker.ui.components.BiteWiseIcon("Leftover", modifier = Modifier.size(32.dp))
                }
                DropdownMenu(choosingLeftoverMeal, { choosingLeftoverMeal = false }) {
                    Meal.entries.filter { meal -> selectedEntries.any { it.meal == meal } }.forEach { meal ->
                        DropdownMenuItem(text = { Text(meal.label) }, onClick = { choosingLeftoverMeal = false; leftoverEntries = selectedEntries.filter { it.meal == meal } })
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Meal.entries.forEach { meal ->
                val mealEntries = selectedEntries.filter { it.meal == meal }
                val copyOptions = mealCopyOptions[meal] ?: MealCopyOptions()
                AppCardContainer {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            meal.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (mealEntries.isEmpty()) "0 cal" else "${formatNumber(totalsForEntries(mealEntries).calories)} cal",
                            color = if (mealEntries.isEmpty()) AppMuted else Color.Unspecified,
                            fontWeight = if (mealEntries.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                        )
                    }

                    if (mealEntries.isNotEmpty()) {
                        mealEntries.forEach { entry ->
                            DiaryEntryRow(
                                entry = entry,
                                expanded = expandedEntryId == entry.id,
                                onToggleExpanded = {
                                    expandedEntryId = if (expandedEntryId == entry.id) null else entry.id
                                },
                                onEdit = {
                                    expandedEntryId = null
                                    onEditEntry(entry)
                                },
                                onDelete = {
                                    expandedEntryId = null
                                    pendingDeleteEntry = entry
                                },
                            )
                        }
                    }

                }
            }
        }
    }

    pendingDeleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDeleteEntry = null },
            title = { Text("Delete diary item?") },
            text = { Text("Delete ${entry.food.name} from ${entry.meal.label.lowercase()}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteEntry = null
                        onDeleteEntry(entry)
                    },
                ) {
                    Text("Delete", color = Color(0xFFFF5449), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteEntry = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun MealCopyHeaderActions(
    meal: Meal,
    copyOptions: MealCopyOptions,
    isViewingToday: Boolean,
    hasMealEntries: Boolean,
    onCopyMealFromDate: (Meal, LocalDate) -> Unit,
    onCopyMealToToday: (Meal) -> Unit,
) {
    if (
        copyOptions.previousDate == null &&
        copyOptions.yesterdayDate == null &&
        copyOptions.selectableDates.isEmpty() &&
        (isViewingToday || !hasMealEntries)
    ) {
        return
    }

    var dayPickerVisible by remember(meal, copyOptions.selectableDates) { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        copyOptions.previousDate?.let { previousDate ->
            CopyMealIconButton(
                label = "Repeat previous ${meal.label.lowercase()}",
                icon = "\u21BA",
                onClick = { onCopyMealFromDate(meal, previousDate) },
            )
        }
        if (copyOptions.selectableDates.isNotEmpty()) {
            CopyMealIconButton(
                label = "Choose a day to copy ${meal.label.lowercase()} from",
                icon = "\uD83D\uDCC5",
                onClick = { dayPickerVisible = true },
            )
        }
        if (!isViewingToday && hasMealEntries) {
            CopyMealIconButton(
                label = "Copy this ${meal.label.lowercase()} to today",
                icon = "\u21BB",
                onClick = { onCopyMealToToday(meal) },
            )
        }
    }

    if (dayPickerVisible) {
        MealCopyCalendarDialog(
            meal = meal,
            selectableDates = copyOptions.selectableDates,
            onDismiss = { dayPickerVisible = false },
            onSelectDate = { date ->
                dayPickerVisible = false
                onCopyMealFromDate(meal, date)
            },
        )
    }
}

@Composable
private fun CopyMealIconButton(
    label: String,
    icon: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(appSoftColor(), RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onClick) {
            Text(
                icon,
                color = AppBlue,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun MealCopyCalendarDialog(
    meal: Meal,
    selectableDates: List<LocalDate>,
    onDismiss: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    title: String? = null,
) {
    val selectableDateSet = remember(selectableDates) { selectableDates.toSet() }
    val mostRecentMonth = remember(selectableDates) { YearMonth.from(selectableDates.maxOrNull() ?: LocalDate.now()) }
    var visibleMonth by remember(meal, selectableDates) { mutableStateOf(mostRecentMonth) }
    val earliestMonth = remember(selectableDates) { YearMonth.from(selectableDates.minOrNull() ?: LocalDate.now()) }
    val today = LocalDate.now()
    val monthDays = remember(visibleMonth) {
        val firstDay = visibleMonth.atDay(1)
        val leadingEmpty = firstDay.dayOfWeek.value % 7
        buildList<LocalDate?> {
            repeat(leadingEmpty) { add(null) }
            for (day in 1..visibleMonth.lengthOfMonth()) {
                add(visibleMonth.atDay(day))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AppBlue)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title ?: "Copy ${meal.label.lowercase()} from", fontWeight = FontWeight.ExtraBold)
                Text(
                    if (title == null) "Days without this meal are unavailable." else "Choose a day with logged foods.",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                        enabled = visibleMonth > earliestMonth,
                    ) {
                        Text("\u2039", color = if (visibleMonth > earliestMonth) AppBlue else AppMuted)
                    }
                    Text(
                        "${visibleMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${visibleMonth.year}",
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                        enabled = visibleMonth < YearMonth.from(today),
                    ) {
                        Text("\u203A", color = if (visibleMonth < YearMonth.from(today)) AppBlue else AppMuted)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                            Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, color = AppMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    monthDays.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (week + List(7 - week.size) { null }).forEach { date ->
                                val selectable = date != null && date in selectableDateSet
                                Box(Modifier.weight(1f).height(40.dp)
                                    .background(if (selectable) AppBlue.copy(alpha = .12f) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable(enabled = selectable) { date?.let(onSelectDate) }, contentAlignment = Alignment.Center) {
                                    Text(date?.dayOfMonth?.toString().orEmpty(), color = if (selectable) AppBlue else AppMuted.copy(alpha = .45f), fontWeight = if (selectable) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun VoiceStatusCard(
    label: String?,
    transcript: String?,
    message: String?,
    candidateMatches: List<FoodItem>,
    retryVisible: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onSelectCandidate: (FoodItem) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appSoftColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!label.isNullOrBlank()) {
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            if (!transcript.isNullOrBlank()) {
                Text("\"$transcript\"", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            if (!message.isNullOrBlank()) {
                Text(message, color = AppMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (candidateMatches.isNotEmpty()) {
                Text("Pick the closest match", fontWeight = FontWeight.Bold)
                candidateMatches.forEach { item ->
                    DiaryVoiceCandidateRow(
                        name = item.name,
                        brand = item.brand,
                        onSelect = { onSelectCandidate(item) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (retryVisible) {
                    TextButton(onClick = onRetry) {
                        Text("Retry", color = AppBlue)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AppMuted)
                }
            }
        }
    }
}

@Composable
private fun DiaryVoiceCandidateRow(
    name: String,
    brand: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, fontWeight = FontWeight.Bold)
            if (brand.isNotBlank()) {
                Text(brand, color = AppMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = onSelect) {
            Text("Use", color = AppBlue, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
private fun MacroRow(protein: Double, carbs: Double, fat: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MacroStat("${formatNumber(protein)}g", "PROTEIN", Modifier.weight(1f))
        MacroStat("${formatNumber(carbs)}g", "CARBS", Modifier.weight(1f))
        MacroStat("${formatNumber(fat)}g", "FAT", Modifier.weight(1f))
    }
}

@Composable
private fun MacroStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(appBorderColor()),
                )
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(label, color = AppMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1200)
@Composable
private fun DiaryScreenPreview() {
    PreviewData.Theme {
        DiaryScreen(
            selectedDate = PreviewData.date,
            entries = PreviewData.diaryEntries,
            targetRangeMin = PreviewData.syncSettings.calorieTargetMin,
            targetRangeMax = PreviewData.syncSettings.calorieTargetMax,
            onDateChange = {},
            onBack = {},
            onAddFood = {},
            onVoiceLog = {},
            onOpenSyncSettings = {},
            onDeleteEntry = {},
            onEditEntry = {},
            onRetryVoiceLog = {},
            onDismissVoiceFeedback = {},
            onSelectVoiceCandidate = {},
        )
    }
}

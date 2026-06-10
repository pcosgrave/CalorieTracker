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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.AiCreatableFoodDraft
import com.philipcosgrave.calorietracker.model.AiDiaryEntryDraft
import com.philipcosgrave.calorietracker.model.AiDiaryEntryMatchStatus
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
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DiaryScreen(
    selectedDate: LocalDate,
    entries: List<DiaryEntry>,
    targetRangeMin: Int,
    targetRangeMax: Int,
    onDateChange: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onAddFood: () -> Unit,
    onVoiceLog: () -> Unit,
    onAiLogParse: (String) -> Unit,
    aiLogIsParsing: Boolean,
    aiLogErrorMessage: String?,
    aiLogParsedDrafts: List<AiDiaryEntryDraft>,
    onAiLogConfirm: (List<AiDiaryEntryDraft>) -> Unit,
    onAiLogCancelReview: () -> Unit,
    onRetryAiLogParse: () -> Unit,
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
    val pageScrollState = rememberScrollState()
    var expandedEntryId by remember(selectedDate) { mutableStateOf<String?>(null) }
    var pendingDeleteEntry by remember(selectedDate) { mutableStateOf<DiaryEntry?>(null) }
    var aiLogSheetOpen by remember { mutableStateOf(false) }
    var aiLogTranscript by remember { mutableStateOf("") }
    var reviewDrafts by remember { mutableStateOf<List<AiDiaryEntryDraft>>(emptyList()) }
    val today = LocalDate.now()
    val selectedEntries = entries.filter { it.date == selectedDate }
    val totals = totalsForEntries(selectedEntries)
    val isViewingToday = selectedDate == today
    val rangeMin = targetRangeMin.coerceAtLeast(0)
    val rangeMax = max(targetRangeMax, rangeMin + 1)
    val overflowSpan = max(rangeMax - rangeMin, 200)
    val currentCalories = totals.calories.toFloat()
    val currentProgress =
        when {
            currentCalories <= rangeMin -> {
                if (rangeMin == 0) 0f else (currentCalories / rangeMin.toFloat()) * (1f / 3f)
            }

            currentCalories <= rangeMax -> {
                val inRangeProgress = (currentCalories - rangeMin.toFloat()) / (rangeMax - rangeMin).toFloat()
                (1f / 3f) + inRangeProgress * (1f / 3f)
            }

            else -> {
                val overProgress = ((currentCalories - rangeMax.toFloat()) / overflowSpan.toFloat()).coerceIn(0f, 1f)
                (2f / 3f) + overProgress * (1f / 3f)
            }
        }.coerceIn(0f, 1f)
    LaunchedEffect(pageScrollState.isScrollInProgress, selectedDate) {
        if (pageScrollState.isScrollInProgress) {
            expandedEntryId = null
        }
    }
    Page(scrollState = pageScrollState) {
        PageHeader(
            title = "Food Log",
            onBack = onBack,
        )

        AppCardContainer {
            DatePillsRow(
                selectedDate = selectedDate,
                today = today,
                onDateChange = onDateChange,
                modifier = Modifier.fillMaxWidth(),
            )

            AppCardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent, RoundedCornerShape(28.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(appSoftColor(), RoundedCornerShape(32.dp))
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            formatNumber(totals.calories),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text("$rangeMin-$rangeMax target", color = AppMuted)
                    }
                }

                IntakeRangeBar(
                    currentProgress = currentProgress,
                    lowerTarget = rangeMin,
                    upperTarget = rangeMax,
                )

                MacroRow(
                    protein = totals.protein,
                    carbs = totals.carbs,
                    fat = totals.fat,
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(AppBlue, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = {
                            expandedEntryId = null
                            onAddFood()
                        }) {
                            Text("+", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(appSoftColor(), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = {
                            expandedEntryId = null
                            onVoiceLog()
                        }) {
                            Text("\uD83C\uDFA4", color = AppBlue, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .background(appSoftColor(), RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = {
                            expandedEntryId = null
                            aiLogTranscript = ""
                            aiLogSheetOpen = true
                        }) {
                            Text("AI Log", color = AppBlue, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        if (!voiceStatusLabel.isNullOrBlank() || !voiceTranscript.isNullOrBlank() || voiceCandidateMatches.isNotEmpty()) {
            VoiceStatusCard(
                label = voiceStatusLabel,
                transcript = voiceTranscript,
                message = voiceStatusMessage,
                candidateMatches = voiceCandidateMatches,
                retryVisible = voiceRetryVisible,
                onRetry = onRetryVoiceLog,
                onDismiss = onDismissVoiceFeedback,
                onSelectCandidate = onSelectVoiceCandidate,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Meal Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
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
                        MealCopyHeaderActions(
                            meal = meal,
                            copyOptions = copyOptions,
                            isViewingToday = isViewingToday,
                            hasMealEntries = mealEntries.isNotEmpty(),
                            onCopyMealFromDate = { selectedMeal, sourceDate ->
                                expandedEntryId = null
                                onCopyMealFromDate(selectedMeal, sourceDate)
                            },
                            onCopyMealToToday = { selectedMeal ->
                                expandedEntryId = null
                                onCopyMealToToday(selectedMeal)
                            },
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

    if (aiLogSheetOpen) {
        ModalBottomSheet(onDismissRequest = { aiLogSheetOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("AI Log", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = aiLogTranscript,
                    onValueChange = { aiLogTranscript = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Transcript") },
                    minLines = 4,
                )
                if (!aiLogErrorMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = appSoftColor()),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("AI Log failed", fontWeight = FontWeight.Bold)
                            Text(aiLogErrorMessage, color = AppMuted, style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = onRetryAiLogParse, enabled = !aiLogIsParsing) {
                                    Text("Retry", color = AppBlue)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { aiLogSheetOpen = false }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onAiLogParse(aiLogTranscript) },
                        enabled = !aiLogIsParsing,
                    ) {
                        Text(if (aiLogIsParsing) "Parsing..." else "Parse")
                    }
                }
            }
        }
    }

    LaunchedEffect(aiLogParsedDrafts) {
        if (aiLogParsedDrafts.isNotEmpty()) {
            reviewDrafts = aiLogParsedDrafts
            aiLogSheetOpen = false
        }
    }

    if (reviewDrafts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Review parsed foods") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reviewDrafts.forEachIndexed { index, draft ->
                        var mealMenuExpanded by remember(index) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(draft.foodName, fontWeight = FontWeight.Bold)
                                val detailText = when (draft.matchStatus) {
                                    AiDiaryEntryMatchStatus.Matched -> "Matched"
                                    AiDiaryEntryMatchStatus.Creatable -> {
                                        val calories = draft.creatableFood?.nutrients?.calories
                                        if (calories != null) "${formatNumber(calories)} cal • Create new food" else "Create new food"
                                    }
                                    AiDiaryEntryMatchStatus.Unresolved -> "Needs review"
                                }
                                Text(detailText, color = AppMuted)
                            }
                            Box {
                                TextButton(onClick = { mealMenuExpanded = true }) {
                                    Text(draft.meal.label, fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(
                                    expanded = mealMenuExpanded,
                                    onDismissRequest = { mealMenuExpanded = false },
                                ) {
                                    Meal.entries.forEach { meal ->
                                        DropdownMenuItem(
                                            text = { Text(meal.label) },
                                            onClick = {
                                                reviewDrafts = reviewDrafts.toMutableList().also {
                                                    it[index] = draft.copy(meal = meal)
                                                }
                                                mealMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                            TextButton(onClick = { reviewDrafts = reviewDrafts.filterIndexed { i, _ -> i != index } }) {
                                Text("Remove", color = Color(0xFFFF5449))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAiLogConfirm(reviewDrafts)
                        reviewDrafts = emptyList()
                    },
                ) {
                    Text("Confirm", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    reviewDrafts = emptyList()
                    onAiLogCancelReview()
                }) {
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
                Text("Copy ${meal.label.lowercase()} from", fontWeight = FontWeight.ExtraBold)
                Text(
                    "Days without this meal are unavailable.",
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

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 7,
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                        Box(
                            modifier = Modifier.width(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = AppMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    monthDays.forEach { date ->
                        if (date == null) {
                            Box(modifier = Modifier.width(36.dp).height(36.dp))
                        } else {
                            val selectable = date in selectableDateSet
                            val isToday = date == today
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(36.dp)
                                    .background(
                                        when {
                                            selectable -> AppBlue.copy(alpha = 0.12f)
                                            else -> appSoftColor()
                                        },
                                        RoundedCornerShape(12.dp),
                                    )
                                    .border(
                                        width = if (isToday) 1.dp else 0.dp,
                                        color = if (isToday) appBorderStrongColor() else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .then(
                                        if (selectable) {
                                            Modifier.clickable { onSelectDate(date) }
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = if (selectable) AppBlue else AppMuted.copy(alpha = 0.45f),
                                    fontWeight = if (selectable) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
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
private fun IntakeRangeBar(
    currentProgress: Float,
    lowerTarget: Int,
    upperTarget: Int,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        val barWidth = maxWidth
        val firstBreak = maxWidth / 3f
        val secondBreak = firstBreak * 2f
        val markerOffset = ((barWidth - 18.dp) * currentProgress).coerceIn(0.dp, barWidth - 18.dp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(appBorderColor(), RoundedCornerShape(999.dp)),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFFF0D58A), RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFF9BE2AB)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFFFFC4BA), RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)),
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = firstBreak - 1.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .padding(start = secondBreak - 1.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = markerOffset)
                        .size(18.dp)
                        .background(AppBlue, CircleShape),
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "$lowerTarget",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(72.dp)
                        .align(Alignment.CenterStart)
                        .absoluteOffset((firstBreak - 36.dp).coerceAtLeast(0.dp), 0.dp)
                )
                Text(
                    "$upperTarget",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(72.dp)
                        .align(Alignment.CenterStart)
                        .absoluteOffset((secondBreak - 36.dp).coerceAtLeast(0.dp), 0.dp)
                )
            }
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
            onAiLogParse = {},
            aiLogIsParsing = false,
            aiLogErrorMessage = null,
            aiLogParsedDrafts = listOf(
                AiDiaryEntryDraft(
                    foodName = "Greek yogurt",
                    meal = Meal.Breakfast,
                    matchStatus = AiDiaryEntryMatchStatus.Creatable,
                    creatableFood = AiCreatableFoodDraft(
                        name = "Greek yogurt",
                        servingQuantity = 1.0,
                        servingUnit = "cup",
                        nutrients = com.philipcosgrave.calorietracker.model.Nutrients(calories = 170.0),
                    ),
                ),
            ),
            onAiLogConfirm = {},
            onAiLogCancelReview = {},
            onRetryAiLogParse = {},
            onOpenSyncSettings = {},
            onDeleteEntry = {},
            onEditEntry = {},
            onRetryVoiceLog = {},
            onDismissVoiceFeedback = {},
            onSelectVoiceCandidate = {},
        )
    }
}

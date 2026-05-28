package com.philipcosgrave.calorietracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.componentSummary
import com.philipcosgrave.calorietracker.domain.compatibleMeasurementUnits
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.domain.measurementUnits
import com.philipcosgrave.calorietracker.domain.scale
import com.philipcosgrave.calorietracker.domain.servingLabel
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.SortMode
import com.philipcosgrave.calorietracker.model.Totals
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import kotlinx.coroutines.android.awaitFrame
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val AppBlue = Color(0xFF1677F0)
val AppSuccess = Color(0xFF36C15B)
val AppBackground = Color(0xFFF3F6FB)
val AppCard = Color(0xFFFFFFFF)
val AppMuted = Color(0xFF7B8594)
val AppBorder = Color(0xFFD9DFEA)
val AppBorderStrong = Color(0xFFC4CBD8)
val AppSoft = Color(0xFFF6F8FC)

@Composable
fun appBackgroundColor(): Color = MaterialTheme.colorScheme.background

@Composable
fun appCardColor(): Color = MaterialTheme.colorScheme.surface

@Composable
fun appMutedColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun appBorderColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

@Composable
fun appBorderStrongColor(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.48f)

@Composable
fun appSoftColor(): Color = MaterialTheme.colorScheme.surfaceVariant.copy()

fun isDigitsOnlyInput(value: String): Boolean = value.isEmpty() || value.all { it.isDigit() }
fun isDecimalNumberInput(value: String): Boolean =
    value.isEmpty() || Regex("""^\d*(\.\d{0,2})?$""").matches(value)
fun normalizeDecimalNumberInput(value: String): String = if (value.startsWith(".")) "0$value" else value

@Composable
fun Page(
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackgroundColor())
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun PageHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back", color = AppBlue) }
            Box(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.width(72.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
fun TotalsGrid(totals: Totals) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Metric(formatNumber(totals.calories), "Calories", Modifier.weight(1f))
        Metric("${formatNumber(totals.protein)}g", "Protein", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Metric("${formatNumber(totals.carbs)}g", "Carbs", Modifier.weight(1f))
        Metric("${formatNumber(totals.fat)}g", "Fat", Modifier.weight(1f))
    }
}

@Composable
fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryRow(
    entry: DiaryEntry,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val nutrients = entry.food.nutrients.scale(entry.servingMultiplier)
    InteractiveFoodRow(
        title = entry.food.name,
        trailing = "${formatNumber(nutrients.calories)} cal",
        onClick = {},
        onLongClick = onToggleExpanded,
        onEdit = onEdit,
        onDelete = onDelete,
        compact = false,
        supporting = if (expanded) {
            "${entry.food.brand.ifBlank { entry.food.servingLabel }} - ${formatNumber(entry.loggedAmount)} ${entry.loggedUnit}"
        } else {
            null
        },
    )
}

@Composable
fun FoodSearchRow(
    item: FoodItem,
    showCalories: Boolean,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    expanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val manageable = onEdit != null && onToggleExpanded != null
    InteractiveFoodRow(
        title = item.name,
        trailing = if (showCalories) "${formatNumber(item.nutrients.calories)} cal" else null,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        onLongClick = if (manageable) onToggleExpanded else onEdit,
        onEdit = onEdit,
        onDelete = onDelete,
        compact = true,
        supporting = if (!manageable || expanded) {
            buildList {
                if (item.kind == FoodKind.Recipe) add("Recipe")
                if (item.brand.isNotBlank()) add(item.brand)
                add(item.servingLabel)
                if (!manageable && showCalories) add("${formatNumber(item.nutrients.calories)} cal")
            }.joinToString(" - ")
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InteractiveFoodRow(
    title: String,
    trailing: String?,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    compact: Boolean,
    supporting: String?,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit?.invoke()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete?.invoke()
                    false
                }
                else -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onEdit != null,
        enableDismissFromEndToStart = onDelete != null,
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (onEdit != null) {
                        SwipeActionBackground(
                            alignment = Alignment.CenterStart,
                            color = Color(0xFF4CAF50),
                        ) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (onDelete != null) {
                        SwipeActionBackground(
                            alignment = Alignment.CenterEnd,
                            color = Color(0xFFFF5449),
                        ) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }
                else -> Unit
            }
        },
    ) {
        FoodRowCardContent(
            title = title,
            trailing = trailing,
            compact = compact,
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onLongClick = onLongClick,
        ) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (!supporting.isNullOrBlank()) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = appMutedColor(),
                )
            }
        }
    }
}

@Composable
private fun FoodRowCardContent(
    title: String,
    trailing: String?,
    compact: Boolean,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    supporting: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = appSoftColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, appBorderStrongColor()),
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick,
                    onLongClick = onLongClick,
                )
                .padding(
                    start = 18.dp,
                    top = if (compact) 6.dp else 16.dp,
                    end = 18.dp,
                    bottom = if (compact) 6.dp else 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
                content = supporting,
            )
            if (trailing != null) {
                Text(
                    trailing,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun SwipeActionBackground(
    alignment: Alignment,
    color: Color,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        icon()
    }
}

@Composable
fun RecipeComponentRow(
    component: RecipeComponent,
    onChange: (RecipeComponent) -> Unit,
    onRemove: () -> Unit,
) {
    var amount by remember(component.item.id, component.amount) { mutableStateOf(formatNumber(component.amount)) }
    val availableUnits = remember(component.unit, component.item.servingUnit) {
        buildList {
            if (component.unit.isNotBlank()) add(component.unit)
            addAll(compatibleMeasurementUnits(component.item.servingUnit).filterNot { it in this })
            if (component.item.servingUnit.isNotBlank() && component.item.servingUnit !in this) add(component.item.servingUnit)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = appSoftColor()),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(component.item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${formatNumber(component.item.nutrients.calories)} cal", style = MaterialTheme.typography.bodySmall, color = appMutedColor())
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        val normalized = normalizeDecimalNumberInput(it)
                        if (isDecimalNumberInput(normalized)) {
                            amount = normalized
                            onChange(component.copy(amount = normalized.toDoubleOrNull() ?: component.amount))
                        }
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                )
                UnitPicker(component.unit, { onChange(component.copy(unit = it)) }, Modifier.weight(1f), availableUnits)
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                )
            }
        }
    }
}

@Composable
fun KindIcon(kind: FoodKind) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (kind == FoodKind.Recipe) "R" else "I", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun OverflowMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("\u22EE", color = AppMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
        }
    }
}

@Composable
fun MealPicker(meal: Meal, onMealChange: (Meal) -> Unit, darkMode: Boolean = false) {
    val textColor = if (darkMode) Color.White else Color.Unspecified
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.take(2).forEach { option ->
                MealChoice(option, meal == option, onMealChange, textColor, Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.drop(2).forEach { option ->
                MealChoice(option, meal == option, onMealChange, textColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MealChoice(option: Meal, selected: Boolean, onMealChange: (Meal) -> Unit, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier.clickable { onMealChange(option) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = { onMealChange(option) })
        Text(option.label.uppercase(), color = color, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DateStepper(date: LocalDate, onDateChange: (LocalDate) -> Unit, darkMode: Boolean = false) {
    val textColor = if (darkMode) Color.White else Color.Unspecified
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Day", color = textColor, fontWeight = FontWeight.Bold)
        Button(onClick = { onDateChange(date.minusDays(1)) }) { Text("<") }
        Text(date.format(DateTimeFormatter.ISO_LOCAL_DATE), color = textColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Button(onClick = { onDateChange(date.plusDays(1)) }) { Text(">") }
    }
}

@Composable
fun UnitPicker(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, units: List<String> = measurementUnits) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text("Unit") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            readOnly = true,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppBlue,
                unfocusedBorderColor = appBorderColor(),
            ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(text = { Text(unit) }, onClick = { expanded = false; onChange(unit) })
            }
        }
    }
}

@Composable
fun AppFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppBlue,
            unfocusedBorderColor = appBorderColor(),
            focusedContainerColor = appCardColor(),
            unfocusedContainerColor = appCardColor(),
        ),
    )
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(
        containerColor = AppBlue,
        disabledContainerColor = Color(0xFFCAD1DB),
    ),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = colors,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppCardContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = appCardColor()),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(appCardColor(), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            val shape: Shape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (selected) AppBlue else appSoftColor(), shape)
                    .border(1.dp, if (selected) AppBlue else appBorderStrongColor(), shape)
                    .clickable { onSelectedIndexChange(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontWeight = FontWeight.Bold, color = if (selected) Color.White else AppMuted)
            }
        }
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(color = appBorderColor())
}

@Composable
fun SortMenu(
    value: SortMode,
    onChange: (SortMode) -> Unit,
    modifier: Modifier = Modifier,
    labelForMode: (SortMode) -> String = { it.label },
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text("=")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Sort by") }, onClick = { })
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(if (mode == value) "* ${labelForMode(mode)}" else labelForMode(mode)) },
                    onClick = {
                        expanded = false
                        onChange(mode)
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 300)
@Composable
private fun TotalsPreview() {
    PreviewData.Theme {
        Page {
            TotalsGrid(PreviewData.totals)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 500)
@Composable
private fun RowsPreview() {
    PreviewData.Theme {
        Page {
            DiaryEntryRow(
                entry = PreviewData.diaryEntries.first(),
                expanded = true,
                onToggleExpanded = {},
                onEdit = {},
                onDelete = {},
            )
            FoodSearchRow(item = PreviewData.foods.last(), showCalories = true, onClick = {}, onEdit = {}, onDelete = {})
            RecipeComponentRow(component = PreviewData.recipeComponent, onChange = {}, onRemove = {})
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 450)
@Composable
private fun PickersPreview() {
    PreviewData.Theme {
        Page {
            MealPicker(meal = Meal.Lunch, onMealChange = {})
            DatePillsRow(selectedDate = PreviewData.date, today = PreviewData.date,  onDateChange = {})
            UnitPicker(value = "banana", onChange = {}, units = listOf("banana", "serving", "cup"))
            SortMenu(value = SortMode.Recent, onChange = {})
        }
    }
}

@Composable
fun DatePillsRow(
    selectedDate: LocalDate,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clampedSelectedDate = minOf(selectedDate, today)
    val earliestDate = minOf(clampedSelectedDate, today.minusDays(60))
    val dates = remember(clampedSelectedDate, today) {
        generateSequence(earliestDate) { current ->
            current.takeIf { it.isBefore(today) }?.plusDays(1)
        }.toList()
    }

    ScrollablePillSelector(
        options = dates,
        selectedOption = clampedSelectedDate,
        currentOption = today,
        currentLabel = "Today",
        onSelect = onDateChange,
        labelForOption = { date -> date.format(DateTimeFormatter.ofPattern("MMM d")) },
        modifier = modifier,
    )
}

@Composable
fun <T> ScrollablePillSelector(
    options: List<T>,
    selectedOption: T,
    currentOption: T? = null,
    currentLabel: String? = null,
    onSelect: (T) -> Unit,
    labelForOption: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val historicalOptions = remember(options, currentOption) {
        if (currentOption == null) options else options.filter { it != currentOption }
    }
    val selectedHistoricalIndex = historicalOptions.indexOf(selectedOption)
    val listState = rememberLazyListState()
    val fadeColor = appCardColor()

    LaunchedEffect(selectedHistoricalIndex, historicalOptions.size, currentOption, selectedOption) {
        if (historicalOptions.isEmpty()) return@LaunchedEffect
        val targetIndex =
            when {
                currentOption != null && selectedOption == currentOption -> historicalOptions.lastIndex
                selectedHistoricalIndex >= 0 -> selectedHistoricalIndex
                else -> historicalOptions.lastIndex
            }
        listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        awaitFrame()
        if (currentOption == null && selectedHistoricalIndex >= 0) {
            val layoutInfo = listState.layoutInfo
            val selectedItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == selectedHistoricalIndex }
            if (selectedItem != null) {
                val viewportWidth = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val centeredOffset = -((viewportWidth - selectedItem.size) / 2)
                listState.animateScrollToItem(selectedHistoricalIndex, centeredOffset)
            }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(historicalOptions, key = { _, option -> option.hashCode() }) { _, option ->
                    val selected = option == selectedOption
                    Box(
                        modifier = Modifier
                            .background(if (selected) AppBlue else appSoftColor(), RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp)
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = { onSelect(option) }) {
                            Text(
                                labelForOption(option),
                                color = if (selected) Color.White else AppMuted,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            if (listState.canScrollBackward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(24.dp)
                        .height(48.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(fadeColor, fadeColor.copy(alpha = 0f)),
                            ),
                        ),
                )
            }

            if (listState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(24.dp)
                        .height(48.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(fadeColor.copy(alpha = 0f), fadeColor),
                            ),
                        ),
                )
            }
        }

        if (currentOption != null && currentLabel != null) {
            val currentSelected = selectedOption == currentOption
            Box(
                modifier = Modifier
                    .background(if (currentSelected) AppBlue else appSoftColor(), RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = { onSelect(currentOption) }) {
                    Text(
                        currentLabel,
                        color = if (currentSelected) Color.White else AppBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

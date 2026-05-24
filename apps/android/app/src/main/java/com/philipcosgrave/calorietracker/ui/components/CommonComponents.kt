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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.componentSummary
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val AppBlue = Color(0xFF1677F0)
val AppBackground = Color(0xFFF3F6FB)
val AppCard = Color(0xFFFFFFFF)
val AppMuted = Color(0xFF7B8594)
val AppBorder = Color(0xFFD9DFEA)
val AppBorderStrong = Color(0xFFC4CBD8)
val AppSuccess = Color(0xFF36C15B)
val AppSoft = Color(0xFFF6F8FC)

fun isDigitsOnlyInput(value: String): Boolean = value.isEmpty() || value.all { it.isDigit() }

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
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

@Composable
fun DiaryEntryRow(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val nutrients = entry.food.nutrients.scale(entry.servingMultiplier)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onEdit)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.food.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${entry.food.brand.ifBlank { entry.food.servingLabel }} • ${formatNumber(entry.servingMultiplier)} serving",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text("${formatNumber(nutrients.calories)} cal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OverflowMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
fun FoodSearchRow(
    item: FoodItem,
    showCalories: Boolean,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard),
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick,
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildList {
                        if (item.kind == FoodKind.Recipe) add("Recipe")
                        add(item.servingLabel)
                        if (item.brand.isNotBlank()) add(item.brand)
                        if (showCalories) add("${formatNumber(item.nutrients.calories)} cal")
                    }.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMuted,
                )
            }
            if (onEdit != null && onDelete != null) {
                OverflowMenu(onEdit = onEdit, onDelete = onDelete)
            }
        }
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
            if (component.item.servingUnit.isNotBlank() && component.item.servingUnit !in this) add(component.item.servingUnit)
            addAll(measurementUnits.filterNot { it in this })
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppSoft),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(component.item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${formatNumber(component.item.nutrients.calories)} cal", style = MaterialTheme.typography.bodySmall, color = AppMuted)
                }
                TextButton(onClick = onRemove) { Text("-", color = Color(0xFFFF5449), style = MaterialTheme.typography.titleMedium) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (isDigitsOnlyInput(it)) {
                            amount = it
                            onChange(component.copy(amount = it.toDoubleOrNull() ?: component.amount))
                        }
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                )
                UnitPicker(component.unit, { onChange(component.copy(unit = it)) }, Modifier.weight(1f), availableUnits)
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
fun OverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text("\u22EE", color = AppMuted) }
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
            onValueChange = onChange,
            label = { Text("Unit") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppBlue,
                unfocusedBorderColor = AppBorder,
            ),
        )
        Box(modifier = Modifier.fillMaxSize().clickable { expanded = true })
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
            unfocusedBorderColor = AppBorder,
            focusedContainerColor = AppCard,
            unfocusedContainerColor = AppCard,
        ),
    )
}

@Composable
fun AppPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppBlue, disabledContainerColor = Color(0xFFCAD1DB)),
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppCardContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard),
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
            .background(Color.White, RoundedCornerShape(20.dp))
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
                    .background(if (selected) AppBlue else AppSoft, shape)
                    .border(1.dp, if (selected) AppBlue else AppBorderStrong, shape)
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
    HorizontalDivider(color = AppBorder)
}

@Composable
fun SortMenu(value: SortMode, onChange: (SortMode) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text("=")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Sort by") }, onClick = { })
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(if (mode == value) "* ${mode.label}" else mode.label) },
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
            DiaryEntryRow(entry = PreviewData.diaryEntries.first(), onEdit = {}, onDelete = {})
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
    val dates = buildList {
        var cursor = minOf(selectedDate, today)
        add(cursor)
        while (size < 3 && cursor.isAfter(LocalDate.MIN.plusDays(1))) {
            cursor = cursor.minusDays(1)
            add(0, cursor)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dates.forEach { date ->
            val selected = date == selectedDate
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) AppBlue else Color(0xFFF1F4F9), RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = { onDateChange(date) }) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("MMM d")),
                        color = if (selected) Color.White else AppMuted,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

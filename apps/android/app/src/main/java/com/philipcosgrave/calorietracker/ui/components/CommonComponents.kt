package com.philipcosgrave.calorietracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun Header(onOpenSyncSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CalorieTracker", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Manual labels, private barcode shortcuts, cloud sync when signed in.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { }) {
                Text("Sign in with Google")
            }
            TextButton(onClick = onOpenSyncSettings) {
                Text("Sync")
            }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onEdit)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.food.name, fontWeight = FontWeight.Bold)
                Text("${entry.food.brand.ifBlank { "No brand" }} - ${entry.food.servingLabel} x ${formatNumber(entry.servingMultiplier)}")
            }
            Text("${formatNumber(nutrients.calories)} cal", fontWeight = FontWeight.Bold)
            OverflowMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
fun FoodSearchRow(
    item: FoodItem,
    showCalories: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KindIcon(item.kind)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text(
                    listOf(item.brand, item.componentSummary()).filter { it.isNotBlank() }.joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.servingLabel, style = MaterialTheme.typography.bodySmall)
                if (showCalories) Text("${formatNumber(item.nutrients.calories)} cal", fontWeight = FontWeight.Bold)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KindIcon(component.item.kind)
                Column(modifier = Modifier.weight(1f)) {
                    Text(component.item.name, fontWeight = FontWeight.Bold)
                    Text(component.item.servingLabel, style = MaterialTheme.typography.bodySmall)
                    if (component.item.components.isNotEmpty()) {
                        Text(component.item.componentSummary(), style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        onChange(component.copy(amount = it.toDoubleOrNull() ?: component.amount))
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
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
        TextButton(onClick = { expanded = true }) { Text("...") }
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
private fun HeaderAndTotalsPreview() {
    PreviewData.Theme {
        Page {
            Header(onOpenSyncSettings = {})
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
            DateStepper(date = PreviewData.date, onDateChange = {})
            UnitPicker(value = "banana", onChange = {}, units = listOf("banana", "serving", "cup"))
            SortMenu(value = SortMode.Recent, onChange = {})
        }
    }
}

package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.model.WeightEntry
import com.philipcosgrave.calorietracker.ui.components.AppFormField
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.AppPrimaryButton
import com.philipcosgrave.calorietracker.ui.components.DatePillsRow
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.ui.components.isDecimalNumberInput
import com.philipcosgrave.calorietracker.ui.preview.PreviewData
import java.time.LocalDate

@Composable
fun LogWeightScreen(
    initialDate: LocalDate,
    weightUnit: SyncSettings.WeightUnit,
    existingEntry: WeightEntry? = null,
    onBack: () -> Unit,
    onSave: (WeightEntry) -> Unit,
) {
    var selectedDate by remember(existingEntry?.date, initialDate) { mutableStateOf(existingEntry?.date ?: initialDate) }
    var weightText by remember(existingEntry?.weightKg, weightUnit) {
        mutableStateOf(existingEntry?.weightKg?.let { formatNumber(convertWeightFromKg(it, weightUnit)) } ?: "")
    }

    Page {
        PageHeader("Log Weight", onBack = onBack)
        Text("Save a weight entry in ${weightUnitDescription(weightUnit)}.", color = AppMuted)
        DatePillsRow(
            selectedDate = selectedDate,
            today = LocalDate.now(),
            onDateChange = { if (!it.isAfter(LocalDate.now())) selectedDate = it },
        )
        AppFormField(
            value = weightText,
            onValueChange = {
                if (isDecimalNumberInput(it)) {
                    weightText = it
                }
            },
            label = "Weight (${weightUnitLabel(weightUnit)})",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppPrimaryButton(
                text = if (existingEntry == null) "Save Weight" else "Update Weight",
                onClick = {
                    val entered = weightText.toDoubleOrNull() ?: return@AppPrimaryButton
                    onSave(
                        WeightEntry(
                            id = existingEntry?.id ?: "",
                            date = selectedDate,
                            weightKg = convertWeightToKg(entered, weightUnit),
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = weightText.toDoubleOrNull() != null,
            )
        }
    }
}

private fun convertWeightFromKg(weightKg: Double, unit: SyncSettings.WeightUnit): Double =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> weightKg
        SyncSettings.WeightUnit.Pounds -> weightKg * 2.2046226218
    }

private fun convertWeightToKg(value: Double, unit: SyncSettings.WeightUnit): Double =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> value
        SyncSettings.WeightUnit.Pounds -> value / 2.2046226218
    }

private fun weightUnitLabel(unit: SyncSettings.WeightUnit): String =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> "kg"
        SyncSettings.WeightUnit.Pounds -> "lb"
    }

private fun weightUnitDescription(unit: SyncSettings.WeightUnit): String =
    when (unit) {
        SyncSettings.WeightUnit.Kilograms -> "kilograms"
        SyncSettings.WeightUnit.Pounds -> "pounds"
    }

@Preview(showBackground = true, widthDp = 412, heightDp = 700)
@Composable
private fun LogWeightScreenPreview() {
    PreviewData.Theme {
        LogWeightScreen(
            initialDate = PreviewData.date,
            weightUnit = PreviewData.syncSettings.weightUnit,
            existingEntry = PreviewData.weightEntries.last(),
            onBack = {},
            onSave = { },
        )
    }
}

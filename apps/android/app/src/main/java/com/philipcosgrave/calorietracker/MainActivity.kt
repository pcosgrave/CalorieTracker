package com.philipcosgrave.calorietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieTrackerApp()
        }
    }
}

@Composable
private fun CalorieTrackerApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header()
                DailySummary()
                FoodLabelForm()
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CalorieTracker", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Manual labels, private barcode shortcuts, cloud sync when signed in.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = { }) {
            Text("Sign in with Google")
        }
    }
}

@Composable
private fun DailySummary() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Today", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("140", "Calories", Modifier.weight(1f))
                Metric("20g", "Protein", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("8g", "Carbs", Modifier.weight(1f))
                Metric("3g", "Fat", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FoodLabelForm() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add Food From Label", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Barcode") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Product name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Brand") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Serving") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Grams") },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Calories") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Protein") },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Carbs") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("Fat") },
                    modifier = Modifier.weight(1f),
                )
            }
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Save Food")
            }
        }
    }
}

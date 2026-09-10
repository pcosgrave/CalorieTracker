package com.philipcosgrave.calorietracker.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.*
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.ui.components.*

@Composable fun FoodConfirmationScreen(title: String, foods: List<Pair<FoodItem, String>>, primaryLabel: String, onPrimary: () -> Unit, onDone: () -> Unit, secondaryLabel: String = "Done") {
    Page {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.displayLarge) }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        foods.forEach { (food, detail) -> AppCardContainer {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FoodPhoto(food.photoPath, Modifier.size(64.dp))
                Column { Text(food.name, fontWeight = FontWeight.Bold); Text(detail, color = AppMuted) }
            }
        } }
        AppPrimaryButton(primaryLabel, onPrimary, Modifier.fillMaxWidth())
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(secondaryLabel) }
    }
}

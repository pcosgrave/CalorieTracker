package com.philipcosgrave.calorietracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.philipcosgrave.calorietracker.domain.formatAmount
import com.philipcosgrave.calorietracker.model.*

@Composable
fun AdditionalNutrition(nutrients: Nutrients) {
    val fields = additionalNutrientFields.filterKeys { it in nutrients.additional }
    AppCardContainer {
        if (fields.isEmpty()) Text("No additional nutrition information available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        fields.forEach { (key, field) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(field.first)
                Text("${formatAmount(nutrients.additional.getValue(key))} ${field.second}")
            }
        }
    }
}

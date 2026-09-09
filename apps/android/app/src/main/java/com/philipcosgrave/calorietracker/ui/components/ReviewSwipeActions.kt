package com.philipcosgrave.calorietracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSwipeActions(enabled: Boolean = true, onEdit: () -> Unit, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val edit by rememberUpdatedState(onEdit)
    val delete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.35f }, confirmValueChange = {
        if (enabled) when (it) {
            SwipeToDismissBoxValue.StartToEnd -> edit()
            SwipeToDismissBoxValue.EndToStart -> delete()
            else -> Unit
        }
        false
    })
    SwipeToDismissBox(state, enableDismissFromStartToEnd = enabled, enableDismissFromEndToStart = enabled,
        modifier = Modifier.semantics {
            if (enabled) customActions = listOf(
                CustomAccessibilityAction("Edit food") { edit(); true },
                CustomAccessibilityAction("Delete food") { delete(); true },
            )
        },
        backgroundContent = {
            val deleting = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            if (state.dismissDirection != SwipeToDismissBoxValue.Settled) {
                Box(Modifier.fillMaxSize().background(if (deleting) Color(0xFFFF5449) else Color(0xFF4CAF50), RoundedCornerShape(18.dp)).padding(20.dp),
                    contentAlignment = if (deleting) Alignment.CenterEnd else Alignment.CenterStart) {
                    Icon(if (deleting) Icons.Filled.Delete else Icons.Filled.Edit, if (deleting) "Delete" else "Edit", tint = Color.White)
                }
            }
        },
    ) { content() }
}

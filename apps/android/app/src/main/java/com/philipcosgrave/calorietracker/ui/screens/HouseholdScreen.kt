package com.philipcosgrave.calorietracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.philipcosgrave.calorietracker.ui.components.AppCardContainer
import com.philipcosgrave.calorietracker.ui.components.AppFormField
import com.philipcosgrave.calorietracker.ui.components.AppPrimaryButton
import com.philipcosgrave.calorietracker.ui.components.AppMuted
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import com.philipcosgrave.calorietracker.data.sync.RemoteHousehold
import com.philipcosgrave.calorietracker.data.sync.RemoteHouseholdMember
import com.philipcosgrave.calorietracker.domain.HouseholdScreenState
import com.philipcosgrave.calorietracker.domain.householdScreenState

@Composable
fun HouseholdScreen(household: RemoteHousehold?, members: List<RemoteHouseholdMember>, loading: Boolean, onCreate: (String) -> Unit, onBack: () -> Unit, onOpenSettings: () -> Unit = {}) {
    var householdName by rememberSaveable { mutableStateOf("") }
    when (householdScreenState(loading, household != null)) {
        HouseholdScreenState.Loading -> { Page { PageHeader("Household", onBack = onBack); Text("Loading household…") }; return }
        HouseholdScreenState.Create -> Unit
        HouseholdScreenState.Dashboard -> Unit
    }
    if (householdScreenState(loading, household != null) == HouseholdScreenState.Create) {
        Page {
            PageHeader("Household", onBack = onBack)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(42.dp))
                Text("🌳  ⌂  🌳", style = MaterialTheme.typography.displaySmall, color = Color(0xFF80A98D))
                Text("Create your household", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Share meals, leftovers, and more\nwith the people you live with.", color = AppMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            AppCardContainer {
                AppFormField(value = householdName, onValueChange = { householdName = it }, label = "Household name", modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { listOf(Color(0xFF2D8069), Color(0xFF9BC9C7), Color(0xFF6B8EE8), Color(0xFFC28BC1), Color(0xFFFFC985), Color(0xFFD8A56B)).forEach { color -> androidx.compose.material3.Surface(Modifier.size(28.dp), shape = CircleShape, color = color) {} } }
                AppPrimaryButton("Create household", onClick = { if (householdName.isNotBlank()) onCreate(householdName) }, modifier = Modifier.fillMaxWidth())
            }
        }
        return
    }
    val activeHousehold = household ?: return
    Page {
        PageHeader("${activeHousehold.name} ⌄", onBack = onBack)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("${members.size} members", color = AppMuted) }
            Text("♧", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onOpenSettings) { Text("⚙", style = MaterialTheme.typography.titleLarge) }
        }
        AppCardContainer {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                members.take(4).map { it.name to Color(0xFF80A98D) }.forEach { (name, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { androidx.compose.material3.Surface(Modifier.size(48.dp), shape = CircleShape, color = color) { Text(name.take(1), modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold) }; Text(name, style = MaterialTheme.typography.bodySmall) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { androidx.compose.material3.Surface(Modifier.size(48.dp), shape = CircleShape, color = Color(0xFFF0F1ED)) { Text("+", modifier = Modifier.padding(13.dp), color = Color(0xFF2D8069), style = MaterialTheme.typography.titleLarge) }; Text("Invite", style = MaterialTheme.typography.bodySmall) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Text("Meals", fontWeight = FontWeight.Bold); Text("Leftovers", color = AppMuted) }
        HorizontalDivider(color = Color(0xFF2D8069))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Shared with household", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("›", style = MaterialTheme.typography.titleLarge) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppCardContainer(Modifier.weight(1f)) { Text("🍝", style = MaterialTheme.typography.displaySmall); Text("Pasta Primavera", fontWeight = FontWeight.Bold); Text("Alex · Dinner · Today", color = AppMuted, style = MaterialTheme.typography.bodySmall); Text("2 left", color = Color(0xFF2D8069)) }
            AppCardContainer(Modifier.weight(1f)) { Text("🍲", style = MaterialTheme.typography.displaySmall); Text("Chicken Rice Bowl", fontWeight = FontWeight.Bold); Text("You · Lunch · Today", color = AppMuted, style = MaterialTheme.typography.bodySmall); Text("1 left", color = Color(0xFF2D8069)) }
        }
    }
}

@Composable
fun HouseholdSettingsScreen(onBack: () -> Unit) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    Page {
        PageHeader("Household settings", onBack = onBack)
        AppCardContainer { Text("⌂   The Taylor Household", fontWeight = FontWeight.Bold); Text("›", modifier = Modifier.align(Alignment.End)) }
        Text("Members (4)", color = AppMuted)
        AppCardContainer { listOf("You" to "Admin", "Alex" to "Member", "Jordan" to "Member", "Sam" to "Member").forEach { (name, role) -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(name, Modifier.weight(1f)); Text(role, color = AppMuted); Text("⋯") }; HorizontalDivider() }; TextButton(onClick = {}) { Text("＋  Invite people", color = Color(0xFF2D8069)) } }
        TextButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete household  ›", color = Color(0xFFE55A5A)) }
    }
    if (showDeleteConfirmation) AlertDialog(onDismissRequest = { showDeleteConfirmation = false }, title = { Text("Delete household?") }, text = { Text("This removes the household and its shared items. This action cannot be undone.") }, confirmButton = { TextButton(onClick = { showDeleteConfirmation = false; onBack() }) { Text("Delete", color = Color(0xFFE55A5A)) } }, dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } })
}

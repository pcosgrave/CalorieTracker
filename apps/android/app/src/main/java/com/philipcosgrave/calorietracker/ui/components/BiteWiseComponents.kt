package com.philipcosgrave.calorietracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.model.Totals
import com.philipcosgrave.calorietracker.domain.formatNumber
import com.philipcosgrave.calorietracker.ui.AppScreen

@Composable fun BiteWiseBottomNavigation(selected: AppScreen, onSelect: (AppScreen) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        listOf(Triple(AppScreen.Home, "⌂", "Home"), Triple(AppScreen.Diary, "≡", "Log"), Triple(AppScreen.Scan, "▣", "Scan"), Triple(AppScreen.Insights, "▥", "Insights"), Triple(AppScreen.More, "•••", "More")).forEach { (screen, icon, title) ->
            NavigationBarItem(selected = selected == screen, onClick = { onSelect(screen) }, icon = { BiteWiseIcon(title, if (selected == screen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }, label = { Text(title) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedTextColor = MaterialTheme.colorScheme.primary))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AddFoodBottomSheet(context: String, onDismiss: () -> Unit, onSearch: () -> Unit, onManual: () -> Unit, onCamera: () -> Unit, onVoice: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Food", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(context, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(Triple("⌕", "Search for a food", onSearch), Triple("▤", "Quick calories", onManual), Triple("▣", "Scan a label or meal", onCamera), Triple("♩", "Speak your food", onVoice)).forEach { (icon, label, action) ->
                Card(Modifier.fillMaxWidth().clickable(onClick = action)) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { BiteWiseIcon(label, MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Text(label, fontWeight = FontWeight.Bold) } }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable fun ExpandableNutritionSummary(totals: Totals, minimum: Int, maximum: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    AppCardContainer {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Text("🔥 ${formatNumber(totals.calories)} / $maximum kcal", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (expanded) "⌃" else "⌄")
        }
        IntakeRangeBar(com.philipcosgrave.calorietracker.domain.calorieRangeProgress(totals.calories, minimum, maximum), minimum, maximum)
        Text("Daily target $minimum–$maximum kcal", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (expanded) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Protein\n${formatNumber(totals.protein)} g")
                Text("Carbs\n${formatNumber(totals.carbs)} g")
                Text("Fat\n${formatNumber(totals.fat)} g")
            }
        }
    }
}

@Composable fun DefaultFoodImage(modifier: Modifier = Modifier) {
    Surface(modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) { BiteWiseIcon("Log", MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable fun BiteWiseIcon(kind: String, tint: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(tint, Offset(w*x,h*y), Offset(w*x2,h*y2), w*.075f, StrokeCap.Round)
        when {
            kind == "Copy" -> { drawRect(tint,Offset(w*.3f,h*.3f),Size(w*.55f,h*.6f),style=Stroke(w*.07f)); line(.15f,.7f,.15f,.1f); line(.15f,.1f,.7f,.1f) }
            kind == "Leftover" -> { drawRoundRect(tint,Offset(w*.15f,h*.35f),Size(w*.7f,h*.5f),androidx.compose.ui.geometry.CornerRadius(w*.08f),style=Stroke(w*.07f)); line(.1f,.35f,.9f,.35f); line(.2f,.18f,.8f,.18f) }
            kind == "Home" -> { line(.12f,.45f,.5f,.12f); line(.5f,.12f,.88f,.45f); line(.23f,.4f,.23f,.87f); line(.23f,.87f,.77f,.87f); line(.77f,.87f,.77f,.4f) }
            kind == "Log" -> { line(.25f,.13f,.25f,.88f); line(.1f,.13f,.1f,.4f); line(.4f,.13f,.4f,.4f); line(.1f,.4f,.4f,.4f); line(.73f,.13f,.73f,.88f); line(.85f,.13f,.73f,.5f) }
            kind == "Insights" -> { line(.2f,.85f,.2f,.55f); line(.5f,.85f,.5f,.2f); line(.8f,.85f,.8f,.4f) }
            kind == "More" -> { listOf(.2f,.5f,.8f).forEach { drawCircle(tint,w*.055f,Offset(w*it,h*.5f)) } }
            kind.contains("Search",true) -> { drawCircle(tint,w*.28f,Offset(w*.4f,h*.4f),style=Stroke(w*.07f)); line(.62f,.62f,.9f,.9f) }
            kind.contains("Speak",true) -> { drawRoundRect(tint,Offset(w*.37f,h*.12f),Size(w*.26f,h*.48f),androidx.compose.ui.geometry.CornerRadius(w*.13f),style=Stroke(w*.07f)); drawArc(tint,0f,180f,false,Offset(w*.2f,h*.24f),Size(w*.6f,h*.5f),style=Stroke(w*.07f)); line(.5f,.74f,.5f,.9f) }
            kind.contains("calories",true) -> { drawRect(tint,Offset(w*.1f,h*.2f),Size(w*.8f,h*.6f),style=Stroke(w*.07f)); line(.25f,.4f,.75f,.4f); line(.25f,.6f,.65f,.6f) }
            else -> { drawRoundRect(tint,Offset(w*.1f,h*.22f),Size(w*.8f,h*.6f),androidx.compose.ui.geometry.CornerRadius(w*.08f),style=Stroke(w*.07f)); drawCircle(tint,w*.17f,Offset(w*.5f,h*.52f),style=Stroke(w*.07f)); line(.35f,.14f,.65f,.14f) }
        }
    }
}

@Composable fun DailyMetricRing(label: String, value: String, caption: String, accent: Color, progress: Float? = null, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                drawArc(accent.copy(alpha=.22f), -90f, 360f, false, style=Stroke(5.dp.toPx(),cap=StrokeCap.Round))
                progress?.let { drawArc(accent,-90f,360f*it.coerceIn(0f,1f),false,style=Stroke(5.dp.toPx(),cap=StrokeCap.Round)) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable fun NutritionSummary(totals: Totals) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Calories" to formatNumber(totals.calories), "Protein" to "${formatNumber(totals.protein)} g", "Fat" to "${formatNumber(totals.fat)} g", "Carbs" to "${formatNumber(totals.carbs)} g").forEach { (label, value) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

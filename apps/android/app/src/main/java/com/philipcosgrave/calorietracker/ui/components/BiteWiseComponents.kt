package com.philipcosgrave.calorietracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
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
@Composable fun AddFoodBottomSheet(onDismiss: () -> Unit, onSearch: () -> Unit, onManual: () -> Unit, onCamera: () -> Unit, onVoice: () -> Unit, newFoodOnly: Boolean = false, onSearchCnf: () -> Unit = {}, onScanBarcode: () -> Unit = {}) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Food", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            data class AddFoodOption(val label: String, val description: String, val action: () -> Unit)
            val options = if (newFoodOnly) listOf(
                AddFoodOption("Scan barcode", "Scan a packaged food barcode", onScanBarcode),
                AddFoodOption("Search CNF foods", "Find foods from the database", onSearchCnf),
                AddFoodOption("Enter manually", "Enter nutrition info yourself", onManual),
            ) else listOf(
                AddFoodOption("Search for a food", "Find items from our food database", onSearch),
                AddFoodOption("Quick calories", "Enter calories and macros yourself", onManual),
                AddFoodOption("Scan a label or meal", "Take a photo of food or nutrition label", onCamera),
                AddFoodOption("Speak your food", "Tell us what you ate", onVoice),
            )
            options.forEach { option ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = option.action),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f)),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .2f)) {
                            Box(contentAlignment = Alignment.Center) { BiteWiseIcon(option.label, modifier = Modifier.size(25.dp)) }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(option.label, fontWeight = FontWeight.Bold)
                            Text(option.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Text("Cancel") }
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
            kind == "Target" -> { drawCircle(tint, w*.34f, Offset(w*.5f, h*.5f), style = Stroke(w*.07f)); drawCircle(tint, w*.14f, Offset(w*.5f, h*.5f), style = Stroke(w*.07f)); line(.5f,.08f,.5f,.2f); line(.5f,.8f,.5f,.92f); line(.08f,.5f,.2f,.5f); line(.8f,.5f,.92f,.5f) }
            kind == "Scale" -> { drawRoundRect(tint, Offset(w*.15f,h*.18f), Size(w*.7f,h*.68f), androidx.compose.ui.geometry.CornerRadius(w*.12f), style = Stroke(w*.07f)); drawArc(tint, 205f, 130f, false, Offset(w*.32f,h*.32f), Size(w*.36f,h*.3f), style = Stroke(w*.06f)); line(.5f,.47f,.62f,.39f) }
            kind == "Health" -> { drawCircle(tint, w*.18f, Offset(w*.33f,h*.36f), style = Stroke(w*.07f)); drawCircle(tint, w*.18f, Offset(w*.67f,h*.36f), style = Stroke(w*.07f)); line(.16f,.44f,.5f,.82f); line(.5f,.82f,.84f,.44f) }
            kind == "Fitbit" -> { listOf(.28f,.5f,.72f).forEachIndexed { column, x -> listOf(.28f,.5f,.72f).forEachIndexed { row, y -> if (!(column == 0 && row == 0) && !(column == 2 && row == 2)) drawCircle(tint, w*.07f, Offset(w*x,h*y)) } } }
            kind == "Profile" -> { drawCircle(tint, w*.17f, Offset(w*.5f,h*.32f), style = Stroke(w*.07f)); drawArc(tint, 205f, 130f, false, Offset(w*.18f,h*.42f), Size(w*.64f,h*.46f), style = Stroke(w*.07f)) }
            kind == "Subscription" -> { line(.16f,.34f,.3f,.68f); line(.3f,.68f,.5f,.26f); line(.5f,.26f,.7f,.68f); line(.7f,.68f,.84f,.34f); line(.16f,.34f,.84f,.34f); line(.3f,.76f,.7f,.76f) }
            kind == "Units" -> { drawRoundRect(tint, Offset(w*.12f,h*.35f), Size(w*.76f,h*.3f), androidx.compose.ui.geometry.CornerRadius(w*.06f), style = Stroke(w*.07f)); listOf(.28f,.46f,.64f,.78f).forEachIndexed { index, x -> line(x,.36f,x,if (index % 2 == 0) .54f else .47f) } }
            kind == "Notifications" -> { drawArc(tint, 200f, 140f, false, Offset(w*.22f,h*.16f), Size(w*.56f,h*.65f), style = Stroke(w*.07f)); line(.2f,.72f,.8f,.72f); drawCircle(tint, w*.05f, Offset(w*.5f,h*.85f)) }
            kind == "Edit" -> { line(.2f,.8f,.33f,.52f); line(.33f,.52f,.76f,.09f); line(.76f,.09f,.91f,.24f); line(.91f,.24f,.48f,.67f); line(.48f,.67f,.2f,.8f); line(.2f,.8f,.18f,.61f) }
            kind == "Delete" -> { drawRoundRect(tint, Offset(w*.28f,h*.3f), Size(w*.44f,h*.58f), androidx.compose.ui.geometry.CornerRadius(w*.04f), style = Stroke(w*.07f)); line(.2f,.24f,.8f,.24f); line(.42f,.12f,.58f,.12f); line(.42f,.46f,.42f,.72f); line(.58f,.46f,.58f,.72f) }
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

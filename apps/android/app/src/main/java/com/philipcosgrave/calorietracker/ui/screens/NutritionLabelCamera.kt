package com.philipcosgrave.calorietracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.philipcosgrave.calorietracker.domain.NutritionLabel
import com.philipcosgrave.calorietracker.ui.components.Page
import com.philipcosgrave.calorietracker.ui.components.PageHeader
import kotlinx.coroutines.*

@Composable
internal fun NutritionLabelCamera(onBack: () -> Unit, onParsed: (NutritionLabel) -> Unit, onAnalyze: suspend (android.graphics.Bitmap, (String) -> Unit) -> NutritionLabel, title: String = "Scan nutrition label", guidance: String = "Include the serving size and nutrition numbers. The photo stays in memory and is discarded after reading.") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    LaunchedEffect(Unit) { if (!allowed) permission.launch(Manifest.permission.CAMERA) }
    fun close() { job?.cancel(); onBack() }
    BackHandler { close() }
    Page {
        PageHeader(title, onBack = ::close)
        Text(guidance)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (busy) {
            LinearProgressIndicator()
            Text(status)
            TextButton(onClick = { job?.cancel() }) { Text("Cancel analysis") }
        } else if (!allowed) {
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
        } else {
            MemoryPhotoCapture(onError = { error = it }, onCaptured = { bitmap ->
                busy = true
                error = null
                job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        val result = withTimeout(300_000) { onAnalyze(bitmap) { status = it } }
                        onParsed(result)
                    } catch (timeout: TimeoutCancellationException) {
                        error = "Reading took too long. Please retake the label photo."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        error = exception.message ?: "Unable to read the label. Please retake the photo."
                    } finally {
                        bitmap.recycle()
                        busy = false
                    }
                }
            })
        }
    }
}

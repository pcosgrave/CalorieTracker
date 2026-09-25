package com.philipcosgrave.calorietracker.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.philipcosgrave.calorietracker.domain.SpeechCaptureSession
import com.philipcosgrave.calorietracker.ui.components.*
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun SpeakFoodScreen(onReview: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val lifecycleOwner = LocalLifecycleOwner.current
    val review by rememberUpdatedState(onReview)
    var text by rememberSaveable { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var session by remember { mutableStateOf<SpeechCaptureSession?>(null) }
    var languageMissing by remember { mutableStateOf(false) }
    var downloadPrompt by remember { mutableStateOf(false) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }
    var disposed by remember { mutableStateOf(false) }

    fun cancel() {
        session?.cancel(); session = null
        listening = false; processing = false
        recognizer?.cancel(); recognizer?.destroy(); recognizer = null
    }
    fun finish(capture: SpeechCaptureSession, result: String? = null) {
        if (disposed || capture !== session) return
        val completed = capture.finish(result)
        listening = false; processing = false
        if (completed != null) { text = completed; review(completed) }
        else if (error == null) error = "No food heard. Tap the mic to try again."
    }
    fun start() {
        cancel()
        text = ""; error = null
        val capture = SpeechCaptureSession()
        session = capture
        try {
            // Keep acquisition local. A missing on-device model leaves a usable text fallback.
            if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                languageMissing = true; downloadPrompt = true
                error = "On-device speech is unavailable. Enable or download offline speech recognition in Android settings, or type below."
                return
            }
            val speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = speech
            speech.setRecognitionListener(object : RecognitionListener {
                fun current() = !disposed && capture === session && capture.isActive
                override fun onReadyForSpeech(params: Bundle?) { if (current()) listening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { if (current()) { listening = false; processing = true } }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (current()) { capture.partial(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()); text = capture.transcript }
                }
                override fun onResults(results: Bundle?) { if (current()) finish(capture, results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()) }
                override fun onError(code: Int) {
                    if (!current()) return
                    if (capture.transcript.isNotBlank()) finish(capture)
                    else {
                        if (code == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || code == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) { languageMissing = true; downloadPrompt = true }
                        capture.cancel(); listening = false; processing = false
                        error = when (code) {
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone access to speak your food."
                            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Download the offline speech language in Android settings, or type below."
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No food heard. Tap the mic to try again."
                            else -> "Speech could not start. Tap the mic to retry, or type below."
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            listening = true
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        } catch (_: Exception) { cancel(); error = "Speech is unavailable. Tap the mic to retry, or type below." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else error = "Allow microphone access to speak your food, or type below."
    }
    fun requestStart() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    LaunchedEffect(Unit) { if (text.isBlank()) requestStart() }
    LaunchedEffect(processing, session) {
        if (processing) { delay(2500); session?.let { capture -> if (processing) { finish(capture); recognizer?.cancel() } } }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) cancel() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { disposed = true; cancel(); lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun openSpeechSettings() {
        val settings = Intent("android.settings.VOICE_INPUT_SETTINGS")
        runCatching { context.startActivity(settings) }.onFailure { runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } }
    }
    fun downloadLanguage() {
        downloadPrompt = false
        val speech = recognizer
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            if (Build.VERSION.SDK_INT >= 34 && speech != null) {
                downloadStatus = "Starting language download…"
                speech.triggerModelDownload(intent, context.mainExecutor, object : android.speech.ModelDownloadListener {
                    override fun onProgress(progress: Int) { if (!disposed) downloadStatus = "Downloading speech language: $progress%" }
                    override fun onSuccess() { if (!disposed) { downloadStatus = "Language ready. Tap the mic to start."; languageMissing = false; error = null } }
                    override fun onScheduled() { if (!disposed) downloadStatus = "Download queued by Android. Tap the mic when it finishes." }
                    override fun onError(error: Int) { if (!disposed) downloadStatus = "Android could not download this language. Open speech settings to install it." }
                })
            } else if (Build.VERSION.SDK_INT >= 33 && speech != null) {
                speech.triggerModelDownload(intent); downloadStatus = "Language download requested. Tap the mic when it finishes."
            } else openSpeechSettings()
        } catch (_: Exception) { downloadStatus = "Open speech settings to download the offline language." }
    }
    if (downloadPrompt) AlertDialog(onDismissRequest = { downloadPrompt = false },
        title = { Text("Download speech language?") },
        text = { Text("Offline speech for ${locale.displayLanguage} is not installed. Download it to speak your food on this device.") },
        confirmButton = { TextButton(onClick = ::downloadLanguage) { Text("Download") } },
        dismissButton = { TextButton(onClick = { downloadPrompt = false }) { Text("Not now") } })
    val transition = rememberInfiniteTransition(label = "Listening pulse")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Restart), label = "Mic rings")
    val accent = MaterialTheme.colorScheme.primary
    Page {
        PageHeader("Speak your food", onBack = { cancel(); onBack() })
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            if (listening) Canvas(Modifier.size(260.dp)) {
                repeat(3) { index ->
                    val phase = (pulse + index / 3f) % 1f
                    drawCircle(accent.copy(alpha = (1 - phase) * .35f), radius = size.minDimension * (.27f + phase * .22f), style = Stroke(2.dp.toPx()))
                }
            }
            OutlinedButton(onClick = {
                if (listening) { listening = false; processing = true; recognizer?.stopListening() }
                else requestStart()
            }, enabled = !processing, modifier = Modifier.size(132.dp).semantics { contentDescription = if (listening) "Stop and review food" else "Start listening" }, shape = CircleShape) {
                BiteWiseIcon("Speak", modifier = Modifier.size(56.dp))
            }
        }
        Text(if (listening) "Listening…" else if (processing) "Reviewing what you said…" else "Tell us what you ate", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        Text(if (listening) "Tap the mic when you’re done." else "Include food names and amounts. You can edit them in review.", color = AppMuted)
        if (listening || processing) Text(text.ifBlank { "Your words will appear here…" }, Modifier.fillMaxWidth().heightIn(min = 100.dp), style = MaterialTheme.typography.headlineSmall)
        else {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            downloadStatus?.let { Text(it, color = AppMuted) }
            if (languageMissing) Row {
                TextButton(onClick = ::downloadLanguage) { Text("Download language") }
                TextButton(onClick = ::openSpeechSettings) { Text("Speech settings") }
            }
            OutlinedTextField(text, { text = it }, label = { Text("Your food") }, modifier = Modifier.fillMaxWidth())
            AppPrimaryButton("Review foods", { cancel(); review(text) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
        }
        OutlinedButton(onClick = { cancel(); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

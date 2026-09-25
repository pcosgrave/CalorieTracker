package com.philipcosgrave.calorietracker.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Uses only CameraX's in-memory callback: no file, MediaStore entry, or camera-app intent. */
@Composable
internal fun MemoryPhotoCapture(onCaptured: (Bitmap) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val view = remember(context) { PreviewView(context) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val latestCaptured by rememberUpdatedState(onCaptured)
    val latestError by rememberUpdatedState(onError)
    var ready by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val active = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    DisposableEffect(lifecycle, view, capture) {
        active.set(true)
        val future = ProcessCameraProvider.getInstance(context)
        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
        var provider: ProcessCameraProvider? = null
        future.addListener({
            if (active.get()) {
                try {
                    provider = future.get()
                    provider!!.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    ready = true
                } catch (exception: Exception) {
                    latestError("Unable to open the camera. Go back and try again.")
                }
            }
        }, executor)
        onDispose {
            active.set(false)
            ready = false
            provider?.unbind(preview, capture)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().height(380.dp))
        Button(enabled = ready && !capturing, modifier = Modifier.fillMaxWidth(), onClick = {
            capturing = true
            capture.targetRotation = view.display.rotation
            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    var bitmap: Bitmap? = null
                    try {
                        if (!active.get()) return
                        val source = image.toBitmap()
                        bitmap = source
                        val ratio = minOf(1f, 1600f / maxOf(source.width, source.height))
                        val matrix = Matrix().apply {
                            postScale(ratio, ratio)
                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                        }
                        val upright = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                        if (upright !== source) source.recycle()
                        bitmap = upright
                        latestCaptured(upright) // Ownership transfers to analysis; it releases the bitmap in finally.
                        bitmap = null
                    } catch (exception: Exception) {
                        if (active.get()) latestError("Unable to capture this photo. Please try again.")
                    } finally {
                        bitmap?.recycle()
                        image.close()
                        capturing = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    if (active.get()) latestError("Unable to capture this photo. Please try again.")
                }
            })
        }) { Text(if (capturing) "Capturing…" else "Analyze photo") }
    }
}

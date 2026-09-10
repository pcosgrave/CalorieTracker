package com.philipcosgrave.calorietracker.data.local

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The bundled OCR model runs locally, without sending the recipe image to a server. */
suspend fun readRecipeText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    // Own a copy until the asynchronous recognizer releases it, even if the screen is cancelled.
    val owned = bitmap.copy(Bitmap.Config.ARGB_8888, false)
    recognizer.process(InputImage.fromBitmap(owned, 0))
        .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.text) }
        .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        .addOnCompleteListener { recognizer.close(); owned.recycle() }
}

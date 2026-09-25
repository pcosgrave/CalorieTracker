package com.philipcosgrave.calorietracker.data.local

import android.graphics.Bitmap

/** The SDK receives a disposable copy, never the capture retained for subsequent food requests. */
internal suspend fun <T> withInferenceBitmap(source: Bitmap, infer: suspend (Bitmap) -> T): T {
    check(!source.isRecycled) { "The photo is no longer available. Please take another photo." }
    val requestBitmap = checkNotNull(source.copy(Bitmap.Config.ARGB_8888, false)) { "Unable to prepare the photo for analysis." }
    try {
        return infer(requestBitmap)
    } finally {
        if (!requestBitmap.isRecycled) requestBitmap.recycle()
    }
}

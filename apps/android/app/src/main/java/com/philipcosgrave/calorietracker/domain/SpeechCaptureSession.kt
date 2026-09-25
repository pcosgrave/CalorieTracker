package com.philipcosgrave.calorietracker.domain

/** One utterance can produce many partial callbacks, but only one review. */
class SpeechCaptureSession {
    var transcript: String = ""
        private set
    private var active = true
    val isActive: Boolean get() = active
    fun partial(text: String?) { if (active && !text.isNullOrBlank()) transcript = text }
    fun finish(text: String? = null): String? {
        if (!active) return null
        partial(text)
        active = false
        return transcript.trim().takeIf { it.isNotEmpty() }
    }
    fun cancel() { active = false }
}

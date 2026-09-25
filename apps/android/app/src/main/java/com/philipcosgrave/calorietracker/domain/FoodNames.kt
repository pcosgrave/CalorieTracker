package com.philipcosgrave.calorietracker.domain

/** Capitalize word starts without damaging existing brand acronyms or internal capitalization. */
fun foodTitle(value: String): String = Regex("(^|[\\s(\\-])\\p{L}").replace(value.trim()) { it.value.uppercase(java.util.Locale.ROOT) }

package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import java.time.LocalDate

data class Leftover(val id: String, val name: String, val entries: List<DiaryEntry>) {
    // The original meal date is retained in every saved entry, including older leftovers.
    val date: LocalDate get() = entries.first().date
}
data class LeftoverSplit(val leftover: Leftover, val remaining: List<DiaryEntry>)

fun splitLeftover(entries: List<DiaryEntry>, percentage: Double, name: String, id: String = createId("leftover")): LeftoverSplit {
    require(entries.isNotEmpty() && entries.map { it.id }.distinct().size == entries.size) { "Select at least one food." }
    require(entries.map { it.date to it.meal }.distinct().size == 1) { "Select foods from one meal." }
    require(name.isNotBlank() && percentage.isFinite() && percentage > 0 && percentage <= 100) { "Enter a name and percentage greater than 0 and up to 100." }
    require(entries.all { it.loggedAmount.isFinite() && it.loggedAmount > 0 && it.servingMultiplier.isFinite() && it.servingMultiplier > 0 })
    val fraction = percentage / 100.0
    fun portion(entry: DiaryEntry, fraction: Double) = entry.copy(loggedAmount = entry.loggedAmount * fraction, servingMultiplier = entry.servingMultiplier * fraction)
    return LeftoverSplit(Leftover(id, name.trim(), entries.map { portion(it, fraction) }),
        if (percentage == 100.0) emptyList() else entries.map { portion(it, 1 - fraction) })
}

fun leftoverDiaryEntries(leftover: Leftover, date: LocalDate, meal: Meal): List<DiaryEntry> =
    leftover.entries.mapIndexed { index, entry -> entry.copy(id = "${leftover.id}-entry-$index", date = date, meal = meal) }

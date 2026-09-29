package com.philipcosgrave.calorietracker.domain

enum class HouseholdScreenState { Loading, Create, Dashboard }

fun householdScreenState(isLoading: Boolean, hasHousehold: Boolean): HouseholdScreenState =
    when {
        isLoading -> HouseholdScreenState.Loading
        hasHousehold -> HouseholdScreenState.Dashboard
        else -> HouseholdScreenState.Create
    }

package com.philipcosgrave.calorietracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HouseholdFlowTest {
    @Test fun loadingAlwaysWinsUntilTheSharedHouseholdStateIsReady() {
        assertEquals(HouseholdScreenState.Loading, householdScreenState(isLoading = true, hasHousehold = false))
        assertEquals(HouseholdScreenState.Loading, householdScreenState(isLoading = true, hasHousehold = true))
    }

    @Test fun emptySharedStateShowsCreationAndExistingMembershipShowsDashboard() {
        assertEquals(HouseholdScreenState.Create, householdScreenState(isLoading = false, hasHousehold = false))
        assertEquals(HouseholdScreenState.Dashboard, householdScreenState(isLoading = false, hasHousehold = true))
    }
}

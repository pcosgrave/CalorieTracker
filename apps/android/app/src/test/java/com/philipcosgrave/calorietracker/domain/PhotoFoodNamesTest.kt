package com.philipcosgrave.calorietracker.domain

import org.junit.Assert.*
import org.junit.Test

class PhotoFoodNamesTest {
    @Test fun multiFoodDiscoveryRetainsAllDistinctFoods() {
        assertEquals(listOf("Rice", "Chicken", "Broccoli"), parsePhotoFoodNames("""{"foods":["Rice","Chicken","Broccoli","rice"],"tooMany":false}"""))
    }
    @Test fun tooManyFoodsAreNotSilentlyDropped() {
        assertThrows(IllegalArgumentException::class.java) { parsePhotoFoodNames("""{"foods":["Rice"],"tooMany":true}""") }
    }
    @Test fun emptyPhotoReturnsNoFoods() {
        assertTrue(parsePhotoFoodNames("""{"foods":[]}""").isEmpty())
    }
    @Test fun truncatedDiscoveryCannotSavePartialMeal() {
        assertThrows(Exception::class.java) { parsePhotoFoodNames("""{"foods":["Rice","Chicken"""") }
    }
    @org.junit.Test fun containersAreExcludedWithoutDroppingEdibleNames() {
        val names = parsePhotoFoodNames("""{"foods":["mug","white ceramic mug","dinner plate","fork","digital kitchen scale","apple","mug cake","rice paper","coffee","tea"]}""")
        org.junit.Assert.assertEquals(listOf("apple", "mug cake", "rice paper", "coffee", "tea"), names)
        org.junit.Assert.assertTrue(parsePhotoFoodNames("""{"foods":["mug","bowl","napkin"]}""").isEmpty())
    }

}

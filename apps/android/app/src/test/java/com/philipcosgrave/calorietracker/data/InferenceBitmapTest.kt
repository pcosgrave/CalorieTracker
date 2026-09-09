package com.philipcosgrave.calorietracker.data.local

import android.graphics.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class InferenceBitmapTest {
    @Test fun sdkMayRecycleOneRequestWithoutBreakingTheNextFood() = runBlocking {
        val source = mock(Bitmap::class.java)
        val first = mock(Bitmap::class.java)
        val second = mock(Bitmap::class.java)
        `when`(source.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(first, second)
        doAnswer { `when`(first.isRecycled).thenReturn(true); null }.`when`(first).recycle()

        withInferenceBitmap(source) { input ->
            assertSame(first, input)
            assertNotSame(source, input)
            input.recycle() // Simulate SDK cleanup during the food-name discovery request.
        }
        val result = withInferenceBitmap(source) { input ->
            assertSame(second, input)
            assertFalse(input.isRecycled)
            "next food"
        }
        assertEquals("next food", result)
        verify(first, times(1)).recycle()
        verify(second).recycle()
        verify(source, never()).recycle()
    }

    @Test fun failedAndCancelledRequestsReleaseOnlyTheirOwnCopy() = runBlocking {
        for (failure in listOf(IllegalStateException("inference failed"), CancellationException("cancelled"))) {
            val source = mock(Bitmap::class.java)
            val copy = mock(Bitmap::class.java)
            `when`(source.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(copy)
            try {
                withInferenceBitmap(source) { throw failure }
                fail("Expected inference failure")
            } catch (actual: Exception) {
                assertSame(failure, actual)
            }
            verify(copy).recycle()
            verify(source, never()).recycle()
        }
    }
}

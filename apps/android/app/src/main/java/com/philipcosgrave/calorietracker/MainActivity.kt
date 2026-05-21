package com.philipcosgrave.calorietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.philipcosgrave.calorietracker.ui.CalorieTrackerApp
import com.philipcosgrave.calorietracker.ui.theme.CalorieTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieTrackerTheme {
                CalorieTrackerApp()
            }
        }
    }
}

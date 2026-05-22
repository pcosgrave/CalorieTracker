package com.philipcosgrave.calorietracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.philipcosgrave.calorietracker.ui.CalorieTrackerApp
import com.philipcosgrave.calorietracker.ui.theme.CalorieTrackerTheme

class MainActivity : ComponentActivity() {
    private var authCallbackUri by mutableStateOf(intent?.dataString)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authCallbackUri = intent?.dataString
        setContent {
            CalorieTrackerTheme {
                CalorieTrackerApp(
                    authCallbackUri = authCallbackUri,
                    onAuthCallbackConsumed = { authCallbackUri = null },
                    openExternalUri = { uri ->
                        startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)),
                        )
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        authCallbackUri = intent.dataString
    }
}

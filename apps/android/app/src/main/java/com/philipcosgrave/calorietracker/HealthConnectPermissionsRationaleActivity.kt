package com.philipcosgrave.calorietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.philipcosgrave.calorietracker.ui.theme.CalorieTrackerTheme

class HealthConnectPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieTrackerTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Health Connect access", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "CalorieTracker only writes nutrition entries that you log in this app. " +
                                "We use Health Connect so your meals can appear in other connected health apps on your device.",
                        )
                        Text(
                            "The app does not read your other Health Connect data as part of this flow. " +
                                "You can revoke access any time from Health Connect settings.",
                        )
                        Button(onClick = ::finish) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

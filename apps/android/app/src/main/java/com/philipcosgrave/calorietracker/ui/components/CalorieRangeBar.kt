package com.philipcosgrave.calorietracker.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
@Composable
fun IntakeRangeBar(
    currentProgress: Float,
    lowerTarget: Int,
    upperTarget: Int,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        val barWidth = maxWidth
        val firstBreak = maxWidth / 3f
        val secondBreak = firstBreak * 2f
        val markerOffset = ((barWidth - 18.dp) * currentProgress).coerceIn(0.dp, barWidth - 18.dp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(appBorderColor(), RoundedCornerShape(999.dp)),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFFF0D58A), RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFF9BE2AB)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFFFFC4BA), RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)),
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = firstBreak - 1.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .padding(start = secondBreak - 1.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = markerOffset)
                        .size(18.dp)
                        .background(Color(0xFF1684ED), CircleShape),
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "$lowerTarget",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(72.dp)
                        .align(Alignment.CenterStart)
                        .absoluteOffset((firstBreak - 36.dp).coerceAtLeast(0.dp), 0.dp)
                )
                Text(
                    "$upperTarget",
                    color = AppMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(72.dp)
                        .align(Alignment.CenterStart)
                        .absoluteOffset((secondBreak - 36.dp).coerceAtLeast(0.dp), 0.dp)
                )
            }
        }
    }
}

package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmicBg
import com.example.ui.theme.CosmicMutedText
import com.example.ui.theme.CosmicPrimary
import com.example.ui.theme.CosmicSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBg),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic abstract rings behind
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val ringPulse by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ring_pulse"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CosmicSecondary.copy(alpha = 0.08f * ringPulse), Color.Transparent),
                    center = centerOffset,
                    radius = 300.dp.toPx()
                ),
                radius = 300.dp.toPx()
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scaleAnim)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing neon ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = CosmicSecondary,
                        radius = 45.dp.toPx(),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Rotating cybernetic indicator arcs
                    val arcSize = size * 0.9f
                    val topLeftOffset = Offset(
                        (size.width - arcSize.width) / 2f,
                        (size.height - arcSize.height) / 2f
                    )
                    drawArc(
                        color = CosmicPrimary,
                        startAngle = (System.currentTimeMillis() / 8 % 360).toFloat(),
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner Phone Icon
                Icon(
                    imageVector = Icons.Rounded.PhoneInTalk,
                    contentDescription = "WakeCall AI Brand",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "WakeCall AI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Smart Virtual Calling Companion",
                color = CosmicSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Initializing AI Calling Core...",
                color = CosmicMutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier.scale(alphaAnim)
            )
        }
    }
}

package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlarmDatabase
import com.example.data.AlarmHistory
import com.example.data.AlarmRepository
import com.example.receiver.AlarmScheduler
import com.example.service.AlarmService
import com.example.ui.theme.CosmicBg
import com.example.ui.theme.CosmicMutedText
import com.example.ui.theme.CosmicPrimary
import com.example.ui.theme.CosmicSecondary
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.CosmicTertiary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure window to unlock screen, show when locked, and keep screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val label = intent.getStringExtra("ALARM_LABEL") ?: "WakeUp AI Call"
        val isVibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)
        val isSnooze = intent.getBooleanExtra("IS_SNOOZE", false)

        // Read preferences
        val sharedPrefs = getSharedPreferences("wakecall_prefs", Context.MODE_PRIVATE)
        val voice = sharedPrefs.getString("voice_personality", "Serene AI Assistant") ?: "Serene AI Assistant"
        val greeting = sharedPrefs.getString("custom_greeting", "Rise and shine! Today is a fresh canvas.") ?: "Rise and shine! Today is a fresh canvas."
        val snoozeSeconds = 300 // Fixed at 5 minutes as per premium requirements

        // Log triggered entry
        logAlarmHistory(alarmId, label, "TRIGGERED")

        setContent {
            MyApplicationTheme {
                AlarmRingScreen(
                    alarmId = alarmId,
                    label = label,
                    isVibrate = isVibrate,
                    voicePersonality = voice,
                    customGreeting = greeting,
                    snoozeSeconds = snoozeSeconds,
                    isSnoozedAlready = isSnooze,
                    onDismiss = {
                        logAlarmHistory(alarmId, label, "DISMISSED")
                        stopAlarmService()
                        finish()
                    },
                    onSnooze = { isAuto ->
                        logAlarmHistory(alarmId, label, if (isAuto) "AUTO_SNOOZED" else "SNOOZED")
                        snoozeAlarm(alarmId, label, isVibrate, snoozeSeconds)
                        stopAlarmService()
                        finish()
                    }
                )
            }
        }
    }

    private fun logAlarmHistory(alarmId: Int, label: String, action: String) {
        val database = AlarmDatabase.getDatabase(this)
        val repository = AlarmRepository(database.alarmDao())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.insertHistory(
                    AlarmHistory(
                        alarmId = alarmId,
                        alarmLabel = label,
                        alarmTime = timeStr,
                        action = action
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAlarmService() {
        val serviceIntent = Intent(this, AlarmService::class.java)
        stopService(serviceIntent)
    }

    private fun snoozeAlarm(alarmId: Int, label: String, isVibrate: Boolean, snoozeSeconds: Int) {
        val scheduler = AlarmScheduler(this)
        scheduler.scheduleSnooze(alarmId, label, isVibrate, snoozeSeconds)
    }
}

@Composable
fun AlarmRingScreen(
    alarmId: Int,
    label: String,
    isVibrate: Boolean,
    voicePersonality: String,
    customGreeting: String,
    snoozeSeconds: Int,
    isSnoozedAlready: Boolean,
    onDismiss: () -> Unit,
    onSnooze: (Boolean) -> Unit
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var autoSnoozeTimer by remember { mutableStateOf(60) } // Auto snooze in 60s

    LaunchedEffect(Unit) {
        while (true) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    // Auto snooze decrement logic
    LaunchedEffect(Unit) {
        while (autoSnoozeTimer > 0) {
            delay(1000)
            autoSnoozeTimer--
        }
        onSnooze(true) // Trigger auto snooze on timeout
    }

    // Phase shift for voice waves
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CosmicBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Draw glowing cybernetic grid elements in the background
                    val nodes = listOf(
                        Offset(size.width * 0.1f, size.height * 0.15f),
                        Offset(size.width * 0.9f, size.height * 0.25f),
                        Offset(size.width * 0.5f, size.height * 0.85f)
                    )
                    nodes.forEach { node ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CosmicSecondary.copy(alpha = 0.1f), Color.Transparent),
                                center = node,
                                radius = 250.dp.toPx()
                            ),
                            radius = 250.dp.toPx(),
                            center = node
                        )
                    }
                }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header (AI Status Indicators)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(CosmicSecondary.copy(alpha = 0.1f), RoundedCornerShape(100))
                            .border(1.dp, CosmicSecondary.copy(alpha = 0.3f), RoundedCornerShape(100))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CosmicSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INCOMING WAKECALL AI",
                            color = CosmicSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = voicePersonality,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Voice Assistant on Duty",
                        color = CosmicMutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Interactive Audio / Waves Visualizer Card
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(CosmicSurface.copy(alpha = 0.6f))
                        .border(2.dp, CosmicSurfaceVariant, CircleShape)
                ) {
                    // Sine-wave rendering to simulate active AI assistant voice speech
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val midY = height / 2

                        // Draw three overlapping sine waves with different frequencies and offsets
                        val waves = listOf(
                            Triple(0.04f, 25.dp.toPx(), CosmicSecondary.copy(alpha = 0.8f)),
                            Triple(0.025f, 40.dp.toPx(), CosmicPrimary.copy(alpha = 0.6f)),
                            Triple(0.06f, 15.dp.toPx(), CosmicTertiary.copy(alpha = 0.5f))
                        )

                        waves.forEach { (frequency, amplitude, color) ->
                            val path = Path()
                            path.moveTo(0f, midY)

                            for (x in 0..width.toInt() step 5) {
                                val angle = x * frequency + phaseShift
                                val y = midY + sin(angle) * amplitude
                                path.lineTo(x.toFloat(), y)
                            }

                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Floating microphone indicator in the center
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CosmicBg)
                            .border(1.dp, CosmicSecondary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "AI Voice Mic",
                            tint = CosmicSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Real-time AI Speech Transcription Bubble (Gives a sense of "Living AI")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "LIVE TRANSCRIPTION",
                        color = CosmicMutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicSurface)
                            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneInTalk,
                                contentDescription = "Active call",
                                tint = CosmicSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"$customGreeting\"",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Call Controls: HOLD-PADS
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 36.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(100))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HOLD EITHER BUTTON FOR 1.5s TO OPERATE",
                            color = CosmicMutedText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hold to Snooze (Decline Button Style)
                        HoldButtonPad(
                            text = if (isSnoozedAlready) "Snooze Maxed" else "Snooze 5 Min",
                            color = if (isSnoozedAlready) CosmicMutedText else Color(0xFFEF4444),
                            icon = Icons.Rounded.Snooze,
                            onConfirmed = { onSnooze(false) },
                            enabled = !isSnoozedAlready,
                            modifier = Modifier.testTag("hold_snooze_button")
                        )

                        // Hold to Answer / Dismiss Alarm
                        HoldButtonPad(
                            text = "Answer & Wake",
                            color = CosmicSecondary,
                            icon = Icons.Rounded.Done,
                            onConfirmed = onDismiss,
                            enabled = true,
                            modifier = Modifier.testTag("hold_wakeup_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Auto-snoozing in ${autoSnoozeTimer}s",
                        color = CosmicMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun HoldButtonPad(
    text: String,
    color: Color,
    icon: ImageVector,
    onConfirmed: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0f) }
    var isPressing by remember { mutableStateOf(false) }

    LaunchedEffect(isPressing, enabled) {
        if (!enabled) {
            progress = 0f
            isPressing = false
            return@LaunchedEffect
        }
        if (isPressing) {
            val startTime = System.currentTimeMillis()
            val holdDuration = 1500f // 1.5 seconds hold
            while (progress < 1f && isPressing) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed / holdDuration).coerceAtMost(1f)
                if (progress >= 1f) {
                    onConfirmed()
                    break
                }
                delay(16)
            }
        } else {
            while (progress > 0f && !isPressing) {
                progress = (progress - 0.2f).coerceAtLeast(0f)
                delay(16)
            }
        }
    }

    Box(
        modifier = modifier
            .size(124.dp)
            .clip(CircleShape)
            .background(if (enabled) CosmicSurface else CosmicSurface.copy(alpha = 0.3f))
            .border(2.dp, if (enabled) CosmicSurfaceVariant else CosmicSurfaceVariant.copy(alpha = 0.2f), CircleShape)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressing = true
                                tryAwaitRelease()
                                isPressing = false
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Continuous circular progress arc
        if (enabled) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 4.dp,
                trackColor = Color.Transparent
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (!enabled) CosmicMutedText else if (isPressing) color else Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isPressing) "HOLD..." else text,
                color = if (!enabled) CosmicMutedText else if (isPressing) color else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

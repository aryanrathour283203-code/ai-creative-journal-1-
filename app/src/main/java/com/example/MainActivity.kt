package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Assistant
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Alarm
import com.example.data.AlarmHistory
import com.example.ui.AlarmViewModel
import com.example.ui.theme.CosmicBg
import com.example.ui.theme.CosmicMutedText
import com.example.ui.theme.CosmicPrimary
import com.example.ui.theme.CosmicSecondary
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.CosmicTertiary
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AlarmViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDark) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    com.example.ui.SplashScreen(onTimeout = { showSplash = false })
                } else {
                    MainAlarmScreen(viewModel = viewModel)
                }
            }
        }
    }
}

enum class AppTab {
    HOME,
    ALARMS,
    HISTORY,
    SETTINGS
}

@Composable
fun MainAlarmScreen(viewModel: AlarmViewModel = viewModel()) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsState()
    val history by viewModel.history.collectAsState()
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var showAddAlarmScreen by remember { mutableStateOf(false) }

    val snoozeDuration by viewModel.snoozeDuration.collectAsState()
    val voicePersonality by viewModel.voicePersonality.collectAsState()
    val customGreeting by viewModel.customGreeting.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val customRingtone by viewModel.customRingtone.collectAsState()
    val vibrationPattern by viewModel.vibrationPattern.collectAsState()
    val gradualVolume by viewModel.gradualVolume.collectAsState()

    // Notification Permission check (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = CosmicSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.HOME && !showAddAlarmScreen,
                    onClick = {
                        currentTab = AppTab.HOME
                        showAddAlarmScreen = false
                    },
                    icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicBg,
                        selectedTextColor = CosmicSecondary,
                        indicatorColor = CosmicSecondary,
                        unselectedIconColor = CosmicMutedText,
                        unselectedTextColor = CosmicMutedText
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.ALARMS || showAddAlarmScreen,
                    onClick = {
                        currentTab = AppTab.ALARMS
                    },
                    icon = { Icon(Icons.Rounded.Alarm, contentDescription = "Alarms") },
                    label = { Text("Alarms", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicBg,
                        selectedTextColor = CosmicSecondary,
                        indicatorColor = CosmicSecondary,
                        unselectedIconColor = CosmicMutedText,
                        unselectedTextColor = CosmicMutedText
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY && !showAddAlarmScreen,
                    onClick = {
                        currentTab = AppTab.HISTORY
                        showAddAlarmScreen = false
                    },
                    icon = { Icon(Icons.Rounded.History, contentDescription = "History") },
                    label = { Text("History", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicBg,
                        selectedTextColor = CosmicSecondary,
                        indicatorColor = CosmicSecondary,
                        unselectedIconColor = CosmicMutedText,
                        unselectedTextColor = CosmicMutedText
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS && !showAddAlarmScreen,
                    onClick = {
                        currentTab = AppTab.SETTINGS
                        showAddAlarmScreen = false
                    },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicBg,
                        selectedTextColor = CosmicSecondary,
                        indicatorColor = CosmicSecondary,
                        unselectedIconColor = CosmicMutedText,
                        unselectedTextColor = CosmicMutedText
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab == AppTab.ALARMS && !showAddAlarmScreen) {
                FloatingActionButton(
                    onClick = { showAddAlarmScreen = true },
                    containerColor = CosmicSecondary,
                    contentColor = CosmicBg,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("add_alarm_fab")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Create WakeCall",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBg)
                .padding(innerPadding)
        ) {
            // Draw visual decorative cyber background nodes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val nodes = listOf(
                            Offset(size.width * 0.15f, size.height * 0.2f),
                            Offset(size.width * 0.85f, size.height * 0.45f),
                            Offset(size.width * 0.35f, size.height * 0.8f)
                        )
                        nodes.forEach { offset ->
                            drawCircle(
                                color = CosmicPrimary.copy(alpha = 0.05f),
                                radius = 180.dp.toPx(),
                                center = offset
                            )
                        }
                    }
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (showAddAlarmScreen) {
                    AddAlarmScreen(
                        onClose = { showAddAlarmScreen = false },
                        onSave = { hour, minute, label, isVibrate, daysOfWeek ->
                            viewModel.addAlarm(hour, minute, label, isVibrate, daysOfWeek)
                            showAddAlarmScreen = false
                        }
                    )
                } else {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(
                            alarms = alarms,
                            history = history,
                            voicePersonality = voicePersonality,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestPermission = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                        AppTab.ALARMS -> AlarmListScreen(
                            alarms = alarms,
                            viewModel = viewModel
                        )
                        AppTab.HISTORY -> HistoryScreen(
                            history = history,
                            onClear = { viewModel.clearHistory() }
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            snoozeDuration = snoozeDuration,
                            voicePersonality = voicePersonality,
                            customGreeting = customGreeting,
                            themeMode = themeMode,
                            customRingtone = customRingtone,
                            vibrationPattern = vibrationPattern,
                            gradualVolume = gradualVolume,
                            onUpdateSnooze = { viewModel.updateSnoozeDuration(it) },
                            onUpdateVoice = { viewModel.updateVoicePersonality(it) },
                            onUpdateGreeting = { viewModel.updateCustomGreeting(it) },
                            onUpdateTheme = { viewModel.updateThemeMode(it) },
                            onUpdateRingtone = { viewModel.updateCustomRingtone(it) },
                            onUpdateVibration = { viewModel.updateVibrationPattern(it) },
                            onUpdateGradualVolume = { viewModel.updateGradualVolume(it) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. HOME SCREEN COMPOSABLE
// ==========================================
@Composable
fun HomeScreen(
    alarms: List<Alarm>,
    history: List<AlarmHistory>,
    voicePersonality: String,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val scrollState = rememberScrollState()

    // 1. Live Running Clock & Date States
    var liveTime by remember { mutableStateOf("") }
    var liveDate by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        while (true) {
            val now = Calendar.getInstance().time
            liveTime = timeFormat.format(now)
            liveDate = dateFormat.format(now)
            delay(1000)
        }
    }

    // Determine the next alarm time
    val nextAlarmText = remember(alarms) { calculateNextAlarmText(alarms) }

    // Dynamic daily wake-up quotes
    val dailyQuote = remember {
        val quotes = listOf(
            "“Your future depends on what you do today. Rise, shine, and conquer!”",
            "“Each morning we are born again. What we do today is what matters most.”",
            "“The secret of getting ahead is getting started. Let's make it count!”",
            "“Do not shorten morning by getting up late. Life is a beautiful canvas!”",
            "“You don't have to be great to start, but you have to start to be great.”"
        )
        val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        quotes[day % quotes.size]
    }

    // Calculate quick stats for dashboard overview
    val streak = remember(history) {
        val dismissedDates = history
            .filter { it.action == "DISMISSED" }
            .map {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(java.util.Date(it.timestamp))
            }
            .toSet()

        var currentStreak = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        val todayStr = sdf.format(cal.time)
        if (dismissedDates.contains(todayStr)) {
            currentStreak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            while (dismissedDates.contains(sdf.format(cal.time))) {
                currentStreak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)
            if (dismissedDates.contains(yesterdayStr)) {
                currentStreak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
                while (dismissedDates.contains(sdf.format(cal.time))) {
                    currentStreak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
        }
        currentStreak
    }

    val dismissedCount = history.count { it.action == "DISMISSED" }
    val autoSnoozedCount = history.count { it.action == "AUTO_SNOOZED" }
    val totalWakeups = dismissedCount + autoSnoozedCount
    val successRate = if (totalWakeups > 0) (dismissedCount * 100) / totalWakeups else 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        // App title with glowing indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WakeCall AI",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Your smart virtual calling companion",
                    fontSize = 13.sp,
                    color = CosmicMutedText
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CosmicSecondary.copy(alpha = 0.15f))
                    .border(1.dp, CosmicSecondary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Assistant,
                    contentDescription = "AI Active",
                    tint = CosmicSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 1. Live Running Clock & Date Glassmorphic Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LIVE DASHBOARD TIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicSecondary,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = liveTime.ifBlank { "Initializing..." },
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = liveDate.ifBlank { "Gathering clock cycles..." },
                    fontSize = 13.sp,
                    color = CosmicMutedText,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Permission Alert
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable { onRequestPermission() },
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = "Alert", tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Feed Offline",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to grant permission so WakeCall AI can place incoming calls.",
                            color = CosmicMutedText,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. Next Scheduled Alarm & Weather Widgets side-by-side Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Next scheduled Alarm Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Active Status",
                            tint = CosmicSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEXT ALARM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = nextAlarmText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    )

                    Text(
                        text = "Caller: ${voicePersonality.split(" ").firstOrNull() ?: "AI"}",
                        fontSize = 10.sp,
                        color = CosmicMutedText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Weather widget Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WEATHER TODAY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTertiary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "☀️",
                            fontSize = 14.sp
                        )
                    }

                    Column {
                        Text(
                            text = "72°F",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Sunny & Clear",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTertiary
                        )
                    }

                    Text(
                        text = "Humidity 45% • Wind 5mph",
                        fontSize = 10.sp,
                        color = CosmicMutedText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Compact Stats Highlights Row (Streak and Success)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("STREAK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CosmicSecondary)
                        Text("$streak Days Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CosmicSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📈", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("SUCCESS RATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CosmicTertiary)
                        Text("$successRate% Punctual", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Beautiful AI Morning Motivation Quote Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = CosmicSecondary.copy(alpha = 0.1f),
                            radius = size.width / 2
                        )
                        drawCircle(
                            color = CosmicSecondary.copy(alpha = 0.2f),
                            radius = size.width / 2.8f,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = CosmicSecondary,
                            radius = 12.dp.toPx()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MORNING MOTIVATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dailyQuote,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Recent History Logs Widget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent History Logs",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassEmpty,
                        contentDescription = "No Logs",
                        tint = CosmicSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No wake call events recorded.",
                        color = CosmicMutedText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                history.take(3).forEach { log ->
                    MiniHistoryItem(log)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MiniHistoryItem(log: AlarmHistory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicSurface)
            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(log.actionColorHex))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = log.alarmLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = log.formattedDate,
                    color = CosmicMutedText,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = log.actionText,
            color = Color(log.actionColorHex),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Helper to calculate next active alarm
fun calculateNextAlarmText(alarms: List<Alarm>): String {
    val active = alarms.filter { it.isEnabled }
    if (active.isEmpty()) return "No Active WakeCalls scheduled"

    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)

    val sorted = active.sortedWith(compareBy({ it.hour }, { it.minute }))
    val nextToday = sorted.firstOrNull { it.hour > currentHour || (it.hour == currentHour && it.minute > currentMinute) }
    val next = nextToday ?: sorted.first()

    val isTomorrow = nextToday == null
    val labelStr = if (next.label.isNotBlank()) " [${next.label}]" else ""
    return "${if (isTomorrow) "Tomorrow" else "Today"} at ${next.formattedTime}$labelStr"
}


// ==========================================
// 2. ALARM LIST SCREEN COMPOSABLE
// ==========================================
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    viewModel: AlarmViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "WakeCalls Schedulers",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(top = 36.dp, bottom = 4.dp)
        )
        Text(
            text = "Set triggers for automated virtual companion calling.",
            fontSize = 13.sp,
            color = CosmicMutedText,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AlarmOff,
                        contentDescription = "No Alarms",
                        tint = CosmicSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your schedulers are empty",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the '+' floating button below to configure your very first virtual assistant caller.",
                        color = CosmicMutedText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCardItem(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmCardItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (alarm.isEnabled) CosmicSecondary.copy(alpha = 0.3f) else CosmicSurfaceVariant,
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Time
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = alarm.formattedTime,
                        color = if (alarm.isEnabled) Color.White else CosmicMutedText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Label and repeating details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alarm.label,
                        color = if (alarm.isEnabled) CosmicSecondary else CosmicMutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(CosmicMutedText)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alarm.repeatingDaysText,
                        color = CosmicMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (alarm.isVibrate) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = "Vibration Enabled",
                            tint = CosmicMutedText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dual-Pulse Vibe Enabled",
                            color = CosmicMutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Switch toggle
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CosmicBg,
                        checkedTrackColor = CosmicSecondary,
                        uncheckedThumbColor = CosmicMutedText,
                        uncheckedTrackColor = CosmicSurfaceVariant
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Alarm",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


// ==========================================
// 3. ADD ALARM SCREEN COMPOSABLE
// ==========================================
@Composable
fun AddAlarmScreen(
    onClose: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, isVibrate: Boolean, daysOfWeek: String) -> Unit
) {
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(30) }
    var label by remember { mutableStateOf("") }
    var isVibrate by remember { mutableStateOf(true) }

    // Repetitive days tracking: list of toggled day indices (1 to 7)
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup WakeCall",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Button(
                onClick = {
                    val daysString = selectedDays.sorted().joinToString(",")
                    onSave(hour, minute, label, isVibrate, daysString)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_alarm_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = CosmicBg)
            }
        }

        Text(
            text = "Configure exact triggers for custom calls.",
            color = CosmicMutedText,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Custom Time Value Pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeValueSelector(
                value = hour,
                range = 0..23,
                label = "HOUR (24h format)",
                onValueChange = { hour = it }
            )

            Text(
                text = ":",
                color = CosmicSecondary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )

            TimeValueSelector(
                value = minute,
                range = 0..59,
                label = "MINUTE",
                onValueChange = { minute = it }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Custom Label Text Field
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Call Identifier / Label", color = CosmicMutedText) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmicSecondary,
                unfocusedBorderColor = CosmicSurfaceVariant,
                focusedLabelColor = CosmicSecondary,
                unfocusedLabelColor = CosmicMutedText,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Repeat selector chips
        Text(
            text = "Active Calling Days",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayNames.forEachIndexed { index, name ->
                val dayIndex = index + 1
                val isSelected = selectedDays.contains(dayIndex)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) CosmicSecondary else CosmicSurface)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else CosmicSurfaceVariant,
                            CircleShape
                        )
                        .clickable {
                            selectedDays = if (isSelected) {
                                selectedDays - dayIndex
                            } else {
                                selectedDays + dayIndex
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) CosmicBg else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Vibration setting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicSurface)
                .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = "Vibration",
                    tint = CosmicSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Rumble & Vibrate",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Continuously pulse dual vibration motor",
                        color = CosmicMutedText,
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = isVibrate,
                onCheckedChange = { isVibrate = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CosmicBg,
                    checkedTrackColor = CosmicSecondary,
                    uncheckedThumbColor = CosmicMutedText,
                    uncheckedTrackColor = CosmicSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cancel / Back Button
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Cancel Setup", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
fun TimeValueSelector(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(CosmicSurface, RoundedCornerShape(14.dp))
            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp)
    ) {
        Text(text = label, color = CosmicMutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newVal = if (value > range.first) value - 1 else range.last
                    onValueChange(newVal)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text("-", color = CosmicSecondary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = String.format("%02d", value),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = {
                    val newVal = if (value < range.last) value + 1 else range.first
                    onValueChange(newVal)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = CosmicSecondary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}


// ==========================================
// 4. HISTORY SCREEN COMPOSABLE
// ==========================================
@Composable
fun HistoryScreen(
    history: List<AlarmHistory>,
    onClear: () -> Unit
) {
    // 1. Calculate stats
    val streak = remember(history) {
        val dismissedDates = history
            .filter { it.action == "DISMISSED" }
            .map {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(it.timestamp))
            }
            .toSet()

        var currentStreak = 0
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()

        val todayStr = sdf.format(cal.time)
        if (dismissedDates.contains(todayStr)) {
            currentStreak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            while (dismissedDates.contains(sdf.format(cal.time))) {
                currentStreak++
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
        } else {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)
            if (dismissedDates.contains(yesterdayStr)) {
                currentStreak++
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                while (dismissedDates.contains(sdf.format(cal.time))) {
                    currentStreak++
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                }
            }
        }
        currentStreak
    }

    val dismissedCount = history.count { it.action == "DISMISSED" }
    val autoSnoozedCount = history.count { it.action == "AUTO_SNOOZED" }
    val totalWakeups = dismissedCount + autoSnoozedCount
    val successRate = if (totalWakeups > 0) (dismissedCount * 100) / totalWakeups else 100

    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    val weeklyHistory = history.filter { it.timestamp >= sevenDaysAgo }
    val weeklyDismissed = weeklyHistory.count { it.action == "DISMISSED" }
    val weeklyAutoSnooze = weeklyHistory.count { it.action == "AUTO_SNOOZED" }
    val weeklyTotal = weeklyDismissed + weeklyAutoSnooze
    val weeklySuccessRate = if (weeklyTotal > 0) (weeklyDismissed * 100) / weeklyTotal else 100

    val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
    val monthlyHistory = history.filter { it.timestamp >= thirtyDaysAgo }
    val monthlyDismissed = monthlyHistory.count { it.action == "DISMISSED" }
    val monthlyAutoSnooze = monthlyHistory.count { it.action == "AUTO_SNOOZED" }
    val monthlyTotal = monthlyDismissed + monthlyAutoSnooze
    val monthlySuccessRate = if (monthlyTotal > 0) (monthlyDismissed * 100) / monthlyTotal else 100

    val last7DaysData = remember(history) {
        val sdfDay = java.text.SimpleDateFormat("E", java.util.Locale.getDefault())
        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val dataList = mutableListOf<Pair<String, Int>>()
        
        for (i in 0..6) {
            val dateStr = sdfDate.format(cal.time)
            val dayName = sdfDay.format(cal.time)
            val count = history.count { it.action == "DISMISSED" && sdfDate.format(java.util.Date(it.timestamp)) == dateStr }
            dataList.add(Pair(dayName, count))
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        dataList.reverse()
        dataList
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wake Analytics",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Clear logs",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = "Track your streak, response success metrics, and daily trends.",
                fontSize = 13.sp,
                color = CosmicMutedText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // 2. Statistics Grid Block
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Streak Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔥", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("STREAK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmicSecondary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$streak Days", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Consecutive days", fontSize = 11.sp, color = CosmicMutedText)
                        }
                    }

                    // Success Rate Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📈", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SUCCESS RATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmicTertiary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$successRate%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Total punctuality", fontSize = 11.sp, color = CosmicMutedText)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Weekly Success Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📅", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WEEKLY RATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmicPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$weeklySuccessRate%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Past 7 days", fontSize = 11.sp, color = CosmicMutedText)
                        }
                    }

                    // Monthly Success Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🌌", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MONTHLY RATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$monthlySuccessRate%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Past 30 days", fontSize = 11.sp, color = CosmicMutedText)
                        }
                    }
                }
            }
        }

        // 3. Canvas Weekly Wake-ups Bar Chart Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WEEKLY WAKE-UP ACTIVITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val maxVal = remember(last7DaysData) {
                        val maxCount = last7DaysData.maxOf { it.second }
                        if (maxCount == 0) 5 else maxCount
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        val barCount = last7DaysData.size
                        val spacing = 16.dp.toPx()
                        val totalSpacing = spacing * (barCount - 1)
                        val barWidth = (size.width - totalSpacing) / barCount

                        last7DaysData.forEachIndexed { idx, pair ->
                            val value = pair.second
                            val barHeightPercent = value.toFloat() / maxVal
                            val barHeight = (size.height - 20.dp.toPx()) * barHeightPercent
                            
                            val startX = idx * (barWidth + spacing)
                            val startY = size.height - 20.dp.toPx() - barHeight

                            // Draw gradient bar
                            drawRoundRect(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(CosmicSecondary, CosmicPrimary)
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(startX, startY),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(6.dp.toPx())),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Labels below bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        last7DaysData.forEach { pair ->
                            Text(
                                text = pair.first,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicMutedText,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Header for Logs
        item {
            Text(
                text = "Wake Event History Logs",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "History audit is currently empty.",
                        color = CosmicMutedText,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(history) { log ->
                HistoryItemCard(log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HistoryItemCard(log: AlarmHistory) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(log.actionColorHex).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (log.action) {
                            "DISMISSED" -> Icons.Rounded.CheckCircle
                            "SNOOZED" -> Icons.Rounded.Snooze
                            "AUTO_SNOOZED" -> Icons.Rounded.Info
                            else -> Icons.Rounded.NotificationsActive
                        },
                        contentDescription = log.actionText,
                        tint = Color(log.actionColorHex),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = log.alarmLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = log.formattedDate,
                        color = CosmicMutedText,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = log.actionText,
                color = Color(log.actionColorHex),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ==========================================
// 5. SETTINGS SCREEN COMPOSABLE
// ==========================================
@Composable
fun SettingsScreen(
    snoozeDuration: Int,
    voicePersonality: String,
    customGreeting: String,
    themeMode: String,
    customRingtone: String,
    vibrationPattern: String,
    gradualVolume: Boolean,
    onUpdateSnooze: (Int) -> Unit,
    onUpdateVoice: (String) -> Unit,
    onUpdateGreeting: (String) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateRingtone: (String) -> Unit,
    onUpdateVibration: (String) -> Unit,
    onUpdateGradualVolume: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "WakeCall Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(top = 36.dp, bottom = 4.dp)
        )
        Text(
            text = "Customize calling parameters, themes, and sound architectures.",
            fontSize = 13.sp,
            color = CosmicMutedText,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. App Theme Configuration
        Text(
            text = "Application Visual Theme",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themeOptions = listOf(
                Pair("light", "Light Mode"),
                Pair("dark", "Dark Mode"),
                Pair("system", "System")
            )

            themeOptions.forEach { (mode, label) ->
                val isSelected = themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CosmicSecondary else CosmicSurface)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else CosmicSurfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onUpdateTheme(mode) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) CosmicBg else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Custom Ringtone Configuration
        Text(
            text = "Calling Audio Ringtone",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val ringtones = listOf("Classic Alarm", "Gentle Melody", "Simple Alert")
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ringtones.forEach { name ->
                val isSelected = customRingtone == name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CosmicTertiary.copy(alpha = 0.1f) else CosmicSurface)
                        .border(
                            1.dp,
                            if (isSelected) CosmicTertiary.copy(alpha = 0.5f) else CosmicSurfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onUpdateRingtone(name) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CosmicTertiary else CosmicMutedText)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            color = if (isSelected) CosmicTertiary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (isSelected) {
                        Text(
                            text = "ACTIVE",
                            color = CosmicTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Vibration Pattern Configuration
        Text(
            text = "Haptic Vibration Pattern",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val vibrations = listOf("Continuous", "Heartbeat", "Staccato", "Zen Wave")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vibrations.forEach { pattern ->
                val isSelected = vibrationPattern == pattern
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CosmicSecondary else CosmicSurface)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else CosmicSurfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onUpdateVibration(pattern) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pattern,
                        color = if (isSelected) CosmicBg else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Gradual Volume Switch Configuration
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gradual Volume Increase",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Escalate volume slowly from 5% to 100% over 30s to protect your ears.",
                        color = CosmicMutedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                androidx.compose.material3.Switch(
                    checked = gradualVolume,
                    onCheckedChange = { onUpdateGradualVolume(it) },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = CosmicSecondary,
                        checkedTrackColor = CosmicSecondary.copy(alpha = 0.4f),
                        uncheckedThumbColor = CosmicMutedText,
                        uncheckedTrackColor = CosmicSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Snooze Duration Configuration
        Text(
            text = "Default Snooze Delay",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf(
                Pair(15, "15s"),
                Pair(30, "30s"),
                Pair(60, "1m"),
                Pair(300, "5m")
            )

            options.forEach { (sec, label) ->
                val isSelected = snoozeDuration == sec
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CosmicSecondary else CosmicSurface)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else CosmicSurfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onUpdateSnooze(sec) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) CosmicBg else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. AI Companion Personality selection
        Text(
            text = "AI Voice Personality",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val personalities = listOf(
            "Serene AI Assistant",
            "Energetic Drill Sergeant",
            "Sassy Cyborg Oracle"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            personalities.forEach { name ->
                val isSelected = voicePersonality == name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CosmicSecondary.copy(alpha = 0.1f) else CosmicSurface)
                        .border(
                            1.dp,
                            if (isSelected) CosmicSecondary.copy(alpha = 0.5f) else CosmicSurfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onUpdateVoice(name) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CosmicSecondary else CosmicMutedText)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            color = if (isSelected) CosmicSecondary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (isSelected) {
                        Text(
                            text = "ASSIGNED",
                            color = CosmicSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. Custom speech message
        Text(
            text = "Transcribed Wakeup Speech",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = customGreeting,
            onValueChange = { onUpdateGreeting(it) },
            label = { Text("What should the caller transcribe?", color = CosmicMutedText) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmicSecondary,
                unfocusedBorderColor = CosmicSurfaceVariant,
                focusedLabelColor = CosmicSecondary,
                unfocusedLabelColor = CosmicMutedText,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 8. Overlap permission checklist and link
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicSurfaceVariant, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Tips",
                        tint = CosmicSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CRITICAL PERMISSIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To let WakeCall AI display the full screen incoming call overlay even when your screen is locked, you must configure 'Display over other apps' access.",
                    fontSize = 11.sp,
                    color = CosmicMutedText,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Configure Overlay Access",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicBg
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

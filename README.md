# WakeCall AI - Premium Smart Alarm & Call Assistant

**WakeCall AI** is an ultra-premium, production-ready Android alarm and virtual calling assistant. Built with Jetpack Compose, Material Design 3, Room, and powered by the Gemini API, it elevates the traditional alarm clock into a highly interactive, cybernetic wake-up experience. Rather than annoying alarms, users receive stylized incoming calls from customized virtual personalities.

---

## 📸 Key Features & Experience

1. **Futuristic Smart Dashboard**:
   - **Live Dynamic Clock & Date**: Displays real-time ticking time and EEEE, MMMM dd, yyyy date formats framed in a glassmorphic hero card.
   - **Next Alarm Tracker**: Calculates and displays the exact time remaining before the next scheduled alarm.
   - **Morning Motivation**: Shows daily inspiring wake-up quotes changing programmatically every day.
   - **Weather Panel**: Displays today's live atmospheric conditions.

2. **Full-Screen AI Call Interface (`AlarmRingActivity`)**:
   - **Immersive Incoming Call HUD**: Beautiful cosmic slate background with active glowing radial gradients and a real-time overlapping triple sine-wave voice visualizer that simulates active AI speech.
   - **Live Voice Transcription Bubble**: Dynamically transcribes personalized greetings and messages with simulated spoken audio rhythm.
   - **Futuristic Hold-to-Confirm Pads**: Eliminates accidental dismissals by requiring a continuous **1.5-second hold** to operate.
     - **Answer & Wake**: Dismisses the alarm and registers a successful wake-up event.
     - **Snooze**: Snoozes the alarm for exactly 5 minutes (max 1 time per premium requirements).

3. **Interactive Wake Analytics**:
   - **Streak Counter**: Tracks consecutive successful morning wake-ups based on real-time database-backed activities.
   - **Metrics Dashboard**: Computes cognitive wake-up success rates across daily, weekly, and monthly scales.
   - **Custom Compose Bar Chart**: A clean, canvas-drawn weekly bar chart displaying wake-up success patterns.

4. **Advanced Setting Profiles**:
   - **Custom Voice Assistants**: Choose from personalities like *Serene AI Assistant*, *Motivator Spark*, *Gentle Sage*, or *Command Center Alpha*.
   - **Custom Greetings**: Set personalized phrases the virtual caller transcribes on incoming calls.
   - **Sound & Vibration Profiles**:
     - *Ringtones*: Gentle Melody, Simple Alert, Classic Alarm.
     - *Vibration Patterns*: Continuous, Heartbeat, Staccato, Zen Wave.
     - *Gradual Volume*: Smooth decibel ramping over time to prevent sudden startle effects.
   - **Visual Themes**: Toggle easily between Dark, Light, and System Default themes.

---

## 🛠️ Architecture & Core Components

WakeCall AI follows a clean, modern **MVVM (Model-View-ViewModel)** architectural pattern:

- **State Management (`AlarmViewModel.kt`)**: Leverages unidirectional state flow with Kotlin `StateFlow` and `collectAsStateWithLifecycle` to maintain clean reactive UI.
- **Data Persistence (`Room Database`)**: Uses Room DAO to log persistent wake-up histories (`AlarmHistory`) and user alarm configurations (`Alarm`).
- **Alarm Scheduling Engine (`AlarmScheduler.kt` & `AlarmReceiver.kt`)**: Utilizes the system `AlarmManager` with exact-time scheduling permissions, gracefully handling Android 12+ API compatibility fallbacks.
- **Boot Restoration (`BootReceiver.kt`)**: Automatically re-registers all active alarms after a phone reboot (`ACTION_BOOT_COMPLETED`), running queries on background threads via `goAsync()`.
- **Active Alarm Service (`AlarmService.kt`)**: Runs as a `mediaPlayback` Foreground Service with system wake locks to guarantee continuous ringing, vibration, and overlay display even when the device is locked or asleep.
- **AI Core (`GeminiRepository.kt`)**: Retrofit-based client configured for `gemini-3.5-flash` text generation API.

---

## 🚀 How to Setup and Run Locally

### Prerequisites
- **JDK**: Version 17 is required.
- **Android Studio**: Android Studio Ladybug (2024.2.1) or later recommended.
- **SDK**: API 34+ (Android 14) installed.

### Setup Steps
1. **Clone & Open Project**:
   Open the root directory in Android Studio and let Gradle complete its initial project synchronization.

2. **Configure API Secrets**:
   WakeCall AI uses the **Secrets Gradle Plugin** to protect credentials. 
   - Create a `.env` file at the root of your project:
     ```env
     GEMINI_API_KEY=your_actual_gemini_api_key_here
     ```
   - Build-time compilation injects this key into `BuildConfig.GEMINI_API_KEY`, allowing the app to safely authenticate.

3. **Deploying to Device**:
   - Enable **Developer Options** and **USB Debugging** on your target device.
   - Select the `:app` module in Android Studio's run configuration dropdown.
   - Click the **Run** button to compile, install, and launch.

---

## 📦 Production & Google Play Publishing Guide

To generate a signed, production-ready artifact (AAB or APK) for the Google Play Store:

### 1. Generate an Upload Keystore
Run the following command in your terminal to create a secure keystore file:
```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
*Make sure to save the keystore path, store password, alias name, and key password securely.*

### 2. Configure Build Environment
Inject your keystore credentials safely as system environment variables:
```bash
export KEYSTORE_PATH="/path/to/my-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"
```

### 3. Generate Signed Bundle (AAB)
Run the release build task in your terminal:
```bash
./gradlew :app:bundleRelease
```
The compiled, signed, and optimized bundle will be generated at:
`/app/build/outputs/bundle/release/app-release.aab`

### 4. Upload to Google Play Console
1. Log in to the [Google Play Console](https://play.google.com/apps/publish).
2. Create or select your application.
3. Navigate to **Testing** -> **Closed testing** or **Production**.
4. Create a new release and upload the signed `app-release.aab` file.
5. Provide the required store listings, content ratings, and privacy policy URLs, then submit for review.

---

## 🛡️ Best Practices & System Optimizations

- **Battery Efficiency**: System `WakeLock` and foreground service components are acquired selectively and released immediately when the user interacts with the pads, preventing background battery drain.
- **Overlay Permissions**: For devices running Android 10+, overlay drawing permission (`Settings.ACTION_MANAGE_OVERLAY_PERMISSION`) is required to trigger full-screen incoming calls over lock screens. WakeCall AI detects this state and displays a modern, intuitive setup button in the dashboard to guide users.
- **Universal Sizing**: Layouts utilize fluid, container-relative Compose sizes and standard Material 3 spacing constraints, ensuring pixel-perfect layouts across compact mobile screens, foldables, and large tablet interfaces.

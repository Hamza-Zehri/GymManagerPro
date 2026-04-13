# 🏋️ Gym Manager Pro — Android App

A **complete, production-ready Android gym management system** built with Jetpack Compose,
Room SQLite, and Google Drive backup. Based on the Figma UI design by Engr. Hamza Asad.

---

## ✅ Features

| Feature | Details |
|---|---|
| **Member Management** | Add, view, edit, delete members (soft-delete with confirmation) |
| **Time Shifts** | Morning / Afternoon / Evening / Night / Custom per member |
| **Photo Upload** | Camera or gallery for member photos |
| **Attendance** | Per-day, per-shift tracking with toggle tap |
| **Fee Management** | Paid / Unpaid / Partial status; record partial payments |
| **Payment History** | Full ledger per member (Cash, Bank, Easypaisa, JazzCash) |
| **Subscription Plans** | Seed plans + add/remove plans; auto-link on member add |
| **Expenses** | Track gym costs by category |
| **Google Drive Backup** | Auto-upload daily when internet available |
| **Restore from Drive** | After reinstall → sign in → restore in Backup screen |
| **Settings** | WhatsApp/SMS toggles, app lock toggle, auto-backup toggle |
| **Dark Theme** | Full Material3 dark theme matching Figma |
| **Splash Screen** | Animated splash with API 31+ SplashScreen compat |

---

## 🗂️ Project Structure

```
GymManagerPro/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/gymmanager/
│   │   ├── MainActivity.kt              ← NavHost entry point
│   │   ├── data/
│   │   │   ├── model/Entities.kt        ← Room entities (Member, Attendance, Payment …)
│   │   │   ├── db/Daos.kt               ← All DAOs
│   │   │   ├── db/GymDatabase.kt        ← Room DB with seed data
│   │   │   └── repository/GymRepository.kt
│   │   ├── backup/
│   │   │   └── DriveBackupManager.kt    ← Google Drive upload / download / WorkManager
│   │   ├── viewmodel/GymViewModel.kt    ← Single ViewModel for all screens
│   │   ├── utils/DateUtils.kt
│   │   └── ui/
│   │       ├── Screen.kt                ← Navigation routes
│   │       ├── theme/Theme.kt + Typography.kt
│   │       ├── components/Components.kt ← Shared composables
│   │       └── screens/
│   │           ├── SplashSetup.kt
│   │           ├── DashboardScreen.kt
│   │           ├── AddMemberScreen.kt
│   │           ├── MembersListScreen.kt
│   │           ├── MemberProfileScreen.kt
│   │           ├── AttendanceScreen.kt
│   │           ├── FeeExpensePlansScreens.kt
│   │           ├── SettingsScreen.kt
│   │           └── BackupRestoreScreen.kt
│   └── res/
│       ├── drawable/ic_gym_splash.xml
│       ├── values/strings.xml + themes.xml
│       └── xml/file_paths.xml + backup_rules.xml …
```

---

## ⚙️ Setup Instructions

### 1. Prerequisites
- **Android Studio Hedgehog** or newer
- **JDK 17**
- **Android SDK** (API 24 minimum, API 34 target)

### 2. Open in Android Studio
```bash
File → Open → Select the GymManagerPro folder
```
Wait for Gradle sync to complete.

### 3. Google Drive Backup Setup (Required for backup feature)

#### a) Create a Firebase / Google Cloud project
1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create new project → **"GymManagerPro"**
3. Enable **Google Drive API**

#### b) Create OAuth 2.0 credentials
1. APIs & Services → Credentials → Create Credentials → OAuth 2.0 Client ID
2. Select **Android** → Enter your `applicationId` (`com.gymmanager`) and SHA-1 fingerprint
3. Also create a **Web Application** client ID — copy this value

#### c) Add the Web Client ID to the app
Open `DriveBackupManager.kt` and replace:
```kotlin
private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
```
With your actual Web Client ID from step b.

#### d) Download google-services.json
1. Firebase Console → Add Android App → enter `com.gymmanager`
2. Download `google-services.json`
3. Place it at: `app/google-services.json`

### 4. Generate Debug APK
```bash
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

### 5. Generate Signed Release APK

#### a) Create keystore
```bash
keytool -genkey -v -keystore gym_manager.keystore \
  -alias gym_manager -keyalg RSA -keysize 2048 -validity 10000
```

#### b) Add to `app/build.gradle`
```groovy
android {
    signingConfigs {
        release {
            storeFile file("../../gym_manager.keystore")
            storePassword "YOUR_STORE_PASSWORD"
            keyAlias "gym_manager"
            keyPassword "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

#### c) Build release APK
```bash
./gradlew assembleRelease
```
APK output: `app/build/outputs/apk/release/app-release.apk`

---

## 📱 How to Install APK on Device

1. Enable **Developer Options** on Android phone
2. Enable **Install from Unknown Sources**
3. Transfer APK via USB / WhatsApp / Google Drive
4. Tap APK → Install

---

## 🔁 Backup & Restore Flow

### Auto Backup (happens in background)
- App schedules a daily `AutoBackupWorker` via WorkManager
- When internet is available + user is signed into Google → DB is uploaded to Drive
- Up to 5 recent backups kept in Drive

### Manual Backup
- Settings → Backup & Restore → "Backup to Google Drive"

### Restore After Reinstall
1. Install app fresh
2. Complete setup screen
3. Go to Settings → Backup & Restore
4. Sign in with same Google account
5. Tap "Restore from Google Drive"
6. Restart the app

---

## 🕐 Time Shifts
Members can be assigned to one of these shifts:
- ☀️ **Morning** — 6 AM to 12 PM
- 🌤 **Afternoon** — 12 PM to 4 PM
- 🌇 **Evening** — 4 PM to 7 PM
- 🌙 **Night** — 7 PM to 12 AM
- 🕐 **Custom** — any time you specify (e.g. 05:30)

Attendance screen has a shift filter so you can mark only the members present in a specific slot.

---

## 🎨 Design Credits
UI design by **Engr. Hamza Asad** (Figma)  
Android implementation: Jetpack Compose + Material3 dark theme

---

## 📦 Dependencies Used
- Jetpack Compose + Material3
- Room (SQLite ORM)
- Navigation Compose
- Lifecycle ViewModel
- WorkManager (background backup)
- Google Sign-In + Drive API v3
- Coil (image loading)
- DataStore Preferences
- Accompanist (permissions)
- Core SplashScreen

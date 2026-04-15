# 🏋️ Gym Manager Pro — Android App

A **complete, production-ready Android gym management system** built with Jetpack Compose,
Room SQLite, and an automated manual-first backup system. Based on the Figma UI design by Engr. Hamza Asad.

---

## 📸 Screenshots

<p align="center">
  <img src="Assets/applock.png" width="200" />
  <img src="Assets/photo0.png" width="200" />
  <img src="Assets/photo1.png" width="200" />
  <img src="Assets/photo2.png" width="200" />
</p>
<p align="center">
  <img src="Assets/photo3.png" width="200" />
  <img src="Assets/photo4.png" width="200" />
  <img src="Assets/photo5.png" width="200" />
  <img src="Assets/photo6.png" width="200" />
</p>
<p align="center">
  <img src="Assets/photo7.png" width="200" />
  <img src="Assets/settings.png" width="200" />
</p>

---

## ✅ Key Features

| Feature | Description |
| :--- | :--- |
| **Member Management** | Add, edit, block/unblock members. Uses mandatory CNIC for unique ID. |
| **Internal Image Storage** | Photos are saved inside the app. They won't disappear even if you delete them from your gallery. |
| **Sync Two Phones** | Transfer all members, photos, and payments from one phone to another instantly. No internet required! |
| **App Lock (High Security)** | Fingerprint & PIN support. Auto-locks if you leave the app for 5 minutes. |
| **Full Backup & Restore** | Create a complete backup of data and photos into a single ZIP file. |
| **Auto Backup (24h)** | Automatically saves your data every day to your phone's memory. |
| **Messaging** | One-tap WhatsApp or SMS reminders for fees. |
| **Attendance** | Easy daily attendance tracking with shift support. |
| **Fee Management** | Track Paid, Unpaid, and Partial payments with a clear history. |
| **Subscription Plans** | Create custom packages (e.g., Monthly, Yearly) for your gym. |
| **Expenses** | Keep track of gym electricity, rent, and other costs. |
| **Professional UI** | Clean, dark theme design optimized for all screen sizes. |

---

## 📲 How to Sync Two Phones (Transfer Data)

If you have a new phone or want to copy data between devices:

### 1. On Phone A (The one with the data)
- Turn on your **Mobile Hotspot**.
- Open the app → **Settings → Sync Data**.
- Tap **"Run as Server"**.
- Note the **IP Address** shown on the screen (e.g., `192.168.43.1`).

### 2. On Phone B (The new phone)
- Connect to Phone A's **Hotspot** via Wi-Fi.
- Open the app → **Settings → Sync Data**.
- Enter the **IP Address** you saw on Phone A.
- Tap **"Sync Now"**.

**Done!** Everything (members, photos, and records) is now on the new phone.

---

## 🔁 Backup & Restore Flow

The app keeps your data safe with a simple backup system:

### 1. Manual Backup (Recommended)
- Go to **Settings → Backup & Restore**.
- Tap **Create Backup**.
- A full backup file (containing all data and photos) is saved in your **Downloads/GymBackup** folder.

### 2. Restoring Data
- Tap **Restore from File**.
- Select your backup file (it will be a `.zip` or `.db` file).
- The app will recover everything instantly.

---

## 🛡️ Security Features

- **5-Minute Grace Period**: The app only locks if it has been closed for more than 5 minutes.
- **App-Specific PIN**: Set a 4-digit PIN just for this app.
- **Biometric Integration**: Use your Fingerprint for fast, secure access.

---

## ⚙️ Setup Instructions

### 1. Prerequisites
- **Android Studio** (Hedgehog or newer)
- **Android Device** (API 24 minimum)

### 2. Build & Run
1. Open the project in Android Studio.
2. Click the **Run** button.

---

## 🎨 Design Credits
UI design by **Engr. Hamza Asad** (Figma)  
Implementation: Jetpack Compose + Material3

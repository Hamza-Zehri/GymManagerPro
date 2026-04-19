# 🏋️ Gym Manager Pro — Android App (v2.0.0)

A **complete, production-ready Android gym management system** built with Jetpack Compose,
Room SQLite, and an automated manual-first backup system. Based on the Figma UI design by Engr. Hamza Asad.


---

## 📸 Screenshots

<p align="center">
  <img src="Assets/featuregraphics.png" width="200" />
</p>
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


---

## ✅ Key Features (New in v2.0.0)

| Feature | Description |
| :--- | :--- |
| **New! Hotspot Sync (v2.0)** | Transfer data between phones instantly using a Local Hotspot. No internet required. |
| **New! ZIP Backups (v2.0)** | Backups now include both the database AND member photos in a single `.zip` file. |
| **Member Management** | Add, edit, block/unblock members. Uses mandatory CNIC for unique ID. |
| **Internal Image Storage** | Photos are saved inside the app's private storage for maximum security. |
| **App Lock (High Security)** | Fingerprint & PIN support. Locks instantly when you leave the app or lock the screen. |
| **Auto Backup (24h)** | Automatically saves your data every day to your phone's memory. |
| **Messaging** | One-tap WhatsApp or SMS reminders for fees. |
| **Attendance** | Easy daily attendance tracking with shift support. |
| **Fee Management** | Track Paid, Unpaid, and Partial payments with a clear history. |
| **Subscription Plans** | Create, edit, or delete custom packages (e.g., Monthly, Yearly). |
| **Intelligent Renewal** | Resubscribe expired members with cumulative debt tracking and exact date alignment (e.g., 5th to 5th). |
| **Smart Expiry Tracker** | Dashboard notification icon (Amber) for members whose subscription expires within 5 days. |
| **Professional UI** | Clean, dark theme design optimized for all screen sizes. |

---

## 📲 How to Sync Two Phones (Local Transfer)

If you have a new phone or want to copy data between devices:

### 1. On Phone A (The one with the data)
- Turn on your **Mobile Hotspot**.
- Open the app → **Settings → Sync Data**.
- Tap **"Host Data"** (Phone A).
- Note the **IP Address** shown on the screen (e.g., `192.168.43.1`).

### 2. On Phone B (The new phone)
- Connect to Phone A's **Hotspot** via Wi-Fi.
- Open the app → **Settings → Sync Data**.
- Tap **"Receive Data"** (Phone B).
- Enter the **IP Address** you saw on Phone A.
- Tap **"Start Sync"**.

**Done!** Everything (members, photos, and records) is now on the new phone.

---

## 🔁 Backup & Restore Flow (ZIP Format)

The app keeps your data safe with an improved backup system:

### 1. Manual Backup
- Go to **Settings → Backup & Restore**.
- Tap **Create Backup**.
- A full backup file (`GymBackup_YYYYMMDD_HHMMSS.zip`) is saved in your **Downloads/GymBackup** folder.
- This file contains your entire database and all member profile pictures.

### 2. Restoring Data
- Tap **Restore from File**.
- Select your `.zip` backup file.
- The app will extract the photos and restore the database instantly.

---

## 🛡️ Privacy Policy
Your privacy is important to us. View our full privacy policy here:
[Privacy Policy](https://github.com/Hamza-Zehri/GymManagerPro/blob/master/PRIVACY_POLICY.md)

---

## 🛡️ Security Features

- **Instant Lock**: The app locks immediately every time it is minimized or the screen turns off.
- **App-Specific PIN**: Set a 4-digit PIN just for this app.
- **Biometric Integration**: Use your Fingerprint for fast, secure access.

---

## ⚙️ Setup Instructions

### 1. Prerequisites
- **Android Studio Hedgehog** or newer
- **Android Device** (API 24 minimum)

### 2. Build & Run
1. Open the project in Android Studio.
2. Click the **Run** button.

---

## 🎨 Design Credits
UI design by **Engr. Hamza Asad** (Figma)  
Implementation: Jetpack Compose + Material3

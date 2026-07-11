# Plan Next Day — Private Android Planner

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%2010%2B%20%28API%2029%2B%29-green.svg?style=flat&logo=android)](https://www.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-navy.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A clean, modern, and privacy-focused Android planner built entirely with **Jetpack Compose** and **Material 3**. 

Unlike other planner apps, **Plan Next Day** is local-only. There are no accounts, cloud sync, analytics, or trackers. All your plans stay privately on your device.

---

## 📱 Key Features

* **Today (Read-Only)**: Focus entirely on today's tasks. Tasks can only be checked/unchecked to protect your focus and keep you motivated.
* **Tomorrow (Planning)**: Plan the next day in advance. Add tasks through a bouncy composer or confirmation-protected "Copy Today's Tasks" utility. At midnight, tomorrow's plan becomes today's active list automatically.
* **Weekly Promise Tracker**: A motivating card that tracks missed tasks against a user-defined threshold. Once reached, it locks into a supportive "try again next week" state.
* **Modern Material 3 Expressive Design**: Pill-shaped bottom navigation dock, soft theme-matched color palettes, and satisfying custom physics-based animations (bouncy spring checkboxes, layout size transitions).
* **Smart Reminders**: Scheduled reminders that only trigger if you haven't planned your next day by your custom reminder time, helping you build a daily habit without spamming notifications.
* **Full Dark Theme**: Rich light and dark color schemes that match system settings.

---

## 🛠️ Tech Stack & Architecture

This project follows Android development best practices and modern clean architecture guidelines:

* **Jetpack Compose**: 100% declarative UI toolkit for building modern layouts.
* **Room Database**: Offline-first local SQLite database for task persistence and weekly tracking.
* **DataStore Preferences**: Reactive key-value storage for settings (reminders, theme, weekly limits).
* **WorkManager**: Background task scheduling for battery-friendly, local cutoff notifications.
* **StateFlow & Coroutines**: Reactive state management and async operations.
* **Jetpack ViewModel**: Architectural separation of business logic and UI state.

---

## 🚀 Getting Started

### Install the App (User)
You can download the pre-compiled APK directly and install it on your Android phone:
1. Go to the **Releases** tab on the right side of this repository.
2. Download **`PlanNextDay.apk`**.
3. Open the file on your device and tap **Install** (if prompted, select *Allow installation from unknown sources*).

### Build from Source (Developer)
1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/PlanNextDay.git
   ```
2. Open the project folder in **Android Studio** (Koala or newer).
3. Connect your Android device or start an emulator (API 29+).
4. Click **Run** (`Shift + F10`) to build and run the app.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

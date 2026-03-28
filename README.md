# GearDex 🚗

A modern Android vehicle management app built with Kotlin. Track your vehicles, maintenance logs, fuel economy, documents, and discover scenic Greek driving routes.

## Features

- **Garage** — Manage Cars, Motorcycles and ATVs with odometer tracking
- **Logs** — Fuel economy calculations and service history (oil change, brakes, timing belt, etc.)
- **Glovebox** — Secure on-device storage for KTEO, insurance, road tax and other documents
- **Ekdromes** — Curated Greek driving routes with region and difficulty filters
- **Cloud Sync** — Optional Firebase backend to sync your data across devices
- **Bilingual** — Full English and Greek (Ελληνικά) support

## Tech Stack

| Layer | Library |
|-------|---------|
| Language | Kotlin 2.1.20 |
| UI | Material 3, ViewBinding, Navigation Component |
| DI | Hilt 2.56 |
| Database | Room 2.7.0 |
| Async | Kotlin Coroutines + Flow |
| Background | WorkManager 2.10.0 |
| Cloud | Firebase Auth + Firestore |
| Min SDK | 24 (Android 7.0) |

## Getting Started

1. Clone the repo
   ```bash
   git clone https://github.com/TheoSfak/GearDex.git
   ```
2. Open in Android Studio
3. (Optional) Copy `firebase.properties.template` to `firebase.properties` and fill in your Firebase credentials for cloud sync
4. Build & run on an emulator or device (API 24+)

> Cloud sync is fully optional. The app works completely offline without any Firebase configuration.

## Author

**Theodore Sfakianakis**
theodore.sfakianakis@gmail.com

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

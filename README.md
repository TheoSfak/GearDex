# GearDex 🚗

A modern Android vehicle management app built with Kotlin. Track your vehicles, maintenance, fuel, expenses, trips, and discover scenic Greek driving routes.

## Features

### Core
- **Garage** — Manage Cars, Motorcycles, and ATVs with full vehicle profiles and odometer tracking
- **Logs** — Fuel economy calculations, service history (oil change, brakes, timing belt, etc.), and tabbed analytics
- **Glovebox** — Secure on-device storage for KTEO, insurance, road tax, and other documents with ML Kit OCR scan
- **Ekdromes** — Curated Greek driving routes with region, difficulty filters, bookmarks, and community suggestions
- **Cloud Sync** — Optional Firebase backend to sync data across devices
- **Bilingual** — Full English and Greek (Ελληνικά) support

### v1.5.0 — New Features
- **Expense Tracker & Budget Planner** — Log all vehicle costs (fuel, insurance, fines, repairs), monthly budget alerts, and cost-per-km analytics
- **Vehicle Health Score** — Predictive insights based on service intervals, reminders, and mileage; scores each vehicle 0–100 and flags issues early
- **Trip Logger** — Record trips with start/end odometer, purpose, fuel used, and cost per trip; view trip history in the Logs tab
- **Marketplace** — Browse and list vehicle parts for sale/trade via Firebase Firestore crowdsourced listings
- **Fleet Dashboard** — Side-by-side comparison of all your vehicles: health scores, fuel economy, monthly costs, and service status
- **Drive Mode** — Start/stop timed drive sessions and automatically track duration, distance, and estimated fuel consumption
- **Service Shop Directory** — Browse community-submitted service shops by category and region, save favourites, and submit new shops
- **Parking Saver** — Save your parking spot with GPS or manual address, set a metered parking timer with a 5-minute notification alert, open in Maps, or share your location

## Tech Stack

| Layer | Library |
|-------|---------|
| Language | Kotlin 2.1.20 |
| UI | Material 3, ViewBinding, Navigation Component |
| DI | Hilt 2.56 |
| Database | Room 2.7.0 (v7, AutoMigrations) |
| Async | Kotlin Coroutines + Flow |
| Background | WorkManager 2.10.0 |
| Charts | Vico 2.0.2 |
| Cloud | Firebase Auth + Firestore |
| OCR | ML Kit Text Recognition |
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

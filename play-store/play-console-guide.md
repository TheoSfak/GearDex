# GearDex Play Console Guide

## Build Artifact

Upload this Android App Bundle:

`app/build/outputs/bundle/playstoreRelease/app-playstore-release.aab`

Version:

- Version code: 31
- Version name: 1.6.16
- Application ID: `com.geardex.app`
- Target SDK: 36

## Store Listing

App name: GearDex

Short description:

Vehicle management for fuel logs, service reminders, expenses, documents, trips, and parking.

Full description:

GearDex helps you manage your vehicles in one organized garage. Track cars, motorcycles, and ATVs with odometer history, maintenance records, fuel logs, service plans, expenses, documents, and reminders.

Keep your garage up to date with fuel economy calculations, service history, cost analytics, and vehicle health insights. Store important vehicle documents such as insurance, KTEO, road tax, and service receipts in the digital glovebox. Use camera-based receipt scanning to speed up data entry, with OCR handled on device.

GearDex also includes trip logging, parking spot saving, drive sessions, service shop notes, route discovery, local route reviews, and marketplace-style part tracking. The app is designed for everyday vehicle ownership: simple enough for quick fuel logs, detailed enough for long-term maintenance planning.

Key features:
- Vehicle garage for cars, motorcycles, and ATVs
- Fuel, service, expense, and trip logs
- Maintenance reminders and service plans
- Digital glovebox for vehicle documents
- On-device OCR receipt scanning
- Parking saver with optional location support
- Vehicle health score and cost analytics
- Route discovery and local route notes
- Home screen widget
- English and Greek support

GearDex can be used offline. Optional cloud sync is available only when configured and enabled by the user.

## Graphics

App icon:

`play-store/assets/app-icon-512.png`

Feature graphic:

`play-store/assets/feature-graphic-1024x500.png`

Phone screenshots:

- `play-store/screenshots/phone/01-garage.jpg`
- `play-store/screenshots/phone/02-parking.jpg`
- `play-store/screenshots/phone/03-logs.jpg`
- `play-store/screenshots/phone/04-glovebox.jpg`
- `play-store/screenshots/phone/05-excursions.jpg`

## App Setup

App type: App

Free or paid: Free

Category: Auto & Vehicles

Ads: No ads

Contact email: `theodore.sfakianakis@gmail.com`

## Release Notes

Initial GearDex release.

- Manage cars, motorcycles, and ATVs
- Track fuel, service, expense, and trip history
- Save documents in a digital glovebox
- Scan receipts with on-device OCR
- Create reminders and service plans
- Save parking spots and meter alerts
- View vehicle health and cost insights
- Explore routes and local notes

## Data Safety

The current local Play Store build has `firebase.enabled=false`.

Declare data accurately based on the uploaded build:

- No ads.
- No data selling.
- Camera is used for user-requested photos and receipt/document scanning.
- OCR runs on device.
- Location is used for parking and route/navigation features when the user grants permission.
- Notifications are used for reminders and parking alerts when the user grants permission.
- App records are stored locally on the device.

If you later enable Firebase cloud sync, update Data safety to include account/sign-in data and synced vehicle app records.

## Privacy Policy URL

Local source:

`docs/privacy-policy.html`

If GitHub Pages is enabled for the repository from the `docs/` folder, the likely URL is:

`https://theosfak.github.io/GearDex/privacy-policy.html`

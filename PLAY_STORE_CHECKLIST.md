# GearDex — Google Play Store Release Checklist

## 1. Create Release Keystore

```powershell
keytool -genkey -v -keystore geardex-release.ks -keyalg RSA -keysize 2048 -validity 10000 -alias geardex
```

Then create `keystore.properties` in the project root (DO NOT commit this file):

```properties
storeFile=../geardex-release.ks
storePassword=YOUR_STORE_PASSWORD
keyAlias=geardex
keyPassword=YOUR_KEY_PASSWORD
```

> Keep a backup of `geardex-release.ks` somewhere safe. If you lose it, you can never update the app on Play Store.

---

## 2. Build Release AAB

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
.\gradlew.bat bundleRelease --no-daemon
```

Output: `app/build/outputs/bundle/release/app-release.aab`

---

## 3. Privacy Policy

Required because the app uses **CAMERA** permission.

- Write a privacy policy page stating:
  - Camera is used only for scanning fuel receipts (OCR), images are processed locally via ML Kit and never uploaded
  - No personal data is collected or shared
  - Firebase sync is optional and user-initiated
  - Data stays on-device in a local Room database
- Host it on GitHub Pages, a free site, or Google Docs (public link)
- You'll paste this URL in Play Console

---

## 4. Play Console Setup

### 4a. Create Developer Account
- Go to https://play.google.com/console
- Pay the one-time $25 registration fee
- Verify identity (may take 48h)

### 4b. Create App
- App name: **GearDex**
- Default language: **English (en)**
- App type: **App** (not game)
- Free or paid: **Free**

### 4c. Store Listing
Fill in all required fields:

| Field | Value |
|-------|-------|
| App name | GearDex |
| Short description | Vehicle management: fuel logs, service reminders, documents & health score |
| Full description | (Write ~300 words about features: garage, fuel/service logs, OCR receipt scanner, maintenance reminders, document storage, vehicle health score, home widget, bilingual EN/EL) |
| App icon | 512×512 PNG (use the gear+odometer+wrench from `ic_launcher_foreground.xml` on `#1A1A1A` background) |
| Feature graphic | 1024×500 PNG (app name + tagline on branded background) |
| Screenshots | At least 2 phone screenshots (Phone), optionally tablet |
| App category | **Auto & Vehicles** |
| Contact email | your email |

### 4d. Content Rating
- Go to **Policy → App content → Content rating**
- Fill the IARC questionnaire
- No violence, no user-generated content, no personal info → should get **Everyone** rating

### 4e. Data Safety Form
Go to **Policy → App content → Data safety** and declare:

| Question | Answer |
|----------|--------|
| Does your app collect or share user data? | **No** (if Firebase is disabled) or **Yes** (if Firebase enabled) |
| Camera | Used for OCR scanning only, processed on-device, not shared |
| Files/documents | Stored locally only |
| Location | Not collected |
| Personal info | Not collected |
| App activity | Not collected |

If Firebase is enabled, also declare:
- Authentication data (email) — collected, not shared, user can delete account
- Cloud sync data — collected with user consent, encrypted in transit

### 4f. Target Audience
- Select **18+** or **All ages** (no child-directed content)
- Confirm the app is not designed for children

### 4g. Ads Declaration
- **No ads** in the app

---

## 5. Upload & Release

1. Go to **Production → Create new release**
2. Upload `app-release.aab`
3. Add release notes:
   ```
   Initial release of GearDex v1.0.0
   - Vehicle garage management
   - Fuel & service log tracking with analytics
   - OCR receipt scanner for quick fuel log entry
   - Maintenance reminders (km-based & date-based)
   - Document storage (KTEO, insurance, road tax)
   - Vehicle health score (0-100)
   - Home screen widget
   - Bilingual: English & Greek
   ```
4. Review and roll out to **Production** (or start with **Internal testing** first)

---

## 6. Version Bumping (for future updates)

In `app/build.gradle.kts`, increment before each release:

```kotlin
versionCode = 2          // must increase every upload
versionName = "1.1.0"    // semantic version for users
```

---

## 7. Optional — App Signing by Google Play

Google Play Console will offer **Play App Signing**. This is recommended:
- Google manages your upload key
- If you lose your keystore, Google can still accept updates
- Enables automatic optimization of APK delivery

You'll be prompted when you upload your first AAB.

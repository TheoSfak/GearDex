# Firebase Setup for GearDex

## 1. Create Firebase Project
1. Go to https://console.firebase.google.com/
2. Sign in with your Google account
3. Click **"Create a project"** → name it `GearDex`
4. Disable Google Analytics (not needed) → **Create**

## 2. Add Android App
1. Click the **Android icon** to add an Android app
2. Package name: `com.geardex.app`
3. Click **Register app**
4. **Skip** the `google-services.json` download (your app uses manual init)

## 3. Copy Config Values
Go to **Project Settings** (gear icon, top-left) and copy:
- **Project ID** (e.g. `geardex-xxxxx`)
- **Web API Key**
- **App ID** (format: `1:123456789:android:abcdef`)

## 4. Enable Firestore
1. In the left menu: **Build → Firestore Database**
2. Click **Create database**
3. Choose **Start in test mode**
4. Pick region: `europe-west1` (closest to Greece)

## 5. Fill in firebase.properties
Open `firebase.properties` in the project root and fill in:
```properties
firebase.enabled=true
firebase.appId=YOUR_APP_ID_HERE
firebase.apiKey=YOUR_WEB_API_KEY_HERE
firebase.projectId=YOUR_PROJECT_ID_HERE
```

## 6. Rebuild the APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
.\gradlew.bat assembleDebug --no-daemon
Copy-Item "app\build\outputs\apk\debug\app-debug.apk" "$env:USERPROFILE\Desktop\GearDex.apk" -Force
```

Once rebuilt, every user who installs the APK will have community routes enabled automatically.

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.androidx.navigation.safeargs)
}

// ── Signing config (create keystore.properties to enable release signing) ──
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// ── Firebase config (create firebase.properties to enable cloud sync) ──
val firebasePropertiesFile = rootProject.file("firebase.properties")
val firebaseProperties = Properties()
if (firebasePropertiesFile.exists()) {
    firebaseProperties.load(FileInputStream(firebasePropertiesFile))
}

android {
    namespace = "com.geardex.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.geardex.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 16
        versionName = "1.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Support all common ABIs for Play Store
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // Firebase build config fields (populated from firebase.properties)
        buildConfigField("boolean", "FIREBASE_ENABLED",
            firebaseProperties.getProperty("firebase.enabled", "false"))
        buildConfigField("String", "FIREBASE_APP_ID",
            "\"${firebaseProperties.getProperty("firebase.appId", "")}\"")
        buildConfigField("String", "FIREBASE_API_KEY",
            "\"${firebaseProperties.getProperty("firebase.apiKey", "")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID",
            "\"${firebaseProperties.getProperty("firebase.projectId", "")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID",
            "\"${firebaseProperties.getProperty("google.webClientId", "")}\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_UPDATE_CHECK", "true")
            buildConfigField("String", "GITHUB_REPO", "\"TheoSfak/GearDex\"")
        }
        create("playstore") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_UPDATE_CHECK", "false")
            buildConfigField("String", "GITHUB_REPO", "\"\"")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    bundle {
        language {
            // Keep all language splits so EN + EL both download
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    androidResources {
        // Only bundle EN + EL localizations
        localeFilters += listOf("en", "el")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        // Treat missing translations as error
        error += "MissingTranslation"
        // These noisy advisory checks are tracked separately from release-blocking lint.
        disable += listOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "KaptUsageInsteadOfKsp",
            "Overdraw",
            "SetTextI18n",
            "UnusedResources",
            "UseCompoundDrawables",
            "UselessParent",
            "VectorPath"
        )
    }

    // Room schema export directory for AutoMigration
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime)

    // Hilt WorkManager integration
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)

    // Firebase (no google-services.json needed — uses manual init via firebase.properties)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Vico charts
    implementation(libs.vico.views)

    // ML Kit Text Recognition (bundled — no Play Services needed, works offline)
    implementation(libs.mlkit.text.recognition)

    // Glance App Widget
    implementation(libs.glance.appwidget)

    // Google Sign-In
    implementation(libs.play.services.auth)
}

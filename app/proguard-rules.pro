# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK proguard/proguard-android.txt file.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see:
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# ── Firebase ──
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── Room entities ──
-keep class com.geardex.app.data.local.entity.** { *; }

# ── Hilt generated ──
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager { *; }

# ── Kotlin coroutines ──
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Navigation SafeArgs ──
-keep class com.geardex.app.**Args { *; }
-keep class com.geardex.app.**Directions { *; }

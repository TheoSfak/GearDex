# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK proguard/proguard-android.txt file.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see:
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# ── General ──
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable   # useful for crash reports
-renamesourcefileattribute SourceFile

# ── Firebase ──
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Room entities & DAOs ──
-keep class com.geardex.app.data.local.entity.** { *; }
-keep class com.geardex.app.data.local.dao.** { *; }

# ── Room TypeConverters ──
-keep class com.geardex.app.data.local.GearDexDatabase$Converters { *; }

# ── Hilt generated ──
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager { *; }
-dontwarn dagger.hilt.**

# ── Kotlin coroutines ──
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Navigation SafeArgs ──
-keep class com.geardex.app.**Args { *; }
-keep class com.geardex.app.**Directions { *; }

# ── ML Kit Text Recognition ──
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── Glance AppWidget ──
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# ── Vico charts ──
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ── Enums (stored as TEXT in Room) ──
-keepclassmembers enum com.geardex.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── DataStore ──
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Keep R8 from stripping Parcelable/Serializable ──
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

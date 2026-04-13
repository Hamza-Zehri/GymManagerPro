# Add project specific ProGuard rules here.
-keep class com.gymmanager.data.model.** { *; }
-keep class com.gymmanager.data.db.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Google API / Drive
-keep class com.google.api.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.android.gms.**

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

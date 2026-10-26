# ProGuard / R8 rules for Infinity Sandbox
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Moshi models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.example.data.model.** { *; }
-keep class com.example.domain.model.** { *; }

# Keep Java utilities
-keep class com.example.util.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep BuildConfig
-keep class com.example.BuildConfig { *; }

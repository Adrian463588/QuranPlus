# ==============================================================================
# Quran Plus Hardened R8 / ProGuard Configuration (OWASP MASVS-RESILIENCE)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Code Shrinking & Obfuscation Aggression
# ------------------------------------------------------------------------------
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.quranplus.app.obf'
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,EnclosingMethod

# Strip verbose/debug logs in release builds to prevent information disclosure
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ------------------------------------------------------------------------------
# 2. Kotlin & Coroutines Safety
# ------------------------------------------------------------------------------
-keepclassmembers class * extends kotlin.jvm.internal.Lambda {
    <fields>;
}

# ------------------------------------------------------------------------------
# 3. Koin Dependency Injection
# ------------------------------------------------------------------------------
-keep class * implements org.koin.core.module.Module
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ------------------------------------------------------------------------------
# 4. Room Database & FTS5
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ------------------------------------------------------------------------------
# 5. Native JNI & AI Runtimes (ONNX Runtime, LiteRT-LM, SQLite-Vec)
# ------------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**

# ------------------------------------------------------------------------------
# 6. Media3 & ExoPlayer
# ------------------------------------------------------------------------------
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }

# ------------------------------------------------------------------------------
# 7. Security Engine & App Integrity Protection
# ------------------------------------------------------------------------------
-keep class com.quranplus.app.core.security.** { *; }

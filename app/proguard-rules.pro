# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# App classes
-keep class com.auraai.overlay.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**

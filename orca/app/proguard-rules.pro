# Orca - Abyssal Neon ProGuard Rules

# Keep Gemini AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Keep Room entities
-keep class com.orca.agent.data.models.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.orca.agent.data.network.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep data classes used with Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# General Android
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.accessibilityservice.AccessibilityService

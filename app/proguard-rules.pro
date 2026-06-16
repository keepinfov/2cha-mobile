# =============================================================================
# 2cha VPN Client - ProGuard Rules
# =============================================================================

# =============================================================================
# GENERAL OPTIMIZATION
# =============================================================================

# Preserve source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep attributes for better debugging
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# =============================================================================
# KOTLIN
# =============================================================================

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Don't warn about kotlinx serialization
-dontwarn kotlinx.serialization.**
-dontnote kotlinx.serialization.AnnotationsKt

# =============================================================================
# ANDROID COMPONENTS
# =============================================================================

# Keep VPN Service - critical for app functionality
-keep public class * extends android.net.VpnService
-keep class dev.yaul.twocha.vpn.TwochaVpnService { *; }
-keepclassmembers class dev.yaul.twocha.vpn.TwochaVpnService {
    public <methods>;
}

# Keep custom Application class
-keep public class * extends android.app.Application

# Keep Activities
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Activity

# Keep Fragments
-keep public class * extends androidx.fragment.app.Fragment

# =============================================================================
# JETPACK COMPOSE
# =============================================================================

# Keep only essential Compose classes - let R8 remove unused code
-keep,allowobfuscation @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Don't warn about Compose
-dontwarn androidx.compose.**

# =============================================================================
# HILT / DAGGER
# =============================================================================

# Keep only essential Hilt/Dagger classes - R8 handles most of this automatically
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Keep ViewModel classes (Hilt injects these)
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class dev.yaul.twocha.viewmodel.** { *; }

# =============================================================================
# NAVIGATION
# =============================================================================

# Navigation is handled well by R8, minimal rules needed

# =============================================================================
# DATASTORE
# =============================================================================

# Keep preference manager to prevent key obfuscation issues
-keep class dev.yaul.twocha.data.PreferencesManager { *; }
-keep class dev.yaul.twocha.data.PreferencesManager$* { *; }

# =============================================================================
# APP-SPECIFIC CLASSES
# =============================================================================

# Keep configuration classes for serialization
-keep,includedescriptorclasses class dev.yaul.twocha.config.**$$serializer { *; }
-keepclassmembers class dev.yaul.twocha.config.** {
    *** Companion;
    <fields>;
    <methods>;
}
-keepclasseswithmembers class dev.yaul.twocha.config.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class dev.yaul.twocha.config.** {
    *;
}

# Keep protocol classes - critical for VPN functionality
-keep class dev.yaul.twocha.protocol.** { *; }
-keepclassmembers class dev.yaul.twocha.protocol.** {
    <fields>;
    <methods>;
}

# Keep ThemeStyle enum for proper serialization/deserialization
-keep enum dev.yaul.twocha.ui.theme.ThemeStyle { *; }
-keepclassmembers enum dev.yaul.twocha.ui.theme.ThemeStyle {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
}

# Keep data classes
-keep @kotlinx.serialization.Serializable class * {
    *;
}

# Keep models/entities
-keep class dev.yaul.twocha.data.** { *; }

# =============================================================================
# NATIVE FFI (JNA + uniffi)
# =============================================================================

# JNA binds native code to these classes by reflecting on field/method names
# (e.g. com.sun.jna.Pointer.peer is read from libjnidispatch.so via JNI). R8
# obfuscation renames those members and breaks Native.initIDs with
# "Can't obtain peer field ID for class com.sun.jna.Pointer". Keep JNA intact.
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { *; }
-dontwarn java.awt.**

# The uniffi-generated bindings declare JNA Structure subclasses
# (UniffiRustCallStatus, RustBuffer, ...) whose public fields JNA marshals by
# name and order — they must not be renamed, reordered or stripped.
-keep class uniffi.** { *; }
-keepclassmembers class uniffi.** { *; }

# =============================================================================
# THIRD-PARTY LIBRARIES
# =============================================================================

# Bouncy Castle - Keep only crypto provider classes
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-dontwarn org.bouncycastle.**
-dontnote org.bouncycastle.**

# TOML Parser - R8 can optimize this well
-dontwarn com.akuleshov7.ktoml.**

# Accompanist - R8 can optimize this well
-dontwarn com.google.accompanist.**

# =============================================================================
# R8 COMPATIBILITY
# =============================================================================

# R8 full mode compatibility
-allowaccessmodification
-repackageclasses

# Remove logging in release builds for better performance
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# =============================================================================
# SUPPRESS WARNINGS
# =============================================================================

# General warnings suppression for libraries that are correctly configured
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.openjsse.**

# Suppress warnings for missing classes that are not used
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# =============================================================================
# REFLECTION
# =============================================================================

# Keep classes that use reflection
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <init>(...);
}

# Keep custom annotations
-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

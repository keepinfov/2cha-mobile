# 2cha VPN Client ProGuard Rules

# Keep crypto classes
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep config classes
-keep,includedescriptorclasses class dev.yaul.twocha.config.**$$serializer { *; }
-keepclassmembers class dev.yaul.twocha.config.** {
    *** Companion;
}
-keepclasseswithmembers class dev.yaul.twocha.config.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep VPN Service
-keep class dev.yaul.twocha.vpn.TwochaVpnService { *; }

# Keep protocol classes
-keep class dev.yaul.twocha.protocol.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Compose
-dontwarn androidx.compose.**

# DataStore Preferences - prevent ClassCastException
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep preference keys to prevent type confusion after obfuscation
-keep class dev.yaul.twocha.data.PreferencesManager { *; }
-keep class dev.yaul.twocha.data.PreferencesManager$Companion { *; }

# Keep ThemeStyle enum for proper deserialization
-keep enum dev.yaul.twocha.ui.theme.ThemeStyle { *; }

# Keep ViewModel classes
-keep class dev.yaul.twocha.viewmodel.** { *; }
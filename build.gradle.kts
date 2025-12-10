// Top-level build file for 2cha Android VPN Client
// Root build.gradle.kts
plugins {
    // 1. Stable Android Gradle Plugin
    id("com.android.application") version "8.13.1" apply false
    id("com.android.library") version "8.13.1" apply false

    // 2. Stable Kotlin (Use 2.2.21)
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false

    // 3. Matching KSP for Kotlin 2.2.21 (Critical)
    id("com.google.devtools.ksp") version "2.2.21-1.0.27" apply false

    // 4. Hilt (Standard version)
    id("com.google.dagger.hilt.android") version "2.57.2" apply false

    alias(libs.plugins.compose.compiler) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
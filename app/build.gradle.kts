plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.yaul.twocha"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "dev.yaul.twocha"
        minSdk = 29
        targetSdk = 36
        versionCode = 35
        versionName = "0.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Additional optimizations for smaller APK
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    // Split APKs by ABI to reduce individual APK size
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/*.kotlin_module"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM - Material 3
    implementation(platform(libs.compose.bom.v20240200))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Material Icons Extended is large (~3MB) - consider replacing with core icons or custom drawables
    // For now keeping it but R8 will remove unused icons with optimized ProGuard rules
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)

    // Fragments and material components for fragment-based UI
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // TOML Parser
    implementation(libs.ktoml.core)
    implementation(libs.ktoml.file)

    // Bouncy Castle for ChaCha20-Poly1305 and AES-GCM
    implementation(libs.bcprov.jdk18on)

    // JNA — runtime loader for the uniffi-generated bindings (libtwocha_mobile.so)
    implementation(libs.jna) {
        artifact {
            type = "aar"
        }
    }

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom.v20240200))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ---------------------------------------------------------------------------
// Native Rust core (twocha-mobile) — cargo-ndk + uniffi bindings
//
// The v4 engine lives in the `2cha` workspace, vendored here as a git submodule
// at `native/2cha`. These tasks compile it to a per-ABI .so via cargo-ndk and
// generate the Kotlin FFI bindings via the crate's own uniffi-bindgen, wiring
// both into the app source set so a plain `./gradlew assembleDebug` produces a
// self-contained APK. Run inside the Nix dev shell (`nix develop` / direnv),
// which provides the Rust toolchain, cargo-ndk and the Android NDK.
// ---------------------------------------------------------------------------

val nativeRoot = rootProject.layout.projectDirectory.dir("native/2cha")
val nativeManifest = nativeRoot.file("Cargo.toml")
val jniLibsDir = layout.projectDirectory.dir("src/main/jniLibs")
val bindingsDir = layout.projectDirectory.dir("src/main/java")

// ABIs we ship; cargo-ndk maps each to its matching Rust android target.
val nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

// Resolve the NDK: prefer an already-exported env (Nix dev shell / CI), else
// fall back to the newest NDK installed under the configured Android SDK.
fun resolveNdkHome(): String? {
    System.getenv("ANDROID_NDK_HOME")?.takeIf { it.isNotBlank() }?.let { return it }
    System.getenv("ANDROID_NDK_ROOT")?.takeIf { it.isNotBlank() }?.let { return it }
    return File(android.sdkDirectory, "ndk").listFiles()
        ?.filter { it.isDirectory }
        ?.maxByOrNull { it.name }
        ?.absolutePath
}

val buildRustNative by tasks.registering(Exec::class) {
    group = "native"
    description = "Compile twocha-mobile to per-ABI .so via cargo-ndk"

    workingDir = nativeRoot.asFile
    commandLine = listOf("cargo", "ndk") +
        nativeAbis.flatMap { listOf("-t", it) } +
        listOf("-o", jniLibsDir.asFile.absolutePath, "build", "--release", "-p", "twocha-mobile")

    doFirst {
        if (!nativeManifest.asFile.exists()) {
            throw GradleException(
                "Native crate not found at ${nativeManifest.asFile}. " +
                    "Run `git submodule update --init --recursive`.")
        }
        val ndk = resolveNdkHome() ?: throw GradleException(
            "Android NDK not found. Enter the Nix dev shell (it exports " +
                "ANDROID_NDK_HOME) or install an NDK via the SDK manager.")
        environment("ANDROID_NDK_HOME", ndk)
        environment("ANDROID_NDK_ROOT", ndk)
        jniLibsDir.asFile.mkdirs()
    }
}

val generateUniffiBindings by tasks.registering(Exec::class) {
    group = "native"
    description = "Generate Kotlin FFI bindings from the built .so"
    dependsOn(buildRustNative)

    workingDir = nativeRoot.asFile
    // Library mode reads metadata from any one ABI's .so; arm64-v8a always builds.
    val lib = jniLibsDir.file("arm64-v8a/libtwocha_mobile.so").asFile
    commandLine = listOf(
        "cargo", "run", "--release", "-p", "twocha-mobile", "--bin", "uniffi-bindgen",
        "--", "generate", "--library", lib.absolutePath,
        "--language", "kotlin", "--out-dir", bindingsDir.asFile.absolutePath)

    doFirst { bindingsDir.asFile.mkdirs() }
}

// Every Android build first produces the native artifacts + bindings.
tasks.named("preBuild") {
    dependsOn(generateUniffiBindings)
}
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.compose.compiler)
}

// Release signing credentials live in a gitignored keystore.properties at the
// repo root. When it's absent (e.g. CI without secrets) release builds fall
// back to unsigned, so the build still succeeds.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
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
        versionCode = 40
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    // JNA — runtime loader for the uniffi-generated bindings (libtwocha_mobile.so)
    implementation(libs.jna) {
        artifact {
            type = "aar"
        }
    }

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // EncryptedSharedPreferences — at-rest storage for the client private key
    implementation(libs.androidx.security.crypto)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Accompanist
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // QR config import: CameraX preview/analysis + ZXing decoding
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.core)

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
// Overridable via -PnativeAbis=arm64-v8a[,...] so CI can build a fast
// single-ABI debug APK on pull requests (release builds keep all four).
val nativeAbis = (findProperty("nativeAbis") as String?)
    ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
    ?.takeIf { it.isNotEmpty() }
    ?: listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

// Android ABI → Rust/cargo-ndk target triple. Used to locate the Go
// libgoreality.so (REALITY transport) that the crate's build.rs drops in
// target/<triple>/release/ so it can ship next to the app's own .so.
val abiToRustTriple = mapOf(
    "arm64-v8a" to "aarch64-linux-android",
    "armeabi-v7a" to "armv7-linux-androideabi",
    "x86_64" to "x86_64-linux-android",
    "x86" to "i686-linux-android",
)

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

// Stamp file recording which submodule commit the jniLibs were built from,
// so packaging can prove the .so matches the checked-out native code.
val nativeStamp = jniLibsDir.file(".native-commit")

fun nativeHeadSha(): String {
    val proc = ProcessBuilder("git", "-C", nativeRoot.asFile.absolutePath, "rev-parse", "HEAD")
        .redirectErrorStream(true)
        .start()
    val out = proc.inputStream.bufferedReader().readText().trim()
    if (proc.waitFor() != 0) {
        throw GradleException("cannot resolve native submodule commit: $out")
    }
    return out
}

val buildRustNative by tasks.registering(Exec::class) {
    group = "native"
    description = "Compile twocha-mobile to per-ABI .so via cargo-ndk"

    workingDir = nativeRoot.asFile
    // --features reality links the Go xtls/reality core per ABI (cargo-ndk exports
    // the NDK clang as CC_<target>, which the crate's build.rs forwards to cgo).
    commandLine = listOf("cargo", "ndk") +
        nativeAbis.flatMap { listOf("-t", it) } +
        listOf("-o", jniLibsDir.asFile.absolutePath, "build", "--release", "-p", "twocha-mobile", "--features", "reality")

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
        // Invalidate the stamp up front: a failed/partial build must not
        // leave a stamp claiming the previous libs are current.
        nativeStamp.asFile.delete()
    }

    doLast {
        // Ship the Go xtls/reality shared lib (emitted by twocha-lib's build.rs
        // into target/<triple>/release/) next to the app's .so in each ABI dir;
        // the app's libtwocha_mobile.so has a DT_NEEDED on it.
        nativeAbis.forEach { abi ->
            val triple = abiToRustTriple[abi]
                ?: throw GradleException("no Rust triple mapping for ABI '$abi'")
            val src = nativeRoot.file("target/$triple/release/libgoreality.so").asFile
            if (!src.exists()) {
                throw GradleException("libgoreality.so not found for $abi at $src")
            }
            val dst = jniLibsDir.dir(abi).file("libgoreality.so").asFile
            dst.parentFile.mkdirs()
            src.copyTo(dst, overwrite = true)
        }
        nativeStamp.asFile.writeText(nativeHeadSha())
    }
}

val generateUniffiBindings by tasks.registering(Exec::class) {
    group = "native"
    description = "Generate Kotlin FFI bindings from the built .so"
    dependsOn(buildRustNative)

    workingDir = nativeRoot.asFile
    // Library mode reads metadata from any one ABI's .so; use the first
    // configured ABI so restricted (-PnativeAbis=...) builds keep working.
    val lib = jniLibsDir.file("${nativeAbis.first()}/libtwocha_mobile.so").asFile
    // `--no-format` skips uniffi's optional ktlint pass (not on PATH in the Nix
    // dev shell); the generated Kotlin is already well-formed. The cdylib keeps
    // its symbol table via the `twocha-mobile` release profile override, which
    // uniffi's library-mode reader needs to find the `UNIFFI_META_*` records.
    commandLine = listOf(
        "cargo", "run", "--release", "-p", "twocha-mobile", "--bin", "uniffi-bindgen",
        "--", "generate", "--no-format", "--library", lib.absolutePath,
        "--language", "kotlin", "--out-dir", bindingsDir.asFile.absolutePath)

    doFirst { bindingsDir.asFile.mkdirs() }
}

// Guard against packaging stale native libs: every configured ABI's .so must
// exist and the stamp written by buildRustNative must match the submodule
// commit that is actually checked out. Catches a restored-but-outdated cache,
// a partial rebuild, or any future skip-the-native-build shortcut.
val verifyNativeArtifacts by tasks.registering {
    group = "native"
    description = "Fail the build if jniLibs don't match the native submodule commit"
    dependsOn(generateUniffiBindings)

    doLast {
        val expected = nativeHeadSha()
        val stampFile = nativeStamp.asFile
        if (!stampFile.exists()) {
            throw GradleException(
                "jniLibs have no build stamp (${stampFile}); the native build " +
                    "did not complete — refusing to package possibly stale libs.")
        }
        val actual = stampFile.readText().trim()
        if (actual != expected) {
            throw GradleException(
                "stale native libs: jniLibs were built from $actual but the " +
                    "native/2cha submodule is at $expected. Re-run " +
                    ":app:buildRustNative before packaging.")
        }
        for (abi in nativeAbis) {
            val so = jniLibsDir.file("$abi/libtwocha_mobile.so").asFile
            if (!so.exists()) {
                throw GradleException("missing native lib for ABI $abi: $so")
            }
        }
    }
}

// Every Android build first produces the native artifacts + bindings, then
// proves they belong to the checked-out submodule commit.
tasks.named("preBuild") {
    dependsOn(verifyNativeArtifacts)
}
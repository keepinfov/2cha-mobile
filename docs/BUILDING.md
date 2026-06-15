# Building & running 2cha-mobile

The Android app is a thin Compose + `VpnService` shell around the **shared Rust
v4 engine**. It does not reimplement the protocol: the `2cha` workspace is
vendored as a git submodule under `native/2cha`, compiled to a per-ABI
`.so` via `cargo-ndk`, and exposed to Kotlin through `uniffi`-generated
bindings. The build produces all of this automatically — a plain
`./gradlew assembleDebug` runs the native steps first, then the normal Android
build.

```
Compose UI / VpnService (Kotlin)
        │  uniffi.twocha_mobile.*  (generated bindings)
        ▼
libtwocha_mobile.so  ──  twocha-mobile (uniffi)  ──  twocha-lib / twocha-core
   (per ABI)                                          Noise_IK + obfs transport
```

The app owns the **data plane** (tunnel fd, addresses, routes, DNS, MTU via
`VpnService.Builder`); the engine owns the **protocol** (Noise_IK handshake,
the obfuscation transport, PFS rekeys, packet pump).

---

## 1. Prerequisites

You need the Android SDK + NDK, a JDK 21 toolchain, the Rust toolchain with the
four Android targets, and `cargo-ndk`. There are two ways to get them.

### Option A — Nix (recommended)

The repo ships a flake that pins the entire toolchain. With
[Nix](https://nixos.org/download) (flakes enabled):

```sh
nix develop          # enter the dev shell (first run downloads the SDK/NDK, multi-GB)
```

or, with [direnv](https://direnv.net/):

```sh
direnv allow         # auto-enters the shell on cd, using the bundled .envrc
```

The shell exports `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_NDK_HOME`, and puts
`cargo`, `cargo-ndk`, `kotlinc` and `gradle`'s Java on `PATH`. It provides the
`aarch64`, `armv7`, `i686`, and `x86_64` `-linux-android` Rust std targets.

### Option B — manual

- Android SDK (platforms 35/36, build-tools 35/36) + an NDK (r27 / 29.x).
- JDK 21.
- `rustup` with: `aarch64-linux-android armv7-linux-androideabi
  i686-linux-android x86_64-linux-android`.
- `cargo install cargo-ndk`.
- Export `ANDROID_NDK_HOME` (or `ANDROID_NDK_ROOT`). If unset, the Gradle build
  falls back to the newest NDK under the configured Android SDK.

---

## 2. Get the source (with the submodule)

The native engine lives in a submodule, so a plain clone is not enough:

```sh
git clone git@github.com:keepinfov/2cha-mobile.git
cd 2cha-mobile
git submodule update --init --recursive
```

If you already cloned without `--recursive`, just run that last command. The
submodule is pinned to a specific `2cha` `master` commit; `git submodule update`
checks out exactly that commit.

---

## 3. Build the APK

From inside the dev shell (Option A) or a shell with the manual toolchain
(Option B):

```sh
./gradlew assembleDebug
```

What happens, in order (see the `native` tasks in `app/build.gradle.kts`):

1. **`buildRustNative`** — `cargo ndk -t <abi> … build --release -p twocha-mobile`
   for each ABI, dropping `libtwocha_mobile.so` into
   `app/src/main/jniLibs/<abi>/`.
2. **`generateUniffiBindings`** — runs the crate's `uniffi-bindgen` against the
   arm64 `.so` and writes the Kotlin bindings into `app/src/main/java`
   (`uniffi/twocha_mobile/…`).
3. The normal Android build compiles the Kotlin (now able to resolve
   `uniffi.twocha_mobile.*`) and packages the APK.

`jniLibs/` and the generated `uniffi/` bindings are **git-ignored** — they are
build artifacts, regenerated every build.

Outputs (ABI-split + a universal APK) land in:

```
app/build/outputs/apk/debug/
```

Other targets:

```sh
./gradlew assembleRelease        # minified/shrunk release build
./gradlew :app:buildRustNative   # native .so only
```

---

## 4. Install & run

```sh
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

(Use the ABI-specific APK if you prefer; `arm64-v8a` for most modern phones,
`x86_64` for an emulator.)

`minSdk` is 29, `targetSdk` is 36.

---

## 5. Server side: authorize the device

The server keeps a peer whitelist; handshakes from unknown keys are dropped. On
the server, using the `2cha` CLI:

```sh
# one-time: generate the server identity if you don't have one
2cha genkey server.key          # prints the server PUBLIC key — you paste this into the app

# authorize the phone (its public key comes from the app, step 6)
2cha peer add <device-public-key> --name phone
2cha peer list
```

In the server config, `[server] transport` (`quic` or `tls`) **must match** the
client's transport, and the cipher must be one both sides support.

---

## 6. Configure the app

Open the app → **Config**:

1. **Server address** — `host:port` (default port `51820` if omitted).
2. **Transport** — `QUIC` (default, obfuscated UDP) or `TLS` (over TCP). Must
   match the server.
3. **Cipher** — `ChaCha20-Poly1305` (default) or `AES-256-GCM`.
4. **Server public key** — the base64 key printed by `2cha genkey` on the
   server.
5. **Device public key** — generated on first run and shown read-only with a
   copy button. Paste it into the server's peer list (step 5). The matching
   private key is generated on-device and stored in
   `EncryptedSharedPreferences` (Android Keystore-backed); it never leaves the
   device and is never written to the config. Use the regenerate button to roll
   it (you must re-authorize the new public key on the server).

Advanced (optional): MTU (default 1420), IPv4/IPv6 addressing, `route_all`,
custom routes/excludes, DNS servers — these drive the `VpnService.Builder`, not
the engine.

Tap **Connect**. The app builds the tunnel interface, hands the engine the tun
fd plus a `protect(fd)` callback (so the engine's carrier sockets bypass the VPN
route), and the engine completes the Noise_IK handshake and starts pumping
packets.

---

## 7. Troubleshooting

- **`Native crate not found at native/2cha/Cargo.toml`** — the submodule isn't
  checked out. Run `git submodule update --init --recursive`.
- **`Android NDK not found`** — outside the Nix shell, export
  `ANDROID_NDK_HOME`, or install an NDK via the SDK manager so the fallback can
  find it.
- **Unresolved `uniffi.twocha_mobile.*` in the IDE** — the bindings are
  generated at build time. Run `./gradlew :app:generateUniffiBindings` once, then
  re-sync.
- **Connects but no traffic** — confirm `protect(fd)` is succeeding (check
  logcat, tag `twocha`), that the device public key is in the server's peer
  list, and that client/server `transport` and cipher match.
- **Logs** — the engine logs to logcat under tag `twocha` (see
  `init_logging()`).

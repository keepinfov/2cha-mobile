{
  description = "2cha Android app — Rust (uniffi) + Android NDK dev shell";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    fenix = {
      url = "github:nix-community/fenix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, fenix }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" "x86_64-darwin" ];
      forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f system);
    in
    {
      devShells = forAllSystems (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config.allowUnfree = true;
            config.android_sdk.accept_license = true;
          };

          # Rust toolchain + the Android std targets the native lib is built for.
          rustToolchain = fenix.packages.${system}.combine [
            fenix.packages.${system}.stable.toolchain
            fenix.packages.${system}.targets.aarch64-linux-android.stable.rust-std
            fenix.packages.${system}.targets.armv7-linux-androideabi.stable.rust-std
            fenix.packages.${system}.targets.i686-linux-android.stable.rust-std
            fenix.packages.${system}.targets.x86_64-linux-android.stable.rust-std
          ];

          androidComposition = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "35" "36" ];
            buildToolsVersions = [ "35.0.0" "36.0.0" ];
            includeNDK = true;
            includeEmulator = false;
          };
          androidSdk = androidComposition.androidsdk;
        in
        {
          default = pkgs.mkShell {
            nativeBuildInputs = with pkgs; [
              rustToolchain
              cargo-ndk        # cargo ndk -t <abi> build  (sets NDK linkers for you)
              go               # cgo core for the REALITY transport (native/goreality)
              pkg-config
              jdk21            # matches the app's Java/Kotlin toolchain (21)
              kotlin           # for the generated uniffi Kotlin bindings
              androidSdk
            ];

            shellHook = ''
              export JAVA_HOME="${pkgs.jdk21}"
              export ANDROID_HOME="${androidSdk}/libexec/android-sdk"
              export ANDROID_SDK_ROOT="$ANDROID_HOME"

              # cargo-ndk looks for ANDROID_NDK_HOME / ANDROID_NDK_ROOT
              export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$(ls -1 "$ANDROID_HOME/ndk" | head -1)"
              export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
              export NDK_HOME="$ANDROID_NDK_HOME"

              echo "2cha-mobile dev shell"
              echo "  Rust:   $(rustc --version)"
              echo "  NDK:    $ANDROID_NDK_HOME"
              echo "  ./gradlew assembleDebug   # builds native .so + uniffi bindings, then the APK"
            '';
          };
        });
    };
}

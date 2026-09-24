# SpinMe

**Background Gremlin Group — Creating Unique Tools for Unique Individuals**

SpinMe is a local-first Android image/GIF spinner with a companion web build. Its defining rule is that the source animation clock and the spin clock are independent: changing RPM never changes the source GIF timing.

## Download

[Download SpinMe v0.2.0 APK](releases/SpinMe-v0.2.0.apk)

- Package: `com.spinme.app`
- Version: `0.2.0` (versionCode 2)
- minSdk: 29
- targetSdk: 36
- APK path on `main`: [`releases/SpinMe-v0.2.0.apk`](releases/SpinMe-v0.2.0.apk)
- SHA-256: `4429878ccbe3427dd46a33ecaa9a6b60f9a8dab261fe67325c90271513befd2b`
- Publisher/creator: **Background Gremlin Group**
- Tagline: **Creating Unique Tools for Unique Individuals**

Release artifact verification is recorded in [releases/SHA256SUMS.txt](releases/SHA256SUMS.txt) and [releases/RELEASE-METADATA.txt](releases/RELEASE-METADATA.txt).

**Signing note:** v0.2.0 starts a new signing-key lineage. Because v0.1.0 used a different certificate, Android requires v0.1.0 to be uninstalled before installing v0.2.0. Keep the v0.2.0 release key for all future in-place upgrades from this release line.

## Android features

- GIF, PNG, JPEG, and WebP input through Android's document picker.
- Native animated GIF playback while the complete media layer spins.
- Source playback modes: Ping-pong (default), Loop, and Once.
- User-defined spin speed with no fixed RPM ceiling; the existing 0–3000 RPM slider remains for convenient adjustment, while exact RPM entry accepts higher values such as 20,000+ RPM.
- Linear ramp-up mode from 0 RPM to the selected target speed with an adjustable 0.5–60 second duration.
- Clockwise/counter-clockwise direction and spin pause/resume.
- Adjustable start angle, scale, pivot X/Y, and direct pivot dragging on the preview.
- Output sizes: 512, 720, 1080, and 1440 square.
- Animated export duration from 0.5 to 20 seconds.
- PNG, animated GIF, and MP4 export.
- GIF export up to 100 FPS; MP4 export up to 240 FPS.
- Dark and light presentation modes.
- Local processing only; imported media is not sent to a server.

## Timing model

`degreesPerSecond = rpm * 6`

Spin angle is calculated from elapsed time. Animated-source timing is calculated separately from the source duration and selected playback mode. Export samples both clocks at each output timestamp.

## Build

The Android app has no third-party runtime dependencies. Two build paths are included:

1. Standard Android Studio/Gradle project files.
2. [tools/build-apk.sh](tools/build-apk.sh), which can build directly with JDK + Android SDK Platform 36 / Build Tools 36.0.0.

The release signing keystore and passwords are intentionally **not** stored in this repository.

## Web

The `web/` project is the companion browser implementation.

`cd web && npm install && npm run dev`

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Development](docs/DEVELOPMENT.md)
- [Export behavior](docs/EXPORTS.md)
- [Branding and credits](docs/BRANDING.md)
- [Security](SECURITY.md)
- [Changelog](CHANGELOG.md)

## Credits

SpinMe is created and published by **Background Gremlin Group**.

**Creating Unique Tools for Unique Individuals.**

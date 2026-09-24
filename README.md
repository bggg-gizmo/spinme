<div align="center">

<img src="app/src/main/res/drawable-nodpi/ic_launcher_art.webp" width="160" alt="SpinMe spiral logo">

# SpinMe

**Independent image rotation for Android and the web.**

**Background Gremlin Group**  
*Creating Unique Tools for Unique Individuals*

![Version](https://img.shields.io/badge/version-0.2.0-2f81f7)
![Android](https://img.shields.io/badge/Android-API%2029%2B-3ddc84)
![Target SDK](https://img.shields.io/badge/targetSdk-36-6f42c1)
![Local First](https://img.shields.io/badge/processing-local--first-2ea043)
![Exports](https://img.shields.io/badge/export-PNG%20%7C%20GIF%20%7C%20MP4-d29922)

[Download APK](releases/SpinMe-v0.2.0.apk) · [Changelog](CHANGELOG.md) · [Architecture](docs/ARCHITECTURE.md) · [Build Guide](docs/DEVELOPMENT.md)

</div>

---

## Overview

SpinMe is a local-first image and animated-GIF spinner built around one non-negotiable timing rule:

> **The source animation clock and the rotation clock are independent.**

Changing RPM changes rotation speed only. It does not speed up, slow down, or otherwise alter the timing of an animated source. Preview and export use the same timing model so the rendered result tracks what you see in the app.

SpinMe includes a native Android application and a companion browser implementation.

## Get SpinMe

| Release | Package | Android | APK |
| --- | --- | --- | --- |
| **v0.2.0** | `com.spinme.app` | minSdk 29 · targetSdk 36 | **[Download SpinMe-v0.2.0.apk](releases/SpinMe-v0.2.0.apk)** |

**APK SHA-256**

```text
4429878ccbe3427dd46a33ecaa9a6b60f9a8dab261fe67325c90271513befd2b
```

Full verification data is kept in [`releases/SHA256SUMS.txt`](releases/SHA256SUMS.txt) and [`releases/RELEASE-METADATA.txt`](releases/RELEASE-METADATA.txt). Older release artifacts are archived under [`old_bulids/`](old_bulids/).

> [!IMPORTANT]
> **v0.2.0 begins a new Android signing-key lineage.** An installed v0.1.0 cannot be upgraded in place because it was signed with a different certificate. Uninstall v0.1.0 before installing v0.2.0. Future releases intended to upgrade v0.2.0 installations must use the v0.2.0 release key.

## Highlights

| Area | Capability |
| --- | --- |
| **Input** | GIF, PNG, JPEG, and WebP through Android's document picker |
| **Animated sources** | Native animated GIF playback with Ping-pong, Loop, and Once modes |
| **RPM** | Existing 0–3000 RPM slider plus uncapped exact RPM entry; 20,000+ RPM is supported |
| **Ramp-up** | Automatic linear ramp from 0 RPM to the selected target over 0.5–60 seconds |
| **Direction** | Clockwise and counter-clockwise rotation |
| **Spin state** | Pause/resume rotation without pausing the source animation |
| **Transform** | Start angle, scale, normalized pivot X/Y, and direct pivot dragging |
| **Output sizes** | 512, 720, 1080, and 1440 square |
| **Animated duration** | 0.5–20 seconds |
| **Export** | PNG, animated GIF, and H.264 MP4 |
| **Frame rates** | GIF up to 100 FPS; MP4 up to 240 FPS |
| **Appearance** | Dark carbon-fiber surfaces with gold inlays; light ivory carbon fiber with mother-of-pearl inlays |
| **Processing** | Imported media is processed locally rather than uploaded to a remote service |

## Timing model

Spin velocity is derived directly from elapsed time:

```text
degreesPerSecond = rpm × 6
```

The source-animation phase is calculated separately from the spin phase. Output FPS controls sampling density; it does **not** define angular velocity.

The core invariants are:

1. RPM never changes source-animation timing.
2. Pausing spin does not pause the animated source.
3. Direction changes affect rotation only.
4. Playback mode affects the source clock only.
5. Export begins from the current visual/source state.
6. Ramp progress is preserved in animated exports.
7. Pivot coordinates remain normalized to the output canvas.

For the implementation model, see [Architecture](docs/ARCHITECTURE.md) and [Export behavior](docs/EXPORTS.md).

## Export behavior

SpinMe separates motion from sampling so high frame rates improve temporal resolution without changing the requested RPM.

| Format | Behavior |
| --- | --- |
| **PNG** | Captures the current rendered frame |
| **Animated GIF** | Samples source and spin clocks independently at the selected GIF FPS |
| **MP4** | Samples the same state model into H.264 video at the selected MP4 FPS |

Ramp-up exports continue from the current ramp phase rather than restarting the ramp at frame zero.

## Build the Android app

The Android application has no third-party runtime dependencies.

### Requirements

- JDK 17+
- Android SDK Platform 36
- Android Build Tools 36.0.0

### Android Studio / Gradle

Open the repository in Android Studio and build the `app` module normally.

### Direct SDK build

A direct build path is also included for environments where Gradle is not desired:

```bash
ANDROID_SDK_ROOT=/path/to/sdk ./tools/build-apk.sh
```

For a signed build, pass signing material through environment variables:

```bash
SPINME_KEYSTORE=/secure/path/release.jks \
SPINME_KEY_ALIAS=your-alias \
SPINME_STOREPASS='...' \
SPINME_KEYPASS='...' \
ANDROID_SDK_ROOT=/path/to/sdk \
./tools/build-apk.sh
```

Release keys and passwords do not belong in the repository. See [Development](docs/DEVELOPMENT.md) for the complete build and signing notes.

## Verify the release

Verify the APK checksum:

```bash
sha256sum releases/SpinMe-v0.2.0.apk
```

Expected SHA-256:

```text
4429878ccbe3427dd46a33ecaa9a6b60f9a8dab261fe67325c90271513befd2b
```

Verify the signing certificate with Android Build Tools:

```bash
apksigner verify --verbose --print-certs releases/SpinMe-v0.2.0.apk
```

Expected signing-certificate SHA-256:

```text
bcfd9417869e6671e2f33efa7345cd49c0f4a9452d014f1d1fab67de2e166093
```

## Web companion

The browser implementation lives in [`web/`](web/) and follows the same independent-clock model.

```bash
cd web
npm install
npm run dev
```

## Repository map

```text
spinme/
├── app/                 Android application
├── web/                 Companion browser implementation
├── docs/                Architecture, development, export, and branding docs
├── releases/            Current signed release and verification metadata
├── old_bulids/          Archived previous release artifacts
├── tools/               Direct Android SDK build tooling
├── CHANGELOG.md         Release history
└── SECURITY.md          Vulnerability-reporting policy
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — clocks, state, transforms, and rendering model
- [Development](docs/DEVELOPMENT.md) — Android requirements, build paths, and signing
- [Export behavior](docs/EXPORTS.md) — PNG/GIF/MP4 timing and sampling behavior
- [Branding](docs/BRANDING.md) — visual identity and publisher information
- [Changelog](CHANGELOG.md) — release-by-release changes
- [Security policy](SECURITY.md) — vulnerability reporting and supported versions

## Security

SpinMe processes imported media locally. For vulnerability reports, use the process described in [SECURITY.md](SECURITY.md) rather than opening a public issue with sensitive details.

## Publisher

**Background Gremlin Group**  
*Creating Unique Tools for Unique Individuals.*


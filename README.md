<div align="center">

<img src="app/src/main/res/drawable-nodpi/ic_launcher_art.webp" width="160" alt="SpinMe spiral logo">

# SpinMe

**Independent image rotation for Android and the web.**

**Background Gremlin Group**  
*Creating Unique Tools for Unique Individuals*

![Version](https://img.shields.io/badge/version-1.0.0-2f81f7)
![Android](https://img.shields.io/badge/Android-API%2029%2B-3ddc84)
![Target SDK](https://img.shields.io/badge/targetSdk-36-6f42c1)
![Local First](https://img.shields.io/badge/processing-local--first-2ea043)
![Exports](https://img.shields.io/badge/export-PNG%20%7C%20GIF%20%7C%20MP4-d29922)

[Download APK](releases/SpinMe-v1.0.0.apk) · [Release verification](releases/README.md) · [Changelog](CHANGELOG.md) · [Architecture](docs/ARCHITECTURE.md) · [Build Guide](docs/DEVELOPMENT.md)

</div>

---

## Current public release

SpinMe **v1.0.0** is the current verified Android release on `main`.

| Item | Value |
| --- | --- |
| APK | **[SpinMe-v1.0.0.apk](releases/SpinMe-v1.0.0.apk)** |
| Package | `com.spinme.app` |
| Version | `1.0.0` |
| versionCode | `3` |
| minSdk | `29` |
| targetSdk | `36` |
| APK size | `45,800 bytes` |
| APK SHA-256 | `ec7b0d95d4a0c1ba0e7d9e5d70ce673efc28ca93c7a30624925556d0d4301389` |
| Git blob | `deb9f22b9b2c2a209593ee776eee68957408bf8d` |
| Signing certificate SHA-256 | `bcfd9417869e6671e2f33efa7345cd49c0f4a9452d014f1d1fab67de2e166093` |

The release artifact was rebuilt from the current Android source and verified for ZIP structure, zip alignment, Android package/version metadata, and APK Signature Scheme v3 before publication.

The malformed public **v0.2.0** APK was withdrawn and is retained only as a clearly labeled historical artifact under [`old_bulids/`](old_bulids/). It is not a supported release.

> [!IMPORTANT]
> v1.0.0 uses the same Android signing lineage introduced with v0.2.0. v0.1.0 used a different certificate, so a v0.1.0 installation must be uninstalled before installing v1.0.0. Future releases intended to upgrade v1.0.0 must use the same v1.0.0 release key.

## Overview

SpinMe is a local-first image and animated-GIF spinner built around one non-negotiable timing rule:

> **The source animation clock and the rotation clock are independent.**

Changing RPM changes rotation speed only. It does not speed up, slow down, or otherwise alter the timing of an animated source. Preview and export use the same timing model so rendered output tracks the visual state of the app.

SpinMe includes a native Android application and a companion browser implementation.

## Highlights

| Area | Capability |
| --- | --- |
| **Input** | GIF, PNG, JPEG, and WebP through Android's document picker |
| **Animated sources** | Native animated GIF playback with Ping-pong, Loop, and Once modes |
| **RPM** | 0–3000 RPM convenience slider plus uncapped exact RPM entry; 20,000+ RPM is supported |
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

Core invariants:

1. RPM never changes source-animation timing.
2. Pausing spin does not pause the animated source.
3. Direction changes affect rotation only.
4. Playback mode affects the source clock only.
5. Export begins from the current visual/source state.
6. Ramp progress is preserved in animated exports.
7. Pivot coordinates remain normalized to the output canvas.

See [Architecture](docs/ARCHITECTURE.md) and [Export behavior](docs/EXPORTS.md) for implementation details.

## Build the Android app

Requirements:

- JDK 17+
- Android SDK Platform 36
- Android Build Tools 36.0.0

Android Studio/Gradle users can build the `app` module normally.

A direct SDK build is also available:

```bash
ANDROID_SDK_ROOT=/path/to/sdk ./tools/build-apk.sh
```

For a signed build:

```bash
SPINME_KEYSTORE=/secure/path/release.jks \
SPINME_KEY_ALIAS=your-alias \
SPINME_STOREPASS='...' \
SPINME_KEYPASS='...' \
ANDROID_SDK_ROOT=/path/to/sdk \
./tools/build-apk.sh
```

The direct build script verifies the signed APK with `apksigner`, `zipalign -c`, and `unzip -t` before printing its SHA-256.

Release keys and passwords do not belong in the repository. See [Development](docs/DEVELOPMENT.md).

## Verify the public APK

```bash
sha256sum releases/SpinMe-v1.0.0.apk
```

Expected:

```text
ec7b0d95d4a0c1ba0e7d9e5d70ce673efc28ca93c7a30624925556d0d4301389  releases/SpinMe-v1.0.0.apk
```

Verify the Android signature:

```bash
apksigner verify --verbose --print-certs releases/SpinMe-v1.0.0.apk
```

Expected signer certificate SHA-256:

```text
bcfd9417869e6671e2f33efa7345cd49c0f4a9452d014f1d1fab67de2e166093
```

Also see [`releases/SHA256SUMS.txt`](releases/SHA256SUMS.txt) and [`releases/RELEASE-METADATA.txt`](releases/RELEASE-METADATA.txt).

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
├── releases/            Current verified signed release
├── old_bulids/          Historical/withdrawn release artifacts
├── tools/               Direct Android SDK build tooling
├── demo/                Demo documentation
├── CHANGELOG.md         Release history
└── SECURITY.md          Vulnerability-reporting policy
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Development and release process](docs/DEVELOPMENT.md)
- [Export behavior](docs/EXPORTS.md)
- [Branding](docs/BRANDING.md)
- [Current release verification](releases/README.md)
- [Historical artifacts](old_bulids/README.md)
- [Changelog](CHANGELOG.md)
- [Security policy](SECURITY.md)

## Security

SpinMe processes imported media locally. For vulnerability reports, follow [SECURITY.md](SECURITY.md) rather than opening a public issue containing sensitive details.

## Publisher

**Background Gremlin Group**  
*Creating Unique Tools for Unique Individuals.*

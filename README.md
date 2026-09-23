# SpinMe

SpinMe is a local-first image and GIF spinner for Android with a companion web build.

The defining behavior is simple: **source animation timing and spin timing are independent**. An animated GIF keeps its own frame delays while the entire image layer rotates from a separate continuous clock.

[Watch or download the 22-second demo](demo/SpinMe_Demo_22s.mp4)

## Highlights

- Import GIF, PNG, JPEG, and WebP.
- Live spinning preview with controls that update while the source keeps animating.
- Spin speed from 0 to 3000 RPM.
- Clockwise and counter-clockwise rotation.
- Pause/resume spin without pausing the source animation.
- Source playback modes: Ping-pong, Loop, and Once.
- Ping-pong is the default so one-shot GIFs remain continuously animated.
- Drag the pivot directly on the preview or use X/Y sliders.
- Independent scale, start angle, export duration, and output FPS controls.
- Dark theme: black carbon fiber with metallic gold inlays.
- Light theme: ivory carbon fiber with mother-of-pearl inlays.
- Full-bleed hypnotic spiral launcher/web icon with no added white padding.
- Local processing; media does not need to be uploaded to a server.

## Platform matrix

| Capability | Android | Web |
| --- | --- | --- |
| GIF / PNG / JPEG / WebP input | Yes | Yes |
| Native animated-source preview | Yes | Yes |
| Ping-pong / Loop / Once | Yes | Yes* |
| Live RPM control | Yes | Yes |
| Direct pivot dragging | Yes | Yes |
| Dark / light material themes | Yes | Yes |
| PNG export | Yes | Yes |
| Animated GIF export | Yes | Yes |
| MP4 export | Yes | No |
| WebM export | No | Yes |

\* Web ping-pong uses browser `ImageDecoder` when available. If deterministic frame decoding is unavailable, SpinMe falls back to the browser's native animated-image playback.

## Timing model

Rotation is derived from elapsed time, not frame count:

```text
degreesPerSecond = rpm * 6
angle(t) = startAngle + direction * degreesPerSecond * t
```

For source playback, SpinMe tracks a separate source clock. Ping-pong maps that clock onto a triangle wave:

```text
phase = sourceTime mod (2 * duration)

if phase < duration:
    sourcePosition = phase
else:
    sourcePosition = 2 * duration - phase
```

The renderer samples both clocks at the current timestamp:

```text
sourceFrame = source.frameAt(sourcePosition(t))
rotation    = spinAngle(t)
outputFrame = rotate(sourceFrame, rotation)
```

Changing RPM never changes GIF frame timing. Changing output FPS only changes how often the combined state is sampled for an export.

## Android

Current project configuration:

- Application ID: `com.spinme.app`
- minSdk: 29
- compileSdk: 36
- targetSdk: 36
- Java: 17
- Android Gradle Plugin: 9.1.1
- Gradle: 9.3.1
- Kotlin / Compose compiler plugin: 2.4.20
- Jetpack Compose BOM: 2026.09.00
- Media3: 1.11.1
- android-gif-drawable: 1.2.32

Build:

```bash
gradle :app:assembleDebug
```

Run timing tests:

```bash
gradle :app:testDebugUnitTest
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Web

Requirements: Node.js 22 and npm.

```bash
cd web
npm install
npm run dev
```

Production build:

```bash
npm run build
```

The built site is written to `web/dist/`.

## Repository layout

```text
app/                 Android application
web/                 Browser build
demo/                Product demo video and notes
docs/                Architecture, development, export, and branding documentation
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Development](docs/DEVELOPMENT.md)
- [Export behavior](docs/EXPORTS.md)
- [Branding and artwork](docs/BRANDING.md)
- [Demo video](demo/README.md)

## Security

Please do not disclose suspected vulnerabilities in a public issue. Use GitHub private vulnerability reporting for this repository once enabled, or follow the instructions in [SECURITY.md](SECURITY.md).

## Release

Current public release: **v0.1.0**.
See [CHANGELOG.md](CHANGELOG.md) for release notes.

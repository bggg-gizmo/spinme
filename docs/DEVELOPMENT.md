# Development

**Publisher:** Background Gremlin Group  
**Tagline:** Creating Unique Tools for Unique Individuals

## Android requirements

- JDK 17+
- Android SDK Platform 36
- Android Build Tools 36.0.0

The Android runtime has no third-party library dependencies.

## Gradle / Android Studio

Open the repository in Android Studio or use the included Gradle wrapper.

The Android module is `app/`, package `com.spinme.app`, minSdk 29, targetSdk 36, version 0.2.0.

## Direct SDK build

`tools/build-apk.sh` builds the same Java/framework implementation directly with `aapt`, `javac`, `d8`, `zipalign`, and `apksigner`.

For an unsigned build:

```bash
ANDROID_SDK_ROOT=/path/to/sdk ./tools/build-apk.sh
```

For signing, provide the signing material only through environment variables:

```bash
SPINME_KEYSTORE=/secure/path/release.jks \
SPINME_KEY_ALIAS=your-alias \
SPINME_STOREPASS='...' \
SPINME_KEYPASS='...' \
ANDROID_SDK_ROOT=/path/to/sdk \
./tools/build-apk.sh
```

Never commit release keys or passwords.

## Core invariants

1. Source animation timing is independent of RPM.
2. Preview/output FPS does not define spin velocity.
3. Pausing spin does not pause source animation.
4. Direction changes affect rotation only.
5. Export samples the same source/spin state model as preview.
6. Pivot coordinates remain normalized to the output canvas.
7. Exact RPM values are not capped at 3000; manual RPM input may exceed 20,000 RPM.
8. Ramp-up pauses with spin pause and export preserves the current ramp phase.
9. Dark mode uses visible carbon-fiber surfaces with metallic-gold inlays; light mode uses white/ivory carbon fiber with mother-of-pearl inlays.

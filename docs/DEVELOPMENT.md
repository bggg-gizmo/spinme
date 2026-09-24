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

### Release signing lineage

SpinMe v0.2.0 begins a new Android signing lineage. Its certificate SHA-256 is `bcfd9417869e6671e2f33efa7345cd49c0f4a9452d014f1d1fab67de2e166093`. Preserve that key outside the repository and use it for future releases that must upgrade v0.2.0 installations. Because v0.1.0 used a different signing certificate, it cannot be upgraded in place to v0.2.0; the older installation must be removed first.

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

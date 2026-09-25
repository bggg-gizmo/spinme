# Development and release process

**Publisher:** Background Gremlin Group  
**Tagline:** Creating Unique Tools for Unique Individuals  
**Current release:** SpinMe v1.0.0 (`versionCode 3`)

## Android requirements

- JDK 17+
- Android SDK Platform 36
- Android Build Tools 36.0.0

The Android runtime has no third-party library dependencies.

## Android Studio / Gradle

Open the repository in Android Studio or use the Gradle project.

The Android module is `app/`:

```text
applicationId = com.spinme.app
minSdk        = 29
targetSdk     = 36
versionCode   = 3
versionName   = 1.0.0
```

## Direct SDK build

`tools/build-apk.sh` builds the Java/framework implementation directly with `aapt`, `javac`, `d8`, `zipalign`, and `apksigner`.

Unsigned build:

```bash
ANDROID_SDK_ROOT=/path/to/sdk ./tools/build-apk.sh
```

Signed build:

```bash
SPINME_KEYSTORE=/secure/path/release.jks \
SPINME_KEY_ALIAS=your-alias \
SPINME_STOREPASS='...' \
SPINME_KEYPASS='...' \
ANDROID_SDK_ROOT=/path/to/sdk \
./tools/build-apk.sh
```

Never commit release keys, passwords, or exported private-key material.

## Mandatory release validation

A signed public APK is not considered releasable merely because signing succeeds.

The direct build path performs:

1. `apksigner verify --verbose --print-certs`
2. `zipalign -c -p 4`
3. `unzip -t`
4. SHA-256 generation

Before publishing, also verify package/version metadata with Android Build Tools and confirm the resulting values match the source configuration.

For v1.0.0 the expected release identity is:

```text
APK:         releases/SpinMe-v1.0.0.apk
Package:     com.spinme.app
Version:     1.0.0
VersionCode: 3
minSdk:      29
targetSdk:   36
SHA-256:     ec7b0d95d4a0c1ba0e7d9e5d70ce673efc28ca93c7a30624925556d0d4301389
```

Expected signer certificate SHA-256:

```text
bcfd9417869e6671e2f33efa7345cd49c0f4a9452d014f1d1fab67de2e166093
```

## Release signing lineage

v1.0.0 uses the signing lineage introduced with v0.2.0.

v0.1.0 used a different certificate, so Android cannot upgrade a v0.1.0 installation directly to v1.0.0. Users coming from v0.1.0 must uninstall that version first.

Preserve the current release key outside the repository. Future releases intended to upgrade v1.0.0 installations must use that same signing identity.

## Release publication checklist

Before changing the public release pointer/documentation:

- build from the intended source revision;
- verify `versionName` and `versionCode`;
- verify ZIP integrity;
- verify zip alignment;
- verify APK signature and certificate fingerprint;
- verify package, minSdk, and targetSdk;
- calculate the final APK SHA-256;
- publish the exact verified binary to `releases/`;
- read the published artifact back from GitHub and verify its path/blob/size;
- update `releases/SHA256SUMS.txt`;
- update `releases/RELEASE-METADATA.txt`;
- update root/release documentation;
- quarantine withdrawn/broken historical artifacts under `old_bulids/`;
- remove temporary staging material.

Do not publish a checksum or metadata record for a different binary than the one actually stored under `releases/`.

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

## Public repository hygiene

The current public artifact belongs under `releases/`. Withdrawn or superseded artifacts belong under the deliberately named historical directory `old_bulids/`.

Temporary transfer/staging chunks must not remain in `main` after publication.

SpinMe does not use GitHub Actions for its release process.


## Web development

The browser companion is versioned **1.0.0**.

Requirements:

- a current Node.js runtime;
- npm;
- a modern browser for development/preview.

Install and validate:

```bash
cd web
npm install
npm run check
npm run build
```

Run the development server:

```bash
npm run dev
```

Preview the production bundle:

```bash
npm run preview
```

`npm run check` performs JavaScript syntax validation. `npm run build` performs the Vite production build.

The generated `web/dist/` directory and `web/node_modules/` are intentionally ignored and must not be committed as release source.

### Web release checks

Before publishing a web-version change:

- ensure `web/package.json` has the intended version;
- ensure `web/public/version.json` matches it;
- ensure visible web release identity matches it;
- parse `site.webmanifest`, `version.json`, and `package.json` as valid JSON;
- run JavaScript syntax validation;
- run the Vite production build;
- verify `web/public/icon.webp` and `favicon.webp` use the canonical launcher artwork;
- verify exact RPM, ramp, pause, direction, start angle, playback modes, pivot, scale, and export behavior;
- verify preview/export transform parity;
- verify controls cannot mutate render state during an active export;
- verify no temporary build/staging files remain in source control.

The browser implementation processes imported media locally. It does not upload source images to a SpinMe service.

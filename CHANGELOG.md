# Changelog

## v1.0.0 — Verified public release

- Rebuilt and republished the Android application as `versionName 1.0.0`, `versionCode 3`.
- Published the verified signed APK at `releases/SpinMe-v1.0.0.apk`.
- APK SHA-256: `ec7b0d95d4a0c1ba0e7d9e5d70ce673efc28ca93c7a30624925556d0d4301389`.
- Signing certificate SHA-256: `bcfd9417869e6671e2f33efa7345cd49c0f4a9452d014f1d1fab67de2e166093`.
- Verified ZIP structure, zip alignment, package identity, SDK levels, version metadata, and APK Signature Scheme v3 before publication.
- Added mandatory `apksigner`, `zipalign -c`, and `unzip -t` checks to the direct release build path.
- Preserved uncapped exact RPM, adjustable ramp-up, independent source/spin clocks, playback modes, direction, pause, pivot, scale, start angle, theme switching, PNG/GIF/MP4 export, and the web companion.
- Brought the browser companion to the same v1.0.0 release line.
- Updated web favicon/PWA icon to the canonical full-bleed launcher artwork.
- Fixed stale literal `\\n` metadata artifacts in `web/index.html`.
- Added web start-angle control and explicit `WEB v1.0.0` release identity.
- Corrected web export scaling so preview and export use the same contain-and-scale transform model.
- Preserved spin/playback state when replacing source media instead of resetting unrelated controls.
- Prevented control mutation during an active web export so a render uses a stable state snapshot.
- Withdrew the malformed v0.2.0 public APK from the current release location and quarantined it under `old_bulids/`.
- v1.0.0 continues the signing lineage introduced with v0.2.0. v0.1.0 used a different certificate.

## v0.2.0 — Uncapped spin, ramp-up, and material polish — withdrawn binary

- Added exact RPM entry without a fixed 3000 RPM ceiling.
- Added automatic linear ramp-up from 0 RPM to the selected target over an adjustable 0.5–60 second duration.
- Ramp progress pauses with spin pause and is preserved in animated exports.
- Added carbon-fiber surfaces and gold/mother-of-pearl inlay accents.
- Launcher resources use full-bleed spiral artwork.
- Introduced the signing lineage that is retained by v1.0.0.
- Preserved existing import, playback, pivot, scale, theme, PNG, GIF, MP4, and web export features.
- The public v0.2.0 APK artifact was later found malformed and is **withdrawn/unsupported**. It remains archived only for provenance.

## v0.1.0 — Initial public release

Published by **Background Gremlin Group — Creating Unique Tools for Unique Individuals**.

- Local-first Android image/GIF spinner plus companion web build.
- Independent source-animation and rotation clocks.
- Ping-pong, Loop, and Once source playback.
- Live 0–3000 RPM controls, direction, spin pause, scale, start angle, and draggable pivot.
- PNG, animated GIF, and H.264 MP4 export on Android.
- Used the original v0.1.0 signing certificate, which differs from the current release lineage.

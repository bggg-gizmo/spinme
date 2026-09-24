# Changelog

## v0.2.0 — Uncapped spin, ramp-up, and material polish

- Exact RPM entry no longer has a fixed 3000 RPM ceiling.
- Added automatic linear ramp-up from 0 RPM to the selected target over an adjustable 0.5–60 second duration.
- Ramp progress pauses with spin pause and is preserved in animated exports.
- Carbon-fiber surfaces and gold/mother-of-pearl inlay accents are rendered directly in the existing UI surfaces.
- Launcher resources use the full-bleed spiral artwork without a white backing cell or baked-in padding.
- v0.2.0 starts a new Android signing-key lineage; v0.1.0 must be uninstalled before installing v0.2.0 because the package signatures differ.
- Fixed public web metadata files containing literal `\\n` sequences.
- Preserved all existing import, playback, pivot, scale, theme, PNG, GIF, MP4, and web export features.


## v0.1.0 — Initial public release

Published by **Background Gremlin Group — Creating Unique Tools for Unique Individuals**.

- Local-first Android image/GIF spinner plus companion web build.
- Independent source-animation and rotation clocks.
- Ping-pong, Loop, and Once source playback.
- Live 0–3000 RPM controls, direction, spin pause, scale, start angle, and draggable pivot.
- PNG, animated GIF, and H.264 MP4 export on Android.
- Public source and signed Android APK.

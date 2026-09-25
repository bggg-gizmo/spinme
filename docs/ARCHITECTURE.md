# Architecture

SpinMe is created and published by **Background Gremlin Group — Creating Unique Tools for Unique Individuals**.

Current public release: **v1.0.0** (`versionCode 3`).

The Android app intentionally uses Android framework APIs only. This keeps the release self-contained and avoids third-party runtime dependencies.

## Independent clocks

SpinMe has two independent clocks:

- **Source clock** — determines which frame of an animated source should be visible.
- **Spin clock** — determines the current rotation angle.

The source clock never derives from RPM or output FPS.

### Source playback

For a source duration `D` and elapsed source time `t`:

- Ping-pong: map `t` onto a triangle wave over `2D`.
- Loop: `t mod D`.
- Once: clamp to the final source frame.

### Spin

```text
degreesPerSecond = rpm × 6
angle(t) = startAngle + direction × degreesPerSecond × elapsedSeconds
```

Changing RPM or direction rebases the spin clock at the current visual angle so controls do not introduce discontinuities.

Exact RPM is intentionally uncapped. The 0–3000 RPM slider is a convenience control, not a model constraint.

### Ramp-up clock

Ramp-up is a linear angular-velocity transition from 0 RPM to the selected target RPM over 0.5–60 seconds.

Ramp time advances only while the spin clock is running. Pausing spin freezes both angle and ramp progress while the independent source animation continues.

For target RPM `R`, ramp duration `D`, and ramp time `t`:

```text
rpm(t) = R × t / D
```

The renderer integrates that velocity over elapsed time rather than approximating the ramp by changing frame count. Export snapshots include the current ramp phase, so GIF and MP4 continue the same motion model from the current visual state.

## Android implementation

- `MainActivity.java` builds the existing control surface and handles document import/export commands.
- `SpinView.java` renders static media or GIF frames, applies rotation/scale/pivot, and maintains the independent clocks.
- `Exporter.java` renders snapshots for PNG/GIF/MP4 output.
- `GifEncoder.java` produces animated GIF output without an external encoder dependency.
- Android `MediaCodec` + `MediaMuxer` produce H.264 MP4 output.

The app operates on local bytes/URIs and writes exports through MediaStore.

## Preview/export state contract

Preview and export share the same conceptual state:

- source media and source playback mode;
- source playback phase;
- current spin angle;
- target RPM and direction;
- ramp active/inactive state and current ramp phase;
- scale;
- normalized pivot X/Y;
- start angle.

Output FPS changes sampling density only. It does not change source timing or angular velocity.

## Release artifact boundary

Source code is authoritative for implementation. The signed APK under `releases/` is a derived release artifact.

For v1.0.0 the public APK is:

`releases/SpinMe-v1.0.0.apk`

Release publication requires successful ZIP-integrity, zipalign, package/version, and APK-signature verification. See [Development](DEVELOPMENT.md) and [release metadata](../releases/README.md).


## Web implementation

The `web/` companion is versioned 1.0.0 and mirrors the same behavioral model using browser APIs.

- `web/src/main.js` owns source playback, independent spin timing, ramp integration, preview state, and browser export.
- `ImageDecoder` is used when available to obtain deterministic animated GIF/WebP frames for Ping-pong, Loop, Once, and sampled export.
- Static/native-image fallback remains available when decoded animation support is unavailable.
- `gifenc` produces animated GIF output locally in the browser.
- `MediaRecorder` + `canvas.captureStream()` produce WebM when supported by the browser.
- PNG uses the browser canvas encoder.

Preview and web export use the same contain-and-scale transform:

```text
scale = min(outputWidth / sourceWidth, outputHeight / sourceHeight) × userScale
```

Rotation uses the same normalized output-canvas pivot as preview. Export controls are locked while an export is running so RPM, direction, playback mode, pivot, scale, theme, duration, FPS, or format cannot change mid-render.

Loading replacement media resets the source clock for the new media but preserves unrelated spin controls and current spin state.

Web release identity is published in `web/public/version.json`, while installable/PWA metadata is in `web/public/site.webmanifest`.

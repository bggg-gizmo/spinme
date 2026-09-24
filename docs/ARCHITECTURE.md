# Architecture

SpinMe is created and published by **Background Gremlin Group — Creating Unique Tools for Unique Individuals**.

The Android app intentionally uses Android framework APIs only. This keeps the release self-contained and avoids third-party runtime dependencies.

## Independent clocks

SpinMe has two clocks:

- **Source clock** — determines which frame of an animated source should be visible.
- **Spin clock** — determines the current rotation angle.

The source clock never derives from RPM or output FPS.

### Source playback

For a source duration `D` and elapsed source time `t`:

- Ping-pong: map `t` onto a triangle wave over `2D`.
- Loop: `t mod D`.
- Once: clamp to the last source frame.

### Spin

`degreesPerSecond = rpm * 6`

`angle(t) = startAngle + direction * degreesPerSecond * elapsedSeconds`

Changing RPM or direction rebases the spin clock at the current visual angle so controls do not introduce discontinuities.

## Android implementation

- `MainActivity.java` builds the control surface and handles document import/export commands.
- `SpinView.java` renders static media or GIF frames, applies rotation/scale/pivot, and maintains the independent clocks.
- `Exporter.java` renders snapshots for PNG/GIF/MP4 output.
- `GifEncoder.java` produces animated GIF output without an external encoder dependency.
- Android `MediaCodec` + `MediaMuxer` produce H.264 MP4 output.

The app operates on local bytes/URIs and writes exports through MediaStore.

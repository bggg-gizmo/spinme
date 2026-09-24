# Export behavior

SpinMe is created by **Background Gremlin Group — Creating Unique Tools for Unique Individuals**.

Current public release: **v1.0.0**.

Export uses the same source/spin state model as preview. Output FPS is a sampling parameter only and never controls source-animation timing or requested angular velocity.

## PNG

PNG exports a single rendered frame using the current source state, rotation angle, scale, and pivot. Transparency is preserved where the source/render permits it.

## GIF

Animated GIF export samples the independent source and spin clocks at each output timestamp.

- Output FPS is capped at 100 FPS.
- Changing output FPS does not retime the input animation.
- If ramp-up is active, export captures the current ramp phase and continues the linear acceleration model before holding the selected target RPM.
- Export begins from the current visual/source state rather than resetting playback.

## MP4

MP4 export uses Android `MediaCodec` (H.264/AVC) and `MediaMuxer`.

- Output FPS is capped at 240 FPS.
- Frames are rendered locally and converted for H.264 encoding.
- The encoded temporary MP4 is published through MediaStore.
- Active ramp-up state is sampled from the current phase exactly like GIF export.

## Output locations

- PNG/GIF: `Pictures/SpinMe`
- MP4: `Movies/SpinMe`

All export processing is local to the device.

## Release invariant

A release change must not silently create a second timing implementation for export. Preview and exported media must continue to derive source phase, spin angle, direction, ramp state, scale, and normalized pivot from the same state model.

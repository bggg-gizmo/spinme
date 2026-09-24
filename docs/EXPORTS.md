# Export behavior

SpinMe is created by **Background Gremlin Group — Creating Unique Tools for Unique Individuals**.

## PNG

PNG exports a single rendered frame using the current source state, rotation angle, scale, and pivot. Transparency is preserved where the source/render permits it.

## GIF

Animated GIF export samples the independent source and spin clocks for each output timestamp. Output FPS is capped at 100 FPS. Changing output FPS does not retime the input animation.

## MP4

MP4 export uses Android `MediaCodec` (H.264/AVC) and `MediaMuxer`. Output FPS is capped at 240 FPS. Frames are rendered locally, converted to YUV420, encoded, and written to a temporary MP4 before being published through MediaStore.

## Output locations

- PNG/GIF: `Pictures/SpinMe`
- MP4: `Movies/SpinMe`

All export processing is local to the device.

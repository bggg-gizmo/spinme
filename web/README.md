# SpinMe web companion

Current web version: **1.0.0**

The browser implementation follows the same core timing contract as the Android release:

- source animation and spin clocks are independent;
- RPM changes rotation only;
- pause freezes spin/ramp progression while source playback continues;
- direction changes rotation only;
- Ping-pong, Loop, and Once affect source playback only;
- export begins from the current visual/source phase;
- output FPS controls sampling only;
- pivot coordinates are normalized;
- preview and export use the same contain/scale transform model.

## Features

- GIF, PNG, JPEG, and WebP input by picker, drag/drop, or paste.
- Uncapped exact RPM input plus the existing convenience slider/presets.
- Linear 0 → target RPM ramp over 0.5–60 seconds.
- Clockwise/counter-clockwise spin and spin-only pause.
- Start angle, scale, normalized pivot X/Y, and direct pivot dragging.
- Dark carbon/gold and light ivory/mother-of-pearl presentation.
- PNG, animated GIF, and WebM export.
- GIF sampling up to 100 FPS; WebM capture up to 240 FPS.
- Local browser processing; imported media is not uploaded by SpinMe.

## Development

```bash
cd web
npm install
npm run check
npm run build
npm run dev
```

The production build is written to `web/dist/`, which is intentionally ignored by Git.

## Browser capability notes

Decoded Ping-pong/Loop/Once animation playback uses the browser `ImageDecoder` API when available. If animated frame decoding is unavailable, SpinMe falls back to native browser image playback and reports that limitation in the interface.

WebM export uses `MediaRecorder` and `canvas.captureStream()`; codec availability depends on the browser. PNG and GIF export are generated directly in-browser.

## Release identity

- Application: `SpinMe`
- Web version: `1.0.0`
- Publisher: `Background Gremlin Group`
- Tagline: `Creating Unique Tools for Unique Individuals`
- Manifest: `public/site.webmanifest`
- Release metadata: `public/version.json`

The web icon and favicon use the same canonical full-bleed spiral artwork as the Android launcher.

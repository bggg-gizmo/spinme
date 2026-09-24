import "./style.css";
import { GIFEncoder, quantize, applyPalette } from "gifenc";

const $ = (id) => document.getElementById(id);

const el = {
  openBtn: $("openBtn"),
  emptyOpenBtn: $("emptyOpenBtn"),
  themeBtn: $("themeBtn"),
  fileInput: $("fileInput"),
  dropZone: $("dropZone"),
  sourceImage: $("sourceImage"),
  sourceCanvas: $("sourceCanvas"),
  spinnerLayer: $("spinnerLayer"),
  pivotMarker: $("pivotMarker"),
  emptyState: $("emptyState"),
  sourceStatus: $("sourceStatus"),
  angleStatus: $("angleStatus"),
  rpmRange: $("rpmRange"),
  rpmInput: $("rpmInput"),
  rpmReadout: $("rpmReadout"),
  rampDuration: $("rampDuration"),
  rampDurationOut: $("rampDurationOut"),
  rampBtn: $("rampBtn"),
  pauseBtn: $("pauseBtn"),
  directionBtn: $("directionBtn"),
  zeroBtn: $("zeroBtn"),
  playbackLabel: $("playbackLabel"),
  playbackButtons: [...document.querySelectorAll(".playback-button")],
  pivotX: $("pivotX"),
  pivotXOut: $("pivotXOut"),
  pivotY: $("pivotY"),
  pivotYOut: $("pivotYOut"),
  zoom: $("zoom"),
  zoomOut: $("zoomOut"),
  centerBtn: $("centerBtn"),
  formatLabel: $("formatLabel"),
  formatButtons: [...document.querySelectorAll(".format-button")],
  motionExportControls: $("motionExportControls"),
  fps: $("fps"),
  fpsOut: $("fpsOut"),
  duration: $("duration"),
  durationOut: $("durationOut"),
  size: $("size"),
  sizeOut: $("sizeOut"),
  transparentBg: $("transparentBg"),
  exportHint: $("exportHint"),
  progressWrap: $("progressWrap"),
  progressBar: $("progressBar"),
  exportBtn: $("exportBtn"),
  statusText: $("statusText"),
  renderCanvas: $("renderCanvas"),
};

const state = {
  sourceFile: null,
  objectUrl: null,
  rpm: 60,
  rpmRangeMax: 3000,
  ramping: false,
  rampElapsed: 0,
  rampDuration: 5,
  direction: 1,
  angle: 0,
  spinning: true,
  lastFrameTime: 0,
  pivotX: 0.5,
  pivotY: 0.5,
  zoom: 0.86,
  format: "gif",
  fps: 60,
  duration: 4,
  size: 1080,
  transparent: true,
  exportBusy: false,
  sourceLoadedAt: 0,
  sourcePlayback: "pingpong",
};

let frameProvider = null;

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function normalizeDegrees(value) {
  const wrapped = value % 360;
  return wrapped < 0 ? wrapped + 360 : wrapped;
}

function setStatus(message, error = false) {
  el.statusText.textContent = message;
  el.statusText.classList.toggle("error", error);
}

function setProgress(value, visible = true) {
  el.progressWrap.hidden = !visible;
  el.progressBar.style.width = String(clamp(value, 0, 1) * 100) + "%";
}

function updatePreview() {
  el.spinnerLayer.style.transformOrigin =
    String(state.pivotX * 100) + "% " + String(state.pivotY * 100) + "%";
  el.spinnerLayer.style.transform = "rotate(" + state.angle + "deg)";
  el.sourceImage.style.maxWidth = String(state.zoom * 100) + "%";
  el.sourceImage.style.maxHeight = String(state.zoom * 100) + "%";
  el.sourceCanvas.style.maxWidth = String(state.zoom * 100) + "%";
  el.sourceCanvas.style.maxHeight = String(state.zoom * 100) + "%";
  el.pivotMarker.style.left = String(state.pivotX * 100) + "%";
  el.pivotMarker.style.top = String(state.pivotY * 100) + "%";
  el.angleStatus.textContent = state.angle.toFixed(1) + "°";
}

function updateControls() {
  el.openBtn.disabled = state.exportBusy;
  el.emptyOpenBtn.disabled = state.exportBusy;
  el.fileInput.disabled = state.exportBusy;
  el.rpmReadout.textContent = state.rpm.toFixed(1) + " RPM";
  el.rpmRange.max = String(state.rpmRangeMax);
  el.rpmRange.value = String(Math.min(state.rpm, state.rpmRangeMax));
  el.rampDuration.value = String(state.rampDuration);
  el.rampDurationOut.textContent = state.rampDuration.toFixed(1) + "s";
  el.rampBtn.textContent = state.ramping ? "Restart ramp 0 → target" : "Ramp 0 → target";
  if (document.activeElement !== el.rpmInput) {
    el.rpmInput.value = String(Number(state.rpm.toFixed(1)));
  }

  el.pauseBtn.textContent = state.spinning ? "Pause spin" : "Resume spin";
  el.directionBtn.textContent = state.direction === 1 ? "Clockwise" : "Counter";
  el.playbackLabel.textContent =
    state.sourcePlayback === "pingpong"
      ? "PING-PONG"
      : state.sourcePlayback === "loop"
        ? "LOOP"
        : "ONCE";
  el.playbackButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.playback === state.sourcePlayback);
  });

  el.pivotX.value = String(Math.round(state.pivotX * 100));
  el.pivotY.value = String(Math.round(state.pivotY * 100));
  el.zoom.value = String(Math.round(state.zoom * 100));
  el.pivotXOut.textContent = Math.round(state.pivotX * 100) + "%";
  el.pivotYOut.textContent = Math.round(state.pivotY * 100) + "%";
  el.zoomOut.textContent = Math.round(state.zoom * 100) + "%";

  el.fps.value = String(state.fps);
  el.fpsOut.textContent = String(state.fps);
  el.duration.value = String(state.duration);
  el.durationOut.textContent = state.duration.toFixed(1) + "s";
  el.size.value = String(state.size);
  el.sizeOut.textContent = String(state.size);
  el.transparentBg.checked = state.transparent;

  el.formatButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.format === state.format);
  });

  el.motionExportControls.hidden = state.format === "png";
  el.transparentBg.disabled = state.format === "webm";

  if (state.format === "png") {
    el.formatLabel.textContent = "SNAPSHOT";
    el.exportBtn.textContent = "Export PNG";
    el.exportHint.textContent = "PNG captures the source frame and spin angle currently on screen.";
  } else if (state.format === "gif") {
    el.formatLabel.textContent = "ANIMATED GIF";
    el.exportBtn.textContent = "Export GIF";
    el.exportHint.textContent =
      "The source GIF stays on its native timeline while output frames sample the independent spin clock.";
  } else {
    el.formatLabel.textContent = "WEBM VIDEO";
    el.exportBtn.textContent = "Export WebM";
    el.exportHint.textContent =
      "WebM records in real time so the source GIF continues at its exact browser-native playback speed.";
  }

  el.exportBtn.disabled = !state.sourceFile || state.exportBusy;
}

function sourcePlaybackPosition(elapsedMs, durationMs) {
  const duration = Math.max(1, durationMs);
  const time = Math.max(0, elapsedMs);

  if (state.sourcePlayback === "once") {
    return Math.min(time, duration - 0.001);
  }

  if (state.sourcePlayback === "loop") {
    return ((time % duration) + duration) % duration;
  }

  const period = duration * 2;
  const phase = ((time % period) + period) % period;
  return phase < duration ? phase : period - phase;
}

function sourceAtElapsed(elapsedMs) {
  if (!frameProvider) return el.sourceImage;
  return frameProvider.frameAt(
    sourcePlaybackPosition(elapsedMs, frameProvider.totalDuration),
  );
}

function renderSourcePreview(now) {
  if (!frameProvider) return;

  const elapsedMs = Math.max(0, now - state.sourceLoadedAt);
  const source = sourceAtElapsed(elapsedMs);
  const canvas = el.sourceCanvas;
  const ctx = canvas.getContext("2d", { alpha: true });

  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(source, 0, 0, canvas.width, canvas.height);
}

function animationLoop(now) {
  if (state.lastFrameTime !== 0 && state.spinning) {
    const dtSeconds = (now - state.lastFrameTime) / 1000;
    let rpmSeconds = state.rpm * dtSeconds;

    if (state.ramping) {
      const duration = Math.max(0.1, state.rampDuration);
      const startProgress = clamp(state.rampElapsed, 0, duration);
      const rampSlice = Math.min(dtSeconds, Math.max(0, duration - startProgress));
      const endProgress = startProgress + rampSlice;

      rpmSeconds =
        state.rpm *
        ((endProgress * endProgress) - (startProgress * startProgress)) /
        (2 * duration);

      const postRampSeconds = Math.max(0, dtSeconds - rampSlice);
      rpmSeconds += state.rpm * postRampSeconds;
      state.rampElapsed = endProgress;

      if (endProgress >= duration) {
        state.ramping = false;
        updateControls();
      }
    }

    state.angle = normalizeDegrees(
      state.angle + state.direction * 6 * rpmSeconds,
    );
    updatePreview();
  }

  renderSourcePreview(now);
  state.lastFrameTime = now;
  requestAnimationFrame(animationLoop);
}

function inferredMime(file) {
  if (file.type && file.type.startsWith("image/")) return file.type;
  const name = file.name.toLowerCase();
  if (name.endsWith(".gif")) return "image/gif";
  if (name.endsWith(".png")) return "image/png";
  if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
  if (name.endsWith(".webp")) return "image/webp";
  return "";
}

async function loadSource(file) {
  if (state.exportBusy) return;
  const mime = file ? inferredMime(file) : "";
  if (!file || !mime) {
    setStatus("Choose a GIF, PNG, JPEG, or WebP image.", true);
    return;
  }

  if (frameProvider) {
    frameProvider.close();
    frameProvider = null;
  }

  if (state.objectUrl) {
    URL.revokeObjectURL(state.objectUrl);
  }

  if (!file.type && mime) {
    file = new File([file], file.name, { type: mime });
  }

  state.sourceFile = file;
  state.objectUrl = URL.createObjectURL(file);
  state.angle = 0;
  state.spinning = true;
  state.ramping = false;
  state.rampElapsed = 0;
  state.sourcePlayback = "pingpong";

  el.sourceImage.src = state.objectUrl;
  el.sourceImage.alt = file.name;
  el.sourceImage.style.display = "block";
  el.dropZone.style.touchAction = "none";

  try {
    await el.sourceImage.decode();
  } catch {
    await new Promise((resolve, reject) => {
      el.sourceImage.onload = resolve;
      el.sourceImage.onerror = reject;
    });
  }

  state.sourceLoadedAt = performance.now();
  el.sourceCanvas.style.display = "none";
  el.emptyState.hidden = true;
  el.pivotMarker.style.display = "block";
  el.sourceStatus.textContent =
    file.name + " • " +
    el.sourceImage.naturalWidth + "×" + el.sourceImage.naturalHeight;

  setStatus("Ready. GIFs default to ping-pong; spin timing remains independent.");
  updateControls();
  updatePreview();

  const loadedFile = file;
  if (file.type === "image/gif" || file.type === "image/webp") {
    buildDecodedAnimation(file).then((decoded) => {
      if (state.sourceFile !== loadedFile) {
        decoded?.close();
        return;
      }

      if (frameProvider) frameProvider.close();
      frameProvider = decoded;

      if (frameProvider) {
        el.sourceCanvas.width = el.sourceImage.naturalWidth;
        el.sourceCanvas.height = el.sourceImage.naturalHeight;
        el.sourceCanvas.style.display = "block";
        el.sourceImage.style.display = "none";
        state.sourceLoadedAt = performance.now();
        setStatus("Ping-pong source preview active. GIF frame delays remain native.");
      } else {
        el.sourceCanvas.style.display = "none";
        el.sourceImage.style.display = "block";
        setStatus("Native GIF playback active; this browser cannot decode frames for ping-pong.", true);
      }
    });
  }
}

class DecodedAnimation {
  constructor(decoder, frames, starts, durations, totalDuration) {
    this.decoder = decoder;
    this.frames = frames;
    this.starts = starts;
    this.durations = durations;
    this.totalDuration = totalDuration;
  }

  frameAt(timeMs) {
    if (this.frames.length === 1 || this.totalDuration <= 0) {
      return this.frames[0];
    }

    const position = ((timeMs % this.totalDuration) + this.totalDuration) % this.totalDuration;

    let low = 0;
    let high = this.starts.length - 1;

    while (low <= high) {
      const mid = (low + high) >> 1;
      const start = this.starts[mid];
      const end = start + this.durations[mid];

      if (position < start) {
        high = mid - 1;
      } else if (position >= end) {
        low = mid + 1;
      } else {
        return this.frames[mid];
      }
    }

    return this.frames[this.frames.length - 1];
  }

  close() {
    this.frames.forEach((frame) => frame.close());
    this.decoder.close();
  }
}

async function buildDecodedAnimation(file) {
  if (!("ImageDecoder" in window)) {
    return null;
  }

  if (file.type !== "image/gif" && file.type !== "image/webp") {
    return null;
  }

  const buffer = await file.arrayBuffer();
  const decoder = new ImageDecoder({
    data: buffer,
    type: file.type,
    preferAnimation: true,
  });

  try {
    await decoder.tracks.ready;
    const track = decoder.tracks.selectedTrack;
    if (!track || track.frameCount <= 1) {
      decoder.close();
      return null;
    }

    const pixels =
      Math.max(1, el.sourceImage.naturalWidth) *
      Math.max(1, el.sourceImage.naturalHeight) *
      track.frameCount;

    if (pixels * 4 > 256 * 1024 * 1024) {
      decoder.close();
      return null;
    }

    const frames = [];
    const starts = [];
    const durations = [];
    let total = 0;

    for (let i = 0; i < track.frameCount; i += 1) {
      const result = await decoder.decode({
        frameIndex: i,
        completeFramesOnly: true,
      });

      const videoFrame = result.image;
      const bitmap = await createImageBitmap(videoFrame);
      const durationMs = Math.max(10, Number(videoFrame.duration || 100000) / 1000);

      starts.push(total);
      durations.push(durationMs);
      total += durationMs;
      frames.push(bitmap);
      videoFrame.close();
    }

    return new DecodedAnimation(decoder, frames, starts, durations, total);
  } catch (error) {
    decoder.close();
    return null;
  }
}

function sourceDimensions(source) {
  if ("naturalWidth" in source) {
    return [source.naturalWidth, source.naturalHeight];
  }
  if ("displayWidth" in source) {
    return [source.displayWidth, source.displayHeight];
  }
  return [source.width, source.height];
}

function prepareCanvas() {
  const canvas = el.renderCanvas;
  canvas.width = state.size;
  canvas.height = state.size;
  return canvas;
}

function renderComposite(ctx, source, angleDegrees, transparent) {
  const size = ctx.canvas.width;
  ctx.save();
  ctx.clearRect(0, 0, size, size);

  if (!transparent) {
    const light = document.documentElement.dataset.theme === "light";
    ctx.fillStyle = light ? "#eee7d8" : "#070809";
    ctx.fillRect(0, 0, size, size);
  }

  const [sourceWidth, sourceHeight] = sourceDimensions(source);
  const diagonal = Math.max(1, Math.hypot(sourceWidth, sourceHeight));
  const scale = (size * state.zoom) / diagonal;
  const drawWidth = sourceWidth * scale;
  const drawHeight = sourceHeight * scale;
  const left = (size - drawWidth) / 2;
  const top = (size - drawHeight) / 2;
  const pivotX = size * state.pivotX;
  const pivotY = size * state.pivotY;

  ctx.translate(pivotX, pivotY);
  ctx.rotate((angleDegrees * Math.PI) / 180);
  ctx.translate(-pivotX, -pivotY);
  ctx.drawImage(source, left, top, drawWidth, drawHeight);
  ctx.restore();
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function spinAngleForExport(startAngle, elapsedSeconds, spinState) {
  const elapsed = Math.max(0, elapsedSeconds);
  if (!spinState.ramping || spinState.rampDuration <= 0) {
    return normalizeDegrees(
      startAngle + spinState.direction * spinState.rpm * 6 * elapsed,
    );
  }

  const duration = Math.max(0.001, spinState.rampDuration);
  const startProgress = clamp(spinState.rampElapsed, 0, duration);
  const endProgress = startProgress + elapsed;
  const rampEnd = Math.min(endProgress, duration);

  let rpmSeconds = 0;
  if (rampEnd > startProgress) {
    rpmSeconds +=
      spinState.rpm *
      ((rampEnd * rampEnd) - (startProgress * startProgress)) /
      (2 * duration);
  }

  rpmSeconds += spinState.rpm * Math.max(0, endProgress - duration);
  return normalizeDegrees(
    startAngle + spinState.direction * 6 * rpmSeconds,
  );
}

function captureSpinExportState() {
  return {
    rpm: state.rpm,
    direction: state.direction,
    ramping: state.ramping,
    rampElapsed: state.rampElapsed,
    rampDuration: state.rampDuration,
  };
}

function outputName(extension) {
  const now = new Date();
  const stamp =
    now.getFullYear().toString() +
    String(now.getMonth() + 1).padStart(2, "0") +
    String(now.getDate()).padStart(2, "0") + "_" +
    String(now.getHours()).padStart(2, "0") +
    String(now.getMinutes()).padStart(2, "0") +
    String(now.getSeconds()).padStart(2, "0");
  return "SpinMe_" + stamp + "." + extension;
}

async function exportPng() {
  const canvas = prepareCanvas();
  const ctx = canvas.getContext("2d", { alpha: true });
  const elapsedMs = Math.max(0, performance.now() - state.sourceLoadedAt);
  renderComposite(ctx, sourceAtElapsed(elapsedMs), state.angle, state.transparent);

  const blob = await new Promise((resolve) => canvas.toBlob(resolve, "image/png"));
  if (!blob) throw new Error("PNG encoding failed.");

  downloadBlob(blob, outputName("png"));
}

function encodeGifFrame(gif, ctx, delayMs, transparent) {
  const width = ctx.canvas.width;
  const height = ctx.canvas.height;
  const rgba = ctx.getImageData(0, 0, width, height).data;

  if (transparent) {
    const flattened = new Uint8Array(rgba.length);

    for (let i = 0; i < rgba.length; i += 4) {
      const alpha = rgba[i + 3];
      flattened[i] = Math.round((rgba[i] * alpha) / 255);
      flattened[i + 1] = Math.round((rgba[i + 1] * alpha) / 255);
      flattened[i + 2] = Math.round((rgba[i + 2] * alpha) / 255);
      flattened[i + 3] = 255;
    }

    const palette = quantize(flattened, 255);
    const indexed = applyPalette(flattened, palette);

    for (let pixel = 0; pixel < indexed.length; pixel += 1) {
      indexed[pixel] = rgba[pixel * 4 + 3] <= 8 ? 0 : indexed[pixel] + 1;
    }

    palette.unshift([0, 0, 0]);

    gif.writeFrame(indexed, width, height, {
      palette,
      delay: delayMs,
      repeat: 0,
      transparent: true,
      transparentIndex: 0,
      dispose: 2,
    });
  } else {
    const palette = quantize(rgba, 256);
    const indexed = applyPalette(rgba, palette);

    gif.writeFrame(indexed, width, height, {
      palette,
      delay: delayMs,
      repeat: 0,
      transparent: false,
      dispose: 2,
    });
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function yieldToBrowser() {
  await new Promise((resolve) => setTimeout(resolve, 0));
}

async function exportGif() {
  const canvas = prepareCanvas();
  const ctx = canvas.getContext("2d", { alpha: true, willReadFrequently: true });
  const fps = clamp(Math.round(state.fps), 1, 100);
  const frameCount = Math.max(1, Math.ceil(state.duration * fps));
  const delayMs = Math.max(10, Math.round(1000 / fps));
  const startAngle = state.angle;
  const spinState = captureSpinExportState();
  const gif = GIFEncoder();

  if (!frameProvider) {
    setStatus("Preparing source animation…");
    frameProvider = await buildDecodedAnimation(state.sourceFile);
  }

  const deterministic = frameProvider !== null;
  const realtimeStart = performance.now();
  const sourcePhaseMs = deterministic
    ? Math.max(0, realtimeStart - state.sourceLoadedAt)
    : 0;

  for (let i = 0; i < frameCount; i += 1) {
    const tMs = (i * 1000) / fps;

    if (!deterministic && state.sourceFile.type === "image/gif") {
      const target = realtimeStart + tMs;
      const wait = target - performance.now();
      if (wait > 0) {
        await sleep(wait);
      }
    }

    const source = deterministic
      ? sourceAtElapsed(sourcePhaseMs + tMs)
      : el.sourceImage;

    const angle = spinAngleForExport(
      startAngle,
      tMs / 1000,
      spinState,
    );

    renderComposite(ctx, source, angle, state.transparent);
    encodeGifFrame(gif, ctx, delayMs, state.transparent);

    setProgress((i + 1) / frameCount);

    if (i % 2 === 1) {
      await yieldToBrowser();
    }
  }

  gif.finish();
  const bytes = gif.bytes();
  const blob = new Blob([bytes], { type: "image/gif" });
  downloadBlob(blob, outputName("gif"));
}

function chooseWebmMimeType() {
  const candidates = [
    "video/webm;codecs=vp9",
    "video/webm;codecs=vp8",
    "video/webm",
  ];

  for (const candidate of candidates) {
    if (MediaRecorder.isTypeSupported(candidate)) {
      return candidate;
    }
  }

  return "";
}

async function exportWebm() {
  if (!("MediaRecorder" in window)) {
    throw new Error("This browser does not support WebM recording.");
  }

  const canvas = prepareCanvas();
  const ctx = canvas.getContext("2d", { alpha: false });
  const fps = clamp(Math.round(state.fps), 1, 240);
  const stream = canvas.captureStream(fps);
  const mimeType = chooseWebmMimeType();
  const recorder = new MediaRecorder(
    stream,
    mimeType ? { mimeType, videoBitsPerSecond: 14_000_000 } : undefined,
  );

  const chunks = [];
  recorder.ondataavailable = (event) => {
    if (event.data.size > 0) chunks.push(event.data);
  };

  const stopped = new Promise((resolve, reject) => {
    recorder.onstop = resolve;
    recorder.onerror = () => reject(recorder.error || new Error("WebM recording failed."));
  });

  const startAngle = state.angle;
  const spinState = captureSpinExportState();
  const start = performance.now();
  const sourcePhaseMs = Math.max(0, start - state.sourceLoadedAt);
  const durationMs = state.duration * 1000;
  const frameInterval = 1000 / fps;
  let nextFrame = 0;

  recorder.start(250);

  while (true) {
    const elapsed = performance.now() - start;
    if (elapsed >= durationMs) break;

    if (elapsed >= nextFrame) {
      const angle = spinAngleForExport(
        startAngle,
        elapsed / 1000,
        spinState,
      );
      renderComposite(
        ctx,
        frameProvider ? sourceAtElapsed(sourcePhaseMs + elapsed) : el.sourceImage,
        angle,
        false,
      );
      nextFrame += frameInterval;
      setProgress(elapsed / durationMs);
    }

    await sleep(Math.min(8, Math.max(1, nextFrame - (performance.now() - start))));
  }

  const finalAngle = spinAngleForExport(
    startAngle,
    durationMs / 1000,
    spinState,
  );
  renderComposite(
    ctx,
    frameProvider ? sourceAtElapsed(sourcePhaseMs + durationMs) : el.sourceImage,
    finalAngle,
    false,
  );
  setProgress(1);

  recorder.stop();
  await stopped;
  stream.getTracks().forEach((track) => track.stop());

  const blob = new Blob(chunks, { type: recorder.mimeType || "video/webm" });
  downloadBlob(blob, outputName("webm"));
}

async function runExport() {
  if (!state.sourceFile || state.exportBusy) return;

  state.exportBusy = true;
  updateControls();
  setProgress(0, true);
  setStatus("Rendering export…");

  try {
    if (state.format === "png") {
      await exportPng();
    } else if (state.format === "gif") {
      await exportGif();
    } else {
      await exportWebm();
    }

    setProgress(1, true);
    setStatus("Export complete.");
  } catch (error) {
    console.error(error);
    setStatus(
      "Export failed: " + (error && error.message ? error.message : String(error)),
      true,
    );
  } finally {
    state.exportBusy = false;
    updateControls();
    setTimeout(() => {
      if (!state.exportBusy) setProgress(0, false);
    }, 1200);
  }
}

function openPicker() {
  el.fileInput.click();
}

el.openBtn.addEventListener("click", openPicker);
el.emptyOpenBtn.addEventListener("click", openPicker);

el.fileInput.addEventListener("change", () => {
  loadSource(el.fileInput.files[0]);
});

window.addEventListener("paste", (event) => {
  if (state.exportBusy) return;
  const item = [...event.clipboardData.items].find((entry) =>
    entry.type.startsWith("image/"),
  );
  if (item) {
    const file = item.getAsFile();
    if (file) loadSource(file);
  }
});

["dragenter", "dragover"].forEach((eventName) => {
  el.dropZone.addEventListener(eventName, (event) => {
    event.preventDefault();
    el.dropZone.classList.add("dragging");
  });
});

["dragleave", "drop"].forEach((eventName) => {
  el.dropZone.addEventListener(eventName, (event) => {
    event.preventDefault();
    el.dropZone.classList.remove("dragging");
  });
});

el.dropZone.addEventListener("drop", (event) => {
  const file = event.dataTransfer.files[0];
  loadSource(file);
});

let pivotDragging = false;

function setPivotFromPointer(event) {
  if (!state.sourceFile) return;
  const rect = el.dropZone.getBoundingClientRect();
  state.pivotX = clamp((event.clientX - rect.left) / rect.width, 0, 1);
  state.pivotY = clamp((event.clientY - rect.top) / rect.height, 0, 1);
  updateControls();
  updatePreview();
}

el.dropZone.addEventListener("pointerdown", (event) => {
  if (!state.sourceFile || event.button !== 0) return;
  pivotDragging = true;
  el.dropZone.setPointerCapture(event.pointerId);
  setPivotFromPointer(event);
});

el.dropZone.addEventListener("pointermove", (event) => {
  if (pivotDragging) setPivotFromPointer(event);
});

el.dropZone.addEventListener("pointerup", (event) => {
  if (!pivotDragging) return;
  pivotDragging = false;
  if (el.dropZone.hasPointerCapture(event.pointerId)) {
    el.dropZone.releasePointerCapture(event.pointerId);
  }
});

el.dropZone.addEventListener("pointercancel", () => {
  pivotDragging = false;
});

el.themeBtn.addEventListener("click", () => {
  const root = document.documentElement;
  root.dataset.theme = root.dataset.theme === "light" ? "dark" : "light";
});

function setRpm(value) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return;

  state.rpm = Math.max(0, parsed);
  if (state.rpm > state.rpmRangeMax) {
    state.rpmRangeMax = Math.max(
      3000,
      Math.ceil(state.rpm / 1000) * 1000,
    );
  }
  state.ramping = false;
  state.rampElapsed = 0;
  updateControls();
}

el.rpmRange.addEventListener("input", () => setRpm(el.rpmRange.value));
el.rpmInput.addEventListener("input", () => setRpm(el.rpmInput.value));

document.querySelectorAll(".rpm-preset").forEach((button) => {
  button.addEventListener("click", () => setRpm(button.dataset.rpm));
});

el.rampDuration.addEventListener("input", () => {
  state.rampDuration = clamp(Number(el.rampDuration.value) || 5, 0.5, 60);
  if (state.ramping && state.rampElapsed >= state.rampDuration) {
    state.ramping = false;
    state.rampElapsed = state.rampDuration;
  }
  updateControls();
});

el.rampBtn.addEventListener("click", () => {
  state.rampElapsed = 0;
  state.ramping = true;
  state.spinning = true;
  updateControls();
  setStatus(
    "Ramping from 0 to " + state.rpm.toFixed(1) +
    " RPM over " + state.rampDuration.toFixed(1) + " seconds.",
  );
});

el.pauseBtn.addEventListener("click", () => {
  state.spinning = !state.spinning;
  updateControls();
});

el.directionBtn.addEventListener("click", () => {
  state.direction *= -1;
  updateControls();
});

el.zeroBtn.addEventListener("click", () => {
  state.angle = 0;
  updatePreview();
});

el.playbackButtons.forEach((button) => {
  button.addEventListener("click", () => {
    state.sourcePlayback = button.dataset.playback;
    state.sourceLoadedAt = performance.now();
    updateControls();

    if (state.sourcePlayback === "pingpong") {
      setStatus("Ping-pong source playback active.");
    } else if (state.sourcePlayback === "loop") {
      setStatus("Forward source loop active.");
    } else {
      setStatus("Source will play once and hold on its last frame.");
    }
  });
});

el.pivotX.addEventListener("input", () => {
  state.pivotX = Number(el.pivotX.value) / 100;
  updateControls();
  updatePreview();
});

el.pivotY.addEventListener("input", () => {
  state.pivotY = Number(el.pivotY.value) / 100;
  updateControls();
  updatePreview();
});

el.zoom.addEventListener("input", () => {
  state.zoom = Number(el.zoom.value) / 100;
  updateControls();
  updatePreview();
});

el.centerBtn.addEventListener("click", () => {
  state.pivotX = 0.5;
  state.pivotY = 0.5;
  state.zoom = 0.86;
  updateControls();
  updatePreview();
});

el.formatButtons.forEach((button) => {
  button.addEventListener("click", () => {
    state.format = button.dataset.format;

    if (state.format === "gif") {
      state.fps = clamp(state.fps, 1, 100);
      el.fps.max = "100";
    } else if (state.format === "webm") {
      state.fps = clamp(state.fps, 1, 240);
      el.fps.max = "240";
    }

    updateControls();
  });
});

el.fps.addEventListener("input", () => {
  state.fps = Number(el.fps.value);
  updateControls();
});

el.duration.addEventListener("input", () => {
  state.duration = Number(el.duration.value);
  updateControls();
});

el.size.addEventListener("change", () => {
  state.size = Number(el.size.value);
  updateControls();
});

el.transparentBg.addEventListener("change", () => {
  state.transparent = el.transparentBg.checked;
  updateControls();
});

el.exportBtn.addEventListener("click", runExport);

window.addEventListener("beforeunload", () => {
  if (state.objectUrl) URL.revokeObjectURL(state.objectUrl);
  if (frameProvider) frameProvider.close();
});

updateControls();
updatePreview();
requestAnimationFrame(animationLoop);

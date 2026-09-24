package com.spinme.app;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.Arrays;

public final class SpinView extends View {
    public enum PlaybackMode { PING_PONG, LOOP, ONCE }
    public interface PivotListener { void onPivotChanged(float x, float y); }

    private Bitmap bitmap;
    private Movie movie;
    private byte[] gifData;
    private int sourceDurationMs;
    private long sourceStartMs = SystemClock.uptimeMillis();
    private long spinStartMs = SystemClock.uptimeMillis();
    private float startAngleDeg = 0f;
    private float rpm = 60f;
    private float direction = 1f;
    private float scale = 1f;
    private float pivotX = .5f;
    private float pivotY = .5f;
    private boolean spinPaused = false;
    private float pausedAngleDeg = 0f;
    private PlaybackMode playbackMode = PlaybackMode.PING_PONG;
    private PivotListener pivotListener;
    private final Paint pivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public SpinView(Context c) { super(c); init(); }
    public SpinView(Context c, AttributeSet a) { super(c, a); init(); }
    private void init() {
        setBackgroundColor(Color.rgb(12,12,12));
        pivotPaint.setColor(Color.rgb(230,184,78));
        pivotPaint.setStrokeWidth(dp(2));
        setFocusable(true);
    }

    public void setPivotListener(PivotListener l) { pivotListener = l; }

    public void setMedia(byte[] data) {
        recycleBitmap();
        gifData = null;
        movie = null;
        sourceDurationMs = 0;
        if (data == null || data.length == 0) { invalidate(); return; }
        boolean looksGif = data.length > 6 && data[0]=='G' && data[1]=='I' && data[2]=='F';
        if (looksGif) {
            Movie m = Movie.decodeByteArray(data, 0, data.length);
            if (m != null && m.width() > 0 && m.height() > 0) {
                movie = m;
                gifData = Arrays.copyOf(data, data.length);
                sourceDurationMs = m.duration() > 0 ? m.duration() : 1000;
            }
        }
        if (movie == null) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        sourceStartMs = SystemClock.uptimeMillis();
        invalidate();
    }

    private void recycleBitmap() {
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        bitmap = null;
    }

    public boolean hasMedia() { return movie != null || bitmap != null; }
    public boolean isAnimated() { return movie != null; }
    public int getSourceDurationMs() { return sourceDurationMs; }

    public void setPlaybackMode(PlaybackMode mode) { playbackMode = mode; invalidate(); }
    public PlaybackMode getPlaybackMode() { return playbackMode; }

    public void setRpm(float value) {
        rebaseSpin();
        rpm = Math.max(0f, Math.min(3000f, value));
    }
    public float getRpm() { return rpm; }

    public void setDirection(boolean clockwise) {
        rebaseSpin();
        direction = clockwise ? 1f : -1f;
    }
    public boolean isClockwise() { return direction > 0; }

    public void setScaleFactor(float value) { scale = Math.max(.1f, Math.min(3f, value)); invalidate(); }
    public float getScaleFactor() { return scale; }

    public void setStartAngle(float deg) {
        startAngleDeg = normalize(deg);
        spinStartMs = SystemClock.uptimeMillis();
        if (spinPaused) pausedAngleDeg = startAngleDeg;
        invalidate();
    }

    public void setPivot(float x, float y) {
        pivotX = clamp01(x); pivotY = clamp01(y); invalidate();
    }
    public float getPivotXNorm() { return pivotX; }
    public float getPivotYNorm() { return pivotY; }

    public void togglePause() {
        if (spinPaused) {
            startAngleDeg = pausedAngleDeg;
            spinStartMs = SystemClock.uptimeMillis();
            spinPaused = false;
        } else {
            pausedAngleDeg = currentAngle(SystemClock.uptimeMillis());
            spinPaused = true;
        }
        invalidate();
    }
    public boolean isSpinPaused() { return spinPaused; }

    private void rebaseSpin() {
        long now = SystemClock.uptimeMillis();
        float angle = currentAngle(now);
        startAngleDeg = angle;
        spinStartMs = now;
        if (spinPaused) pausedAngleDeg = angle;
    }

    private float currentAngle(long now) {
        if (spinPaused) return pausedAngleDeg;
        double seconds = Math.max(0, now - spinStartMs) / 1000.0;
        return normalize((float)(startAngleDeg + direction * rpm * 6.0 * seconds));
    }

    private long sourceElapsed(long now) { return Math.max(0, now - sourceStartMs); }

    public static int mapSourcePosition(long elapsedMs, int durationMs, PlaybackMode mode) {
        if (durationMs <= 1) return 0;
        if (mode == PlaybackMode.ONCE) return (int)Math.min(durationMs - 1L, elapsedMs);
        if (mode == PlaybackMode.LOOP) return (int)(elapsedMs % durationMs);
        long period = durationMs * 2L;
        long phase = elapsedMs % period;
        long mapped = phase < durationMs ? phase : period - phase;
        return (int)Math.min(durationMs - 1L, mapped);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        drawMedia(canvas, getWidth(), getHeight(), currentAngle(now), sourceElapsed(now));
        drawPivot(canvas);
        postInvalidateOnAnimation();
    }

    private void drawMedia(Canvas canvas, int width, int height, float angle, long sourceElapsedMs) {
        if (!hasMedia() || width <= 0 || height <= 0) return;
        int sw = movie != null ? movie.width() : bitmap.getWidth();
        int sh = movie != null ? movie.height() : bitmap.getHeight();
        if (sw <= 0 || sh <= 0) return;
        float base = Math.min(width / (float)sw, height / (float)sh) * scale;
        float dw = sw * base, dh = sh * base;
        float left = (width - dw) * .5f, top = (height - dh) * .5f;
        canvas.save();
        canvas.rotate(angle, pivotX * width, pivotY * height);
        if (movie != null) {
            movie.setTime(mapSourcePosition(sourceElapsedMs, sourceDurationMs, playbackMode));
            canvas.save();
            canvas.translate(left, top);
            canvas.scale(base, base);
            movie.draw(canvas, 0, 0);
            canvas.restore();
        } else {
            RectF dst = new RectF(left, top, left + dw, top + dh);
            canvas.drawBitmap(bitmap, null, dst, bitmapPaint);
        }
        canvas.restore();
    }

    private void drawPivot(Canvas c) {
        float x = pivotX * getWidth(), y = pivotY * getHeight(), r = dp(10);
        c.drawCircle(x, y, r, pivotPaint);
        c.drawLine(x-r*1.6f,y,x+r*1.6f,y,pivotPaint);
        c.drawLine(x,y-r*1.6f,x,y+r*1.6f,pivotPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN || e.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (getWidth() > 0 && getHeight() > 0) {
                setPivot(e.getX()/getWidth(), e.getY()/getHeight());
                if (pivotListener != null) pivotListener.onPivotChanged(pivotX, pivotY);
                return true;
            }
        }
        return e.getActionMasked() == MotionEvent.ACTION_UP || super.onTouchEvent(e);
    }

    public Snapshot snapshot() {
        long now = SystemClock.uptimeMillis();
        Snapshot s = new Snapshot();
        s.bitmap = bitmap == null ? null : bitmap.copy(Bitmap.Config.ARGB_8888, false);
        s.gifData = gifData == null ? null : Arrays.copyOf(gifData, gifData.length);
        s.durationMs = sourceDurationMs;
        s.sourceElapsedMs = sourceElapsed(now);
        s.angleDeg = currentAngle(now);
        s.rpm = rpm;
        s.direction = direction;
        s.scale = scale;
        s.pivotX = pivotX;
        s.pivotY = pivotY;
        s.mode = playbackMode;
        return s;
    }

    public static final class Snapshot {
        public Bitmap bitmap;
        public byte[] gifData;
        public int durationMs;
        public long sourceElapsedMs;
        public float angleDeg, rpm, direction, scale, pivotX, pivotY;
        public PlaybackMode mode;
        public boolean animated() { return gifData != null; }
        public void close() { if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle(); }
    }

    private static float normalize(float d) { float v=d%360f; return v<0?v+360f:v; }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f,v)); }
    private float dp(float n) { return n * getResources().getDisplayMetrics().density; }
}

package com.spinme.app;

import android.app.Activity;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int PICK_MEDIA = 42;

    private SpinView spinView;
    private TextView status;
    private TextView rpmLabel;
    private TextView scaleLabel;
    private TextView angleLabel;
    private TextView pivotLabel;
    private TextView durationLabel;
    private TextView fpsLabel;
    private TextView rampLabel;

    private SeekBar rpmSeek;
    private SeekBar scaleSeek;
    private SeekBar angleSeek;
    private SeekBar pivotXSeek;
    private SeekBar pivotYSeek;
    private SeekBar durationSeek;
    private SeekBar fpsSeek;
    private SeekBar rampSeek;

    private EditText rpmInput;
    private Spinner modeSpinner;
    private Spinner sizeSpinner;

    private Button pauseButton;
    private Button directionButton;
    private Button themeButton;
    private Button rampButton;

    private boolean clockwise = true;
    private boolean dark = true;
    private boolean syncingRpm = false;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("SPINME", 30, true);
        root.addView(title);
        TextView by = text("Background Gremlin Group", 14, true);
        root.addView(by);
        TextView tag = text("Creating Unique Tools for Unique Individuals", 13, false);
        root.addView(tag);
        addSpace(root, 8);

        LinearLayout top = row();
        Button open = button("Import image / GIF");
        open.setOnClickListener(v -> pickMedia());
        top.addView(open, weight());

        themeButton = button("Light");
        themeButton.setOnClickListener(v -> {
            dark = !dark;
            applyTheme(scroll);
        });
        top.addView(themeButton, wrap());
        root.addView(top, matchWrap());

        spinView = new SpinView(this);
        spinView.setPivotListener((x, y) -> runOnUiThread(() -> {
            pivotXSeek.setProgress(Math.round(x * 100));
            pivotYSeek.setProgress(Math.round(y * 100));
            updatePivotLabel();
        }));
        root.addView(spinView, new LinearLayout.LayoutParams(-1, dp(420)));

        status = text("Import media to begin.", 13, false);
        root.addView(status);

        addSection(root, "SOURCE PLAYBACK");
        modeSpinner = new Spinner(this);
        String[] modes = {"Ping-pong", "Loop", "Once"};
        modeSpinner.setAdapter(
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                modes
            )
        );
        modeSpinner.setOnItemSelectedListener(
            new SimpleItemSelected() {
                public void selected(int p) {
                    spinView.setPlaybackMode(
                        p == 1
                            ? SpinView.PlaybackMode.LOOP
                            : p == 2
                                ? SpinView.PlaybackMode.ONCE
                                : SpinView.PlaybackMode.PING_PONG
                    );
                }
            }
        );
        root.addView(modeSpinner, matchWrap());

        addSection(root, "SPIN");

        rpmLabel = text("Speed: 60 RPM", 14, true);
        root.addView(rpmLabel);

        rpmSeek = seek(3000, 60, this::applyRpmFromSlider);
        root.addView(rpmSeek);

        LinearLayout presets = row();
        for (int v : new int[]{60, 360, 1000, 3000}) {
            Button q = button(v == 3000 ? "3k RPM" : v + " RPM");
            q.setOnClickListener(x -> rpmSeek.setProgress(v));
            presets.addView(q, weight());
        }
        root.addView(presets, matchWrap());

        rpmInput = new EditText(this);
        rpmInput.setSingleLine(true);
        rpmInput.setInputType(
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        rpmInput.setText("60");
        rpmInput.setHint("Exact RPM — no fixed maximum");
        rpmInput.addTextChangedListener(
            new TextWatcher() {
                @Override
                public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
                ) {}

                @Override
                public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
                ) {}

                @Override
                public void afterTextChanged(Editable s) {
                    applyExactRpm(s.toString());
                }
            }
        );
        root.addView(rpmInput, matchWrap());
        root.addView(
            text(
                "Exact RPM is intentionally uncapped. Source animation timing never changes with spin speed.",
                12,
                false
            )
        );

        rampLabel = text("Ramp duration: 5.0 s", 14, true);
        root.addView(rampLabel);

        rampSeek = seek(
            595,
            45,
            p -> {
                float seconds = .5f + p / 10f;
                spinView.setRampDurationMs(
                    Math.round(seconds * 1000f)
                );
                rampLabel.setText(
                    String.format(
                        Locale.US,
                        "Ramp duration: %.1f s",
                        seconds
                    )
                );
            }
        );
        root.addView(rampSeek);

        rampButton = button("Ramp 0 → target");
        rampButton.setOnClickListener(v -> {
            spinView.startRamp();
            if (pauseButton != null) {
                pauseButton.setText("Pause spin");
            }
            status.setText(
                String.format(
                    Locale.US,
                    "Ramping from 0 to %s RPM over %.1f seconds.",
                    formatRpm(spinView.getRpm()),
                    spinView.getRampDurationMs() / 1000f
                )
            );
        });
        root.addView(rampButton, matchWrap());

        LinearLayout spinBtns = row();

        directionButton = button("Clockwise");
        directionButton.setOnClickListener(v -> {
            clockwise = !clockwise;
            spinView.setDirection(clockwise);
            directionButton.setText(
                clockwise ? "Clockwise" : "Counter-clockwise"
            );
        });
        spinBtns.addView(directionButton, weight());

        pauseButton = button("Pause spin");
        pauseButton.setOnClickListener(v -> {
            spinView.togglePause();
            pauseButton.setText(
                spinView.isSpinPaused()
                    ? "Resume spin"
                    : "Pause spin"
            );
        });
        spinBtns.addView(pauseButton, weight());
        root.addView(spinBtns, matchWrap());

        scaleLabel = text("Scale: 100%", 14, true);
        root.addView(scaleLabel);

        scaleSeek = seek(
            290,
            90,
            p -> {
                float s = (p + 10) / 100f;
                spinView.setScaleFactor(s);
                scaleLabel.setText(
                    "Scale: " + Math.round(s * 100) + "%"
                );
            }
        );
        root.addView(scaleSeek);

        angleLabel = text("Start angle: 0°", 14, true);
        root.addView(angleLabel);

        angleSeek = seek(
            359,
            0,
            p -> {
                spinView.setStartAngle(p);
                angleLabel.setText("Start angle: " + p + "°");
            }
        );
        root.addView(angleSeek);

        addSection(root, "PIVOT");

        pivotLabel = text("Pivot: 50%, 50%", 14, true);
        root.addView(pivotLabel);

        pivotXSeek = seek(
            100,
            50,
            p -> {
                spinView.setPivot(
                    p / 100f,
                    pivotYSeek == null
                        ? .5f
                        : pivotYSeek.getProgress() / 100f
                );
                updatePivotLabel();
            }
        );
        root.addView(pivotXSeek);

        pivotYSeek = seek(
            100,
            50,
            p -> {
                spinView.setPivot(
                    pivotXSeek.getProgress() / 100f,
                    p / 100f
                );
                updatePivotLabel();
            }
        );
        root.addView(pivotYSeek);

        TextView pivotHint = text(
            "Drag directly on the preview to move the pivot.",
            12,
            false
        );
        root.addView(pivotHint);

        addSection(root, "EXPORT");

        LinearLayout sizeRow = row();
        sizeRow.addView(text("Output size", 14, true), weight());

        sizeSpinner = new Spinner(this);
        sizeSpinner.setAdapter(
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                    "512 × 512",
                    "720 × 720",
                    "1080 × 1080",
                    "1440 × 1440"
                }
            )
        );
        sizeRow.addView(sizeSpinner, wrap());
        root.addView(sizeRow, matchWrap());

        durationLabel = text("Duration: 3.0 s", 14, true);
        root.addView(durationLabel);

        durationSeek = seek(
            195,
            25,
            p -> {
                float sec = .5f + p / 10f;
                durationLabel.setText(
                    String.format(
                        Locale.US,
                        "Duration: %.1f s",
                        sec
                    )
                );
            }
        );
        root.addView(durationSeek);

        fpsLabel = text("Output FPS: 30", 14, true);
        root.addView(fpsLabel);

        fpsSeek = seek(
            239,
            29,
            p -> fpsLabel.setText(
                "Output FPS: " + (p + 1)
            )
        );
        root.addView(fpsSeek);

        LinearLayout exports = row();

        Button png = button("PNG");
        png.setOnClickListener(v -> export("PNG"));
        exports.addView(png, weight());

        Button gif = button("GIF");
        gif.setOnClickListener(v -> export("GIF"));
        exports.addView(gif, weight());

        Button mp4 = button("MP4");
        mp4.setOnClickListener(v -> export("MP4"));
        exports.addView(mp4, weight());

        root.addView(exports, matchWrap());

        addSection(root, "ABOUT");
        root.addView(
            text(
                "SpinMe v0.2.0\n" +
                "© Background Gremlin Group\n" +
                "Creating Unique Tools for Unique Individuals\n\n" +
                "Local-first processing. Source animation timing and spin timing are independent. " +
                "Exact RPM has no fixed maximum and ramp-up can accelerate automatically from 0 to the selected target.",
                13,
                false
            )
        );

        setContentView(scroll);
        applyTheme(scroll);
    }

    private void applyRpmFromSlider(int progress) {
        if (syncingRpm) return;

        syncingRpm = true;
        float value = progress;
        spinView.setRpm(value);
        rpmLabel.setText(
            "Speed: " + formatRpm(value) + " RPM"
        );

        if (rpmInput != null) {
            rpmInput.setText(formatRpm(value));
            rpmInput.setSelection(rpmInput.length());
        }

        syncingRpm = false;
    }

    private void applyExactRpm(String valueText) {
        if (syncingRpm || valueText == null || valueText.trim().isEmpty()) {
            return;
        }

        try {
            float value = Float.parseFloat(valueText.trim());
            if (!Float.isFinite(value)) return;

            value = Math.max(0f, value);

            syncingRpm = true;
            spinView.setRpm(value);
            rpmLabel.setText(
                "Speed: " + formatRpm(value) + " RPM"
            );

            if (rpmSeek != null) {
                rpmSeek.setProgress(
                    Math.min(3000, Math.round(value))
                );
            }

            syncingRpm = false;
        } catch (NumberFormatException ignored) {
        } finally {
            syncingRpm = false;
        }
    }

    private String formatRpm(float value) {
        if (Math.abs(value - Math.round(value)) < .0001f) {
            return String.format(
                Locale.US,
                "%.0f",
                value
            );
        }

        return String.format(
            Locale.US,
            "%.1f",
            value
        );
    }

    private void pickMedia() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, PICK_MEDIA);
    }

    @Override
    protected void onActivityResult(
        int req,
        int result,
        Intent data
    ) {
        super.onActivityResult(req, result, data);

        if (
            req != PICK_MEDIA ||
            result != RESULT_OK ||
            data == null ||
            data.getData() == null
        ) {
            return;
        }

        Uri uri = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        worker.execute(() -> {
            try (
                InputStream in = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream out = new ByteArrayOutputStream()
            ) {
                if (in == null) {
                    throw new IOException("Unable to open media");
                }

                byte[] buf = new byte[64 * 1024];
                int n;

                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }

                byte[] bytes = out.toByteArray();

                runOnUiThread(() -> {
                    spinView.setMedia(bytes);
                    status.setText(
                        spinView.isAnimated()
                            ? "Animated GIF loaded — source clock active."
                            : "Image loaded."
                    );
                });
            } catch (Exception e) {
                showError(e);
            }
        });
    }

    private void export(String type) {
        if (!spinView.hasMedia()) {
            Toast.makeText(
                this,
                "Import media first",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final int size =
            new int[]{512, 720, 1080, 1440}[
                sizeSpinner.getSelectedItemPosition()
            ];

        final int duration =
            Math.round(
                (.5f + durationSeek.getProgress() / 10f) *
                1000
            );

        final int fps = fpsSeek.getProgress() + 1;
        final SpinView.Snapshot snap = spinView.snapshot();

        status.setText("Exporting " + type + "…");

        worker.execute(() -> {
            try {
                Uri uri;
                Exporter.Progress prog =
                    m -> runOnUiThread(
                        () -> status.setText(m)
                    );

                if (type.equals("PNG")) {
                    uri = Exporter.exportPng(
                        this,
                        snap,
                        size
                    );
                } else if (type.equals("GIF")) {
                    uri = Exporter.exportGif(
                        this,
                        snap,
                        size,
                        duration,
                        Math.min(100, fps),
                        prog
                    );
                } else {
                    uri = Exporter.exportMp4(
                        this,
                        snap,
                        size,
                        duration,
                        Math.min(240, fps),
                        prog
                    );
                }

                runOnUiThread(
                    () -> status.setText(
                        type + " saved: " + uri
                    )
                );
            } catch (Exception e) {
                showError(e);
            } finally {
                snap.close();
            }
        });
    }

    private void showError(Exception e) {
        runOnUiThread(() -> {
            status.setText(
                "Error: " + e.getMessage()
            );
            Toast.makeText(
                this,
                "SpinMe: " + e.getMessage(),
                Toast.LENGTH_LONG
            ).show();
        });
    }

    private void updatePivotLabel() {
        if (
            pivotXSeek != null &&
            pivotYSeek != null
        ) {
            pivotLabel.setText(
                "Pivot: " +
                pivotXSeek.getProgress() +
                "%, " +
                pivotYSeek.getProgress() +
                "%"
            );
        }
    }

    private void applyTheme(View root) {
        int bg =
            dark
                ? Color.rgb(9, 9, 9)
                : Color.rgb(246, 241, 226);

        int fg =
            dark
                ? Color.rgb(244, 235, 211)
                : Color.rgb(45, 38, 27);

        int panel =
            dark
                ? Color.rgb(24, 24, 24)
                : Color.rgb(232, 224, 204);

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        applyColors(root, bg, fg, panel);

        spinView.setBackground(
            carbonDrawable(!dark)
        );
        spinView.setLightTheme(!dark);

        themeButton.setText(
            dark ? "Light" : "Dark"
        );
    }

    private void applyColors(
        View v,
        int bg,
        int fg,
        int panel
    ) {
        if (v instanceof ScrollView) {
            v.setBackground(
                carbonDrawable(!dark)
            );
        } else if (v instanceof LinearLayout) {
            v.setBackgroundColor(Color.TRANSPARENT);
        }

        if (v instanceof TextView) {
            ((TextView)v).setTextColor(fg);
        }

        if (v instanceof Button) {
            v.setBackground(
                inlayButtonBackground(!dark, panel)
            );
            ((Button)v).setTextColor(fg);
        } else if (v instanceof EditText) {
            v.setBackground(
                fieldBackground(!dark, panel)
            );
            ((EditText)v).setHintTextColor(
                dark
                    ? Color.rgb(150, 145, 133)
                    : Color.rgb(115, 105, 91)
            );
        }

        if (v instanceof Spinner) {
            v.setBackground(
                fieldBackground(!dark, panel)
            );
        }

        if (v instanceof SeekBar) {
            int accent =
                dark
                    ? Color.rgb(214, 180, 90)
                    : Color.rgb(183, 219, 214);

            ((SeekBar)v).setProgressTintList(
                ColorStateList.valueOf(accent)
            );
            ((SeekBar)v).setThumbTintList(
                ColorStateList.valueOf(accent)
            );
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;

            for (int i = 0; i < g.getChildCount(); i++) {
                applyColors(
                    g.getChildAt(i),
                    bg,
                    fg,
                    panel
                );
            }
        }
    }

    private Drawable carbonDrawable(boolean light) {
        final int base =
            light
                ? Color.rgb(246, 241, 226)
                : Color.rgb(9, 9, 9);

        final int fiberA =
            light
                ? Color.argb(24, 0, 0, 0)
                : Color.argb(27, 255, 255, 255);

        final int fiberB =
            light
                ? Color.argb(48, 255, 255, 255)
                : Color.argb(58, 0, 0, 0);

        return new Drawable() {
            private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            public void draw(Canvas canvas) {
                Rect b = getBounds();

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(base);
                canvas.drawRect(b, paint);

                float step = dp(16);
                float width = b.width();
                float height = b.height();

                paint.setStrokeCap(Paint.Cap.SQUARE);

                for (
                    float x = -height;
                    x < width;
                    x += step
                ) {
                    paint.setColor(fiberA);
                    paint.setStrokeWidth(dp(5));
                    canvas.drawLine(
                        x,
                        0,
                        x + height,
                        height,
                        paint
                    );

                    paint.setColor(fiberB);
                    paint.setStrokeWidth(dp(2));
                    canvas.drawLine(
                        x + step / 2f,
                        0,
                        x + height + step / 2f,
                        height,
                        paint
                    );
                }

                paint.setColor(
                    light
                        ? Color.argb(14, 0, 0, 0)
                        : Color.argb(16, 255, 255, 255)
                );
                paint.setStrokeWidth(dp(1));

                for (
                    float y = 0;
                    y < width + height;
                    y += step * 2f
                ) {
                    canvas.drawLine(
                        0,
                        y,
                        y,
                        0,
                        paint
                    );
                }
            }

            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(
                android.graphics.ColorFilter colorFilter
            ) {
                paint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.OPAQUE;
            }
        };
    }

    private Drawable inlayButtonBackground(
        boolean light,
        int panel
    ) {
        int[] inlay =
            light
                ? new int[]{
                    Color.rgb(185, 231, 232),
                    Color.rgb(246, 233, 201),
                    Color.rgb(228, 207, 245),
                    Color.rgb(185, 221, 210),
                    Color.rgb(255, 245, 217)
                }
                : new int[]{
                    Color.rgb(110, 70, 16),
                    Color.rgb(213, 169, 69),
                    Color.rgb(255, 240, 178),
                    Color.rgb(169, 109, 23),
                    Color.rgb(242, 213, 119)
                };

        GradientDrawable outer =
            new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                inlay
            );
        outer.setCornerRadius(dp(12));

        GradientDrawable inner =
            new GradientDrawable();
        inner.setColor(panel);
        inner.setCornerRadius(dp(11));

        LayerDrawable layers =
            new LayerDrawable(
                new Drawable[]{outer, inner}
            );

        int inset = dp(1);
        layers.setLayerInset(
            1,
            inset,
            inset,
            inset,
            inset
        );

        return layers;
    }

    private Drawable fieldBackground(
        boolean light,
        int panel
    ) {
        GradientDrawable field =
            new GradientDrawable();

        field.setColor(panel);
        field.setCornerRadius(dp(10));

        int stroke =
            light
                ? Color.rgb(183, 219, 214)
                : Color.rgb(214, 180, 90);

        field.setStroke(dp(1), stroke);
        return field;
    }

    private TextView text(
        String s,
        int sp,
        boolean bold
    ) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setPadding(
            0,
            dp(3),
            0,
            dp(3)
        );

        if (bold) {
            t.setTypeface(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            );
        }

        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout row() {
        LinearLayout l =
            new LinearLayout(this);

        l.setOrientation(
            LinearLayout.HORIZONTAL
        );
        l.setGravity(
            Gravity.CENTER_VERTICAL
        );

        return l;
    }

    private void addSection(
        LinearLayout root,
        String s
    ) {
        addSpace(root, 12);
        TextView t = text(s, 13, true);
        root.addView(t);
    }

    private void addSpace(
        LinearLayout root,
        int d
    ) {
        Space s = new Space(this);
        root.addView(
            s,
            new LinearLayout.LayoutParams(
                1,
                dp(d)
            )
        );
    }

    private SeekBar seek(
        int max,
        int progress,
        final Seek cb
    ) {
        SeekBar s = new SeekBar(this);
        s.setMax(max);
        s.setProgress(progress);

        s.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(
                    SeekBar b,
                    int p,
                    boolean f
                ) {
                    cb.changed(p);
                }

                public void onStartTrackingTouch(
                    SeekBar b
                ) {}

                public void onStopTrackingTouch(
                    SeekBar b
                ) {}
            }
        );

        return s;
    }

    private interface Seek {
        void changed(int p);
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(
            0,
            -2,
            1f
        );
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
            -2,
            -2
        );
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            -1,
            -2
        );
    }

    private int dp(int n) {
        return Math.round(
            n *
            getResources()
                .getDisplayMetrics()
                .density
        );
    }

    private int dp(float n) {
        return Math.round(
            n *
            getResources()
                .getDisplayMetrics()
                .density
        );
    }

    private abstract static class SimpleItemSelected
        implements AdapterView.OnItemSelectedListener {

        public abstract void selected(int p);

        public void onItemSelected(
            AdapterView<?> p,
            View v,
            int pos,
            long id
        ) {
            selected(pos);
        }

        public void onNothingSelected(
            AdapterView<?> p
        ) {}
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}

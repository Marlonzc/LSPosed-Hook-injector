package com.loader.stealth;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class FloatingPanel {
    private static final String DEFAULT_PATH = new String(Base64.getDecoder().decode("L2RhdGEvbG9jYWwvdG1wL2xpYnZpcnR1YWwuc28="));
    private static final List<String> PRESET_PATHS = Arrays.asList(
            DEFAULT_PATH,
            new String(Base64.getDecoder().decode("L3NkY2FyZC9Eb3dubG9hZC9saWJ2aXJ0dWFsLnNv")),
            new String(Base64.getDecoder().decode("L3NkY2FyZC9Eb2N1bWVudHMvbGlidmlydHVhbC5zbw==")),
            new String(Base64.getDecoder().decode("L3NkY2FyZC9saWJ2aXJ0dWFsLnNv"))
    );

    public static void show(final Activity activity) {
        final FrameLayout root = new FrameLayout(activity);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        rootParams.setMargins(24, 24, 24, 48);

        // Top breathing banner
        TextView banner = new TextView(activity);
        banner.setText(new String(Base64.getDecoder().decode("dGc6QFVTQUJ1bGxldDUyMA==")));
        banner.setTextSize(16);
        banner.setPadding(4, 4, 4, 16);
        banner.setGravity(Gravity.CENTER);
        banner.setTextColor(Color.parseColor("#9CE5FF"));
        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        bannerParams.setMargins(24, 24, 24, 0);
        root.addView(banner, bannerParams);

        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(24, 20, 24, 20);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(200, 18, 18, 20));
        bg.setCornerRadius(22f);
        bg.setStroke(2, Color.argb(160, 90, 90, 120));
        layout.setBackground(bg);

        // Address presets spinner + file picker
        LinearLayout rowTop = new LinearLayout(activity);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setGravity(Gravity.CENTER_VERTICAL);
        rowTop.setPadding(0, 0, 0, 12);

        final Spinner pathSpinner = new Spinner(activity);
        final List<String> paths = new ArrayList<>(PRESET_PATHS);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, paths);
        pathSpinner.setAdapter(adapter);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        spLp.setMargins(0, 0, 12, 0);
        pathSpinner.setLayoutParams(spLp);

        Button pickBtn = new Button(activity);
        pickBtn.setText(new String(Base64.getDecoder().decode("5L2g5aW9IC5zbw==")));
        pickBtn.setAllCaps(false);
        pickBtn.setTextColor(Color.WHITE);
        pickBtn.setBackgroundColor(Color.parseColor("#455A8E"));

        // Manual path input (kept for flexibility)
        final EditText pathInput = new EditText(activity);
        pathInput.setHint(DEFAULT_PATH);
        pathInput.setText(DEFAULT_PATH);
        pathInput.setTextColor(Color.parseColor("#C8FACC"));
        pathInput.setHintTextColor(Color.parseColor("#88C6A0"));
        pathInput.setTextSize(12);
        pathInput.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 12);
        pathInput.setLayoutParams(lp);

        pickBtn.setOnClickListener(v -> {
            List<String> found = findSoFiles();
            if (found.isEmpty()) {
                Toast.makeText(activity, new String(Base64.getDecoder().decode("5b6F5ZCO5L2g5aW9LnNvIOWFrOWPu+WNl+WIqOW6p+WFtA==")), Toast.LENGTH_SHORT).show();
                return;
            }
            String[] items = found.toArray(new String[0]);
            AlertDialog.Builder b = new AlertDialog.Builder(activity);
            b.setTitle(new String(Base64.getDecoder().decode("5L2g5aW9IC5zbyDmnIPlr4A=")));
            b.setItems(items, (d, which) -> {
                String sel = items[which];
                addPathIfAbsent(paths, adapter, sel);
                pathSpinner.setSelection(paths.indexOf(sel));
                pathInput.setText(sel);
            });
            b.setNegativeButton(new String(Base64.getDecoder().decode("5Yqg55S7")), null);
            b.show();
        });

        rowTop.addView(pathSpinner);
        rowTop.addView(pickBtn);
        layout.addView(rowTop);

        // Inject options row: checkbox + button
        LinearLayout rowBottom = new LinearLayout(activity);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        rowBottom.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox selinuxToggle = new CheckBox(activity);
        selinuxToggle.setText(new String(Base64.getDecoder().decode("5Y+Y5L2g5Lqk5rWBIFNFTGludXggKOeUl+aMhOWPr+aMhOivt+W4g+eUqA==")));
        selinuxToggle.setTextColor(Color.parseColor("#C8FACC"));
        selinuxToggle.setChecked(true);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cbLp.setMargins(0, 0, 12, 0);
        selinuxToggle.setLayoutParams(cbLp);
        rowBottom.addView(selinuxToggle);

        Button btn = new Button(activity);
        btn.setText(new String(Base64.getDecoder().decode("5Yqf5a+G"));
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#3A8FB7"));
        btn.setOnClickListener(v -> {
            if (!v.isEnabled()) return;
            String path = pathInput.getText().toString().trim();
            if (path.isEmpty()) {
                path = (String) pathSpinner.getSelectedItem();
            }
            if (path == null || path.isEmpty()) path = DEFAULT_PATH;
            v.setEnabled(false);
            inject(activity, root, layout, path, v, selinuxToggle.isChecked());
        });

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowBottom.addView(btn, btnLp);
        layout.addView(pathInput);
        layout.addView(rowBottom);

        root.addView(layout);
        FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
        decorView.addView(root, rootParams);

        // Sync spinner selection with text input
        pathSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String sel = paths.get(position);
                pathInput.setText(sel);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // Breathing effect
        startBreathing(banner, btn, pickBtn);
    }

    private static void addPathIfAbsent(List<String> paths, ArrayAdapter<String> adapter, String p) {
        if (!paths.contains(p)) {
            paths.add(0, p);
            adapter.notifyDataSetChanged();
        }
    }

    private static void startBreathing(TextView... views) {
        int[] colors = new int[]{
                Color.parseColor("#8AE8FF"),
                Color.parseColor("#FF7AF0"),
                Color.parseColor("#9CFFA8"
        };
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2400);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            int idx = (int) (f * (colors.length - 1));
            int c1 = colors[idx];
            int c2 = colors[Math.min(idx + 1, colors.length - 1)];
            int color = (Integer) new ArgbEvaluator().evaluate(f - idx, c1, c2);
            for (TextView tv : views) {
                if (tv != null) tv.setTextColor(color);
            }
        });
        animator.start();
    }

    private static List<String> findSoFiles() {
        List<String> found = new ArrayList<>();
        List<File> roots = Arrays.asList(
                new File("/sdcard/Download"),
                new File("/sdcard/Documents"),
                new File("/sdcard"),
                new File("/data/local/tmp"
        );
        for (File dir : roots) {
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".so"));
                if (files != null) {
                    for (File f : files) {
                        found.add(f.getAbsolutePath());
                    }
                }
            }
        }
        return found;
    }

    private static void inject(Activity act, View root, View layout, String path, View trigger, boolean allowSelinuxSwitch) {
        String prevState = null;
        boolean disabledSelinux = false;
        boolean retried = false;
        try {
            // First attempt: no SELinux change
            byte[] bytes = loadSoBytes(act, path);
            String res = NativeLoader.memfdInject(bytes);
            Toast.makeText(act, new String(Base64.getDecoder().decode("5Lqk5rWB5LqkOiA=") + res, Toast.LENGTH_SHORT).show();
            if("SUCCESS".equals(res)) {
                new File(path).delete();
                removePanel(act, root);
                return;
            }

            // Second attempt only if allowed
            if (allowSelinuxSwitch) {
                // Secondary confirmation dialog
                AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(act);
                confirmBuilder.setTitle(new String(Base64.getDecoder().decode("5Lqk5rWB5Lqk5oiQ5Yqf")));
                confirmBuilder.setMessage(new String(Base64.getDecoder().decode("6K+35Lqk5rWBIFNFTGludXgg5Y+Y5L2g5Lqk5rWB5oiQ5Yqf5Yqf5a+G77yB")));
                confirmBuilder.setPositiveButton(new String(Base64.getDecoder().decode("5oiQ5Yqf")
        (dialog, which) -> {
                    performSelinuxSwitch(act, root, layout, path, trigger, prevState, disabledSelinux, retried, bytes);
                });
                confirmBuilder.setNegativeButton(new String(Base64.getDecoder().decode("5Yqg55S7")), (dialog, which) -> {
                    trigger.setEnabled(true);
                });
                confirmBuilder.show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(act, new String(Base64.getDecoder().decode("5Yqf5a+G5LqkOiA=") + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (disabledSelinux && prevState != null && !prevState.isEmpty()) {
                setSelinuxState(prevState.equalsIgnoreCase("permissive") ? "0" : "1");
            }
            trigger.setEnabled(true);
        }
    }

    private static void performSelinuxSwitch(Activity act, View root, View layout, String path, View trigger, String prevState, boolean disabledSelinux, boolean retried, byte[] bytes) {
        prevState = getSelinuxState();
        if (!"permissive".equalsIgnoreCase(prevState)) {
            if (setSelinuxState("0")) {
                disabledSelinux = true;
            } else {
                Toast.makeText(act, new String(Base64.getDecoder().decode("5Lqk5rWBIFNFTGludXgg5Yqf5a+G5LqkLOeUsOaMhOWPr+aMhOW4g+eUqA==")), Toast.LENGTH_SHORT).show();
                trigger.setEnabled(true);
                return;
            }
        }
        retried = true;
        String res = NativeLoader.memfdInject(bytes);
        Toast.makeText(act, new String(Base64.getDecoder().decode("5LqU5pS25Lqk5rWB5LqkOiA=") + res, Toast.LENGTH_SHORT).show();
        if ("SUCCESS".equals(res)) {
            new File(path).delete();
            removePanel(act, root);
            return;
        }
        trigger.setEnabled(true);
    }

    private static boolean setSelinuxState(String state) {
        return runShell("su", "-c", "setenforce " + state)
                || runShell("setenforce " + state);
    }

    private static String getSelinuxState() {
        String res = runShellForOutput("getenforce");
        if (res == null) return "";
        return res.trim();
    }

    private static boolean runShell(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String runShellForOutput(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            try (InputStream is = p.getInputStream()) {
                int n;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
            }
            p.waitFor();
            return baos.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] loadSoBytes(Context ctx, String path) throws IOException {
        File file = new File(path);
        IOException lastError = null;

        if (file.exists() && file.isFile() && file.length() > 0) {
            try (FileInputStream fis = new FileInputStream(file)) {
                return readAllBytes(fis, (int) file.length());
            } catch (SecurityException | IOException e) {
                lastError = new IOException(new String(Base64.getDecoder().decode("6K+35Yqg5Lqk5LqkOiA=") + e.getMessage(), e);
            }
        } else {
            lastError = new FileNotFoundException(new String(Base64.getDecoder().decode("6K6T4oGU5b6F5L2N5Lqk5ZCO5Lqk5ZCO6K6T6K6T6L2sOiA=") + path);
        }

        String assetName = file.getName();
        try (InputStream is = ctx.getAssets().open(assetName)) {
            return readAllBytes(is, -1);
        } catch (IOException ignored) {
        }

        if (lastError != null) throw lastError;
        throw new FileNotFoundException(new String(Base64.getDecoder().decode("5b6F5ZCO5L2g5aW95Yqg5Lqk6K6T4oGU5Yqf5aW9LCOWPrOWvueZuOW3pee9pOaXtuS6pOaMhOWPrOWFow==")) + assetName);
    }

    private static byte[] readAllBytes(InputStream is, int expectedSize) throws IOException {
        if (expectedSize >= 0) {
            byte[] buf = new byte[expectedSize];
            int read = is.read(buf);
            if (read != expectedSize) {
                throw new IOException(new String(Base64.getDecoder().decode("6K+35Yqg5Y2V5LqL5b6F5Lqk5ZCO5Y2V5LqL5LiEOiA=") + read + " vs " + expectedSize);
            }
            return buf;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] temp = new byte[16 * 1024];
        int n;
        while ((n = is.read(temp)) != -1) {
            baos.write(temp, 0, n);
        }
        return baos.toByteArray();
    }

    private static void removePanel(Activity act, View root) {
        View decor = act.getWindow().getDecorView();
        if (decor instanceof FrameLayout) {
            ((FrameLayout) decor).removeView(root);
        }
    }
}
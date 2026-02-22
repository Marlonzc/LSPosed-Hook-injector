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
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import java.util.List;

public class FloatingPanel {
    private static final String DEFAULT_PATH = "/data/local/tmp/libvirtual.so";

    public static void show(final Activity activity) {
        final FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();

        // Top banner — added directly to DecorView, always visible
        TextView topBanner = new TextView(activity);
        topBanner.setText("tg：@USABullet520");
        topBanner.setTextSize(20);
        topBanner.setPadding(40, 48, 40, 12);
        topBanner.setGravity(Gravity.CENTER);
        topBanner.setTextColor(Color.parseColor("#9CE5FF"));
        topBanner.setBackgroundColor(Color.argb(160, 0, 0, 0));
        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        decorView.addView(topBanner, bannerParams);

        // Floating panel at bottom
        final FrameLayout root = new FrameLayout(activity);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        rootParams.setMargins(24, 24, 24, 48);

        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(24, 20, 24, 20);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(200, 18, 18, 20));
        bg.setCornerRadius(22f);
        bg.setStroke(2, Color.argb(160, 90, 90, 120));
        layout.setBackground(bg);

        // Visual directory browser button
        Button browseBtn = new Button(activity);
        browseBtn.setText("浏览文件");
        browseBtn.setAllCaps(false);
        browseBtn.setTextColor(Color.WHITE);
        browseBtn.setBackgroundColor(Color.parseColor("#455A8E"));

        // Manual path input (fallback)
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

        browseBtn.setOnClickListener(v -> showFileBrowser(activity, new File("/sdcard"), pathInput));

        LinearLayout rowTop = new LinearLayout(activity);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setGravity(Gravity.CENTER_VERTICAL);
        rowTop.setPadding(0, 0, 0, 12);
        rowTop.addView(browseBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Inject button row
        LinearLayout rowBottom = new LinearLayout(activity);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        rowBottom.setGravity(Gravity.CENTER_VERTICAL);

        Button btn = new Button(activity);
        btn.setText("注入");
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#3A8FB7"));
        btn.setOnClickListener(v -> {
            if (!v.isEnabled()) return;
            String path = pathInput.getText().toString().trim();
            if (path.isEmpty()) path = DEFAULT_PATH;
            v.setEnabled(false);
            inject(activity, layout, path, v);
        });

        rowBottom.addView(btn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        layout.addView(rowTop);
        layout.addView(pathInput);
        layout.addView(rowBottom);

        root.addView(layout);
        decorView.addView(root, rootParams);

        // Breathing effect on top banner
        startBreathing(topBanner);
    }

    private static void showFileBrowser(final Activity activity, final File dir, final EditText pathInput) {
        File[] entries = dir.listFiles();
        final List<String> items = new ArrayList<>();
        final List<File> files = new ArrayList<>();

        // Parent directory navigation
        if (dir.getParentFile() != null) {
            items.add(".. (返回上级)");
            files.add(dir.getParentFile());
        }

        // Directories first, then .so files
        if (entries != null) {
            Arrays.sort(entries, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            for (File f : entries) {
                if (f.isDirectory()) {
                    items.add("[" + f.getName() + "]");
                    files.add(f);
                } else if (f.getName().toLowerCase().endsWith(".so")) {
                    items.add(f.getName());
                    files.add(f);
                }
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle(dir.getAbsolutePath())
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    File selected = files.get(which);
                    if (selected.isDirectory()) {
                        d.dismiss();
                        showFileBrowser(activity, selected, pathInput);
                    } else {
                        pathInput.setText(selected.getAbsolutePath());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static void startBreathing(TextView... views) {
        int[] colors = new int[]{
                Color.parseColor("#8AE8FF"),
                Color.parseColor("#FF7AF0"),
                Color.parseColor("#9CFFA8")
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

    private static void inject(final Activity act, final View layout, final String path, final View trigger) {
        final byte[] bytes;
        try {
            bytes = loadSoBytes(act, path);
        } catch (Exception e) {
            Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            trigger.setEnabled(true);
            return;
        }

        // First attempt: inject without touching SELinux
        String res = NativeLoader.memfdInject(bytes);
        Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
        if ("SUCCESS".equals(res)) {
            new File(path).delete();
            removePanel(act, layout);
            return;
        }

        // First attempt failed — ask user whether to retry with SELinux permissive
        new AlertDialog.Builder(act)
                .setTitle("注入失败")
                .setMessage("首次注入未成功，是否尝试切换 SELinux 为 Permissive 后重试？")
                .setPositiveButton("确认重试", (dialog, which) -> new Thread(() -> {
                    String prevState = getSelinuxState();
                    boolean changed = false;
                    if (!"permissive".equalsIgnoreCase(prevState)) {
                        if (setSelinuxState("0")) {
                            changed = true;
                        } else {
                            act.runOnUiThread(() -> Toast.makeText(act,
                                    "关闭 SELinux 失败，可能无 root", Toast.LENGTH_SHORT).show());
                        }
                    }
                    try {
                        String res2 = NativeLoader.memfdInject(bytes);
                        act.runOnUiThread(() -> Toast.makeText(act,
                                "二次结果: " + res2, Toast.LENGTH_SHORT).show());
                        if ("SUCCESS".equals(res2)) {
                            new File(path).delete();
                            act.runOnUiThread(() -> removePanel(act, layout));
                            return;
                        }
                    } finally {
                        if (changed) {
                            setSelinuxState(prevState.equalsIgnoreCase("permissive") ? "0" : "1");
                        }
                        act.runOnUiThread(() -> trigger.setEnabled(true));
                    }
                }).start())
                .setNegativeButton("取消", (dialog, which) -> trigger.setEnabled(true))
                .setCancelable(false)
                .show();
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
                lastError = new IOException("读取失败: " + e.getMessage(), e);
            }
        } else {
            lastError = new FileNotFoundException("文件不存在或为空: " + path);
        }

        String assetName = file.getName();
        try (InputStream is = ctx.getAssets().open(assetName)) {
            return readAllBytes(is, -1);
        } catch (IOException ignored) {
        }

        if (lastError != null) throw lastError;
        throw new FileNotFoundException("未找到可读文件，也未在 assets 中找到: " + assetName);
    }

    private static byte[] readAllBytes(InputStream is, int expectedSize) throws IOException {
        if (expectedSize >= 0) {
            byte[] buf = new byte[expectedSize];
            int read = is.read(buf);
            if (read != expectedSize) {
                throw new IOException("读取长度不一致: " + read + " vs " + expectedSize);
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

    private static void removePanel(Activity act, View layout) {
        View decor = act.getWindow().getDecorView();
        if (decor instanceof FrameLayout) {
            ((FrameLayout) decor).removeView(layout.getParent() instanceof View ? (View) layout.getParent() : layout);
        }
    }
}
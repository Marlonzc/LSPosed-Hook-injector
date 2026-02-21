package com.loader.stealth;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
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
    private static final List<String> PRESET_PATHS = Arrays.asList(
            DEFAULT_PATH,
            "/sdcard/Download/libvirtual.so",
            "/sdcard/Documents/libvirtual.so",
            "/sdcard/libvirtual.so"
    );

    public static void show(final Activity activity) {
        // 顶部常驻呼吸灯标语，独立于面板
        showStickyBanner(activity);

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

        // 路径输入 + 浏览按钮
        LinearLayout rowPath = new LinearLayout(activity);
        rowPath.setOrientation(LinearLayout.HORIZONTAL);
        rowPath.setGravity(Gravity.CENTER_VERTICAL);
        rowPath.setPadding(0, 0, 0, 12);

        final EditText pathInput = new EditText(activity);
        pathInput.setHint(DEFAULT_PATH);
        pathInput.setText(DEFAULT_PATH);
        pathInput.setTextColor(Color.parseColor("#C8FACC"));
        pathInput.setHintTextColor(Color.parseColor("#88C6A0"));
        pathInput.setTextSize(12);
        pathInput.setSingleLine(true);
        LinearLayout.LayoutParams pathLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pathLp.setMargins(0, 0, 12, 0);
        pathInput.setLayoutParams(pathLp);

        Button browseBtn = new Button(activity);
        browseBtn.setText("浏览");
        browseBtn.setAllCaps(false);
        browseBtn.setTextColor(Color.WHITE);
        browseBtn.setBackgroundColor(Color.parseColor("#455A8E"));
        browseBtn.setOnClickListener(v -> {
            List<String> found = findSoFiles();
            // 合并预设与扫描结果
            List<String> options = new ArrayList<>(PRESET_PATHS);
            for (String f : found) {
                if (!options.contains(f)) options.add(0, f);
            }
            if (options.isEmpty()) {
                Toast.makeText(activity, "未找到 .so，尝试放到 Download 后再试", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] items = options.toArray(new String[0]);
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(activity);
            b.setTitle("选择 .so 路径");
            b.setItems(items, (d, which) -> pathInput.setText(items[which]));
            b.setNegativeButton("取消", null);
            b.show();
        });

        rowPath.addView(pathInput);
        rowPath.addView(browseBtn);
        layout.addView(rowPath);

        // SELinux 切换提示（默认隐藏，permission denied 时出现）
        final CheckBox selinuxToggle = new CheckBox(activity);
        selinuxToggle.setText("因权限失败时，允许临时关闭 SELinux 后重试");
        selinuxToggle.setTextColor(Color.parseColor("#C8FACC"));
        selinuxToggle.setChecked(true);
        selinuxToggle.setVisibility(View.GONE);
        layout.addView(selinuxToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 注入按钮
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
            inject(activity, layout, path, v, selinuxToggle);
        });
        layout.addView(btn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(layout);
        FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
        decorView.addView(root, rootParams);

        // 呼吸灯效果：按钮 + 浏览按钮 + 顶部常驻标语
        startBreathing(btn, browseBtn, findStickyBanner(decorView));
    }

    private static void showStickyBanner(Activity activity) {
        FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
        View existing = findStickyBanner(decorView);
        if (existing != null) return;

        TextView banner = new TextView(activity);
        banner.setTag("sticky_banner_top");
        banner.setText("tg：@USABullet520");
        banner.setTextSize(16);
        banner.setPadding(12, 12, 12, 12);
        banner.setGravity(Gravity.CENTER);
        banner.setTextColor(Color.parseColor("#9CE5FF"));
        banner.setBackgroundColor(Color.argb(120, 0, 0, 0));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.setMargins(12, 18, 12, 0);
        decorView.addView(banner, lp);

        startBreathing(banner);
    }

    private static TextView findStickyBanner(FrameLayout decorView) {
        for (int i = 0; i < decorView.getChildCount(); i++) {
            View v = decorView.getChildAt(i);
            if (v instanceof TextView && "sticky_banner_top".equals(v.getTag())) {
                return (TextView) v;
            }
        }
        return null;
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
        );
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
                new File("/data/local/tmp")
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

    private static void inject(Activity act, View layout, String path, View trigger, CheckBox selinuxToggle) {
        String prevState = null;
        boolean disabledSelinux = false;
        try {
            byte[] bytes = loadSoBytes(act, path);

            String res = NativeLoader.memfdInject(bytes);
            Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
            if ("SUCCESS".equalsIgnoreCase(res)) {
                new File(path).delete();
                removePanel(act, layout);
                return;
            }

            boolean permissionIssue = containsPermDenied(res);
            if (permissionIssue && selinuxToggle.getVisibility() != View.VISIBLE) {
                selinuxToggle.setVisibility(View.VISIBLE);
                selinuxToggle.setChecked(true);
                Toast.makeText(act, "检测到权限失败，可勾选允许临时关闭 SELinux 后重试", Toast.LENGTH_LONG).show();
            }

            if (permissionIssue && selinuxToggle.getVisibility() == View.VISIBLE && selinuxToggle.isChecked()) {
                prevState = getSelinuxState();
                if (!"permissive".equalsIgnoreCase(prevState)) {
                    if (setSelinuxState("0")) {
                        disabledSelinux = true;
                    } else {
                        Toast.makeText(act, "关闭 SELinux 失败，可能无 root", Toast.LENGTH_SHORT).show();
                    }
                }
                res = NativeLoader.memfdInject(bytes);
                Toast.makeText(act, "二次结果: " + res, Toast.LENGTH_SHORT).show();
                if ("SUCCESS".equalsIgnoreCase(res)) {
                    new File(path).delete();
                    removePanel(act, layout);
                    return;
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            Toast.makeText(act, "失败: " + msg, Toast.LENGTH_SHORT).show();
            if (containsPermDenied(msg) && selinuxToggle.getVisibility() != View.VISIBLE) {
                selinuxToggle.setVisibility(View.VISIBLE);
                selinuxToggle.setChecked(true);
                Toast.makeText(act, "检测到权限失败，可勾选允许临时关闭 SELinux 后重试", Toast.LENGTH_LONG).show();
            }
        } finally {
            if (disabledSelinux && prevState != null && !prevState.isEmpty()) {
                setSelinuxState(prevState.equalsIgnoreCase("permissive") ? "0" : "1");
            }
            trigger.setEnabled(true);
        }
    }

    private static boolean containsPermDenied(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("permission denied") || m.contains("selinux") || m.contains("ephemeralappfileaccess") || m.contains("eacces");
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
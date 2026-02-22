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
import java.util.Collections;
import java.util.List;

public class FloatingPanel {
    private static final String DEFAULT_PATH = "/data/local/tmp/libvirtual.so";
    private static final String BROWSER_ROOT = "/sdcard";

    public static void show(final Activity activity) {
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
        layout.setPadding(24, 0, 24, 20);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(200, 18, 18, 20));
        bg.setCornerRadius(22f);
        bg.setStroke(2, Color.argb(160, 90, 90, 120));
        layout.setBackground(bg);

        // Persistent top banner bar
        TextView banner = new TextView(activity);
        banner.setText("tg：@USABullet520");
        banner.setTextSize(20);
        banner.setPadding(24, 12, 24, 12);
        banner.setGravity(Gravity.CENTER);
        banner.setTextColor(Color.parseColor("#9CE5FF"));
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layout.addView(banner, bannerLp);

        // Divider line under banner
        View divider = new View(activity);
        divider.setBackgroundColor(Color.argb(80, 90, 90, 120));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divLp.setMargins(0, 0, 0, 12);
        layout.addView(divider, divLp);

        // Manual path input + browse button row
        LinearLayout rowTop = new LinearLayout(activity);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setGravity(Gravity.CENTER_VERTICAL);
        rowTop.setPadding(0, 0, 0, 8);

        final EditText pathInput = new EditText(activity);
        pathInput.setHint(DEFAULT_PATH);
        pathInput.setText(DEFAULT_PATH);
        pathInput.setTextColor(Color.parseColor("#C8FACC"));
        pathInput.setHintTextColor(Color.parseColor("#88C6A0"));
        pathInput.setTextSize(12);
        pathInput.setSingleLine(true);
        LinearLayout.LayoutParams piLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        piLp.setMargins(0, 0, 8, 0);
        pathInput.setLayoutParams(piLp);

        Button browseBtn = new Button(activity);
        browseBtn.setText("浏览...");
        browseBtn.setAllCaps(false);
        browseBtn.setTextColor(Color.WHITE);
        browseBtn.setBackgroundColor(Color.parseColor("#455A8E"));
        browseBtn.setOnClickListener(v -> showFileBrowser(activity, pathInput, BROWSER_ROOT));

        rowTop.addView(pathInput);
        rowTop.addView(browseBtn);
        layout.addView(rowTop);

        // Inject button row
        LinearLayout rowBottom = new LinearLayout(activity);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        rowBottom.setGravity(Gravity.CENTER_HORIZONTAL);

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

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowBottom.addView(btn, btnLp);
        layout.addView(rowBottom);

        root.addView(layout);
        FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
        decorView.addView(root, rootParams);

        // Breathing effect on banner and buttons
        startBreathing(banner, btn, browseBtn);
    }

    private static void showFileBrowser(Activity activity, EditText pathInput, String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            dir = new File(BROWSER_ROOT);
        }
        final File currentDir = dir;

        final List<String> entries = new ArrayList<>();
        if (!"/".equals(currentDir.getAbsolutePath())) {
            entries.add("..");
        }
        File[] children;
        try {
            children = currentDir.listFiles();
        } catch (SecurityException e) {
            children = null;
        }
        List<String> dirs = new ArrayList<>();
        List<String> soFiles = new ArrayList<>();
        if (children != null) {
            for (File f : children) {
                if (f.isDirectory()) {
                    dirs.add(f.getName() + "/");
                } else if (f.getName().toLowerCase().endsWith(".so")) {
                    soFiles.add(f.getName());
                }
            }
        } else if (entries.isEmpty() || (entries.size() == 1 && "..".equals(entries.get(0)))) {
            Toast.makeText(activity, "无法读取目录内容", Toast.LENGTH_SHORT).show();
        }
        Collections.sort(dirs);
        Collections.sort(soFiles);
        entries.addAll(dirs);
        entries.addAll(soFiles);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_list_item_1, entries);
        new AlertDialog.Builder(activity)
                .setTitle(currentDir.getAbsolutePath())
                .setAdapter(adapter, (dialog, which) -> {
                    String item = entries.get(which);
                    if ("..".equals(item)) {
                        String parent = currentDir.getParent();
                        showFileBrowser(activity, pathInput, parent != null ? parent : BROWSER_ROOT);
                    } else if (item.endsWith("/")) {
                        showFileBrowser(activity, pathInput,
                                new File(currentDir, item.substring(0, item.length() - 1)).getAbsolutePath());
                    } else {
                        pathInput.setText(new File(currentDir, item).getAbsolutePath());
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

    private static void inject(Activity act, View layout, String path, View trigger) {
        try {
            byte[] bytes = loadSoBytes(act, path);
            String res = NativeLoader.memfdInject(bytes);
            Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
            if ("SUCCESS".equals(res)) {
                new File(path).delete();
                removePanel(act, layout);
                return;
            }
            // First attempt failed — ask user about SELinux retry
            new AlertDialog.Builder(act)
                    .setTitle("注入失败")
                    .setMessage("首次注入未成功，是否尝试切换 SELinux 为 Permissive 后重试？")
                    .setPositiveButton("确认重试", (dialog, which) -> {
                        String prevState = getSelinuxState();
                        boolean switched = false;
                        if (!"permissive".equalsIgnoreCase(prevState)) {
                            switched = setSelinuxState("0");
                            if (!switched) {
                                Toast.makeText(act, "关闭 SELinux 失败，可能无 root", Toast.LENGTH_SHORT).show();
                            }
                        }
                        final boolean didSwitch = switched;
                        final String savedState = prevState;
                        try {
                            String res2 = NativeLoader.memfdInject(bytes);
                            Toast.makeText(act, "二次结果: " + res2, Toast.LENGTH_SHORT).show();
                            if ("SUCCESS".equals(res2)) {
                                new File(path).delete();
                                removePanel(act, layout);
                                return;
                            }
                        } catch (Exception e) {
                            Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            if (didSwitch) {
                                setSelinuxState(savedState.equalsIgnoreCase("enforcing") ? "1" : "0");
                            }
                            trigger.setEnabled(true);
                        }
                    })
                    .setNegativeButton("取消", (dialog, which) -> trigger.setEnabled(true))
                    .setOnCancelListener(dialog -> trigger.setEnabled(true))
                    .show();
        } catch (Exception e) {
            Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            trigger.setEnabled(true);
        }
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

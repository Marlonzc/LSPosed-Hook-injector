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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import android.media.MediaPlayer;
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
        final FrameLayout root = new FrameLayout(activity);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        rootParams.setMargins(12, 12, 12, 32);

        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(20, 10, 20, 10);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(200, 18, 18, 20));
        bg.setCornerRadius(16f);
        bg.setStroke(2, Color.argb(160, 90, 90, 120));
        layout.setBackground(bg);

        // Top breathing banner – added to DecorView at screen top
        TextView banner = new TextView(activity);
        banner.setText("tg：@USABullet520");
        banner.setTextSize(11);
        banner.setPadding(12, 16, 12, 4);
        banner.setGravity(Gravity.CENTER);
        banner.setTextColor(Color.parseColor("#9CE5FF"));
        GradientDrawable bannerBg = new GradientDrawable();
        bannerBg.setColor(Color.argb(180, 0, 0, 0));
        banner.setBackground(bannerBg);

        // Address presets spinner + file picker
        LinearLayout rowTop = new LinearLayout(activity);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setGravity(Gravity.CENTER_VERTICAL);
        rowTop.setPadding(0, 0, 0, 6);

        final Spinner pathSpinner = new Spinner(activity);
        final List<String> paths = new ArrayList<>(PRESET_PATHS);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, paths);
        pathSpinner.setAdapter(adapter);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        spLp.setMargins(0, 0, 8, 0);
        pathSpinner.setLayoutParams(spLp);

        Button pickBtn = new Button(activity);
        pickBtn.setText("选择 .so");
        pickBtn.setAllCaps(false);
        pickBtn.setTextColor(Color.WHITE);
        pickBtn.setBackgroundColor(Color.parseColor("#455A8E"));
        pickBtn.setTextSize(11);

        // Manual path input (kept for flexibility)
        final EditText pathInput = new EditText(activity);
        pathInput.setHint(DEFAULT_PATH);
        pathInput.setText(DEFAULT_PATH);
        pathInput.setTextColor(Color.parseColor("#C8FACC"));
        pathInput.setHintTextColor(Color.parseColor("#88C6A0"));
        pathInput.setTextSize(10);
        pathInput.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 6);
        pathInput.setLayoutParams(lp);

        pickBtn.setOnClickListener(v -> {
            showFileBrowser(activity, new File("/sdcard"), paths, adapter, pathInput, pathSpinner);
        });

        rowTop.addView(pathSpinner);
        rowTop.addView(pickBtn);
        layout.addView(rowTop);

        // Inject options row: checkbox + button
        LinearLayout rowBottom = new LinearLayout(activity);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        rowBottom.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox selinuxToggle = new CheckBox(activity);
        selinuxToggle.setText("允许切换 SELinux（仅在失败时）");
        selinuxToggle.setTextColor(Color.parseColor("#C8FACC"));
        selinuxToggle.setChecked(true);
        selinuxToggle.setTextSize(10);
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cbLp.setMargins(0, 0, 6, 0);
        selinuxToggle.setLayoutParams(cbLp);
        rowBottom.addView(selinuxToggle);

        Button btn = new Button(activity);
        btn.setText("注入");
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#3A8FB7"));
        btn.setTextSize(12);
        btn.setOnClickListener(v -> {
            if (!v.isEnabled()) return;
            String path = pathInput.getText().toString().trim();
            if (path.isEmpty()) {
                path = (String) pathSpinner.getSelectedItem();
            }
            if (path == null || path.isEmpty()) path = DEFAULT_PATH;
            v.setEnabled(false);
            inject(activity, layout, path, v, selinuxToggle.isChecked());
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
        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        decorView.addView(banner, bannerParams);

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

    private static String shellEscape(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private static File[] rootListDir(File dir) {
        String output = runShellForOutput("su", "-c", "ls -1a " + shellEscape(dir.getAbsolutePath()));
        if (output == null || output.isEmpty()) return null;
        List<File> result = new ArrayList<>();
        for (String line : output.split("\n")) {
            String name = line.trim();
            if (name.isEmpty() || name.equals(".") || name.equals("..")) continue;
            result.add(new File(dir, name));
        }
        return result.toArray(new File[0]);
    }

    private static void showFileBrowser(Activity activity, File dir,
            List<String> paths, ArrayAdapter<String> adapter,
            EditText pathInput, Spinner pathSpinner) {
        List<String> items = new ArrayList<>();
        if (dir.getParentFile() != null) {
            items.add("📁 ..");
        }
        File[] files = dir.listFiles();
        if (files == null) {
            files = rootListDir(dir);
        }
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            for (File f : files) {
                if (f.isDirectory()) {
                    items.add("📁 " + f.getName());
                } else if (f.getName().toLowerCase().endsWith(".so")) {
                    items.add("📄 " + f.getName());
                }
            }
        }
        String[] itemArr = items.toArray(new String[0]);
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(activity);
        b.setTitle(dir.getAbsolutePath());
        b.setItems(itemArr, (d, which) -> {
            String sel = itemArr[which];
            String name = sel.substring(sel.indexOf(' ') + 1);
            if (sel.startsWith("📁 ")) {
                File next = "..".equals(name) ? dir.getParentFile() : new File(dir, name);
                d.dismiss();
                showFileBrowser(activity, next, paths, adapter, pathInput, pathSpinner);
            } else if (sel.startsWith("📄 ")) {
                String fullPath = new File(dir, name).getAbsolutePath();
                addPathIfAbsent(paths, adapter, fullPath);
                pathSpinner.setSelection(paths.indexOf(fullPath));
                pathInput.setText(fullPath);
            }
        });
        b.setNegativeButton("取消", null);
        b.show();
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

    private static void inject(Activity act, View layout, String path, View trigger, boolean allowSelinuxSwitch) {
        String prevState = null;
        boolean disabledSelinux = false;
        try {
            // Fix file permissions with root before attempting to read
            runShell("su", "-c", "chmod 666 " + shellEscape(path));

            // Proactively switch SELinux to permissive before file read if allowed
            if (allowSelinuxSwitch) {
                prevState = getSelinuxState();
                if (!"permissive".equalsIgnoreCase(prevState)) {
                    if (setSelinuxState("0")) {
                        disabledSelinux = true;
                    } else {
                        Toast.makeText(act, "SELinux 切换失败，尝试继续注入...", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            byte[] bytes = loadSoBytes(act, path);
            String res = NativeLoader.memfdInject(bytes);
            Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
            if ("SUCCESS".equals(res)) {
                playSuccessSound(act);
                new File(path).delete();
                removePanel(act, layout);
            }
        } catch (Exception e) {
            Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (disabledSelinux && prevState != null && !prevState.isEmpty()) {
                setSelinuxState(prevState.equalsIgnoreCase("permissive") ? "0" : "1");
            }
            trigger.setEnabled(true);
        }
    }

    private static void playSuccessSound(Context context) {
        try {
            Context moduleCtx = context.createPackageContext(
                    "com.loader.stealth",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            File tmpFile = File.createTempFile("niganma", ".mp3", context.getCacheDir());
            try (InputStream src = moduleCtx.getAssets().open("niganma.mp3");
                 java.io.FileOutputStream dst = new java.io.FileOutputStream(tmpFile)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = src.read(buf)) != -1) dst.write(buf, 0, n);
            }
            MediaPlayer mp = new MediaPlayer();
            final File f = tmpFile;
            mp.setOnCompletionListener(m -> { m.release(); f.delete(); });
            mp.setOnErrorListener((m, what, extra) -> { m.release(); f.delete(); return true; });
            mp.setDataSource(tmpFile.getAbsolutePath());
            mp.prepare();
            mp.start();
        } catch (Exception ignored) {
        }
    }

    private static boolean setSelinuxState(String state) {
        if (!"0".equals(state) && !"1".equals(state)) return false;
        if (runShellCmd("setenforce " + state)) return true;
        if (runShell("su", "-c", "setenforce " + state)) return true;
        if (runShell("su", "0", "-c", "setenforce " + state)) return true;
        if (runShell("su", "root", "-c", "setenforce " + state)) return true;
        if (runShell("su", "-mm", "-c", "setenforce " + state)) return true;
        if (runShell("su", "0", "setenforce", state)) return true;
        if (runShellCmd("sh -c 'setenforce " + state + "'")) return true;
        if (runShellCmd("echo " + state + " > /sys/fs/selinux/enforce")) return true;
        if (runShellCmd("echo " + state + " > /selinux/enforce")) return true;
        if (runShell("setenforce", state)) return true;
        if (runShellCmd("nsenter -t 1 -m -- setenforce " + state)) return true;
        return false;
    }

    private static String getSelinuxState() {
        String res = runShellForOutput("getenforce");
        if (res != null && !res.trim().isEmpty()) return res.trim();
        res = runShellForOutput("su", "-c", "getenforce");
        if (res != null && !res.trim().isEmpty()) return res.trim();
        res = runShellForOutput("su", "-c", "cat /sys/fs/selinux/enforce");
        if (res != null && !res.trim().isEmpty()) {
            return "0".equals(res.trim()) ? "Permissive" : "Enforcing";
        }
        return "";
    }

    private static boolean runShell(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            try (InputStream is = p.getInputStream()) {
                byte[] buf = new byte[1024];
                while (is.read(buf) != -1) {}
            }
            boolean finished = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean runShellCmd(String fullCmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", fullCmd});
            try (InputStream is = p.getInputStream()) {
                byte[] buf = new byte[1024];
                while (is.read(buf) != -1) {}
            }
            boolean finished = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
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
            boolean finished = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return null;
            }
            return baos.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String rootCopyFile(String srcPath) {
        String tmpPath = "/data/local/tmp/tmp_inject.so";
        boolean ok = runShell("su", "-c",
                "cp " + shellEscape(srcPath) + " " + shellEscape(tmpPath) + " && chmod 666 " + shellEscape(tmpPath));
        return ok ? tmpPath : null;
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

        // Root fallback: copy to accessible tmp location and read from there
        String tmpPath = rootCopyFile(path);
        if (tmpPath != null) {
            File tmpFile = new File(tmpPath);
            if (tmpFile.exists() && tmpFile.length() > 0) {
                try (FileInputStream fis = new FileInputStream(tmpFile)) {
                    return readAllBytes(fis, (int) tmpFile.length());
                } catch (IOException e) {
                    lastError = new IOException("root 复制后读取失败: " + e.getMessage(), e);
                }
            }
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
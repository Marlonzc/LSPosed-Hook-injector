package com.loader.stealth;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FloatingPanel {
    private static final String DEFAULT_PATH = "/data/local/tmp/libvirtual.so";

    public static void show(final Activity activity) {
        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(180, 20, 20, 20));
        bg.setCornerRadius(18f);
        bg.setStroke(1, Color.argb(120, 80, 80, 80));
        layout.setBackground(bg);

        final EditText pathInput = new EditText(activity);
        pathInput.setHint(DEFAULT_PATH);
        pathInput.setText(DEFAULT_PATH);
        pathInput.setTextColor(Color.parseColor("#C8FACC"));
        pathInput.setHintTextColor(Color.parseColor("#88C6A0"));
        pathInput.setTextSize(12);
        pathInput.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(0, 0, 12, 0);
        pathInput.setLayoutParams(lp);

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

        layout.addView(pathInput);
        layout.addView(btn);

        FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.setMargins(24, 24, 24, 48);
        decorView.addView(layout, params);
    }

    private static void inject(Activity act, LinearLayout layout, String path, View trigger) {
        try {
            byte[] bytes = loadSoBytes(act, path);
            String res = NativeLoader.memfdInject(bytes);
            Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
            if ("SUCCESS".equals(res)) {
                new File(path).delete();
                removePanel(act, layout);
                return;
            }
        } catch (Exception e) {
            Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        trigger.setEnabled(true);
    }

    private static byte[] loadSoBytes(Activity act, String path) throws IOException {
        File file = new File(path);
        IOException lastError = null;

        // 1) 尝试直接读文件
        if (file.exists() && file.isFile() && file.length() > 0) {
            try (FileInputStream fis = new FileInputStream(file)) {
                return readAllBytes(fis, (int) file.length());
            } catch (SecurityException | IOException e) {
                lastError = new IOException("读取失败: " + e.getMessage(), e);
            }
        } else {
            lastError = new FileNotFoundException("文件不存在或为空: " + path);
        }

        // 2) 回退到 assets（文件名取末尾段）
        String assetName = file.getName();
        AssetManager am = act.getAssets();
        try (InputStream is = am.open(assetName)) {
            return readAllBytes(is, -1);
        } catch (IOException ignored) {
            // ignore and throw the previous error below
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
            ((FrameLayout) decor).removeView(layout);
        }
    }
}
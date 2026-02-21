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
import android.widget.AdapterView;
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
        // prevent duplicates
        ViewGroup decor = activity.findViewById(android.R.id.content);
        if (decor != null && decor.findViewWithTag("FloatingPanelTag") != null) {
            return;
        }

        final FrameLayout root = new FrameLayout(activity);
        root.setTag("FloatingPanelTag");
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

        // Top breathing banner
        TextView banner = new TextView(activity);
        banner.setText("tg：@USABullet520");
        banner.setTextSize(16);
        banner.setPadding(4, 4, 4, 16);
        banner.setGravity(Gravity.CENTER);
        banner.setTextColor(Color.parseColor("#9CE5FF"));
        layout.addView(banner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

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
        pickBtn.setText("选择 .so");
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

        rowTop.addView(pathInput);

        // Load button
        Button loadBtn = new Button(activity);
        loadBtn.setText("加载");
        loadBtn.setAllCaps(false);
        loadBtn.setTextColor(Color.WHITE);
        loadBtn.setBackgroundColor(Color.parseColor("#2E8B57"));
        LinearLayout.LayoutParams loadLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        loadLp.setMargins(12, 0, 0, 0);
        loadBtn.setLayoutParams(loadLp);
        rowTop.addView(loadBtn);

        // Bottom row: close button
        Button closeBtn = new Button(activity);
        closeBtn.setText("关闭");
        closeBtn.setAllCaps(false);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setBackgroundColor(Color.parseColor("#FF6347"));
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        closeLp.setMargins(0, 12, 0, 0);
        closeBtn.setLayoutParams(closeLp);
        layout.addView(closeBtn);

        // Add views to root
        root.addView(layout);
        rootParams.setMargins(0, 0, 0, 0);
        root.setLayoutParams(rootParams);

        // Set up listeners
        pickBtn.setOnClickListener(v -> {
            List<String> found = findSoFiles();
            if (found.isEmpty()) {
                Toast.makeText(activity, "未找到 .so，尝试放到 Download 后再试", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] items = found.toArray(new String[0]);
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(activity);
            b.setTitle("选择 .so 文件");
            b.setItems(items, (dialog, which) -> {
                String selected = found.get(which);
                pathInput.setText(selected);
                pathSpinner.setSelection(paths.indexOf(selected) != -1 ? paths.indexOf(selected) : 0);
            });
            b.show();
        });

        pathSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pathInput.setText(paths.get(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadBtn.setOnClickListener(v -> {
            String path = pathInput.getText().toString().trim();
            if (path.isEmpty()) {
                Toast.makeText(activity, "请输入路径", Toast.LENGTH_SHORT).show();
                return;
            }
            File file = new File(path);
            if (!file.exists()) {
                Toast.makeText(activity, "文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                byte[] data = readFile(file);
                Toast.makeText(activity, "加载成功，大小: " + data.length + " bytes", Toast.LENGTH_SHORT).show();
                // Here you would integrate with your hooking logic
            } catch (IOException e) {
                Toast.makeText(activity, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        closeBtn.setOnClickListener(v -> {
            ((ViewGroup) root.getParent()).removeView(root);
        });

        // Show the panel
        ViewGroup target = decor != null ? decor : (ViewGroup) activity.getWindow().getDecorView();
        target.addView(root, rootParams);
    }

    private static List<String> findSoFiles() {
        List<String> found = new ArrayList<>();
        for (String path : PRESET_PATHS) {
            File dir = new File(path).getParentFile();
            if (dir != null && dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".so"));
                if (files != null) {
                    for (File f : files) {
                        found.add(f.getAbsolutePath());
                    }
                }
            }
        }
        return found;
    }

    private static byte[] readFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return os.toByteArray();
        }
    }
}
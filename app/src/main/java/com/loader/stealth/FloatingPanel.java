package com.loader.stealth;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;

public class FloatingPanel {
    public static void show(final Activity activity) {
        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setBackgroundColor(Color.argb(180, 0, 0, 0));
        layout.setPadding(10, 10, 10, 10);

        final EditText pathInput = new EditText(activity);
        pathInput.setHint("/data/local/tmp/libvirtual.so");
        pathInput.setTextColor(Color.GREEN);
        pathInput.setTextSize(12);

        Button btn = new Button(activity);
        btn.setText("隐匿注入");
        btn.setOnClickListener(v -> {
            String path = pathInput.getText().toString().trim();
            if (path.isEmpty()) path = "/data/local/tmp/libvirtual.so";
            inject(activity, layout, path);
        });

        layout.addView(pathInput);
        layout.addView(btn);

        FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        decorView.addView(layout, params);
    }

    private static void inject(Activity act, LinearLayout layout, String path) {
        File file = new File(path);
        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = new byte[(int) file.length()];
                int read = fis.read(bytes);
                if (read != bytes.length) {
                    throw new IOException("Read size mismatch: " + read + " vs " + bytes.length);
                }
                String res = NativeLoader.memfdInject(bytes);
                Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
                if ("SUCCESS".equals(res)) {
                    file.delete();
                    removePanel(act, layout);
                }
            }
        } catch (Exception e) {
            Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void removePanel(Activity act, View layout) {
        View decor = act.getWindow().getDecorView();
        if (decor instanceof FrameLayout) {
            ((FrameLayout) decor).removeView(layout);
        }
    }
}
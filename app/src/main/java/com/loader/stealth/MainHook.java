package com.loader.stealth;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // LSPosed decides scope; do not filter package here.
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity act = (Activity) param.thisObject;
                XposedBridge.log("[LSHI] onResume: " + act.getClass().getName());
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (act.isFinishing()) return;
                    ViewGroup decor = act.findViewById(android.R.id.content);
                    if (decor == null) return;
                    if (decor.findViewWithTag("FloatingPanelTag") != null) return; // already added
                    try {
                        FloatingPanel.show(act);
                    } catch (Throwable t) {
                        XposedBridge.log("[LSHI] FloatingPanel error: " + t);
                    }
                }, 300); // small delay to ensure content is laid out
            }
        });
    }
}
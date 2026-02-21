package com.loader.stealth;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 由 LSPosed 勾选决定目标应用，不限制包名
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity act = (Activity) param.thisObject;
                new Handler(Looper.getMainLooper()).post(() -> {
                    ViewGroup decor = act.findViewById(android.R.id.content);
                    if (decor == null) return;
                    // 已添加则不重复
                    if (decor.findViewWithTag("FloatingPanelTag") != null) return;
                    FloatingPanel.show(act);
                });
            }
        });
    }
}
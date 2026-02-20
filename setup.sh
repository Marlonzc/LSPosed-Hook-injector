#!/bin/bash

# --- 1. 创建目录结构 ---
mkdir -p .github/workflows
mkdir -p app/src/main/java/com/loader/stealth
mkdir -p app/src/main/cpp
mkdir -p app/src/main/assets

# --- 2. 创建全局配置 ---
cat <<EOF > settings.gradle
include ':app'
EOF

cat <<EOF > build.gradle
buildscript {
    repositories { google(); mavenCentral() }
    dependencies { classpath 'com.android.tools.build:gradle:8.1.1' }
}
allprojects {
    repositories { google(); mavenCentral() }
}
EOF

# --- 3. 创建 App 级配置 (build.gradle) ---
cat <<EOF > app/build.gradle
plugins { id 'com.android.application' }

android {
    namespace 'com.loader.stealth'
    compileSdk 33
    defaultConfig {
        applicationId "com.loader.stealth"
        minSdk 26
        targetSdk 33
        versionCode 1
        versionName "1.0"
        externalNativeBuild { cmake { cppFlags "" } }
    }
    buildTypes { release { minifyEnabled false } }
    externalNativeBuild { cmake { path "src/main/cpp/CMakeLists.txt" } }
}

dependencies {
    compileOnly 'org.lsposed.lsposed:api:1.0.0'
}
EOF

# --- 4. 创建原生 C++ 核心 (memfd 注入) ---
cat <<EOF > app/src/main/cpp/native-lib.cpp
#include <jni.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <linux/memfd.h>
#include <android/dlext.h>
#include <dlfcn.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_loader_stealth_NativeLoader_memfdInject(JNIEnv *env, jclass clazz, jbyteArray so_bytes) {
    jsize len = env->GetArrayLength(so_bytes);
    jbyte* data = env->GetByteArrayElements(so_bytes, nullptr);

    // 随机化 memfd 名称以规避字符串扫描
    std::string mem_name = "sys_mem_" + std::to_string(getpid());
    int fd = syscall(__NR_memfd_create, mem_name.c_str(), MFD_CLOEXEC);
    if (fd < 0) return env->NewStringUTF("FD_CREATE_FAILED");

    write(fd, data, len);
    env->ReleaseByteArrayElements(so_bytes, data, JNI_ABORT);

    android_dlextinfo extinfo;
    extinfo.flags = ANDROID_DLEXT_USE_LIBRARY_FD;
    extinfo.library_fd = fd;

    // 隐匿加载：Maps 中不显示路径
    void* handle = android_dlopen_ext("lib_anon.so", RTLD_NOW, &extinfo);
    close(fd);

    return (handle != nullptr) ? env->NewStringUTF("SUCCESS") : env->NewStringUTF(dlerror());
}
EOF

cat <<EOF > app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project("stealth")
add_library(stealth SHARED native-lib.cpp)
find_library(log-lib log)
target_link_libraries(stealth \${log-lib} dl)
EOF

# --- 5. 创建 Java 层 (包含带路径输入的悬浮面板) ---
cat <<EOF > app/src/main/java/com/loader/stealth/NativeLoader.java
package com.loader.stealth;
public class NativeLoader {
    static { System.loadLibrary("stealth"); }
    public static native String memfdInject(byte[] soBytes);
}
EOF

cat <<EOF > app/src/main/java/com/loader/stealth/FloatingPanel.java
package com.loader.stealth;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;

public class FloatingPanel {
    public static void show(final Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
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
            if(path.isEmpty()) path = "/data/local/tmp/libvirtual.so";
            inject(activity, path);
        });

        layout.addView(pathInput);
        layout.addView(btn);

        FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        decorView.addView(layout, params);
    }

    private static void inject(Activity act, String path) {
        File file = new File(path);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            fis.close();
            String res = NativeLoader.memfdInject(bytes);
            Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
            if(res.equals("SUCCESS")) file.delete(); // 成功后删除
        } catch (Exception e) {
            Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
EOF

cat <<EOF > app/src/main/java/com/loader/stealth/MainHook.java
package com.loader.stealth;

import android.app.Activity;
import android.os.Bundle;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                FloatingPanel.show((Activity) param.thisObject);
            }
        });
    }
}
EOF

# --- 6. 配置文件 & GitHub Action ---
cat <<EOF > app/src/main/assets/xposed_init
com.loader.stealth.MainHook
EOF

cat <<EOF > .github/workflows/build.yml
name: Build APK
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: set up JDK 17
        uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - name: Build
        run: |
          chmod +x gradlew
          ./gradlew assembleRelease
      - name: Upload
        uses: actions/upload-artifact@v4
        with: { name: StealthModule, path: app/build/outputs/apk/release/*.apk }
EOF

# 生成 Gradle Wrapper 核心文件
touch gradlew
chmod +x gradlew

echo "[+] 项目结构创建完成！"


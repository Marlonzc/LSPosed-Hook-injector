package com.loader.stealth;
public class NativeLoader {
    static { System.loadLibrary("stealth"); }
    public static native String memfdInject(byte[] soBytes);
}

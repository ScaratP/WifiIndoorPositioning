package com.example.wifiindoorpositioning.datatype;

import android.net.wifi.ScanResult;

public class WifiResult {
    public String SSID;
    public String BSSID;
    public int level;

    public transient String apId;

    public transient float rpLevel;

    public WifiResult(ScanResult result) {
        BSSID = result.BSSID;
        SSID = result.SSID;
        level = result.level;
        apId = SSID + ":" + BSSID;
        rpLevel = 0f; // 初始化 rpLevel
    }

    public WifiResult(String apId, int level) {
        this.BSSID = "";
        this.SSID = "";
        this.level = level;
        this.apId = apId;
        this.rpLevel = 0f; // 初始化 rpLevel
    }

    public void applyApId() {
        this.apId = SSID + ":" + BSSID;
    }

    // 添加 setRpLevel 方法
    public void setRpLevel(float rpLevel) {
        this.rpLevel = rpLevel;
    }
}
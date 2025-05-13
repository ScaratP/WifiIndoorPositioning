package com.example.wifiindoorpositioning;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.example.wifiindoorpositioning.datatype.ApDistanceInfo;
import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.WifiResult;

public class InfoDisplayView extends LinearLayout {
    public DistanceInfo distance;
    public WifiResult wifiResult;
    public ApDistanceInfo apDistance;

    public InfoDisplayView(Context context) {
        super(context);
    }

    public InfoDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInfo(DistanceInfo distance) {
        this.distance = distance;
        // 假設的顯示邏輯
    }

    public void setInfo(WifiResult result) {
        this.wifiResult = result;
        // 假設的顯示邏輯
    }

    public void setInfo(ApDistanceInfo apDistance) {
        this.apDistance = apDistance;
        // 假設的顯示邏輯
    }
}
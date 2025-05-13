package com.example.wifiindoorpositioning.datatype;

import java.util.Comparator;

public class ApDistanceInfo {
    public String apValueName;
    public String highlightFunctionName;
    public String weightFunctionName;
    public float x;
    public float y;
    public float distance;

    public ApDistanceInfo(String apValueName, String highlightFunctionName, String weightFunctionName, float x, float y, float distance) {
        this.apValueName = apValueName;
        this.highlightFunctionName = highlightFunctionName;
        this.weightFunctionName = weightFunctionName;
        this.x = x;
        this.y = y;
        this.distance = distance;
    }

    // 添加 nameComparator
    public static Comparator<ApDistanceInfo> nameComparator = (lhs, rhs) -> lhs.apValueName.compareTo(rhs.apValueName);

    // 添加 distanceComparator
    public static Comparator<ApDistanceInfo> distanceComparator = (lhs, rhs) -> Float.compare(lhs.distance, rhs.distance);
}
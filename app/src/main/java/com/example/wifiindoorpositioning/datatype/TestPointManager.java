package com.example.wifiindoorpositioning.manager;

import com.example.wifiindoorpositioning.datatype.TestPoint;
import java.util.ArrayList;
import java.util.List;

public class TestPointManager {
    private List<TestPoint> testPoints = new ArrayList<>();
    private TestPoint currentOrigin;

    public TestPointManager() {
        // 根據平面圖座標範圍定義六個測試點（假設平面圖尺寸為 5000x5000 像素，根據截圖）
        testPoints.add(new TestPoint("Point 1", 500, 500));
        testPoints.add(new TestPoint("Point 2", 1500, 500));
        testPoints.add(new TestPoint("Point 3", 2500, 1500));
        testPoints.add(new TestPoint("Point 4", 3500, 2500));
        testPoints.add(new TestPoint("Point 5", 1000, 3000));
        testPoints.add(new TestPoint("Point 6", 2000, 4000));
        currentOrigin = testPoints.get(0); // 預設原點為第一個測試點
    }

    public List<TestPoint> getTestPoints() {
        return testPoints;
    }

    public TestPoint getCurrentOrigin() {
        return currentOrigin;
    }

    public void setCurrentOrigin(TestPoint point) {
        this.currentOrigin = point;
    }
}
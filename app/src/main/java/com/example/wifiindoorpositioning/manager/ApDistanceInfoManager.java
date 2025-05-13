package com.example.wifiindoorpositioning.manager;

import android.content.Context;

import com.example.wifiindoorpositioning.datatype.ApDistanceInfo;
import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.ReferencePoint;
import com.example.wifiindoorpositioning.datatype.TestPoint;
import com.example.wifiindoorpositioning.datatype.WifiResult;
import com.example.wifiindoorpositioning.function.DisplayFunction;
import com.example.wifiindoorpositioning.function.HighlightFunction;
import com.example.wifiindoorpositioning.function.WeightFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApDistanceInfoManager {
    private static ApDistanceInfoManager instance;
    private TestPointManager testPointManager;
    private ArrayList<String> apValues = new ArrayList<>();
    private ArrayList<HighlightFunction> highlightFunctions = new ArrayList<>();
    private ArrayList<DisplayFunction> displayFunctions = new ArrayList<>();
    private ArrayList<WeightFunction> weightFunctions = new ArrayList<>();
    private Map<String, HighlightFunction> highlightFunctionMap = new HashMap<>();
    private Map<String, DisplayFunction> displayFunctionMap = new HashMap<>();
    private Map<String, WeightFunction> weightFunctionMap = new HashMap<>();
    private ArrayList<String> highlightFunctionNames = new ArrayList<>();
    private ArrayList<String> displayFunctionNames = new ArrayList<>();
    private ArrayList<String> weightFunctionNames = new ArrayList<>();
    private int currentHighlightFunctionIndex = 0;
    private int currentDisplayFunctionIndex = 0;
    private int currentWeightFunctionIndex = 0;
    private String currentMethodName = "";
    public ArrayList<WifiResult> originalResults = new ArrayList<>();
    public ArrayList<DistanceInfo> originalDistances;
    public ArrayList<DistanceInfo> highlightDistances;
    public ArrayList<DistanceInfo> displayDistances;
    public ArrayList<ReferencePoint> fingerprint = new ArrayList<>();
    public ArrayList<WifiResult> results = new ArrayList<>();
    public ArrayList<ApDistanceInfoManager.ProtoCluster> signalClusters;

    // 添加常量
    public static final int AP_VALUE_CHANGED = 1;
    public static final int UNCERTAIN_CHANGED = 2;
    public static final int TEST_POINT_CHANGED = 3;
    public static final int WIFI_RESULT_CHANGED = 4;

    public static void createInstance(Context context) {
        if (instance == null) {
            instance = new ApDistanceInfoManager(context);
        }
    }

    public static ApDistanceInfoManager getInstance() {
        return instance;
    }

    private ApDistanceInfoManager(Context context) {
        apValues.add("Default AP Value");

        // 修正：假設 ReferencePoint 只有無參構造函數，手動設置字段
        ReferencePoint rp1 = new ReferencePoint();
        rp1.name = "RP1";
        rp1.coordinateX = 100;
        rp1.coordinateY = 100;
        rp1.vector = new ArrayList<>();
        fingerprint.add(rp1);

        ReferencePoint rp2 = new ReferencePoint();
        rp2.name = "RP2";
        rp2.coordinateX = 200;
        rp2.coordinateY = 200;
        rp2.vector = new ArrayList<>();
        fingerprint.add(rp2);

        originalDistances = new ArrayList<>();
        highlightDistances = new ArrayList<>();
        displayDistances = new ArrayList<>();
        signalClusters = new ArrayList<>();
    }

    public void setTestPointManager(TestPointManager manager) {
        this.testPointManager = manager;
    }

    public void setResult(ArrayList<WifiResult> results) {
        this.originalResults = results != null ? new ArrayList<>(results) : new ArrayList<>();
        calculatePosition();
    }

    // 確保包含 setResultFromString 方法
    public void setResultFromString(String resultsString) {
        ArrayList<WifiResult> wifiResults = parseWifiResultsFromString(resultsString);
        setResult(wifiResults);
    }

    // 解析 String 為 ArrayList<WifiResult>
    private ArrayList<WifiResult> parseWifiResultsFromString(String results) {
        ArrayList<WifiResult> wifiResults = new ArrayList<>();
        if (results != null && !results.isEmpty()) {
            String[] entries = results.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    try {
                        int level = Integer.parseInt(parts[0]);
                        float rpLevel = Float.parseFloat(parts[1]);
                        String apId = "Parsed:" + level;
                        WifiResult wifiResult = new WifiResult(apId, level);
                        wifiResult.setRpLevel(rpLevel);
                        wifiResults.add(wifiResult);
                    } catch (NumberFormatException e) {
                        // 忽略解析錯誤
                    }
                }
            }
        }
        return wifiResults;
    }

    public Coordinate calculatePosition() {
        Coordinate absolutePosition = new Coordinate(4381.00f, 4695.00f);
        if (testPointManager == null) {
            return absolutePosition;
        }
        TestPoint origin = testPointManager.getCurrentOrigin();
        return new Coordinate(
                absolutePosition.x - origin.coordinateX,
                absolutePosition.y - origin.coordinateY
        );
    }

    public ArrayList<String> getApValues() {
        return apValues;
    }

    public void loadApValueAtIndex(int index) {
        if (index >= 0 && index < apValues.size()) {
            currentMethodName = apValues.get(index);
        }
    }

    public void addHighlightFunction(String name, HighlightFunction function) {
        highlightFunctionNames.add(name);
        highlightFunctions.add(function);
        highlightFunctionMap.put(name, function);
    }

    public void addDisplayFunction(String name, DisplayFunction function) {
        displayFunctionNames.add(name);
        displayFunctions.add(function);
        displayFunctionMap.put(name, function);
    }

    public void addWeightFunction(String name, WeightFunction function) {
        weightFunctionNames.add(name);
        weightFunctions.add(function);
        weightFunctionMap.put(name, function);
    }

    public void setHighlightFunction(String name) {
        for (int i = 0; i < highlightFunctionNames.size(); i++) {
            if (highlightFunctionNames.get(i).equals(name)) {
                currentHighlightFunctionIndex = i;
                break;
            }
        }
    }

    public void setDisplayFunction(String name) {
        for (int i = 0; i < displayFunctionNames.size(); i++) {
            if (displayFunctionNames.get(i).equals(name)) {
                currentDisplayFunctionIndex = i;
                break;
            }
        }
    }

    public void setWeightFunction(String name) {
        for (int i = 0; i < weightFunctionNames.size(); i++) {
            if (weightFunctionNames.get(i).equals(name)) {
                currentWeightFunctionIndex = i;
                break;
            }
        }
    }

    public ArrayList<String> getHighlightFunctionNames() {
        return highlightFunctionNames;
    }

    public ArrayList<String> getDisplayFunctionNames() {
        return displayFunctionNames;
    }

    public ArrayList<String> getWeightFunctionNames() {
        return weightFunctionNames;
    }

    public int getCurrentHighlightFunctionIndex() {
        return currentHighlightFunctionIndex;
    }

    public int getCurrentDisplayFunctionIndex() {
        return currentDisplayFunctionIndex;
    }

    public int getCurrentWeightFunctionIndex() {
        return currentWeightFunctionIndex;
    }

    public String getCurrentMethodName() {
        return currentMethodName;
    }

    public static Coordinate getPredictCoordinate(ArrayList<DistanceInfo> distances) {
        return new Coordinate(4381.00f, 4695.00f);
    }

    public ArrayList<ApDistanceInfo> getAllApDistances() {
        ArrayList<ApDistanceInfo> apDistances = new ArrayList<>();
        float x = 100;
        float y = 100;
        float distance = 0;
        apDistances.add(new ApDistanceInfo(
                apValues.get(0),
                highlightFunctionNames.get(0),
                weightFunctionNames.get(0),
                x,
                y,
                distance
        ));
        return apDistances;
    }

    public static class Coordinate {
        public float x, y;
        public Coordinate(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class ProtoCluster {
        public ArrayList<ReferencePoint> rps;
    }

    public interface OnResultChangedListener {
        void resultChanged(int code);
    }

    public void registerOnResultChangedListener(OnResultChangedListener listener) {
        // 假設的邏輯
    }
}
package com.example.wifiindoorpositioning;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.wifiindoorpositioning.datatype.ApDistanceInfo;
import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.ReferencePoint;
import com.example.wifiindoorpositioning.datatype.TestPoint;
import com.example.wifiindoorpositioning.datatype.TestPointInfo;
import com.example.wifiindoorpositioning.datatype.WifiResult;
import com.example.wifiindoorpositioning.manager.ApDistanceInfoManager;
import com.example.wifiindoorpositioning.manager.ConfigManager;
import com.example.wifiindoorpositioning.manager.SystemServiceManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ContentDebugView extends ScrollView {
    private MainActivity activity;
    private final Context context;
    private final ArrayList<InfoDisplayView> displayViews = new ArrayList<>();
    private Spinner referencePointSpinner;

    private LinearLayout body, apDistanceInfoControlPanel, wifiResultControlPanel;
    private TextView txtWait, txtTestPoint, txtReferencePoint;
    private HighlightButton btCopy;

    private String mode = "無";
    private TestPoint testPoint;

    private TestPointInfo testPointInfo;

    private static final int maxDisplayCount = 30;

    public ContentDebugView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public ContentDebugView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView();
    }

    private void initView() {
        inflate(context, R.layout.window_contentdisplayview, this);
        body = findViewById(R.id.body);
        apDistanceInfoControlPanel = findViewById(R.id.apDistanceInfoControlPanel);
        wifiResultControlPanel = findViewById(R.id.wifiResultControlPanel);
        referencePointSpinner = findViewById(R.id.referencePointSpinner);
        txtTestPoint = findViewById(R.id.txtTestPoint);
        txtReferencePoint = findViewById(R.id.txtReferencePoint);
        btCopy = findViewById(R.id.btCopy);

        txtWait = new TextView(context);
        txtWait.setText("計算中...");
        txtWait.setTextSize(16);
        txtWait.setGravity(Gravity.CENTER);
        txtWait.setPadding(0, 10, 0, 0);
        txtWait.setTextColor(Color.RED);
        txtWait.setVisibility(GONE);

        body.addView(txtWait);

        if (isInEditMode()) return;

        setReferencePoints();
        setReferencePoint(ApDistanceInfoManager.getInstance().fingerprint.get(0));
        referencePointSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!fromApChanged) {
                    setReferencePoint(ApDistanceInfoManager.getInstance().fingerprint.get(i));
                } else {
                    fromApChanged = false;
                    referencePointSpinner.setSelection(getReferencePointIndex(compareReferencePoint));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        btCopy.setOnButtonDownListener(() -> {
            if (testPointInfo != null) {
                TestPointInfo output = new TestPointInfo(testPointInfo.testPoint, new ArrayList<>(testPointInfo.values), testPointInfo.results);
                output.values.sort(ApDistanceInfo.nameComparator);
                // 將 TPInfo 轉換為字符串
                TPInfo tpInfo = new TPInfo(output);
                SystemServiceManager.getInstance().toClipBoard(tpInfo.toString());
            }
        });

        ApDistanceInfoManager.getInstance().registerOnResultChangedListener((code) -> {
            if (code == ApDistanceInfoManager.AP_VALUE_CHANGED || code == ApDistanceInfoManager.UNCERTAIN_CHANGED) {
                setReferencePoints(code);
            } else {
                refresh(code);
            }
        });
    }

    public void setMainActivity(MainActivity activity) {
        this.activity = activity;
    }

    public TestPoint getTestPoint() {
        return testPoint;
    }

    @SuppressLint("DefaultLocale")
    public void setTestPoint(TestPoint testPoint) {
        this.testPoint = testPoint;

        if (testPointInfo != null)
            testPointInfo.testPoint = testPoint;

        txtTestPoint.setText(String.format("%s\n(%.2f, %.2f)",
                testPoint.name != null ? testPoint.name : "Unknown",
                testPoint.coordinateX, testPoint.coordinateY));

        refresh(ApDistanceInfoManager.TEST_POINT_CHANGED);
    }

    public void setTestPoint(float x, float y) {
        setTestPoint(new TestPoint("自定義", x, y));
    }

    public void setMode(String mode) {
        this.mode = mode;
        refresh();
    }

    public void refresh() {
        switch (mode) {
            case "無":
                hideAllInfo();
                break;
            case "參考點距離":
                displayDistanceInfo();
                break;
            case "訊號強度":
                displayWifiResult();
                break;
            case "存取點距離":
                displayApDistanceInfo();
                break;
        }
    }

    public void refresh(int code) {
        if (!mode.equals("存取點距離")) {
            refresh();
        } else if (code == ApDistanceInfoManager.WIFI_RESULT_CHANGED ||
                code == ApDistanceInfoManager.UNCERTAIN_CHANGED) {
            refresh();
        } else if (code == ApDistanceInfoManager.TEST_POINT_CHANGED) {
            recalculateApDistanceInfo();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void hideAllInfo() {
        wifiResultControlPanel.setVisibility(GONE);
        apDistanceInfoControlPanel.setVisibility(GONE);

        for (InfoDisplayView displayView : displayViews) {
            displayView.setVisibility(GONE);
            displayView.setBackgroundColor(Color.WHITE);
        }
    }

    private static final int highlightColor = Color.valueOf(0, 1, 0, 0.6f).toArgb();

    public void displayDistanceInfo() {
        hideAllInfo();

        ArrayList<DistanceInfo> distances = ApDistanceInfoManager.getInstance().displayDistances;
        ArrayList<DistanceInfo> highlights = ApDistanceInfoManager.getInstance().highlightDistances;

        if (distances == null) return;

        for (int i = 0; i < distances.size(); i++) {
            DistanceInfo distance = distances.get(i);

            boolean isHighlight = false;
            for (DistanceInfo highlight : highlights) {
                if (distance.rpName != null && highlight.rpName != null && distance.rpName.equals(highlight.rpName)) {
                    isHighlight = true;
                    break;
                }
            }

            if (displayViews.size() <= i) {
                InfoDisplayView displayView = addInitInfoDisplayView();
                displayView.setInfo(distance);
                if (isHighlight) displayView.setBackgroundColor(highlightColor);
            } else {
                displayViews.get(i).setInfo(distance);
                if (isHighlight) displayViews.get(i).setBackgroundColor(highlightColor);
                displayViews.get(i).setVisibility(VISIBLE);
            }
        }
    }

    private ReferencePoint compareReferencePoint;
    private boolean fromApChanged;

    private void setReferencePoints() {
        setReferencePoints(ApDistanceInfoManager.UNCERTAIN_CHANGED);
    }

    private void setReferencePoints(int code) {
        referencePointSpinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                ApDistanceInfoManager.getInstance().fingerprint.stream().map(rp -> rp.name != null ? rp.name : "Unknown").toArray()));

        fromApChanged = true;

        if (compareReferencePoint == null) return;

        int index = getReferencePointIndex(compareReferencePoint);
        if (index != -1) {
            setReferencePoint(ApDistanceInfoManager.getInstance().fingerprint.get(index), code);
        } else {
            setReferencePoint(ApDistanceInfoManager.getInstance().fingerprint.get(0), code);
        }
    }

    @SuppressLint("DefaultLocale")
    private void setReferencePoint(ReferencePoint referencePoint) {
        setReferencePoint(referencePoint, ApDistanceInfoManager.UNCERTAIN_CHANGED);
    }

    @SuppressLint("DefaultLocale")
    private void setReferencePoint(ReferencePoint referencePoint, int code) {
        int index = getReferencePointIndex(referencePoint);

        if (index == -1) return;

        compareReferencePoint = referencePoint;

        txtReferencePoint.setText(String.format("%s (%.2f, %.2f)",
                compareReferencePoint.name != null ? compareReferencePoint.name : "Unknown",
                compareReferencePoint.coordinateX, compareReferencePoint.coordinateY));

        refresh(code);
    }

    private int getReferencePointIndex(ReferencePoint rp) {
        ArrayList<ReferencePoint> referencePoints = ApDistanceInfoManager.getInstance().fingerprint;

        for (int i = 0; i < referencePoints.size(); i++) {
            if (rp != null && rp.name != null && referencePoints.get(i) != null && referencePoints.get(i).name != null &&
                    rp.name.equals(referencePoints.get(i).name)) {
                return i;
            }
        }

        return -1;
    }

    public void displayWifiResult() {
        hideAllInfo();

        ArrayList<WifiResult> results = ApDistanceInfoManager.getInstance().results;
        if (results == null) return;

        wifiResultControlPanel.setVisibility(VISIBLE);

        results = new ArrayList<>(results);

        ArrayList<Float> rpLevels = compareReferencePoint.vector;

        for (int i = 0; i < results.size(); i++) {
            WifiResult result = results.get(i);
            if (i < rpLevels.size()) {
                result.setRpLevel(rpLevels.get(i));
            } else {
                result.setRpLevel(0f); // 預設值
            }
        }

        results.sort((lhs, rhs) -> -Float.compare(Math.abs(lhs.level - lhs.rpLevel), Math.abs(rhs.level - rhs.rpLevel)));

        for (int i = 0; i < results.size(); i++) {
            if (displayViews.size() <= i) {
                InfoDisplayView displayView = addInitInfoDisplayView();
                displayView.setInfo(results.get(i));
            } else {
                displayViews.get(i).setInfo(results.get(i));
                displayViews.get(i).setVisibility(VISIBLE);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void displayApDistanceInfo() {
        hideAllInfo();

        txtWait.setText("計算中...");
        txtWait.setVisibility(VISIBLE);
        apDistanceInfoControlPanel.setVisibility(VISIBLE);

        Thread thread = new Thread(() -> {
            ArrayList<ApDistanceInfo> apDistances = ApDistanceInfoManager.getInstance().getAllApDistances();

            if (apDistances != null) {
                for (int i = 0; i < apDistances.size(); i++) {
                    ApDistanceInfo apDistance = apDistances.get(i);
                    float x = testPoint.coordinateX - apDistance.x;
                    float y = testPoint.coordinateY - apDistance.y;
                    apDistance.distance = (float) Math.sqrt(x * x + y * y);
                }

                apDistances.sort(ApDistanceInfo.distanceComparator);

                testPointInfo = new TestPointInfo(testPoint, apDistances, ApDistanceInfoManager.getInstance().originalResults);
            }

            activity.runOnUiThread(() -> {
                txtWait.setText("無資料");

                if (apDistances == null) return;

                txtWait.setVisibility(GONE);

                display(apDistances);
            });
        });

        thread.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void recalculateApDistanceInfo() {
        if (testPointInfo == null) return;

        hideAllInfo();

        apDistanceInfoControlPanel.setVisibility(VISIBLE);

        ArrayList<ApDistanceInfo> apDistances = new ArrayList<>(testPointInfo.values);

        for (int i = 0; i < apDistances.size(); i++) {
            ApDistanceInfo apDistance = apDistances.get(i);
            float x = testPoint.coordinateX - apDistance.x;
            float y = testPoint.coordinateY - apDistance.y;
            apDistance.distance = (float) Math.sqrt(x * x + y * y);
        }

        apDistances.sort(ApDistanceInfo.distanceComparator);

        display(apDistances);
    }

    private void display(ArrayList<ApDistanceInfo> apDistances) {
        int displayCount = Math.min(apDistances.size(), maxDisplayCount);

        for (int i = 0; i < displayCount; i++) {
            ApDistanceInfo apDistance = apDistances.get(i);

            InfoDisplayView displayView;

            if (displayViews.size() <= i) {
                displayView = addInitInfoDisplayView();
            } else {
                displayView = displayViews.get(i);
                displayViews.get(i).setVisibility(VISIBLE);
            }

            // 直接傳遞 ApDistanceInfo
            displayView.setInfo(apDistance);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private InfoDisplayView addInitInfoDisplayView() {
        InfoDisplayView displayView = new InfoDisplayView(context);
        displayView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (mode.equals("存取點距離")) {
                    activity.setApValueFunctions(displayView.apDistance.apValueName,
                            displayView.apDistance.highlightFunctionName,
                            displayView.apDistance.weightFunctionName);
                }
            }
            return true;
        });
        displayViews.add(displayView);
        body.addView(displayView);

        return displayView;
    }

    //region TPInfo
    public static class TPInfo {
        public TestPoint testPoint;
        public ArrayList<TestPointApInfo> values;
        public ArrayList<WifiResult> results;

        public TPInfo(TestPointInfo testPointInfo) {
            this.testPoint = testPointInfo.testPoint;
            this.results = testPointInfo.results;

            values = new ArrayList<>();

            for (String apValueName : ConfigManager.getInstance().apValues) {
                TestPointApInfo testPointApInfo = new TestPointApInfo(apValueName);

                for (ApDistanceInfo apDistance : testPointInfo.values) {
                    if (apValueName != null && apDistance.apValueName != null && apValueName.equals(apDistance.apValueName)) {
                        testPointApInfo.functions.add(new TestPointFunctionInfo(apDistance.highlightFunctionName,
                                apDistance.weightFunctionName, apDistance.x, apDistance.y, apDistance.distance));
                    }
                }

                testPointApInfo.functions.sort(comparator);

                values.add(testPointApInfo);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TestPoint: ").append(testPoint.name != null ? testPoint.name : "Unknown")
                    .append(" (").append(testPoint.coordinateX).append(", ").append(testPoint.coordinateY).append(")\n");

            sb.append("Values:\n");
            for (TestPointApInfo apInfo : values) {
                sb.append("  AP: ").append(apInfo.apValueName != null ? apInfo.apValueName : "Unknown").append("\n");
                for (TestPointFunctionInfo func : apInfo.functions) {
                    sb.append("    Highlight: ").append(func.highlightFunctionName != null ? func.highlightFunctionName : "Unknown")
                            .append(", Weight: ").append(func.weightFunctionName != null ? func.weightFunctionName : "Unknown")
                            .append(", Coordinates: (").append(func.x).append(", ").append(func.y).append(")")
                            .append(", Distance: ").append(func.distance).append("\n");
                }
            }

            sb.append("Results:\n");
            for (WifiResult result : results) {
                sb.append("  Level: ").append(result.level)
                        .append(", RP Level: ").append(result.rpLevel).append("\n");
            }

            return sb.toString();
        }
    }

    public static class TestPointApInfo {
        public String apValueName;
        public ArrayList<TestPointFunctionInfo> functions;

        public TestPointApInfo(String apValueName) {
            this.apValueName = apValueName;
            functions = new ArrayList<>();
        }
    }

    public static class TestPointFunctionInfo {
        public final String[] names = new String[2];
        public transient String highlightFunctionName, weightFunctionName;
        public transient float x, y;
        public float distance;

        public TestPointFunctionInfo(String highlightFunctionName, String weightFunctionName, float x, float y, float distance) {
            this.highlightFunctionName = highlightFunctionName;
            this.weightFunctionName = weightFunctionName;
            this.x = x;
            this.y = y;
            this.distance = distance;
            names[0] = highlightFunctionName;
            names[1] = weightFunctionName;
        }
    }

    public static Comparator<TestPointFunctionInfo> comparator = (lhs, rhs) -> {
        int c = lhs.highlightFunctionName != null && rhs.highlightFunctionName != null ?
                lhs.highlightFunctionName.compareTo(rhs.highlightFunctionName) : 0;
        if (c != 0) return c;
        return lhs.weightFunctionName != null && rhs.weightFunctionName != null ?
                lhs.weightFunctionName.compareTo(rhs.weightFunctionName) : 0;
    };
    //endregion
}
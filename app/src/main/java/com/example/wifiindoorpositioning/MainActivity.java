package com.example.wifiindoorpositioning;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wifiindoorpositioning.datatype.DistanceInfo;
import com.example.wifiindoorpositioning.datatype.TestPoint;
import com.example.wifiindoorpositioning.datatype.TestPointInfo;
import com.example.wifiindoorpositioning.datatype.WifiResult;
import com.example.wifiindoorpositioning.function.DistanceRateHighlightFunction;
import com.example.wifiindoorpositioning.function.FirstKDistanceHighlightFunction;
import com.example.wifiindoorpositioning.function.HighlightFunction;
import com.example.wifiindoorpositioning.function.WeightFunction;
import com.example.wifiindoorpositioning.manager.ApDistanceInfoManager;
import com.example.wifiindoorpositioning.manager.ConfigManager;
import com.example.wifiindoorpositioning.manager.SystemServiceManager;
import com.example.wifiindoorpositioning.manager.TestPointManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private LinearLayout debugView;
    private LinearLayout rootView;
    private ImageView imgCompass;
    private TextView txtOrientation, txtStatus, txtDistance, txtMethodName;
    private ZoomableImageView mapImage;
    private Spinner debugModeSpinner, apValueModeSpinner, highlightModeSpinner, displayModeSpinner, weightModeSpinner, resultHistoriesSpinner, testPointSpinner;
    private ContentDebugView contentView;
    private HighlightButton btScan, btSettings, btCopy;
    private HighlightButton[] pointButtons;
    private SettingsView settingsView;

    private TestPoint testPoint;
    private TestPointManager testPointManager;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConfigManager.createInstance(this);
        ApDistanceInfoManager.createInstance(this);
        SystemServiceManager.createInstance(this);

        setContentView(R.layout.activity_main);

        rootView = findViewById(R.id.mainActivityRootView);
        debugView = findViewById(R.id.debugView);
        imgCompass = findViewById(R.id.imgCompass);
        txtOrientation = findViewById(R.id.orientation);
        txtStatus = findViewById(R.id.txtStatus);
        txtDistance = findViewById(R.id.txtTestPoint);
        txtMethodName = findViewById(R.id.txtMethodName);
        mapImage = findViewById(R.id.zoomableView);
        btScan = findViewById(R.id.btScan);
        btSettings = findViewById(R.id.btSettings);
        btCopy = findViewById(R.id.btCopy);
        debugModeSpinner = findViewById(R.id.debugModeSpinner);
        apValueModeSpinner = findViewById(R.id.apValueModeSpinner);
        highlightModeSpinner = findViewById(R.id.highlightModeSpinner);
        displayModeSpinner = findViewById(R.id.displayModeSpinner);
        weightModeSpinner = findViewById(R.id.weightModeSpinner);
        resultHistoriesSpinner = findViewById(R.id.resultHistoriesSpinner);
        testPointSpinner = findViewById(R.id.testPointSpinner);
        contentView = findViewById(R.id.contentView);
        settingsView = new SettingsView(this);

        // 新增：初始化六個按鈕
        pointButtons = new HighlightButton[6];
        pointButtons[0] = findViewById(R.id.btPoint1);
        pointButtons[1] = findViewById(R.id.btPoint2);
        pointButtons[2] = findViewById(R.id.btPoint3);
        pointButtons[3] = findViewById(R.id.btPoint4);
        pointButtons[4] = findViewById(R.id.btPoint5);
        pointButtons[5] = findViewById(R.id.btPoint6);

        // 新增：初始化 TestPointManager
        testPointManager = new TestPointManager();
        ApDistanceInfoManager.getInstance().setTestPointManager(testPointManager);
        mapImage.setTestPointManager(testPointManager);

        // 修改：設置按鈕點擊事件，將定位點平滑移動到對應的 TestPoint 座標
        for (int i = 0; i < pointButtons.length; i++) {
            final int index = i;
            pointButtons[i].setOnButtonDownListener(() -> {
                TestPoint point = testPointManager.getTestPoints().get(index);
                // 獲取當前定位點的位置
                PointF currentPoint = mapImage.getImagePoint();
                float startX = currentPoint.x;
                float startY = currentPoint.y;
                float endX = point.coordinateX;
                float endY = point.coordinateY;

                // 使用 ValueAnimator 實現平滑移動
                ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setDuration(1000); // 動畫持續時間 1 秒
                animator.addUpdateListener(animation -> {
                    float fraction = (float) animation.getAnimatedValue();
                    float newX = startX + (endX - startX) * fraction;
                    float newY = startY + (endY - startY) * fraction;
                    mapImage.setImagePoint(newX, newY);
                    // 記錄移動路徑
                    mapImage.addPathPoint(newX, newY);
                });
                animator.start();

                Toast.makeText(this, "定位點正在移動到 " + (point.name != null ? point.name : "Unknown") +
                        String.format(" (%.2f, %.2f)", point.coordinateX, point.coordinateY), Toast.LENGTH_SHORT).show();
            });
        }

        ConfigManager.getInstance().debugView = debugView;

        ConfigManager.getInstance().addHighlightFunction("距離排序3個", new FirstKDistanceHighlightFunction(3));
        ConfigManager.getInstance().addHighlightFunction("距離排序4個", new FirstKDistanceHighlightFunction(4));
        ConfigManager.getInstance().addHighlightFunction("距離排序5個", new FirstKDistanceHighlightFunction(5));
        ConfigManager.getInstance().addHighlightFunction("距離前20%", new DistanceRateHighlightFunction(0.2f));
        ConfigManager.getInstance().addHighlightFunction("距離前30%", new DistanceRateHighlightFunction(0.3f));
        ConfigManager.getInstance().addHighlightFunction("距離前40%", new DistanceRateHighlightFunction(0.4f));
        ConfigManager.getInstance().addHighlightFunction("取低loss rate", new HighlightFunction() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                ArrayList<DistanceInfo> sortDistances = new ArrayList<>(distances);

                sortDistances.sort(DistanceInfo.distanceComparable);

                if (sortDistances.size() <= 1) return sortDistances;

                float[] LR = new float[sortDistances.size()];
                int notFoundNum = sortDistances.get(0).notFoundNum;
                int pastFoundNum = sortDistances.get(0).pastFoundNum;
                LR[0] = (float) notFoundNum / pastFoundNum;

                for (int i = sortDistances.size() - 1; i > 0; i--) {
                    notFoundNum = sortDistances.get(i).notFoundNum;
                    pastFoundNum = sortDistances.get(i).pastFoundNum;
                    LR[i] = (float) notFoundNum / pastFoundNum;
                    if(LR[i] > LR[0]) {
                        sortDistances.remove(i);
                    }
                }

                return new ArrayList<>(sortDistances.subList(0, Math.min(k, sortDistances.size())));
            }
        });
        ConfigManager.getInstance().addHighlightFunction("取低new rate", new HighlightFunction() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                ArrayList<DistanceInfo> sortDistances = new ArrayList<>(distances);

                sortDistances.sort(DistanceInfo.distanceComparable);

                if (sortDistances.size() <= 1) return sortDistances;

                float[] NR = new float[sortDistances.size()];
                int foundNum = sortDistances.get(0).foundNum;
                int pastNotFoundNum = sortDistances.get(0).pastNotFoundNum;
                NR[0] = (float) foundNum / pastNotFoundNum;

                for (int i = sortDistances.size() - 1; i > 0; i--) {
                    foundNum = sortDistances.get(i).foundNum;
                    pastNotFoundNum = sortDistances.get(i).pastNotFoundNum;
                    NR[i] = (float) foundNum / pastNotFoundNum;
                    if(NR[i] > NR[0]) {
                        sortDistances.remove(i);
                    }
                }

                return new ArrayList<>(sortDistances.subList(0, Math.min(k, sortDistances.size())));
            }
        });
        ConfigManager.getInstance().addHighlightFunction("取低new or loss rate", new HighlightFunction() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                ArrayList<DistanceInfo> sortDistances = new ArrayList<>(distances);

                sortDistances.sort(DistanceInfo.distanceComparable);

                if (sortDistances.size() <= 1) return sortDistances;

                float[] NR = new float[sortDistances.size()];
                int foundNum = sortDistances.get(0).foundNum;
                int pastNotFoundNum = sortDistances.get(0).pastNotFoundNum;
                NR[0] = (float) foundNum / pastNotFoundNum;

                float[] LR = new float[sortDistances.size()];
                int notFoundNum = sortDistances.get(0).notFoundNum;
                int pastFoundNum = sortDistances.get(0).pastFoundNum;
                LR[0] = (float) notFoundNum / pastFoundNum;

                for (int i = sortDistances.size() - 1; i > 0; i--) {
                    foundNum = sortDistances.get(i).foundNum;
                    pastNotFoundNum = sortDistances.get(i).pastNotFoundNum;
                    NR[i] = (float) foundNum / pastNotFoundNum;
                    notFoundNum = sortDistances.get(i).notFoundNum;
                    pastFoundNum = sortDistances.get(i).pastFoundNum;
                    LR[i] = (float) notFoundNum / pastFoundNum;
                    if(NR[i] > (NR[0]+0.01) && LR[i]>(LR[0]+0.01)) {
                        sortDistances.remove(i);
                    }
                }

                return new ArrayList<>(sortDistances.subList(0, Math.min(k, sortDistances.size())));
            }
        });
        ConfigManager.getInstance().addHighlightFunction("Mod 取低new rate", new HighlightFunction() {
            @Override
            public ArrayList<DistanceInfo> highlight(ArrayList<DistanceInfo> distances, int k) {
                ArrayList<DistanceInfo> sortDistances = new ArrayList<>(distances);

                sortDistances.sort(DistanceInfo.distanceComparable);

                if (sortDistances.size() <= 1) return sortDistances;

                for (int i = sortDistances.size() - 1; i > 0; i--)  {
                    float disX = sortDistances.get(i).coordinateX - sortDistances.get(0).coordinateX;
                    float disY = sortDistances.get(i).coordinateY - sortDistances.get(0).coordinateY;

                    float d = (float) Math.sqrt((disX * disX) + (disY * disY));

                    if(d > 1500 ){
                        sortDistances.remove(i);
                    }
                }

                float[] NR = new float[sortDistances.size()];
                int foundNum = sortDistances.get(0).foundNum;
                int pastNotFoundNum = sortDistances.get(0).pastNotFoundNum;
                NR[0] = (float) foundNum / pastNotFoundNum;

                for (int i = sortDistances.size() - 1; i > 0; i--) {
                    foundNum = sortDistances.get(i).foundNum;
                    pastNotFoundNum = sortDistances.get(i).pastNotFoundNum;
                    NR[i] = (float) foundNum / pastNotFoundNum;
                    if(NR[i] > NR[0]) {
                        sortDistances.remove(i);
                    }
                }

                return new ArrayList<>(sortDistances.subList(0, Math.min(k, sortDistances.size())));
            }
        }, false);
        ConfigManager.getInstance().addWeightFunction("自訂權重", highlights -> {
            ArrayList<Float> weights = new ArrayList<>();
            if (highlights.size() == 0) return weights;

            if (highlights.size() == 1){
                weights.add(1f);
                return weights;
            }

            float first = highlights.get(0).distance;
            float totalRate = 0;
            float min = Float.MAX_VALUE, max = 0;
            for (int i = 0; i < highlights.size(); i++){
                float distance = highlights.get(i).distance;
                float rate = first / distance;
                totalRate += rate;
                if (min > distance){
                    min = distance;
                }
                else if (max < distance){
                    max = distance;
                }
            }

            float avgRate = totalRate / highlights.size();
            float multiplier =  ((max / min) - 1) / 0.4f;
            float total = 0;
            for (int i = 0; i < highlights.size(); i++){
                float rate = first / highlights.get(i).distance;
                total += Math.abs(avgRate - rate);
            }

            float baseRate = 1f / highlights.size();
            for (int i = 0; i < highlights.size(); i++){
                DistanceInfo info = highlights.get(i);
                float diff = avgRate - first / info.distance;
                weights.add(baseRate - (Math.abs(diff) / total) * (diff > 0 ? 1 : -1) * baseRate * multiplier);
            }

            return weights;
        }, false);
        ConfigManager.getInstance().addWeightFunction("WKNN", new WeightFunction() {
            @Override
            public ArrayList<Float> weight(ArrayList<DistanceInfo> highlights) {
                ArrayList<Float> weights = new ArrayList<>();
                float sum = 0;
                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    sum += 1/distance.distance;
                }
                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    float weight = 1/distance.distance / sum;
                    weights.add(weight);
                }
                return weights;
            }
        });
        ConfigManager.getInstance().addWeightFunction("new WKNN", new WeightFunction() {
            @Override
            public ArrayList<Float> weight(ArrayList<DistanceInfo> highlights) {
                ArrayList<Float> weights = new ArrayList<>();
                if (highlights.size() == 1){
                    weights.add(1f);
                    return weights;
                }
                float distance_sum = 0;
                float threshold = 0;
                for(int i = 0; i < highlights.size(); i++){
                    if(highlights.get(i).distance > threshold){
                        threshold = highlights.get(i).distance;
                    }
                }
                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    distance_sum += (threshold - distance.distance);
                }
                for (int i = 0; i < highlights.size(); i++){
                    DistanceInfo distance = highlights.get(i);
                    float weight = (threshold - distance.distance) / distance_sum;
                    weights.add(weight);
                }
                return weights;
            }
        });

        SystemServiceManager.getInstance().setOnOrientationChangedListener(degree -> {
            imgCompass.setRotation(degree);
            txtOrientation.setText(String.format("%.2f (%s)", degree, getDirection(degree)));
            mapImage.setLookAngle(degree);
        });

        mapImage.setNorthOffset(180);
        mapImage.setOnImagePointChangedListener(new ZoomableImageView.OnImagePointChangedListener() {
            @Override
            public void pointChange(float x, float y) {
                float diffX = x - testPoint.coordinateX;
                float diffY = y - testPoint.coordinateY;

                txtDistance.setText(String.format("%.2f, %s\n(%.2f, %.2f)",
                        Math.sqrt(diffX * diffX + diffY * diffY),
                        testPoint.name != null ? testPoint.name : "Unknown",
                        testPoint.coordinateX, testPoint.coordinateY));
            }
        });
        mapImage.setOnFingerPointChangedListener(new ZoomableImageView.OnFingerPointChangedListener() {
            @Override
            public void pointChange(float x, float y) {
                PointF p = mapImage.getImagePoint();
                float diffX = x - p.x;
                float diffY = y - p.y;

                testPoint = new TestPoint("自定義", x, y);

                contentView.setTestPoint(x, y);

                txtDistance.setText(String.format("%.2f, %s\n(%.2f, %.2f)",
                        Math.sqrt(diffX * diffX + diffY * diffY),
                        contentView.getTestPoint().name != null ? contentView.getTestPoint().name : "Unknown",
                        x, y));
            }
        });

        btScan.setOnButtonDownListener(() -> {
            txtStatus.setText("掃描中...");
            SystemServiceManager.getInstance().scan((code, results) -> {
                switch (code) {
                    case SystemServiceManager.CODE_SUCCESS:
                        txtStatus.setText("成功");
                        Log.d("MainActivity", "Scan results type: " + (results != null ? results.getClass().getName() : "null"));
                        String wifiResultsString = wifiResultsToString((ArrayList<WifiResult>) results);
                        ApDistanceInfoManager.getInstance().setResultFromString(wifiResultsString);
                        ApDistanceInfoManager.Coordinate position = ApDistanceInfoManager.getInstance().calculatePosition();
                        txtStatus.setText(String.format("掃描結果: (%.2f, %.2f)", position.x, position.y));
                        break;
                    case SystemServiceManager.CODE_NO_LOCATION:
                        txtStatus.setText("未開啟位置");
                        break;
                    case SystemServiceManager.CODE_NO_PERMISSION:
                        txtStatus.setText("未提供權限");
                        break;
                    case SystemServiceManager.CODE_TOO_FREQUENT:
                        txtStatus.setText("過於頻繁");
                        break;
                    default:
                        txtStatus.setText("未知錯誤");
                        break;
                }
            });
        });
        btSettings.setOnButtonDownListener(() -> settingsView.showView(this));
        setDebugView();
        ConfigManager.getInstance().registerOnConfigChangedListener(this::setDebugView);

        testPoint = ConfigManager.getInstance().getTestPointAtIndex(0);

        apValueModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().apValues));
        highlightModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getAllHighlightFunctionNames()));
        displayModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getAllDisplayFunctionNames()));
        weightModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getAllWeightFunctionNames()));
        testPointSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getAllTestPointNames()));
        resultHistoriesSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ConfigManager.getInstance().getResultHistoriesName()));

        highlightModeSpinner.setSelection(ApDistanceInfoManager.getInstance().getCurrentHighlightFunctionIndex());
        displayModeSpinner.setSelection(ApDistanceInfoManager.getInstance().getCurrentDisplayFunctionIndex());
        weightModeSpinner.setSelection(ApDistanceInfoManager.getInstance().getCurrentWeightFunctionIndex());

        apValueModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDistanceInfoManager.getInstance().loadApValueAtIndex(apValueModeSpinner.getSelectedItemPosition());
                txtMethodName.setText(ApDistanceInfoManager.getInstance().getCurrentMethodName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        highlightModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDistanceInfoManager.getInstance().setHighlightFunction(highlightModeSpinner.getSelectedItem().toString());
                txtMethodName.setText(ApDistanceInfoManager.getInstance().getCurrentMethodName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        displayModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDistanceInfoManager.getInstance().setDisplayFunction(displayModeSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        weightModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ApDistanceInfoManager.getInstance().setWeightFunction(weightModeSpinner.getSelectedItem().toString());
                txtMethodName.setText(ApDistanceInfoManager.getInstance().getCurrentMethodName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        resultHistoriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TestPointInfo testPointInfo = ConfigManager.getInstance().getResultHistory(i);
                testPointSpinner.setSelection(ConfigManager.getInstance().getTestPointIndex(testPointInfo.testPoint.name));
                Log.d("MainActivity", "testPointInfo.results type: " + (testPointInfo.results != null ? testPointInfo.results.getClass().getName() : "null"));
                String resultsString = wifiResultsToString(testPointInfo.results);
                ApDistanceInfoManager.getInstance().setResultFromString(resultsString);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        testPointSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                testPoint = ConfigManager.getInstance().getTestPointAtIndex(i);
                PointF p = mapImage.getImagePoint();
                float diffX = testPoint.coordinateX - p.x;
                float diffY = testPoint.coordinateY - p.y;
                mapImage.setFingerPoint(testPoint.coordinateX, testPoint.coordinateY);
                contentView.setTestPoint(testPoint);
                txtDistance.setText(String.format("%.2f, %s\n(%.2f, %.2f)",
                        Math.sqrt(diffX * diffX + diffY * diffY),
                        testPoint.name != null ? testPoint.name : "Unknown",
                        testPoint.coordinateX, testPoint.coordinateY));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        contentView.setMainActivity(this);

        btCopy.setOnButtonDownListener(() -> {
            ArrayList<WifiResult> results = ApDistanceInfoManager.getInstance().originalResults;
            StringBuilder sb = new StringBuilder();
            for (WifiResult result : results) {
                sb.append("Level: ").append(String.valueOf(result.level))
                        .append(", RP Level: ").append(String.valueOf(result.rpLevel)).append("\n");
            }
            SystemServiceManager.getInstance().toClipBoard(sb.toString());
        });

        debugModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String mode = debugModeSpinner.getSelectedItem().toString();
                contentView.setMode(mode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private String wifiResultsToString(ArrayList<WifiResult> wifiResults) {
        if (wifiResults == null || wifiResults.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (WifiResult result : wifiResults) {
            sb.append(result.level).append(",").append(result.rpLevel).append(";");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private void setConfiguration() {
        txtMethodName.setText(ApDistanceInfoManager.getInstance().getCurrentMethodName());
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            System.out.println("vis");
        } else {
            ViewGroup parent = (ViewGroup) txtMethodName.getParent();
            if (parent != null)
                parent.removeView(txtMethodName);
            rootView.addView(txtMethodName);
        }
    }

    private void setDebugView() {
        if (ConfigManager.getInstance().isDebugMode && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup parent = (ViewGroup) debugView.getParent();
            if (parent != null)
                parent.removeView(debugView);
            rootView.addView(debugView);
        } else {
            rootView.removeView(debugView);
        }
    }

    public void setApValueFunctions(String apValueName, String highlightFunctionName, String weightFunctionName) {
        apValueModeSpinner.setSelection(ConfigManager.getInstance().getApValueIndex(apValueName));
        highlightModeSpinner.setSelection(ConfigManager.getInstance().getHighlightFunctionIndex(highlightFunctionName));
        weightModeSpinner.setSelection(ConfigManager.getInstance().getWeightFunctionIndex(weightFunctionName));
    }

    public String getDirection(float degree) {
        float range = Math.abs(degree);
        if (range < 22.5) {
            return "N";
        } else if (range < 67.5) {
            return (degree < 0) ? "NW" : "NE";
        } else if (range < 112.5) {
            return (degree < 0) ? "W" : "E";
        } else if (range < 135) {
            return (degree < 0) ? "W" : "E";
        } else if (range < 157.5) {
            return (degree < 0) ? "SW" : "SE";
        }
        return "S";
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemServiceManager.getInstance().registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SystemServiceManager.getInstance().unregisterSensor();
    }
}
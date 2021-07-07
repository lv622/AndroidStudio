package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String mClientId = "fxcf963wbv";
    private String mClientSecret = "ufdFQhxxA073iiYX0dSmaN9cAzh3e8e3Z2y7dOKO";

    private Spinner mSpinner;
    private NaverMap mNaverMap;
    private CheckBox mLayerGroup;
    private Button mButton;
    private Button mCamera;

    private Marker mMarkerSchool = new Marker();
    private Marker mMarkerPort = new Marker();
    private Marker mMarkerCityHall = new Marker();

    private ArrayList<Marker> markerArr = new ArrayList<>();
    private ArrayList<LatLng> latLngArr = new ArrayList<>();

    private ArrayList<Marker> markerLongArr = new ArrayList<>();
    private ArrayList<LatLng> latLngLongArr = new ArrayList<>();
    private PolygonOverlay nPolygon = new PolygonOverlay();
    private int count = 0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;

    private NaverAddrApi naverAddrApi;
    private InfoWindow infoWindow = new InfoWindow();

    private Button mEnterButton;
    private EditText mEditText;
    private String mText = "";

    private ArrayList<Marker> geoMarkerArr = new ArrayList<>();
    private ArrayList<LatLng> geoLatLngArr = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        Log.d("myLog", "onCreate 실행");

        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        //가로 고정
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        mSpinner = findViewById(R.id.spinner);
        mLayerGroup = findViewById(R.id.checkBox);
        mButton = findViewById(R.id.remove);
        mCamera = findViewById(R.id.camera);
        mEditText = findViewById(R.id.editTextTextPostalAddress);
        mEnterButton = findViewById(R.id.eButton);
    }

    //위치 권한 설정하기
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull @NotNull NaverMap naverMap) {
        this.mNaverMap = naverMap;
        Log.d("myLog", "onMapReady 실행");

        //내 위치 표시
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setVisible(true);

        OnMapMP(); //세 지점 마커, 폴리곤 표시
        onMapSpinner(); //스피너로 지도 유형 변경
        onMapCheckBox(); //체크박스 지적편집도 표시
        onMapLatLngMarker(); //위경도 마커 표시
        onMapRemoveOverlay(); //전체 오버레이 삭제
        onMapMoveCamera(); //카메라 좌표 이동
        onMapLongClickMarker(); //롱-클릭 마커 생성 후 정보창 표시
        threadAddr(); //주소 입력해서 마커 표시
    }

    //세 지점 마커, 폴리곤 표시
    public void OnMapMP() {
        //마커 표시
        mMarkerSchool.setPosition(new LatLng(35.946246, 126.682209));
        mMarkerPort.setPosition(new LatLng(35.967664, 126.736841));
        mMarkerCityHall.setPosition(new LatLng(35.970491, 126.617213));
        mMarkerSchool.setMap(mNaverMap);
        mMarkerPort.setMap(mNaverMap);
        mMarkerCityHall.setMap(mNaverMap);

        //폴리곤 생성
        PolygonOverlay polygon = new PolygonOverlay();
        polygon.setCoords(Arrays.asList(
                new LatLng(35.946246, 126.682209),
                new LatLng(35.967664, 126.736841),
                new LatLng(35.970491, 126.617213),
                new LatLng(35.946246, 126.682209)
        ));

        polygon.setColor(0x7fff0000);
        polygon.setMap(mNaverMap);
    }

    //스피너 지도 유형 변경
    public void onMapSpinner() {
        Log.d("myLog", "spinner 실행");
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) mNaverMap.setMapType(NaverMap.MapType.Basic);
                else mNaverMap.setMapType(NaverMap.MapType.Hybrid);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    //체크박스 지적편집도 표시
    public void onMapCheckBox() {
        Log.d("myLog", "checkBox 실행");
        mLayerGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLayerGroup.isChecked()) {
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                } else {
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                }
            }
        });
    }

    //위경도 마커 표시
    public void onMapLatLngMarker() {
        Log.d("myLog", "위경도 좌표 표시, 새 마커 생성");
        mNaverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull @NotNull PointF pointF, @NonNull @NotNull LatLng latLng) {
                Toast.makeText(getApplicationContext(), "위도: " + latLng.latitude + ", 경도: " + latLng.longitude, Toast.LENGTH_SHORT).show();

                markerArr.add(new Marker(new LatLng(latLng.latitude, latLng.longitude)));
                latLngArr.add(new LatLng(latLng.latitude, latLng.longitude));

                for (int i = 0; i < markerArr.size(); i++) {
                    markerArr.get(i).setMap(mNaverMap);
                }

                if (markerArr.size() > 2) {
                    nPolygon.setCoords(latLngArr);

                    nPolygon.setColor(0x7f00ff00);
                    nPolygon.setMap(mNaverMap);
                }
            }
        });
    }

    //전체 오버레이 삭제
    public void onMapRemoveOverlay() {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("myLog", "전체 오버레이 삭제");
                for (int i = 0; i < markerArr.size(); i++) {
                    markerArr.get(i).setMap(null);
                }
                markerArr.clear();
                latLngArr.clear();
                nPolygon.setMap(null);

                for (int i = 0; i < markerLongArr.size(); i++) {
                    markerLongArr.get(i).setMap(null);
                }
                markerLongArr.clear();
                latLngLongArr.clear();
                count = 0;
            }
        });
    }

    //카메라 좌표 이동
    public void onMapMoveCamera() {
        mCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("myLog", "카메라 이동");
                CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(new LatLng(35.960259, 126.682631), 11).animate(CameraAnimation.Easing, 500);
                mNaverMap.moveCamera(cameraUpdate);
            }
        });
    }

    //롱-클릭 마커 생성 후 정보창 표시
    public void onMapLongClickMarker() {
        mNaverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull @NotNull PointF pointF, @NonNull @NotNull LatLng latLng) {
                //Toast.makeText(getApplicationContext(), "위도: " + latLng.latitude + ", 경도: " + latLng.longitude, Toast.LENGTH_SHORT).show();
                markerLongArr.add(new Marker(new LatLng(latLng.latitude, latLng.longitude)));
                latLngLongArr.add(new LatLng(latLng.latitude, latLng.longitude));

                naverAddrApi = new NaverAddrApi();
                naverAddrApi.execute(latLngLongArr.get(count));
                markerLongArr.get(count).setMap(mNaverMap);
                infoWindow.open(markerLongArr.get(count));
                count++;
            }
        });
    }

    //주소 입력해서 마커 표시
    public void threadAddr() {
        mEnterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String text = mEditText.getText().toString();

                        StringBuilder urlGeoBuilder = new StringBuilder("https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + text);
                        StringBuilder gsb = new StringBuilder();

                        try {
                            Log.d("Thread", "getPointFromNaver: 진행중");
                            URL gURL = new URL(urlGeoBuilder.toString());
                            HttpURLConnection cont = (HttpURLConnection) gURL.openConnection();

                            cont.setRequestMethod("GET");
                            cont.setRequestProperty("Content-Type", "application/json");
                            cont.setRequestProperty("X-NCP-APIGW-API-KEY-ID", mClientId);
                            cont.setRequestProperty("X-NCP-APIGW-API-KEY", mClientSecret);

                            int responseCode = cont.getResponseCode();
                            Log.d("Thread", "Thread response code:" + responseCode);

                            BufferedReader tbr = null;
                            if (responseCode == 200) { // 정상 호출
                                tbr = new BufferedReader(new InputStreamReader(cont.getInputStream()));
                            } else { // 에러 발생
                                tbr = new BufferedReader(new InputStreamReader(cont.getInputStream()));
                            }

                            String line;
                            while ((line = tbr.readLine()) != null) {
                                gsb.append(line);
                            }
                            tbr.close();
                            cont.disconnect();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        JsonParser geoJsonParser = new JsonParser();

                        JsonObject geoJsonObj = (JsonObject) geoJsonParser.parse(gsb.toString());
                        JsonArray geoJsonArray = (JsonArray) geoJsonObj.get("addresses");
                        geoJsonObj = (JsonObject) geoJsonArray.get(0);
                        String geoPNU = "y: " + geoJsonObj.get("y").getAsString() + ", x: " + geoJsonObj.get("x").getAsString();

                        Double geoLat = geoJsonObj.get("y").getAsDouble();
                        Double geoLng = geoJsonObj.get("x").getAsDouble();

                        Log.d("geotest", "run: " + geoPNU);
                        Log.d("geotest", String.valueOf(geoJsonObj));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                geoMarkerArr.add(new Marker());
                                geoMarkerArr.get(0).setPosition(new LatLng(geoLat, geoLng));
                                geoMarkerArr.get(0).setMap(mNaverMap);

                            }
                        });
                    }
                }).start();
            }
        });
    }

    //AsyncTask 롱-클릭 정보창에 주소 표시
    private class NaverAddrApi extends AsyncTask<LatLng, String, String> {
        @Override
        protected String doInBackground(LatLng... latLngs) {
            String strCoord = String.valueOf(latLngs[0].longitude) + "," + String.valueOf(latLngs[0].latitude);
            StringBuilder sb = new StringBuilder();

            try {
                Log.d("Json", "getPointFromNaver: 진행중");

                StringBuilder urlBuilder = new StringBuilder("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords=" + strCoord + "&sourcecrs=epsg:4326&output=json&orders=addr"); // json
                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET"); //요청 방식 선택 //헤더값
                conn.setRequestProperty("Content-Type", "application/json"); //타입설정(application/json) 형식으로 전송 (Request Body 전달시 application/json로 서버에 전달.)
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", mClientId);
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", mClientSecret);

                // request 코드가 200이면 정상적으로 호출
                int responseCode = conn.getResponseCode();
                Log.d("Json", "response code:" + responseCode);

                BufferedReader br = null;

                if (responseCode == 200) { // 정상 호출
                    Log.d("Json", "getPointFromNaver: 정상호출");
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                } else { // 에러 발생
                    Log.d("Json", "getPointFromNaver: 비정상호출");
                }

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return sb.toString();
        }

        @Override //리턴값을 통해 스레드 작업이 끝났을 때의 동작을 구현
        protected void onPostExecute(String jsonStr) {
            super.onPostExecute(jsonStr);
            String pnu = getPnu(jsonStr);

            infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getApplicationContext()) {
                @NonNull
                @Override
                public CharSequence getText(@NonNull InfoWindow infoWindow) {
                    return pnu;
                }
            });
        }

        private String getPnu(String jsonStr) {
            JsonParser jsonParser = new JsonParser();

            JsonObject jsonObj = (JsonObject) jsonParser.parse(jsonStr);
            JsonArray jsonArray = (JsonArray) jsonObj.get("results");
            jsonObj = (JsonObject) jsonArray.get(0);
            jsonObj = (JsonObject) jsonObj.get("region");
            jsonObj = (JsonObject) jsonObj.get("area1");
            String pnu = jsonObj.get("name").getAsString() + " ";

            jsonObj = (JsonObject) jsonArray.get(0);
            jsonObj = (JsonObject) jsonObj.get("region");
            jsonObj = (JsonObject) jsonObj.get("area2");
            pnu += jsonObj.get("name").getAsString() + " ";

            jsonObj = (JsonObject) jsonArray.get(0);
            jsonObj = (JsonObject) jsonObj.get("region");
            jsonObj = (JsonObject) jsonObj.get("area3");
            pnu += jsonObj.get("name").getAsString() + " ";

            jsonObj = (JsonObject) jsonArray.get(0);
            jsonObj = (JsonObject) jsonObj.get("land");
            pnu += jsonObj.get("number1").getAsString();

            Log.d("JsonTest", "Json result: " + pnu);
            return pnu;
        }
    }
}
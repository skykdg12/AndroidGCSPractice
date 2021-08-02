package com.KDG.mygcs;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.KDG.mygcs.activites.helpers.BluetoothDevicesActivity;
import com.KDG.mygcs.utils.prefs.DroidPlannerPrefs;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.List;

public class MainActivity extends AppCompatActivity implements DroneListener, TowerListener, LinkListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    private FusedLocationSource locationSource;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    private double Alti = 3.0;
    private double Add = 0.5;
    private double Minus = 0.5;
    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;
    private Spinner modeSelector;
    NaverMap mNaverMap;
    Marker mDroneMarker;
    ConnectionParameter connParams;
    Handler mainHandler;
    Marker DesMarker = new Marker();
    VehicleMode mVehicleMode;
    boolean mCheckGuideMode = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//      지도 객체 생성
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

//      기체 현재 위치
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        mainHandler = new Handler(getApplicationContext().getMainLooper());
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        mNaverMap = naverMap;
        mDroneMarker = new Marker();

//   지도 타입 위성으로 변경
        naverMap.setMapType(NaverMap.MapType.Satellite);

//        기체 고도 설정 버튼
        final Button BtnAlti = (Button) findViewById(R.id.btnAlti);
        BtnAlti.setText(Alti + "m\n" + "이륙고도");

//      기체 비행 모드 선택
        this.modeSelector = (Spinner) findViewById(R.id.ModeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

//        롱클릭 마커
        mNaverMap.setOnMapLongClickListener((point, coord) -> {

                DesMarker.setPosition(coord);
                DesMarker.setMap(mNaverMap);
                DesMarker.setIcon(MarkerIcons.BLACK);
                DesMarker.setIconTintColor(Color.RED);

                LatLong mGuidedPoint = new LatLong(coord.latitude, coord.longitude);
                GuideMode(mGuidedPoint);
            });
        }

    //  기체 고도 설정 버튼 나타내기/사라지기
    public void Al(View view){
        final Button BtnAdd = (Button) findViewById(R.id.btnadd);
        final Button Btnminus = (Button) findViewById(R.id.btnminus);
        if(BtnAdd.getVisibility() == view.GONE) {
            BtnAdd.setVisibility(view.VISIBLE);
        } else {
            BtnAdd.setVisibility(view.GONE);
        }
        if(Btnminus.getVisibility() == view.GONE) {
            Btnminus.setVisibility(view.VISIBLE);
        } else {
            Btnminus.setVisibility(view.GONE);
        }
    }

//    기체 고도 더하기 버튼
    public void add(View view){
        final Button BtnAlti = (Button) findViewById(R.id.btnAlti);
        if (Alti < 10){
            Alti += Add;
            BtnAlti.setText(Alti + "m\n" + "이륙고도");
        }
    }

//    기체 고도 빼기 버튼
    public void min(View view){
        final Button BtnAlti = (Button) findViewById(R.id.btnAlti);
        if (Alti > 3){
            Alti -= Minus;
            BtnAlti.setText(Alti + "m\n" + "이륙고도");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        //updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

//  드론 연결 및 해제
    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    //    블루투스 연결
    public void BltConnect(View view){
        DroidPlannerPrefs mPrefs = DroidPlannerPrefs.getInstance(getApplicationContext());

        String btAddress = mPrefs.getBluetoothDeviceAddress();
        final @ConnectionType.Type int connectionType = mPrefs.getConnectionParameterType();

        Uri tlogLoggingUri = com.KDG.mygcs.utils.TLogUtils.getTLogLoggingUri(getApplicationContext(),
                connectionType, System.currentTimeMillis());

        final long EVENTS_DISPATCHING_PERIOD = 200L; //MS

        if (TextUtils.isEmpty(btAddress)) {
            connParams = null;
            startActivity(new Intent(getApplicationContext(), BluetoothDevicesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            connParams = ConnectionParameter.newBluetoothConnection(btAddress,
                    tlogLoggingUri, EVENTS_DISPATCHING_PERIOD);
        }
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            this.drone.connect(connParams);
        }
    }

//  드론 상태값 가져오기
    @Override
    public void onDroneEvent(String event, Bundle extras) {
        Button connectButton = (Button) findViewById(R.id.btnUdpConnect);
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                connectButton.setText("Disconnect");
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                connectButton.setText("UdpConnect");
                break;

            case AttributeEvent.STATE_UPDATED:

            case AttributeEvent.STATE_ARMING:

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.GPS_COUNT:
                updateGps_Count();
                break;

            case AttributeEvent.GPS_POSITION:
                updateGps_Position();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    //    가이드 모드
    private void GuideMode(LatLong point) {
        mCheckGuideMode = true;
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if(vehicleState.getVehicleMode() == VehicleMode.COPTER_GUIDED){
            ControlApi.getApi(drone).goTo(point, true, null);
        }

        else if (vehicleState.getVehicleMode() != VehicleMode.COPTER_GUIDED){
            AlertDialog.Builder MoveAlert = new AlertDialog.Builder(MainActivity.this);
            MoveAlert.setMessage("현재고도를 유지하며\n목표지점까지 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {

                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                            new AbstractCommandListener() {
                                @Override

                                public void onSuccess() {
                                    ControlApi.getApi(drone).goTo(point, true, null);
                                }
                                @Override

                                public void onError(int i) {

                                }
                                @Override
                                public void onTimeout() {
                                }
                            });
                }
            }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = MoveAlert.create();
            alert.show();
        }
    }


    //  ARM, TAKE-OFF, LAND 기능 구현
    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        final Button armButton = (Button) findViewById(R.id.btnArmTakeOff);
        final AlertDialog.Builder ArmAlert = new AlertDialog.Builder(MainActivity.this);

        if (vehicleState.isFlying()) {
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }
                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });

        } else if (vehicleState.isArmed()) {
            ArmAlert.setMessage("지정한 이륙고도까지 기체가 상승합니다.\n안전거리를 유지하세요.");
            ArmAlert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ControlApi.getApi(drone).takeoff(Alti, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            alertUser("Taking off...");
                            armButton.setText("LAND");
                        }

                        @Override
                        public void onError(int executionError) {
                            alertUser("Unable to take off.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Unable to take off.");
                        }
                    });
                }
            });
            ArmAlert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            ArmAlert.show();

        } else if (!vehicleState.isConnected()) {
            alertUser("Connect to a drone first");
        } else {
            armButton.setText("ARM");
            ArmAlert.setMessage("모터를 가동합니다.\n모터가 고속으로 회전합니다.");
            ArmAlert.setPositiveButton("확인",new DialogInterface.OnClickListener(){

                public void onClick(DialogInterface dialog, int which){
                    VehicleApi.getApi(drone).arm(true, false, new SimpleCommandListener() {
                        @Override
                        public void onSuccess() {
                            alertUser("Success Arming");
                            armButton.setText("TAKE OFF");
                        }

                        @Override
                        public void onError(int executionError) {
                            alertUser("Unable to arm vehicle.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Arming operation timed out.");
                        }
                    });
                }
            });
            ArmAlert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            ArmAlert.show();
        }
    }

    //    기체 GPS 위치 가져오기
    private void updateGps_Position() {
        Gps droneGps_P = this.drone.getAttribute(AttributeType.GPS);
        LatLong recentDronePosition = droneGps_P.getPosition();
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        float yaw = (float) attitude.getYaw();
        if (yaw < 0){
            yaw += 360;
        }

//        기체 GPS 좌표값
        double dronela = recentDronePosition.getLatitude();
        double dronelo = recentDronePosition.getLongitude();

//        기체 위치로 카메라 이동
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(dronela, dronelo)).animate(CameraAnimation.Linear);
        mNaverMap.moveCamera(cameraUpdate);

//        기체 YAW값에 따라 마커 헤드 방향 조정
        mDroneMarker.setPosition(new LatLng(dronela, dronelo));
        mDroneMarker.setAngle(yaw);
        mDroneMarker.setMap(mNaverMap);
        mDroneMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_baseline_navigation_24));

        // 가이드모드 목표지점 도달 체크
        if (mVehicleMode == VehicleMode.COPTER_GUIDED) {
            if (mCheckGuideMode) {
                if (CheckGoal(drone, new LatLng(dronela, dronelo))) {
                    DesMarker.setMap(null);
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LOITER);
                }
            }
        }
    }

    //    기체 현재 위치와 가이드 모드 포인트 지점 비교
    public boolean CheckGoal(Drone drone, LatLng recentLatLng){
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(), guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    //    기체 YAW값 가져오기
    private void updateYaw() {
        TextView textView = findViewById(R.id.YAW);
        Attitude droneYAW = this.drone.getAttribute(AttributeType.ATTITUDE);
        int yaw = (int) droneYAW.getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        textView.setText(String.format("YAW "  + yaw) + "deg");
    }

    //    기체 위성 수 가져오기
    private void updateGps_Count() {
        TextView textView = findViewById(R.id.Gps);
        Gps droneGps_C = this.drone.getAttribute(AttributeType.GPS);
        textView.setText(String.format("위성 "  + droneGps_C.getSatellitesCount()));
    }

    //  기체 전압 가져오기
    private void updateBattery() {
        TextView textView = findViewById(R.id.Battery);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        textView.setText(String.format("전압 " + "%3.1f", droneBattery.getBatteryVoltage()) + "V");
    }

    //  기체 고도 가져오기
    private void updateAltitude() {
        TextView textView = findViewById(R.id.Altitude);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        textView.setText(String.format("고도 " + "%3.1f", droneAltitude.getAltitude()) + "m");
    }

//  기체 속도 가져오기
    protected void updateSpeed() {
        TextView textView = findViewById(R.id.Speed);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        textView.setText(String.format("속도 " + "%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

//    기체 비행 모드 선택
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }
    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }
    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        mVehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(mVehicleMode));
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {

    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    // Helper methods
    // ==========================================================

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


}

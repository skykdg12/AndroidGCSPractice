package com.viasofts.mygcs;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
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
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import org.droidplanner.services.android.impl.core.gcs.follow.Follow;

public class MainActivity extends AppCompatActivity implements DroneListener, TowerListener, LinkListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private LocationOverlay locationOverlay;

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    private FusedLocationSource locationSource;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private double Alti = 3.0;
    private double Add = 0.5;
    private double Minus = 0.5;

    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;
    NaverMap mNaverMap;
    Marker mDroneMarker;



    Handler mainHandler;

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

/*
        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
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
 */
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
        if (Alti <10){
            Alti += Add;
            BtnAlti.setText(Alti + "m\n" + "이륙고도");
        }
    }

//    기체 고도 빼기 버튼
    public void min(View view){
        final Button BtnAlti = (Button) findViewById(R.id.btnAlti);
        if (Alti >3){
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

//  연결 상태 표시
    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }


//  드론 상태값 가져오기
    @Override
    public void onDroneEvent(String event, Bundle extras) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                connectButton.setText("Disconnect");
/*
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
 */
                checkSoloState();

                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                connectButton.setText("Connect");
/*
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
 */
                break;

            case AttributeEvent.STATE_UPDATED:

            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
/*
                    updateVehicleModesForType(this.droneType);
 */
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

//  ARM, TAKE-OFF, LAND 버튼 구현
    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);


        if (vehicleState.isFlying()) {
            // Land
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
            // Take off
            ControlApi.getApi(this.drone).takeoff(Alti, new AbstractCommandListener() {

                @Override
                public void onSuccess() {
                    alertUser("Taking off...");
                }

                @Override
                public void onError(int i) {
                    alertUser("Unable to take off.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off.");
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("Success Arming");
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
    }

//  ARM, TAKE-OFF, LAND 버튼 구현
    private void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    //    기체 GPS 위치 가져오기
    private void updateGps_Position() {
        Gps droneGps_P = this.drone.getAttribute(AttributeType.GPS);
        droneGps_P.getPosition();
        Attitude droneYAW = this.drone.getAttribute(AttributeType.ATTITUDE);
        int yaw = (int) droneYAW.getYaw();

//        기체 GPS 좌표값
        double dronela = droneGps_P.getPosition().getLatitude();
        double dronelo = droneGps_P.getPosition().getLongitude();

//        기체 위치로 카메라 이동
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(dronela, dronelo)).animate(CameraAnimation.Linear);
        mNaverMap.moveCamera(cameraUpdate);

//        기체 YAW값에 따라 마커 헤드 방향 조정
        mDroneMarker.setPosition(new LatLng(dronela, dronelo));
        mDroneMarker.setMap(mNaverMap);
        mDroneMarker.setAngle(yaw);
        mDroneMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_baseline_navigation_24));


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

    //    기체 비행모드 가져오기
    private void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        TextView textView = findViewById(R.id.VehicleMode);
        textView.setText("비행모드 " + vehicleMode);

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

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
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

package com.bearpot.dgjung.contextawareness_demo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bearpot.dgjung.contextawareness_demo.Database.DBHelper;
import com.bearpot.dgjung.contextawareness_demo.Logger.LogFragment;
import com.bearpot.dgjung.contextawareness_demo.Tracking.DetectActivitiesService;
import com.bearpot.dgjung.contextawareness_demo.Tracking.DetectHospitalService;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private boolean mGeofencesAdded;
    private GoogleApiClient mApiClient;
    DBHelper dbHelper;

    // The fence key is how callback code determines which fence fired.
    private final String FENCE_KEY = "detect_test_fence_key", TAG = getClass().getSimpleName();;
    private PendingIntent mPendingIntent;
    private FenceReceiver mFenceReceiver;

    private LogFragment mLogFragment;

    private Button startBtn, stopBtn, getHospitalFenceBtn, stopHospitalFenceBtn;
    private FloatingActionButton fab;
    private SupportMapFragment mapFragment;

    // The intent action which will be fired when your fence is triggered.
    private final String FENCE_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";
    private static final int MY_PERMISSION_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setLayout();
    }

    public class FenceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
                mLogFragment.getLogView()
                        .println("Received an unsupported action in FenceReceiver: action="
                                + intent.getAction());
                return;
            }

            // The state information for the given fence is em
            FenceState fenceState = FenceState.extract(intent);

            if (TextUtils.equals(fenceState.getFenceKey(), FENCE_KEY)) {
                String fenceStateStr;
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "true";
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "false";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                }
                mLogFragment.getLogView().println("Are you in SPH?: " + fenceStateStr);
            }
        }
    }

    private void setupFences() {
        // DetectedActivityFence will fire when it detects the user performing the specified
        // activity.  In this case it's walking.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        AwarenessFence walkingFence = LocationFence.in(37.523759, 126.926942, 100, 10000);

        // Now that we have an interesting, complex condition, register the fence to receive
        // callbacks.

        // Register the fence to receive callbacks.
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence(FENCE_KEY, walkingFence, mPendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if(status.isSuccess()) {
                            Log.i("CONTEXT_AWARENESS", "Fence was successfully registered.");
                        } else {
                            Log.e("CONTEXT_AWARENESS", "Fence could not be registered: " + status);
                        }
                    }
                });
    }

    private void setListener(){

        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Toast.makeText(getApplicationContext(),"Service 시작",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, DetectActivitiesService.class);
                //Intent intent = new Intent(MainActivity.this,DetectActivitiesService.class);
                startService(intent);
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Service 끝",Toast.LENGTH_SHORT).show();
                Intent activityIntent = new Intent(MainActivity.this,DetectActivitiesService.class);
                stopService(activityIntent);
                Intent hospitalIntent = new Intent(MainActivity.this,DetectHospitalService.class);
                stopService(hospitalIntent);
            }
        });

        getHospitalFenceBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Toast.makeText(getApplicationContext(),"Service 시작",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, DetectHospitalService.class);
                //Intent intent = new Intent(MainActivity.this,DetectActivitiesService.class);
                startService(intent);
            }
        });

        stopHospitalFenceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Service 끝",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this,DetectHospitalService.class);
                stopService(intent);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printSnapshot();
            }
        });
    }

    private void setLayout() {
        startBtn = (Button) findViewById(R.id.buttonStart);
        stopBtn = (Button) findViewById(R.id.buttonStop);
        getHospitalFenceBtn = (Button) findViewById(R.id.buttonStartFence);
        stopHospitalFenceBtn = (Button) findViewById(R.id.buttonStopFence);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mLogFragment = (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);

        dbHelper = new DBHelper(getApplicationContext(), "MyInfo.db", null, 1);
        Toast.makeText(this, dbHelper.getStatus(), Toast.LENGTH_LONG).show();

        mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Awareness.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d("CONTEXT_AWARENESS", "ApiClient에 연결되었습니다.");
                        Intent intent2 = new Intent(FENCE_RECEIVER_ACTION);
                        mPendingIntent =
                                PendingIntent.getBroadcast(MainActivity.this, 0, intent2, 0);
                        // The broadcast receiver that will receive intents when a fence is triggered.
                        mFenceReceiver = new FenceReceiver();
                        registerReceiver(mFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
                        setupFences();
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        AwarenessFence locationFence = LocationFence.in(37.523759, 126.926942, 100, 10000);

                        // Now that we have an interesting, complex condition, register the fence to receive
                        // callbacks.

                        // Register the fence to receive callbacks.
                        Awareness.FenceApi.updateFences(
                                mApiClient,
                                new FenceUpdateRequest.Builder()
                                        .addFence(FENCE_KEY, locationFence, mPendingIntent)
                                        .build())
                                .setResultCallback(new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        if(status.isSuccess()) {
                                            Log.i("CONTEXT_AWARENESS", "Fence was successfully registered.");
                                        } else {
                                            Log.e("CONTEXT_AWARENESS", "Fence could not be registered: " + status);
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
        mApiClient.connect();
        setListener();

    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng sydney = new LatLng(37.523759, 126.926942);
        googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        int REQUEST_CODE_LOCATION = 2;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request missing location permission.
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }else{
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void printSnapshot() {
        // Clear the console screen of previous snapshot / fence log data
        mLogFragment.getLogView().setText("");

        Awareness.SnapshotApi.getDetectedActivity(mApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult dar) {
                        ActivityRecognitionResult arr = dar.getActivityRecognitionResult();

                        DetectedActivity probableActivity = arr.getMostProbableActivity();

                        // Confidence is an int between 0 and 100.
                        int confidence = probableActivity.getConfidence();
                        String activityStr = probableActivity.toString();
                        mLogFragment.getLogView().println("Activity: " + activityStr
                                + ", Confidence: " + confidence + "/100");
                    }
                });
    }
}
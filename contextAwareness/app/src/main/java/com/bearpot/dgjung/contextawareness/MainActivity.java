package com.bearpot.dgjung.contextawareness;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bearpot.dgjung.contextawareness.Database.DBHelper;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private boolean mGeoFencesAdded;

    private GoogleApiClient mGoogleApiClient;
    DBHelper dbHelper;

    //private final String FENCE_KEY = "detect_test_fence_key", TAG= getClass().getSimpleName();

    //private final String


    private TextView mDebugLogView;
    private StringBuilder mLogBuilder = new StringBuilder();

    private void log(String message) {
        mLogBuilder.append(message);
        mLogBuilder.append("\n");

        if(mDebugLogView != null) {
            mDebugLogView.setText(mLogBuilder.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDebugLogView = (TextView) findViewById(R.id.textview);

        findViewById(R.id.btn_get_location).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_get_activity).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_get_place).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_get_weather).setOnClickListener(mOnClickListener);

        Log.i("TEST", "[bearpot] start...");

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Awareness.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        mGoogleApiClient.connect();
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            switch(view.getId()) {
                case R.id.btn_get_location:
                    getLoction();
                    break;
                case R.id.btn_get_activity:
                    getActivity();
                    break;
                case R.id.btn_get_place:
                    getPlace();
                    break;
                case R.id.btn_get_weather:
                    getWeather();
                    break;
            }
        }
    };

    public void getLoction() {
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (!locationResult.getStatus().isSuccess()) {
                            log("Could not get location");
                            return;
                        }

                        Location location = locationResult.getLocation();
                        log("Lat : " + location.getLatitude() + ", Lon" + location.getLongitude());
                        Toast.makeText(getApplicationContext(),"Lat : " + location.getLatitude() + ", Lon" + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void getActivity() {
        Awareness.SnapshotApi.getDetectedActivity(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                    @Override
                    public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                        if(!detectedActivityResult.getStatus().isSuccess()) {
                            log("Could not get the current activity.");
                            return;
                        }

                        ActivityRecognitionResult ar = detectedActivityResult.getActivityRecognitionResult();
                        DetectedActivity probableActivity = ar.getMostProbableActivity();
                        log(probableActivity.toString());
                    }
                });
    }

    public void getPlace() {
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        Awareness.SnapshotApi.getPlaces(mGoogleApiClient)
                .setResultCallback(new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
                        if(!placesResult.getStatus().isSuccess()) {
                            log("Could not get places.");
                            return;
                        }
                        List<PlaceLikelihood> placeLikelihoodList = placesResult.getPlaceLikelihoods();

                        if(placeLikelihoodList == null) {
                            log("Result List is Null!!");
                            return;
                        }
                        // Show the top 5 possible location results.
                        for(int i = 0; i < placeLikelihoodList.size(); i++) {
                            PlaceLikelihood p = placeLikelihoodList.get(i);
                            log(p.getPlace().getName().toString()
                                    + ", likelihood: " + p.getLikelihood());
                        }
                    }
                });
    }

    public void getWeather() {
        if(ContextCompat.checkSelfPermission(
                MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    100
            );
            return;
        }

        Awareness.SnapshotApi.getWeather(mGoogleApiClient)
                .setResultCallback(new ResultCallback<WeatherResult>() {
                    @Override
                    public void onResult(@NonNull WeatherResult weatherResult) {
                        if(!weatherResult.getStatus().isSuccess()) {
                            log("Could not get weather.");
                            return;
                        }
                        Weather weather = weatherResult.getWeather();
                        log("Weather: " + weather);
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}

package com.bearpot.dgjung.contextawareness_demo.Tracking;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bearpot.dgjung.contextawareness_demo.BuildConfig;
import com.bearpot.dgjung.contextawareness_demo.Database.DBHelper;
import com.bearpot.dgjung.contextawareness_demo.MainActivity;
import com.bearpot.dgjung.contextawareness_demo.R;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

/**
 * Created by dg.jung on 2017-11-01.
 */

public class DetectHospitalService extends Service {
    DBHelper dbHelper;
    Handler innerHandler = new Handler();
    Thread GetHospitalAndDetectStatusThread;
    NotificationManager notificationManager;

    String[][] hospitalInfo;
    DetectHospitalServiceThread thread;
    Notification noti;

    private GoogleApiClient mApiClient;
    private PendingIntent mPendingIntent;
    GetHospitalLocationHttp getHospitalHttp;

    GetHospitalLocationHttp getHospitalLocationHttp;

    private final String FENCE_KEY = "detect_hospital_fence_key", TAG = getClass().getSimpleName();
    private final String FENCE_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";
    private HospitalFenceReceiver mHospitalFenceReceiver;

    public class HospitalFenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
                Log.e("CONTEXT_AWARENESS","ActivityFenceReceiver에서 지원하지 않는 액션을 받았습니다.");
                return;
            }

            FenceState fenceState = FenceState.extract(intent);
            Log.d("CONTEXT_AWARENESS","HospitalFenceReceiver 동작");
            if (TextUtils.equals(fenceState.getFenceKey(), FENCE_KEY)) {
                String fenceStateStr;
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        fenceStateStr = "병원업무 사후처리 안내를 확인하세요.";
                        break;
                    case FenceState.FALSE:
                        fenceStateStr = "병원 업무중, 또는 아직 이동 중...";
                        break;
                    case FenceState.UNKNOWN:
                        fenceStateStr = "unknown";
                        break;
                    default:
                        fenceStateStr = "unknown value";
                }
                Log.d("CONTEXT_AWARENESS", fenceStateStr);
                notifyInsuranceResult(fenceStateStr);
            }
        }
    }

    private void notifyInsuranceResult(String fenceStateStr) {
        Intent intent2 = new Intent(DetectHospitalService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(DetectHospitalService.this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        noti = new Notification.Builder(getApplicationContext())
                .setContentTitle("사고 처리")
                .setContentText(fenceStateStr)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("알림!")
                .setContentIntent(pendingIntent)
                .build();

        //소리추가
        noti.defaults = Notification.DEFAULT_SOUND;
        //알림 소리를 한번만 내도록
        noti.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        //확인하면 자동으로 알림이 제거 되도록
        noti.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(777, noti);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }

        if (mApiClient == null) {
            setupService();
            setupClient();
            printMyLocationSnapshot();
        }

        return START_STICKY;
    }

    private void setupService() {

        dbHelper = new DBHelper(getApplicationContext(), "MyInfo.db", null, 1);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //서비스 동작을 확인하기 위한 쓰레드
        myServiceHandler handler = new myServiceHandler();
        thread = new DetectHospitalServiceThread(handler);
        thread.start();

        //Place API Web Service를 이용해 병원 정보를 가져오기 위한 스레드
        getHospitalHttp = new GetHospitalLocationHttp();
        GetHospitalAndDetectStatusThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //InputStream is = requestGet(urlPath);
                hospitalInfo = getHospitalHttp.SendByHttp(dbHelper.getLocationLat(), dbHelper.getLocationLng(), 500, "의원");
                Log.d("CONTEXT_AWARENESS", "반경 500 내 병원 정보");
                for (int i = 0; i < hospitalInfo[0].length-1; i++) {
                    Log.d("CONTEXT_AWARENESS", i + ", " + hospitalInfo[0][i] + ": " + hospitalInfo[1][i] + ", " + hospitalInfo[2][i]);
                }
                Log.d("CONTEXT_AWARENESS", hospitalInfo[0].length-1 + ", " +"37.523631, 126.927156");

                innerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setupHospitalFences();
                        notifyFencingHospitalResult();
                    }
                });
            }
        });
    }

    private void setupClient() {
        mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Awareness.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d("CONTEXT_AWARENESS", "HospitalService : Awareness API 를 등록했습니다.");
                        Intent fenceIntent = new Intent(FENCE_RECEIVER_ACTION);
                        mPendingIntent =
                                PendingIntent.getBroadcast(DetectHospitalService.this, 0, fenceIntent, 0);
                        // The broadcast receiver that will receive intents when a fence is triggered.
                        mHospitalFenceReceiver = new DetectHospitalService.HospitalFenceReceiver();
                        registerReceiver(mHospitalFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
        mApiClient.connect();
    }

    private void setupHospitalFences() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CONTEXT_AWARENESS","지오펜싱 관련 권한이 없음");
            return;
        }

        AwarenessFence[] hospitalFence = new AwarenessFence[hospitalInfo[0].length];
        for (int i = 0; i < hospitalInfo[0].length-1; i++) {
            if(hospitalInfo[0][i]==null)
                break;
            //AwarenessFence vehicleFence = DetectedActivityFence.starting(DetectedActivityFence.IN_VEHICLE);
            hospitalFence[i] = LocationFence.in(
                    Double.parseDouble(hospitalInfo[1][i]), Double.parseDouble(hospitalInfo[2][i]), 70,10000);
        }
        // 테스트용 추가 병원.
        hospitalFence[hospitalInfo[0].length-1] = LocationFence.in(
                37.523631, 126.927156 , 70,10000);

        ;
        // Register the fence to receive callbacks.
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence(FENCE_KEY, AwarenessFence.or(hospitalFence), mPendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if(status.isSuccess()) {
                            Log.d("CONTEXT_AWARENESS", "병원 펜스 성공적으로 등록됨");
                        } else {
                            Log.e("CONTEXT_AWARENESS", "에러. 교통수단 펜스 등록 안됨: " + status);
                        }
                    }
                });
    }

    private void printMyLocationSnapshot() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("CONTEXT_AWARENESS", "내 위치 권한 설정 안됐음");
            return;
        }

        getHospitalHttp = new GetHospitalLocationHttp();
        Awareness.SnapshotApi.getLocation(mApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        Location myLocation;
                        myLocation = locationResult.getLocation();
                        if (myLocation != null) {
                            Log.d("CONTEXT_AWARENESS", "현재 내 위치 : " + myLocation.getLatitude() + " , " + myLocation.getLongitude());
                        }

                        dbHelper.updateLocation(myLocation.getLatitude(), myLocation.getLongitude());
                        GetHospitalAndDetectStatusThread.start();
                    }

                });
    }

    class myServiceHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message msg) {
            Toast.makeText(DetectHospitalService.this, "HospitalService 동작중", Toast.LENGTH_LONG).show();
        }
    }

    private void notifyFencingHospitalResult() {
        Intent intent2 = new Intent(DetectHospitalService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(DetectHospitalService.this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        noti = new Notification.Builder(getApplicationContext())
                .setContentTitle("병원펜스 등록됨.")
                .setContentText("펜스가 설정되었습니다")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("알림!")
                .setContentIntent(pendingIntent)
                .build();

        //소리추가
        noti.defaults = Notification.DEFAULT_SOUND;

        //알림 소리를 한번만 내도록
        noti.flags = Notification.FLAG_ONLY_ALERT_ONCE;

        //확인하면 자동으로 알림이 제거 되도록
        noti.flags = Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(777, noti);
    }

}

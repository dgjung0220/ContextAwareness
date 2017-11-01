package com.bearpot.dgjung.contextawareness_demo.Tracking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bearpot.dgjung.contextawareness_demo.BuildConfig;
import com.bearpot.dgjung.contextawareness_demo.Database.DBHelper;
import com.bearpot.dgjung.contextawareness_demo.MainActivity;
import com.bearpot.dgjung.contextawareness_demo.R;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResult;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.Arrays;
import java.util.Date;

import static android.provider.Settings.System.DATE_FORMAT;

/**
 * Created by dg.jung on 2017-11-01.
 */

public class DetectActivitiesService extends Service implements GoogleApiClient.OnConnectionFailedListener {

    DBHelper dbHelper;

    NotificationManager notificationManager;
    Notification noti;

    // Variables for awareness fence api
    private GoogleApiClient mApiClient;
    private PendingIntent mPendingIntent;
    private ActivityFenceReceiver mActivityFenceReceiver;

    private final String FENCE_KEY = "detect_activity_fence_key", TAG = getClass().getSimpleName();
    private final String FENCE_RECEIVER_ACTION = BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";

    DetectActivitiesServiceThread thread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(mApiClient==null){
            setupService();
            setupClient();
        }

        return START_STICKY;
    }

    private void setupService() {
        dbHelper = new DBHelper(getApplicationContext(), "MyInfo.db", null, 1);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        myServiceHandler handler = new myServiceHandler();
        thread = new DetectActivitiesServiceThread(handler);
        thread.start();
    }

    private void setupClient() {
        mApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Awareness.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d("CONTEXT_AWARENESS","DetectActivitiesService : Awareness API 를 등록했습니다.");
                        Intent intent2 = new Intent(FENCE_RECEIVER_ACTION);
                        mPendingIntent =
                                PendingIntent.getBroadcast(DetectActivitiesService.this, 0, intent2, 0);
                        // The broadcast receiver that will receive intents when a fence is triggered.
                        mActivityFenceReceiver = new ActivityFenceReceiver();
                        registerReceiver(mActivityFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
                        setupVehicleFences();
                    }
                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
        mApiClient.connect();
    }


    public class ActivityFenceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
                Log.e("CONTEXT_AWARENESS","ActivityFenceReceiver에서 지원하지 않는 액션을 받았습니다.");
                return;
            }
            // The state information for the given fence is em
            FenceState fenceState = FenceState.extract(intent);

            if (TextUtils.equals(fenceState.getFenceKey(), FENCE_KEY)) {
                String fenceStateStr;
                int state; // 1:TRUE,  2:FALSE , 3:UNKNOWN
                switch (fenceState.getCurrentState()) {

                    case FenceState.TRUE:
                        state = 1;
                        fenceStateStr = "운송수단 이용중. 병원을 찾아가는 중이라고 가정합니다.";
                        break;
                    case FenceState.FALSE:
                        state = 2;
                        fenceStateStr = "운송수단 사용 종료. 500m 내의 병원을 펜스 등록.";
                        break;
                    case FenceState.UNKNOWN:
                        state = 3;
                        fenceStateStr = "unknown";
                        break;
                    default:
                        state = 4;
                        fenceStateStr = "unknown value";
                }
                notifyVehicleResult(fenceStateStr);
                runDetectedHospitalService(state);

                //펜스 쿼리 확인
                queryFence(FENCE_KEY);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("CONTEXT_AWARENESS","DetectActivitiesService : Google API Client Connection fail");
    }

    class myServiceHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message msg) {
            Toast.makeText(DetectActivitiesService.this, "DetectActivitiesService 동작중", Toast.LENGTH_LONG).show();
        }
    };

    private void notifyVehicleResult(String fenceStateStr) {
        Intent intent2 = new Intent(DetectActivitiesService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(DetectActivitiesService.this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        noti = new Notification.Builder(getApplicationContext())
                .setContentTitle("Activities 알림")
                .setContentText(fenceStateStr)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("알림!!!")
                .setContentIntent(pendingIntent)
                .build();

        //소리추가
        noti.defaults = Notification.DEFAULT_SOUND;

        //알림 소리를 한번만 내도록
        noti.flags = Notification.FLAG_ONLY_ALERT_ONCE;

        //확인하면 자동으로 알림이 제거 되도록
        noti.flags = Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify( 777 , noti);
        Log.d("CONTEXT_AWARENESS" , fenceStateStr);
    }

    private void runDetectedHospitalService(int currentState) {
        String curStatus = dbHelper.getStatus();

        //운전중이거나 교통수단 이용중인 상태에서 -> 이용중이지 않은 상태로 바뀔 경우,
        //병원을 펜스에 등록하고 병원에 근접했는지 확인하기 위한 DetectHospitalService  를 시작한다.
        if(curStatus.equals("VEHICLE") && currentState==2){
            Intent hospitalIntent = new Intent(DetectActivitiesService.this, DetectHospitalService.class);
            Log.i("CONTEXT_AWARENESS","DetectHospitalService를 시작합니다. ");

            startService(hospitalIntent);
        }
        dbHelper.updateStatus(currentState);
        Log.i("CONTEXT_AWARENESS","Activity 상태 변경 확인:"+curStatus+"-->"+dbHelper.getStatus());
    }

    protected void queryFence(final String fenceKey) {
        Awareness.FenceApi.queryFences(mApiClient,
                FenceQueryRequest.forFences(Arrays.asList(fenceKey)))
                .setResultCallback(new ResultCallback<FenceQueryResult>() {
                    @Override
                    public void onResult(@NonNull FenceQueryResult fenceQueryResult) {
                        if (!fenceQueryResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not query fence: " + fenceKey);
                            return;
                        }
                        FenceStateMap map = fenceQueryResult.getFenceStateMap();
                        for (String fenceKey : map.getFenceKeys()) {
                            FenceState fenceState = map.getFenceState(fenceKey);
                            Log.i("CONTEXT_AWARENESS", "Fence " + fenceKey + ": "
                                    + fenceState.getCurrentState()
                                    + ", was="
                                    + fenceState.getPreviousState()
                                    + ", lastUpdateTime="
                                    + DATE_FORMAT.format(
                                    String.valueOf(new Date(fenceState.getLastFenceUpdateTimeMillis()))));
                        }
                    }
                });
    }

    private void setupVehicleFences() {
        //AwarenessFence vehicleFence = DetectedActivityFence.during(DetectedActivityFence.WALKING);
        //AwarenessFence vehicleFence = DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE);
        AwarenessFence vehicleFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);

        // Register the fence to receive callbacks.
        Awareness.FenceApi.updateFences(
                mApiClient,
                new FenceUpdateRequest.Builder()
                        .addFence(FENCE_KEY, vehicleFence, mPendingIntent)
                        .build())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if(status.isSuccess()) {
                            Log.d("CONTEXT_AWARENESS", "교통수단 펜스를 등록했습니다.");
                        } else {
                            Log.e("CONTEXT_AWARENESS", "에러. 교통수단 펜스 등록 안됨: " + status);
                        }
                    }
                });
    }

    public void onDestroy() {
        thread.stopForever();
        thread = null;
    }
}

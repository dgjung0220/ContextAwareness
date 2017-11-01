package com.bearpot.dgjung.contextawareness_demo.Tracking;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by dg.jung on 2017-11-01.
 */

public class DetectHospitalServiceThread extends Thread{
    Handler handler;
    boolean isRun = true;

    public DetectHospitalServiceThread(Handler handler){
        this.handler = handler;
    }

    public void stopForever(){
        synchronized (this) {
            this.isRun = false;
        }
    }

    public void run(){
        //반복적으로 수행할 작업을 한다.
        while(isRun){

            try{
                handler.sendEmptyMessage(0);//쓰레드에 있는 핸들러에게 메세지를 보냄

                Thread.sleep(10000); //10초씩 쉰다.
            }catch (Exception e) {}
        }
    }
}

package com.bearpot.dgjung.contextawareness_demo.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by dg.jung on 2017-10-31.
 */

public class DBHelper extends SQLiteOpenHelper {
    // DBHelper 생성자로 관리할 DB 이름과 버전 정보를 받음
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    // DB를 새로 생성할 때 호출되는 함수
    @Override
    public void onCreate(SQLiteDatabase db) {
        //STATUS : STOP, VEHICLE
        db.execSQL("CREATE TABLE USER_STATUS (uStat TEXT);");
        db.execSQL("INSERT INTO USER_STATUS VALUES ('STOP');");

        db.execSQL("CREATE TABLE LOCATION (Lat double, Lng double)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public String getStatus(){
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        String result = "";

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT uStat FROM USER_STATUS", null);
        while (cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        return result;
    }

    public void updateStatus(int status){
        // 읽고 쓰기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        db.execSQL("DELETE FROM USER_STATUS;");
        if(status==1){
            db.execSQL("INSERT OR REPLACE INTO USER_STATUS VALUES ('VEHICLE')");
        }
        else{
            db.execSQL("INSERT OR REPLACE INTO USER_STATUS VALUES ('STOP')");
        }

        db.close();
    }

    public double getLocationLat(){
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        double result = 0;

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT Lat FROM LOCATION", null);
        while (cursor.moveToNext()) {
            result = cursor.getDouble(0);
        }
        return result;
    }

    public double getLocationLng(){
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        double result=0;

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT Lng FROM LOCATION", null);
        while (cursor.moveToNext()) {
            result = cursor.getDouble(0);
        }
        return result;
    }

    public void updateLocation(double lat, double lng){
        // 읽고 쓰기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        db.execSQL("DELETE FROM LOCATION;");
        db.execSQL("INSERT OR REPLACE INTO LOCATION VALUES ("+lat+","+ lng +")");
        db.close();
    }
}
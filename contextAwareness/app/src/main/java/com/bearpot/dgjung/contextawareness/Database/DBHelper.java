package com.bearpot.dgjung.contextawareness.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by dg.jung on 2017-10-31.
 */

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //STATUS : STOP, VEHICLE
        db.execSQL("CREATE TABLE USER_STATUS (uStat TEXT)");
        db.execSQL("INSERT INTO USER_STATUS VALUES ('STOP');");

        db.execSQL("CREATE TABLE LOCATION (Lat double, Lng double)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public String getStatus() {
        SQLiteDatabase db = getReadableDatabase();
        String result = "";

        Cursor cursor = db.rawQuery("SELECT uStat FROM USER_STATUS", null);
        while(cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        return result;
    }

    public void updateStatus(int status) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM USER_STATUS;");
        if (status == 1) {
            db.execSQL("INSERT OR REPLACE INTO USER_STATUS VALUES ('VEHICLE');");
        } else {
            db.execSQL("INSERT OR REPLACE INTO USER_STATUS VALUES ('STOP')");
        }

        db.close();
    }

    public double getLocationLat() {
        SQLiteDatabase db = getReadableDatabase();
        double result = 0;

        Cursor cursor = db.rawQuery("SELECT Lat FROM LOCATION", null );
        while(cursor.moveToNext()) {
            result = cursor.getDouble(0);
        }
        return result;
    }

    public double getLocationLng() {
        SQLiteDatabase db = getReadableDatabase();
        double result = 0;

        Cursor cursor = db.rawQuery("SELECT Lng FROM LOCATION", null);
        while(cursor.moveToNext()) {
            result = cursor.getDouble(0);
        }
        return result;
    }

    public void updateLocation(double lat, double lng) {
        SQLiteDatabase db = getWritableDatabase();

        db.execSQL("DELETE FROM LOCATION;");
        db.execSQL("INSERT OR REPLACE INTO LOCATION VALUES ("+ lat+","+ lng+");");

    }
}

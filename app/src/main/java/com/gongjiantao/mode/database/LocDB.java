package com.gongjiantao.mode.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.elvishew.xlog.XLog;

public class LocDB extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "HistoryLocation";
    public static final String COL_ID = "DB_COLUMN_ID";
    public static final String COL_LOC = "DB_COLUMN_LOCATION";
    public static final String COL_LNG = "DB_COLUMN_LONGITUDE_WGS84";
    public static final String COL_LAT = "DB_COLUMN_LATITUDE_WGS84";
    public static final String COL_TS = "DB_COLUMN_TIMESTAMP";
    public static final String COL_LNG_BD = "DB_COLUMN_LONGITUDE_CUSTOM";
    public static final String COL_LAT_BD = "DB_COLUMN_LATITUDE_CUSTOM";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "HistoryLocation.db";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (DB_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, DB_COLUMN_LOCATION TEXT, " +
            "DB_COLUMN_LONGITUDE_WGS84 TEXT NOT NULL, DB_COLUMN_LATITUDE_WGS84 TEXT NOT NULL, " +
            "DB_COLUMN_TIMESTAMP BIGINT NOT NULL, DB_COLUMN_LONGITUDE_CUSTOM TEXT NOT NULL, DB_COLUMN_LATITUDE_CUSTOM TEXT NOT NULL)";

    public LocDB(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

    public static void save(SQLiteDatabase db, ContentValues cv) {
        try {
            String lng = cv.getAsString(COL_LNG);
            String lat = cv.getAsString(COL_LAT);
            db.delete(TABLE_NAME,
                    COL_LNG + " = ? AND " + COL_LAT + " = ?",
                    new String[] {lng, lat});
            db.insert(TABLE_NAME, null, cv);
        } catch (Exception e) {
            XLog.e("DATABASE: insert error");
        }
    }

    public static void update(SQLiteDatabase db, String id, String loc) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_LOC, loc);
            db.update(TABLE_NAME, cv, COL_ID + " = ?", new String[] {id});
        } catch (Exception e) {
            XLog.e("DATABASE: update error");
        }
    }
}

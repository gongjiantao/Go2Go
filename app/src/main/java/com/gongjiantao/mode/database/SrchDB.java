package com.gongjiantao.mode.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.elvishew.xlog.XLog;

public class SrchDB extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "HistorySearch";
    public static final String COL_ID = "DB_COLUMN_ID";
    public static final String COL_KEY = "DB_COLUMN_KEY";
    public static final String COL_DESC = "DB_COLUMN_DESCRIPTION";
    public static final String COL_TS = "DB_COLUMN_TIMESTAMP";
    public static final String COL_ISLOC = "DB_COLUMN_IS_LOCATION";
    public static final String COL_LNG = "DB_COLUMN_LONGITUDE_WGS84";
    public static final String COL_LAT = "DB_COLUMN_LATITUDE_WGS84";
    public static final String COL_LNG_BD = "DB_COLUMN_LONGITUDE_CUSTOM";
    public static final String COL_LAT_BD = "DB_COLUMN_LATITUDE_CUSTOM";
    public static final int TYPE_KEY = 0;
    public static final int TYPE_RSLT = 1;

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "HistorySearch.db";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (DB_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, DB_COLUMN_KEY TEXT NOT NULL, " +
            "DB_COLUMN_DESCRIPTION TEXT, DB_COLUMN_TIMESTAMP BIGINT NOT NULL, DB_COLUMN_IS_LOCATION INTEGER NOT NULL, " +
            "DB_COLUMN_LONGITUDE_WGS84 TEXT, DB_COLUMN_LATITUDE_WGS84 TEXT, " +
            "DB_COLUMN_LONGITUDE_CUSTOM TEXT, DB_COLUMN_LATITUDE_CUSTOM TEXT)";

    public SrchDB(Context ctx) {
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
            String key = cv.get(SrchDB.COL_KEY).toString();
            db.delete(SrchDB.TABLE_NAME, SrchDB.COL_KEY + " = ?", new String[] {key});
            db.insert(SrchDB.TABLE_NAME, null, cv);
        } catch (Exception e) {
            XLog.e("DATABASE: insert error");
        }
    }
}

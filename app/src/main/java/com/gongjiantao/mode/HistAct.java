package com.gongjiantao.mode;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.Gravity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Locale;
import java.util.List;
import java.util.Map;

import com.gongjiantao.mode.database.LocDB;
import com.gongjiantao.mode.utils.SysUtil;

public class HistAct extends BaseAct {
    public static final String K_ID = "K_ID";
    public static final String K_LOC = "K_LOC";
    public static final String K_TS = "K_TS";
    public static final String K_LLW = "K_LLW";
    public static final String K_LLC = "K_LLC";

    private ListView lv;
    private TextView noTxt;
    private LinearLayout sly;
    private SQLiteDatabase db;
    private List<Map<String, Object>> allRec;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        initDb();

        initSrch();

        initList();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this add items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish(); // back button
            return true;
        } else if (id ==  R.id.action_delete) {
            new AlertDialog.Builder(HistAct.this)
                    .setTitle("警告")//这里是表头的内容
                    .setMessage("确定要删除全部历史记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",
                            (dialog, which) -> {
                                if (delRec(-1)) {
                                    SysUtil.toast(this, getResources().getString(R.string.history_delete_ok));
                                    updList();
                                }
                            })
                    .setNegativeButton("取消",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initDb() {
        try {
            LocDB hdb = new LocDB(getApplicationContext());
            db = hdb.getWritableDatabase();
        } catch (Exception e) {
            Log.e("HistAct", "ERROR - initDb");
        }

        cleanRec();
    }

    //sqlite 操作 查询所有记录
    private List<Map<String, Object>> loadRec() {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            Cursor cursor = db.query(LocDB.TABLE_NAME, null,
                    LocDB.COL_ID + " > ?", new String[] {"0"},
                    null, null, LocDB.COL_TS + " DESC", null);

            while (cursor.moveToNext()) {
                Map<String, Object> item = new HashMap<>();
                int ID = cursor.getInt(0);
                String Location = cursor.getString(1);
                String Longitude = cursor.getString(2);
                String Latitude = cursor.getString(3);
                long TimeStamp = cursor.getInt(4);
                String bdlng = cursor.getString(5);
                String bdlat = cursor.getString(6);
                Log.d("TB", ID + "\t" + Location + "\t" + Longitude + "\t" + Latitude + "\t" + TimeStamp + "\t" + bdlng + "\t" + bdlat);
                BigDecimal bigDecimalLongitude = BigDecimal.valueOf(Double.parseDouble(Longitude));
                BigDecimal bigDecimalLatitude = BigDecimal.valueOf(Double.parseDouble(Latitude));
                BigDecimal bigDecimalBDLongitude = BigDecimal.valueOf(Double.parseDouble(bdlng));
                BigDecimal bigDecimalBDLatitude = BigDecimal.valueOf(Double.parseDouble(bdlat));
                double doubleLongitude = bigDecimalLongitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleLatitude = bigDecimalLatitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleBDLongitude = bigDecimalBDLongitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleBDLatitude = bigDecimalBDLatitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                item.put(K_ID, Integer.toString(ID));
                item.put(K_LOC, Location);
                item.put(K_TS, SysUtil.tsToDate(Long.toString(TimeStamp)));
                item.put(K_LLW, "[经度:" + doubleLongitude + " 纬度:" + doubleLatitude + "]");
                item.put(K_LLC, "[经度:" + doubleBDLongitude + " 纬度:" + doubleBDLatitude + "]");
                data.add(item);
            }
            cursor.close();
        } catch (Exception e) {
            data.clear();
            Log.e("HistAct", "ERROR - loadRec");
        }

        return data;
    }

    private void cleanRec() {
        double lim;
        try {
            lim = Double.parseDouble(sp.getString("setting_pos_history", getResources().getString(R.string.history_expiration)));
        } catch (NumberFormatException e) {  // GOOD: The exception is caught.
            lim = 7;
        }
        final long wkSec = (long) (lim * 24 * 60 * 60);

        try {
            db.delete(LocDB.TABLE_NAME,
                    LocDB.COL_TS + " < ?", new String[] {Long.toString(System.currentTimeMillis() / 1000 - wkSec)});
        } catch (Exception e) {
            Log.e("HistAct", "ERROR - cleanRec");
        }
    }

    private boolean delRec(int ID) {
        boolean dr = true;

        try {
            if (ID <= -1) {
                db.delete(LocDB.TABLE_NAME,null, null);
            } else {
                db.delete(LocDB.TABLE_NAME,
                        LocDB.COL_ID + " = ?", new String[] {Integer.toString(ID)});
            }
        } catch (Exception e) {
            dr = false;
            Log.e("HistAct", "ERROR - delRec");
        }

        return dr;
    }

    private void initSrch() {
        SearchView mSearchView = findViewById(R.id.searchView);
        mSearchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        mSearchView.setSubmitButtonEnabled(false);//显示提交按钮
        mSearchView.setFocusable(false);
        mSearchView.clearFocus();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {// 当点击搜索按钮时触发该方法
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {// 当搜索内容改变时触发该方法
                if (TextUtils.isEmpty(newText)) {
                    SimpleAdapter adp = new SimpleAdapter(
                            HistAct.this.getBaseContext(),
                            allRec,
                            R.layout.history_item,
                            new String[]{K_ID, K_LOC, K_TS, K_LLW, K_LLC}, // 与下面数组元素要一一对应
                            new int[]{R.id.LocationID, R.id.LocationText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                    lv.setAdapter(adp);
                } else {
                    List<Map<String, Object>> sr = new ArrayList<>();
                    for (int i = 0; i < allRec.size(); i++){
                        if (allRec.get(i).toString().indexOf(newText) > 0){
                            sr.add(allRec.get(i));
                        }
                    }
                    if (!sr.isEmpty()) {
                        SimpleAdapter adp = new SimpleAdapter(
                                HistAct.this.getBaseContext(),
                                sr,
                                R.layout.history_item,
                                new String[]{K_ID, K_LOC, K_TS, K_LLW, K_LLC}, // 与下面数组元素要一一对应
                                new int[]{R.id.LocationID, R.id.LocationText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                        lv.setAdapter(adp);
                    } else {
                        SysUtil.toast(HistAct.this, getResources().getString(R.string.history_error_search));
                        SimpleAdapter adp = new SimpleAdapter(
                                HistAct.this.getBaseContext(),
                                allRec,
                                R.layout.history_item,
                                new String[]{K_ID, K_LOC, K_TS, K_LLW, K_LLC}, // 与下面数组元素要一一对应
                                new int[]{R.id.LocationID, R.id.LocationText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                        lv.setAdapter(adp);
                    }
                }

                return false;
            }
        });
    }

    private void showDelDlg(String lid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("警告");
        builder.setMessage("确定要删除该项历史记录吗?");
        builder.setPositiveButton("确定", (dialog, whichButton) -> {
            boolean dr = delRec(Integer.parseInt(lid));
            if (dr) {
                SysUtil.toast(HistAct.this, getResources().getString(R.string.history_delete_ok));
                updList();
            }
        });
        builder.setNegativeButton("取消", null);

        builder.show();
    }

    private void showEditDlg(String lid, String name) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("名称");
        builder.setView(input);
        builder.setPositiveButton("确认", (dialog, whichButton) -> {
            String inp = input.getText().toString();
            LocDB.update(db, lid, inp);
            updList();
        });
        builder.setNegativeButton("取消", null);

        builder.show();
    }

    private String[] randOff(String longitude, String latitude) {
        String defOff = getResources().getString(R.string.setting_random_offset_default);
        double lngOff = Double.parseDouble(Objects.requireNonNull(sp.getString("setting_lon_max_offset", defOff)));
        double latOff = Double.parseDouble(Objects.requireNonNull(sp.getString("setting_lat_max_offset", defOff)));
        double lon = Double.parseDouble(longitude);
        double lat = Double.parseDouble(latitude);

        double rLngOff = (Math.random() * 2 - 1) * lngOff;  // Longitude offset (meters)
        double rLatOff = (Math.random() * 2 - 1) * latOff;  // Latitude offset (meters)

        lon += rLngOff / 111320;    // (meters -> longitude)
        lat += rLatOff / 110574;    // (meters -> latitude)

        String offMsg = String.format(Locale.US, "经度偏移: %.2f米\n纬度偏移: %.2f米", rLngOff, rLatOff);
        SysUtil.toast(this, offMsg);

        return new String[]{String.valueOf(lon), String.valueOf(lat)};
    }

    private void initList() {
        noTxt = findViewById(R.id.record_no_textview);
        sly = findViewById(R.id.search_linear);
        lv = findViewById(R.id.record_list_view);
        lv.setOnItemClickListener((adapterView, view, i, l) -> {
            String bdlng;
            String bdlat;
            String name;
            name = (String) ((TextView) view.findViewById(R.id.LocationText)).getText();
            String bdll = (String) ((TextView) view.findViewById(R.id.BDLatLngText)).getText();
            bdll = bdll.substring(bdll.indexOf('[') + 1, bdll.indexOf(']'));
            String[] lls = bdll.split(" ");
            bdlng = lls[0].substring(lls[0].indexOf(':') + 1);
            bdlat = lls[1].substring(lls[1].indexOf(':') + 1);

            // Random offset
            if(sp.getBoolean("setting_random_offset", false)) {
                String[] offsetResult = randOff(bdlng, bdlat);
                bdlng = offsetResult[0];
                bdlat = offsetResult[1];
            }

            if (!HomeAct.showLoc(name, bdlng, bdlat)) {
                SysUtil.toast(this, getResources().getString(R.string.history_error_location));
            }
            this.finish();
        });

        lv.setOnItemLongClickListener((parent, view, position, id) -> {
            PopupMenu popupMenu = new PopupMenu(HistAct.this, view);
            popupMenu.setGravity(Gravity.END | Gravity.BOTTOM);
            popupMenu.getMenu().add("编辑");
            popupMenu.getMenu().add("删除");

            popupMenu.setOnMenuItemClickListener(item -> {
                String lid = ((TextView) view.findViewById(R.id.LocationID)).getText().toString();
                String name = ((TextView) view.findViewById(R.id.LocationText)).getText().toString();
                switch (item.getTitle().toString()) {
                    case "编辑":
                        showEditDlg(lid, name);
                        return true;
                    case "删除":
                        showDelDlg(lid);
                        return true;
                    default:
                        return false;
                }
            });

            popupMenu.show();
            return true;
        });

        updList();
    }

    private void updList() {
        allRec = loadRec();

        if (allRec.isEmpty()) {
            lv.setVisibility(View.GONE);
            sly.setVisibility(View.GONE);
            noTxt.setVisibility(View.VISIBLE);
        } else {
            noTxt.setVisibility(View.GONE);
            lv.setVisibility(View.VISIBLE);
            sly.setVisibility(View.VISIBLE);

            try {
                SimpleAdapter adp = new SimpleAdapter(
                        this,
                        allRec,
                        R.layout.history_item,
                        new String[]{K_ID, K_LOC, K_TS, K_LLW, K_LLC},
                        new int[]{R.id.LocationID, R.id.LocationText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                lv.setAdapter(adp);
            } catch (Exception e) {
                Log.e("HistAct", "ERROR - updList");
            }
        }
    }
}
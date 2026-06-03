package com.gongjiantao.mode;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gongjiantao.mode.service.LocSvc;
import com.gongjiantao.mode.database.LocDB;
import com.gongjiantao.mode.database.SrchDB;
import com.gongjiantao.mode.utils.SndUtil;
import com.gongjiantao.mode.utils.SysUtil;
import com.gongjiantao.mode.utils.GeoUtil;
import com.gongjiantao.mode.joystick.JoyStk;

import com.elvishew.xlog.XLog;

import io.noties.markwon.Markwon;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HomeAct extends BaseAct implements SensorEventListener {
    public static HomeAct instance;
    /* 对外 */
    public static final String K_LAT = "LAT_VALUE";
    public static final String K_LNG = "LNG_VALUE";
    public static final String K_ALT = "ALT_VALUE";

    public static final String K_PNM = "POI_NAME";
    public static final String K_PAD = "POI_ADDRESS";
    public static final String K_PLN = "POI_LONGITUDE";
    public static final String K_PLT = "POI_LATITUDE";

    private OkHttpClient http;
    private SharedPreferences sp;

    /*============================== 主界面地图 相关 ==============================*/
    /************** 地图 *****************/
    public final static BitmapDescriptor gPin = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
    public static String curCity = null;
    private MapView mv;
    private static BaiduMap bm = null;
    private static LatLng mkPt = new LatLng(36.547743718042415, 117.07018449827267); // 当前标记的地图点
    private static String mkNm = null;

    private static boolean autoMode;
    private static final ArrayList<LatLng> routePoints = new ArrayList<>();
    private static int routeIndex;
    private static boolean routeRunning;
    private static int loopRemaining;
    private static int loopTotal;
    private View routeBar;
    private TextView routeCount;
    private ImageButton routePlay;
    private TextView routeTips;
    private ImageButton btnAutoMode;
    private Handler autoHandler = new Handler(Looper.getMainLooper());
    private Runnable autoRunner;
    private GeoCoder gcr;
    private SensorManager sm;
    private Sensor sa;
    private Sensor smag;
    private float[] av = new float[3];//加速度传感器数据
    private float[] mgv = new float[3];//地磁传感器数据
    private final float[] rm = new float[9];//旋转矩阵，用来保存磁场和加速度的数据
    private final float[] dv = new float[3];//模拟方向传感器的数据（原始数据为弧度）
    /************** 定位 *****************/
    private LocationClient lc = null;
    private double clt = 0.0;       // 当前位置的百度纬度
    private double cln = 0.0;       // 当前位置的百度经度
    private float cdir = 0.0f;
    private boolean fstLoc = true; // 是否首次定位
    private boolean svcOn = false;
    private LocSvc.LocSvcBinder svc;
    private ServiceConnection cnn;
    private FloatingActionButton btnGo;
    /*============================== 历史记录 相关 ==============================*/
    private SQLiteDatabase dbLoc;
    private SQLiteDatabase dbSrch;
    /*============================== sv 相关 ==============================*/
    private SearchView sv;
    private ListView sl;
    private LinearLayout sly;
    private ListView shl;
    private LinearLayout hly;
    private MenuItem si;
    private SuggestionSearch ss;
    /*============================== 更新 相关 ==============================*/
    private DownloadManager dm = null;
    private long did;
    private BroadcastReceiver dRcv;
    private String ufn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        XLog.i("HomeAct: onCreate");

        instance = this;

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        http = new OkHttpClient();

        initNav();

        initMap();

        initRouteBar();

        initLoc();

        initBtn();

        initGo();

        cnn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                svc = (LocSvc.LocSvcBinder)service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        initDb();

        initSrch();

        initUpd();

        chkUpd(false);
    }

    @Override
    protected void onPause() {
        XLog.i("HomeAct: onPause");
        mv.onPause();
        sm.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        XLog.i("HomeAct: onResume");
        mv.onResume();
        sm.registerListener(this, sa, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, smag, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onStop() {
        XLog.i("HomeAct: onStop");
        //取消注册传感器监听
        sm.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        XLog.i("HomeAct: onDestroy");

        if (svcOn) {
            unbindService(cnn); // 解绑服务，服务要记得解绑，不要造成内存泄漏
            Intent serviceGoIntent = new Intent(HomeAct.this, LocSvc.class);
            stopService(serviceGoIntent);
        }
        unregisterReceiver(dRcv);

        sm.unregisterListener(this);

        // 退出时销毁定位
        lc.stop();
        // 关闭定位图层
        bm.setMyLocationEnabled(false);
        mv.onDestroy();

        //poi search destroy
        ss.destroy();

        //close db
        dbLoc.close();
        dbSrch.close();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //找到searchView
        si = menu.findItem(R.id.action_search);
        si.setOnActionExpandListener(new  MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                sly.setVisibility(View.GONE);
                hly.setVisibility(View.GONE);
                return true;  // Return true to collapse action view
            }
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                hly.setVisibility(View.GONE);
                //展示搜索历史
                List<Map<String, Object>> data = getHist();

                if (!data.isEmpty()) {
                    SimpleAdapter simAdapt = new SimpleAdapter(
                            HomeAct.this,
                            data,
                            R.layout.search_item,
                            new String[] {SrchDB.COL_KEY,
                                    SrchDB.COL_DESC,
                                    SrchDB.COL_TS,
                                    SrchDB.COL_ISLOC,
                                    SrchDB.COL_LNG_BD,
                                    SrchDB.COL_LAT_BD},
                            new int[] {R.id.search_key,
                                    R.id.search_description,
                                    R.id.search_timestamp,
                                    R.id.search_isLoc,
                                    R.id.search_longitude,
                                    R.id.search_latitude});
                    shl.setAdapter(simAdapt);
                    hly.setVisibility(View.VISIBLE);
                }

                return true;  // Return true to expand action view
            }
        });

        sv = (SearchView) si.getActionView();
        sv.setIconified(false);// 设置searchView处于展开状态
        sv.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        sv.setIconifiedByDefault(true);//默认为true在框内，设置false则在框外
        sv.setSubmitButtonEnabled(false);//显示提交按钮
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    ss.requestSuggestion((new SuggestionSearchOption())
                            .keyword(query)
                            .city(curCity)
                    );
                    //搜索历史 插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(SrchDB.COL_KEY, query);
                    contentValues.put(SrchDB.COL_DESC, "搜索关键字");
                    contentValues.put(SrchDB.COL_ISLOC, SrchDB.TYPE_KEY);
                    contentValues.put(SrchDB.COL_TS, System.currentTimeMillis() / 1000);

                    SrchDB.save(dbSrch, contentValues);
                    bm.clear();
                    sly.setVisibility(View.GONE);
                } catch (Exception e) {
                    SysUtil.toast(HomeAct.this, getResources().getString(R.string.app_error_search));
                    XLog.d(getResources().getString(R.string.app_error_search));
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当输入框内容改变的时候回调
                //搜索历史置为不可见
                hly.setVisibility(View.GONE);

                if (newText != null && !newText.isEmpty()) {
                    try {
                        ss.requestSuggestion((new SuggestionSearchOption())
                                .keyword(newText)
                                .city(curCity)
                        );
                    } catch (Exception e) {
                        SysUtil.toast(HomeAct.this, getResources().getString(R.string.app_error_search));
                        XLog.d(getResources().getString(R.string.app_error_search));
                    }
                }

                return true;
            }
        });

        // 搜索框的清除按钮(该按钮属于安卓系统图标)
        ImageView closeButton = sv.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            EditText et = findViewById(androidx.appcompat.R.id.search_src_text);
            et.setText("");
            sv.setQuery("", false);
            sly.setVisibility(View.GONE);
            hly.setVisibility(View.VISIBLE);
        });

        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            av = sensorEvent.values;
        }
        else if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mgv = sensorEvent.values;
        }

        SensorManager.getRotationMatrix(rm, null, av, mgv);
        SensorManager.getOrientation(rm, dv);
        cdir = (float) Math.toDegrees(dv[0]);    // 弧度转角度
        if (cdir < 0) {    // 由 -180 ~ + 180 转为 0 ~ 360
            cdir += 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*============================== NavigationView 相关 ==============================*/
    private void initNav() {
        /*============================== NavigationView 相关 ==============================*/
        NavigationView mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                Intent intent = new Intent(HomeAct.this, HistAct.class);

                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(HomeAct.this, PrefAct.class);
                startActivity(intent);
            } else if (id == R.id.nav_dev) {
                if (!SysUtil.isDevOn(this)) {
                    SysUtil.toast(this, getResources().getString(R.string.app_error_dev));
                } else {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        SysUtil.toast(this, getResources().getString(R.string.app_error_dev));
                    }
                }
            } else if (id == R.id.nav_update) {
                chkUpd(true);
            } else if (id == R.id.nav_feedback) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("WeChatID", "G46645426826");
                if (cm != null) {
                    cm.setPrimaryClip(mClipData);
                    SysUtil.toast(this, "微信号已复制，请在微信中搜索添加");
                    try {
                        Intent intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                        if (intent != null) {
                            startActivity(intent);
                        } else {
                            SysUtil.toast(this, "");
                        }
                    } catch (Exception e) {
                        SysUtil.toast(this, "无法打开微信");
                    }
                }
            } else if (id == R.id.nav_contact) {
                Uri uri = Uri.parse("https://github.com/gongjiantao/Go2Go");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

            return true;
        });

        // 直接获取第 0 个头部视图
        View headerView = mNavigationView.getHeaderView(0);
        TextView app_version = headerView.findViewById(R.id.app_version);
        app_version.setText(SysUtil.getVer(this));
    }

    /*============================== 主界面地图 相关 ==============================*/
    private void initMap() {
        // 地图初始化
        mv = findViewById(R.id.bdMapView);
        mv.showZoomControls(false);
        bm = mv.getMap();
        bm.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        bm.setMyLocationEnabled(true);
        bm.setOnMapTouchListener(event -> {

        });
        bm.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                if (autoMode) {
                    addRoutePoint(point);
                } else {
                    mkPt = point;
                    markPt();
                }
            }
            @Override
            public void onMapPoiClick(MapPoi poi) {
                if (autoMode) {
                    addRoutePoint(poi.getPosition());
                } else {
                    mkPt = poi.getPosition();
                    markPt();
                }
            }
        });
        bm.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                if (autoMode) {
                    addRoutePoint(point);
                } else {
                    mkPt = point;
                    markPt();
                    gcr.reverseGeoCode(new ReverseGeoCodeOption().location(point));
                }
            }
        });
        bm.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            /**
             * 双击地图
             */
            @Override
            public void onMapDoubleClick(LatLng point) {
                bm.animateMapStatus(MapStatusUpdateFactory.zoomIn());
            }
        });

        View poiView = View.inflate(HomeAct.this, R.layout.location_poi_info, null);
        TextView poiAddress = poiView.findViewById(R.id.poi_address);
        TextView poiLongitude = poiView.findViewById(R.id.poi_longitude);
        TextView poiLatitude = poiView.findViewById(R.id.poi_latitude);
        ImageButton ibSave = poiView.findViewById(R.id.poi_save);
        ibSave.setOnClickListener(v -> {
            saveLoc(mkPt.longitude, mkPt.latitude);
            SysUtil.toast(this, getResources().getString(R.string.app_location_save));
        });
        ImageButton ibCopy = poiView.findViewById(R.id.poi_copy);
        ibCopy.setOnClickListener(v -> {
            //获取剪贴板管理器：
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label", mkPt.toString());
            // 将 ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            SysUtil.toast(this,  getResources().getString(R.string.app_location_copy));
        });
        ImageButton ibShare = poiView.findViewById(R.id.poi_share);
        ibShare.setOnClickListener(v -> SndUtil.sendText(HomeAct.this, "分享位置", poiLongitude.getText()+","+poiLatitude.getText()));
        ImageButton ibFly = poiView.findViewById(R.id.poi_fly);
        ibFly.setOnClickListener(this::doGo);
        gcr = GeoCoder.newInstance();
        gcr.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                XLog.i(geoCodeResult.getLocation());
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    XLog.i("逆地理位置失败!");
                } else {
                    mkNm = String.valueOf(reverseGeoCodeResult.getAddress());
                    poiLatitude.setText(String.valueOf(reverseGeoCodeResult.getLocation().latitude));
                    poiLongitude.setText(String.valueOf(reverseGeoCodeResult.getLocation().longitude));
                    poiAddress.setText(reverseGeoCodeResult.getAddress());
                    final InfoWindow mInfoWindow = new InfoWindow(poiView, reverseGeoCodeResult.getLocation(), -100);
                    bm.showInfoWindow(mInfoWindow);
                }
            }
        });

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);// 获取传感器管理服务
        if (sm != null) {
            sa = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sa != null) {
                sm.registerListener(this, sa, SensorManager.SENSOR_DELAY_UI);
            }
            smag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (smag != null) {
                sm.registerListener(this, smag, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    private void initRouteBar() {
        routeBar = findViewById(R.id.route_bar);
        routeCount = findViewById(R.id.route_count);
        routePlay = findViewById(R.id.route_play);
        routeTips = findViewById(R.id.route_tips);

        ImageButton btnAutoMode = findViewById(R.id.btn_auto_mode);
        this.btnAutoMode = btnAutoMode;
        btnAutoMode.setOnClickListener(v -> {
            if (!autoMode) {
                enterAutoMode();
                SysUtil.toast(this, "自动模式：点击地图添加途经点");
            } else {
                exitAutoMode();
            }
        });

        findViewById(R.id.route_add).setOnClickListener(v -> {
            if (mkPt != null) addRoutePoint(mkPt);
        });
        routePlay.setOnClickListener(v -> {
            if (routePoints.isEmpty()) {
                SysUtil.toast(this, "请先在地图上点击添加途经点");
                return;
            }
            if (!routeRunning) {
                if (routePoints.size() >= 2) {
                    LatLng first = routePoints.get(0);
                    LatLng last = routePoints.get(routePoints.size() - 1);
                    double[] fw = GeoUtil.bd2wgs(first.longitude, first.latitude);
                    double[] lw = GeoUtil.bd2wgs(last.longitude, last.latitude);
                    double d = GeoUtil.distance(fw[0], fw[1], lw[0], lw[1]);
                    if (d < 10.0) {
                        View inputView = LayoutInflater.from(this).inflate(R.layout.dialog_loop_input, null);
                        EditText et = inputView.findViewById(R.id.loop_count_input);
                        new AlertDialog.Builder(this)
                                .setTitle("循环设置")
                                .setMessage("首尾途经点相距 " + String.format("%.1f", d) + " 米，是否循环？")
                                .setView(inputView)
                                .setPositiveButton("开始循环", (dialog, which) -> {
                                    try {
                                        int n = Integer.parseInt(et.getText().toString().trim());
                                        if (n < 1) n = 1;
                                        loopRemaining = n;
                                        loopTotal = n;
                                        startAutoRoute();
                                    } catch (NumberFormatException e) {
                                        SysUtil.toast(this, "请输入有效的循环次数");
                                    }
                                })
                                .setNegativeButton("不循环", (dialog, which) -> {
                                    loopRemaining = 0;
                                    loopTotal = 0;
                                    startAutoRoute();
                                })
                                .show();
                        return;
                    }
                }
                loopRemaining = 0;
                loopTotal = 0;
                startAutoRoute();
            } else {
                routeRunning = false;
                autoHandler.removeCallbacks(autoRunner);
                routePlay.setImageResource(R.drawable.ic_play);
                routeTips.setText("已暂停 | 点击地图继续添加途经点");
                routeTips.setBackgroundColor(0xCCFFD54F);
            }
        });
        findViewById(R.id.route_clear).setOnClickListener(v -> exitAutoMode());
    }

    private void addRoutePoint(LatLng point) {
        routePoints.add(point);
        mkPt = point;
        redrawRoute();
    }

    private void redrawRoute() {
        if (bm == null) return;
        bm.clear();
        if (routePoints.isEmpty()) return;

        List<LatLng> pts = new ArrayList<>(routePoints);
        if (pts.size() >= 2) {
            bm.addOverlay(new PolylineOptions().width(8).color(0xCCFFB300).points(pts));
        }
        for (int i = 0; i < pts.size(); i++) {
            LatLng pt = pts.get(i);
            bm.addOverlay(new MarkerOptions().position(pt).icon(gPin).title(String.valueOf(i + 1)));
        }
        routeCount.setText(pts.size() + " 个途经点");
    }

    private void clearRoute() {
        routeRunning = false;
        routeIndex = 0;
        loopRemaining = 0;
        loopTotal = 0;
        autoHandler.removeCallbacks(autoRunner);
        routePlay.setImageResource(R.drawable.ic_play);
        routePoints.clear();
        routeCount.setText("0 个途经点");
        if (bm != null) bm.clear();
    }

    public void enterAutoMode() {
        autoMode = true;
        routeBar.setVisibility(View.VISIBLE);
        routeTips.setVisibility(View.VISIBLE);
        routeTips.setText("点击地图添加途经点");
        routeTips.setBackgroundColor(0xCC81C784);
        if (btnAutoMode != null) {
            btnAutoMode.setColorFilter(getResources().getColor(R.color.colorAccent, getTheme()));
        }
    }

    public void exitAutoMode() {
        autoMode = false;
        routeBar.setVisibility(View.GONE);
        routeTips.setVisibility(View.GONE);
        if (btnAutoMode != null) {
            btnAutoMode.setColorFilter(getResources().getColor(R.color.gray, getTheme()));
        }
        clearRoute();
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    private void startAutoRoute() {
        routeRunning = true;
        routePlay.setImageResource(R.drawable.ic_stop);
        routeTips.setVisibility(View.VISIBLE);
        updateLoopTips();
        routeTips.setBackgroundColor(0xCCFFB300);
        if (!routePoints.isEmpty() && svc != null) {
            if (routeIndex >= routePoints.size()) {
                routeIndex = 0;
            }
            if (routeIndex == 0) {
                LatLng first = routePoints.get(0);
                mkPt = first;
                double[] wgs = GeoUtil.bd2wgs(first.longitude, first.latitude);
                svc.setPosition(wgs[0], wgs[1], 0);
            }
        }
        autoRunner = new Runnable() {
            @Override
            public void run() {
                if (!routeRunning || svc == null) return;
                double[] pos = doAutoStep();
                if (pos != null) {
                    svc.setPosition(pos[0], pos[1], 0);
                }
                if (routeIndex >= routePoints.size()) {
                    loopRemaining--;
                    if (loopRemaining > 0) {
                        routeIndex = 0;
                        updateLoopTips();
                        routeTips.setBackgroundColor(0xCCFFB300);
                        if (!routePoints.isEmpty()) {
                            LatLng first = routePoints.get(0);
                            mkPt = first;
                            double[] wgs = GeoUtil.bd2wgs(first.longitude, first.latitude);
                            svc.setPosition(wgs[0], wgs[1], 0);
                        }
                    } else {
                        routeRunning = false;
                        loopRemaining = 0;
                        loopTotal = 0;
                        routePoints.clear();
                        routeCount.setText("0 个途经点");
                        routePlay.setImageResource(R.drawable.ic_play);
                        routeTips.setText("路线终点已到达");
                        routeTips.setBackgroundColor(0xCCFF6D5A);
                        return;
                    }
                }
                if (routeRunning) {
                    autoHandler.postDelayed(this, 1000);
                }
            }
        };
        autoHandler.post(autoRunner);
    }

    private void updateLoopTips() {
        if (loopTotal > 0) {
            int lap = Math.min(loopTotal - loopRemaining + 1, loopTotal);
            routeTips.setText("自动行进中... 第" + lap + "/" + loopTotal + "圈");
        } else {
            routeTips.setText("自动行进中...");
        }
    }

    public boolean isRouteRunning() {
        return routeRunning;
    }

    public double[] doAutoStep() {
        if (!routeRunning || routePoints.isEmpty()) return null;

        if (routeIndex >= routePoints.size()) {
            return null;
        }

        LatLng target = routePoints.get(routeIndex);
        double[] curWgs = GeoUtil.bd2wgs(mkPt.longitude, mkPt.latitude);
        double[] tgtWgs = GeoUtil.bd2wgs(target.longitude, target.latitude);

        double dx = tgtWgs[0] - curWgs[0];
        double dy = tgtWgs[1] - curWgs[1];
        double dist = Math.sqrt(dx * dx + dy * dy);

        double step = JoyStk.getSpeed() / 111000.0;

        if (dist <= step * 1.5) {
            mkPt = target;
            double[] wgs = GeoUtil.bd2wgs(target.longitude, target.latitude);
            if (bm != null) {
                bm.clear();
                redrawRouteRemaining();
                bm.animateMapStatus(MapStatusUpdateFactory.newLatLng(target));
            }
            routeIndex++;
            return wgs;
        } else {
            double ratio = step / dist;
            double newLng = mkPt.longitude + (target.longitude - mkPt.longitude) * ratio;
            double newLat = mkPt.latitude + (target.latitude - mkPt.latitude) * ratio;
            mkPt = new LatLng(newLat, newLng);
            return GeoUtil.bd2wgs(newLng, newLat);
        }
    }

    private void redrawRouteRemaining() {
        if (routePoints.isEmpty()) return;
        List<LatLng> pts = new ArrayList<>();
        for (int i = routeIndex; i < routePoints.size(); i++) pts.add(routePoints.get(i));
        if (pts.size() >= 2) {
            bm.addOverlay(new PolylineOptions().width(8).color(0xCCFFB300).points(pts));
        }
        for (int i = routeIndex; i < routePoints.size(); i++) {
            bm.addOverlay(new MarkerOptions().position(routePoints.get(i)).icon(gPin).title(String.valueOf(i + 1)));
        }
    }

    //开启地图的定位图层
    private void initLoc() {
        try {
            // 定位初始化
            lc = new LocationClient(this);
            lc.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
                    if (bdLocation == null || mv == null) {
                        return;
                    }

                    int err = bdLocation.getLocType();
                    if (err == 62 || err == 63) {
                        lc.requestLocation();
                        return;
                    }

                    double lat = bdLocation.getLatitude();
                    double lng = bdLocation.getLongitude();
                    if (lat == 0.0 && lng == 0.0) {
                        return;
                    }

                    curCity = bdLocation.getCity();
                    clt = lat;
                    cln = lng;
                    MyLocationData locData = new MyLocationData.Builder()
                            .accuracy(bdLocation.getRadius())
                            .direction(cdir)
                            .latitude(lat)
                            .longitude(lng).build();
                    bm.setMyLocationData(locData);
                    MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, null);
                    bm.setMyLocationConfiguration(configuration);

                    if (fstLoc) {
                        fstLoc = false;
                        mkPt = new LatLng(lat, lng);
                        MapStatus.Builder builder = new MapStatus.Builder();
                        builder.target(mkPt).zoom(18.0f);
                        bm.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                        XLog.i("First Baidu LatLng: " + mkPt);
                    }
                }
                /**
                 * 错误的状态码
                 * <a><a href="http://lbsyun.baidu.com/index.php?title=android-locsdk/guide/addition-func/error-code">...</a></a>
                 * <p>
                 * 回调定位诊断信息，开发者可以根据相关信息解决定位遇到的一些问题
                 *
                 * @param locType      当前定位类型
                 * @param diagnosticType  诊断类型（1~9）
                 * @param diagnosticMessage 具体的诊断信息释义
                 */
                @Override
                public void onLocDiagnosticMessage(int locType, int diagnosticType, String diagnosticMessage) {
                    XLog.i("Baidu ERROR: " + locType + "-" + diagnosticType + "-" + diagnosticMessage);
                }
            });
            LocationClientOption locationOption = getLocOpt();
            //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
            lc.setLocOption(locationOption);
            //开始定位
            lc.start();
        } catch (Exception e) {
            XLog.e("ERROR: initLoc");
        }
    }

    @NonNull
    private static LocationClientOption getLocOpt() {
        LocationClientOption locationOption = new LocationClientOption();
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        locationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        locationOption.setCoorType("bd09ll");
        //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        locationOption.setScanSpan(1000);
        //可选，设置是否需要地址信息，默认不需要
        locationOption.setIsNeedAddress(true);
        //可选，设置是否需要设备方向结果
        locationOption.setNeedDeviceDirect(false);
        //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.setLocationNotify(true);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.setIgnoreKillProcess(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationOption.setIsNeedLocationDescribe(false);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.setIsNeedLocationPoiList(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        locationOption.SetIgnoreCacheException(true);
        //可选，默认false，设置是否开启Gps定位
        //locationOption.setOpenGps(true);
        locationOption.setOpenGnss(true);
        //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.setIsNeedAltitude(false);
        return locationOption;
    }

    //地图上各按键的监听
    private void initBtn() {
        RadioGroup mGroupMapType = this.findViewById(R.id.RadioGroupMapType);
        mGroupMapType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.mapNormal) {
                bm.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            }

            if (checkedId == R.id.mapSatellite) {
                bm.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });

        ImageButton zoomInBtn = this.findViewById(R.id.zoom_in);
        zoomInBtn.setOnClickListener(v -> bm.animateMapStatus(MapStatusUpdateFactory.zoomIn()));

        ImageButton zoomOutBtn = this.findViewById(R.id.zoom_out);
        zoomOutBtn.setOnClickListener(v -> bm.animateMapStatus(MapStatusUpdateFactory.zoomOut()));

        ImageButton inputPosBtn = this.findViewById(R.id.input_pos);
        inputPosBtn.setOnClickListener(v -> {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeAct.this);
            builder.setTitle("请输入经度和纬度");
            View view = LayoutInflater.from(HomeAct.this).inflate(R.layout.location_input, null);
            builder.setView(view);
            dialog = builder.show();

            EditText dialog_lng = view.findViewById(R.id.joystick_longitude);
            EditText dialog_lat = view.findViewById(R.id.joystick_latitude);
            RadioButton rbBD = view.findViewById(R.id.pos_type_bd);

            Button btnGo = view.findViewById(R.id.input_position_ok);
            btnGo.setOnClickListener(v2 -> {
                String dialog_lng_str = dialog_lng.getText().toString();
                String dialog_lat_str = dialog_lat.getText().toString();

                if (TextUtils.isEmpty(dialog_lng_str) || TextUtils.isEmpty(dialog_lat_str)) {
                    SysUtil.toast(HomeAct.this,getResources().getString(R.string.app_error_input));
                } else {
                    double dialog_lng_double = Double.parseDouble(dialog_lng_str);
                    double dialog_lat_double = Double.parseDouble(dialog_lat_str);

                    if (dialog_lng_double > 180.0 || dialog_lng_double < -180.0) {
                        SysUtil.toast(HomeAct.this,  getResources().getString(R.string.app_error_longitude));
                    } else {
                        if (dialog_lat_double > 90.0 || dialog_lat_double < -90.0) {
                            SysUtil.toast(HomeAct.this,  getResources().getString(R.string.app_error_latitude));
                        } else {
                            if (rbBD.isChecked()) {
                                mkPt = new LatLng(dialog_lat_double, dialog_lng_double);
                            } else {
                                double[] bdLonLat = GeoUtil.wgs2bd09(dialog_lat_double, dialog_lng_double);
                                mkPt = new LatLng(bdLonLat[1], bdLonLat[0]);
                            }
                            mkNm = "手动输入的坐标";

                            markPt();

                            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mkPt);
                            bm.setMapStatus(mapstatusupdate);

                            dialog.dismiss();
                        }
                    }
                }
            });

            Button btnCancel = view.findViewById(R.id.input_position_cancel);
            btnCancel.setOnClickListener(v1 -> dialog.dismiss());
        });
    }

    //标定选择的位置
    private void markPt() {
        if (mkPt != null) {
            MarkerOptions ooA = new MarkerOptions().position(mkPt).icon(gPin);
            bm.clear();
            bm.addOverlay(ooA);
        }
    }

    // 在地图上显示位置
    public static boolean showLoc(String name, String bd09Longitude, String bd09Latitude) {
        boolean ret = true;

        try {
            if (!bd09Longitude.isEmpty() && !bd09Latitude.isEmpty()) {
                mkNm = name;
                mkPt = new LatLng(Double.parseDouble(bd09Latitude), Double.parseDouble(bd09Longitude));
                MarkerOptions ooA = new MarkerOptions().position(mkPt).icon(gPin);
                bm.clear();
                bm.addOverlay(ooA);
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mkPt);
                bm.setMapStatus(mapstatusupdate);
            }
        } catch (Exception e) {
            ret = false;
            XLog.e("ERROR: showHistoryLocation");
        }

        return ret;
    }

    private void initGo() {
        btnGo = findViewById(R.id.faBtnStart);
        btnGo.setOnClickListener(this::doGo);
    }

    private void startLoc() {
        Intent serviceGoIntent = new Intent(HomeAct.this, LocSvc.class);
        bindService(serviceGoIntent, cnn, BIND_AUTO_CREATE);    // 绑定服务和活动，之后活动就可以去调服务的方法了
        double[] latLng = GeoUtil.bd2wgs(mkPt.longitude, mkPt.latitude);
        serviceGoIntent.putExtra(K_LNG, latLng[0]);
        serviceGoIntent.putExtra(K_LAT, latLng[1]);
        double alt = Double.parseDouble(sp.getString("setting_altitude", "55.0"));
        serviceGoIntent.putExtra(K_ALT, alt);

        startForegroundService(serviceGoIntent);
        XLog.d("startForegroundService: LocSvc");

        svcOn = true;
    }

    private void stopLoc() {
        unbindService(cnn); // 解绑服务，服务要记得解绑，不要造成内存泄漏
        Intent serviceGoIntent = new Intent(HomeAct.this, LocSvc.class);
        stopService(serviceGoIntent);
        svcOn = false;
    }

    private void doGo(View v) {
        if (!SysUtil.isNetOk(this)) {
            SysUtil.toast(this, getResources().getString(R.string.app_error_network));
            return;
        }

        if (!SysUtil.isGpsOn(this)) {
            SysUtil.showGpsDlg(this);
            return;
        }

        if (!Settings.canDrawOverlays(getApplicationContext())) {//悬浮窗权限判断
            SysUtil.showFloatDlg(this);
            XLog.e("无悬浮窗权限!");
            return;
        }

        if (svcOn) {
            if (mkPt == null) {
                stopLoc();
                Snackbar.make(v, "模拟位置已终止", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                btnGo.setImageResource(R.drawable.ic_position);
            } else {
                double[] latLng = GeoUtil.bd2wgs(mkPt.longitude, mkPt.latitude);
                double alt = Double.parseDouble(sp.getString("setting_altitude", "55.0"));
                svc.setPosition(latLng[0], latLng[1], alt);
                Snackbar.make(v, "已传送到新位置", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                saveLoc(mkPt.longitude, mkPt.latitude);

                bm.clear();
                mkPt = null;

                if (SysUtil.isWifiOn(HomeAct.this)) {
                    SysUtil.showWifiDlg(HomeAct.this);
                }
            }
        } else {
            if (!SysUtil.isMockOk(this)) {
                SysUtil.showMockDlg(this);
                XLog.e("无模拟位置权限!");
            } else {
                if (mkPt == null) {
                    Snackbar.make(v, "请先点击地图位置或者搜索位置", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    startLoc();
                    btnGo.setImageResource(R.drawable.ic_fly);
                    Snackbar.make(v, "模拟位置已启动", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    if (sp.getBoolean("first_loc_hint", true)) {
                        new AlertDialog.Builder(this)
                                .setTitle("提示")
                                .setMessage("首次运行定位服务时，部分手机可能会闪退，别担心，重新打开即可正常使用。")
                                .setPositiveButton("知道了", (d, w) -> {
                                    sp.edit().putBoolean("first_loc_hint", false).apply();
                                })
                                .show();
                    }

                    saveLoc(mkPt.longitude, mkPt.latitude);
                    bm.clear();
                    mkPt = null;

                    if (SysUtil.isWifiOn(HomeAct.this)) {
                        SysUtil.showWifiDlg(HomeAct.this);
                    }
                }
            }
        }
    }

    /*============================== 历史记录 相关 ==============================*/
    private void initDb() {
        try {
            // 定位历史
            LocDB dbLocation = new LocDB(getApplicationContext());
            dbLoc = dbLocation.getWritableDatabase();
            // 搜索历史
            SrchDB dbHistory = new SrchDB(getApplicationContext());
            dbSrch = dbHistory.getWritableDatabase();
        } catch (Exception e) {
            XLog.e("ERROR: sqlite init error");
        }
    }

    //获取查询历史
    private List<Map<String, Object>> getHist() {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            Cursor cursor = dbSrch.query(SrchDB.TABLE_NAME, null,
                    SrchDB.COL_ID + " > ?", new String[] {"0"},
                    null, null, SrchDB.COL_TS + " DESC", null);

            while (cursor.moveToNext()) {
                Map<String, Object> searchHistoryItem = new HashMap<>();
                searchHistoryItem.put(SrchDB.COL_KEY, cursor.getString(1));
                searchHistoryItem.put(SrchDB.COL_DESC, cursor.getString(2));
                searchHistoryItem.put(SrchDB.COL_TS, "" + cursor.getInt(3));
                searchHistoryItem.put(SrchDB.COL_ISLOC, "" + cursor.getInt(4));
                searchHistoryItem.put(SrchDB.COL_LNG_BD, cursor.getString(7));
                searchHistoryItem.put(SrchDB.COL_LAT_BD, cursor.getString(8));
                data.add(searchHistoryItem);
            }
            cursor.close();
        } catch (Exception e) {
            XLog.e("ERROR: getHist");
        }

        return data;
    }

    // 记录请求的位置信息
    private void saveLoc(double lng, double lat) {
        //参数坐标系：bd09
        final String safeCode = BuildConfig.MAPS_SAFE_CODE;
        final String ak = sp.getString("setting_map_key", BuildConfig.MAPS_API_KEY);
        double[] latLng = GeoUtil.bd2wgs(lng, lat);
        //bd09坐标的位置信息
        String mapApiUrl = "https://api.map.baidu.com/reverse_geocoding/v3/?ak=" + ak + "&output=json&coordtype=bd09ll" + "&location=" + lat + "," + lng + "&mcode=" + safeCode;

        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        final Call call = http.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //http 请求失败
                XLog.e("HTTP: HTTP GET FAILED");
                //插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put(LocDB.COL_LOC, mkNm);
                contentValues.put(LocDB.COL_LNG, String.valueOf(latLng[0]));
                contentValues.put(LocDB.COL_LAT, String.valueOf(latLng[1]));
                contentValues.put(LocDB.COL_TS, System.currentTimeMillis() / 1000);
                contentValues.put(LocDB.COL_LNG_BD, Double.toString(lng));
                contentValues.put(LocDB.COL_LAT_BD, Double.toString(lat));

                LocDB.save(dbLoc, contentValues);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String resp = responseBody.string();
                    try {
                        JSONObject getRetJson = new JSONObject(resp);

                        if (Integer.parseInt(getRetJson.getString("status")) == 0) { // 位置获取成功
                            JSONObject posInfoJson = getRetJson.getJSONObject("result");
                            String formatted_address = posInfoJson.getString("formatted_address");
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(LocDB.COL_LOC, formatted_address);
                            contentValues.put(LocDB.COL_LNG, String.valueOf(latLng[0]));
                            contentValues.put(LocDB.COL_LAT, String.valueOf(latLng[1]));
                            contentValues.put(LocDB.COL_TS, System.currentTimeMillis() / 1000);
                            contentValues.put(LocDB.COL_LNG_BD, Double.toString(lng));
                            contentValues.put(LocDB.COL_LAT_BD, Double.toString(lat));
                            LocDB.save(dbLoc, contentValues);
                        } else {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(LocDB.COL_LOC, mkNm == null ? getRetJson.getString("message"): mkNm);
                            contentValues.put(LocDB.COL_LNG, String.valueOf(latLng[0]));
                            contentValues.put(LocDB.COL_LAT, String.valueOf(latLng[1]));
                            contentValues.put(LocDB.COL_TS, System.currentTimeMillis() / 1000);
                            contentValues.put(LocDB.COL_LNG_BD, Double.toString(lng));
                            contentValues.put(LocDB.COL_LAT_BD, Double.toString(lat));
                            LocDB.save(dbLoc, contentValues);
                        }
                    } catch (JSONException e) {
                        XLog.e("JSON: resolve json error");
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(LocDB.COL_LOC, mkNm == null ? getResources().getString(R.string.history_location_default_name) : mkNm);
                        contentValues.put(LocDB.COL_LOC, mkNm);
                        contentValues.put(LocDB.COL_LNG, String.valueOf(latLng[0]));
                        contentValues.put(LocDB.COL_LAT, String.valueOf(latLng[1]));
                        contentValues.put(LocDB.COL_TS, System.currentTimeMillis() / 1000);
                        contentValues.put(LocDB.COL_LNG_BD, Double.toString(lng));
                        contentValues.put(LocDB.COL_LAT_BD, Double.toString(lat));
                        LocDB.save(dbLoc, contentValues);
                    }
                }
            }
        });
    }

    /*============================== sv 相关 ==============================*/
    private void initSrch() {
        sly = findViewById(R.id.search_linear);
        hly = findViewById(R.id.search_history_linear);

        sl = findViewById(R.id.search_list_view);
        sl.setOnItemClickListener((parent, view, position, id) -> {
            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            mkNm = ((TextView) view.findViewById(R.id.poi_name)).getText().toString();
            mkPt = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mkPt);
            bm.setMapStatus(mapstatusupdate);

            markPt();

            double[] latLng = GeoUtil.bd2wgs(mkPt.longitude, mkPt.latitude);

            // sl.setVisibility(View.GONE);
            //搜索历史 插表参数
            ContentValues contentValues = new ContentValues();
            contentValues.put(SrchDB.COL_KEY, mkNm);
            contentValues.put(SrchDB.COL_DESC, ((TextView) view.findViewById(R.id.poi_address)).getText().toString());
            contentValues.put(SrchDB.COL_ISLOC, SrchDB.TYPE_RSLT);
            contentValues.put(SrchDB.COL_LNG_BD, lng);
            contentValues.put(SrchDB.COL_LAT_BD, lat);
            contentValues.put(SrchDB.COL_LNG, String.valueOf(latLng[0]));
            contentValues.put(SrchDB.COL_LAT, String.valueOf(latLng[1]));
            contentValues.put(SrchDB.COL_TS, System.currentTimeMillis() / 1000);

            SrchDB.save(dbSrch, contentValues);
            sly.setVisibility(View.GONE);
            si.collapseActionView();
        });
        //搜索历史列表的点击监听
        shl = findViewById(R.id.search_history_list_view);
        shl.setOnItemClickListener((parent, view, position, id) -> {
            String searchDescription = ((TextView) view.findViewById(R.id.search_description)).getText().toString();
            String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
            String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();

            //如果是定位搜索
            if (searchIsLoc.equals("1")) {
                String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
                // mkNm = ((TextView) view.findViewById(R.id.poi_name)).getText().toString();
                mkPt = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(mkPt);
                bm.setMapStatus(mapstatusupdate);

                markPt();

                double[] latLng = GeoUtil.bd2wgs(mkPt.longitude, mkPt.latitude);

                //设置列表不可见
                hly.setVisibility(View.GONE);
                si.collapseActionView();
                //更新表
                ContentValues contentValues = new ContentValues();
                contentValues.put(SrchDB.COL_KEY, searchKey);
                contentValues.put(SrchDB.COL_DESC, searchDescription);
                contentValues.put(SrchDB.COL_ISLOC, SrchDB.TYPE_RSLT);
                contentValues.put(SrchDB.COL_LNG_BD, lng);
                contentValues.put(SrchDB.COL_LAT_BD, lat);
                contentValues.put(SrchDB.COL_LNG, String.valueOf(latLng[0]));
                contentValues.put(SrchDB.COL_LAT, String.valueOf(latLng[1]));
                contentValues.put(SrchDB.COL_TS, System.currentTimeMillis() / 1000);

                SrchDB.save(dbSrch, contentValues);
            } else if (searchIsLoc.equals("0")) { //如果仅仅是搜索
                try {
                    sv.setQuery(searchKey, true);
                } catch (Exception e) {
                    SysUtil.toast(this, getResources().getString(R.string.app_error_search));
                    XLog.e(getResources().getString(R.string.app_error_search));
                }
            } else {
                XLog.e(getResources().getString(R.string.app_error_param));
            }
        });
        shl.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(HomeAct.this)
                    .setTitle("警告")//这里是表头的内容
                    .setMessage("确定要删除该项搜索记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",(dialog, which) -> {
                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();

                        try {
                            dbSrch.delete(SrchDB.TABLE_NAME, SrchDB.COL_KEY + " = ?", new String[] {searchKey});
                            //删除成功
                            //展示搜索历史
                            List<Map<String, Object>> data = getHist();

                            if (!data.isEmpty()) {
                                SimpleAdapter simAdapt = new SimpleAdapter(
                                        HomeAct.this,
                                        data,
                                        R.layout.search_item,
                                        new String[] {SrchDB.COL_KEY,
                                                SrchDB.COL_DESC,
                                                SrchDB.COL_TS,
                                                SrchDB.COL_ISLOC,
                                                SrchDB.COL_LNG_BD,
                                                SrchDB.COL_LAT_BD}, // 与下面数组元素要一一对应
                                        new int[] {R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude});
                                shl.setAdapter(simAdapt);
                                hly.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            XLog.e("ERROR: delete database error");
                            SysUtil.toast(HomeAct.this,getResources().getString(R.string.history_delete_error));
                        }
                    })
                    .setNegativeButton("取消",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        });
        //设置搜索建议返回值监听
        ss = SuggestionSearch.newInstance();
        ss.setOnGetSuggestionResultListener(suggestionResult -> {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                SysUtil.toast(this,getResources().getString(R.string.app_search_null));
            } else {
                List<Map<String, Object>> data = getList(suggestionResult);

                SimpleAdapter simAdapt = new SimpleAdapter(
                        HomeAct.this,
                        data,
                        R.layout.search_poi_item,
                        new String[] {K_PNM, K_PAD, K_PLN, K_PLT}, // 与下面数组元素要一一对应
                        new int[] {R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
                sl.setAdapter(simAdapt);
                // sl.setVisibility(View.VISIBLE);
                sly.setVisibility(View.VISIBLE);
            }
        });
    }

    @NonNull
    private static List<Map<String, Object>> getList(SuggestionResult suggestionResult) {
        List<Map<String, Object>> data = new ArrayList<>();
        int retCnt = suggestionResult.getAllSuggestions().size();

        for (int i = 0; i < retCnt; i++) {
            if (suggestionResult.getAllSuggestions().get(i).pt == null) {
                continue;
            }

            Map<String, Object> poiItem = new HashMap<>();
            poiItem.put(K_PNM, suggestionResult.getAllSuggestions().get(i).key);
            poiItem.put(K_PAD, suggestionResult.getAllSuggestions().get(i).city + " " + suggestionResult.getAllSuggestions().get(i).district);
            poiItem.put(K_PLN, "" + suggestionResult.getAllSuggestions().get(i).pt.longitude);
            poiItem.put(K_PLT, "" + suggestionResult.getAllSuggestions().get(i).pt.latitude);
            data.add(poiItem);
        }
        return data;
    }

    /*============================== 更新 相关 ==============================*/
    private void initUpd() {
        dm =(DownloadManager) HomeAct.this.getSystemService(DOWNLOAD_SERVICE);

        // 用于监听下载完成后，转到安装界面
        dRcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                instUpd();
            }
        };
        registerReceiver(dRcv, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void chkUpd(boolean result) {
        String mapApiUrl = "https://api.github.com/repos/gongjiantao/Map_mode/releases/latest";

        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        final Call call = http.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                XLog.i("更新检测失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String resp = responseBody.string();
                    // 注意，该请求在子线程，不能直接操作界面
                    runOnUiThread(() -> {
                        try {
                            JSONObject getRetJson = new JSONObject(resp);
                            String curVersion = SysUtil.getVer(HomeAct.this);

                            if (curVersion != null
                                    && (!getRetJson.getString("name").contains(curVersion)
                                    || !getRetJson.getString("tag_name").contains(curVersion))) {
                                final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(HomeAct.this).create();
                                alertDialog.show();
                                alertDialog.setCancelable(false);
                                Window window = alertDialog.getWindow();
                                if (window != null) {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);      // 防止出现闪屏
                                    window.setContentView(R.layout.update);
                                    window.setGravity(Gravity.CENTER);
                                    window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

                                    TextView updateTitle = window.findViewById(R.id.update_title);
                                    updateTitle.setText(getRetJson.getString("name"));
                                    TextView updateTime = window.findViewById(R.id.update_time);
                                    updateTime.setText(getRetJson.getString("created_at"));
                                    TextView updateCommit = window.findViewById(R.id.update_commit);
                                    updateCommit.setText(getRetJson.getString("target_commitish"));

                                    TextView updateContent = window.findViewById(R.id.update_content);
                                    final Markwon markwon = Markwon.create(HomeAct.this);
                                    markwon.setMarkdown(updateContent, getRetJson.getString("body"));

                                    Button updateCancel = window.findViewById(R.id.update_ignore);
                                    updateCancel.setOnClickListener(v -> alertDialog.cancel());

                                    /* 这里用来保存下载地址 */
                                    JSONArray jsonArray = new JSONArray(getRetJson.getString("assets"));
                                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                                    String download_url = jsonObject.getString("browser_download_url");
                                    ufn = jsonObject.getString("name");

                                    Button updateAgree = window.findViewById(R.id.update_agree);
                                    updateAgree.setOnClickListener(v -> {
                                        alertDialog.cancel();
                                        SysUtil.toast(HomeAct.this, getResources().getString(R.string.update_downloading));
                                        dwnUpd(download_url);
                                    });
                                }
                            } else {
                                if (result) {
                                    SysUtil.toast(HomeAct.this, getResources().getString(R.string.update_last));
                                }
                            }
                        } catch (JSONException e) {
                            XLog.e("ERROR: resolve json");
                        }
                    });
                }
            }
        });
    }

    private void dwnUpd(String url) {
        if (dm == null) {
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedOverRoaming(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(SysUtil.getAppName(this));
        request.setDescription("正在下载新版本...");
        request.setMimeType("application/vnd.android.package-archive");

        // DownloadManager不会覆盖已有的同名文件，需要自己来删除已存在的文件
        File file = new File(getExternalFilesDir("Updates"), ufn);
        if (file.exists()) {
            if(!file.delete()) {
                return;
            }
        }
        request.setDestinationUri(Uri.fromFile(file));

        did = dm.enqueue(request);
    }

    private void instUpd() {
        Intent install = new Intent(Intent.ACTION_VIEW);
        Uri downloadFileUri = dm.getUriForDownloadedFile(did);
        File file = new File(getExternalFilesDir("Updates"), ufn);
        if (downloadFileUri != null) {
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // 在Broadcast中启动活动需要添加Intent.FLAG_ACTIVITY_NEW_TASK
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);    //添加这一句表示对目标应用临时授权该Uri所代表的文件
            install.addCategory("android.intent.category.DEFAULT");
            install.setDataAndType(SndUtil.getUri(HomeAct.this, file), "application/vnd.android.package-archive");
            startActivity(install);
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            intent.addCategory("android.intent.category.DEFAULT");
            startActivity(intent);
        }
    }
}

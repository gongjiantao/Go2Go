package com.gongjiantao.mode.joystick;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.SearchView;

import androidx.preference.PreferenceManager;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.gongjiantao.mode.database.LocDB;
import com.gongjiantao.mode.HistAct;
import com.gongjiantao.mode.HomeAct;
import com.gongjiantao.mode.R;
import com.gongjiantao.mode.utils.SysUtil;
import com.gongjiantao.mode.utils.GeoUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoyStk extends View {
    private static final int TICK = 1000;    /* 移动的时间间隔，单位 ms */
    private static final int WIN_J = 0;
    private static final int WIN_M = 1;
    private static final int WIN_H = 2;

    private final Context ctx;
    private WindowManager.LayoutParams wp;
    private WindowManager wm;
    private int curWin = WIN_J;
    private final LayoutInflater inf;
    private boolean walk;
    private ImageButton bWalk;
    private boolean run;
    private ImageButton bRun;
    private boolean bike;
    private ImageButton bBike;
    private JoyStkClickLsn lsn;

    // 移动
    private View jly;
    private SysUtil.TimerX tmr;
    private boolean moving;
    private double spd = 1.2;        /* 默认的速度，单位 m/s */
    private static double sSpd = 1.2;
    private double alt = 55.0;
    private double ang = 0;
    private double mr = 0;
    private double dlng = 0;
    private double dlat = 0;
    private final SharedPreferences sp;
    /* 历史记录悬浮窗相关 */
    private FrameLayout hly;
    private final List<Map<String, Object>> allRec = new ArrayList<> ();
    private TextView noTxt;
    private ListView lv;
    /* 地图悬浮窗相关 */
    private FrameLayout mly;
    private MapView mv;
    private BaiduMap bm;
    private LatLng curPt;
    private LatLng mkPt;
    private SuggestionSearch ss;
    private ListView sl;
    private LinearLayout sly;

    public JoyStk(Context context) {
        super(context);
        this.ctx = context;

        sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        initWm();

        inf = LayoutInflater.from(ctx);

        if (inf != null) {
            initJView();

            initMView();

            initHView();
        }
    }

    public JoyStk(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.ctx = context;

        sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        initWm();

        inf = LayoutInflater.from(ctx);

        if (inf != null) {
            initJView();

            initMView();

            initHView();
        }
    }

    public JoyStk(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;

        sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        initWm();

        inf = LayoutInflater.from(ctx);

        if (inf != null) {
            initJView();

            initMView();

            initHView();
        }
    }

    public void setPos(double lng, double lat, double altitude) {
        double[] lngLat = GeoUtil.wgs2bd09(lng, lat);
        curPt = new LatLng(lngLat[1], lngLat[0]);
        alt = altitude;

        rstBMap();
    }

    public void show() {
        switch (curWin) {
            case WIN_M:
                if (jly.getParent() != null) {
                    wm.removeView(jly);
                }
                if (hly.getParent() != null) {
                    wm.removeView(hly);
                }
                if (mly.getParent() == null) {
                    wm.addView(mly, wp);
                    rstBMap();
                }
                break;
            case WIN_H:
                if (mly.getParent() != null) {
                    wm.removeView(mly);
                }
                if (jly.getParent() != null) {
                    wm.removeView(jly);
                }
                if (hly.getParent() == null) {
                    wm.addView(hly, wp);
                }
                break;
            case WIN_J:
                if (mly.getParent() != null) {
                    wm.removeView(mly);
                }
                if (hly.getParent() != null) {
                    wm.removeView(hly);
                }
                if (jly.getParent() == null) {
                    wm.addView(jly, wp);
                }
                break;
        }
    }

    public void hide() {
        if (mly.getParent() != null) {
            wm.removeViewImmediate(mly);
        }

        if (jly.getParent() != null) {
            wm.removeViewImmediate(jly);
        }

        if (hly.getParent() != null) {
            wm.removeViewImmediate(hly);
        }
    }

    public void destroy() {
        if (mly.getParent() != null) {
            wm.removeViewImmediate(mly);
        }

        if (jly.getParent() != null) {
            wm.removeViewImmediate(jly);
        }

        if (hly.getParent() != null) {
            wm.removeViewImmediate(hly);
        }

        bm.setMyLocationEnabled(false);
        mv.onDestroy();
    }

    public void setListener(JoyStkClickLsn lsn) {
        this.lsn = lsn;
    }

    public static double getSpeed() {
        return sSpd;
    }

    private void initWm() {
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        wp = new WindowManager.LayoutParams();
        wp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        wp.format = PixelFormat.RGBA_8888;
        wp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE      // 不添加这个将导致游戏无法启动（MIUI12）,添加之后导致键盘无法显示
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        wp.gravity = Gravity.START | Gravity.TOP;
        wp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wp.x = 300;
        wp.y = 300;
    }

    @SuppressLint("InflateParams")
    private void initJView() {
        /* 移动计时器 */
        tmr = new SysUtil.TimerX(TICK, TICK);
        tmr.setListener(new SysUtil.TimerX.TimerXLsn() {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                dlng = spd * (double)(TICK / 1000) * mr * Math.cos(ang * 2 * Math.PI / 360) / 1000;
                dlat = spd * (double)(TICK / 1000) * mr * Math.sin(ang * 2 * Math.PI / 360) / 1000;
                lsn.onMoveInfo(spd, dlng, dlat, 90.0F-ang);
                tmr.start();
            }
        });
        // 获取参数区设置的速度
        try {
            spd = Double.parseDouble(sp.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));
        } catch (NumberFormatException e) {
            spd = 1.2;
        }
        sSpd = spd;
        jly = inf.inflate(R.layout.joystick, null);

        /* 整个摇杆拖动事件处理 */
        jly.setOnTouchListener(new JoyStkTouchLsn());

        /* 位置按钮点击事件处理 */
        ImageButton bPos = jly.findViewById(R.id.joystick_position);
        bPos.setOnClickListener(v -> {
            if (mly.getParent() == null) {
                curWin = WIN_M;
                show();
            }
        });

        /* 历史按钮点击事件处理 */
        ImageButton bHis = jly.findViewById(R.id.joystick_history);
        bHis.setOnClickListener(v -> {
            if (hly.getParent() == null) {
                curWin = WIN_H;
                show();
            }
        });

        /* 步行按键的点击处理 */
        bWalk = jly.findViewById(R.id.joystick_walk);
        bWalk.setOnClickListener(v -> {
            if (!walk) {
                bWalk.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));
                walk = true;
                bRun.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                run = false;
                bBike.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                bike = false;
                try {
                    spd = Double.parseDouble(sp.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));
                } catch (NumberFormatException e) {
                    spd = 1.2;
                }
                sSpd = spd;
            }
        });
        /* 默认为步行 */
        walk = true;
        bWalk.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));
        /* 跑步按键的点击处理 */
        run = false;
        bRun = jly.findViewById(R.id.joystick_run);
        bRun.setOnClickListener(v -> {
            if (!run) {
                bRun.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));
                run = true;
                bWalk.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                walk = false;
                bBike.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                bike = false;
                try {
                    spd = Double.parseDouble(sp.getString("setting_run", getResources().getString(R.string.setting_run_default)));
                } catch (NumberFormatException e) {
                    spd = 3.6;
                }
                sSpd = spd;
            }
        });
        /* 自行车按键的点击处理 */
        bike = false;
        bBike = jly.findViewById(R.id.joystick_bike);
        bBike.setOnClickListener(v -> {
            if (!bike) {
                bBike.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));
                bike = true;
                bWalk.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                walk = false;
                bRun.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                run = false;
                try {
                    spd = Double.parseDouble(sp.getString("setting_bike", getResources().getString(R.string.setting_bike_default)));
                } catch (NumberFormatException e) {
                    spd = 10.0;
                }
                sSpd = spd;
            }
        });
        /* 方向键点击处理 */
        RckrView rv = jly.findViewById(R.id.joystick_rocker);
        rv.setListener(this::procDir);

        /* 方向键点击处理 */
        BtnView bv = jly.findViewById(R.id.joystick_button);
        bv.setListener(this::procDir);

        /* 这里用来决定摇杆类型 */
        if (sp.getString("setting_joystick_type", "0").equals("0")) {
            rv.setVisibility(VISIBLE);
            bv.setVisibility(GONE);
        } else {
            rv.setVisibility(GONE);
            bv.setVisibility(VISIBLE);
        }
    }

    private void procDir(boolean auto, double angle, double r) {
        if (r <= 0) {
            tmr.cancel();
            moving = false;
        } else {
            ang = angle;
            mr = r;
            if (auto) {
                if (!moving) {
                    tmr.start();
                    moving = true;
                }
            } else {
                tmr.cancel();
                moving = false;
                // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）且转换为 km
                dlng = spd * (double)(TICK / 1000) * mr * Math.cos(ang * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                dlat = spd * (double)(TICK / 1000) * mr * Math.sin(ang * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                lsn.onMoveInfo(spd, dlng, dlat, 90.0F-ang);
            }
        }
    }

    private class JoyStkTouchLsn implements OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;

                    wp.x += movedX;
                    wp.y += movedY;
                    wm.updateViewLayout(view, wp);
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    public interface JoyStkClickLsn {
        void onMoveInfo(double speed, double dlng, double dlat, double angle);
        void onPositionInfo(double lng, double lat, double alt);
    }


    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private void initMView() {
        mly = (FrameLayout)inf.inflate(R.layout.joystick_map, null);
        mly.setOnTouchListener(new JoyStkTouchLsn());

        sl = mly.findViewById(R.id.map_search_list_view);
        sly = mly.findViewById(R.id.map_search_linear);
        ss = SuggestionSearch.newInstance();
        ss.setOnGetSuggestionResultListener(suggestionResult -> {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                SysUtil.toast(ctx,getResources().getString(R.string.app_search_null));
            } else {
                List<Map<String, Object>> data = new ArrayList<>();
                int cnt = suggestionResult.getAllSuggestions().size();

                for (int i = 0; i < cnt; i++) {
                    if (suggestionResult.getAllSuggestions().get(i).pt == null) {
                        continue;
                    }

                    Map<String, Object> poiItem = new HashMap<>();
                    poiItem.put(HomeAct.K_PNM, suggestionResult.getAllSuggestions().get(i).key);
                    poiItem.put(HomeAct.K_PAD, suggestionResult.getAllSuggestions().get(i).city + " " + suggestionResult.getAllSuggestions().get(i).district);
                    poiItem.put(HomeAct.K_PLN, "" + suggestionResult.getAllSuggestions().get(i).pt.longitude);
                    poiItem.put(HomeAct.K_PLT, "" + suggestionResult.getAllSuggestions().get(i).pt.latitude);
                    data.add(poiItem);
                }

                SimpleAdapter simAdapt = new SimpleAdapter(
                        ctx,
                        data,
                        R.layout.search_poi_item,
                        new String[] {HomeAct.K_PNM, HomeAct.K_PAD, HomeAct.K_PLN, HomeAct.K_PLT}, // 与下面数组元素要一一对应
                        new int[] {R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
                sl.setAdapter(simAdapt);
                sly.setVisibility(View.VISIBLE);
            }
        });
        sl.setOnItemClickListener((parent, view, position, id) -> {
            sly.setVisibility(View.GONE);

            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            markBaiduMap(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
        });

        TextView tips = mly.findViewById(R.id.joystick_map_tips);
        SearchView mSearchView = mly.findViewById(R.id.joystick_map_searchView);
        mSearchView.setOnSearchClickListener(v -> {
            tips.setVisibility(GONE);

            // 特殊处理：这里让搜索框获取焦点，以显示输入法
            wp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            wm.updateViewLayout(mly, wp);
        });
        mSearchView.setOnCloseListener(() -> {
            tips.setVisibility(VISIBLE);
            sly.setVisibility(GONE);

            // 关闭时清除焦点
            wp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            wm.updateViewLayout(mly, wp);

            return false;       /* 这里必须返回false，否则需要自行处理搜索框的折叠 */
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && newText.length() > 0) {
                    try {
                        ss.requestSuggestion((new SuggestionSearchOption())
                                .keyword(newText)
                                .city(HomeAct.curCity)
                        );
                    } catch (Exception e) {
                        SysUtil.toast(ctx,getResources().getString(R.string.app_error_search));
                        e.printStackTrace();
                    }
                } else {
                    sly.setVisibility(GONE);
                }

                return true;
            }
        });

        ImageButton btnGo = mly.findViewById(R.id.btnGo);
        btnGo.setOnClickListener(v -> {
            // 关闭时清除焦点
            wp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            wm.updateViewLayout(mly, wp);

            tips.setVisibility(VISIBLE);
            mSearchView.clearFocus();
            mSearchView.onActionViewCollapsed();

            if (mkPt == null) {
                SysUtil.toast(ctx, getResources().getString(R.string.app_error_location));
            } else {
                if (curPt != mkPt) {
                    curPt = mkPt;
                    mkPt = null;

                    double[] lngLat = GeoUtil.bd2wgs(curPt.longitude, curPt.latitude);
                    lsn.onPositionInfo(lngLat[0], lngLat[1], alt);

                    rstBMap();

                    SysUtil.toast(ctx, getResources().getString(R.string.app_location_ok));
                }
            }
        });
        btnGo.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

        ImageButton btnClose = mly.findViewById(R.id.map_close);
        btnClose.setOnClickListener(v -> {
            // 关闭时清除焦点
            wp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            tips.setVisibility(VISIBLE);
            sly.setVisibility(GONE);
            mSearchView.clearFocus();
            mSearchView.onActionViewCollapsed();

            curWin = WIN_J;
            show();
        });

        ImageButton btnBack = mly.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> rstBMap());
        btnBack.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

        initBaiduMap();
    }

    private void initBaiduMap() {
        mv = mly.findViewById(R.id.map_joystick);
        mv.showZoomControls(false);
        bm = mv.getMap();
        bm.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        bm.setMyLocationEnabled(true);

        bm.setOnMapTouchListener(event -> {

        });

        bm.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            /**
             * 单击地图
             */
            @Override
            public void onMapClick(LatLng point) {
                markBaiduMap(point);
            }

            /**
             * 单击地图中的POI点
             */
            @Override
            public void onMapPoiClick(MapPoi poi) {
                markBaiduMap(poi.getPosition());
            }
        });

        bm.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                markBaiduMap(point);
            }
        });

        bm.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            @Override
            public void onMapDoubleClick(LatLng point) {
                bm.animateMapStatus(MapStatusUpdateFactory.zoomIn());
            }
        });
    }

    private void rstBMap() {
        bm.clear();

        MyLocationData locData = new MyLocationData.Builder()
                .latitude(curPt.latitude)
                .longitude(curPt.longitude)
                .build();
        bm.setMyLocationData(locData);

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(curPt).zoom(18.0f);
        bm.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    private void markBaiduMap(LatLng latLng) {
        mkPt = latLng;

        MarkerOptions ooA = new MarkerOptions().position(latLng).icon(HomeAct.gPin);
        bm.clear();
        bm.addOverlay(ooA);

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(latLng).zoom(18.0f);
        bm.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private void initHView() {
        hly = (FrameLayout)inf.inflate(R.layout.joystick_history, null);
        hly.setOnTouchListener(new JoyStkTouchLsn());

        TextView tips = hly.findViewById(R.id.joystick_his_tips);
        SearchView mSearchView = hly.findViewById(R.id.joystick_his_searchView);
        mSearchView.setOnSearchClickListener(v -> {
            tips.setVisibility(GONE);
            wp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            wm.updateViewLayout(hly, wp);
        });
        mSearchView.setOnCloseListener(() -> {
            tips.setVisibility(VISIBLE);
            wp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            wm.updateViewLayout(hly, wp);
            return false;
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && newText.length() > 0) {
                    try {
                        ss.requestSuggestion((new SuggestionSearchOption()).keyword(newText).city(HomeAct.curCity));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
    }
}



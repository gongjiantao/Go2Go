package com.gongjiantao.mode.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.elvishew.xlog.XLog;
import com.gongjiantao.mode.HomeAct;
import com.gongjiantao.mode.R;
import com.gongjiantao.mode.joystick.JoyStk;

public class LocSvc extends Service {
    // 定位相关变量
    public static final double DEF_LAT = 36.667662;
    public static final double DEF_LNG = 117.027707;
    public static final double DEF_ALT = 55.0D;
    public static final float DEF_BEA = 0.0F;
    private double lat = DEF_LAT;
    private double lng = DEF_LNG;
    private double alt = DEF_ALT;
    private float bea = DEF_BEA;
    private double spd = 1.2;        /* 默认的速度，单位 m/s */
    private static final int MSG_ID = 0;
    private static final String THR_NAME = "ServiceGoLocation";
    private LocationManager lm;
    private HandlerThread ht;
    private Handler hd;
    private boolean stopped = false;
    // 通知栏消息
    private static final int NOTE_ID = 1;
    private static final String ACT_SHOW = "ShowJoyStick";
    private static final String ACT_HIDE = "HideJoyStick";
    private static final String CH_ID = "SERVICE_GO_NOTE";
    private static final String CH_NM = "SERVICE_GO_NOTE";
    private NotifRcv rcv;
    // 摇杆相关
    private JoyStk jsk;

    private final LocSvcBinder bnd = new LocSvcBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return bnd;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        try {
            rmNet();
            addNet();
            rmGps();
            addGps();
        } catch (Exception e) {
            XLog.e("LocSvc: mock provider setup failed, will retry in background", e);
        }

        initLoc();
        initNotif();
        initJsk();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lng = intent.getDoubleExtra(HomeAct.K_LNG, DEF_LNG);
        lat = intent.getDoubleExtra(HomeAct.K_LAT, DEF_LAT);
        alt = intent.getDoubleExtra(HomeAct.K_ALT, DEF_ALT);

        jsk.setPos(lng, lat, alt);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopped = true;
        hd.removeMessages(MSG_ID);
        ht.quit();

        jsk.destroy();

        rmNet();
        rmGps();

        unregisterReceiver(rcv);
        stopForeground(STOP_FOREGROUND_REMOVE);

        super.onDestroy();
    }

    private void initNotif() {
        rcv = new NotifRcv();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACT_SHOW);
        filter.addAction(ACT_HIDE);
        registerReceiver(rcv, filter);

        NotificationChannel ch = new NotificationChannel(CH_ID, CH_NM, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (nm != null) {
            nm.createNotificationChannel(ch);
        }

        //准备intent
        Intent ci = new Intent(this, HomeAct.class);
        PendingIntent cpi = PendingIntent.getActivity(this, 1, ci, PendingIntent.FLAG_IMMUTABLE);
        Intent si = new Intent(ACT_SHOW);
        PendingIntent spi = PendingIntent.getBroadcast(this, 0, si, PendingIntent.FLAG_IMMUTABLE);
        Intent hi = new Intent(ACT_HIDE);
        PendingIntent hpi = PendingIntent.getBroadcast(this, 0, hi, PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(this, CH_ID)
                .setChannelId(CH_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_service_tips))
                .setContentIntent(cpi)
                .addAction(new NotificationCompat.Action(null, getResources().getString(R.string.note_show), spi))
                .addAction(new NotificationCompat.Action(null, getResources().getString(R.string.note_hide), hpi))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(NOTE_ID, notif);
    }

    private void initJsk() {
        jsk = new JoyStk(this);
        jsk.setListener(new JoyStk.JoyStkClickLsn() {
            @Override
            public void onMoveInfo(double speed, double disLng, double disLat, double angle) {
                spd = speed;
                // 根据当前的经纬度和距离，计算下一个经纬度
                // Latitude: 1 deg = 110.574 km // 纬度的每度的距离大约为 110.574km
                // Longitude: 1 deg = 111.320*cos(latitude) km  // 经度的每度的距离从0km到111km不等
                // 具体见：http://wp.mlab.tw/?p=2200
                lng += disLng / (111.320 * Math.cos(Math.abs(lat) * Math.PI / 180));
                lat += disLat / 110.574;
                bea = (float) angle;
            }

            @Override
            public void onPositionInfo(double lngVal, double latVal, double altVal) {
                lng = lngVal;
                lat = latVal;
                alt = altVal;
            }
        });
        jsk.show();
    }

    private void initLoc() {
        // 创建 HandlerThread 实例，第一个参数是线程的名字
        ht = new HandlerThread(THR_NAME, Process.THREAD_PRIORITY_FOREGROUND);
        // 启动 HandlerThread 线程
        ht.start();
        // Handler 对象与 HandlerThread 的 Looper 对象的绑定
        hd = new Handler(ht.getLooper()) {
            // 这里的Handler对象可以看作是绑定在HandlerThread子线程中，所以handlerMessage里的操作是在子线程中运行的
            @Override
            public void handleMessage(@NonNull Message msg) {
                try {
                    Thread.sleep(100);

                    if (!stopped) {
                        setNet();
                        setGps();

                        sendEmptyMessage(MSG_ID);
                    }
                } catch (InterruptedException e) {
                    XLog.e("LocSvc: ERROR - handleMessage");
                    Thread.currentThread().interrupt();
                }
            }
        };

        hd.sendEmptyMessage(MSG_ID);
    }

    private void rmGps() {
        try {
            lm.removeTestProvider(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("wrongconstant")
    private void addGps() {
        try {
            rmGps();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                lm.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
            } else {
                lm.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            }
            lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        } catch (Exception e) {
            XLog.e("LocSvc: ERROR - addGps", e);
        }
    }

    private void setGps() {
        try {
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            loc.setAccuracy(Criteria.ACCURACY_FINE);
            loc.setAltitude(alt);
            loc.setBearing(bea);
            loc.setLatitude(lat);
            loc.setLongitude(lng);
            loc.setTime(System.currentTimeMillis());
            loc.setSpeed((float) spd);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            Bundle bundle = new Bundle();
            bundle.putInt("satellites", 7);
            loc.setExtras(bundle);
            lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
        } catch (Exception e) {
            XLog.e("LocSvc: ERROR - setGps", e);
        }
    }

    private void rmNet() {
        try {
            lm.removeTestProvider(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("wrongconstant")
    private void addNet() {
        try {
            rmNet();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                lm.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_COARSE);
            } else {
                lm.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE);
            }
            lm.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        } catch (Exception e) {
            XLog.e("LocSvc: ERROR - addNet", e);
        }
    }

    private void setNet() {
        try {
            Location loc = new Location(LocationManager.NETWORK_PROVIDER);
            loc.setAccuracy(Criteria.ACCURACY_COARSE);
            loc.setAltitude(alt);
            loc.setBearing(bea);
            loc.setLatitude(lat);
            loc.setLongitude(lng);
            loc.setTime(System.currentTimeMillis());
            loc.setSpeed((float) spd);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            lm.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc);
        } catch (Exception e) {
            XLog.e("LocSvc: ERROR - setNet", e);
        }
    }

    public class NotifRcv extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACT_SHOW)) {
                    jsk.show();
                }

                if (action.equals(ACT_HIDE)) {
                    jsk.hide();
                }
            }
        }
    }

    public class LocSvcBinder extends Binder {
        public void setPosition(double lngVal, double latVal, double altVal) {
            hd.removeMessages(MSG_ID);
            lng = lngVal;
            lat = latVal;
            alt = altVal;
            hd.sendEmptyMessage(MSG_ID);
            jsk.setPos(lngVal, latVal, altVal);
        }
    }
}



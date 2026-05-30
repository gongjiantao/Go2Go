package com.gongjiantao.mode.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.location.provider.ProviderProperties;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SysUtil {
    public static boolean isDevOn(Context ctx) {
        return Settings.Global.getInt(
                ctx.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
        ) == 1;
    }

    public static boolean isWifi(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = cm.getActiveNetwork();
        if (nw == null) {
            return false;
        }
        NetworkCapabilities nwCap = cm.getNetworkCapabilities(nw);
        return nwCap != null && (nwCap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
    }

    public static boolean isWifiOn(Context ctx) {
        WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        return wm.isWifiEnabled();
    }

    public static boolean isMobNet(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = cm.getActiveNetwork();
        if (nw == null) {
            return false;
        }
        NetworkCapabilities nwCap = cm.getNetworkCapabilities(nw);
        return nwCap != null && (nwCap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    public static boolean isNetConn(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = cm.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities nwCap = cm.getNetworkCapabilities(nw);
        return nwCap != null && (nwCap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nwCap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nwCap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || nwCap.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    public static boolean isNetOk(Context ctx) {
        return ((isWifi(ctx) || isMobNet(ctx)) && isNetConn(ctx));
    }

    @SuppressLint("ObsoleteSdkInt")
    public static boolean isGpsOn(Context ctx) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return lm.isLocationEnabled();
        }
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("wrongconstant")
    public static boolean isMockOk(Context ctx) {
        boolean ok = false;
        int idx;

        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

            List<String> list = lm.getAllProviders();
            for (idx = 0; idx < list.size(); idx++) {
                if (list.get(idx).equals(LocationManager.GPS_PROVIDER)) {
                    break;
                }
            }

            if (idx < list.size()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    lm.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                            false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
                } else {
                    lm.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                            false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
                }
                ok = true;
            }

            if (ok) {
                lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                lm.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return ok;
    }

    public static synchronized String getVer(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();

            PackageInfo pi = pm.getPackageInfo(
                    ctx.getPackageName(), 0);

            return pi.versionName;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getAppName(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
            ApplicationInfo appInfo = pi.applicationInfo;
            int labelRes = appInfo.labelRes;
            return ctx.getResources().getString(labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String tsToDate(String seconds) {
        if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        return sdf.format(new Date(Long.parseLong(seconds + "000")));
    }

    public static void showMockDlg(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("启用位置模拟")
                .setMessage("请在\"开发者选项→选择模拟位置信息应用\"中进行设置")
                .setPositiveButton("设置", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> {
                })
                .show();
    }

    public static void showFloatDlg(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("启用悬浮窗")
                .setMessage("为了模拟定位的稳定性，建议开启\"显示悬浮窗\"选项")
                .setPositiveButton("设置", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> {

                })
                .show();
    }

    public static void showGpsDlg(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("启用定位服务")
                .setMessage("是否开启 GPS 定位服务?")
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        ctx.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> {

                })
                .show();
    }

    public static void showWifiDlg(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("警告")
                .setMessage("开启 WIFI 后（即使没有连接热点）将导致定位闪回真实位置。建议关闭 WIFI，使用移动流量进行游戏！")
                .setPositiveButton("去关闭", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        ctx.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("忽略", (dialog, which) -> {
                })
                .show();
    }

    public static void toast(Context ctx, String str) {
        Toast t = Toast.makeText(ctx, str, Toast.LENGTH_LONG);
        t.show();
    }

    public static class TimerX extends CountDownTimer {
        private TimerXLsn mLsn;

        public TimerX(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            mLsn.onFinish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mLsn.onTick(millisUntilFinished);
        }

        public void setListener(TimerXLsn l) {
            this.mLsn = l;
        }

        public interface TimerXLsn {
            void onTick(long millisUntilFinished);
            void onFinish();
        }
    }
}

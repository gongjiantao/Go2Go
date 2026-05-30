package com.gongjiantao.mode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.gongjiantao.mode.utils.SysUtil;

import java.util.ArrayList;

public class StartAct extends AppCompatActivity {
    private static SharedPreferences sp;
    private static final String KEY_AGREE = "KEY_AGREE";

    private static boolean permOk = false;
    private static final int PERM_REQ = 127;
    private static final ArrayList<String> reqPerms = new ArrayList<>();

    private CheckBox cb;
    private Boolean agreeOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        Button startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> goHome());

        chkAgreement();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERM_REQ) {
            for (int i = 0; i < reqPerms.size(); i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    SysUtil.toast(this, getResources().getString(R.string.app_error_permission));
                    return;
                }
            }
            permOk = true;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void chkPerms() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            reqPerms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            reqPerms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            reqPerms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            reqPerms.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (reqPerms.isEmpty()) {
            permOk = true;
        } else {
            requestPermissions(reqPerms.toArray(new String[0]), PERM_REQ);
        }
    }

    private void goHome() {
        if (!cb.isChecked()) {
            SysUtil.toast(this, getResources().getString(R.string.app_error_agreement));
            return;
        }

        if (!SysUtil.isNetOk(this)) {
            SysUtil.toast(this, getResources().getString(R.string.app_error_network));
            return;
        }

        if (!SysUtil.isGpsOn(this)) {
            SysUtil.toast(this, getResources().getString(R.string.app_error_gps));
            return;
        }

        if (permOk) {
            Intent intent = new Intent(StartAct.this, HomeAct.class);
            startActivity(intent);
            StartAct.this.finish();
        } else {
            chkPerms();
        }
    }

    private void doAccept() {
        cb.setChecked(agreeOk);
        if (agreeOk) {
            chkPerms();
        }
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(KEY_AGREE, agreeOk);
        ed.apply();
    }

    private void showPolicyDlg() {
        final AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.show();
        dlg.setCancelable(false);
        Window window = dlg.getWindow();
        if (window != null) {
            window.setContentView(R.layout.dialog_agreement);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView title = window.findViewById(R.id.tv_title);
            TextView content = window.findViewById(R.id.tv_content);
            TextView btnCancel = window.findViewById(R.id.btn_cancel);
            TextView btnAgree = window.findViewById(R.id.btn_agree);

            title.setText(R.string.app_agreement);
            content.setText(R.string.app_agreement_content);

            btnCancel.setOnClickListener(v -> dlg.cancel());

            btnAgree.setOnClickListener(v -> {
                agreeOk = true;
                doAccept();
                dlg.cancel();
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void chkAgreement() {
        sp = getSharedPreferences(KEY_AGREE, MODE_PRIVATE);
        agreeOk = sp.getBoolean(KEY_AGREE, false);

        cb = findViewById(R.id.check_agreement);
        cb.setOnTouchListener((v, event) -> {
            if (v instanceof TextView) {
                TextView text = (TextView) v;
                MovementMethod method = text.getMovementMethod();
                if (method != null && text.getText() instanceof Spannable
                        && event.getAction() == MotionEvent.ACTION_UP) {
                    if (method.onTouchEvent(text, (Spannable) text.getText(), event)) {
                        event.setAction(MotionEvent.ACTION_CANCEL);
                    }
                }
            }
            return false;
        });
        cb.setOnCheckedChangeListener((BtnView, isChecked) -> {
            if (isChecked) {
                if (!agreeOk) {
                    SysUtil.toast(this, getResources().getString(R.string.app_error_read));
                    cb.setChecked(false);
                }
            } else {
                agreeOk = false;
            }
        });

        String str = getString(R.string.app_agreement_privacy);
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        ClickableSpan span = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPolicyDlg();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(ContextCompat.getColor(StartAct.this, R.color.colorPrimary));
                ds.setUnderlineText(false);
            }
        };
        int start = str.indexOf("《");
        int end = str.indexOf("》") + 1;
        builder.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        cb.setText(builder);
        cb.setMovementMethod(LinkMovementMethod.getInstance());

        if (agreeOk) {
            cb.setChecked(true);
            chkPerms();
        } else {
            cb.setChecked(false);
        }
    }
}

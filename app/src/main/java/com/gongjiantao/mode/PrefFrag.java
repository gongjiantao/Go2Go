package com.gongjiantao.mode;

import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.baidu.mapapi.SDKInitializer;
import com.elvishew.xlog.XLog;
import com.gongjiantao.mode.utils.SysUtil;

import java.util.Objects;

public class PrefFrag extends PreferenceFragmentCompat {

    // Set a non-empty decimal EditTextPreference
    private void setupDecPref(EditTextPreference preference) {
        if (preference != null) {
            preference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) EditTextPreference::getText);
            preference.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                editText.setSelection(editText.length());
            });
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue.toString().trim().isEmpty()) {
                    SysUtil.toast(this.getContext(), getResources().getString(R.string.app_error_input_null));
                    return false;
                }
                return true;
            });
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);

        ListPreference pfJsk = findPreference("setting_joystick_type");
        if (pfJsk != null) {
            // 使用自定义 SummaryProvider
            pfJsk.setSummaryProvider((Preference.SummaryProvider<ListPreference>) preference -> Objects.requireNonNull(preference.getEntry()));
            pfJsk.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().isEmpty());
        }

        EditTextPreference pfWk = findPreference("setting_walk");
        setupDecPref(pfWk);

        EditTextPreference pfRn = findPreference("setting_run");
        setupDecPref(pfRn);

        EditTextPreference pfBk = findPreference("setting_bike");
        setupDecPref(pfBk);

        EditTextPreference pfAlt = findPreference("setting_altitude");
        setupDecPref(pfAlt);

        EditTextPreference pfLOff = findPreference("setting_lat_max_offset");
        setupDecPref(pfLOff);

        EditTextPreference pfLngOff = findPreference("setting_lon_max_offset");
        setupDecPref(pfLngOff);

        SwitchPreferenceCompat pLog = findPreference("setting_log_off");
        if (pLog != null) {
            pLog.setOnPreferenceChangeListener((preference, newValue) -> {
                if(((SwitchPreferenceCompat) preference).isChecked() != (Boolean) newValue) {
                    XLog.d(preference.getKey() + newValue);

                    if (Boolean.parseBoolean(newValue.toString())) {
                        XLog.d("on");
                    } else {
                        XLog.d("off");
                    }
                    return true;
                } else {
                    return false;
                }
            });
        }

        EditTextPreference pfHExp = findPreference("setting_history_expiration");
        setupDecPref(pfHExp);

        // 设置版本号
        String ver;
        ver = SysUtil.getVer(PrefFrag.this.getContext());
        Preference pfVer = findPreference("setting_version");
        if (pfVer != null) {
            pfVer.setSummary(ver);
        }
    }
}
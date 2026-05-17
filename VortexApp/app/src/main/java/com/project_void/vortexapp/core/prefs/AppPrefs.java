package com.project_void.vortexapp.core.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.project_void.vortexapp.BuildConfig;

public class AppPrefs {
    private static final String TAG = "core/prefs/AppPrefs";
    private static final String PREF = "app_prefs";
    private static final String KEY_HAPTICS = "haptics_enabled";
    private static final String KEY_THEME = "theme_mode";
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_LIGHT = 2;

    private static SharedPreferences sp(Context ctc){
        if (BuildConfig.DEBUG) Log.d(TAG, "sp: " + ctc);
        return ctc.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean isHapticsEnabled(Context ctc){
        boolean h = sp(ctc).getBoolean(KEY_HAPTICS, true);
        if (BuildConfig.DEBUG) Log.d(TAG, "isHapticsEnabled: " + ctc + " enabled=" + h);
        return h;
    }

    public static void setHapticsEnabled(Context ctc, boolean enabled){
        if (BuildConfig.DEBUG) Log.d(TAG, "setHapticsEnabled: " + ctc + " enabled=" + enabled);
        sp(ctc).edit().putBoolean(KEY_HAPTICS, enabled).apply();
    }

    public static int getThemeMode(Context ctc){
        if (BuildConfig.DEBUG) Log.d(TAG, "getThemeMode: " + ctc);
        return sp(ctc).getInt(KEY_THEME, THEME_SYSTEM);
    }

    public static void setThemeMode(Context ctc, int mode){
        if (BuildConfig.DEBUG) Log.d(TAG, "setThemeMode: " + ctc + " mode=" + mode);
        sp(ctc).edit().putInt(KEY_THEME, mode).apply();
    }
}

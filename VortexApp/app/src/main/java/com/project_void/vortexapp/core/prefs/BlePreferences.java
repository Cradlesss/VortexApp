package com.project_void.vortexapp.core.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.project_void.vortexapp.BuildConfig;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BlePreferences {
    private final static String TAG = "ble/prefs";
    private static final String PREF = "ble_prefs";
    private static final String KEY_LAST_ADDR = "last_device_address";
    private static final String PREFIX_ALIAS = "alias_";
    private static final String PREFIX_IC_CODE = "ic_code_";
    private static final String PREFIX_AUTO_CONNECT = "auto_connect_";
    private static final String PREFIX_MODEL_ID = "model_id_";
    private static final String PREFIX_VER_MAJ = "ver_maj_";
    private static final String PREFIX_VER_MIN = "ver_min_";
    private static final String PREFIX_VER_PAT = "ver_pat_";
    private static BlePreferences I;
    private final SharedPreferences p;

    private BlePreferences(Context c) {
        p = c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static synchronized BlePreferences get(Context c) {
        if (I == null)
            I = new BlePreferences(c);
        return I;
    }

    public void setLastAddress(String addr) {
        p.edit().putString(KEY_LAST_ADDR, addr).apply();
    }

    public String getLastAddress() {
        return p.getString(KEY_LAST_ADDR, null);
    }

    public void setAlias(String addr, String alias) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setAlias: addr=" + addr + " alias=" + alias);
        p.edit().putString(PREFIX_ALIAS + addr, alias).apply();
    }

    public String getAlias(String addr) {
        if (BuildConfig.DEBUG) Log.d(TAG, "getAlias: addr=" + addr + " alias=" + p.getString(PREFIX_ALIAS + addr, null));
        return p.getString(PREFIX_ALIAS + addr, null);
    }

    public void setIcCode(String addr, int code) {
        p.edit().putInt(PREFIX_IC_CODE + addr, code).apply();
    }

    public int getIcCode(String addr, int def) {
        return p.getInt(PREFIX_IC_CODE + addr, def);
    }

    public void setAutoConnect(String addr, boolean enabled){
        p.edit().putBoolean(PREFIX_AUTO_CONNECT + addr, enabled).apply();
    }

    public boolean getAutoConnect(String addr){
        return p.getBoolean(PREFIX_AUTO_CONNECT + addr, false);
    }

    public Set<String> getAllAddresses() {
        Map<String, ?> all = p.getAll();
        HashSet<String> addrs = new HashSet<>();

        for (String k : all.keySet()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "key: " + k + " val: " + all.get(k));
            if (k.startsWith(PREFIX_ALIAS)) addrs.add(k.substring(PREFIX_ALIAS.length()));
            else if (k.startsWith(PREFIX_IC_CODE)) addrs.add(k.substring(PREFIX_IC_CODE.length()));
        }

        String last = getLastAddress();
        if (last != null) addrs.add(last);
        return addrs;
    }

    public static void removeDevice(BlePreferences self, String addr) {
        if (self == null || addr == null || addr.isEmpty()) return;

        final SharedPreferences.Editor e = self.p.edit();
        e.remove(PREFIX_ALIAS + addr);
        e.remove(PREFIX_IC_CODE + addr);
        e.remove(PREFIX_AUTO_CONNECT + addr);
        e.remove(PREFIX_MODEL_ID + addr);
        e.remove(PREFIX_VER_MAJ + addr);
        e.remove(PREFIX_VER_MIN + addr);
        e.remove(PREFIX_VER_PAT + addr);

        final String last = self.getLastAddress();
        if (Objects.equals(addr, last)) e.remove(KEY_LAST_ADDR);

        e.apply();
    }

    public static void removeAllDevices(BlePreferences self) {
        if (self == null) return;
        if (BuildConfig.DEBUG) Log.d(TAG, "removeAllDevices: removing all devices");
        self.p.edit().clear().apply();
    }

    public void setDeviceVersion(String addr, int maj, int min, int pat){
        p.edit()
                .putInt(PREFIX_VER_MAJ + addr, maj)
                .putInt(PREFIX_VER_MIN + addr, min)
                .putInt(PREFIX_VER_PAT + addr, pat)
                .apply();
    }

    public int[] getDeviceVersion(String addr) {
        return new int[]{
                p.getInt(PREFIX_VER_MAJ + addr, -1),
                p.getInt(PREFIX_VER_MIN + addr, -1),
                p.getInt(PREFIX_VER_PAT + addr, -1)
        };
    }

    public void setModelId(String addr, int id){
        p.edit().putInt(PREFIX_MODEL_ID + addr, id).apply();
    }

    public int getModelId(String addr){
        return p.getInt(PREFIX_MODEL_ID + addr, -1);
    }
}

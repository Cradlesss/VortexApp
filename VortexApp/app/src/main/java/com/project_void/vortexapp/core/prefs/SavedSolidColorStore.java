package com.project_void.vortexapp.core.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class SavedSolidColorStore {
    private static final String PREF = "saved_colors";
    private static final String KEY_SLOT_PREFIX = "slot_";
    private static final String KEY_LAST_HEX = "last_hex";
    private final SharedPreferences p;

    public SavedSolidColorStore(Context context) {
        p = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void set(int slot, @ColorInt int color) {
        p.edit().putInt(KEY_SLOT_PREFIX + slot, color).apply();
    }

    @Nullable
    public Integer get(int slot) {
        String k = KEY_SLOT_PREFIX + slot;
        return p.contains(k) ? p.getInt(k, 0) : null;
    }

    public Map<Integer, Integer> getAll() {
        Map<Integer, Integer> out = new HashMap<>();

        for (int slot = 1; slot <= 8; slot++) {
            Integer color = get(slot);
            if (color != null) out.put(slot, color);
        }

        return out;
    }

    public void setLastHex(String hex) {
        p.edit().putString(KEY_LAST_HEX, hex).apply();
    }

    @Nullable
    public String getLastHex() {
        return p.contains(KEY_LAST_HEX) ? p.getString(KEY_LAST_HEX, null) : null;
    }
}

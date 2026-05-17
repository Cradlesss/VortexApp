package com.project_void.vortexapp.core.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class CoachPrefs {
    private static final String KEY = "coach_prefs";
    private static final String KEY_PREFIX = "seen_";

    public static boolean isTourSeen(Context ctx, String screenKey) {
        SharedPreferences sp = ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_PREFIX + screenKey, false);
    }

    public static void setTourSeen(Context ctx, String screenKey, boolean seen) {
        ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PREFIX + screenKey, seen).apply();
    }

    public static void resetAll(Context ctx) {
        ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit().clear().apply();
    }
}

package com.project_void.vortexapp.core.ui;

import android.content.Context;
import android.util.TypedValue;

public class ThemeUtils {
    public static int resolveColor(Context ctx, int attr){
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
    private ThemeUtils(){}
}

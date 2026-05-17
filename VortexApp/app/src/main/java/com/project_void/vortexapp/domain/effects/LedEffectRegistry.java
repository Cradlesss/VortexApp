package com.project_void.vortexapp.domain.effects;

import android.content.Context;
import com.project_void.vortexapp.R;
import java.util.ArrayList;
import java.util.List;

public final class LedEffectRegistry {
    public static final int EFFECT_OFF = 0;
    public static final int EFFECT_TWINKLE = 1;
    public static final int EFFECT_JINX = 2;
    public static final int EFFECT_TRANSITION = 3;
    public static final int EFFECT_BLUE_B = 4;
    public static final int EFFECT_RAINBOW = 5;
    public static final int EFFECT_BEAT_R = 6;
    public static final int EFFECT_RUN_RED = 7;
    public static final int EFFECT_RAW_NOISE = 8;
    public static final int EFFECT_MOVING_RAINBOW = 9;
    public static final int EFFECT_WAVE = 10;
    public static final int EFFECT_BLIGHT_B = 11;
    public static final int EFFECT_STATIC_COLOR = 13;

    private LedEffectRegistry() {}

    public static String label(Context ctx, int mode) {
        return switch (mode) {
            case EFFECT_OFF -> ctx.getString(R.string.effect_off);
            case EFFECT_TWINKLE -> ctx.getString(R.string.effect_twinkle);
            case EFFECT_JINX -> ctx.getString(R.string.effect_jinx);
            case EFFECT_TRANSITION -> ctx.getString(R.string.effect_transition);
            case EFFECT_BLUE_B -> ctx.getString(R.string.effect_blue_b);
            case EFFECT_RAINBOW -> ctx.getString(R.string.effect_rainbow);
            case EFFECT_BEAT_R -> ctx.getString(R.string.effect_beat_r);
            case EFFECT_RUN_RED -> ctx.getString(R.string.effect_run_red);
            case EFFECT_RAW_NOISE -> ctx.getString(R.string.effect_raw_noise);
            case EFFECT_MOVING_RAINBOW -> ctx.getString(R.string.effect_moving_rainbow);
            case EFFECT_WAVE -> ctx.getString(R.string.effect_wave);
            case EFFECT_BLIGHT_B -> ctx.getString(R.string.effect_blight_b);
            case EFFECT_STATIC_COLOR -> ctx.getString(R.string.effect_static_color);
            default -> ctx.getString(R.string.effect_unknown_format, mode);
        };
    }

    public static List<Effect> list(Context ctx) {
        int[] ids = {
            EFFECT_OFF, EFFECT_TWINKLE, EFFECT_JINX, EFFECT_TRANSITION,
            EFFECT_BLUE_B, EFFECT_RAINBOW, EFFECT_BEAT_R, EFFECT_RUN_RED,
            EFFECT_RAW_NOISE, EFFECT_MOVING_RAINBOW, EFFECT_WAVE, EFFECT_BLIGHT_B
        };
        List<Effect> effects = new ArrayList<>(ids.length);
        for (int id : ids) effects.add(new Effect(id, label(ctx, id)));
        return effects;
    }
}

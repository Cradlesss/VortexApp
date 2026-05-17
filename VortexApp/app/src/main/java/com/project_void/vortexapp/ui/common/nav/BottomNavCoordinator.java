package com.project_void.vortexapp.ui.common.nav;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.core.prefs.AppPrefs;

public class BottomNavCoordinator {
    public interface ReselectionHandler {
        void onReselect(@IdRes int itemId);
    }

    private BottomNavCoordinator() {}

    public static void attach(
            @NonNull Activity activity,
            @NonNull BottomNavigationView nav,
            @IdRes int currentItemId,
            @Nullable ReselectionHandler onReselect
    ) {
        if (currentItemId == View.NO_ID)
            for (int i = 0; i < nav.getMenu().size(); i++)
                nav.getMenu().getItem(i).setChecked(false);
        else
            nav.setSelectedItemId(currentItemId);

        Runnable tick = () -> {
            if (!AppPrefs.isHapticsEnabled(activity)) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    Vibrator vib = (Vibrator) activity.getSystemService(Activity.VIBRATOR_SERVICE);
                    if (vib != null)
                        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
                } catch (Throwable e) {
                    Log.e("BottomNavCoordinator", "tick", e);
                }
            } else {
                nav.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        };

        nav.setOnItemSelectedListener(it -> {
            int id = it.getItemId();
            if (id == currentItemId && currentItemId != View.NO_ID) {
                return true;
            }

            Class<?> target;
            if (id == R.id.bottom_navigation_home)
                target = com.project_void.vortexapp.ui.home.HomeScreenActivity.class;
            else if (id == R.id.bottom_navigation_effects)
                target = com.project_void.vortexapp.ui.effects.EffectsActivity.class;
            else if (id == R.id.bottom_navigation_solid_color)
                target = com.project_void.vortexapp.ui.color.SolidColorActivity.class;
            else if (id == R.id.bottom_navigation_settings) {
                target = com.project_void.vortexapp.ui.settings.SettingsActivity.class;
            } else return false;

            tick.run();
            activity.startActivity(new Intent(activity, target));
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        });

        nav.setOnItemReselectedListener(it -> {
            if (onReselect != null) {
                onReselect.onReselect(it.getItemId());
                return;
            }

            if (currentItemId == View.NO_ID){
                int id = it.getItemId();
                Class<?> target;
                if (id == R.id.bottom_navigation_home)
                    target = com.project_void.vortexapp.ui.home.HomeScreenActivity.class;
                else if (id == R.id.bottom_navigation_effects)
                    target = com.project_void.vortexapp.ui.effects.EffectsActivity.class;
                else if (id == R.id.bottom_navigation_solid_color)
                    target = com.project_void.vortexapp.ui.color.SolidColorActivity.class;
                else if (id == R.id.bottom_navigation_settings) {
                    target = com.project_void.vortexapp.ui.settings.SettingsActivity.class;
                } else return;

                tick.run();
                activity.startActivity(new Intent(activity, target));
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    public static void attach(
            @NonNull Activity activity,
            @NonNull BottomNavigationView nav,
            @IdRes int currentItemId
    ) {
        attach(activity, nav, currentItemId, null);
    }

    public static void attach(
            @NonNull Activity activity,
            @NonNull BottomNavigationView nav
    ) {
        attach(activity, nav, View.NO_ID);
    }
}

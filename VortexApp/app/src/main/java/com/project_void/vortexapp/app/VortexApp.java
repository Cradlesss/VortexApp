package com.project_void.vortexapp.app;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.core.di.AppGraph;
import com.project_void.vortexapp.core.prefs.AppPrefs;
import com.project_void.vortexapp.core.prefs.BlePreferences;

public final class VortexApp extends Application implements DefaultLifecycleObserver {
    private BleSessionRepository bleRepo;

    @Override
    public void onCreate() {
        super.onCreate();
        int mode = AppPrefs.getThemeMode(this);

        if (mode == AppPrefs.THEME_DARK)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (mode == AppPrefs.THEME_LIGHT)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        bleRepo = AppGraph.bleRepo(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (bleRepo != null) {
            bleRepo.bind();
            BlePreferences prefs = BlePreferences.get(this);
            String lastAddr = prefs.getLastAddress();
            if (lastAddr != null && prefs.getAutoConnect(lastAddr)){
                BleSessionState state = bleRepo.observeState().getValue();
                if (state == null || state.status() == BleSessionState.Status.DISCONNECTED)
                    bleRepo.connect(lastAddr);
            }
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (bleRepo != null) bleRepo.unbind(this);
    }
}

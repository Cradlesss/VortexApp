package com.project_void.vortexapp.core.di;

import android.app.Application;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;

public final class AppGraph {
    private static BleSessionRepository repo;

    private AppGraph() {}

    public static synchronized BleSessionRepository bleRepo(Application app) {
        if (repo == null) repo = new BleSessionRepository(app);
        return repo;
    }
}

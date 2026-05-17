package com.project_void.vortexapp.core.util;

import android.os.Handler;
import android.os.Looper;

public final class Debouncer {
    private final Handler h = new Handler(Looper.getMainLooper());
    private Runnable pending;

    public void post(int delayMs, Runnable r) {
        if (pending != null) h.removeCallbacks(pending);

        pending = r;
        h.postDelayed(() -> {
            Runnable tmp = pending;
            pending = null;
            if (tmp != null) tmp.run();
        }, delayMs);
    }
}

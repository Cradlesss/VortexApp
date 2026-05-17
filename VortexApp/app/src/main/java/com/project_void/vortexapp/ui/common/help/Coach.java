package com.project_void.vortexapp.ui.common.help;

import android.app.Activity;
import android.view.ViewGroup;
import com.project_void.vortexapp.core.prefs.CoachPrefs;

public final class Coach {
    private Coach() {}

    public static void start(Activity act, ViewGroup root, CoachScript script, String screenKey, boolean respectScreenFlag) {
        if (respectScreenFlag && CoachPrefs.isTourSeen(act, screenKey)) return;

        CoachOverlay overlay = new CoachOverlay(act);
        overlay.start(act, root, script);

        CoachPrefs.setTourSeen(act, screenKey, true);
    }
}

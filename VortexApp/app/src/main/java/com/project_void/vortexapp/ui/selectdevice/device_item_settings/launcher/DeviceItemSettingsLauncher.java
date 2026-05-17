package com.project_void.vortexapp.ui.selectdevice.device_item_settings.launcher;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.project_void.vortexapp.BuildConfig;
import com.project_void.vortexapp.ui.selectdevice.device_item_settings.DeviceItemSettings;

public final class DeviceItemSettingsLauncher {
    private static final String TAG = "ui/selectdevice/device_item_settings/launcher/DeviceItemSettingsLauncher";
    public static final int REQ_SETTINGS = 0x2A17;

    public static void launch(Activity a, String address) {
        Intent i = new Intent(a, DeviceItemSettings.class)
                .putExtra(DeviceItemSettings.EXTRA_ADDRESS, address);
        if (BuildConfig.DEBUG) Log.d(TAG, "launch: " + address);
        a.startActivityForResult(i, REQ_SETTINGS);
    }

    public static boolean didChange(Intent d) {
        return (d != null && d.getBooleanExtra(DeviceItemSettings.EXTRA_RESULT_CHANGED, false));
    }
}

package com.project_void.vortexapp.ui.selectdevice.ic_resolver;

import android.util.Log;
import androidx.annotation.Nullable;
import com.project_void.vortexapp.BuildConfig;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ui.selectdevice.device_info.DeviceSignature;

public class DeviceIconResolver {
    public static int resolve(int caps, @Nullable DeviceSignature sig) {
        final int icCode = (sig != null) ? sig.mfgIconCode : caps;
        if (BuildConfig.DEBUG) Log.d("DeviceIconResolver", "Resolving icon for caps: " + caps + ", sig: " + ((sig != null) ? sig.mfgIconCode : "null"));
        return switch (icCode) {
            case DeviceSignature.TYPE_LED_STRIP -> {
                if (BuildConfig.DEBUG) Log.d("DeviceIconResolver", "assigned LED_STRIP icon for: " + icCode);
                yield R.drawable.ic_led_strip;
            }
            default -> {
                if (BuildConfig.DEBUG) Log.d("DeviceIconResolver", "assigned default icon for: " + ((sig != null) ? sig.mfgIconCode : "null"));
                yield R.drawable.ic_device_default;
            }
        };
    }
}

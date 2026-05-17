package com.project_void.vortexapp.core.permissions;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.Manifest.permission.FOREGROUND_SERVICE;
import static android.Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.*;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
public final class AppPermissions {
    private AppPermissions() {}

    public static String[] requiredForScan() {
        if (SDK_INT >= S) {
            return new String[]{BLUETOOTH_SCAN, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION};
        }
        return new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION};
    }

    public static String[] requiredForConnect() {
        if (SDK_INT >= S) {
            return new String[]{BLUETOOTH_CONNECT};
        }
        return new String[]{};
    }

    public static String[] requiredForNotifications() {
        if (SDK_INT >= TIRAMISU) {
            return new String[]{POST_NOTIFICATIONS};
        }
        return new String[]{};
    }

    @RequiresApi(api = P)
    public static String[] requiredForConnectedDevice() {
        if (SDK_INT >= UPSIDE_DOWN_CAKE) {
            return new String[]{
                    FOREGROUND_SERVICE,
                    FOREGROUND_SERVICE_CONNECTED_DEVICE
            };
        }
        return new String[]{FOREGROUND_SERVICE};
    }

    public static boolean canScan(Context c) {
        return missing(c, requiredForScan()).length == 0;
    }

    public static boolean canConnect(Context c) {
        return missing(c, requiredForConnect()).length == 0;
    }

    public static boolean canPostNotifications(Context c) {
        return missing(c, requiredForNotifications()).length == 0;
    }

    public static String[] missing(Context c, String[] perms) {
        ArrayList<String> out = new ArrayList<>(perms.length);

        for (String p : perms)
            if (ContextCompat.checkSelfPermission(c, p) != PackageManager.PERMISSION_GRANTED)
                out.add(p);

        return out.toArray(new String[0]);
    }
}
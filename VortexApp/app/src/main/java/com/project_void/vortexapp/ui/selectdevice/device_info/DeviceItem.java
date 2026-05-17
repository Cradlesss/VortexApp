package com.project_void.vortexapp.ui.selectdevice.device_info;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.project_void.vortexapp.BuildConfig;
import java.util.Objects;

public final class DeviceItem {
    private final static String TAG = "ui/selectdevice/DeviceItem";
    public final String address;
    public final String displayName;
    public final boolean bonded;
    public final int rssi;
    public final int iconRes;
    public final Context context;
    public boolean compatible;

    public DeviceItem(String address, String displayName, boolean bonded, int rssi, int iconRes, Context context, boolean compatible) {
        this.address = address;
        this.displayName = displayName;
        this.bonded = bonded;
        this.rssi = rssi;
        this.iconRes = iconRes;
        this.context = context;
        this.compatible = compatible;
        if (BuildConfig.DEBUG) Log.d(TAG, "new  DeviceItem created: " + this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceItem)) return false;
        DeviceItem that = (DeviceItem) o;
        return bonded == that.bonded
                && compatible == that.compatible
                && iconRes == that.iconRes
                && Objects.equals(address, that.address)
                && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, displayName, bonded, compatible, iconRes);
    }

    @NonNull
    @Override
    public String toString() {
        return "DeviceItem{ address='" + address + " displayName=" + displayName + " bonded=" + bonded + " rssi=" + rssi + " iconRes=" + iconRes + " compatible=" + compatible + " context=" + context + " }";
    }
}

package com.project_void.vortexapp.core.notify;

import android.app.NotificationManager;
import androidx.annotation.StringRes;
import com.project_void.vortexapp.R;

public enum NotifChannel {
    BLE_SESSION(
            "ble_session",
            R.string.notif_channel_ble_name,
            R.string.notif_channel_ble_desc,
            NotificationManager.IMPORTANCE_LOW
    ),
    GENERAL(
            "general",
            R.string.notif_channel_general_name,
            R.string.notif_channel_general_desc,
            NotificationManager.IMPORTANCE_DEFAULT
    );

    public final String id;
    public final @StringRes int nameRes;
    public final @StringRes int descRes;
    public final int importance;

    NotifChannel(String id, @StringRes int nameRes, @StringRes int descRes, int importance) {
        this.id = id;
        this.nameRes = nameRes;
        this.descRes = descRes;
        this.importance = importance;
    }
}

package com.project_void.vortexapp.core.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.project_void.vortexapp.ble.service.BackgroundBluetoothLeService;
import com.project_void.vortexapp.core.config.AppConstants;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_DISCONNECT = "com.project_void.vortexapp.NOTIF_DISCONNECT";

    @Override
    public void onReceive(Context ctx, Intent i){
        if (!ACTION_DISCONNECT.equals(i.getAction())) return;
        Intent svc = new Intent(ctx, BackgroundBluetoothLeService.class);
        svc.setAction(AppConstants.ACTION_BLE_DISCONNECT);
        ctx.startService(svc);
    }
}

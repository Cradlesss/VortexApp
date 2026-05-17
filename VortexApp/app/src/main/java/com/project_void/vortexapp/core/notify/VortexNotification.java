package com.project_void.vortexapp.core.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.DrawableRes;
import com.project_void.vortexapp.BuildConfig;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ui.home.HomeScreenActivity;
import com.project_void.vortexapp.ui.selectdevice.SelectDeviceActivity;

public final class VortexNotification {
    private final static String TAG = "core/notify/VortexNotification";
    public static final int ID_BLE_SESSION = 1;
    public static final int ID_BLE_DISCONNECT = 2;

    private VortexNotification() {
    }

    public static void ensureChannel(Context ctx, NotifChannel channel) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(channel.id) != null) return;

        NotificationChannel ch = new NotificationChannel(channel.id, ctx.getString(channel.nameRes), channel.importance);
        ch.setDescription(ctx.getString(channel.descRes));
        ch.enableLights(false);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
    }

    public static void ensureAllChannels(Context ctx) {
        for (NotifChannel ch : NotifChannel.values())
            ensureChannel(ctx, ch);
    }

    public static Notification buildBleSession(
            Context ctx,
            CharSequence title,
            CharSequence text,
            boolean removable
    ) {
        if (BuildConfig.DEBUG) Log.d(TAG, "buildBleSession: called");
        Builder b = new Builder(ctx, NotifChannel.BLE_SESSION)
                .title(title)
                .text(text)
                .icon(R.drawable.ic_notification)
                .ongoing(!removable)
                .autoCancel(removable)
                .alertOnce(true)
                .tapIntent(homeTapIntent(ctx));

        if (!removable){
            Intent dsc = new Intent(NotificationActionReceiver.ACTION_DISCONNECT)
                    .setPackage(ctx.getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(
                    ctx, 0, dsc,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            b.action(ctx.getString(R.string.disconnect), pi);
        }
        return b.build();
    }

    public static void postDisconnected(Context ctx, CharSequence deviceName){
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Notification n = new Builder(ctx, NotifChannel.BLE_SESSION)
                .title(ctx.getString(R.string.notif_ble_disconnected))
                .text(deviceName)
                .icon(R.drawable.ic_notification)
                .ongoing(false)
                .autoCancel(true)
                .alertOnce(false)
                .tapIntent(selectDeviceIntent(ctx))
                .build();

        nm.notify(ID_BLE_DISCONNECT, n);
    }

    private static PendingIntent selectDeviceIntent(Context ctx) {
        Intent i = new Intent(ctx, SelectDeviceActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent homeTapIntent(Context ctx) {
        Intent i = new Intent(ctx, HomeScreenActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static final class Builder {
        private final Context ctx;
        private final NotifChannel channel;
        private CharSequence title = "";
        private CharSequence text = "";
        private @DrawableRes int icon = R.drawable.ic_bluetooth;
        private boolean ongoing = false;
        private boolean autoCancel = false;
        private boolean alertOnce = true;
        private @Nullable PendingIntent contentIntent = null;
        private @Nullable PendingIntent actionIntent = null;
        private @Nullable CharSequence actionLabel = null;

        public Builder(Context ctx, NotifChannel channel) {
            this.ctx = ctx;
            this.channel = channel;
        }

        public Builder title(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder text(CharSequence text) {
            this.text = text;
            return this;
        }

        public Builder icon(@DrawableRes int icon) {
            this.icon = icon;
            return this;
        }

        public Builder ongoing(boolean ongoing) {
            this.ongoing = ongoing;
            return this;
        }

        public Builder autoCancel(boolean autoCancel) {
            this.autoCancel = autoCancel;
            return this;
        }

        public Builder alertOnce(boolean alertOnce) {
            this.alertOnce = alertOnce;
            return this;
        }

        public Builder tapIntent(PendingIntent intent) {
            this.contentIntent = intent;
            return this;
        }

        public Builder action(CharSequence label, PendingIntent intent) {
            this.actionLabel = label;
            this.actionIntent = intent;
            return this;
        }

        public Notification build(){
            ensureChannel(ctx, channel);
            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channel.id)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(icon)
                    .setOngoing(ongoing)
                    .setAutoCancel(autoCancel)
                    .setOnlyAlertOnce(alertOnce)
                    .setPriority(channel.importance >= 4 ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_LOW);

            if (contentIntent != null) b.setContentIntent(contentIntent);
            if (actionLabel != null && actionIntent != null) b.addAction(0, actionLabel, actionIntent);

            return b.build();
        }
    }
}

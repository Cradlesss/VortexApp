package com.project_void.vortexapp.ble.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseLongArray;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import com.project_void.vortexapp.BuildConfig;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.protocol.LedControlFrames;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.notify.VortexNotification;
import com.project_void.vortexapp.core.prefs.BlePreferences;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public final class BackgroundBluetoothLeService extends Service {
    private static final String TAG = "Ble/Service";
    private final IBinder binder = new LocalBinder();

    public final class LocalBinder extends Binder {
        public BackgroundBluetoothLeService getService() {
            return BackgroundBluetoothLeService.this;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<byte[]> outQ = new ArrayDeque<>();
    private static final long PING_INTERVAL_MS = 5_000;
    private static final long PING_TIMEOUT_MS = 8_000;
    private int lastPingToken = -1;
    private boolean writing = false;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic controlPointChar;
    private BluetoothGattCharacteristic stateChar;
    @Nullable
    private String currentAddress;
    @Nullable
    private String currentDisplayName;
    private boolean notificationEnabled = false;
    private boolean ctrlNotificationsEnabled = false;
    private boolean keepaliveRunning = false;
    private boolean isGattConnected = false;
    private final Set<Messenger> clients = new CopyOnWriteArraySet<>();
    private final SparseLongArray pingT0 = new SparseLongArray();
    private final Runnable keepalivePingRunnable = new Runnable() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void run() {
            if (gatt == null || controlPointChar == null) return;

            if (BuildConfig.DEBUG) Log.d(TAG, "Keep alive check");
            lastPingToken = (int) (SystemClock.elapsedRealtime() & 0x7F);
            pingT0.put(lastPingToken, SystemClock.elapsedRealtime());
            sendFrame(LedControlFrames.ping(lastPingToken));
            handler.postDelayed(keepaliveTimeoutRunnable, PING_TIMEOUT_MS);
        }
    };
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private final Runnable keepaliveTimeoutRunnable = () -> {
        Log.w(TAG, "Keepalive pong timeout — device likely reset, forcing disconnect");
        if (gatt != null) gatt.disconnect();
    };
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private final Runnable connectTimeoutRunnable = () -> {
        handler.removeCallbacks(keepalivePingRunnable);
        handler.removeCallbacks(keepaliveTimeoutRunnable);
        keepaliveRunning = false;
        if (gatt != null) {
            Log.w(TAG, "Connection timeout - force disconnect");
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        controlPointChar = null;
        writing = false;
        outQ.clear();
        stopForegroundCompat();
        emitDisconnected();
        stopSelf();
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(String address) {
        handler.removeCallbacks(connectTimeoutRunnable);
        BlePreferences prefs = BlePreferences.get(this);
        boolean useAutoConnect = prefs.getAutoConnect(address);
        if (!useAutoConnect)
            handler.postDelayed(connectTimeoutRunnable, 10_000);

        if (BuildConfig.DEBUG) Log.d(TAG, "Connecting to " + address);
        currentAddress = address;
        String alias = prefs.getAlias(address);
        currentDisplayName = (alias != null && !alias.isEmpty()) ? alias : address;
        BluetoothManager mgr = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = (mgr != null) ? mgr.getAdapter() : null;

        if (adapter == null) return;

        BluetoothDevice dev = adapter.getRemoteDevice(address);

        if (dev == null) return;

        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }

        controlPointChar = null;

        gatt = dev.connectGatt(this, useAutoConnect, gattCb, BluetoothDevice.TRANSPORT_LE);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect() {
        if (BuildConfig.DEBUG) Log.d(TAG, "disconnect called");
        handler.removeCallbacks(keepalivePingRunnable);
        handler.removeCallbacks(keepaliveTimeoutRunnable);
        keepaliveRunning = false;
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        stopForegroundCompat();
        emitDisconnected();
        currentAddress = null;
        currentDisplayName = null;
        controlPointChar = null;
        outQ.clear();

        stopSelf();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void setAnimation(int mode) {
        sendFrame(LedControlFrames.setAnimation(mode));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void setBrightness(int bright) {
        sendFrame(LedControlFrames.setBrightness(bright));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void setRgb(int r, int g, int b) {
        sendFrame(LedControlFrames.setRgb(r, g, b));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendFrame(byte[] frame) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Sending frame " + frame.length + " bytes: " + bytesToHex(frame));
        if (frame.length < 2) return;
        outQ.add(frame);
        drain();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void drain() {
        if (BuildConfig.DEBUG) Log.d(TAG, "drain called: writing=" + writing + " controlPointChar=" + controlPointChar + " outQ=" + outQ.size() + " gatt=" + (gatt == null ? "null" : gatt.toString()));
        if (writing) return;
        if (gatt == null || controlPointChar == null) return;

        if (BuildConfig.DEBUG) Log.d(TAG, "draining: " + outQ.size() + " writing=" + writing + " controlPointChar=" + controlPointChar);
        byte[] next = outQ.poll();
        if (next == null) return;

        if (BuildConfig.DEBUG) Log.d(TAG, "writeCharacteristic len=" + next.length + " writeType=" + controlPointChar.getWriteType());
        controlPointChar.setWriteType(Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        controlPointChar.setValue(next);
        writing = true;

        boolean ok = gatt.writeCharacteristic(controlPointChar);

        if (!ok) {
            writing = false;
            Log.e(TAG, "writeCharacteristic failed");
        }
    }

    private final BluetoothGattCallback gattCb = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (BuildConfig.DEBUG) Log.d(TAG, "onConnectionStateChange status=" + status + " newState=" + newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                handler.removeCallbacks(connectTimeoutRunnable);
                refreshGattCache(g);
                g.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                g.close();
                gatt = null;
                controlPointChar = null;
                stateChar = null;
                notificationEnabled = false;
                ctrlNotificationsEnabled = false;
                writing = false;
                keepaliveRunning = false;
                handler.removeCallbacks(keepalivePingRunnable);
                handler.removeCallbacks(keepaliveTimeoutRunnable);
                outQ.clear();
                handler.post(() -> {
                    stopForegroundCompat();
                    emitDisconnected();
                    stopSelf();
                });
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (BuildConfig.DEBUG) Log.d(TAG, "onServicesDiscovered status=" + status);
            if (status != BluetoothGatt.GATT_SUCCESS) return;

            BluetoothGattService svc = g.getService(BleUuids.SERVICE_LED);

            if (svc == null) {
                if (BuildConfig.DEBUG) Log.d(TAG, "LED service not found");
                return;
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "Found LED service " + svc.getUuid());

            controlPointChar = svc.getCharacteristic(BleUuids.CHAR_CONTROL_POINT);

            if (controlPointChar == null) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Control point characteristic not found");
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Found control point characteristic " + controlPointChar.getUuid());
                g.setCharacteristicNotification(controlPointChar, true);
            }

            stateChar = svc.getCharacteristic(BleUuids.CHAR_STATE);

            if (stateChar != null) {
                g.setCharacteristicNotification(stateChar, true);

                BluetoothGattDescriptor cccd = stateChar.getDescriptor(BleUuids.DESC_CCCD);

                if (cccd != null) {
                    byte[] enable = ((stateChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

                    cccd.setValue(enable);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Enabling notifications (cccd)");

                    if (g.writeDescriptor(cccd)) return;
                    Log.e(TAG, "writeDescriptor failed; falling back to READ_REQ");

                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "CCCD descriptor not found on state characteristic");
                }

                notificationEnabled = false;
                emitConnected(currentAddress);
                requestState();
                boolean readStarted = g.readCharacteristic(stateChar);
                if (BuildConfig.DEBUG) Log.d(TAG, "readCharacteristic returned " + readStarted);
                handler.post(BackgroundBluetoothLeService.this::drain);
                return;
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "State characteristic not found");
            }

            emitConnected(currentAddress);
            handler.post(BackgroundBluetoothLeService.this::drain);
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            if (BuildConfig.DEBUG) Log.d(TAG, "onCharacteristicWrite status=" + status);
            if (controlPointChar != null && c.getUuid().equals(controlPointChar.getUuid())) {
                writing = false;
                if (!notificationEnabled && stateChar != null) {
                    handler.postDelayed(() -> {
                        boolean ok = g.readCharacteristic(stateChar);
                        if (BuildConfig.DEBUG) Log.d(TAG, "readCharacteristic returned " + ok);
                    }, 30);
                }
                handler.postDelayed(BackgroundBluetoothLeService.this::drain, 10);
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c, byte[] value) {
            handleCharacteristicChanged(c.getUuid(), value);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            handleCharacteristicChanged(c.getUuid(), c.getValue());
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @Override
        public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, byte[] value, int status) {
            handleCharacteristicRead(c.getUuid(), value, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            handleCharacteristicRead(c.getUuid(), c.getValue(), status);
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (!BleUuids.DESC_CCCD.equals(descriptor.getUuid())) {
                if (BuildConfig.DEBUG) Log.d(TAG, "onDescriptorWrite unknown descriptor " + descriptor.getUuid() + " status: " + status);
                return;
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "onDescriptorWrite status=" + status);

            final BluetoothGattCharacteristic ch = descriptor.getCharacteristic();

            if (ch != null && BleUuids.CHAR_STATE.equals(ch.getUuid())) {
                notificationEnabled = (status == BluetoothGatt.GATT_SUCCESS);

                if (controlPointChar != null && !ctrlNotificationsEnabled) {
                    BluetoothGattDescriptor cccd = controlPointChar.getDescriptor(BleUuids.DESC_CCCD);
                    if (cccd != null) {
                        final int props = controlPointChar.getProperties();
                        final boolean wantsIndicate =
                                (props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
                        cccd.setValue(wantsIndicate
                                ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (BuildConfig.DEBUG) Log.d(TAG, "Enabling " + (wantsIndicate ? "indications" : "notifications")
                                + " (cccd) on control point characteristic");
                        if (gatt.writeDescriptor(cccd)) return;
                        Log.e(TAG, "writeDescriptor for control point failed; proceeding without ctrl notifications");
                    } else {
                        Log.e(TAG, "CCCD descriptor not found on control point characteristic");
                    }
                }
            } else if (ch != null && BleUuids.CHAR_CONTROL_POINT.equals(ch.getUuid())) {
                ctrlNotificationsEnabled = (status == BluetoothGatt.GATT_SUCCESS);
            }

            emitConnected(currentAddress);
            requestState();
            handler.post(BackgroundBluetoothLeService.this::drain);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onStartCommand called: flags=" + flags + " startId=" + startId);
        if (intent != null && AppConstants.ACTION_BLE_CONNECT.equals(intent.getAction())) {
            String text = currentDisplayName != null ? currentDisplayName : currentAddress != null ? currentAddress : "";
            Notification n = VortexNotification.buildBleSession(this, getString(R.string.notif_ble_connecting), text, false);
            startForeground(VortexNotification.ID_BLE_SESSION, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);

            String addr = intent.getStringExtra(AppConstants.EXTRA_BLE_DEVICE_ADDRESS);
            if (addr != null)
                connect(addr);
        } else if (intent != null && AppConstants.ACTION_BLE_DISCONNECT.equals(intent.getAction())) {
            disconnect();
        }
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte x : bytes)
            hexString.append(String.format("%02X", x));
        return hexString.toString().trim();
    }

    public void registerClient(Messenger m) {
        if (m == null) return;
        clients.add(m);
        if (isGattConnected) {
            Bundle b = new Bundle();
            b.putString("address", currentAddress);
            sendTo(m, BleConstants.MSG_CONNECTED, b);
        } else {
            sendTo(m, BleConstants.MSG_DISCONNECT, null);
        }
    }

    public void unregisterClient(Messenger m) {
        if (m != null)
            clients.remove(m);
    }

    private void sendTo(Messenger m, int what, Bundle data) {
        try {
            Message msg = Message.obtain(null, what);
            if (data != null) msg.setData(data);
            m.send(msg);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "sendTo failed", e);
        }
    }

    private void emitConnected(String address) {
        if (BuildConfig.DEBUG) Log.d(TAG, "emitConnected: " + address);
        isGattConnected = true;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(VortexNotification.ID_BLE_DISCONNECT);
        updateConnectedNotification();
        Bundle b = new Bundle();
        b.putString("address", address);
        sendAll(BleConstants.MSG_CONNECTED, b);
        if (!keepaliveRunning) {
            keepaliveRunning = true;
            handler.postDelayed(keepalivePingRunnable, PING_INTERVAL_MS);
        }
    }

    private void emitDisconnected() {
        isGattConnected = false;
        sendAll(BleConstants.MSG_DISCONNECT, null);
        String name = currentDisplayName != null ? currentDisplayName : currentAddress != null ? currentAddress : "";
        VortexNotification.postDisconnected(this, name);
    }

    private void emitState(int mode, int bright, int r, int g, int b) {
        if (BuildConfig.DEBUG) Log.d(TAG, "emitState: " + mode + " " + bright + " " + r + " " + g + " " + b);
        Bundle bu = new Bundle();
        bu.putInt("mode", mode);
        bu.putInt("bright", bright);
        bu.putInt("r", r);
        bu.putInt("g", g);
        bu.putInt("b", b);
        sendAll(BleConstants.MSG_STATE_CHANGED, bu);
    }

    private void emitPong(int token, long rttMillis) {
        if (token < 0x80) return;
        if (BuildConfig.DEBUG) Log.d(TAG, "emitPong: " + token + " " + rttMillis + "ms");
        Bundle bu = new Bundle();
        bu.putInt("token", token);
        bu.putLong("rttMillis", rttMillis);
        sendAll(BleConstants.MSG_PONG, bu);
    }

    private void sendAll(int what, Bundle data) {
        for (Messenger c : clients) {
            try {
                Message msg = Message.obtain(null, what);
                if (data != null) msg.setData(data);
                c.send(msg);
            } catch (Exception e) {
                Log.e(TAG, "sendAll failed", e);
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void requestState() {
        sendFrame(LedControlFrames.getState());
        if (stateChar == null || gatt == null) {
            Log.w(TAG, "requestState: missing gatt/stateChar: " + stateChar + " " + gatt);
            return;
        }

        if (!notificationEnabled) {
            handler.postDelayed(() -> {
                if (gatt == null || stateChar == null) return;
                boolean ok = gatt.readCharacteristic(stateChar);
                if (BuildConfig.DEBUG) Log.d(TAG, "requestState: priming read (notifications not yet enabled). readCharacteristic=" + ok);
            }, 30);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (BuildConfig.DEBUG) Log.d(TAG, "requestState: notifications already enabled; relying on notify/indicate");
            boolean ok = gatt.readCharacteristic(stateChar);
            if (BuildConfig.DEBUG) Log.d(TAG, "requestState: forced read (with notifications). readCharacteristic=" + ok);
        }
    }

    private void updateConnectedNotification() {
        String title = getString(R.string.notif_ble_connected);
        String text = currentDisplayName != null ? currentDisplayName : currentAddress != null ? currentAddress : "";
        Notification n = VortexNotification.buildBleSession(this, title, text, false);
        startForeground(VortexNotification.ID_BLE_SESSION, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void setDeviceName(String name) {
        sendFrame(LedControlFrames.setDeviceName(name));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void ping() {
        int token = 0x80 | (int) (SystemClock.elapsedRealtime() & 0x7F);
        byte[] frame = LedControlFrames.ping(token);
        pingT0.put(token, SystemClock.elapsedRealtime());
        sendFrame(frame);
    }

    private void refreshGattCache(BluetoothGatt g) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return;
        try {
            java.lang.reflect.Method refresh = g.getClass().getMethod("refresh");
            boolean ok = (Boolean) refresh.invoke(g);
            if (BuildConfig.DEBUG) Log.d(TAG, "refreshGattCache: " + ok);
        } catch (Exception e) {
            Log.w(TAG, "refreshGattCache failed: " + e);
        }
    }

    private void handleCharacteristicChanged(UUID uuid, byte[] v) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onCharacteristicChanged uuid=" + uuid);
        if (BleUuids.CHAR_STATE.equals(uuid)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "STATE notify len=" + (v == null ? -1 : v.length));
            if (v != null && v.length >= 5) {
                emitState(v[0] & 0xFF, v[1] & 0xFF, v[2] & 0xFF, v[3] & 0xFF, v[4] & 0xFF);
                if (BuildConfig.DEBUG) Log.d(TAG, "STATE notify: mode=" + (v[0] & 0xFF) + " bright=" + (v[1] & 0xFF) +
                        " rgb=" + (v[2] & 0xFF) + "," + (v[3] & 0xFF) + "," + (v[4] & 0xFF));
            } else {
                Log.w(TAG, "STATE notify payload too short");
            }
            return;
        }
        if (BleUuids.CHAR_CONTROL_POINT.equals(uuid)) {
            if (v == null || v.length < 2) return;
            int op = v[0] & 0xFF;
            int len = v[1] & 0xFF;
            if (op == 0x7F) { // OP_PONG
                int token = (len >= 1 && v.length >= 3) ? (v[2] & 0xFF) : -1;
                long rtt = -1;
                if (token >= 0) {
                    long t = pingT0.get(token, -1);
                    if (t >= 0) {
                        rtt = SystemClock.elapsedRealtime() - t;
                        pingT0.delete(token);
                    }
                }
                handler.removeCallbacks(keepaliveTimeoutRunnable);
                if (keepaliveRunning)
                    handler.postDelayed(keepalivePingRunnable, PING_INTERVAL_MS);
                emitPong(token, rtt);
            }
        }
    }

    private void handleCharacteristicRead(java.util.UUID uuid, byte[] v, int status) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onCharacteristicRead status=" + status);
        if (status == BluetoothGatt.GATT_SUCCESS && BleUuids.CHAR_STATE.equals(uuid)) {
            if (v != null && v.length >= 5) {
                emitState(v[0] & 0xFF, v[1] & 0xFF, v[2] & 0xFF, v[3] & 0xFF, v[4] & 0xFF);
                if (BuildConfig.DEBUG) Log.d(TAG, "STATE read: mode=" + (v[0] & 0xFF) + " bright=" + (v[1] & 0xFF) +
                        " rgb=" + (v[2] & 0xFF) + "," + (v[3] & 0xFF) + "," + (v[4] & 0xFF));
            }
        }
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            stopForeground(STOP_FOREGROUND_REMOVE);
        else
            stopForeground(true);
    }
}

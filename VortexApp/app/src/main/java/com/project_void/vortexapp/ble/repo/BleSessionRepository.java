package com.project_void.vortexapp.ble.repo;

import android.Manifest;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.project_void.vortexapp.BuildConfig;
import com.project_void.vortexapp.ble.service.BackgroundBluetoothLeService;
import com.project_void.vortexapp.ble.service.BleConstants;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.prefs.BlePreferences;
import com.project_void.vortexapp.core.suggestion.SelectionEntry;
import com.project_void.vortexapp.core.suggestion.SelectionHistoryStore;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ble.model.LedState;
import com.project_void.vortexapp.ble.model.PingResult;

public final class BleSessionRepository {
    private static final String TAG = "BLE/Repo";
    private final Application app;
    private final MutableLiveData<BleSessionState> state = new MutableLiveData<>(
            new BleSessionState(BleSessionState.Status.DISCONNECTED, null)
    );
    private BackgroundBluetoothLeService service;
    private ServiceConnection connection;
    private boolean isBound;
    private String pendingConnectionAddress;
    private SelectionHistoryStore historyStore;
    private int bindCount = 0;
    private volatile boolean isConnected = false;
    private final MutableLiveData<LedState> ledState = new MutableLiveData<>(
            new LedState(0, 0, 0, 0, 0)
    );

    private final MutableLiveData<PingResult> pingResult = new MutableLiveData<>();

    private final Messenger clientMessenger;

    public BleSessionRepository(Application app) {
        this.app = app;
        this.historyStore = new SelectionHistoryStore(app);
        Handler msgHandler = new Handler(Looper.getMainLooper(), this::onServiceMessage);
        this.clientMessenger = new Messenger(msgHandler);
    }

    public LiveData<BleSessionState> observeState() {
        return state;
    }

    public LiveData<LedState> observeLedState() {
        return ledState;
    }

    public LiveData<PingResult> observePing() {
        return pingResult;
    }

    public synchronized void bind() {
        if (bindCount++ == 0 && connection == null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "bind called: real bind");

            connection = new ServiceConnection() {

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                @Override
                public void onServiceConnected(ComponentName name, IBinder binder) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "onServiceConnected");
                    service = ((BackgroundBluetoothLeService.LocalBinder) binder).getService();
                    service.registerClient(clientMessenger);
                    isBound = true;
                    service.requestState();

                    if (pendingConnectionAddress != null) {
                        String addr = pendingConnectionAddress;
                        pendingConnectionAddress = null;
                        if (BuildConfig.DEBUG) Log.d(TAG, "Flushing pending connection -> " + addr);
                        service.connect(addr);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "onServiceDisconnected");
                    if (service != null)
                        service.unregisterClient(clientMessenger);

                    service = null;
                    isBound = false;
                    state.postValue(new BleSessionState(BleSessionState.Status.DISCONNECTED, null));
                }
            };

            Intent i = new Intent(app, BackgroundBluetoothLeService.class);
            app.bindService(i, connection, Context.BIND_AUTO_CREATE);
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "bind called: already bound count=" + bindCount);
        }
    }

    public synchronized void unbind(Context c) {
        if (BuildConfig.DEBUG) Log.d(TAG, "unbind called by " + c.toString() + " isBound=" + isBound + " connection=" + connection + " service=" + service);
        if (bindCount == 0) return;
        if (--bindCount > 0) return;

        try {
            if (service != null)
                service.unregisterClient(clientMessenger);
        } catch (Throwable t) {
            Log.e(TAG, "Error unregistering client", t);
        }

        try {
            app.unbindService(connection);
        } catch (Throwable t) {
            Log.e(TAG, "Error unbinding service", t);
        }

        connection = null;
        service = null;
        isBound = false;
    }

    public void connect(String address) {
        if (BuildConfig.DEBUG) Log.d(TAG, "connect called: " + address);
        if (address == null || address.isEmpty()) return;

        BleSessionState current = state.getValue();
        if (current != null && (current.status() == BleSessionState.Status.CONNECTING || current.status() == BleSessionState.Status.CONNECTED)) {
            Log.w(TAG, "connect ignored; already connecting or connected to " + current.deviceAddress());
            return;
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Connecting to " + address);

        state.postValue(new BleSessionState(BleSessionState.Status.CONNECTING, address));

        Intent i = new Intent(app, BackgroundBluetoothLeService.class)
                .setAction(AppConstants.ACTION_BLE_CONNECT)
                .putExtra(AppConstants.EXTRA_BLE_DEVICE_ADDRESS, address);
        ContextCompat.startForegroundService(app, i);
    }

    public void disconnect() {
        if (BuildConfig.DEBUG) Log.d(TAG, "disconnect called");
        pendingConnectionAddress = null;

        Intent i = new Intent(app, BackgroundBluetoothLeService.class)
                .setAction(AppConstants.ACTION_BLE_DISCONNECT);
        app.startService(i);
    }

    public void setAnimation(int mode) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setAnimation called mode= " + mode);
        if (!isConnected("setAnimation")) {
            if (BuildConfig.DEBUG) Log.w(TAG, "setAnimation ignored; not connected or service not bound");
            return;
        }
        historyStore.record(SelectionEntry.effect(mode));
        service.setAnimation(mode);
    }

    public void setBrightness(int value) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setBrightness called brightness= " + value);
        if (!isConnected("setBrightness")) {
            if (BuildConfig.DEBUG) Log.w(TAG, "setBrightness ignored; not connected or service not bound");
            return;
        }
        service.setBrightness(value);
    }

    public void setRgb(int r, int g, int b) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setRgb called rgb= " + r + " " + g + " " + b);
        if (!isConnected("setRgb")) {
            if (BuildConfig.DEBUG) Log.w(TAG, "setRgb ignored; not connected or service not bound");
            return;
        }
        historyStore.record(SelectionEntry.color(r, g, b));
        service.setRgb(r, g, b);
    }

    public void ping() {
        if (isConnected("ping")) service.ping();
        else if (BuildConfig.DEBUG) Log.w(TAG, "ping ignored; service not bound");
    }

    public void setDeviceName(String name) {
        if (!isConnected("setDeviceName")) {
            if (BuildConfig.DEBUG) Log.w(TAG, "setDeviceName ignored; not connected or service not bound");
            return;
        }
        service.setDeviceName(name);
    }

    private boolean onServiceMessage(Message msg) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onServiceMessage: " + msg.what);
        switch (msg.what) {
            case BleConstants.MSG_CONNECTED: {
                isConnected = true;
                String addr = msg.getData() != null ? msg.getData().getString("address") : null;
                if (BuildConfig.DEBUG) Log.d(TAG, "MSG_CONNECTED: " + addr);
                state.postValue(
                        new BleSessionState(BleSessionState.Status.CONNECTED, addr)
                );
                BlePreferences.get(app).setLastAddress(addr);
                return true;
            }
            case BleConstants.MSG_DISCONNECT: {
                isConnected = false;
                if (BuildConfig.DEBUG) Log.d(TAG, "MSG_DISCONNECT");
                state.postValue(
                        new BleSessionState(BleSessionState.Status.DISCONNECTED, null)
                );
                return true;
            }
            case BleConstants.MSG_STATE_CHANGED: {
                Bundle b = msg.getData();
                if (b != null)
                    ledState.postValue(
                            new LedState(
                                    b.getInt("mode"),
                                    b.getInt("bright"),
                                    b.getInt("r"),
                                    b.getInt("g"),
                                    b.getInt("b")
                            )
                    );
                if (BuildConfig.DEBUG) Log.d(TAG, "MSG_STATE_CHANGED: " + b);
                return true;
            }
            case BleConstants.MSG_PONG: {
                Bundle b = msg.getData();
                int token = b != null ? b.getInt("token", -1) : -1;
                long rtt = b != null ? b.getLong("rttMillis", -1) : -1;
                pingResult.postValue(new PingResult(token, rtt));
                if (BuildConfig.DEBUG) Log.d(TAG, "MSG_PONG: token=" + token + " rtt=" + rtt + "ms");
                return true;
            }
        }
        return false;
    }

    public boolean isConnected(String name) {
        if (BuildConfig.DEBUG) Log.d(TAG, "isConnected called: " + name + " state: isConnected=" + isConnected + " service=" + (service == null ? "null" : service.toString()));
        return isConnected && service != null;
    }

    public void requestState() {
        if (BuildConfig.DEBUG) Log.d(TAG, "requestState called");
        if (isConnected("requestState")) service.requestState();
        else if (BuildConfig.DEBUG) Log.w(TAG, "requestState ignored; service not bound");
    }
}

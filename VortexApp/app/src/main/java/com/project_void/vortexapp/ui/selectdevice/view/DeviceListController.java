package com.project_void.vortexapp.ui.selectdevice.view;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import androidx.annotation.Nullable;
import com.project_void.vortexapp.BuildConfig;
import com.project_void.vortexapp.ble.service.BleUuids;
import com.project_void.vortexapp.core.prefs.BlePreferences;
import com.project_void.vortexapp.ui.selectdevice.device_info.DeviceItem;
import com.project_void.vortexapp.ui.selectdevice.device_info.DeviceSignature;
import com.project_void.vortexapp.ui.selectdevice.ic_resolver.DeviceIconResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DeviceListController {
    private final static String TAG = "ui/selectdevice/DeviceListController";

    public interface CallBack {
        void onListUpdated(List<DeviceItem> devices);
    }

    private final BlePreferences prefs;
    private final Handler handler;
    private CallBack callBack;
    private final LinkedHashMap<String, DeviceItem> map = new LinkedHashMap<>();
    private BluetoothLeScanner scanner;
    private boolean scanning;
    private Runnable stopRunnable;
    private final Context appContext;

    public DeviceListController(Context c, BlePreferences p, Handler h) {
        this.appContext = c;
        this.prefs = p;
        this.handler = h;
    }

    public void setCallBack(CallBack c) {
        this.callBack = c;
    }

    @SuppressLint("MissingPermission")
    public void loadInitial(DeviceListAdapter adapter, @Nullable BluetoothAdapter bt) {
        if (BuildConfig.DEBUG) Log.d(TAG, "loadInitial called: " + bt + " " + adapter);
        map.clear();
        HashSet<String> bonded = new HashSet<>();
        if (bt != null) {
            for (BluetoothDevice d : bt.getBondedDevices()) {
                bonded.add(d.getAddress());
            }
        }

        Set<String> saved = prefs.getAllAddresses();

        LinkedHashSet<String> all = new LinkedHashSet<>();
        all.addAll(saved);
        all.addAll(bonded);

        for (String addr : all) {
            int caps = prefs.getIcCode(addr, -1);
            String alias = prefs.getAlias(addr);
            boolean compatible = (caps != -1);
            boolean isBonded = bonded.contains(addr);

            if (!compatible) continue;

            String name = alias != null ? alias : addr;
            if (name.equals(addr) && bt != null && isBonded) {
                try {
                    BluetoothDevice d = bt.getRemoteDevice(addr);
                    if (d != null && d.getName() != null) name = d.getName();
                } catch (Throwable i) {
                    Log.e(TAG, "loadInitial: " + i);
                }
            }

            int icon = DeviceIconResolver.resolve(caps, null);

            DeviceItem it = new DeviceItem(
                    addr,
                    name,
                    isBonded,
                    0,
                    icon,
                    appContext,
                    compatible
            );
            if (BuildConfig.DEBUG) Log.d(TAG, "loadInitial=" + it);
            map.put(addr, it);
        }
        publish();
    }

    @SuppressLint("MissingPermission")
    public void startScan(@Nullable BluetoothAdapter bt, int durationMs, Runnable onStop) {
        stopScan();
        if (bt == null)
            if (onStop != null) {
                onStop.run();
                return;
            }

        scanner = bt.getBluetoothLeScanner();
        if (scanner == null)
            if (onStop != null) {
                onStop.run();
                return;
            }

        try {
            List<ScanFilter> filters = Collections.singletonList(
                    new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BleUuids.SERVICE_LED)).build()
            );

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            if (BuildConfig.DEBUG) Log.d(TAG, "Starting scan for " + durationMs + "ms with filters: " + filters + " and settings: " + settings.toString());

            scanning = true;
            scanner.startScan(filters, settings, cb);
            stopRunnable = () -> {
                stopScan();
                if (onStop != null) onStop.run();
            };
            handler.postDelayed(stopRunnable, durationMs);
        } catch (SecurityException e) {
            if (onStop != null) onStop.run();
            Log.e(TAG, "startScan: SecurityException" + e);
        }
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (!scanning) return;

        scanning = false;

        if (scanner != null) scanner.stopScan(cb);
        if (stopRunnable != null) handler.removeCallbacks(stopRunnable);

        stopRunnable = null;
    }

    private final ScanCallback cb = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult r) {
            if (BuildConfig.DEBUG) Log.d(TAG, "onScanResult: " + r.toString() + " " + r.getDevice().toString());
            handle(r);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult r : results)
                handle(r);
        }
    };

    @SuppressLint("MissingPermission")
    private void handle(ScanResult r) {
        BluetoothDevice d = r.getDevice();
        if (d == null) return;

        String addr = d.getAddress();
        ScanRecord rec = r.getScanRecord();

        boolean isCompatibleNow = isCompatible(rec) || isSavedOrCompatible(addr);
        if (BuildConfig.DEBUG) Log.d(TAG, "Device " + addr + " is compatible now: " + isCompatibleNow + " isisCompatible(rec)=" + isCompatible(rec) + " isSavedOrCompatible(addr)=" + isSavedOrCompatible(addr));

        if (isCompatibleNow && prefs.getIcCode(addr, -1) == -1) {
            prefs.setIcCode(addr, 0x0000);
            if (BuildConfig.DEBUG) Log.d(TAG, "Setting generic icon code for " + addr);
        }

        if (isCompatibleNow && prefs.getAlias(addr) == null) {
            prefs.setAlias(addr, d.getName());
            if (BuildConfig.DEBUG) Log.d(TAG, "Setting alias for " + addr + " to " + d.getName());
        }

        DeviceSignature sig = DeviceSignature.from(rec);
        if (sig != null && sig.mfgIconCode != -1) {
            prefs.setIcCode(addr, sig.mfgIconCode);
            if (BuildConfig.DEBUG) Log.d(TAG, "Saving icon code " + sig.mfgIconCode + " for " + addr);
            prefs.setDeviceVersion(addr, sig.verMajor, sig.verMinor, sig.verPatch);
            prefs.setModelId(addr, sig.modelID);
            if (BuildConfig.DEBUG) Log.d(TAG, "Saving version " + sig.verMajor + "." + sig.verMinor + "." + sig.verPatch + " and model ID " + sig.modelID + " for " + addr);
        }

        if (BuildConfig.DEBUG) {
            if (sig != null) {
                if (sig.verMajor >= 0)
                    Log.d(TAG, "Adv signature: proto=" + sig.proto + ", modelID=" + sig.modelID + ", ver=" + sig.verMajor + "." + sig.verMinor + "." + sig.verPatch + ", mfgCompanyID=" + sig.mfgCompanyID + ", mfgProto=" + sig.mfgProto + " mfgIconCode=" + sig.mfgIconCode + ", src=" + sig.src);
                else
                    Log.d(TAG, "Adv signature: proto=" + sig.proto + ", modelID=" + sig.modelID +
                            ", ver=n/a" + ", mfgCompanyID=" + sig.mfgCompanyID + ", mfgProto=" + sig.mfgProto + ", src=" + sig.src);
            } else {
                Log.d(TAG, "Adv signature: <none>");
            }
        }

        String name = prefs.getAlias(addr);
        if (name == null)
            name = rec != null && rec.getDeviceName() != null ? rec.getDeviceName() : d.getName();
        if (name == null) name = addr;

        DeviceItem prev = map.get(addr);
        int icon = DeviceIconResolver.resolve(prefs.getIcCode(addr, sig != null ? sig.mfgIconCode : 0), sig);

        if (BuildConfig.DEBUG) Log.d(TAG, "Handling device: " + addr + " (" + name + ")" + "  prev=" + (prev == null ? "null" : prev));

        DeviceItem it = new DeviceItem(
                addr,
                name,
                isBonded(d),
                r.getRssi(),
                icon,
                appContext,
                isCompatibleNow
        );

        if (prev == null || !prev.equals(it)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Adding device: " + it);
            map.put(addr, it);
            publish();
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Ignoring device: " + it);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean isBonded(BluetoothDevice d) {
        return d.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    private void publish() {
        if (callBack != null) callBack.onListUpdated(new ArrayList<>(map.values()));
        else return;
        if (BuildConfig.DEBUG) Log.d(TAG, "publish: " + map);
    }

    private boolean isSavedOrCompatible(String addr) {
        return prefs.getIcCode(addr, 0) != 0;
    }

    private boolean isCompatible(@Nullable ScanRecord rec) {
        if (rec == null) return false;

        List<ParcelUuid> uuids = rec.getServiceUuids();
        if (uuids != null)
            for (ParcelUuid u : uuids)
                if (u.getUuid().equals(BleUuids.SERVICE_LED))
                    return true;

        return false;
    }

    public boolean isScanning() {
        return scanning;
    }

    public void clear(boolean publish) {
        map.clear();
        if (publish) publish();
    }
}

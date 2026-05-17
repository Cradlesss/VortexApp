package com.project_void.vortexapp.ui.selectdevice;

import static com.project_void.vortexapp.core.config.AppConstants.COACH_DELAY_MS;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.project_void.vortexapp.BuildConfig;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.di.AppGraph;
import com.project_void.vortexapp.core.prefs.BlePreferences;
import com.project_void.vortexapp.core.prefs.CoachPrefs;
import com.project_void.vortexapp.core.permissions.PermissionsGate;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ui.common.help.Coach;
import com.project_void.vortexapp.ui.common.help.scripts.SelectDeviceScript;
import com.project_void.vortexapp.ui.common.nav.BottomNavCoordinator;
import com.project_void.vortexapp.ui.sandbox.BleSandboxActivity;
import com.project_void.vortexapp.ui.selectdevice.device_info.DeviceItem;
import com.project_void.vortexapp.ui.selectdevice.device_item_settings.launcher.DeviceItemSettingsLauncher;
import com.project_void.vortexapp.ui.selectdevice.view.DeviceListAdapter;
import com.project_void.vortexapp.ui.selectdevice.view.DeviceListController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectDeviceActivity extends AppCompatActivity implements DeviceListController.CallBack {
    private final static String TAG = "ui/selectdevice/SelectDeviceActivity";
    private final static String COACH_KEY = "select_device";
    private BleSessionRepository bleRepo;
    private PermissionsGate gate;
    private DeviceListController controller;
    private DeviceListAdapter adapter;
    private long lastAutoScan = 0;
    private ProgressBar lookingForNewDevices;
    private RecyclerView deviceContainer;
    private BottomNavigationView bottomNav;
    private Button scanBtn;
    private ImageView burgerMenuBtn;
    private FrameLayout backBtnFrame, burgerMenuBtnFrame;
    private PopupMenu optionsMenu;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isInDemoMode = false;

    @Nullable
    private List<DeviceItem> savedDevicesForDemo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        bleRepo = AppGraph.bleRepo(getApplication());
        gate = new PermissionsGate(this, this);

        RecyclerView list = findViewById(R.id.select_device_device_container);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DeviceListAdapter(new DeviceListAdapter.onClick() {
            @Override
            public void onItemClick(DeviceItem item) {
                BleSessionState st = bleRepo.observeState().getValue();
                boolean isThisConnected = st != null && st.deviceAddress() != null && st.deviceAddress().equals(item.address) && st.status() == BleSessionState.Status.CONNECTED;
                if (BuildConfig.DEBUG) Log.d(TAG, "onItemClick:" + item.address + " " + st + " isThisConnected=" + isThisConnected + " item.bonded=" + item.bonded + " item.info=" + item);
                if (isThisConnected)
                    gate.runForConnect(bleRepo::disconnect);
                else if (item.compatible) {
                    controller.stopScan();
                    lookingForNewDevices.setVisibility(View.GONE);
                    gate.runForConnect(() -> bleRepo.connect(item.address));
                }
            }

            @Override
            public void onSettingsClick(DeviceItem item) {
                if (BuildConfig.DEBUG) Log.d(TAG, "onSettingsClick: " + item.address);
                DeviceItemSettingsLauncher.launch(SelectDeviceActivity.this, item.address);
            }
        });

        list.setAdapter(adapter);

        controller = new DeviceListController(this, BlePreferences.get(this), handler);
        controller.setCallBack(this);

        registerUI();
        scanBtn.setOnClickListener(v -> scanBtn.postDelayed(this::startTimedScan, AppConstants.BTN_RIPPLE_DELAY_MS));
        backBtnFrame.setOnClickListener(v -> backBtnFrame.postDelayed(this::finish, AppConstants.BTN_RIPPLE_DELAY_MS));
        burgerMenuBtnFrame.setOnClickListener(v -> burgerMenuBtnFrame.postDelayed(() -> optionsMenu.show(), AppConstants.BTN_RIPPLE_DELAY_MS));

        bleRepo.observeState().observe(this, state -> adapter.setBleState(state));

        controller.loadInitial(adapter, getBluetoothAdapter());
    }

    @Override
    protected void onDestroy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onDestroy called");
        exitDevicesTutorial();
        super.onDestroy();
        controller.stopScan();
    }

    @Override
    protected void onStart() {
        super.onStart();
        BottomNavCoordinator.attach(this, bottomNav);
        bottomNav.getMenu().findItem(R.id.bottom_navigation_home).setChecked(false);
        if (BuildConfig.DEBUG) Log.d(TAG, "List state=" + adapter.getItemCount());

        if (!CoachPrefs.isTourSeen(this, COACH_KEY)) {
            View root = findViewById(android.R.id.content);
            root.postDelayed(() -> {
                if (getWindow() == null) return;
                Coach.start(
                        this,
                        (ViewGroup) root,
                        SelectDeviceScript.build(this),
                        COACH_KEY,
                        false
                );
                CoachPrefs.setTourSeen(this, COACH_KEY, true);
            }, COACH_DELAY_MS);
        }
    }

    private void registerUI() {
        lookingForNewDevices = findViewById(R.id.select_device_looking_for_new);
        deviceContainer = findViewById(R.id.select_device_device_container);
        bottomNav = findViewById(R.id.bottom_navigation);
        scanBtn = findViewById(R.id.select_device_scan_button);
        burgerMenuBtn = findViewById(R.id.select_device_burger_menu_button);
        backBtnFrame = findViewById(R.id.select_device_back_button_frame);
        burgerMenuBtnFrame = findViewById(R.id.select_device_burger_menu_button_frame);
        setupOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DeviceItemSettingsLauncher.REQ_SETTINGS && resultCode == RESULT_OK)
            if (DeviceItemSettingsLauncher.didChange(data)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "onActivityResult: changed");
                controller.loadInitial(adapter, getBluetoothAdapter());
            }
    }

    @Override
    public void onListUpdated(List<DeviceItem> items) {
        if (isInDemoMode) {
            savedDevicesForDemo = new ArrayList<>(items);
            return;
        }

        adapter.submit(items);

        if (items.isEmpty() && !controller.isScanning()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "onListUpdated: empty");
            long now = System.currentTimeMillis();
            if (now - lastAutoScan > AppConstants.AUTO_SCAN_DELAY_MS) {
                lastAutoScan = now;
                startTimedScan();
            }
        } else
            lookingForNewDevices.setVisibility(View.GONE);
    }

    private BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager mgr = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        return mgr != null ? mgr.getAdapter() : null;
    }

    private void startTimedScan() {
        if (isInDemoMode) return;
        lookingForNewDevices.setVisibility(View.VISIBLE);
        gate.runForScan(() ->
                controller.startScan(
                        getBluetoothAdapter(),
                        AppConstants.SCAN_DURATION_MS,
                        () -> lookingForNewDevices.setVisibility(View.GONE)
                )
        );
    }

    private DeviceItem makeDemoDevice() {
        return new DeviceItem(
                "00:00:00:00:00:00",
                getString(R.string.demo_device_name),
                false,
                -60,
                R.drawable.ic_device_default,
                this,
                true
        );
    }

    public void enterDevicesTutorial() {
        if (isInDemoMode) return;
        isInDemoMode = true;

        try {
            savedDevicesForDemo = adapter.snapshot();
        } catch (Throwable t) {
            savedDevicesForDemo = Collections.emptyList();
        }

        adapter.submit(Collections.singletonList(makeDemoDevice()));
        lookingForNewDevices.setVisibility(View.GONE);
    }

    public void exitDevicesTutorial() {
        if (!isInDemoMode) return;
        isInDemoMode = false;

        List<DeviceItem> restore = (savedDevicesForDemo != null)
                ? savedDevicesForDemo
                : Collections.emptyList();

        adapter.submit(restore);
        savedDevicesForDemo = null;
    }

    private void setupOptionsMenu() {
        optionsMenu = new PopupMenu(this, burgerMenuBtn, Gravity.END, 0, R.style.select_device_popup_menu);
        optionsMenu.getMenuInflater().inflate(R.menu.select_device_options_menu, optionsMenu.getMenu());
        optionsMenu.setOnMenuItemClickListener(it -> {
            if (it.getItemId() == R.id.go_to_sandbox) {
                startActivity(new Intent(this, BleSandboxActivity.class));
                return true;
            }
            if (it.getItemId() == R.id.refresh_device_list) {
                Toast.makeText(this, R.string.refreshing, Toast.LENGTH_SHORT).show();
                performRefresh();
                return true;
            }
            if (it.getItemId() == R.id.open_bt_settings) {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                return true;
            }
            if (it.getItemId() == R.id.help) {
                View root = findViewById(android.R.id.content);
                Coach.start(
                        this,
                        (ViewGroup) root,
                        SelectDeviceScript.build(this),
                        COACH_KEY,
                        false
                );
            }
            return false;
        });
    }

    private void performRefresh() {
        if (adapter != null && adapter.getItemCount() == 0 && !controller.isScanning()) return;

        controller.stopScan();
        controller.clear(true);

        adapter.submit(Collections.emptyList());

        BlePreferences.removeAllDevices(BlePreferences.get(this));

        if (lookingForNewDevices != null) lookingForNewDevices.setVisibility(View.GONE);

        if (BuildConfig.DEBUG) Log.d(TAG, "List state=" + adapter.getItemCount() + " " + controller.isScanning());
    }
}

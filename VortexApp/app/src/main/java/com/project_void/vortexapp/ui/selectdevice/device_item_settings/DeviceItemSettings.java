package com.project_void.vortexapp.ui.selectdevice.device_item_settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.project_void.vortexapp.BuildConfig;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.domain.effects.LedEffectRegistry;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.di.AppGraph;
import com.project_void.vortexapp.core.prefs.BlePreferences;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ui.dialogs.VortexDialogInfo;
import com.project_void.vortexapp.ui.dialogs.VortexDialogText;
import com.project_void.vortexapp.ui.selectdevice.ic_resolver.DeviceIconResolver;

public final class DeviceItemSettings extends AppCompatActivity {
    private static final String TAG = "ui/selectdevice/DeviceItemSettings";
    public final static String EXTRA_ADDRESS = "address";
    public final static String EXTRA_RESULT_CHANGED = "changed";
    private TextView deviceNameText, deviceStatusChip, infoMode, infoBrightness, infoColor, infoFwVersion, infoModel, infoAddress;
    private CardView deviceInfoCard;
    private View forgetBtn;
    private SwitchMaterial switchAutoConnect;
    private FrameLayout backBtnFrame;
    private BottomNavigationView deviceActionsNav;
    private String address;
    private boolean changed;
    private BleSessionRepository bleRepo;
    private BlePreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_item_settings);

        address = getIntent().getStringExtra(EXTRA_ADDRESS);
        if (address == null || address.isEmpty()) {
            finish();
            return;
        }

        bleRepo = AppGraph.bleRepo(getApplication());
        prefs = BlePreferences.get(this);
        int deviceIconCode = prefs.getIcCode(address, -1);
        if (BuildConfig.DEBUG) Log.d(TAG, "Device icon code for " + address + ": " + deviceIconCode);

        registerUI();
        renderName();
        renderSavedDeviceInfo();
        renderConnectLabel(bleRepo.isConnected(address));

        switchAutoConnect.setChecked(prefs.getAutoConnect(address));
        switchAutoConnect.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setAutoConnect(address, isChecked));

        bleRepo.observeState().observe(this, st -> {
            boolean connected = st != null
                    && st.status() == BleSessionState.Status.CONNECTED
                    && address.equals(st.deviceAddress());
            renderConnectLabel(connected);
        });

        bleRepo.observeLedState().observe(this, led -> {
            if (led == null) return;
            BleSessionState st = bleRepo.observeState().getValue();

            boolean connected = st != null && st.status() == BleSessionState.Status.CONNECTED && address.equals(st.deviceAddress());

            if (connected){
                infoMode.setText(getString(R.string.info_mode, modeLabel(led.mode())));
                infoBrightness.setText(getString(R.string.info_brightness, led.brightness()));
                infoColor.setText(getString(R.string.info_color, led.r(), led.g(), led.b()));
            }
        });

        backBtnFrame.setOnClickListener(v ->
                backBtnFrame.postDelayed(
                        this::finishWithResult,
                        AppConstants.BTN_RIPPLE_DELAY_MS)
        );

        forgetBtn.setOnClickListener(v -> showForgetDialog());

        setupDeviceActions();

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finishWithResult();
                    }
                });

        bleRepo.observePing().observe(this, p -> {
            if (p == null) return;
            if (BuildConfig.DEBUG) Log.d(TAG, "Ping res: pong in" + p.rttMillis() + "ms, token=" + p.token());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.ping_result_title))
                    .setMessage(getString(R.string.ping_result_msg, p.rttMillis(), p.token()))
                    .setPositiveButton(R.string.ok, (d, w) -> d.dismiss())
                    .show();
        });
    }

    private void setupDeviceActions(){
        deviceActionsNav.getMenu().setGroupCheckable(0, true, false);
        deviceActionsNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_rename){
                showRenameDialog();
                return false;
            }
            if (id == R.id.action_refresh){
                bleRepo.requestState();
                return false;
            }
            if (id == R.id.action_test){
                bleRepo.ping();
                return false;
            }
            if (id == R.id.action_connect){
                toggleConnection();
                changed = true;
                return false;
            }
            return false;
        });
    }

    private void renderSavedDeviceInfo(){
        int[] ver = prefs.getDeviceVersion(address);
        if (ver[0] != -1)
            infoFwVersion.setText(getString(R.string.info_fw_version, ver[0], ver[1], ver[2]));

        int modelId = prefs.getModelId(address);
        if (modelId != -1)
            infoModel.setText(getString(R.string.info_model, modelId));

        infoAddress.setText(getString(R.string.info_address, address));
        deviceInfoCard.setVisibility(View.VISIBLE);
    }

    private void renderName() {
        String alias = prefs.getAlias(address);
        deviceNameText.setText(alias != null && !alias.isEmpty() ? alias : address);
    }

    private void renderConnectLabel(boolean connected) {
        String status = getString(connected ? R.string.connected : R.string.not_paired);
        deviceStatusChip.setText(getString(R.string.device_status_format, status));
        updateActionAvailability(connected);
    }

    private void updateActionAvailability(boolean connected){
        if (deviceActionsNav == null) return;

        deviceActionsNav.getMenu().findItem(R.id.action_rename).setEnabled(connected);
        deviceActionsNav.getMenu().findItem(R.id.action_test).setEnabled(connected);
        deviceActionsNav.getMenu().findItem(R.id.action_refresh).setEnabled(connected);
        deviceActionsNav.getMenu().findItem(R.id.action_connect).setTitle(connected ? R.string.disconnect : R.string.connect);
    }
    private void toggleConnection() {
        BleSessionState st = bleRepo.observeState().getValue();
        boolean thisDeviceConnected = st != null && st.status() == BleSessionState.Status.CONNECTED && address.equals(st.deviceAddress());

        if (thisDeviceConnected)
            bleRepo.disconnect();
        else
            bleRepo.connect(address);
    }

    private void showRenameDialog() {
        VortexDialogText.show(
                this,
                getString(R.string.rename_device_title),
                getString(R.string.rename_device_hint),
                getString(R.string.rename),
                res -> {
                    String name = res.trim();
                    if (name.isEmpty()) return;
                    prefs.setAlias(address, name);
                    bleRepo.setDeviceName(name);
                    renderName();
                    changed = true;
                    Toast.makeText(this, R.string.rename_name_sent, Toast.LENGTH_LONG).show();
                }
        );
    }

    private void showForgetDialog() {
        VortexDialogInfo.show(
                this,
                getString(R.string.forget_device_title),
                getString(R.string.forget_device_message),
                getString(R.string.forget),
                () -> {
                    if (bleRepo.isConnected(address)) bleRepo.disconnect();
                    BlePreferences.removeDevice(prefs, address);
                    changed = true;
                    finishWithResult();
                }
        );
    }

    private void finishWithResult() {
        Intent data = new Intent().putExtra(EXTRA_RESULT_CHANGED, changed);
        setResult(RESULT_OK, data);
        finish();
    }

    private void registerUI() {
        deviceNameText = findViewById(R.id.device_name_text);
        ((ImageView)findViewById(R.id.device_icon)).setImageDrawable(getDrawable(DeviceIconResolver.resolve(prefs.getIcCode(address, -1), null)));
        backBtnFrame = findViewById(R.id.back_button_frame);
        forgetBtn = findViewById(R.id.forget_button);
        deviceStatusChip = findViewById(R.id.device_status_chip);
        switchAutoConnect = findViewById(R.id.switch_auto_connect);
        infoMode = findViewById(R.id.info_mode);
        infoBrightness = findViewById(R.id.info_brightness);
        infoColor = findViewById(R.id.info_color);
        deviceInfoCard = findViewById(R.id.device_info_card);
        infoFwVersion = findViewById(R.id.info_fw_version);
        infoModel = findViewById(R.id.info_model);
        infoAddress = findViewById(R.id.info_address);
        deviceActionsNav = findViewById(R.id.device_actions_navigation);
    }

    private String modeLabel(int mode) {
        return LedEffectRegistry.label(this, mode);
    }
}

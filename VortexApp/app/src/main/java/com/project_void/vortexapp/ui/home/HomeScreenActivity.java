package com.project_void.vortexapp.ui.home;

import static com.project_void.vortexapp.core.config.AppConstants.COACH_DELAY_MS;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.project_void.vortexapp.BuildConfig;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.di.AppGraph;
import com.project_void.vortexapp.core.prefs.BlePreferences;
import com.project_void.vortexapp.core.prefs.CoachPrefs;
import com.project_void.vortexapp.core.prefs.SavedSolidColorStore;
import com.project_void.vortexapp.core.suggestion.ColorNamer;
import com.project_void.vortexapp.core.suggestion.SelectionEntry;
import com.project_void.vortexapp.core.suggestion.SelectionHistoryStore;
import com.project_void.vortexapp.core.suggestion.SuggestionEngine;
import com.project_void.vortexapp.domain.effects.LedEffectRegistry;
import com.project_void.vortexapp.core.util.Debouncer;
import com.project_void.vortexapp.core.permissions.PermissionsGate;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ble.model.LedState;
import com.project_void.vortexapp.ui.color.SolidColorActivity;
import com.project_void.vortexapp.ui.common.help.Coach;
import com.project_void.vortexapp.ui.common.help.scripts.HomeScreenScript;
import com.project_void.vortexapp.ui.common.nav.BottomNavCoordinator;
import com.project_void.vortexapp.ui.effects.EffectsActivity;
import com.project_void.vortexapp.ui.selectdevice.SelectDeviceActivity;

public class HomeScreenActivity extends AppCompatActivity {
    private static final String TAG = "ui/home/HomeScreenActivity";
    private View effectsBtn, solidColorBtn, currentDeviceCard, suggestedEffectRow;
    private final SuggestionEngine suggestionEngine = new SuggestionEngine();
    private SelectionHistoryStore historyStore;
    private SavedSolidColorStore colorStore;
    private SuggestionEngine.Suggestion currentSuggestion;
    private FrameLayout selectDeviceFrame;
    private TextView deviceStatusText, activeModeText, suggestedModeText;
    private SeekBar brightnessSeekBar;
    private BottomNavigationView bottomNav;
    private BleSessionRepository bleRepo;
    private PermissionsGate gate;
    private final Debouncer debouncer = new Debouncer();
    private boolean updatingFromState = false;
    private boolean isConnected = false;
    private BlePreferences prefs;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        registerUI();

        gate = new PermissionsGate(this, this);
        getWindow().getDecorView().post(() ->
                gate.requestAllSilent(false)
        );

        selectDeviceFrame.setOnClickListener(v -> selectDeviceFrame.postDelayed(this::goToSelectDevice, AppConstants.BTN_RIPPLE_DELAY_MS));
        effectsBtn.setOnClickListener(v -> effectsBtn.postDelayed(this::goToEffects, AppConstants.BTN_RIPPLE_DELAY_MS));
        solidColorBtn.setOnClickListener(v -> solidColorBtn.postDelayed(this::goToSolidColor, AppConstants.BTN_RIPPLE_DELAY_MS));
        currentDeviceCard.setOnClickListener(v -> {if (!isConnected) currentDeviceCard.postDelayed(this::goToSelectDevice, AppConstants.BTN_RIPPLE_DELAY_MS);});
        suggestedEffectRow.setOnClickListener(v -> {
            if (!isConnected) {
                Toast.makeText(this, R.string.no_device_connected, Toast.LENGTH_SHORT).show();
                return;
            }

            if (bleRepo == null) return;

            gate.runForConnect(() -> {
                if (currentSuggestion == null) return;
                if (currentSuggestion.type == SelectionEntry.Type.EFFECT)
                    bleRepo.setAnimation(currentSuggestion.effectMode);
                else
                    bleRepo.setRgb(currentSuggestion.r, currentSuggestion.g, currentSuggestion.b);
            });
        });
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (updatingFromState) return;

                debouncer.post(24, () -> {
                    if (bleRepo != null) gate.runForConnect(() -> bleRepo.setBrightness(progress));
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bleRepo = AppGraph.bleRepo(getApplication());
        historyStore = new SelectionHistoryStore(this);
        colorStore = new SavedSolidColorStore(this);
        prefs = BlePreferences.get(this);

        bleRepo.observeState().observe(this, s -> {
            if (s == null) return;
            isConnected = s.status() == BleSessionState.Status.CONNECTED;

            String txt = isConnected ? s.status().toLowerExceptFirst() : getString(R.string.no_device_connected);
            if (s.status() == BleSessionState.Status.CONNECTED && s.deviceAddress() != null)
                txt += " " + getString(R.string.home_connected_to) + " " + prefs.getAlias(s.deviceAddress());

            deviceStatusText.setText(txt);
            currentDeviceCard.setClickable(!isConnected);
            currentDeviceCard.setFocusable(!isConnected);
            if (!isConnected) activeModeText.setText(formatModeName(0, 0, 0, 0));
        });

        bleRepo.observeLedState().observe(this, ls -> {
            if (ls == null) return;

            if (BuildConfig.DEBUG) Log.d(TAG, "observeLedState: mode=" + ls.mode() + ", brightness=" + ls.brightness() + ", rgb=(" + ls.r() + "," + ls.g() + "," + ls.b() + ")");

            updatingFromState = true;
            brightnessSeekBar.setProgress(ls.brightness());
            activeModeText.setText(formatModeName(ls.mode(), ls.r(), ls.g(), ls.b()));
            refreshSuggestion(ls);
            updatingFromState = false;
        });

        BottomNavCoordinator.attach(this, bottomNav, R.id.bottom_navigation_home);

        final String COACH_KEY = "home_screen";
        if (!CoachPrefs.isTourSeen(this, COACH_KEY)) {
            View root = findViewById(android.R.id.content);
            root.postDelayed(() -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Starting coach");
                if (getWindow() == null) return;
                Coach.start(
                        this,
                        (ViewGroup) root,
                        HomeScreenScript.build(this),
                        COACH_KEY,
                        false
                );
                CoachPrefs.setTourSeen(this, COACH_KEY, true);
            }, COACH_DELAY_MS);
        }
    }

    private void goToSelectDevice() {
        BluetoothManager btMgr = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = btMgr != null ? btMgr.getAdapter() : null;
        if (adapter != null)
            gate.runForScanAndConnect(() -> startActivity(new Intent(this, SelectDeviceActivity.class)));
        else
            Toast.makeText(this, R.string.bt_adapter_off, Toast.LENGTH_SHORT).show();
    }

    private void goToSolidColor() {
        startActivity(new Intent(this, SolidColorActivity.class));
    }

    private void goToEffects() {
        startActivity(new Intent(this, EffectsActivity.class));
    }

    private String formatModeName(int m, int r, int g, int b) {
        if (m == LedEffectRegistry.EFFECT_STATIC_COLOR) return ColorNamer.name(r, g, b);
        return LedEffectRegistry.label(this, m);
    }
    private void refreshSuggestion(LedState current){
        currentSuggestion = suggestionEngine.compute(
                historyStore.load(),
                colorStore.getAll(),
                current
        );

        if (currentSuggestion.type == SelectionEntry.Type.EFFECT)
            suggestedModeText.setText(formatModeName(currentSuggestion.effectMode, 0, 0, 0));
        else
            suggestedModeText.setText(formatModeName(LedEffectRegistry.EFFECT_STATIC_COLOR, currentSuggestion.r, currentSuggestion.g, currentSuggestion.b));
    }

    private void registerUI() {
        effectsBtn = findViewById(R.id.effects_btn);
        solidColorBtn = findViewById(R.id.solid_color_btn);
        deviceStatusText = findViewById(R.id.device_connection_status_text_view);
        selectDeviceFrame = findViewById(R.id.select_device_frame);
        brightnessSeekBar = findViewById(R.id.brightness_seek_bar);
        brightnessSeekBar.setMax(AppConstants.LED_BRIGHTNESS_MAX);
        bottomNav = findViewById(R.id.bottom_navigation);
        currentDeviceCard = findViewById(R.id.current_device_card);
        suggestedEffectRow = findViewById(R.id.home_suggested_effect_row);
        activeModeText = findViewById(R.id.home_active_mode_text);
        suggestedModeText = findViewById(R.id.home_suggested_mode_text);
    }
}

package com.project_void.vortexapp.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.project_void.vortexapp.BuildConfig;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.prefs.AppPrefs;
import com.project_void.vortexapp.core.prefs.BlePreferences;
import com.project_void.vortexapp.core.prefs.CoachPrefs;
import com.project_void.vortexapp.ui.common.nav.BottomNavCoordinator;
import com.project_void.vortexapp.ui.dialogs.VortexDialogInfo;
import com.project_void.vortexapp.ui.dialogs.VortexDialogSingleChoice;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "ui/settings/SettingsActivity";
    private BottomNavigationView bottomNav;
    private FrameLayout backBtnFrame, rowTheme, rowResetTutorials, rowCopyDebug, rowAbout, rowTerms, rowPrivacy;
    private TextView themeValue;
    private SwitchMaterial switchHaptics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate: Launching");
        registerUI();
        backBtnFrame.setOnClickListener(v -> backBtnFrame.postDelayed(this::finish, AppConstants.BTN_RIPPLE_DELAY_MS));
        int themeMode = AppPrefs.getThemeMode(this);
        themeValue.setText(themeLabel(themeMode));

        switchHaptics.setChecked(AppPrefs.isHapticsEnabled(this));
        switchHaptics.setOnCheckedChangeListener((btn, checked) -> AppPrefs.setHapticsEnabled(this, checked));

        rowTheme.setOnClickListener(v -> openThemePicker());
        rowResetTutorials.setOnClickListener(v -> confirmResetTutorials());

        rowCopyDebug.setOnClickListener(v ->{
            copyToClipBoard("Vortex Debug Info", buildDebugInfo());
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        });

        rowAbout.setOnClickListener(v -> showDialogInfo(
            getString(R.string.settings_about),
            getString(R.string.settings_about_body)
        ));

        rowTerms.setOnClickListener(v -> showDialogInfo(
            getString(R.string.settings_terms),
            getString(R.string.settings_terms_body)
        ));

        rowPrivacy.setOnClickListener(v -> showDialogInfo(
                getString(R.string.settings_privacy),
                getString(R.string.settings_privacy_body)
        ));
    }

    @Override
    protected void onStart() {
        super.onStart();
        BottomNavCoordinator.attach(this, bottomNav);
    }

    private void registerUI(){
        bottomNav = findViewById(R.id.bottom_navigation);
        backBtnFrame = findViewById(R.id.back_button_frame);
        rowTheme = findViewById(R.id.row_theme);
        themeValue = findViewById(R.id.theme_value);
        switchHaptics = findViewById(R.id.switch_haptics);
        rowResetTutorials = findViewById(R.id.row_reset_tutorials);
        rowCopyDebug = findViewById(R.id.row_copy_debug);
        rowAbout = findViewById(R.id.row_about);
        rowTerms = findViewById(R.id.row_terms);
        rowPrivacy = findViewById(R.id.row_privacy);
    }

    private void openThemePicker(){
        String[] items = new String[]{
                getString(R.string.settings_theme_system),
                getString(R.string.settings_theme_dark),
                getString(R.string.settings_theme_light)
        };

        int selected = AppPrefs.getThemeMode(this) == AppPrefs.THEME_DARK ? 1
                : AppPrefs.getThemeMode(this) == AppPrefs.THEME_LIGHT ? 2
                : 0;

        VortexDialogSingleChoice.show(
                this,
                getString(R.string.settings_theme),
                items,
                selected,
                getString(R.string.cancel),
                getString(R.string.ok),
                which -> {
                    int mode = which == 1 ? AppPrefs.THEME_DARK
                            : which == 2 ? AppPrefs.THEME_LIGHT
                            : AppPrefs.THEME_SYSTEM;
                    applyTheme(mode);
                    recreate();
                }
        );
    }

    private void applyTheme(int mode){
        AppPrefs.setThemeMode(this, mode);
        if (mode == AppPrefs.THEME_DARK)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (mode == AppPrefs.THEME_LIGHT)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private String themeLabel(int mode){
        if (mode == AppPrefs.THEME_DARK)
            return getString(R.string.settings_theme_dark);
        if (mode == AppPrefs.THEME_LIGHT)
            return getString(R.string.settings_theme_light);
        return getString(R.string.settings_theme_system);
    }

    private void confirmResetTutorials(){
        VortexDialogInfo.show(
                this,
                getString(R.string.settings_reset_tutorials),
                getString(R.string.settings_reset_tutorials_confirm),
                getString(R.string.reset),
                getString(R.string.cancel),
                () -> {
                    CoachPrefs.resetAll(this);
                    Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showDialogInfo(String title, String msg){
        VortexDialogInfo.show(
                this,
                title,
                msg,
                getString(R.string.ok),
                null,
                () -> {}
        );
    }

    private void copyToClipBoard(String label, String text){
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private String buildDebugInfo() {
        String versionName = "?";
        long versionCode = -1;
        try {
            android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pi.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                versionCode = pi.getLongVersionCode();
        } catch (Exception e) {
            Log.e(TAG, "buildDebugInfo: Failed to get version info", e);
        }

        BlePreferences prefs = BlePreferences.get(this);
        Set<String> addresses = prefs.getAllAddresses();

        StringBuilder sb = new StringBuilder();
        sb.append("=== VortexApp Debug Info ===\n");
        sb.append("App: ").append(getString(R.string.app_name)).append("\n");
        sb.append("Version: ").append(versionName).append(" (").append(versionCode).append(")\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("Haptics: ").append(AppPrefs.isHapticsEnabled(this)).append("\n");
        sb.append("Theme: ").append(themeLabel(AppPrefs.getThemeMode(this))).append("\n");
        sb.append("Last address: ").append(prefs.getLastAddress() != null ? prefs.getLastAddress() : "none").append("\n");

        sb.append("\n=== Known Devices (").append(addresses.size()).append(") ===\n");
        for (String addr : addresses) {
            String alias = prefs.getAlias(addr);
            int icCode = prefs.getIcCode(addr, -1);
            boolean autoConnect = prefs.getAutoConnect(addr);
            sb.append("• ").append(addr).append("\n");
            sb.append("  Alias: ").append(alias != null ? alias : "none").append("\n");
            sb.append("  Icon code: ").append(icCode == -1 ? "not set" : "0x" + Integer.toHexString(icCode)).append("\n");
            sb.append("  Auto-connect: ").append(autoConnect).append("\n");
        }

        return sb.toString();
    }
}

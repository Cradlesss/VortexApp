package com.project_void.vortexapp.ui.color;

import static com.project_void.vortexapp.core.config.AppConstants.COACH_DELAY_MS;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.di.AppGraph;
import com.project_void.vortexapp.core.prefs.AppPrefs;
import com.project_void.vortexapp.core.prefs.CoachPrefs;
import com.project_void.vortexapp.core.prefs.SavedSolidColorStore;
import com.project_void.vortexapp.core.permissions.PermissionsGate;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ui.common.help.Coach;
import com.project_void.vortexapp.ui.common.help.scripts.SolidColorScript;
import com.project_void.vortexapp.ui.common.nav.BottomNavCoordinator;
import com.skydoves.colorpickerview.ColorPickerView;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.google.android.material.color.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import java.util.Locale;
import java.util.Map;

public class SolidColorActivity extends AppCompatActivity implements SolidColorController.Callbacks {
    private final static String TAG = "ui/color/SolidColorActivity";
    private View colorPreview;
    private EditText hexInput;
    private TextView rValue, gValue, bValue, errorMessage;
    private Button save1, save2, save3, applyBtn;
    private FrameLayout backBtnFrame;
    private BottomNavigationView bottomNav;
    private ColorPickerView colorPicker;
    private GradientDrawable previewBg;
    private PermissionsGate gate;
    private SolidColorController controller;
    private boolean editingHex = false;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solid_color);

        registerUI();
        initPreviewBackground();

        gate = new PermissionsGate(this, this);
        BleSessionRepository bleRepo = AppGraph.bleRepo(getApplication());
        controller = new SolidColorController(new SavedSolidColorStore(this), bleRepo, this);

        bleRepo.observeState().observe(this, st -> connected = (st != null && st.status() == BleSessionState.Status.CONNECTED));

        hexInput.addTextChangedListener(hexWatcher);

        colorPicker.setColorListener((ColorEnvelopeListener) (env, fromUser) -> {
            controller.onColorPicked(env.getColor());
        });

        save1.setOnClickListener(v -> controller.onPickSaved(1));
        save2.setOnClickListener(v -> controller.onPickSaved(2));
        save3.setOnClickListener(v -> controller.onPickSaved(3));

        save1.setOnLongClickListener(v -> {
            controller.onSaveSlot(1);
            vibrate();
            return true;
        });
        save2.setOnLongClickListener(v -> {
            controller.onSaveSlot(2);
            vibrate();
            return true;
        });
        save3.setOnLongClickListener(v -> {
            controller.onSaveSlot(3);
            vibrate();
            return true;
        });

        applyBtn.setOnClickListener(v -> {
            if (!connected) {
                Toast.makeText(this, R.string.no_device_connected, Toast.LENGTH_SHORT).show();
                return;
            }
            gate.runForConnect(controller::onApply);
        });

        backBtnFrame.setOnClickListener(v -> backBtnFrame.postDelayed(this::finish, AppConstants.BTN_RIPPLE_DELAY_MS));
    }

    @Override
    protected void onStart() {
        super.onStart();
        controller.onStart();
        BottomNavCoordinator.attach(this, bottomNav, R.id.bottom_navigation_solid_color);

        final String COACH_KEY = "solid_color";
        if (!CoachPrefs.isTourSeen(this, COACH_KEY)) {
            CoachPrefs.setTourSeen(this, COACH_KEY, true);
            View root = findViewById(android.R.id.content);
            root.postDelayed(() -> {
                if (getWindow() == null) return;
                Coach.start(
                        this,
                        (ViewGroup) root,
                        SolidColorScript.build(this),
                        COACH_KEY,
                        false
                );
            }, COACH_DELAY_MS);
        }
    }

    private void registerUI() {
        colorPreview = findViewById(R.id.color_preview);
        hexInput = findViewById(R.id.hex_input);
        rValue = findViewById(R.id.r_value);
        gValue = findViewById(R.id.g_value);
        bValue = findViewById(R.id.b_value);
        save1 = findViewById(R.id.saved_color_1);
        save2 = findViewById(R.id.saved_color_2);
        save3 = findViewById(R.id.saved_color_3);
        applyBtn = findViewById(R.id.apply_color_button);
        bottomNav = findViewById(R.id.bottom_navigation);
        colorPicker = findViewById(R.id.color_picker_view);
        errorMessage = findViewById(R.id.error_message);
        backBtnFrame = findViewById(R.id.back_button_frame);
    }

    @Override
    public void renderColor(@ColorInt int color, String hex, int r, int g, int b) {
        if (previewBg != null) {
            previewBg.setColor(color);
            int stroke = MaterialColors.getColor(colorPreview, R.attr.vortexUiMain);
            previewBg.setStroke(4, stroke);
        } else
            colorPreview.setBackgroundColor(color);

        rValue.setText(getString(R.string.rgb_r_label, r));
        gValue.setText(getString(R.string.rgb_g_label, g));
        bValue.setText(getString(R.string.rgb_b_label, b));

        editingHex = true;
        hexInput.setText(hex);
        hexInput.setSelection(hex.length());
        editingHex = false;

        errorMessage.setVisibility(View.GONE);
    }

    @Override
    public void updateSavedSlots(Map<Integer, Integer> slots) {
        paintSlot(save1, slots.get(1));
        paintSlot(save2, slots.get(2));
        paintSlot(save3, slots.get(3));
    }

    @Override
    public void showInvalidHex() {
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void  paintSlot(Button btn, @Nullable Integer color) {
        if (color == null) {
            btn.setBackgroundResource(R.drawable.bg_rounded_border);
            ViewCompat.setBackgroundTintList(btn, null);
            btn.setContentDescription(getString(R.string.saved_color_empty));
            return;
        }
        btn.setBackgroundResource(R.drawable.bg_rounded_border);
        ViewCompat.setBackgroundTintList(btn, ColorStateList.valueOf(color));
        btn.setContentDescription(getString(R.string.saved_color_with_value));
    }

    private final TextWatcher hexWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (editingHex) return;
            controller.onHexChanged(s.toString());
        }
    };

    private void initPreviewBackground() {
        Drawable base = AppCompatResources.getDrawable(this, R.drawable.bg_rounded_border);
        if (base != null) {
            Drawable clone = (base.getConstantState() != null
                    ? base.getConstantState().newDrawable()
                    : base).mutate();

            if (clone instanceof GradientDrawable) {
                previewBg = (GradientDrawable) clone;
                colorPreview.setBackground(previewBg);
            }
        }
    }

    private void vibrate(){
        if (!AppPrefs.isHapticsEnabled(this)) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            try {
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator())
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
            } catch (Throwable t){
                Log.e(TAG, "vibrate err: ", t);
            }
        } else{
            View root = findViewById(android.R.id.content);
            root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }
}

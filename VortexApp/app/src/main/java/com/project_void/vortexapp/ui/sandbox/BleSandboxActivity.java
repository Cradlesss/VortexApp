package com.project_void.vortexapp.ui.sandbox;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import com.project_void.vortexapp.BuildConfig;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.di.AppGraph;
import com.project_void.vortexapp.core.permissions.PermissionsGate;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.ble.model.LedState;

public class BleSandboxActivity extends AppCompatActivity {
    private static final String TAG = "ui/sandbox/BleSandboxActivity";
    private BleSessionRepository repo;
    private EditText etAddress;
    private Button btnConnect, btnAnim5, btnBright, btnRed, btnReqState, btnGreen, pingBtn;
    private FrameLayout backBtnFrame;
    private TextView tvConn, tvState;
    private PermissionsGate gate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_sandbox);

        repo = AppGraph.bleRepo(getApplication());
        gate = new PermissionsGate(this, this);

        registerUI();

        btnConnect.setOnClickListener(v -> {
            String address = etAddress.getText().toString().trim();

            if (TextUtils.isEmpty(address)) {
                Toast.makeText(this, R.string.ble_sandbox_err_address_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            repo.connect(address);
            Toast.makeText(this, getString(R.string.ble_sandbox_connecting_to, address), Toast.LENGTH_SHORT).show();
        });
        btnAnim5.setOnClickListener(v -> gate.runForConnect(() -> repo.setAnimation(5)));
        btnBright.setOnClickListener(v -> gate.runForConnect(() ->repo.setBrightness(180)));
        btnRed.setOnClickListener(v -> gate.runForConnect(() -> repo.setRgb(255, 0, 0)));
        btnGreen.setOnClickListener(v -> gate.runForConnect(() -> repo.setRgb(0, 255, 0)));
        backBtnFrame.setOnClickListener(v -> backBtnFrame.postDelayed(this::finish, AppConstants.BTN_RIPPLE_DELAY_MS));
        btnReqState.setOnClickListener(v -> gate.runForConnect(() -> repo.requestState()));
        pingBtn.setOnClickListener(v -> gate.runForConnect(() -> repo.ping()));
        repo.observeState().observe(this, s -> {
            if (s == null) return;
            String txt = "Connection: " + s.status();
            if (BuildConfig.DEBUG) Log.d(TAG, "observeState: " + txt);
            if (s.status() == BleSessionState.Status.CONNECTED && s.deviceAddress() != null)
                txt += " (" + s.deviceAddress() + ")";
            tvConn.setText(txt);
        });
        repo.observeLedState().observe(this, ls -> {
            if (ls == null) {
                if (BuildConfig.DEBUG) Log.d(TAG, "observeLedState: null");
                return;
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "observeLedState: " + ls);
            tvState.setText(formatState(ls));
        });
        repo.observePing().observe(this, p -> {
            if (p == null) return;
            if (BuildConfig.DEBUG) Log.d(TAG, "observePing: " + p);

            String mgs = (p.token() >= 0)
                    ? getString(R.string.ble_sandbox_pong_format, p.rttMillis(), p.token())
                    : getString(R.string.ble_sandbox_pong_simple_format, p.rttMillis());

            new AlertDialog.Builder(this)
                    .setTitle(R.string.ble_sandbox_ping_result_title)
                    .setMessage(mgs)
                    .setPositiveButton(R.string.finish, (d, w) -> d.dismiss())
                    .show();
        });
    }

    private void registerUI() {
        btnConnect = findViewById(R.id.btnConnect);
        btnAnim5 = findViewById(R.id.btnAnim5);
        btnBright = findViewById(R.id.btnBright180);
        btnRed = findViewById(R.id.btnRed);
        btnReqState = findViewById(R.id.requestState);
        btnGreen = findViewById(R.id.btnGreen);
        etAddress = findViewById(R.id.etAddress);
        tvConn = findViewById(R.id.tvConn);
        tvState = findViewById(R.id.tvState);
        backBtnFrame = findViewById(R.id.back_button_frame);
        pingBtn = findViewById(R.id.btnPing);
    }

    private String formatState(LedState ls) {
        return "State: mode=" + ls.mode() + ", brightness=" + ls.brightness() + ", rgb=(" + ls.r() + "," + ls.g() + "," + ls.b() + ")";
    }
}

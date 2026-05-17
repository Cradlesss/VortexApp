package com.project_void.vortexapp.ui.effects;

import static androidx.recyclerview.widget.RecyclerView.*;
import static com.project_void.vortexapp.core.config.AppConstants.COACH_DELAY_MS;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.core.config.AppConstants;
import com.project_void.vortexapp.core.di.AppGraph;
import com.project_void.vortexapp.core.prefs.CoachPrefs;
import com.project_void.vortexapp.core.permissions.PermissionsGate;
import com.project_void.vortexapp.ble.model.BleSessionState;
import com.project_void.vortexapp.domain.effects.Effect;
import com.project_void.vortexapp.domain.effects.LedEffectRegistry;
import com.project_void.vortexapp.ui.common.help.Coach;
import com.project_void.vortexapp.ui.common.help.scripts.EffectsScreenScript;
import com.project_void.vortexapp.ui.common.nav.BottomNavCoordinator;
import java.util.List;

public class EffectsActivity extends AppCompatActivity implements EffectsAdapter.OnItemClick {
    private RecyclerView rv;
    private View overlay;
    private EffectsAdapter adapter;
    private OverlayCarouselHelper carousel;
    private BleSessionRepository bleRepo;
    private final int[] locationBuffer = new int[2];
    private boolean connected = false;
    private FrameLayout backBtnFrame;
    private BottomNavigationView bottomNav;
    private PermissionsGate gate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_effects);

        registerUI();

        gate = new PermissionsGate(this, this);

        rv.setLayoutManager(new LinearLayoutManager(this, VERTICAL, false));
        rv.setClipToPadding(false);

        adapter = new EffectsAdapter(buildEffects(), this);
        rv.setAdapter(adapter);

        int itemSpacing = (int) (12 * getResources().getDisplayMetrics().density);

        carousel = new OverlayCarouselHelper(rv, overlay, pos -> rv.post(() -> adapter.setSelected(pos)));
        rv.addOnScrollListener(carousel);

        rv.post(() -> {
            ensureEdgePadding();
            carousel.recompute();
            carousel.maybeSnap();
        });

        rv.addItemDecoration(new ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
                outRect.bottom = itemSpacing;
            }
        });

        rv.post(this::ensureEdgePadding);

        bleRepo = AppGraph.bleRepo(getApplication());
        bleRepo.observeState().observe(this, s -> connected = (s != null && s.status() == BleSessionState.Status.CONNECTED));

        backBtnFrame.setOnClickListener(v -> backBtnFrame.postDelayed(this::finish, AppConstants.BTN_RIPPLE_DELAY_MS));
    }

    @Override
    protected void onStart() {
        super.onStart();
        BottomNavCoordinator.attach(this, bottomNav, R.id.bottom_navigation_effects);

        final String COACH_KEY = "effects";
        if (!CoachPrefs.isTourSeen(this, COACH_KEY)) {
            View root = findViewById(android.R.id.content);
            root.postDelayed(() -> {
                if (getWindow() == null) return;
                Coach.start(
                        this,
                        (ViewGroup) root,
                        EffectsScreenScript.build(this),
                        COACH_KEY,
                        false
                );
            }, COACH_DELAY_MS);
            CoachPrefs.setTourSeen(this, COACH_KEY, true);
        }
    }

    private void ensureEdgePadding() {
        if (rv.getChildCount() == 0) {
            rv.post(this::ensureEdgePadding);
            return;
        }

        View firstChild = rv.getChildAt(0);
        int itemHeight = firstChild.getHeight();
        int overlayCenterY = overlayCenterInRvSpace();

        int topPad = Math.max(0, overlayCenterY - itemHeight / 2);
        int bottomPad = Math.max(0, rv.getHeight() - overlayCenterY - itemHeight / 2);

        rv.setPadding(
                rv.getPaddingLeft(),
                topPad,
                rv.getPaddingRight(),
                bottomPad
        );

        carousel.recompute();
        carousel.maybeSnap();
    }

    private List<Effect> buildEffects() {
        return LedEffectRegistry.list(this);
    }

    @Override
    public void onItemClick(int adapterPos, Effect effect) {
        int selected = carousel.getSelectedAdapterPosition();
        if (adapterPos != selected) {
            carousel.centerOn(adapterPos);
            return;
        }

        if (!connected) {
            Toast.makeText(this, R.string.no_device_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        gate.runForConnect(() -> bleRepo.setAnimation(effect.id()));
        Toast.makeText(this, getString(R.string.effect_applied_toast, effect.name()), Toast.LENGTH_SHORT).show();
    }

    private int topOnScreen(View v) {
        v.getLocationOnScreen(locationBuffer);
        return locationBuffer[1];
    }

    private void registerUI() {
        rv = findViewById(R.id.effects_recycler);
        overlay = findViewById(R.id.effects_overlay);
        backBtnFrame = findViewById(R.id.back_button_frame);
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private int overlayCenterInRvSpace() {
        int rvTopOnScreen = topOnScreen(rv);
        int overlayTopOnScreen = topOnScreen(overlay);
        int overlayTopInRv = overlayTopOnScreen - rvTopOnScreen;
        return overlayTopInRv + overlay.getHeight() / 2;
    }
}

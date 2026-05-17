package com.project_void.vortexapp.ui.color;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.project_void.vortexapp.ble.repo.BleSessionRepository;
import com.project_void.vortexapp.core.prefs.SavedSolidColorStore;
import java.util.Map;

public final class SolidColorController {
    public interface Callbacks {
        void renderColor(@ColorInt int color, String hex, int r, int g, int b);

        void updateSavedSlots(Map<Integer, Integer> slots);

        void showInvalidHex();
    }

    private final SavedSolidColorStore store;
    private final BleSessionRepository repo;
    private final Callbacks ui;
    private @ColorInt int currentColor = Color.WHITE;
    private String currentHex = "#FFFFFF";

    public SolidColorController(SavedSolidColorStore store, BleSessionRepository repo, Callbacks ui) {
        this.store = store;
        this.repo = repo;
        this.ui = ui;
    }

    public void onStart() {
        ui.updateSavedSlots(store.getAll());

        String last = store.getLastHex();
        if (last != null)
            setCurrent(hexToColor(last), last, true);
        else
            setCurrent(currentColor, currentHex, true);
    }

    public void onHexChanged(String raw) {
        String norm = normalizeHex(raw);
        if (norm == null) {
            ui.showInvalidHex();
            return;
        }
        @ColorInt int c = hexToColor(norm);
        setCurrent(c, norm, true);
    }

    public void onColorPicked(@ColorInt int color) {
        String hex = colorToHex(color);
        setCurrent(color, hex, true);
    }

    public void onApply() {
        sendNow(currentColor);
        store.setLastHex(currentHex);
    }

    public void onSaveSlot(int slot) {
        store.set(slot, currentColor);
        store.setLastHex(currentHex);
        ui.updateSavedSlots(store.getAll());
    }

    public void onPickSaved(int slot) {
        Integer c = store.get(slot);
        if (c != null)
            setCurrent(c, colorToHex(c), true);
    }

    private void setCurrent(@ColorInt int color, String hex, boolean push) {
        currentColor = color;
        currentHex = hex;
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        if (push) ui.renderColor(color, hex, r, g, b);
    }

    private void sendNow(@ColorInt int color) {
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        repo.setRgb(r, g, b);
    }

    @Nullable
    private static String normalizeHex(String raw) {
        if (raw == null) return null;

        String s = raw.trim();

        if (s.isEmpty()) return null;
        if (s.charAt(0) != '#') s = "#" + s;
        if (s.length() != 7) return null;

        for (int i = 1; i < 7; i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') ||
                    (c >= 'a' && c <= 'f') ||
                    (c >= 'A' && c <= 'F');
            if (!ok) return null;
        }
        return s.toUpperCase();
    }

    private static @ColorInt int hexToColor(String hex) {
        return Color.parseColor(hex);
    }

    private static String colorToHex(@ColorInt int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }
}
package com.project_void.vortexapp.ble.model;

import androidx.annotation.NonNull;

public record LedState(int mode, int brightness, int r, int g, int b) {

    @NonNull
    @Override
    public String toString() {
        return "LedState{mode=" + mode + ", brightness=" + brightness + ", rgb=(" + r + "," + g + "," + b + ")}";
    }
}

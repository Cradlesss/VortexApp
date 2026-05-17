package com.project_void.vortexapp.ble.model;

import androidx.annotation.NonNull;

public record PingResult(int token, long rttMillis) {

    @NonNull
    @Override
    public String toString() {
        return "PingResult{" +
                "token=" + token +
                ", rttMillis=" + rttMillis +
                '}';
    }
}

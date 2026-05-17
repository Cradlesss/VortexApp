package com.project_void.vortexapp.ble.service;

import java.util.UUID;

public final class BleUuids {
    private BleUuids() {}

    public static final UUID SERVICE_LED = UUID.fromString("895dc926-817a-424d-8736-f582d2dbac8e");
    public static final UUID CHAR_CONTROL_POINT = UUID.fromString("7953deb4-b2e1-4829-a692-8ec173cc71fc");
    public static final UUID CHAR_STATE = UUID.fromString("a4de8684-c8ef-460f-ab4f-027237d50997");
    public static final UUID DESC_CCCD = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
}

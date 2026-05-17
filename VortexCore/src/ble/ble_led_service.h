#pragma once
#include <bluefruit.h>
#include "proto.h"

class LedController;

class BleLedService {
    public:
    explicit BleLedService(LedController& controller);

    bool begin();
    void startAdvertising(const char* defaultName, bool restartOnDisconnect = true);
    void publishState();
    void tick();

    const char* activeName() const { return activeName_; }
    const char* pendingName() const { return pendingName_; }
    bool isNameSavePending() const { return nameDirty_; }

    private:
    static void onControlWriteThunk(uint16_t conn_hdl, BLECharacteristic* chr, uint8_t* data, uint16_t len);
    static void onConnectThunk(uint16_t conn_hdl);
    static void onDisconnectThunk(uint16_t conn_hdl, uint8_t reason);
    void onConnect(uint16_t conn_hdl);
    void onDisconnect(uint16_t conn_hdl, uint8_t reason);
    void onControlWrite(uint16_t conn_hdl, uint8_t* data, uint16_t len);

    void markDirty_();
    void maybePublishState_();
    void maybeSaveName_();

    static constexpr uint32_t PUBLISH_INTERVAL_MS = 120;
    static constexpr uint8_t  MAX_NAME_LEN = 18;

    bool dirty_ = false;
    uint32_t lastPublishMs_ = 0;

    char activeName_[MAX_NAME_LEN + 1]  = {};
    char pendingName_[MAX_NAME_LEN + 1] = {};
    bool nameDirty_ = false;

    LedController& ctrl_;
    BLEService svc_{UUID_SVC_LED};
    BLECharacteristic chrCtrl_{UUID_CHR_CONTROL};
    BLECharacteristic chrState_{UUID_CHR_STATE};

    static BleLedService* self_;
};
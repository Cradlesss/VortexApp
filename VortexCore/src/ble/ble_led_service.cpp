#include "ble_led_service.h"
#include "led/led_controller.h"
#include "led/core/StateStorage.h"
#include <bluefruit.h>
#include <Arduino.h>
#include <array>
#include <debug.h>

BleLedService* BleLedService::self_ = nullptr;

BleLedService::BleLedService(LedController& controller) : ctrl_(controller){}

bool BleLedService::begin() {
    self_ = this;

    auto& storage = StateStorage::instance();
    storage.loadName();
    if (storage.nameValid()) {
        const auto& nd = storage.nameData();
        uint8_t n = nd.len < MAX_NAME_LEN ? nd.len : MAX_NAME_LEN;
        memcpy(activeName_, nd.name, n);
        activeName_[n] = '\0';
    }

    Bluefruit.begin();

    svc_.begin();

    Bluefruit.Periph.setConnectCallback(onConnectThunk);
    Bluefruit.Periph.setDisconnectCallback(onDisconnectThunk);

    chrState_.setProperties(CHR_PROPS_READ | CHR_PROPS_NOTIFY);
    chrState_.setPermission(SECMODE_OPEN, SECMODE_OPEN);
    chrState_.setFixedLen(5);
    chrState_.begin();

    chrCtrl_.setProperties(CHR_PROPS_WRITE | CHR_PROPS_WRITE_WO_RESP | CHR_PROPS_NOTIFY);
    chrCtrl_.setPermission(SECMODE_OPEN, SECMODE_OPEN);
    chrCtrl_.setMaxLen(20);
    chrCtrl_.setWriteCallback(onControlWriteThunk);
    chrCtrl_.begin();

    publishState();
    return true;
}

void BleLedService::startAdvertising(const char* defaultName, bool restartOnDisconnect) {
    if (activeName_[0]) Bluefruit.setName(activeName_);
    else if (defaultName && *defaultName) Bluefruit.setName(defaultName);

    Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);
    Bluefruit.Advertising.addTxPower();
    Bluefruit.Advertising.addService(svc_);
    Bluefruit.ScanResponse.addName();

    Adv_Manufacturer mfg{};
    mfg.companyId = 0xFFFF;
    mfg.protoVer = 0x01;
    mfg.modelId = 0x01;
    mfg.version[0] = VER_MAJOR;
    mfg.version[1] = VER_MINOR;
    mfg.version[2] = VER_PATCH;
    mfg.iconCode = ICON_TEST;
    mfg.features = FEAT_LED_RGB | FEAT_LED_ANIMATION;

    Bluefruit.ScanResponse.addData(BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA, &mfg, sizeof(mfg));


    Bluefruit.Advertising.setInterval(160, 160);
    Bluefruit.Advertising.setFastTimeout(30);
    Bluefruit.Advertising.restartOnDisconnect(restartOnDisconnect);
    Bluefruit.Advertising.start(0);
}

void BleLedService::publishState() {
    const auto& s = ctrl_.state();
    const std::array<uint8_t, 5> payload = {s.mode, s.brightness, s.r, s.g, s.b};

    VDBG("OP GET_STATE current.mode=%u, current.brightness=%u, current.r=%u, current.g=%u, current.b=%u\n",
         s.mode, s.brightness, s.r, s.g, s.b);

    chrState_.write(payload.data(), payload.size());
#ifdef VORTEX_DEBUG
    const bool ok = chrState_.notify(payload.data(), payload.size());
    VDBG("Publish state: %s\n", ok ? "Success" : "Fail");
#else
    chrState_.notify(payload.data(), payload.size());
#endif
}

void BleLedService::markDirty_() {
    dirty_ = true;
    lastPublishMs_ = millis();
}

void BleLedService::maybePublishState_() {
    if (dirty_ && (millis() - lastPublishMs_ >= PUBLISH_INTERVAL_MS)) {
        dirty_ = false;
        publishState();
    }
}

void BleLedService::tick() {
    maybePublishState_();
    maybeSaveName_();
}

void BleLedService::onControlWriteThunk(uint16_t conn_hdl, BLECharacteristic* chr, uint8_t* data, uint16_t len) {
    (void)chr;
    if (self_) self_->onControlWrite(conn_hdl, data, len);
}

void BleLedService::onConnectThunk(uint16_t conn_hdl) {
    if (self_) self_->onConnect(conn_hdl);
}

void BleLedService::onConnect(uint16_t conn_hdl) {
#ifdef VORTEX_DEBUG
    BLEConnection* conn = Bluefruit.Connection(conn_hdl);
    char centralName[32] = {};
    conn->getPeerName(centralName, sizeof(centralName));
    ble_gap_addr_t addr = conn->getPeerAddr();
    VDBG("==== CENTRAL CONNECTED ====\n"
         "Handle: %u\nName: %s\n"
         "MAC: %02X:%02X:%02X:%02X:%02X:%02X\n"
         "Interval (ms): %.2f\nMTU: %u\nRSSI: %d\n",
         conn_hdl,
         centralName[0] ? centralName : "(unknown)",
         addr.addr[5], addr.addr[4], addr.addr[3],
         addr.addr[2], addr.addr[1], addr.addr[0],
         static_cast<float>(conn->getConnectionInterval()) * 1.25f,
         conn->getMtu(),
         conn->getRssi());
#else
    (void)conn_hdl;
#endif
}

void BleLedService::onDisconnectThunk(uint16_t conn_hdl, uint8_t reason) {
    if (self_) self_->onDisconnect(conn_hdl, reason);
}

void BleLedService::onDisconnect(uint16_t conn_hdl, uint8_t reason) {
#ifdef VORTEX_DEBUG
    const char* msg = "Unknown";
    switch (reason) {
        case BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION:         msg = "User disconnected";    break;
        case BLE_HCI_CONNECTION_TIMEOUT:                        msg = "Connection timeout";   break;
        case BLE_HCI_REMOTE_DEV_TERMINATION_DUE_TO_LOW_RESOURCES: msg = "Remote low resources"; break;
        case BLE_HCI_LOCAL_HOST_TERMINATED_CONNECTION:          msg = "Local disconnect";     break;
        case BLE_HCI_CONN_FAILED_TO_BE_ESTABLISHED:             msg = "Failed to establish";  break;
        case BLE_HCI_CONN_INTERVAL_UNACCEPTABLE:                msg = "Unacceptable interval"; break;
        default: break;
    }
    VDBG("==== CENTRAL DISCONNECTED ====\nHandle: %u\nReason: %u (%s)\n", conn_hdl, reason, msg);
#else
    (void)conn_hdl; (void)reason;
#endif
}

void BleLedService::onControlWrite(uint16_t conn_hdl, uint8_t *data, uint16_t len) {
#ifdef VORTEX_DEBUG
    VDBG("CTRL WRITE len=%u : ", len);
    for (uint16_t i = 0; i < len; ++i) VDBG("%02X ", data[i]);
    Serial.println();
#endif

    if (len < 2) return;

    const uint8_t opByte = data[0];
    const uint8_t payLen = data[1];
    const uint16_t expect = static_cast<uint16_t>(2u + payLen);
    if (len != expect) return;

    switch (static_cast<Op>(opByte)) {
        case Op::SetAnimation:
            if (payLen >= 1) {
                ctrl_.setAnimation(data[2]);
                publishState();
            }
            break;
        case Op::SetBrightness:
            if (payLen >= 1) {
                ctrl_.setBrightness(data[2]);
                markDirty_();
            }
            break;
        case Op::SetRgb:
            if (payLen >= 3) {
                ctrl_.setRgb(data[2], data[3], data[4]);
                publishState();
            }
            break;
        case Op::SetName: {
            if (payLen < 1 || payLen > MAX_NAME_LEN) {
                VDBG("OP SET_NAME: invalid payLen=%u\n", payLen);
                break;
            }
            memcpy(pendingName_, data + 2, payLen);
            pendingName_[payLen] = '\0';
            nameDirty_ = true;
            VDBG("OP SET_NAME: queued '%s' (will save on next tick)\n", pendingName_);
            break;
        }
        case Op::GetState:
            publishState();
            break;
        case Op::Ping: {
            VDBG_LN("OP PING");
            uint8_t tokenLen = (payLen >= 1) ? 1 : 0;
            uint8_t resp[3] = {OP_PONG, tokenLen, 0x00};
            if (tokenLen == 1) resp[2] = data[2];
            chrCtrl_.notify(resp, static_cast<uint16_t>(2 + tokenLen));// returns bool;
            break;
        }
        default:
            VDBG_LN("Unknown op");
            break;
    }
}

void BleLedService::maybeSaveName_() {
    if (!nameDirty_) return;
    nameDirty_ = false;
    uint8_t len = static_cast<uint8_t>(strnlen(pendingName_, MAX_NAME_LEN));
    if (len > 0) StateStorage::instance().saveName(pendingName_, len);
}
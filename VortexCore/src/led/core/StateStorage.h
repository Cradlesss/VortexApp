#pragma once
#include <Arduino.h>
#include "led/core/Commands/Commands.h"

struct SavedState  {
  uint8_t magic;
  uint8_t version;
  uint8_t mode;
  uint8_t brightness;
  uint8_t r, g, b;
  uint8_t checksum;
};

struct SavedName {
  uint8_t magic;
  uint8_t version;
  uint8_t len;
  char    name[18];
  uint8_t checksum;
};

class StateStorage {
public:
  static constexpr uint8_t NAME_MAX_LEN = 18;

  static StateStorage& instance();

  void load();
  void save(Command cmd, uint8_t brightness, uint8_t r, uint8_t g, uint8_t b);
  void debugDump();
  bool valid() const { return valid_; }
  const SavedState& data() const { return state_; }

  void loadName();
  void saveName(const char* name, uint8_t len);
  bool nameValid() const { return nameValid_; }
  const SavedName& nameData() const { return nameBuf_; }

private:
  StateStorage() = default;

  void ensureFsMounted();
  static uint8_t checksum(const SavedState& state);
  static uint8_t nameChecksum(const SavedName& s);

  SavedState state_{};
  bool valid_{false};
  bool fsOk_{false};
  bool fsTried_{false};

  SavedName nameBuf_{};
  bool nameValid_{false};
};
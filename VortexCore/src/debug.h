#pragma once

#ifdef VORTEX_DEBUG
#  define VDBG(fmt, ...)  Serial.printf(fmt, ##__VA_ARGS__)
#  define VDBG_LN(str)    Serial.println(F(str))
#else
#  define VDBG(fmt, ...)  do {} while (0)
#  define VDBG_LN(str)    do {} while (0)
#endif

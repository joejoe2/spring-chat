package com.joejoe2.chat.utils;

import java.time.Instant;

public class TimeUtil {
  public static Instant roundToMicro(Instant instant) {
    long nano = instant.getNano() % 1000;
    if (nano >= 500) {
      return instant.plusNanos(-nano).plusNanos(1000);
    } else {
      return instant.plusNanos(-nano);
    }
  }
}

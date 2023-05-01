package com.joejoe2.chat.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimeUtilTest {
  @Test
  void round() {
    Instant instant = Instant.now();
    Instant up = Instant.parse("2023-04-17T04:53:13.123456600Z");
    Instant down = Instant.parse("2023-04-17T04:53:13.123456400Z");
    assertEquals(up.plusNanos(400), TimeUtil.roundToMicro(up));
    assertEquals(down.plusNanos(-400), TimeUtil.roundToMicro(down));
  }
}

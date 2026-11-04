package com.example

import com.example.ui.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFormatBytes() {
    assertEquals("0 B", FormatUtils.formatBytes(0L))
    assertEquals("1.0 KB", FormatUtils.formatBytes(1024L))
    assertEquals("1.5 MB", FormatUtils.formatBytes(1572864L))
  }

  @Test
  fun testFormatSpeed() {
    assertEquals("0 KB/s", FormatUtils.formatSpeed(0L))
    assertTrue(FormatUtils.formatSpeed(2097152L).contains("MB/s"))
  }

  @Test
  fun testFormatEta() {
    assertEquals("Finishing...", FormatUtils.formatEta(0L))
    assertEquals("30s left", FormatUtils.formatEta(30L))
    assertEquals("2m 5s left", FormatUtils.formatEta(125L))
  }
}

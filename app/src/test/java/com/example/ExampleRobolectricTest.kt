package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CloudDrop", appName)
  }

  @Test
  fun `format bytes correctly`() {
    assertEquals("0 B", FormatUtils.formatBytes(0L))
    assertEquals("500 B", FormatUtils.formatBytes(500L))
    assertEquals("1.0 KB", FormatUtils.formatBytes(1024L))
    assertEquals("1.5 MB", FormatUtils.formatBytes(1572864L))
    assertEquals("1.0 GB", FormatUtils.formatBytes(1073741824L))
  }

  @Test
  fun `format speed correctly`() {
    assertEquals("0 KB/s", FormatUtils.formatSpeed(0L))
    assertTrue(FormatUtils.formatSpeed(1048576L).contains("MB/s"))
  }

  @Test
  fun `format eta correctly`() {
    assertEquals("Finishing...", FormatUtils.formatEta(0L))
    assertEquals("45s left", FormatUtils.formatEta(45L))
    assertEquals("1m 15s left", FormatUtils.formatEta(75L))
  }
}

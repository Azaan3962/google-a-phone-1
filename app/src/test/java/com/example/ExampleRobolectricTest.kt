package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.ApkManager
import com.example.util.ProfileStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("GoLogin & APK Hub", appName)
  }

  @Test
  fun `verify profile storage initial values`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val profiles = ProfileStorage.getProfiles(context)
    assertTrue("Default profiles should be created", profiles.isNotEmpty())
    val activeId = ProfileStorage.getActiveProfileId(context)
    assertNotNull(activeId)
  }

  @Test
  fun `verify apk directory creation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dir = ApkManager.getApksDirectory(context)
    assertTrue(dir.exists())
  }
}

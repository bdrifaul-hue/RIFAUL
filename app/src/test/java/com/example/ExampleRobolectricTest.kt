package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.AppControlManager
import com.example.service.SpecialCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RIFAUL AI", appName)
  }

  @Test
  fun `verify app control command detection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = AppControlManager(context)

    // Bengali YouTube launch command
    val youtubeTarget = manager.matchAppLaunchCommand("YouTube চালু করো")
    assertNotNull(youtubeTarget)
    assertEquals("com.google.android.youtube", youtubeTarget?.packageName)

    // English Chrome launch command
    val chromeTarget = manager.matchAppLaunchCommand("Open Chrome browser")
    assertNotNull(chromeTarget)
    assertEquals("com.android.chrome", chromeTarget?.packageName)

    // Bengali Facebook launch command
    val fbTarget = manager.matchAppLaunchCommand("Facebook খোলো")
    assertNotNull(fbTarget)
    assertEquals("com.facebook.katana", fbTarget?.packageName)

    // Bengali Special Command: Time
    val timeCmd = manager.detectSpecialCommand("এখন কয়টা বাজে?")
    assertTrue(timeCmd is SpecialCommand.CurrentTime)

    // Stop command
    val stopCmd = manager.detectSpecialCommand("Stop listening")
    assertEquals(SpecialCommand.StopListening, stopCmd)
  }
}

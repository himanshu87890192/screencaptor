package com.wealthfront.screencaptor

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.widget.ImageView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.wealthfront.screencaptor.views.modifier.TextViewDataModifier
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

  private val screenShotDirectory = "/sdcard/screenshots/"

  @get:Rule
  var activityTestRule : ActivityTestRule<SampleActivity> = ActivityTestRule(SampleActivity::class.java)

  @get:Rule
  var permissionsRule = GrantPermissionRule.grant(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

  @After
  fun cleanUpScreenshots() {
    File(screenShotDirectory).deleteRecursively()
  }

  @Test
  fun takeScreenshot_view() {
    val rootView = activityTestRule.activity.window.decorView

    ScreenCaptor.takeScreenshot(
      view = rootView,
      screenshotName = "screenshot_view"
    )

    Espresso.onIdle {
      assertTrue(File(screenShotDirectory).exists())
      assertTrue(File(screenShotDirectory).listFiles()!!.isNotEmpty())
      assertTrue(File(screenShotDirectory).listFiles()!!.find { it.name.contains("screenshot_view") }!!.exists())
    }
  }

  @Test
  fun takeScreenshot_views() {
    val imageView = activityTestRule.activity.findViewById<ImageView>(R.id.wealthfrontIcon)

    ScreenCaptor.takeScreenshot(
      views = listOf(imageView),
      screenshotName = "screenshot_views"
    )

    Espresso.onIdle {
      assertTrue(File(screenShotDirectory).exists())
      assertTrue(File(screenShotDirectory).listFiles()!!.isNotEmpty())
      assertTrue(File(screenShotDirectory).listFiles()!!.find { it.name.contains("screenshot_views") }!!.exists())
    }
  }

  @Test
  fun takeScreenshot_activity() {
    onView(withId(R.id.showToast)).perform(click())

    ScreenCaptor.takeScreenshot(
      activity = activityTestRule.activity,
      screenshotName = "screenshot_activity"
    )

    Espresso.onIdle {
      assertTrue(File(screenShotDirectory).exists())
      assertTrue(File(screenShotDirectory).listFiles()!!.isNotEmpty())
      assertTrue(File(screenShotDirectory).listFiles()!!.find { it.name.contains("screenshot_activity") }!!.exists())
    }
  }

  @Test
  fun takeScreenshot_modify() {
    onView(withId(R.id.showToast)).perform(click())

    ScreenCaptor.takeScreenshot(
      activity = activityTestRule.activity,
      screenshotName = "screenshot_change_text",
      viewModifiers = setOf(TextViewDataModifier(R.id.textView, "Some shorter sample data"))
    )

    Espresso.onIdle {
      assertTrue(File(screenShotDirectory).exists())
      assertTrue(File(screenShotDirectory).listFiles()!!.isNotEmpty())
      assertTrue(File(screenShotDirectory).listFiles()!!.find { it.name.contains("screenshot_change_text") }!!.exists()
      )
    }
  }

  @Test
  fun takeScreenshot_exclude() {
    ScreenCaptor.takeScreenshot(
      activity = activityTestRule.activity,
      screenshotName = "screenshot_no_logo",
      viewIdsToExclude = setOf(R.id.wealthfrontIcon)
    )

    Espresso.onIdle {
      assertTrue(File(screenShotDirectory).exists())
      assertTrue(File(screenShotDirectory).listFiles()!!.isNotEmpty())
      assertTrue(File(screenShotDirectory).listFiles()!!.find { it.name.contains("screenshot_no_logo") }!!.exists())
    }
  }
}
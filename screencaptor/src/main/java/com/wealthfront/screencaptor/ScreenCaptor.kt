package com.wealthfront.screencaptor

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.wealthfront.screencaptor.ScreenCaptor.takeScreenshot
import com.wealthfront.screencaptor.ScreenshotFormat.PNG
import com.wealthfront.screencaptor.ScreenshotQuality.BEST
import com.wealthfront.screencaptor.views.modifier.DataModifier
import com.wealthfront.screencaptor.views.modifier.DefaultViewDataProcessor
import com.wealthfront.screencaptor.views.modifier.ViewDataProcessor
import com.wealthfront.screencaptor.views.modifier.ViewVisibilityModifier
import com.wealthfront.screencaptor.views.mutator.CursorHider
import com.wealthfront.screencaptor.views.mutator.ScrollbarHider
import com.wealthfront.screencaptor.views.mutator.ViewMutator
import com.wealthfront.screencaptor.views.mutator.ViewTreeMutator
import java.io.File
import java.io.FileOutputStream
import java.util.Locale.ENGLISH
import java.util.concurrent.atomic.AtomicInteger

/**
 * Has the ability to take a screenshot of the current view displayed on the screen using the method [takeScreenshot].
 */
object ScreenCaptor {

  data class ViewTreeState(val viewVisibilityStates: Map<Int, Int>, val viewDataStates: Set<DataModifier>)

  private val mainHandler = Handler(Looper.getMainLooper())
  private val SCREENSHOT = javaClass.simpleName

  /**
   * Takes a screenshot whenever the method is called from the test thread or the main thread.
   * Also note that the operation takes place entirely on the main thread in a synchronized fashion.
   * In this method, we post the operation to the main thread since we mutate the views and change
   * the visibility of certain views before and after taking the screenshot.
   *
   * @param activity to be captured as the screenshot and saved on the path provided.
   *
   * @param screenshotName is the name of the file that the screenshot will be saved under.
   * Usually, it's a pretty good idea to have this be pretty descriptive. By default, the name of
   * the screenshot will have the device and sdk information attached to it.
   *
   * @param screenshotNameSuffix is an optional param to add a suffix to the name of the screenshot file.
   *
   * @param viewIdsToExclude takes in set of ids to exclude from the screenshot by changing the
   * visibility to be invisible when the screenshot is taken and then turning it back to the
   * visibility that the view initially had.
   *
   * @param viewModifiers takes in a set of data modifiers which will be processed by the [viewDataProcessor].
   *
   * @param viewDataProcessor allows you to pass in a custom view modification processor.
   *
   * @param viewMutators allows you to mutate the subclasses of views in any particular way
   * (Hides scrollbars and cursors by default).
   *
   * @param screenshotDirectory allows you to specify where the screenshot taken should be saved in
   * the device. Note: On devices above API 29, saving directly to an external storage in not allowed.
   * So remember to pass in a valid path retrieved from the context as follows
   * {@code context.filesDir or context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}.
   *
   * @param screenshotFormat specifies the format of the screenshot file.
   *
   * @param screenshotQuality specifies the level of compression of the screenshot.
   *
   */
  @Synchronized
  fun takeScreenshot(
    activity: Activity,
    screenshotName: String,
    screenshotNameSuffix: String = "",
    viewIdsToExclude: Set<Int> = setOf(),
    viewModifiers: Set<DataModifier> = setOf(),
    viewDataProcessor: ViewDataProcessor = DefaultViewDataProcessor(),
    viewMutators: Set<ViewMutator> = setOf(ScrollbarHider, CursorHider),
    screenshotDirectory: String = "/sdcard/screenshots",
    screenshotFormat: ScreenshotFormat = PNG,
    screenshotQuality: ScreenshotQuality = BEST
  ) {
    val rootViews = getRootViewsFromActivity(activity)
    takeScreenshot(
      rootViews,
      screenshotName,
      screenshotNameSuffix,
      viewIdsToExclude,
      viewModifiers,
      viewDataProcessor,
      viewMutators,
      screenshotDirectory,
      screenshotFormat,
      screenshotQuality
    )
  }

  private fun getRootViewsFromActivity(activity: Activity): List<View> {
    val windowMgr = activity.windowManager!!
    val globalWindowManagerField = windowMgr.javaClass.getDeclaredField("mGlobal")
      .apply { isAccessible = true }
    val globalWindowManager = globalWindowManagerField.get(windowMgr)

    val viewsField = globalWindowManager.javaClass.getDeclaredField("mViews")
      .apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    return viewsField.get(globalWindowManager) as List<View>
  }

  /**
   * Takes a screenshot whenever the method is called from the test thread or the main thread.
   * Also note that the operation takes place entirely on the main thread in a synchronized fashion.
   * In this method, we post the operation to the main thread since we mutate the views and change
   * the visibility of certain views before and after taking the screenshot.
   *
   * @param view to be captured as the screenshot and saved on the path provided.
   *
   * @param screenshotFilename is the name of the file that the screenshot will be saved under.
   * Usually, it's a pretty good idea to have this be pretty descriptive. By default, the name of
   * the screenshot will have the device and sdk information attached to it.
   *
   * @param screenshotNameSuffix is an optional param to add a suffix to the name of the screenshot file.
   *
   * @param viewIdsToExclude takes in set of ids to exclude from the screenshot by changing the
   * visibility to be invisible when the screenshot is taken and then turning it back to the
   * visibility that the view initially had.
   *
   * @param viewModifiers takes in a set of data modifiers which will be processed by the [viewDataProcessor].
   *
   * @param viewDataProcessor allows you to pass in a custom view modification processor.
   *
   * @param viewMutators allows you to mutate the subclasses of views in any particular way
   * (Hides scrollbars and cursors by default).
   *
   * @param screenshotDirectory allows you to specify where the screenshot taken should be saved in
   * the device. Note: On devices above API 29, saving directly to an external storage in not allowed.
   * So remember to pass in a valid path retrieved from the context as follows
   * {@code context.filesDir or context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}.
   *
   * @param screenshotFormat specifies the format of the screenshot file.
   *
   * @param screenshotQuality specifies the level of compression of the screenshot.
   *
   */
  @Synchronized
  fun takeScreenshot(
    view: View,
    screenshotName: String,
    screenshotNameSuffix: String = "",
    viewIdsToExclude: Set<Int> = setOf(),
    viewModifiers: Set<DataModifier> = setOf(),
    viewDataProcessor: ViewDataProcessor = DefaultViewDataProcessor(),
    viewMutators: Set<ViewMutator> = setOf(ScrollbarHider, CursorHider),
    screenshotDirectory: String = "/sdcard/screenshots",
    screenshotFormat: ScreenshotFormat = PNG,
    screenshotQuality: ScreenshotQuality = BEST
  ) {
    takeScreenshot(
      listOf(view),
      screenshotName,
      screenshotNameSuffix,
      viewIdsToExclude,
      viewModifiers,
      viewDataProcessor,
      viewMutators,
      screenshotDirectory,
      screenshotFormat,
      screenshotQuality
    )
  }

  /**
   * Takes a screenshot whenever the method is called from the test thread or the main thread.
   * Also note that the operation takes place entirely on the main thread in a synchronized fashion.
   * In this method, we post the operation to the main thread since we mutate the views and change
   * the visibility of certain views before and after taking the screenshot.
   *
   * @param views to be captured as the screenshot and saved on the path provided.
   *
   * @param screenshotFilename is the name of the file that the screenshot will be saved under.
   * Usually, it's a pretty good idea to have this be pretty descriptive. By default, the name of
   * the screenshot will have the device and sdk information attached to it.
   *
   * @param screenshotNameSuffix is an optional param to add a suffix to the name of the screenshot file.
   *
   * @param viewIdsToExclude takes in set of ids to exclude from the screenshot by changing the
   * visibility to be invisible when the screenshot is taken and then turning it back to the
   * visibility that the view initially had.
   *
   * @param viewModifiers takes in a set of data modifiers which will be processed by the [viewDataProcessor].
   *
   * @param viewDataProcessor allows you to pass in a custom view modification processor.
   *
   * @param viewMutators allows you to mutate the subclasses of views in any particular way
   * (Hides scrollbars and cursors by default).
   *
   * @param screenshotDirectory allows you to specify where the screenshot taken should be saved in
   * the device. Note: On devices above API 29, saving directly to an external storage in not allowed.
   * So remember to pass in a valid path retrieved from the context as follows
   * {@code context.filesDir or context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}.
   *
   * @param screenshotFormat specifies the format of the screenshot file.
   *
   * @param screenshotQuality specifies the level of compression of the screenshot.
   *
   */
  @Synchronized
  fun takeScreenshot(
    views: List<View>,
    screenshotName: String,
    screenshotNameSuffix: String = "",
    viewIdsToExclude: Set<Int> = setOf(),
    viewModifiers: Set<DataModifier> = setOf(),
    viewDataProcessor: ViewDataProcessor = DefaultViewDataProcessor(),
    viewMutators: Set<ViewMutator> = setOf(ScrollbarHider, CursorHider),
    screenshotDirectory: String = "/sdcard/screenshots",
    screenshotFormat: ScreenshotFormat = PNG,
    screenshotQuality: ScreenshotQuality = BEST
  ) {
    views.forEach { view ->
      Log.d(SCREENSHOT, "Root view has height(${view.height}) and width(${view.width})")
      if (view.width == 0 || view.height == 0) {
        throw IllegalStateException("This view ($view) has no height or width. Is ${view.id} the currently displayed activity?")
      }
    }

    val deviceName = MANUFACTURER.replaceWithUnderscore() + "_" + MODEL.replaceWithUnderscore()
    val screenshotId = "${screenshotName.toLowerCase(ENGLISH)}_${deviceName}_${SDK_INT}_$screenshotNameSuffix"
    val screenshotFilePath = "$screenshotDirectory/$screenshotId.${screenshotFormat.extension}"
    if (!File(screenshotDirectory).exists()) {
      Log.d(SCREENSHOT, "Creating directory $screenshotDirectory since it does not exist")
      File(screenshotDirectory).mkdirs()
    }

    val bitmap = createCanvasBitmap(views)
    val canvas = Canvas(bitmap)

    val viewsToCapture = AtomicInteger(views.size)
    mainHandler.post {
      Log.d(SCREENSHOT, "Modifying view tree for '$screenshotName'")
      val initialStateOfViews = hashMapOf<View, ViewTreeState>()
      views.forEach { rootView ->
        val viewState = modifyViewBeforeScreenshot(
          rootView,
          viewDataProcessor,
          viewMutators,
          viewModifiers,
          viewIdsToExclude
        )
        initialStateOfViews[rootView] = viewState
      }

      views.forEach { rootView ->
        rootView.post {
          Log.d(SCREENSHOT, "Taking screenshot for '$screenshotName'")
          rootView.draw(canvas)

          if (viewsToCapture.decrementAndGet() == 0) {
            Log.d(SCREENSHOT, "Resetting view tree for '$screenshotName'")
            resetViewTreeAfterScreenshot(
              viewDataProcessor,
              views,
              initialStateOfViews,
              viewIdsToExclude
            )

            storeScreenshot(bitmap, screenshotFilePath, screenshotFormat, screenshotQuality)
          }
        }
      }
    }
  }

  private fun modifyViewBeforeScreenshot(
    view: View,
    viewDataProcessor: ViewDataProcessor,
    viewMutators: Set<ViewMutator>,
    viewModifiers: Set<DataModifier>,
    viewIdsToExclude: Set<Int>
  ): ViewTreeState {
    Log.d(SCREENSHOT, "Mutating views as needed inside $view")
    ViewTreeMutator.Builder()
      .viewMutators(viewMutators)
      .mutateView(view)
      .mutate()

    Log.d(SCREENSHOT, "Disabling views: $viewIdsToExclude")
    val initialVisibilityOfViews = ViewVisibilityModifier.hideViews(view, viewIdsToExclude)
    val initialDataOfViews = viewDataProcessor.modifyViews(view, viewModifiers)
    return ViewTreeState(initialVisibilityOfViews, initialDataOfViews)
  }

  private fun resetViewTreeAfterScreenshot(
    viewDataProcessor: ViewDataProcessor,
    views: List<View>,
    initialViewStates: Map<View, ViewTreeState>,
    viewIdsToExclude: Set<Int>
  ) {
    views.forEach { view ->
      Log.d(SCREENSHOT, "Enabling views: $viewIdsToExclude")
      val initialViewState = initialViewStates[view]!!
      ViewVisibilityModifier.showViews(view, viewIdsToExclude, initialViewState.viewVisibilityStates)
      viewDataProcessor.resetViews(view, initialViewState.viewDataStates)
    }
  }

  private fun storeScreenshot(
    bitmap: Bitmap,
    screenshotFilePath: String,
    screenshotFormat: ScreenshotFormat,
    screenshotQuality: ScreenshotQuality
  ) {
    Log.d(SCREENSHOT, "Writing to disk for '$screenshotFilePath'")
    bitmap.compress(
      screenshotFormat.compression,
      screenshotQuality.value,
      FileOutputStream(screenshotFilePath)
    )
    Log.d(SCREENSHOT, "Successfully wrote to disk for '$screenshotFilePath'")
  }

  private fun createCanvasBitmap(rootViews: List<View>): Bitmap {
    val canvasWidth = rootViews.maxBy { it.width }!!.width
    val canvasHeight = rootViews.maxBy { it.height }!!.height
    return Bitmap.createBitmap(canvasWidth, canvasHeight, ARGB_8888)
  }
}

private fun String.replaceWithUnderscore(): String {
  return toLowerCase(ENGLISH).replace(" ", "_")
}

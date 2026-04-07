package com.controlledinput

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.annotation.UiThread
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.UIManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * RN + Jetpack Compose hosting view aligned with Expo UI / [ExpoComposeView] + [ExpoView]:
 * - [shouldUseAndroidLayout]: requestLayout posts measureAndLayout (RN #17968)
 * - onMeasure skips child [ComposeView] until attached (window + WindowRecomposer)
 *
 * @see expo.modules.kotlin.views.ExpoComposeView
 * @see expo.modules.kotlin.views.ExpoView
 */
class ControlledInputView : LinearLayout, LifecycleOwner {
  constructor(context: Context) : super(context) {
    configureComponent(context)
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    configureComponent(context)
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    configureComponent(context)
  }

  private val lifecycleRegistry = LifecycleRegistry(this)
  override val lifecycle: Lifecycle get() = lifecycleRegistry

  internal lateinit var viewModel: JetpackComposeViewModel
  private val blurSignal = MutableStateFlow(0)
  private val focusSignal = MutableStateFlow(0)
  private lateinit var composeView: ComposeView
  private var usesLocalFallbackLifecycle = false
  private var windowLifecycleBound = false

  /** Same role as ExpoComposeView(withHostingView = true) → ExpoView.shouldUseAndroidLayout */
  private val shouldUseAndroidLayout = true

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    // ExpoComposeView.onMeasure — do not measure ComposeView until attached to a window.
    if (shouldUseAndroidLayout && !isAttachedToWindow) {
      setMeasuredDimension(
        MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(0),
        MeasureSpec.getSize(heightMeasureSpec).coerceAtLeast(0)
      )
      return
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  /**
   * ExpoView.requestLayout / measureAndLayout — Fabric/Yoga often won't drive Android layout
   * for native children; this mirrors expo-modules-core behavior.
   */
  override fun requestLayout() {
    super.requestLayout()
    if (shouldUseAndroidLayout) {
      post { measureAndLayout() }
    }
  }

  @UiThread
  private fun measureAndLayout() {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )
    layout(left, top, right, bottom)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    bindComposeToWindowLifecycle()
  }

  override fun onDetachedFromWindow() {
    if (usesLocalFallbackLifecycle) {
      lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    super.onDetachedFromWindow()
  }

  private fun bindComposeToWindowLifecycle() {
    if (windowLifecycleBound) {
      return
    }
    windowLifecycleBound = true

    val activity = (context as? ReactContext)?.currentActivity
    val activityOwner = activity as? LifecycleOwner
    if (activityOwner != null) {
      usesLocalFallbackLifecycle = false
      composeView.setViewTreeLifecycleOwner(activityOwner)
      val savedStateOwner = activity as? SavedStateRegistryOwner
      if (savedStateOwner != null) {
        composeView.setViewTreeSavedStateRegistryOwner(savedStateOwner)
      }
    } else {
      findViewTreeLifecycleOwnerFromAncestors()?.let { parentOwner ->
        usesLocalFallbackLifecycle = false
        composeView.setViewTreeLifecycleOwner(parentOwner)
      } ?: run {
        usesLocalFallbackLifecycle = true
        composeView.setViewTreeLifecycleOwner(this)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
      }
    }
  }

  private fun findViewTreeLifecycleOwnerFromAncestors(): LifecycleOwner? {
    var parent = this.parent as? View ?: return null
    while (true) {
      parent.findViewTreeLifecycleOwner()?.let {
        return it
      }
      parent = parent.parent as? View ?: return null
    }
  }

  fun blur() {
    blurSignal.value = blurSignal.value + 1
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
  }

  fun focus() {
    focusSignal.value = focusSignal.value + 1
  }

  private fun configureComponent(context: Context) {
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    clipChildren = false
    clipToPadding = false

    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT
    )

    viewModel = JetpackComposeViewModel()

    composeView = ComposeView(context).also { cv ->
      cv.layoutParams = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
      )
      cv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
      cv.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      cv.setContent {
        val value = viewModel.value.collectAsState().value
        val blurTick by blurSignal.collectAsState()
        val focusTick by focusSignal.collectAsState()
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(blurTick) {
          if (blurTick > 0) {
            focusManager.clearFocus(force = true)
          }
        }

        LaunchedEffect(focusTick) {
          if (focusTick > 0) {
            focusRequester.requestFocus()
          }
        }

        JetpackComposeView(
          value = value,
          inputStyle = viewModel.inputStyle,
          autoComplete = viewModel.autoComplete,
          placeholder = viewModel.placeholder,
          placeholderTextColor = viewModel.placeholderTextColor,
          selectionColor = viewModel.selectionColor,
          autoCapitalize = viewModel.autoCapitalize,
          keyboardType = viewModel.keyboardType,
          returnKeyType = viewModel.returnKeyType,
          onTextChange = { value ->
            val surfaceId = UIManagerHelper.getSurfaceId(context)
            val viewId = this@ControlledInputView.id
            UIManagerHelper
              .getEventDispatcherForReactTag(context as ReactContext, viewId)
              ?.dispatchEvent(
                TextChangeEvent(
                  surfaceId,
                  viewId,
                  value
                )
              )
          },
          onFocus = {
            val surfaceId = UIManagerHelper.getSurfaceId(context)
            val viewId = this@ControlledInputView.id
            UIManagerHelper
              .getEventDispatcherForReactTag(context as ReactContext, viewId)
              ?.dispatchEvent(
                FocusEvent(
                  surfaceId,
                  viewId
                )
              )
          },
          onBlur = {
            val surfaceId = UIManagerHelper.getSurfaceId(context)
            val viewId = this@ControlledInputView.id
            UIManagerHelper
              .getEventDispatcherForReactTag(context as ReactContext, viewId)
              ?.dispatchEvent(
                BlurEvent(
                  surfaceId,
                  viewId
                )
              )
          },
          focusRequester = focusRequester
        )
      }
      addView(cv)
    }
  }
}

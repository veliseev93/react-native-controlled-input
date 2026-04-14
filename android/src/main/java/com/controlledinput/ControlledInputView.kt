package com.controlledinput

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
import com.facebook.react.uimanager.events.Event
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * - [shouldUseAndroidLayout]: requestLayout posts measureAndLayout
 * - onMeasure skips child [ComposeView] until attached (window + WindowRecomposer)
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

  /**
   * Hidden [EditText] used only as [FocusedInputObserver.lastFocusedInput] from
   * https://github.com/kirillzyusko/react-native-keyboard-controller; [syncUpLayout] reads
   * [EditText]-scoped geometry. It is NOT focused and does not participate in the focus
   * chain — we push state via reflection + synthetic selection events instead.
   */
  private val kbcLayoutHost: EditText by lazy {
    EditText(context).also { v ->
      v.layoutParams = LayoutParams(0, 0)
      v.alpha = 0f
      v.isFocusable = false
      v.isFocusableInTouchMode = false
      v.showSoftInputOnFocus = false
      v.isClickable = false
      v.isCursorVisible = false
      v.isLongClickable = false
    }
  }

  /**
   * EdgeToEdgeViewRegistry → KeyboardAnimationCallback + FocusedInputObserver
   * (https://github.com/kirillzyusko/react-native-keyboard-controller).
   * Null if that library is missing or not initialized.
   */
  private fun resolveKbcCallbackAndObserver(): Pair<Any, Any>? {
    try {
      val registryClass =
        Class.forName("com.reactnativekeyboardcontroller.views.EdgeToEdgeViewRegistry")
      val registryInstance = registryClass.getField("INSTANCE").get(null)
      val edgeToEdgeView =
        registryClass.getDeclaredMethod("get").invoke(registryInstance) ?: return null

      val callbackField =
        edgeToEdgeView.javaClass.declaredFields.firstOrNull {
          it.type.simpleName == "KeyboardAnimationCallback"
        } ?: return null
      callbackField.isAccessible = true
      val callback = callbackField.get(edgeToEdgeView) ?: return null

      val observerField =
        callback.javaClass.declaredFields.firstOrNull {
          it.type.simpleName == "FocusedInputObserver"
        } ?: return null
      observerField.isAccessible = true
      val observer = observerField.get(callback) ?: return null
      return Pair(callback, observer)
    } catch (_: ClassNotFoundException) {
      return null
    } catch (_: Exception) {
      return null
    }
  }

  private fun setKbcViewTagFocused(callback: Any) {
    try {
      val f = callback.javaClass.getDeclaredField("viewTagFocused")
      f.isAccessible = true
      f.setInt(callback, id)
    } catch (_: Exception) {
    }
  }

  private fun setKbcFocusedInputHolder() {
    try {
      val holderClass =
        Class.forName("com.reactnativekeyboardcontroller.traversal.FocusedInputHolder")
      val instance = holderClass.getField("INSTANCE").get(null)
      holderClass
        .getMethod("set", EditText::class.java)
        .invoke(instance, kbcLayoutHost)
    } catch (_: Exception) {
    }
  }

  /**
   * `selection.end.y` for https://github.com/kirillzyusko/react-native-keyboard-controller / JS
   * customHeight: prefer explicit style height (dp, same as padding in [InputStyle]), else measured
   * view height in dp.
   */
  private fun approximateSelectionEndYDp(): Double {
    viewModel.inputStyle.value?.height?.takeIf { it > 0 }?.let { return it }
    val dm = resources.displayMetrics
    if (height > 0) {
      return (height / dm.density).toDouble()
    }
    return 12.0
  }

  private fun dispatchSyntheticKbcSelectionEvent(observer: Any) {
    val reactContext = context as? ReactContext ?: return
    try {
      val epField = observer.javaClass.getDeclaredField("eventPropagationView")
      epField.isAccessible = true
      val propagationId = (epField.get(observer) as View).id

      val surfaceId = UIManagerHelper.getSurfaceId(this)
      val targetId = id
      val endY = approximateSelectionEndYDp()

      val dataClz =
        Class.forName("com.reactnativekeyboardcontroller.events.FocusedInputSelectionChangedEventData")
      val dataCtor =
        dataClz.declaredConstructors.singleOrNull { it.parameterTypes.size == 7 } ?: return
      dataCtor.isAccessible = true
      val data =
        dataCtor.newInstance(targetId, 0.0, 0.0, 0.0, endY, 0, 0)

      val eventClz =
        Class.forName("com.reactnativekeyboardcontroller.events.FocusedInputSelectionChangedEvent")
      val eventCtor =
        eventClz.getConstructor(
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          dataClz,
        )
      val event = eventCtor.newInstance(surfaceId, propagationId, data) as Event<*>

      UIManagerHelper.getEventDispatcherForReactTag(reactContext, propagationId)
        ?.dispatchEvent(event)
    } catch (_: Exception) {
    }
  }

  /**
   * Pushes ControlledInput state into https://github.com/kirillzyusko/react-native-keyboard-controller
   * without stealing Compose focus: viewTagFocused, lastFocusedInput, FocusedInputHolder,
   * syncUpLayout, synthetic selection.
   */
  private fun syncKeyboardControllerFocusedInput() {
    kbcLayoutHost.id = id
    viewModel.inputStyle.value?.fontSize?.toFloat()?.let {
      kbcLayoutHost.setTextSize(TypedValue.COMPLEX_UNIT_SP, it)
    }

    val (callback, observer) = resolveKbcCallbackAndObserver() ?: return

    setKbcViewTagFocused(callback)
    setKbcFocusedInputHolder()

    try {
      val lastFocusedField = observer.javaClass.getDeclaredField("lastFocusedInput")
      lastFocusedField.isAccessible = true
      lastFocusedField.set(observer, kbcLayoutHost)

      val syncMethod = observer.javaClass.getDeclaredMethod("syncUpLayout")
      syncMethod.isAccessible = true
      syncMethod.invoke(observer)

      dispatchSyntheticKbcSelectionEvent(observer)
    } catch (_: Exception) {
    }
  }

  private val shouldUseAndroidLayout = true

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)
    kbcLayoutHost.layout(0, 0, width, height)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (shouldUseAndroidLayout && !isAttachedToWindow) {
      setMeasuredDimension(
        MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(0),
        MeasureSpec.getSize(heightMeasureSpec).coerceAtLeast(0)
      )
      return
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

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

    addView(kbcLayoutHost)

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
              ?.dispatchEvent(FocusEvent(surfaceId, viewId))
            post { syncKeyboardControllerFocusedInput() }
          },
          onBlur = {
            val surfaceId = UIManagerHelper.getSurfaceId(context)
            val viewId = this@ControlledInputView.id
            UIManagerHelper
              .getEventDispatcherForReactTag(context as ReactContext, viewId)
              ?.dispatchEvent(BlurEvent(surfaceId, viewId))
          },
          focusRequester = focusRequester
        )
      }
      addView(cv)
    }
  }
}

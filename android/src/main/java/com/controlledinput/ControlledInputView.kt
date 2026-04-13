package com.controlledinput

import android.content.Context
import android.util.AttributeSet
import android.util.Log
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
 *
 * @see expo.modules.kotlin.views.ExpoComposeView
 * @see expo.modules.kotlin.views.ExpoView
 */
class ControlledInputView : LinearLayout, LifecycleOwner {
  companion object {
    private const val TAG = "ControlledInputView"
  }
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
   * Hidden [EditText] used only as KBC's [FocusedInputObserver.lastFocusedInput]: [syncUpLayout]
   * reads [EditText]-scoped geometry. It is NOT focused and does not participate in the focus
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
   * EdgeToEdgeViewRegistry → KeyboardAnimationCallback + FocusedInputObserver.
   * Null if react-native-keyboard-controller is missing or not initialized.
   */
  private fun resolveKbcCallbackAndObserver(): Pair<Any, Any>? {
    try {
      val registryClass =
        Class.forName("com.reactnativekeyboardcontroller.views.EdgeToEdgeViewRegistry")
      val registryInstance = registryClass.getField("INSTANCE").get(null)
      val edgeToEdgeView =
        registryClass.getDeclaredMethod("get").invoke(registryInstance)
          ?: run {
            Log.w(TAG, "resolveKbcCallbackAndObserver: EdgeToEdgeViewRegistry.get() == null")
            return null
          }

      val callbackField =
        edgeToEdgeView.javaClass.declaredFields.firstOrNull {
          it.type.simpleName == "KeyboardAnimationCallback"
        }
          ?: run {
            Log.w(TAG, "resolveKbcCallbackAndObserver: KeyboardAnimationCallback field not found")
            return null
          }
      callbackField.isAccessible = true
      val callback =
        callbackField.get(edgeToEdgeView)
          ?: run {
            Log.w(TAG, "resolveKbcCallbackAndObserver: callback == null")
            return null
          }

      val observerField =
        callback.javaClass.declaredFields.firstOrNull {
          it.type.simpleName == "FocusedInputObserver"
        }
          ?: run {
            Log.w(TAG, "resolveKbcCallbackAndObserver: FocusedInputObserver field not found")
            return null
          }
      observerField.isAccessible = true
      val observer =
        observerField.get(callback)
          ?: run {
            Log.w(TAG, "resolveKbcCallbackAndObserver: layoutObserver == null")
            return null
          }
      return Pair(callback, observer)
    } catch (_: ClassNotFoundException) {
      Log.d(TAG, "resolveKbcCallbackAndObserver: keyboard-controller not on classpath")
      return null
    } catch (e: Exception) {
      Log.w(TAG, "resolveKbcCallbackAndObserver: ${e.javaClass.simpleName}: ${e.message}")
      return null
    }
  }

  private fun setKbcViewTagFocused(callback: Any) {
    try {
      val f = callback.javaClass.getDeclaredField("viewTagFocused")
      f.isAccessible = true
      f.setInt(callback, id)
      Log.d(TAG, "setKbcViewTagFocused: viewTagFocused=$id")
    } catch (e: Exception) {
      Log.w(TAG, "setKbcViewTagFocused: ${e.javaClass.simpleName}: ${e.message}")
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
    } catch (e: Exception) {
      Log.w(TAG, "setKbcFocusedInputHolder: ${e.javaClass.simpleName}: ${e.message}")
    }
  }

  /**
   * `selection.end.y` for KBC / JS customHeight: prefer explicit style height (dp, same as padding
   * in [InputStyle]), else measured view height in dp.
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
        dataClz.declaredConstructors.singleOrNull { it.parameterTypes.size == 7 }
          ?: run {
            Log.w(TAG, "dispatchSyntheticKbcSelectionEvent: no 7-arg data ctor")
            return
          }
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
      Log.d(
        TAG,
        "dispatchSyntheticKbcSelectionEvent: propagationId=$propagationId target=$targetId endY(dp)=$endY",
      )
    } catch (e: Exception) {
      Log.w(TAG, "dispatchSyntheticKbcSelectionEvent: ${e.javaClass.simpleName}: ${e.message}")
    }
  }

  /**
   * Pushes ControlledInput state into KBC without stealing Compose focus:
   * viewTagFocused, lastFocusedInput, FocusedInputHolder, syncUpLayout, synthetic selection.
   */
  private fun syncKeyboardControllerFocusedInput() {
    Log.d(TAG, "syncKeyboardControllerFocusedInput: id=$id")
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
      Log.d(TAG, "syncKeyboardControllerFocusedInput: syncUpLayout() ok")

      dispatchSyntheticKbcSelectionEvent(observer)
    } catch (e: Exception) {
      Log.w(TAG, "syncKeyboardControllerFocusedInput: ${e.javaClass.simpleName}: ${e.message}")
    }
  }

  private val shouldUseAndroidLayout = true

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)
    kbcLayoutHost.layout(0, 0, width, height)
    if (changed) {
      val loc = IntArray(2)
      getLocationOnScreen(loc)
      Log.d(TAG, "onLayout: view=${width}x${height} screenX=${loc[0]} screenY=${loc[1]}")
    }
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
    Log.d(TAG, "onAttachedToWindow: id=$id")
    bindComposeToWindowLifecycle()
  }

  override fun onDetachedFromWindow() {
    Log.d(TAG, "onDetachedFromWindow: id=$id")
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
    Log.d(TAG, "blur() called from JS ref")
    blurSignal.value = blurSignal.value + 1
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
  }

  fun focus() {
    Log.d(TAG, "focus() called from JS ref")
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
            Log.d(TAG, "Compose onFocus id=$id")
            val surfaceId = UIManagerHelper.getSurfaceId(context)
            val viewId = this@ControlledInputView.id
            UIManagerHelper
              .getEventDispatcherForReactTag(context as ReactContext, viewId)
              ?.dispatchEvent(FocusEvent(surfaceId, viewId))
            post { syncKeyboardControllerFocusedInput() }
          },
          onBlur = {
            Log.d(TAG, "Compose onBlur id=$id")
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

package com.controlledinput

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
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
   * True while we are in the focus-restoration dance:
   *   proxy stole Android focus from Compose → onBlur fired → we're restoring Compose focus.
   * During this window the second onFocus must NOT call requestFocusProxy() again.
   */
  private var isRestoringComposeFocus = false

  /**
   * Invisible EditText that acts as a focus proxy for react-native-keyboard-controller.
   *
   * keyboard-controller's FocusedInputObserver only tracks views that are `EditText` instances.
   * Since ControlledInputView uses Compose BasicTextField, it is invisible to that observer.
   * Focusing this proxy when Compose gains focus makes keyboard-controller aware of the input
   * and allows KeyboardAwareScrollView to scroll correctly.
   *
   * Layout height is 0 so LinearLayout ignores it visually. onLayout() forces its bounds to
   * match ControlledInputView so keyboard-controller reads the correct width/height/position.
   */
  private val focusProxy: EditText by lazy {
    EditText(context).also { proxy ->
      proxy.layoutParams = LayoutParams(0, 0)
      // Must stay VISIBLE — Android's canTakeFocus() returns false for INVISIBLE/GONE views,
      // causing requestFocus() to silently fail. Use alpha=0 to hide it visually instead.
      proxy.alpha = 0f
      proxy.isFocusableInTouchMode = true
      proxy.showSoftInputOnFocus = false
      proxy.isClickable = false
      proxy.isCursorVisible = false
      proxy.isLongClickable = false
    }
  }

  private fun requestFocusProxy() {
    val fontSize = viewModel.inputStyle.value?.fontSize?.toFloat()
    Log.d(TAG, "──── requestFocusProxy ────")
    Log.d(TAG, "  fontSize=$fontSize")
    Log.d(TAG, "  proxy before: isFocused=${focusProxy.isFocused} size=${focusProxy.width}x${focusProxy.height}")

    // Sync proxy ID with ControlledInputView's React Native tag BEFORE requestFocus(),
    // so KeyboardAnimationCallback.focusListener sets viewTagFocused = this.id (not -1).
    // Without this, KeyboardAwareScrollView JS sees e.target=-1 → focusWasChanged=false
    // → layout.value never updated → maybeScroll() always returns 0 → no scroll.
    focusProxy.id = this.id
    Log.d(TAG, "  proxy.id set to ${focusProxy.id} (= ControlledInputView RN tag)")

    // Sync textSize so KeyboardControllerSelectionWatcher computes the correct
    // cursor Y via android.text.Layout.getLineBottom() — used as `customHeight`
    // in KeyboardAwareScrollView to determine how far to scroll.
    fontSize?.let {
      focusProxy.setTextSize(TypedValue.COMPLEX_UNIT_SP, it)
      Log.d(TAG, "  proxy textSize set to ${focusProxy.textSize}px (${it}sp)")
    }

    focusProxy.requestFocus()
    Log.d(TAG, "  proxy after requestFocus: isFocused=${focusProxy.isFocused} hasFocus=${focusProxy.hasFocus()}")

    if (focusProxy.isFocused) {
      // Proxy stole Android focus from AndroidComposeView → Compose will fire onBlur.
      // Mark restoration mode so onBlur knows to recover (not dispatch BlurEvent to JS).
      isRestoringComposeFocus = true
      Log.d(TAG, "  isRestoringComposeFocus=true (proxy has Android focus)")
    }

    // setSelection triggers a selection change (lastSelectionStart starts at -1),
    // guaranteeing the watcher fires on the next pre-draw frame even if focus
    // was just transferred.
    focusProxy.setSelection(0)
    Log.d(TAG, "  proxy selectionStart=${focusProxy.selectionStart} layout=${focusProxy.layout != null}")

    // Log absolute screen position — this is what keyboard-controller reads for scroll calculation
    val loc = IntArray(2)
    focusProxy.getLocationOnScreen(loc)
    Log.d(TAG, "  proxy screenLocation x=${loc[0]} y=${loc[1]} → absoluteY for KBC=${loc[1]}px")
    Log.d(TAG, "──────────────────────────")
  }

  private fun clearFocusProxy() {
    Log.d(TAG, "clearFocusProxy: proxy.isFocused=${focusProxy.isFocused} hasFocus=${focusProxy.hasFocus()}")
    focusProxy.clearFocus()
    Log.d(TAG, "clearFocusProxy: after clearFocus isFocused=${focusProxy.isFocused}")
  }

  /**
   * Fires when the proxy loses Android focus (i.e. after composeView.requestFocus() restores
   * Compose). At this point KBC's listener has already cleared lastFocusedInput (KBC registered
   * its ViewTreeObserver listener before us → it fires first). We post restoreKbcTracking() to
   * run after all synchronous focus-change handlers complete.
   */
  private val proxyFocusLostListener =
    ViewTreeObserver.OnGlobalFocusChangeListener { oldFocus, newFocus ->
      // Fire for ANY proxy focus loss while we're in the restoration dance.
      // newFocus can be null (proxy→null→AndroidComposeView, two steps on first focus)
      // or AndroidComposeView directly (proxy→AndroidComposeView, subsequent focuses).
      // Both cases need restoreKbcTracking(); KBC's null→AndroidComposeView transition does
      // NOT clear lastFocusedInput, so restoring it once is enough for both paths.
      if (oldFocus == focusProxy && isRestoringComposeFocus) {
        Log.d(
          TAG,
          "proxyFocusLostListener: proxy → ${newFocus?.javaClass?.simpleName ?: "null"}, posting restoreKbcTracking()",
        )
        post { restoreKbcTracking() }
      }
    }

  /**
   * Uses reflection to restore KBC's lastFocusedInput = focusProxy and call syncUpLayout(),
   * so that KeyboardAwareScrollView receives the correct absoluteY / height to scroll to.
   *
   * Chain: EdgeToEdgeViewRegistry.get() → .callback → .layoutObserver → .lastFocusedInput / .syncUpLayout()
   */
  private fun restoreKbcTracking() {
    Log.d(TAG, "restoreKbcTracking: starting reflection chain")
    try {
      // 1. EdgeToEdgeViewRegistry is a Kotlin object — access via INSTANCE field
      val registryClass =
        Class.forName("com.reactnativekeyboardcontroller.views.EdgeToEdgeViewRegistry")
      val registryInstance = registryClass.getField("INSTANCE").get(null)
      val edgeToEdgeView =
        registryClass.getDeclaredMethod("get").invoke(registryInstance)
          ?: run {
            Log.w(TAG, "restoreKbcTracking: EdgeToEdgeViewRegistry.get() == null")
            return
          }

      // 2. callback: KeyboardAnimationCallback (internal var — find by type)
      val callbackField =
        edgeToEdgeView.javaClass.declaredFields.firstOrNull {
          it.type.simpleName == "KeyboardAnimationCallback"
        }
          ?: run {
            Log.w(TAG, "restoreKbcTracking: KeyboardAnimationCallback field not found")
            return
          }
      callbackField.isAccessible = true
      val callback =
        callbackField.get(edgeToEdgeView)
          ?: run {
            Log.w(TAG, "restoreKbcTracking: callback == null")
            return
          }

      // 3. layoutObserver: FocusedInputObserver (internal var — find by type)
      val observerField =
        callback.javaClass.declaredFields.firstOrNull {
          it.type.simpleName == "FocusedInputObserver"
        }
          ?: run {
            Log.w(TAG, "restoreKbcTracking: FocusedInputObserver field not found")
            return
          }
      observerField.isAccessible = true
      val observer =
        observerField.get(callback)
          ?: run {
            Log.w(TAG, "restoreKbcTracking: layoutObserver == null")
            return
          }

      // 4. Set lastFocusedInput = focusProxy (private var)
      val lastFocusedField = observer.javaClass.getDeclaredField("lastFocusedInput")
      lastFocusedField.isAccessible = true
      lastFocusedField.set(observer, focusProxy)
      Log.d(TAG, "restoreKbcTracking: lastFocusedInput set to focusProxy")

      // 5. Call syncUpLayout() — public fun, dispatches FocusedInputLayoutChangedEvent to JS
      val syncMethod = observer.javaClass.getDeclaredMethod("syncUpLayout")
      syncMethod.isAccessible = true
      syncMethod.invoke(observer)
      Log.d(TAG, "restoreKbcTracking: ✅ syncUpLayout() invoked — KBC layout event sent to JS")
    } catch (e: Exception) {
      Log.w(TAG, "restoreKbcTracking: ❌ ${e.javaClass.simpleName}: ${e.message}")
    }
  }

  private val shouldUseAndroidLayout = true

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)
    // Force proxy bounds to match ControlledInputView so keyboard-controller reads
    // the correct width/height/absolutePosition when the proxy is focused.
    focusProxy.layout(0, 0, width, height)
    if (changed) {
      val loc = IntArray(2)
      getLocationOnScreen(loc)
      Log.d(TAG, "onLayout: view=${width}x${height} screenX=${loc[0]} screenY=${loc[1]}")
      Log.d(TAG, "onLayout: proxy=${focusProxy.width}x${focusProxy.height} (should match view)")
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    // Do not measure ComposeView until attached to a window.
    if (shouldUseAndroidLayout && !isAttachedToWindow) {
      setMeasuredDimension(
        MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(0),
        MeasureSpec.getSize(heightMeasureSpec).coerceAtLeast(0)
      )
      return
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  // Fabric/Yoga often won't drive Android layout for native children.
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
    viewTreeObserver.addOnGlobalFocusChangeListener(proxyFocusLostListener)
    bindComposeToWindowLifecycle()
  }

  override fun onDetachedFromWindow() {
    Log.d(TAG, "onDetachedFromWindow: id=$id")
    viewTreeObserver.removeOnGlobalFocusChangeListener(proxyFocusLostListener)
    isRestoringComposeFocus = false
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
    isRestoringComposeFocus = false
    blurSignal.value = blurSignal.value + 1
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
    clearFocusProxy()
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

    addView(focusProxy)

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
            Log.d(TAG, "━━ Compose onFocus ━━ id=$id isRestoring=$isRestoringComposeFocus")
            if (isRestoringComposeFocus) {
              // Second onFocus triggered by focusRequester.requestFocus() inside the restoration
              // dance — KBC is being synced via reflection, keyboard is showing.
              // Do NOT dispatch FocusEvent again (already sent on first onFocus).
              // Do NOT call requestFocusProxy() again (would cause infinite loop).
              Log.d(TAG, "  → restoration onFocus: clearing flag, skipping proxy request")
              isRestoringComposeFocus = false
            } else {
              val surfaceId = UIManagerHelper.getSurfaceId(context)
              val viewId = this@ControlledInputView.id
              UIManagerHelper
                .getEventDispatcherForReactTag(context as ReactContext, viewId)
                ?.dispatchEvent(FocusEvent(surfaceId, viewId))
              requestFocusProxy()
            }
          },
          onBlur = {
            Log.d(TAG, "━━ Compose onBlur ━━ id=$id proxyFocused=${focusProxy.isFocused}")
            if (focusProxy.isFocused) {
              // Proxy stole Android focus from AndroidComposeView → this blur is synthetic.
              // Restore Compose focus so the keyboard stays/re-appears.
              // Do NOT dispatch BlurEvent to JS.
              Log.d(TAG, "  → proxy-caused blur: restoring Compose focus")
              post {
                // Give AndroidComposeView Android focus back (needed for showSoftInput to work)
                composeView.requestFocus()
                // Trigger BasicTextField to re-gain Compose focus → shows keyboard → fires onFocus
                focusSignal.value = focusSignal.value + 1
              }
            } else {
              // Real blur (user dismissed keyboard / tapped elsewhere)
              Log.d(TAG, "  → real blur: dispatching BlurEvent")
              isRestoringComposeFocus = false
              val surfaceId = UIManagerHelper.getSurfaceId(context)
              val viewId = this@ControlledInputView.id
              UIManagerHelper
                .getEventDispatcherForReactTag(context as ReactContext, viewId)
                ?.dispatchEvent(BlurEvent(surfaceId, viewId))
              clearFocusProxy()
            }
          },
          focusRequester = focusRequester
        )
      }
      addView(cv)
    }
  }
}

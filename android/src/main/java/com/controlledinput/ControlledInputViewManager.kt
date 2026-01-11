package com.controlledinput

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.ControlledInputViewManagerInterface
import com.facebook.react.viewmanagers.ControlledInputViewManagerDelegate

@ReactModule(name = ControlledInputViewManager.NAME)
class ControlledInputViewManager : SimpleViewManager<ControlledInputView>(),
  ControlledInputViewManagerInterface<ControlledInputView> {
  private val mDelegate: ViewManagerDelegate<ControlledInputView>

  init {
    mDelegate = ControlledInputViewManagerDelegate(this)
  }

  override fun getDelegate(): ViewManagerDelegate<ControlledInputView>? {
    return mDelegate
  }

  override fun getName(): String {
    return NAME
  }

  public override fun createViewInstance(context: ThemedReactContext): ControlledInputView {
    return ControlledInputView(context)
  }

  @ReactProp(name = "color")
  override fun setColor(view: ControlledInputView?, color: Int?) {
    view?.setBackgroundColor(color ?: Color.TRANSPARENT)
  }

  companion object {
    const val NAME = "ControlledInputView"
  }
}

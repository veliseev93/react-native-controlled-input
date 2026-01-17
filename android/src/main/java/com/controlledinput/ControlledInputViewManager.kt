package com.controlledinput

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.ControlledInputViewManagerInterface
import com.facebook.react.viewmanagers.ControlledInputViewManagerDelegate
import com.facebook.react.common.MapBuilder

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

  @ReactProp(name = "value")
  override fun setValue(view: ControlledInputView, value: String?) {
    value?.let {
      view.viewModel.setValue(value)
    }
  }

  @ReactProp(name = "inputStyle")
  override fun setInputStyle(view: ControlledInputView, inputStyle: ReadableMap?) {
    val style = if (inputStyle == null) {
      null
    } else {
      InputStyle(
        color = if (inputStyle.hasKey("color")) inputStyle.getString("color") else null,
        fontSize = if (inputStyle.hasKey("fontSize")) inputStyle.getDouble("fontSize") else null,
        height = if (inputStyle.hasKey("height")) inputStyle.getDouble("height") else null,
        paddingTop = if (inputStyle.hasKey("paddingTop")) inputStyle.getDouble("paddingTop") else null,
        paddingBottom = if (inputStyle.hasKey("paddingBottom")) inputStyle.getDouble("paddingBottom") else null,
        paddingLeft = if (inputStyle.hasKey("paddingLeft")) inputStyle.getDouble("paddingLeft") else null,
        paddingRight = if (inputStyle.hasKey("paddingRight")) inputStyle.getDouble("paddingRight") else null,
      )
    }
    view.viewModel.setInputStyle(style)
  }

  companion object {
    const val NAME = "ControlledInputView"
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> = mutableMapOf(
    TextChangeEvent.EVENT_NAME to MapBuilder.of("registrationName", "onTextChange"),
    FocusEvent.EVENT_NAME to MapBuilder.of("registrationName", "onFocus"),
    BlurEvent.EVENT_NAME to MapBuilder.of("registrationName", "onBlur")
  )
}

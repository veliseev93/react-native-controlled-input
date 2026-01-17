package com.controlledinput

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.UIManagerHelper

class ControlledInputView : LinearLayout {
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

  internal lateinit var viewModel: JetpackComposeViewModel

  private fun configureComponent(context: Context) {

    viewModel = JetpackComposeViewModel()

    layoutParams = LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    )

    ComposeView(context).also {
      it.layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
      )



      it.setContent {
        val value = viewModel.value.collectAsState().value
        JetpackComposeView(
          value = value,
          inputStyle = viewModel.inputStyle,
          onTextChange = { value ->
            val surfaceId = UIManagerHelper.getSurfaceId(context)
            val viewId = this.id
            UIManagerHelper
              .getEventDispatcherForReactTag(context as ReactContext, viewId)
              ?.dispatchEvent(
                TextChangeEvent(
                  surfaceId,
                  viewId,
                  value
                )
              )
          }
        )
      }
      addView(it)

    }
  }
}

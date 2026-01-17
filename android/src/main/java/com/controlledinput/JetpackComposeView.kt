package com.controlledinput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class InputStyle(
  val color: String? = null,
  val fontSize: Double? = null,
  val height: Double? = null,
  val paddingTop: Double? = null,
  val paddingBottom: Double? = null,
  val paddingLeft: Double? = null,
  val paddingRight: Double? = null,
)

@Composable
fun JetpackComposeView(
  value: String,
  inputStyle: StateFlow<InputStyle?>,
  onTextChange: (value: String) -> Unit
) {
  val state = remember { TextFieldState(value) }
  val style by inputStyle.collectAsState()

  if (state.text.toString() != value) {
    state.setTextAndPlaceCursorAtEnd(value)
  }

  val textColor = style?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.White
  val fontSize = style?.fontSize?.let { it.sp } ?: 24.sp
  val height = style?.height?.let { it.dp }
  val paddingValues = PaddingValues(
    start = style?.paddingLeft?.dp ?: 0.dp,
    top = style?.paddingTop?.dp ?: 0.dp,
    end = style?.paddingRight?.dp ?: 0.dp,
    bottom = style?.paddingBottom?.dp ?: 0.dp,
  )

  Column(modifier = Modifier.fillMaxSize()) {
    BasicTextField(
      state,
      inputTransformation = InputTransformation.byValue { _, proposed ->
        onTextChange(proposed.toString())
        proposed
      },
      modifier = Modifier
        .fillMaxWidth()
        .then(height?.let { Modifier.height(it) } ?: Modifier)
        .padding(paddingValues),
      textStyle = TextStyle(
        color = textColor,
        fontSize = fontSize,
      ),
    )
  }
}

class TextChangeEvent(
  surfaceId: Int,
  viewId: Int,
  val value: String,
) : Event<TextChangeEvent>(surfaceId, viewId) {
  override fun getEventName() = EVENT_NAME

  override fun getCoalescingKey(): Short = 0

  override fun getEventData(): WritableMap? = Arguments.createMap().also {

    it.putString("value", value)
  }

  companion object {
    const val EVENT_NAME = "onTextChange"
  }
}


class JetpackComposeViewModel : ViewModel() {
  private val _value = MutableStateFlow("")
  private val _inputStyle = MutableStateFlow<InputStyle?>(null)

  val value: StateFlow<String> get() = _value
  val inputStyle: StateFlow<InputStyle?> get() = _inputStyle

  fun setValue(newValue: String) {
    _value.value = newValue
  }

  fun setInputStyle(style: InputStyle?) {
    _inputStyle.value = style
  }
}

package com.controlledinput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
@Composable
fun JetpackComposeView(value: String, onTextChange: (value: String)  -> Unit) {
  val state = remember { TextFieldState(value) }

  if (state.text.toString() != value) {
    state.setTextAndPlaceCursorAtEnd(value)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    BasicTextField(
      state,
      inputTransformation = InputTransformation.byValue { _, proposed ->
        onTextChange(proposed.toString())
        proposed
      },
      modifier = Modifier
        .fillMaxWidth(),
      textStyle = TextStyle(
        color = Color.White,
        fontSize = 24.sp,
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

  val value: StateFlow<String> get() = _value

  fun setValue(newValue: String) {
    _value.value = newValue
  }
}

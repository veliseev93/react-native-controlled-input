package com.controlledinput

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
  val fontFamily: String? = null,
  val paddingTop: Double? = null,
  val paddingBottom: Double? = null,
  val paddingLeft: Double? = null,
  val paddingRight: Double? = null,
  val borderWidth: Double? = null,
  val borderRadius: Double? = null,
  val borderColor: String? = null,
  val backgroundColor: String? = null,
)

@Composable
fun JetpackComposeView(
  value: String,
  inputStyle: StateFlow<InputStyle?>,
  keyboardType: StateFlow<String?>,
  returnKeyType: StateFlow<String?>,
  onTextChange: (value: String) -> Unit,
  onFocus: (() -> Unit)? = null,
  onBlur: (() -> Unit)? = null,
  focusRequester: FocusRequester,
) {
  val state = remember { TextFieldState(value) }
  val style by inputStyle.collectAsState()
  val keyboardTypeValue by keyboardType.collectAsState()
  val returnKeyTypeValue by returnKeyType.collectAsState()
  val interactionSource = remember { MutableInteractionSource() }

  if (state.text.toString() != value) {
    state.setTextAndPlaceCursorAtEnd(value)
  }

  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      when (interaction) {
        is FocusInteraction.Focus -> {
          onFocus?.invoke()
        }
        is FocusInteraction.Unfocus -> {
          onBlur?.invoke()
        }
      }
    }
  }

  val context = LocalContext.current
  val textColor = style?.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.White
  val fontSize = style?.fontSize?.let { it.sp } ?: 24.sp
  val fontFamily = remember(style?.fontFamily) {
    style?.fontFamily?.let { name ->
      try {
        val typeface = com.facebook.react.views.text.ReactFontManager.getInstance()
          .getTypeface(name, Typeface.NORMAL, context.assets)
        typeface?.let { FontFamily(it) }
      } catch (_: Exception) {
        null
      }
    }
  }

  val paddingTop = style?.paddingTop?.dp ?: 0.dp
  val paddingBottom = style?.paddingBottom?.dp ?: 0.dp
  val paddingLeft = style?.paddingLeft?.dp ?: 0.dp
  val paddingRight = style?.paddingRight?.dp ?: 0.dp
  val borderWidth = style?.borderWidth?.dp ?: 0.dp
  val borderRadius = style?.borderRadius?.dp ?: 0.dp
  val borderColor = style?.borderColor
    ?.let { Color(android.graphics.Color.parseColor(it)) }
    ?: Color.Transparent
  val backgroundColor = style?.backgroundColor
    ?.let { Color(android.graphics.Color.parseColor(it)) }
    ?: Color.Transparent
  val shape = RoundedCornerShape(borderRadius)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .clip(shape)
      .background(backgroundColor)
      .border(borderWidth, borderColor, shape),
  ) {
    BasicTextField(
      state,
      inputTransformation = InputTransformation.byValue { _, proposed ->
        onTextChange(proposed.toString())
        proposed
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(
          start = paddingLeft,
          top = paddingTop,
          end = paddingRight,
          bottom = paddingBottom,
        )
        .focusRequester(focusRequester),
      textStyle = TextStyle(
        color = textColor,
        fontSize = fontSize,
        fontFamily = fontFamily,
      ),
      keyboardOptions = KeyboardOptions(
        keyboardType = toComposeKeyboardType(keyboardTypeValue),
        imeAction = toComposeImeAction(returnKeyTypeValue),
      ),
      interactionSource = interactionSource,
      decorator = { innerTextField ->
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.CenterStart,
        ) {
          innerTextField()
        }
      },
    )
  }
}

private fun toComposeKeyboardType(value: String?): KeyboardType = when (value) {
  "ascii-capable" -> KeyboardType.Ascii
  "numbers-and-punctuation" -> KeyboardType.Text
  "url" -> KeyboardType.Uri
  "number-pad", "numeric" -> KeyboardType.Number
  "phone-pad" -> KeyboardType.Phone
  "email-address" -> KeyboardType.Email
  "decimal-pad" -> KeyboardType.Decimal
  "visible-password" -> KeyboardType.Password
  else -> KeyboardType.Text
}

private fun toComposeImeAction(value: String?): ImeAction = when (value) {
  "go" -> ImeAction.Go
  "next" -> ImeAction.Next
  "search" -> ImeAction.Search
  "send" -> ImeAction.Send
  "done" -> ImeAction.Done
  "none" -> ImeAction.None
  "previous" -> ImeAction.Previous
  else -> ImeAction.Default
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

class FocusEvent(
  surfaceId: Int,
  viewId: Int,
) : Event<FocusEvent>(surfaceId, viewId) {
  override fun getEventName() = EVENT_NAME

  override fun getCoalescingKey(): Short = 0

  override fun getEventData(): WritableMap? = Arguments.createMap()

  companion object {
    const val EVENT_NAME = "onFocus"
  }
}

class BlurEvent(
  surfaceId: Int,
  viewId: Int,
) : Event<BlurEvent>(surfaceId, viewId) {
  override fun getEventName() = EVENT_NAME

  override fun getCoalescingKey(): Short = 0

  override fun getEventData(): WritableMap? = Arguments.createMap()

  companion object {
    const val EVENT_NAME = "onBlur"
  }
}


class JetpackComposeViewModel : ViewModel() {
  private val _value = MutableStateFlow("")
  private val _inputStyle = MutableStateFlow<InputStyle?>(null)
  private val _autoComplete = MutableStateFlow<String?>(null)
  private val _keyboardType = MutableStateFlow<String?>(null)
  private val _returnKeyType = MutableStateFlow<String?>(null)

  val value: StateFlow<String> get() = _value
  val inputStyle: StateFlow<InputStyle?> get() = _inputStyle
  val autoComplete: StateFlow<String?> get() = _autoComplete
  val keyboardType: StateFlow<String?> get() = _keyboardType
  val returnKeyType: StateFlow<String?> get() = _returnKeyType

  fun setValue(newValue: String) {
    _value.value = newValue
  }

  fun setInputStyle(style: InputStyle?) {
    _inputStyle.value = style
  }

  fun setAutoComplete(value: String?) {
    _autoComplete.value = value
  }

  fun setKeyboardType(value: String?) {
    _keyboardType.value = value
  }

  fun setReturnKeyType(value: String?) {
    _returnKeyType.value = value
  }
}

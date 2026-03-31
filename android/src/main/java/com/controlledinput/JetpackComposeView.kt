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
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.unit.sp
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event
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
  autoComplete: StateFlow<String?>,
  placeholder: StateFlow<String?>,
  placeholderTextColor: StateFlow<Int?>,
  selectionColor: StateFlow<Int?>,
  autoCapitalize: StateFlow<String?>,
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
  val autoCapitalizeValue by autoCapitalize.collectAsState()
  val returnKeyTypeValue by returnKeyType.collectAsState()
  val autoCompleteValue by autoComplete.collectAsState()
  val placeholderValue by placeholder.collectAsState()
  val placeholderTextColorValue by placeholderTextColor.collectAsState()
  val selectionColorValue by selectionColor.collectAsState()
  val interactionSource = remember { MutableInteractionSource() }

  val autofill = LocalAutofill.current
  val autofillNode = remember {
    AutofillNode(
      autofillTypes = toAutofillTypes(autoCompleteValue),
      onFill = { onTextChange(it) }
    )
  }
  LocalAutofillTree.current += autofillNode

  if (state.text.toString() != value) {
    state.setTextAndPlaceCursorAtEnd(value)
  }

  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      when (interaction) {
        is FocusInteraction.Focus -> {
          onFocus?.invoke()
          autofill?.requestAutofillForNode(autofillNode)
        }
        is FocusInteraction.Unfocus -> {
          onBlur?.invoke()
          autofill?.cancelAutofillForNode(autofillNode)
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

  val cursorColor = selectionColorValue?.let { Color(it) } ?: textColor
  val textSelectionColors = remember(cursorColor) {
    TextSelectionColors(
      handleColor = cursorColor,
      backgroundColor = cursorColor.copy(alpha = 0.4f)
    )
  }

  CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
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
          .onGloballyPositioned {
            autofillNode.boundingBox = it.boundsInWindow()
          }
          .focusRequester(focusRequester),
        textStyle = TextStyle(
          color = textColor,
          fontSize = fontSize,
          fontFamily = fontFamily,
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = toComposeCapitalization(autoCapitalizeValue),
          keyboardType = toComposeKeyboardType(keyboardTypeValue),
          imeAction = toComposeImeAction(returnKeyTypeValue),
        ),
        interactionSource = interactionSource,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(cursorColor),
        decorator = { innerTextField ->
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (state.text.isEmpty() && !placeholderValue.isNullOrEmpty()) {
              val finalPlaceholderColor = placeholderTextColorValue?.let { Color(it) }
                ?: textColor.copy(alpha = 0.5f)
              androidx.compose.material3.Text(
                text = placeholderValue!!,
                style = TextStyle(
                  color = finalPlaceholderColor,
                  fontSize = fontSize,
                  fontFamily = fontFamily,
                )
              )
            }
            innerTextField()
          }
        },
      )
    }
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

private fun toComposeCapitalization(value: String?): KeyboardCapitalization = when (value) {
  "none" -> KeyboardCapitalization.None
  "characters" -> KeyboardCapitalization.Characters
  "words" -> KeyboardCapitalization.Words
  "sentences" -> KeyboardCapitalization.Sentences
  else -> KeyboardCapitalization.Sentences
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

private fun toAutofillTypes(autoComplete: String?): List<AutofillType> = when (autoComplete) {
  "email" -> listOf(AutofillType.EmailAddress)
  "name", "given-name", "family-name", "additional-name" -> listOf(AutofillType.PersonFullName)
  "username" -> listOf(AutofillType.Username)
  "password", "new-password" -> listOf(AutofillType.Password)
  "tel" -> listOf(AutofillType.PhoneNumber)
  "postal-code" -> listOf(AutofillType.PostalCode)
  "street-address" -> listOf(AutofillType.AddressStreet)
  "cc-number" -> listOf(AutofillType.CreditCardNumber)
  "cc-exp" -> listOf(AutofillType.CreditCardExpirationDate)
  "cc-exp-month" -> listOf(AutofillType.CreditCardExpirationMonth)
  "cc-exp-year" -> listOf(AutofillType.CreditCardExpirationYear)
  "cc-csc" -> listOf(AutofillType.CreditCardSecurityCode)
  else -> emptyList()
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

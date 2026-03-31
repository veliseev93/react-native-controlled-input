package com.controlledinput

import android.os.Build
import android.view.View
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JetpackComposeViewModel : ViewModel() {
  private val _value = MutableStateFlow("")
  private val _inputStyle = MutableStateFlow<InputStyle?>(null)
  private val _autoComplete = MutableStateFlow<String?>(null)
  private val _placeholder = MutableStateFlow<String?>(null)
  private val _placeholderTextColor = MutableStateFlow<Int?>(null)
  private val _selectionColor = MutableStateFlow<Int?>(null)
  private val _autoCapitalize = MutableStateFlow<String?>(null)
  private val _keyboardType = MutableStateFlow<String?>(null)
  private val _returnKeyType = MutableStateFlow<String?>(null)

  val value: StateFlow<String> get() = _value
  val inputStyle: StateFlow<InputStyle?> get() = _inputStyle
  val autoComplete: StateFlow<String?> get() = _autoComplete
  val placeholder: StateFlow<String?> get() = _placeholder
  val placeholderTextColor: StateFlow<Int?> get() = _placeholderTextColor
  val selectionColor: StateFlow<Int?> get() = _selectionColor
  val autoCapitalize: StateFlow<String?> get() = _autoCapitalize
  val keyboardType: StateFlow<String?> get() = _keyboardType
  val returnKeyType: StateFlow<String?> get() = _returnKeyType

  fun setValue(newValue: String) {
    _value.value = newValue
  }

  fun setInputStyle(style: InputStyle?) {
    _inputStyle.value = style
  }

  fun setAutoComplete(newValue: String?) {
    _autoComplete.value = newValue
  }

  fun setPlaceholder(newValue: String?) {
    _placeholder.value = newValue
  }

  fun setPlaceholderTextColor(newValue: Int?) {
    _placeholderTextColor.value = newValue
  }

  fun setSelectionColor(newValue: Int?) {
    _selectionColor.value = newValue
  }

  fun setAutoCompleteWithAutofill(hostView: View, newValue: String?) {
    setAutoComplete(newValue)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }

    val hint = when (newValue) {
      "email" -> View.AUTOFILL_HINT_EMAIL_ADDRESS
      "name", "given-name", "family-name" -> View.AUTOFILL_HINT_NAME
      "username" -> View.AUTOFILL_HINT_USERNAME
      "password", "new-password" -> View.AUTOFILL_HINT_PASSWORD
      "tel" -> View.AUTOFILL_HINT_PHONE
      "postal-code" -> View.AUTOFILL_HINT_POSTAL_CODE
      "street-address" -> View.AUTOFILL_HINT_POSTAL_ADDRESS
      "cc-number" -> View.AUTOFILL_HINT_CREDIT_CARD_NUMBER
      "cc-exp" -> View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE
      "cc-exp-month" -> View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
      "cc-exp-year" -> View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
      "cc-csc" -> View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE
      "additional-name" -> View.AUTOFILL_HINT_NAME
      else -> null
    }

    if (hint == null || newValue == "off" || newValue.isNullOrEmpty()) {
      hostView.setAutofillHints(*emptyArray())
      return
    }

    hostView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    hostView.setAutofillHints(hint)
  }

  fun setAutoCapitalize(newValue: String?) {
    _autoCapitalize.value = newValue
  }

  fun setKeyboardType(newValue: String?) {
    _keyboardType.value = newValue
  }

  fun setReturnKeyType(newValue: String?) {
    _returnKeyType.value = newValue
  }
}

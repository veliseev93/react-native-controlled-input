# @ronas-it/react-native-controlled-input

A controlled React Native input that lets you format and constrain the value exactly how you want in JS, while keeping the displayed text in sync without invalid characters flashing in the field.

## Problem

With a regular controlled `TextInput`, native input is applied first, then JS receives the change, filters it, and sends the next `value` back.

That means invalid characters can still flash in the field for a moment.

`@ronas-it/react-native-controlled-input` is built for this exact case: you decide what text is valid, and the displayed value stays driven by `value`.

## Install

```sh
npm install @ronas-it/react-native-controlled-input
```

Requires React Native New Architecture / Fabric.

## Example

```tsx
import { useRef, useState } from 'react';
import { StyleSheet } from 'react-native';
import {
  ControlledInputView,
  type ControlledInputViewRef,
} from '@ronas-it/react-native-controlled-input';

export function Example() {
  const [value, setValue] = useState('');
  const inputRef = useRef<ControlledInputViewRef>(null);

  return (
    <ControlledInputView
      ref={inputRef}
      value={value}
      onTextChange={(text) => setValue(text.replace(/\d/g, ''))}
      style={styles.input}
      onFocus={() => {}}
      onBlur={() => {}}
    />
  );
}

const styles = StyleSheet.create({
  input: {
    height: 48,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    paddingHorizontal: 12,
    fontSize: 16,
    color: '#111',
  },
});
```

```tsx
inputRef.current?.focus();
inputRef.current?.blur();
```

## Props


| Prop                   | Type                      | Description                                                                                                                                   |
| ---------------------- | ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `value`                | `string`                  | Current input value.                                                                                                                          |
| `onTextChange`         | `(value: string) => void` | Called with the next text value. Filter it and update `value`.                                                                                |
| `onFocus`              | `() => void`              | Called when the text input is focused.                                                                                                        |
| `onBlur`               | `() => void`              | Called when the text input is blurred.                                                                                                        |
| `autoComplete`         | `string`                  | Specifies autocomplete hints for the system. Same as React Native `[TextInput](https://reactnative.dev/docs/textinput#autocomplete)`.         |
| `autoCapitalize`       | `string`                  | Can be `none`, `sentences`, `words`, `characters`. Same as React Native `[TextInput](https://reactnative.dev/docs/textinput#autocapitalize)`. |
| `keyboardType`         | `string`                  | Determines which keyboard to open, e.g. `numeric`. Same as React Native `[TextInput](https://reactnative.dev/docs/textinput#keyboardtype)`.   |
| `returnKeyType`        | `string`                  | Determines how the return key should look. Same as React Native `[TextInput](https://reactnative.dev/docs/textinput#returnkeytype)`.          |
| `placeholder`          | `string`                  | The string that will be rendered before text input has been entered.                                                                          |
| `placeholderTextColor` | `ColorValue`              | The text color of the placeholder string.                                                                                                     |
| `selectionColor`       | `ColorValue`              | The highlight and cursor color of the text input.                                                                                             |


## Style support

The same `style` API is supported on both iOS and Android.

Commonly used supported styles:

- `color`, `fontSize`, `fontFamily`
- `padding`, `paddingVertical`, `paddingHorizontal`
- `paddingTop`, `paddingBottom`, `paddingLeft`, `paddingRight`, `paddingStart`, `paddingEnd`
- `borderWidth`, `borderRadius`, `borderColor`, `backgroundColor`
- layout styles like `width`, `height`, `margin`, `flex`

Implementation differs internally between platforms, but usage is the same for library consumers.

## Fonts

In Expo projects, `**fontFamily` on this input only applies when the font is linked for native use**. Relying on runtime loading alone (`useFonts` / `loadAsync`) is often not enough here; use the **expo-font config plugin** so fonts are embedded at build time. See [Expo Font — Configuration in app config](https://docs.expo.dev/versions/latest/sdk/font/#configuration-in-app-config).

## Ref

- `focus()`
- `blur()`

## Troubleshooting

### `react-native-keyboard-controller`

If you use [react-native-keyboard-controller](https://github.com/kirillzyusko/react-native-keyboard-controller) with this package, apply the patch below that matches **your** installed library version so keyboard-aware scrolling and focused-input layout stay correct (especially on Android).

`1.20.7` (`react-native-keyboard-controller+1.20.7.patch`)

```diff
diff --git a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt
index ddd9b88..b8a851b 100644
--- a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt
+++ b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt
@@ -8,7 +8,6 @@ import android.view.Gravity
 import android.view.View
 import android.view.ViewTreeObserver.OnPreDrawListener
 import android.widget.EditText
-import com.facebook.react.views.scroll.ReactScrollView
 import com.facebook.react.views.textinput.ReactEditText
 import com.reactnativekeyboardcontroller.log.Logger
 import java.lang.reflect.Field
@@ -99,24 +98,7 @@ fun EditText.addOnTextChangedListener(action: (String) -> Unit): TextWatcher {
 }
 
 val EditText.parentScrollViewTarget: Int
-  get() {
-    var currentView: View? = this
-
-    while (currentView != null) {
-      val parentView = currentView.parent as? View
-
-      if (parentView is ReactScrollView && parentView.scrollEnabled) {
-        // If the parent is a vertical, scrollable ScrollView - return its id
-        return parentView.id
-      }
-
-      // Move to the next parent view
-      currentView = parentView
-    }
-
-    // ScrollView was not found
-    return -1
-  }
+  get() = keyboardParentScrollViewTarget()
 
 fun EditText?.focus() {
   if (this is ReactEditText) {
diff --git a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/ViewKeyboardScrollHost.kt b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/ViewKeyboardScrollHost.kt
new file mode 100644
index 0000000..75c1ba5
--- /dev/null
+++ b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/ViewKeyboardScrollHost.kt
@@ -0,0 +1,35 @@
+package com.reactnativekeyboardcontroller.extensions
+
+import android.view.View
+import com.facebook.react.views.scroll.ReactScrollView
+
+/**
+ * Nearest vertical [ReactScrollView] above this view (same strategy as [EditText.parentScrollViewTarget]).
+ */
+fun View.keyboardParentScrollViewTarget(): Int {
+  var current: View? = this
+  while (current != null) {
+    val parent = current.parent as? View ?: break
+    if (parent is ReactScrollView && parent.scrollEnabled) {
+      return parent.id
+    }
+    current = parent
+  }
+  return -1
+}
+
+/**
+ * react-native-controlled-input uses Compose [androidx.compose.foundation.text.BasicTextField] without a
+ * platform [android.widget.EditText], so [FocusedInputObserver] never sees `newFocus is EditText`.
+ * Resolve the RN view manager root to measure bounds and [keyboardParentScrollViewTarget].
+ */
+fun View?.findReactControlledInputHostOrNull(): View? {
+  var v: View? = this
+  while (v != null) {
+    if (v.javaClass.name == "com.controlledinput.ControlledInputView") {
+      return v
+    }
+    v = v.parent as? View
+  }
+  return null
+}
diff --git a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt
index 1e7be51..373444b 100644
--- a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt
+++ b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt
@@ -19,6 +19,8 @@ import com.reactnativekeyboardcontroller.extensions.addOnTextChangedListener
 import com.reactnativekeyboardcontroller.extensions.dispatchEvent
 import com.reactnativekeyboardcontroller.extensions.dp
 import com.reactnativekeyboardcontroller.extensions.emitEvent
+import com.reactnativekeyboardcontroller.extensions.findReactControlledInputHostOrNull
+import com.reactnativekeyboardcontroller.exteыnsions.keyboardParentScrollViewTarget
 import com.reactnativekeyboardcontroller.extensions.parentScrollViewTarget
 import com.reactnativekeyboardcontroller.extensions.rootView
 import com.reactnativekeyboardcontroller.extensions.screenLocation
@@ -47,6 +49,7 @@ class FocusedInputObserver(
 
   // state variables
   private var lastFocusedInput: EditText? = null
+  private var lastComposeHost: View? = null
   private var lastEventDispatched: FocusedInputLayoutChangedEventData = noFocusedInputEvent
   private var textWatcher: TextWatcher? = null
   private var selectionSubscription: (() -> Unit)? = null
@@ -101,6 +104,7 @@ class FocusedInputObserver(
       // unfocused or focus was changed
       if (newFocus == null || oldFocus != null) {
         lastFocusedInput?.removeOnLayoutChangeListener(layoutListener)
+        lastComposeHost?.removeOnLayoutChangeListener(layoutListener)
         lastFocusedInput?.let { input ->
           val watcher = textWatcher
           // remove it asynchronously to avoid crash in stripe input
@@ -111,6 +115,7 @@ class FocusedInputObserver(
         }
         selectionSubscription?.invoke()
         lastFocusedInput = null
+        lastComposeHost = null
       }
       if (newFocus is EditText) {
         lastFocusedInput = newFocus
@@ -130,6 +135,22 @@ class FocusedInputObserver(
             putInt("count", allInputFields.size)
           },
         )
+      } else {
+        val host = newFocus.findReactControlledInputHostOrNull()
+        if (host != null) {
+          lastComposeHost = host
+          host.addOnLayoutChangeListener(layoutListener)
+          this.syncUpLayout()
+
+          val allInputFields = ViewHierarchyNavigator.getAllInputFields(context?.rootView)
+          context.emitEvent(
+            "KeyboardController::focusDidSet",
+            Arguments.createMap().apply {
+              putInt("current", -1)
+              putInt("count", allInputFields.size)
+            },
+          )
+        }
       }
       // unfocused
       if (newFocus == null) {
@@ -142,19 +163,37 @@ class FocusedInputObserver(
   }
 
   fun syncUpLayout() {
-    val input = lastFocusedInput ?: return
+    val input = lastFocusedInput
+    if (input != null) {
+      val (x, y) = input.screenLocation
+      val event =
+        FocusedInputLayoutChangedEventData(
+          x = input.x.dp,
+          y = input.y.dp,
+          width = input.width.toFloat().dp,
+          height = input.height.toFloat().dp,
+          absoluteX = x.toFloat().dp,
+          absoluteY = y.toFloat().dp,
+          target = input.id,
+          parentScrollViewTarget = input.parentScrollViewTarget,
+        )
+
+      dispatchEventToJS(event)
+      return
+    }
 
-    val (x, y) = input.screenLocation
+    val host = lastComposeHost ?: return
+    val (x, y) = host.screenLocation
     val event =
       FocusedInputLayoutChangedEventData(
-        x = input.x.dp,
-        y = input.y.dp,
-        width = input.width.toFloat().dp,
-        height = input.height.toFloat().dp,
+        x = host.x.dp,
+        y = host.y.dp,
+        width = host.width.toFloat().dp,
+        height = host.height.toFloat().dp,
         absoluteX = x.toFloat().dp,
         absoluteY = y.toFloat().dp,
-        target = input.id,
-        parentScrollViewTarget = input.parentScrollViewTarget,
+        target = host.id,
+        parentScrollViewTarget = host.keyboardParentScrollViewTarget(),
       )
 
     dispatchEventToJS(event)
diff --git a/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx b/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx
index 4b21666..e35f03c 100644
--- a/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx
+++ b/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx
@@ -246,7 +246,15 @@ const KeyboardAwareScrollView = forwardRef<
 
       const customHeight = lastSelection.value?.selection.end.y;
 
-      if (!input.value?.layout || !customHeight) {
+      // Without selection geometry (e.g. Compose BasicTextField / controlled-input on Android)
+      // we still must mirror `input` into local `layout` so `maybeScroll` sees parentScrollViewTarget.
+      if (!input.value?.layout) {
+        layout.value = input.value;
+        return false;
+      }
+
+      if (!customHeight) {
+        layout.value = input.value;
         return false;
       }
 
@@ -351,25 +359,25 @@ const KeyboardAwareScrollView = forwardRef<
             keyboardWillChangeSize ||
             focusWasChanged
           ) {
-            // persist scroll value
-            scrollPosition.value = position.value;
-            // just persist height - later will be used in interpolation
-            keyboardHeight.value = e.height;
+            // Do not clobber scroll / keyboard height while hiding — `keyboardWillHide`
+            // already anchored `scrollPosition` to `scrollBeforeKeyboardMovement`. A bogus
+            // `focusWasChanged` would otherwise zero `keyboardHeight` / wrong scroll anchor,
+            // or run `maybeScroll(0)` and produce an up-then-down jump.
+            if (!keyboardWillHide) {
+              scrollPosition.value = position.value;
+              keyboardHeight.value = e.height;
+            }
           }
 
-          // focus was changed
           if (focusWasChanged) {
             tag.value = e.target;
-            // save position of focused text input when keyboard starts to move
-            updateLayoutFromSelection();
-            // save current scroll position - when keyboard will hide we'll reuse
-            // this value to achieve smooth hide effect
-            scrollBeforeKeyboardMovement.value = position.value;
+            if (!keyboardWillHide) {
+              updateLayoutFromSelection();
+              scrollBeforeKeyboardMovement.value = position.value;
+            }
           }
 
-          if (focusWasChanged && !keyboardWillAppear.value) {
-            // update position on scroll value, so `onEnd` handler
-            // will pick up correct values
+          if (focusWasChanged && !keyboardWillAppear.value && !keyboardWillHide) {
             position.value += maybeScroll(e.height, true);
           }
         },
```



`1.21.4` (`react-native-keyboard-controller+1.21.4.patch`)

```diff
diff --git a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt
index ddd9b880..b8a851b5 100644
--- a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt
+++ b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/EditText.kt
@@ -8,7 +8,6 @@ import android.view.Gravity
 import android.view.View
 import android.view.ViewTreeObserver.OnPreDrawListener
 import android.widget.EditText
-import com.facebook.react.views.scroll.ReactScrollView
 import com.facebook.react.views.textinput.ReactEditText
 import com.reactnativekeyboardcontroller.log.Logger
 import java.lang.reflect.Field
@@ -99,24 +98,7 @@ fun EditText.addOnTextChangedListener(action: (String) -> Unit): TextWatcher {
 }
 
 val EditText.parentScrollViewTarget: Int
-  get() {
-    var currentView: View? = this
-
-    while (currentView != null) {
-      val parentView = currentView.parent as? View
-
-      if (parentView is ReactScrollView && parentView.scrollEnabled) {
-        // If the parent is a vertical, scrollable ScrollView - return its id
-        return parentView.id
-      }
-
-      // Move to the next parent view
-      currentView = parentView
-    }
-
-    // ScrollView was not found
-    return -1
-  }
+  get() = keyboardParentScrollViewTarget()
 
 fun EditText?.focus() {
   if (this is ReactEditText) {
diff --git a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/ViewKeyboardScrollHost.kt b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/ViewKeyboardScrollHost.kt
new file mode 100644
index 00000000..75c1ba52
--- /dev/null
+++ b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/extensions/ViewKeyboardScrollHost.kt
@@ -0,0 +1,35 @@
+package com.reactnativekeyboardcontroller.extensions
+
+import android.view.View
+import com.facebook.react.views.scroll.ReactScrollView
+
+/**
+ * Nearest vertical [ReactScrollView] above this view (same strategy as [EditText.parentScrollViewTarget]).
+ */
+fun View.keyboardParentScrollViewTarget(): Int {
+  var current: View? = this
+  while (current != null) {
+    val parent = current.parent as? View ?: break
+    if (parent is ReactScrollView && parent.scrollEnabled) {
+      return parent.id
+    }
+    current = parent
+  }
+  return -1
+}
+
+/**
+ * react-native-controlled-input uses Compose [androidx.compose.foundation.text.BasicTextField] without a
+ * platform [android.widget.EditText], so [FocusedInputObserver] never sees `newFocus is EditText`.
+ * Resolve the RN view manager root to measure bounds and [keyboardParentScrollViewTarget].
+ */
+fun View?.findReactControlledInputHostOrNull(): View? {
+  var v: View? = this
+  while (v != null) {
+    if (v.javaClass.name == "com.controlledinput.ControlledInputView") {
+      return v
+    }
+    v = v.parent as? View
+  }
+  return null
+}
diff --git a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt
index 4de762da..dfb90a87 100644
--- a/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt
+++ b/node_modules/react-native-keyboard-controller/android/src/main/java/com/reactnativekeyboardcontroller/listeners/FocusedInputObserver.kt
@@ -19,6 +19,8 @@ import com.reactnativekeyboardcontroller.extensions.addOnTextChangedListener
 import com.reactnativekeyboardcontroller.extensions.dispatchEvent
 import com.reactnativekeyboardcontroller.extensions.dp
 import com.reactnativekeyboardcontroller.extensions.emitEvent
+import com.reactnativekeyboardcontroller.extensions.findReactControlledInputHostOrNull
+import com.reactnativekeyboardcontroller.extensions.keyboardParentScrollViewTarget
 import com.reactnativekeyboardcontroller.extensions.parentScrollViewTarget
 import com.reactnativekeyboardcontroller.extensions.rootView
 import com.reactnativekeyboardcontroller.extensions.screenLocation
@@ -47,6 +49,7 @@ class FocusedInputObserver(
 
   // state variables
   private var lastFocusedInput: EditText? = null
+  private var lastComposeHost: View? = null
   private var lastEventDispatched: FocusedInputLayoutChangedEventData = noFocusedInputEvent
   private var textWatcher: TextWatcher? = null
   private var selectionSubscription: (() -> Unit)? = null
@@ -101,6 +104,7 @@ class FocusedInputObserver(
       // unfocused or focus was changed
       if (newFocus == null || oldFocus != null) {
         lastFocusedInput?.removeOnLayoutChangeListener(layoutListener)
+        lastComposeHost?.removeOnLayoutChangeListener(layoutListener)
         lastFocusedInput?.let { input ->
           val watcher = textWatcher
           // remove it asynchronously to avoid crash in stripe input
@@ -111,6 +115,7 @@ class FocusedInputObserver(
         }
         selectionSubscription?.invoke()
         lastFocusedInput = null
+        lastComposeHost = null
       }
       if (newFocus is EditText) {
         lastFocusedInput = newFocus
@@ -131,6 +136,22 @@ class FocusedInputObserver(
             putInt("count", allInputFields.size)
           },
         )
+      } else {
+        val host = newFocus.findReactControlledInputHostOrNull()
+        if (host != null) {
+          lastComposeHost = host
+          host.addOnLayoutChangeListener(layoutListener)
+          this.syncUpLayout()
+
+          val allInputFields = ViewHierarchyNavigator.getAllInputFields(context?.rootView)
+          context.emitEvent(
+            "KeyboardController::focusDidSet",
+            Arguments.createMap().apply {
+              putInt("current", -1)
+              putInt("count", allInputFields.size)
+            },
+          )
+        }
       }
       // unfocused
       if (newFocus == null) {
@@ -143,19 +164,37 @@ class FocusedInputObserver(
   }
 
   fun syncUpLayout() {
-    val input = lastFocusedInput ?: return
+    val input = lastFocusedInput
+    if (input != null) {
+      val (x, y) = input.screenLocation
+      val event =
+        FocusedInputLayoutChangedEventData(
+          x = input.x.dp,
+          y = input.y.dp,
+          width = input.width.toFloat().dp,
+          height = input.height.toFloat().dp,
+          absoluteX = x.toFloat().dp,
+          absoluteY = y.toFloat().dp,
+          target = input.id,
+          parentScrollViewTarget = input.parentScrollViewTarget,
+        )
+
+      dispatchEventToJS(event)
+      return
+    }
 
-    val (x, y) = input.screenLocation
+    val host = lastComposeHost ?: return
+    val (x, y) = host.screenLocation
     val event =
       FocusedInputLayoutChangedEventData(
-        x = input.x.dp,
-        y = input.y.dp,
-        width = input.width.toFloat().dp,
-        height = input.height.toFloat().dp,
+        x = host.x.dp,
+        y = host.y.dp,
+        width = host.width.toFloat().dp,
+        height = host.height.toFloat().dp,
         absoluteX = x.toFloat().dp,
         absoluteY = y.toFloat().dp,
-        target = input.id,
-        parentScrollViewTarget = input.parentScrollViewTarget,
+        target = host.id,
+        parentScrollViewTarget = host.keyboardParentScrollViewTarget(),
       )
 
     dispatchEventToJS(event)
diff --git a/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx b/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx
index 90586e84..e506d284 100644
--- a/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx
+++ b/node_modules/react-native-keyboard-controller/src/components/KeyboardAwareScrollView/index.tsx
@@ -287,7 +287,15 @@ const KeyboardAwareScrollView = forwardRef<
 
       const customHeight = lastSelection.value?.selection.end.y;
 
-      if (!input.value?.layout || !customHeight) {
+      // Without selection geometry (e.g. Compose BasicTextField / controlled-input on Android)
+      // we still must mirror `input` into local `layout` so `maybeScroll` sees parentScrollViewTarget.
+      if (!input.value?.layout) {
+        layout.value = input.value;
+        return false;
+      }
+
+      if (!customHeight) {
+        layout.value = input.value;
         return false;
       }
 
@@ -410,10 +418,14 @@ const KeyboardAwareScrollView = forwardRef<
             keyboardWillChangeSize ||
             focusWasChanged
           ) {
-            // persist scroll value
-            scrollPosition.value = position.value;
-            // just persist height - later will be used in interpolation
-            keyboardHeight.value = e.height;
+            // Do not clobber scroll / keyboard height while hiding — `keyboardWillHide`
+            // already anchored `scrollPosition` to `scrollBeforeKeyboardMovement`. A bogus
+            // `focusWasChanged` would otherwise zero `keyboardHeight` / wrong scroll anchor,
+            // or run `maybeScroll(0)` and produce an up-then-down jump.
+            if (!keyboardWillHide) {
+              scrollPosition.value = position.value;
+              keyboardHeight.value = e.height;
+            }
 
             // insets mode: set the full contentInset upfront so that maybeScroll
             // calculations are correct from the very first onMove frame.
@@ -428,33 +440,35 @@ const KeyboardAwareScrollView = forwardRef<
           if (focusWasChanged) {
             tag.value = e.target;
 
-            if (
-              lastSelection.value?.target === e.target &&
-              selectionUpdatedSinceHide.value
-            ) {
-              // fresh selection arrived before onStart - use it to update layout
-              updateLayoutFromSelection();
-              pendingSelectionForFocus.value = false;
-            } else {
-              // selection hasn't arrived yet for the new target (iOS 15),
-              // or it's stale from previous session (Android refocus same input).
-              // Use stale selection as best-effort fallback if available for same target,
-              // otherwise fall back to full input layout.
-              // Will be corrected if a fresh onSelectionChange arrives.
-              if (lastSelection.value?.target === e.target) {
+            if (!keyboardWillHide) {
+              if (
+                lastSelection.value?.target === e.target &&
+                selectionUpdatedSinceHide.value
+              ) {
+                // fresh selection arrived before onStart - use it to update layout
                 updateLayoutFromSelection();
-              } else if (input.value) {
-                layout.value = input.value;
+                pendingSelectionForFocus.value = false;
+              } else {
+                // selection hasn't arrived yet for the new target (iOS 15),
+                // or it's stale from previous session (Android refocus same input).
+                // Use stale selection as best-effort fallback if available for same target,
+                // otherwise fall back to full input layout.
+                // Will be corrected if a fresh onSelectionChange arrives.
+                if (lastSelection.value?.target === e.target) {
+                  updateLayoutFromSelection();
+                } else if (input.value) {
+                  layout.value = input.value;
+                }
+                pendingSelectionForFocus.value = true;
               }
-              pendingSelectionForFocus.value = true;
-            }
 
-            // save current scroll position - when keyboard will hide we'll reuse
-            // this value to achieve smooth hide effect
-            scrollBeforeKeyboardMovement.value = position.value;
+              // save current scroll position - when keyboard will hide we'll reuse
+              // this value to achieve smooth hide effect
+              scrollBeforeKeyboardMovement.value = position.value;
+            }
           }
 
-          if (focusWasChanged && !keyboardWillAppear.value) {
+          if (focusWasChanged && !keyboardWillAppear.value && !keyboardWillHide) {
             if (!pendingSelectionForFocus.value) {
               // update position on scroll value, so `onEnd` handler
               // will pick up correct values
               position.value += maybeScroll(e.height, true);
             }
           }
```



## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
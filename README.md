# @ronas-it/react-native-controlled-input

A controlled React Native input that lets you format and constrain the value exactly how you want in JS, while keeping the displayed text in sync without invalid characters flashing in the field.

**`ControlledInputView`** (left) vs React Native **`TextInput`** (right), same JS formatting: with `TextInput`, rejected characters and intermediate states often flash until the filtered `value` is applied.

### Promo / invite code (ABCD-1234)

| ControlledInputView | TextInput |
| :-----------------: | :-------: |
| <img src="assets/promo-code-controlled-input.gif" alt="ControlledInputView promo code" width="325"> | <img src="./assets/promo-code-default-input.gif" alt="TextInput promo code" width="325"> |

### Card expiry (MM/YY)

| ControlledInputView | TextInput |
| :-----------------: | :-------: |
| <img src="assets/date-controlled-input.gif" alt="ControlledInputView date" width="325"> | <img src="./assets/date-default-input.gif" alt="TextInput date" width="325"> |

## Problem

With a regular controlled `TextInput`, native input is applied first, then JS receives the change, filters it, and sends the next `value` back.

That means invalid characters can still flash in the field for a moment.

`@ronas-it/react-native-controlled-input` is built for this exact case: you decide what text is valid, and the displayed value stays driven by `value`.


## Install

```sh
npm install @ronas-it/react-native-controlled-input
```

Requires React Native New Architecture / Fabric.

Compatible with [`react-native-keyboard-controller`](https://github.com/kirillzyusko/react-native-keyboard-controller).

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

| Prop | Type | Description |
|------|------|-------------|
| `value` | `string` | Current input value. |
| `onTextChange` | `(value: string) => void` | Called with the next text value. Filter it and update `value`. |
| `onFocus` | `() => void` | Called when the text input is focused. |
| `onBlur` | `() => void` | Called when the text input is blurred. |
| `autoComplete` | `string` | Specifies autocomplete hints for the system. Same as React Native [`TextInput`](https://reactnative.dev/docs/textinput#autocomplete). |
| `autoCapitalize` | `string` | Can be `none`, `sentences`, `words`, `characters`. Same as React Native [`TextInput`](https://reactnative.dev/docs/textinput#autocapitalize). |
| `keyboardType` | `string` | Determines which keyboard to open, e.g. `numeric`. Same as React Native [`TextInput`](https://reactnative.dev/docs/textinput#keyboardtype). |
| `returnKeyType` | `string` | Determines how the return key should look. Same as React Native [`TextInput`](https://reactnative.dev/docs/textinput#returnkeytype). |
| `placeholder` | `string` | The string that will be rendered before text input has been entered. |
| `placeholderTextColor` | `ColorValue` | The text color of the placeholder string. |
| `selectionColor` | `ColorValue` | The highlight and cursor color of the text input. |

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

In Expo projects, **`fontFamily` on this input only applies when the font is linked for native use**. Relying on runtime loading alone (`useFonts` / `loadAsync`) is often not enough here; use the **expo-font config plugin** so fonts are embedded at build time. See [Expo Font — Configuration in app config](https://docs.expo.dev/versions/latest/sdk/font/#configuration-in-app-config).

## Ref

- `focus()`
- `blur()`

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

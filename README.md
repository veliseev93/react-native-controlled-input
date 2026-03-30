# react-native-controlled-input

A controlled React Native input that lets you format and constrain the value exactly how you want in JS, while keeping the displayed text in sync without invalid characters flashing in the field.

## Problem

With a regular controlled `TextInput`, native input is applied first, then JS receives the change, filters it, and sends the next `value` back.

That means invalid characters can still flash in the field for a moment.

`react-native-controlled-input` is built for this exact case: you decide what text is valid, and the displayed value stays driven by `value`.

## Install

```sh
npm install react-native-controlled-input
```

Requires React Native New Architecture / Fabric.

## Example

```tsx
import { useRef, useState } from 'react';
import { StyleSheet } from 'react-native';
import {
  ControlledInputView,
  type ControlledInputViewRef,
} from 'react-native-controlled-input';

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
| `onFocus` | `() => void` | Called on focus. |
| `onBlur` | `() => void` | Called on blur. |
| `style` | `StyleProp<ViewStyle>` | Input styles. Same public API on iOS and Android, with platform-specific internal handling. |

## Style support

The same `style` API is supported on both iOS and Android.

Commonly used supported styles:

- `color`, `fontSize`, `fontFamily`
- `padding`, `paddingVertical`, `paddingHorizontal`
- `paddingTop`, `paddingBottom`, `paddingLeft`, `paddingRight`, `paddingStart`, `paddingEnd`
- `borderWidth`, `borderRadius`, `borderColor`, `backgroundColor`
- layout styles like `width`, `height`, `margin`, `flex`

Implementation differs internally between platforms, but usage is the same for library consumers.

## Ref

- `focus()`
- `blur()`

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

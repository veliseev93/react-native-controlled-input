import {
  forwardRef,
  useImperativeHandle,
  useRef,
  type ElementRef,
} from 'react';
import {
  Platform,
  processColor,
  StyleSheet,
  type ViewStyle,
} from 'react-native';
import ControlledInputViewNativeComponent, {
  Commands,
  type NativeProps,
} from './ControlledInputViewNativeComponent';

export interface ControlledInputViewRef {
  blur: () => void;
  focus: () => void;
}

export type ControlledInputViewProps = Omit<
  NativeProps,
  'inputStyle' | 'onTextChange'
> & {
  onTextChange?: (value: string) => void;
};

// All style props that Android handles via Compose instead of the native View layer
const ANDROID_HANDLED_KEYS = [
  'color',
  'fontSize',
  'fontFamily',
  'padding',
  'paddingVertical',
  'paddingHorizontal',
  'paddingTop',
  'paddingBottom',
  'paddingLeft',
  'paddingRight',
  'paddingStart',
  'paddingEnd',
  'borderWidth',
  'borderRadius',
  'borderColor',
  'backgroundColor',
];

function resolveAndroidPadding(flat: Record<string, any>) {
  const base = flat.padding ?? 0;
  return {
    paddingTop: flat.paddingTop ?? flat.paddingVertical ?? base,
    paddingBottom: flat.paddingBottom ?? flat.paddingVertical ?? base,
    paddingLeft:
      flat.paddingLeft ?? flat.paddingStart ?? flat.paddingHorizontal ?? base,
    paddingRight:
      flat.paddingRight ?? flat.paddingEnd ?? flat.paddingHorizontal ?? base,
  };
}

export const ControlledInputView = forwardRef<
  ControlledInputViewRef,
  ControlledInputViewProps
>(({ style, onTextChange, ...rest }, ref) => {
  const nativeRef =
    useRef<ElementRef<typeof ControlledInputViewNativeComponent>>(null);

  const flat = (StyleSheet.flatten(style) ?? {}) as Record<string, any>;

  let viewStyle: ViewStyle;
  let inputStyle: Record<string, any> | undefined;

  if (Platform.OS === 'android') {
    viewStyle = Object.fromEntries(
      Object.entries(flat).filter(([k]) => !ANDROID_HANDLED_KEYS.includes(k))
    ) as ViewStyle;

    const hasPadding = ANDROID_HANDLED_KEYS.slice(3, 12).some(
      (k) => flat[k] != null
    );

    inputStyle = {
      color: flat.color,
      fontSize: flat.fontSize,
      fontFamily: flat.fontFamily,
      ...(hasPadding ? resolveAndroidPadding(flat) : {}),
      borderWidth: flat.borderWidth,
      borderRadius: flat.borderRadius,
      borderColor: flat.borderColor,
      backgroundColor: flat.backgroundColor,
    };
  } else {
    const { color, fontSize, fontFamily, ...iosRest } = flat;
    viewStyle = iosRest as ViewStyle;

    const hasTextStyle =
      color != null || fontSize != null || fontFamily != null;
    inputStyle = hasTextStyle
      ? {
          color: color != null ? processColor(color) : undefined,
          fontSize,
          fontFamily,
        }
      : undefined;
  }

  useImperativeHandle(ref, () => ({
    blur: () => {
      if (!nativeRef.current) return;
      if (Platform.OS === 'ios' || Platform.OS === 'android') {
        Commands.blur(nativeRef.current);
      }
    },
    focus: () => {
      if (!nativeRef.current) return;
      if (Platform.OS === 'ios' || Platform.OS === 'android') {
        Commands.focus(nativeRef.current);
      }
    },
  }));

  return (
    <ControlledInputViewNativeComponent
      {...rest}
      style={viewStyle}
      inputStyle={inputStyle as NativeProps['inputStyle']}
      onTextChange={
        onTextChange ? (e) => onTextChange(e.nativeEvent.value) : undefined
      }
      ref={nativeRef as any}
    />
  );
});

export * from './ControlledInputViewNativeComponent';

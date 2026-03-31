import {
  forwardRef,
  memo,
  useImperativeHandle,
  useRef,
  type ElementRef,
} from 'react';
import {
  Platform,
  processColor,
  StyleSheet,
  type TextInputProps,
  type ViewStyle,
  type TextStyle,
} from 'react-native';
import ControlledInputViewNativeComponent, {
  Commands,
  type NativeProps,
  type TextChangeEvent,
} from './ControlledInputViewNativeComponent';

export interface ControlledInputViewRef {
  blur: () => void;
  focus: () => void;
}

type ForwardedTextInputProps = Pick<
  TextInputProps,
  'autoComplete' | 'autoCapitalize' | 'keyboardType' | 'returnKeyType'
>;

export type ControlledInputViewProps = Omit<
  NativeProps,
  'inputStyle' | 'onTextChange' | keyof ForwardedTextInputProps
> &
  ForwardedTextInputProps & {
    onTextChange?: (value: string) => void;
  };

// All style props that Android handles via Compose instead of the native View layer
const androidComposeHandledKeys = [
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
  'borderWidth',
  'borderRadius',
  'borderColor',
  'backgroundColor',
];

function resolveAndroidComposeViewPadding(flat: Record<string, any>) {
  const padding = flat.padding ?? 0;

  return {
    paddingTop: flat.paddingTop ?? flat.paddingVertical ?? padding,
    paddingBottom: flat.paddingBottom ?? flat.paddingVertical ?? padding,
    paddingLeft: flat.paddingLeft ?? flat.paddingHorizontal ?? padding,
    paddingRight: flat.paddingRight ?? flat.paddingHorizontal ?? padding,
  };
}

export const ControlledInputView = memo(
  forwardRef<ControlledInputViewRef, ControlledInputViewProps>(
    ({ style, onTextChange, ...rest }, ref) => {
      const nativeRef =
        useRef<ElementRef<typeof ControlledInputViewNativeComponent>>(null);

      const flattenedStyle = (StyleSheet.flatten(style) ?? {}) as TextStyle;

      let viewStyle: ViewStyle;
      let inputStyle: Record<string, any> | undefined;

      if (Platform.OS === 'android') {
        viewStyle = Object.fromEntries(
          Object.entries(flattenedStyle).filter(
            ([k]) => !androidComposeHandledKeys.includes(k)
          )
        );

        const hasPadding = Object.entries(flattenedStyle).some(
          ([k, v]) => k.includes('padding') && v != null
        );

        inputStyle = {
          color: flattenedStyle.color,
          fontSize: flattenedStyle.fontSize,
          fontFamily: flattenedStyle.fontFamily,
          ...(hasPadding
            ? resolveAndroidComposeViewPadding(flattenedStyle)
            : {}),
          borderWidth: flattenedStyle.borderWidth,
          borderRadius: flattenedStyle.borderRadius,
          borderColor: flattenedStyle.borderColor,
          backgroundColor: flattenedStyle.backgroundColor,
        };
      } else {
        const { color, fontSize, fontFamily, ...iosViewStyle } = flattenedStyle;
        viewStyle = iosViewStyle;

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

      const handleTextChange = (e: {
        nativeEvent: Readonly<TextChangeEvent>;
      }) => {
        if (onTextChange) {
          onTextChange(e.nativeEvent.value);
        }
      };

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
          inputStyle={inputStyle}
          onTextChange={handleTextChange}
          ref={nativeRef}
        />
      );
    }
  )
);

ControlledInputView.displayName = 'ControlledInputView';

export * from './ControlledInputViewNativeComponent';

import {
  forwardRef,
  useImperativeHandle,
  useRef,
  type ElementRef,
} from 'react';
import { Platform, processColor } from 'react-native';
import ControlledInputViewNativeComponent, {
  Commands,
  type NativeProps,
} from './ControlledInputViewNativeComponent';

export interface ControlledInputViewRef {
  blur: () => void;
  focus: () => void;
}

export const ControlledInputView = forwardRef<
  ControlledInputViewRef,
  NativeProps
>((props, ref) => {
  const nativeRef =
    useRef<ElementRef<typeof ControlledInputViewNativeComponent>>(null);
  const nativeProps =
    Platform.OS === 'ios' && props.inputStyle
      ? {
          ...props,
          inputStyle: {
            ...props.inputStyle,
            color:
              props.inputStyle.color == null
                ? props.inputStyle.color
                : processColor(props.inputStyle.color),
            borderColor:
              props.inputStyle.borderColor == null
                ? props.inputStyle.borderColor
                : processColor(props.inputStyle.borderColor),
          },
        }
      : props;

  useImperativeHandle(ref, () => ({
    blur: () => {
      if (!nativeRef.current) {
        return;
      }

      if (Platform.OS === 'ios' || Platform.OS === 'android') {
        console.log(
          `[ControlledInputView] ${Platform.OS} blur command -> native`
        );
        Commands.blur(nativeRef.current);
      }
    },
    focus: () => {
      if (!nativeRef.current) {
        return;
      }

      if (Platform.OS === 'ios' || Platform.OS === 'android') {
        console.log(
          `[ControlledInputView] ${Platform.OS} focus command -> native`
        );
        Commands.focus(nativeRef.current);
      }
    },
  }));

  return (
    <ControlledInputViewNativeComponent
      {...nativeProps}
      ref={nativeRef as any}
    />
  );
});

export * from './ControlledInputViewNativeComponent';

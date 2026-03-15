import {
  forwardRef,
  useImperativeHandle,
  useRef,
  type ElementRef,
} from 'react';
import { Platform } from 'react-native';
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
    <ControlledInputViewNativeComponent {...props} ref={nativeRef as any} />
  );
});

export * from './ControlledInputViewNativeComponent';

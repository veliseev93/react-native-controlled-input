import {
  forwardRef,
  useImperativeHandle,
  useRef,
  type ComponentProps,
  type ElementRef,
} from 'react';
import { findNodeHandle, UIManager, Platform } from 'react-native';
import ControlledInputViewNativeComponent, {
  Commands,
} from './ControlledInputViewNativeComponent';

type NativeProps = ComponentProps<typeof ControlledInputViewNativeComponent>;

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
      if (Platform.OS === 'ios') {
        if (nativeRef.current) {
          console.log('[ControlledInputView] iOS blur command -> native');
          Commands.blur(nativeRef.current);
        }
        return;
      }

      const tag = findNodeHandle(nativeRef.current);
      if (!tag) {
        return;
      }

      if (Platform.OS === 'android') {
        const config = UIManager.getViewManagerConfig(
          'ControlledInputView'
        ) as any;
        const command = config?.Commands?.blur;
        if (command != null) {
          UIManager.dispatchViewManagerCommand(tag, command, []);
        }
      }
    },
    focus: () => {
      if (Platform.OS === 'ios') {
        if (nativeRef.current) {
          console.log('[ControlledInputView] iOS focus command -> native');
          Commands.focus(nativeRef.current);
        }
        return;
      }

      const tag = findNodeHandle(nativeRef.current);
      if (!tag) {
        return;
      }

      if (Platform.OS === 'android') {
        const config = UIManager.getViewManagerConfig(
          'ControlledInputView'
        ) as any;
        const command = config?.Commands?.focus;
        if (command != null) {
          UIManager.dispatchViewManagerCommand(tag, command, []);
        }
      }
    },
  }));

  return (
    <ControlledInputViewNativeComponent {...props} ref={nativeRef as any} />
  );
});

export * from './ControlledInputViewNativeComponent';

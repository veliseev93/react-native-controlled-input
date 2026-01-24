import {
  forwardRef,
  useImperativeHandle,
  useRef,
  type ComponentProps,
} from 'react';
import {
  findNodeHandle,
  UIManager,
  Platform,
  type HostComponent,
} from 'react-native';
import ControlledInputViewNativeComponent from './ControlledInputViewNativeComponent';

type NativeProps = ComponentProps<typeof ControlledInputViewNativeComponent>;

export interface ControlledInputViewRef {
  blur: () => void;
}

export const ControlledInputView = forwardRef<
  ControlledInputViewRef,
  NativeProps
>((props, ref) => {
  const nativeRef = useRef<HostComponent<NativeProps>>(null);

  useImperativeHandle(ref, () => ({
    blur: () => {
      const tag = findNodeHandle(nativeRef.current);
      if (!tag) return;

      if (Platform.OS === 'android' || Platform.OS === 'ios') {
        const config = UIManager.getViewManagerConfig(
          'ControlledInputView'
        ) as any;
        UIManager.dispatchViewManagerCommand(tag, config.Commands.blur, []);
      }
    },
  }));

  return (
    <ControlledInputViewNativeComponent {...props} ref={nativeRef as any} />
  );
});

export * from './ControlledInputViewNativeComponent';

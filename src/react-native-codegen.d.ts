declare module 'react-native/Libraries/Types/CodegenTypes' {
  export type Double = number;
  export type Float = number;
  export type Int32 = number;
  export type UnsafeObject = object;

  export interface BubblingEventHandler<T> {
    (event: { nativeEvent: T }): void;
  }

  export interface DirectEventHandler<T> {
    (event: { nativeEvent: T }): void;
  }
}

declare module 'react-native/Libraries/Utilities/codegenNativeComponent' {
  import type { HostComponent } from 'react-native';
  export default function codegenNativeComponent<T>(
    componentName: string
  ): HostComponent<T>;
}

declare module 'react-native/Libraries/Components/TextInput/TextInputState' {
  import type { HostInstance } from 'react-native';

  const TextInputState: {
    registerInput(textField: HostInstance): void;
    unregisterInput(textField: HostInstance): void;
    focusInput(textField: HostInstance | null): void;
    blurInput(textField: HostInstance | null): void;
    focusTextInput(textField: HostInstance | null): void;
    blurTextInput(textField: HostInstance | null): void;
    currentlyFocusedInput(): HostInstance | null;
  };

  export default TextInputState;
}

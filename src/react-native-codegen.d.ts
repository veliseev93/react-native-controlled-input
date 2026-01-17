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

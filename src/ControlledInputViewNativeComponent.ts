import {
  codegenNativeComponent,
  type ViewProps,
  type ColorValue,
  type HostComponent,
} from 'react-native';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
import type {
  BubblingEventHandler,
  Double,
} from 'react-native/Libraries/Types/CodegenTypes';

interface TextChangeEvent {
  value: string;
}

interface FocusEvent {
  // Empty event
}

interface BlurEvent {
  // Empty event
}

interface InputStyle {
  color?: ColorValue;
  fontSize?: Double;
  fontFamily?: string;
  height?: Double;
  paddingTop?: Double;
  paddingBottom?: Double;
  paddingLeft?: Double;
  paddingRight?: Double;
  borderWidth?: Double;
  borderRadius?: Double;
  borderColor?: ColorValue;
}

interface NativeProps extends ViewProps {
  value?: string;
  inputStyle?: InputStyle;
  onTextChange?: BubblingEventHandler<Readonly<TextChangeEvent>>;
  onFocus?: BubblingEventHandler<Readonly<FocusEvent>>;
  onBlur?: BubblingEventHandler<Readonly<BlurEvent>>;
}

export interface NativeCommands {
  focus: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  blur: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['focus', 'blur'],
});

export default codegenNativeComponent<NativeProps>('ControlledInputView');

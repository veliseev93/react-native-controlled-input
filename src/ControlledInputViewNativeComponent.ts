import { codegenNativeComponent, type ViewProps } from 'react-native';
import type {
  BubblingEventHandler,
  Double,
} from 'react-native/Libraries/Types/CodegenTypes';

interface TextChangeEvent {
  value: string;
}

interface InputStyle {
  color?: string;
  fontSize?: Double;
  height?: Double;
  paddingTop?: Double;
  paddingBottom?: Double;
  paddingLeft?: Double;
  paddingRight?: Double;
}

interface NativeProps extends ViewProps {
  value?: string;
  inputStyle?: InputStyle;
  onTextChange?: BubblingEventHandler<Readonly<TextChangeEvent>>;
}

export default codegenNativeComponent<NativeProps>('ControlledInputView');

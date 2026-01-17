import { codegenNativeComponent, type ViewProps } from 'react-native';
import type { BubblingEventHandler } from 'react-native/Libraries/Types/CodegenTypes';

interface TextChangeEvent {
  value: string;
}
interface NativeProps extends ViewProps {
  value?: string;
  onTextChange?: BubblingEventHandler<Readonly<TextChangeEvent>>;
}

export default codegenNativeComponent<NativeProps>('ControlledInputView');

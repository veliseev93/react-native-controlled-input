import { useRef, useState, type ReactElement } from 'react';
import { StyleSheet, ScrollView, Button } from 'react-native';
import {
  ControlledInputView,
  type ControlledInputViewRef,
} from 'react-native-controlled-input';

export default function App(): ReactElement {
  const [value, setValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const inputRef = useRef<ControlledInputViewRef>(null);

  const handleValueChange = (text: string): void => {
    setValue(text.replace(/\d/g, ''));
  };

  const handleFocus = (): void => {
    setIsFocused(true);
  };

  const handleBlur = (): void => {
    setIsFocused(false);
  };

  const focus = (): void => {
    inputRef.current?.focus();
  };

  const blur = (): void => {
    inputRef.current?.blur();
  };

  return (
    <ScrollView
      contentContainerStyle={styles.container}
      keyboardShouldPersistTaps='handled'
    >
      <ControlledInputView
        value={value}
        ref={inputRef}
        placeholder='Type something...'
        onTextChange={handleValueChange}
        style={[styles.input, isFocused && styles.focusedInput]}
        onFocus={handleFocus}
        onBlur={handleBlur}
      />
      <Button title='Focus' onPress={focus} />
      <Button title='Blur' onPress={blur} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    flex: 1,
    gap: 20,
  },
  input: {
    height: 48,
    marginHorizontal: 20,
    borderWidth: 1,
    borderColor: '#F7F8FA',
    borderRadius: 8,
    fontSize: 16,
    backgroundColor: '#F7F8FA',
    paddingHorizontal: 10,
    color: '#000000',
    fontFamily: 'AlbertSans-Regular',
  },
  focusedInput: {
    borderColor: '#167BF1',
  },
});

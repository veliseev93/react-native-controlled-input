import { BottomSheetModalProvider } from '@gorhom/bottom-sheet';
import { useRef, useState, type ReactElement } from 'react';
import { StyleSheet, View, Text, Button, TextInput } from 'react-native';
import {
  ControlledInputView,
  type ControlledInputViewRef,
} from 'react-native-controlled-input';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import {
  KeyboardAwareScrollView,
  KeyboardProvider,
} from 'react-native-keyboard-controller';
import { SheetExample } from './SheetExample';

function InputScreen(): ReactElement {
  const [value, setValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const inputRef = useRef<ControlledInputViewRef>(null);

  return (
    <KeyboardAwareScrollView
      contentContainerStyle={styles.container}
      keyboardShouldPersistTaps='handled'
      mode='layout'
    >
      {/* Spacer to push input below the fold so scroll is required */}
      <View style={styles.spacer}>
        <Text style={styles.hint}>
          Tap the input below — it should scroll into view above the keyboard
        </Text>
        <SheetExample />
      </View>
     <TextInput style={styles.input} />
      <ControlledInputView
        value={value}
        ref={inputRef}
        placeholder='Type something (no digits)...'
        onTextChange={(text) => setValue(text.replace(/\d/g, ''))}
        style={[styles.input, isFocused && styles.focusedInput]}
        onFocus={() => {
          console.log('[ControlledInput] onFocus fired');
          setIsFocused(true);
        }}
        onBlur={() => {
          console.log('[ControlledInput] onBlur fired');
          setIsFocused(false);
        }}
      />

      <Button title='FOCUS' onPress={() => inputRef.current?.focus()} />
      <Button title='BLUR' onPress={() => inputRef.current?.blur()} />
    </KeyboardAwareScrollView>
  );
}

export default function App(): ReactElement {
  return (
    <GestureHandlerRootView style={styles.root}>
      <KeyboardProvider>
        <BottomSheetModalProvider>
          <InputScreen />
        </BottomSheetModalProvider>
      </KeyboardProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  container: {
    backgroundColor: '#ffffff',
    paddingBottom: 40,
  },
  spacer: {
    height: 600,
    justifyContent: 'flex-end',
    paddingHorizontal: 20,
    paddingBottom: 20,
    gap: 12,
  },
  hint: {
    color: '#888',
    fontSize: 14,
    textAlign: 'center',
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
  },
  focusedInput: {
    borderColor: '#167BF1',
  },
});

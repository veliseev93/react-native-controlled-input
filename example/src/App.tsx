import { useRef, useState } from 'react';
import { StyleSheet, ScrollView, Button } from 'react-native';
import {
  ControlledInputView,
  type ControlledInputViewRef,
} from 'react-native-controlled-input';

export default function App() {
  const [value, setValue] = useState('');
  const inputRef = useRef<ControlledInputViewRef>(null);

  return (
    <ScrollView
      contentContainerStyle={styles.container}
      keyboardShouldPersistTaps="handled"
      onTouchStart={() => inputRef.current?.blur()}
    >
      <ControlledInputView
        value={value}
        ref={inputRef}
        onTextChange={(event) => {
          setValue(event.nativeEvent.value.replace(/\d/g, ''));
        }}
        style={styles.box}
        inputStyle={styles.textInput}
        onFocus={() => {
          console.log('onFocus');
        }}
        onBlur={() => {
          console.log('onBlur');
        }}
      />
      <Button title="Focus" onPress={() => inputRef.current?.focus()} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 250,
  },
  box: {
    maxWidth: '100%',
    height: 46,
    marginVertical: 20,
    marginHorizontal: 10,
    flex: 1,
  },
  textInput: {
    color: 'black',
    fontSize: 24,
    height: 46,
    paddingTop: 10,
    paddingBottom: 10,
    paddingLeft: 10,
    paddingRight: 10,
    borderWidth: 1,
    borderColor: 'green',
    borderRadius: 10,
  },
});

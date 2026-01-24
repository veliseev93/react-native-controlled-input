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
      contentContainerStyle={{ flex: 1, backgroundColor: 'green' }}
      style={styles.container}
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
      <Button title="Blur" onPress={() => inputRef.current?.blur()} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  box: {
    width: '100%',
    height: 44,
    marginVertical: 20,
    backgroundColor: 'red',
  },
  textInput: {
    color: 'black',
    fontSize: 24,
    height: 44,
    paddingTop: 10,
    paddingBottom: 10,
    paddingLeft: 10,
    paddingRight: 10,
  },
});

import { useRef, useState } from 'react';
import {
  StyleSheet,
  ScrollView,
  findNodeHandle,
  UIManager,
  Button,
  TextInput,
} from 'react-native';
import { ControlledInputView } from 'react-native-controlled-input';

export default function App() {
  const [value, setValue] = useState('');
  const inputRef = useRef(null);

  function blurNative(ref: any) {
    const tag = findNodeHandle(ref);
    if (!tag) return;

    // Paper:
    UIManager.dispatchViewManagerCommand(
      tag,
      UIManager.getViewManagerConfig('ControlledInputView').Commands.blur,
      []
    );

    // Если Fabric — будет другая функция (через native commands из codegen).
  }
  return (
    <ScrollView
      contentContainerStyle={{ flex: 1, backgroundColor: 'green' }}
      style={styles.container}
      keyboardShouldPersistTaps="handled"
      onTouchStart={() => blurNative(inputRef.current)}
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
      <Button title="Blur" onPress={() => blurNative(inputRef.current)} />
      <TextInput placeholder="Enter text" />
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

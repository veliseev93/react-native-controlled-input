import { useState } from 'react';
import { StyleSheet, ScrollView } from 'react-native';
import { ControlledInputView } from 'react-native-controlled-input';

export default function App() {
  const [value, setValue] = useState('');

  return (
    <ScrollView
      contentContainerStyle={{ flex: 1, backgroundColor: 'green' }}
      style={styles.container}
      keyboardShouldPersistTaps="never"
    >
      <ControlledInputView
        value={value}
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

import { useState } from 'react';
import { View, StyleSheet, TextInput } from 'react-native';
import { ControlledInputView } from 'react-native-controlled-input';

export default function App() {
  const [value, setValue] = useState('');

  return (
    <View style={styles.container}>
      <ControlledInputView
        value={value}
        onTextChange={(event) => {
          setValue(event.nativeEvent.value.replace(/\d/g, ''));
        }}
        style={styles.box}
        inputStyle={styles.textInput}
      />
      <TextInput
        style={{ color: 'white', backgroundColor: 'white' }}
        value={value}
        onChangeText={setValue}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,

    justifyContent: 'center',
    backgroundColor: 'green',
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

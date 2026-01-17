import { useState } from 'react';
import { View, StyleSheet } from 'react-native';
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
    height: 60,
    marginVertical: 20,
    backgroundColor: 'red',
  },
});

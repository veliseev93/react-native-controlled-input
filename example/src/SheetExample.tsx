import { useState, type ReactElement } from 'react';
import {
  Button,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { ControlledInputView } from 'react-native-controlled-input';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';

function SheetHeader({
  onCancel,
  onDone,
}: {
  onCancel: () => void;
  onDone: () => void;
}): ReactElement {
  return (
    <View style={styles.header}>
      <TouchableOpacity onPress={onCancel} hitSlop={12}>
        <Text style={styles.headerCancel}>Cancel</Text>
      </TouchableOpacity>
      <Text style={styles.headerTitle}>New Event</Text>
      <TouchableOpacity onPress={onDone} hitSlop={12}>
        <Text style={styles.headerDone}>Done</Text>
      </TouchableOpacity>
    </View>
  );
}

function SheetContent({ onClose }: { onClose: () => void }): ReactElement {
  const [title, setTitle] = useState('');
  const [location, setLocation] = useState('');
  const [notes, setNotes] = useState('');

  return (
    <KeyboardAwareScrollView
      contentContainerStyle={styles.container}
      keyboardShouldPersistTaps='handled'
      mode='layout'
    >
      <View style={styles.handlePill} />
      <SheetHeader onCancel={onClose} onDone={onClose} />

      <View style={styles.section}>
        <Text style={styles.label}>Title</Text>
        <ControlledInputView
          value={title}
          onTextChange={setTitle}
          placeholder='Event title...'
          style={styles.input}
          onFocus={() => console.log('[Sheet] title focused')}
          onBlur={() => console.log('[Sheet] title blurred')}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Location</Text>
        <ControlledInputView
          value={location}
          onTextChange={setLocation}
          placeholder='Add location...'
          style={styles.input}
          onFocus={() => console.log('[Sheet] location focused')}
          onBlur={() => console.log('[Sheet] location blurred')}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Notes</Text>
        <ControlledInputView
          value={notes}
          onTextChange={setNotes}
          placeholder='Add notes...'
          style={[styles.input, styles.notesInput]}
          onFocus={() => console.log('[Sheet] notes focused')}
          onBlur={() => console.log('[Sheet] notes blurred')}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>Extra field 1</Text>
        <ControlledInputView
          value=''
          onTextChange={() => {}}
          placeholder='Some extra field...'
          style={styles.input}
          onFocus={() => console.log('[Sheet] extra1 focused')}
          onBlur={() => console.log('[Sheet] extra1 blurred')}
        />
      </View>
      <TextInput style={styles.input} />
      <View style={styles.section}>
        <Text style={styles.label}>Extra field 2</Text>
        <ControlledInputView
          value=''
          onTextChange={() => {}}
          placeholder='Another extra field...'
          style={styles.input}
          onFocus={() => console.log('[Sheet] extra2 focused')}
          onBlur={() => console.log('[Sheet] extra2 blurred')}
        />
      </View>
    </KeyboardAwareScrollView>
  );
}

export function SheetExample(): ReactElement {
  const [visible, setVisible] = useState(false);

  const close = (): void => setVisible(false);

  return (
    <>
      <Button
        title='Open Sheet'
        onPress={() => {
          console.log('open');
          setVisible(true);
        }}
      />
      <Modal
        visible={visible}
        transparent
        animationType='fade'
        onRequestClose={close}
        statusBarTranslucent
      >
        <View style={styles.modalRoot}>
          <Pressable style={styles.backdrop} onPress={close} />
          <View style={styles.sheet}>
            <SheetContent onClose={close} />
          </View>
        </View>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  modalRoot: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
  },
  sheet: {
    maxHeight: '75%',
    width: '100%',
    backgroundColor: '#1C1C1E',
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    overflow: 'hidden',
  },
  handlePill: {
    alignSelf: 'center',
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#DCDDE1',
    marginTop: 8,
    marginBottom: 4,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#38383A',
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '600',
  },
  headerCancel: {
    color: '#8E8E93',
    fontSize: 16,
  },
  headerDone: {
    color: '#167BF1',
    fontSize: 16,
    fontWeight: '600',
  },
  container: {
    gap: 24,
    paddingBottom: 40,
  },
  section: {
    gap: 8,
    paddingHorizontal: 16,
  },
  label: {
    color: '#8E8E93',
    fontSize: 13,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  input: {
    height: 48,
    borderWidth: 1,
    borderColor: '#38383A',
    borderRadius: 10,
    fontSize: 16,
    backgroundColor: '#2C2C2E',
    paddingHorizontal: 12,
    color: '#FFFFFF',
  },
  notesInput: {
    height: 96,
  },
});

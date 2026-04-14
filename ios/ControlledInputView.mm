#import "ControlledInputView.h"

#if __has_include("ControlledInput-Swift.h")
#import "ControlledInput-Swift.h"
#else
#import <ControlledInput/ControlledInput-Swift.h>
#endif

#import <React/RCTConversions.h>
#import <React/RCTFabricComponentsPlugins.h>

#import <react/renderer/components/ControlledInputViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/ControlledInputViewSpec/EventEmitters.h>
#import <react/renderer/components/ControlledInputViewSpec/Props.h>
#import <react/renderer/components/ControlledInputViewSpec/RCTComponentViewHelpers.h>

using namespace facebook::react;

@interface ControlledInputView () <RCTControlledInputViewViewProtocol, RNControlledInputDelegate>
@end

@implementation ControlledInputView {
    RNControlledInput * _inputView;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<ControlledInputViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ControlledInputViewProps>();
    _props = defaultProps;

    _inputView = [[RNControlledInput alloc] initWithFrame:self.bounds];
    _inputView.delegate = self;

    self.contentView = _inputView;
  }

  return self;
}

- (void)setTag:(NSInteger)tag
{
  [super setTag:tag];
  _inputView.tag = tag;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<ControlledInputViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<ControlledInputViewProps const>(props);

    if (oldViewProps.value != newViewProps.value) {
        _inputView.value = [NSString stringWithUTF8String:newViewProps.value.c_str()];
    }

    if (oldViewProps.autoComplete != newViewProps.autoComplete) {
        _inputView.autoComplete = newViewProps.autoComplete.empty() ? nil : [NSString stringWithUTF8String:newViewProps.autoComplete.c_str()];
    }

    if (oldViewProps.keyboardType != newViewProps.keyboardType) {
        _inputView.keyboardType = newViewProps.keyboardType.empty() ? nil : [NSString stringWithUTF8String:newViewProps.keyboardType.c_str()];
    }

    if (oldViewProps.returnKeyType != newViewProps.returnKeyType) {
        _inputView.returnKeyType = newViewProps.returnKeyType.empty() ? nil : [NSString stringWithUTF8String:newViewProps.returnKeyType.c_str()];
    }

    if (oldViewProps.autoCapitalize != newViewProps.autoCapitalize) {
        _inputView.autoCapitalize = newViewProps.autoCapitalize.empty() ? nil : [NSString stringWithUTF8String:newViewProps.autoCapitalize.c_str()];
    }

    if (oldViewProps.placeholder != newViewProps.placeholder) {
        _inputView.placeholder = newViewProps.placeholder.empty() ? nil : [NSString stringWithUTF8String:newViewProps.placeholder.c_str()];
    }

    if (oldViewProps.placeholderTextColor != newViewProps.placeholderTextColor) {
        _inputView.placeholderTextColor = RCTUIColorFromSharedColor(newViewProps.placeholderTextColor);
    }

    if (oldViewProps.selectionColor != newViewProps.selectionColor) {
        _inputView.selectionColor = RCTUIColorFromSharedColor(newViewProps.selectionColor);
    }

    const auto &style = newViewProps.inputStyle;
    const auto &oldStyle = oldViewProps.inputStyle;

    if (oldStyle.color != style.color) {
        _inputView.textColor = RCTUIColorFromSharedColor(style.color);
    }

    if (oldStyle.fontSize != style.fontSize) {
        _inputView.fontSize = style.fontSize;
    }

    if (oldStyle.fontFamily != style.fontFamily) {
        _inputView.fontFamily = style.fontFamily.empty() ? nil : [NSString stringWithUTF8String:style.fontFamily.c_str()];
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args
{
    if ([commandName isEqualToString:@"focus"]) {
        [_inputView focus];
        return;
    }

    if ([commandName isEqualToString:@"blur"]) {
        [_inputView blur];
        return;
    }

    [super handleCommand:commandName args:args];
}

- (void)controlledInputDidChangeText:(RNControlledInput *)input value:(NSString *)value
{
    if (_eventEmitter == nullptr) {
        return;
    }

    const auto eventEmitter = std::static_pointer_cast<const ControlledInputViewEventEmitter>(_eventEmitter);
    const char *utf8Value = value.UTF8String ?: "";

    eventEmitter->onTextChange(ControlledInputViewEventEmitter::OnTextChange {
        .value = std::string(utf8Value),
    });
}

- (void)controlledInputDidFocus:(RNControlledInput *)input
{
    if (_eventEmitter == nullptr) {
        return;
    }

    const auto eventEmitter = std::static_pointer_cast<const ControlledInputViewEventEmitter>(_eventEmitter);
    eventEmitter->onFocus(ControlledInputViewEventEmitter::OnFocus {});
}

- (void)controlledInputDidBlur:(RNControlledInput *)input
{
    if (_eventEmitter == nullptr) {
        return;
    }

    const auto eventEmitter = std::static_pointer_cast<const ControlledInputViewEventEmitter>(_eventEmitter);
    eventEmitter->onBlur(ControlledInputViewEventEmitter::OnBlur {});
}

@end

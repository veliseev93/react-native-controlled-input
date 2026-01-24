#import "ControlledInputView.h"

#if __has_include("ControlledInput-Swift.h")
#import "ControlledInput-Swift.h"
#else
#import <ControlledInput/ControlledInput-Swift.h>
#endif

#import <React/RCTConversions.h>
#import <React/RCTFabricComponentsPlugins.h>

#import <react/renderer/components/ControlledInputViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/ControlledInputViewSpec/Props.h>
#import <react/renderer/components/ControlledInputViewSpec/RCTComponentViewHelpers.h>

using namespace facebook::react;

@interface ControlledInputView () <RCTControlledInputViewViewProtocol>
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

    self.contentView = _inputView;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<ControlledInputViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<ControlledInputViewProps const>(props);

    if (oldViewProps.value != newViewProps.value) {
        _inputView.value = [NSString stringWithUTF8String:newViewProps.value.c_str()];
    }

    // Update inputStyle props
    const auto &style = newViewProps.inputStyle;
    const auto &oldStyle = oldViewProps.inputStyle;

    if (oldStyle.color != style.color) {
        _inputView.textColor = RCTUIColorFromSharedColor(style.color);
    }
    
    if (oldStyle.fontSize != style.fontSize) {
        _inputView.fontSize = style.fontSize;
    }
    
    if (oldStyle.height != style.height) {
        _inputView.inputHeight = style.height;
    }
    
    if (oldStyle.paddingTop != style.paddingTop || 
        oldStyle.paddingBottom != style.paddingBottom ||
        oldStyle.paddingLeft != style.paddingLeft ||
        oldStyle.paddingRight != style.paddingRight) {
        _inputView.padding = UIEdgeInsetsMake(style.paddingTop, style.paddingLeft, style.paddingBottom, style.paddingRight);
    }
    
    if (oldStyle.borderWidth != style.borderWidth) {
        _inputView.borderWidth = style.borderWidth;
    }
    
    if (oldStyle.borderRadius != style.borderRadius) {
        _inputView.borderRadius = style.borderRadius;
    }
    
    if (oldStyle.borderColor != style.borderColor) {
        _inputView.borderColor = RCTUIColorFromSharedColor(style.borderColor);
    }

    [super updateProps:props oldProps:oldProps];
}

@end

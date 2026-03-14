import UIKit

@objc(RNControlledInput)
public class RNControlledInput: UIView {
    
    @objc public var value: String? {
        didSet {
            // Update UI
        }
    }
    
    @objc public var textColor: UIColor?
    @objc public var fontSize: CGFloat = 16
    @objc public var inputHeight: CGFloat = 0
    @objc public var padding: UIEdgeInsets = .zero
    @objc public var borderWidth: CGFloat = 0
    @objc public var borderRadius: CGFloat = 0
    @objc public var borderColor: UIColor?

    public override var canBecomeFirstResponder: Bool {
        true
    }

    @objc public func focus() {
        print("[ControlledInputView] RNControlledInput.focus()")
        _ = becomeFirstResponder()
    }

    @objc public func blur() {
        print("[ControlledInputView] RNControlledInput.blur()")
        _ = resignFirstResponder()
    }

    @objc public override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = .red
        self.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            self.widthAnchor.constraint(equalToConstant: 100),
            self.heightAnchor.constraint(equalToConstant: 100)
        ])
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

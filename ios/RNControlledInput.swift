import UIKit

@objc public protocol RNControlledInputDelegate: AnyObject {
    func controlledInputDidChangeText(_ input: RNControlledInput, value: String)
}

@objc(RNControlledInput)
public class RNControlledInput: UIView, UITextFieldDelegate {

    private let textField = UITextField()
    @objc public weak var delegate: RNControlledInputDelegate?

    @objc public var value: String? {
        didSet {
            if textField.text != value {
                textField.text = value
            }
        }
    }

    @objc public var textColor: UIColor? {
        didSet { textField.textColor = textColor }
    }

    @objc public var fontSize: CGFloat = 16 {
        didSet { applyFont() }
    }

    @objc public var fontFamily: String? {
        didSet { applyFont() }
    }

    @objc public var inputHeight: CGFloat = 0 {
        didSet { invalidateIntrinsicContentSize() }
    }

    @objc public var padding: UIEdgeInsets = .zero {
        didSet { applyPadding() }
    }

    @objc public var borderWidth: CGFloat = 0 {
        didSet { layer.borderWidth = borderWidth }
    }

    @objc public var borderRadius: CGFloat = 0 {
        didSet { layer.cornerRadius = borderRadius }
    }

    @objc public var borderColor: UIColor? {
        didSet { layer.borderColor = borderColor?.cgColor }
    }

    public override var canBecomeFirstResponder: Bool { true }

    @objc public func focus() {
        textField.becomeFirstResponder()
    }

    @objc public func blur() {
        textField.resignFirstResponder()
    }

    @objc public override init(frame: CGRect) {
        super.init(frame: frame)
        setupTextField()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupTextField() {
        textField.translatesAutoresizingMaskIntoConstraints = false
        textField.borderStyle = .none
        textField.delegate = self
        addSubview(textField)

        NSLayoutConstraint.activate([
            textField.topAnchor.constraint(equalTo: topAnchor),
            textField.leadingAnchor.constraint(equalTo: leadingAnchor),
            textField.trailingAnchor.constraint(equalTo: trailingAnchor),
            textField.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])

        applyFont()
    }

    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        let currentText = textField.text ?? ""
        guard let stringRange = Range(range, in: currentText) else { return true }
        let newText = currentText.replacingCharacters(in: stringRange, with: string)

        delegate?.controlledInputDidChangeText(self, value: newText)

        return false
    }

    private func applyFont() {
        if let family = fontFamily, let font = UIFont(name: family, size: fontSize) {
            textField.font = font
        } else {
            textField.font = UIFont.systemFont(ofSize: fontSize)
        }
    }

    private func applyPadding() {
        // UITextField doesn't have built-in edge insets — wrap with container views
        let leftView = UIView(frame: CGRect(x: 0, y: 0, width: padding.left, height: 1))
        let rightView = UIView(frame: CGRect(x: 0, y: 0, width: padding.right, height: 1))
        textField.leftView = leftView
        textField.leftViewMode = .always
        textField.rightView = rightView
        textField.rightViewMode = .always
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        layer.borderColor = borderColor?.cgColor
    }
}

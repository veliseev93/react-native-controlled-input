import UIKit

@objc public protocol RNControlledInputDelegate: AnyObject {
    func controlledInputDidChangeText(_ input: RNControlledInput, value: String)
    func controlledInputDidFocus(_ input: RNControlledInput)
    func controlledInputDidBlur(_ input: RNControlledInput)
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

    @objc public var autoComplete: String? {
        didSet { applyAutoComplete() }
    }

    @objc public var autoCapitalize: String? {
        didSet { applyAutoCapitalize() }
    }

    @objc public var keyboardType: String? {
        didSet { applyKeyboardType() }
    }

    @objc public var returnKeyType: String? {
        didSet { applyReturnKeyType() }
    }

    @objc public var placeholder: String? {
        didSet { applyPlaceholder() }
    }

    @objc public var placeholderTextColor: UIColor? {
        didSet { applyPlaceholder() }
    }

    @objc public var selectionColor: UIColor? {
        didSet { textField.tintColor = selectionColor }
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
        textField.textColor = textColor
        addSubview(textField)

        NSLayoutConstraint.activate([
            textField.topAnchor.constraint(equalTo: topAnchor),
            textField.leadingAnchor.constraint(equalTo: leadingAnchor),
            textField.trailingAnchor.constraint(equalTo: trailingAnchor),
            textField.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])

        applyFont()
        applyAutoComplete()
        applyAutoCapitalize()
        applyKeyboardType()
        applyReturnKeyType()
        applyPlaceholder()
    }

    private func applyPlaceholder() {
        guard let placeholder = placeholder else {
            textField.attributedPlaceholder = nil
            return
        }

        var attributes: [NSAttributedString.Key: Any] = [:]
        if let placeholderTextColor = placeholderTextColor {
            attributes[.foregroundColor] = placeholderTextColor
        }

        textField.attributedPlaceholder = NSAttributedString(string: placeholder, attributes: attributes)

        if let color = textColor {
            textField.textColor = color
        }
    }

    public func textFieldDidBeginEditing(_ textField: UITextField) {
        delegate?.controlledInputDidFocus(self)
    }

    public func textFieldDidEndEditing(_ textField: UITextField) {
        delegate?.controlledInputDidBlur(self)
    }

    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        let currentText = textField.text ?? ""
        guard let stringRange = Range(range, in: currentText) else { return true }
        let newText = currentText.replacingCharacters(in: stringRange, with: string)

        delegate?.controlledInputDidChangeText(self, value: newText)

        // If the replacement string is more than one character, it's likely an autocomplete/paste
        // In this case, we should let the UITextField update itself to handle it correctly
        if string.count > 1 {
            return true
        }

        return false
    }

    private func applyFont() {
        guard let family = fontFamily else {
            textField.font = UIFont.systemFont(ofSize: fontSize)
            return
        }
        
        if let font = UIFont(name: family, size: fontSize) {
            textField.font = font
            return
        }
        for face in UIFont.fontNames(forFamilyName: family) {
            if let font = UIFont(name: face, size: fontSize) {
                textField.font = font
                return
            }
        }
        textField.font = UIFont.systemFont(ofSize: fontSize)
    }

    private func applyAutoComplete() {
        guard let autoComplete else {
            textField.textContentType = nil
            return
        }

        if autoComplete == "off" || autoComplete.isEmpty {
            textField.textContentType = nil
            return
        }

        let mappedValue: String
        switch autoComplete {
        case "email":
            mappedValue = "emailAddress"
        case "tel":
            mappedValue = "telephoneNumber"
        case "name":
            mappedValue = "name"
        case "given-name":
            mappedValue = "givenName"
        case "middle-name":
            mappedValue = "middleName"
        case "family-name":
            mappedValue = "familyName"
        case "username":
            mappedValue = "username"
        case "password":
            mappedValue = "password"
        case "new-password":
            mappedValue = "newPassword"
        case "one-time-code":
            mappedValue = "oneTimeCode"
        case "postal-code":
            mappedValue = "postalCode"
        case "street-address":
            mappedValue = "fullStreetAddress"
        case "country":
            mappedValue = "countryName"
        case "cc-number":
            mappedValue = "creditCardNumber"
        case "cc-csc":
            mappedValue = "creditCardSecurityCode"
        case "cc-exp":
            mappedValue = "creditCardExpiration"
        case "cc-exp-month":
            mappedValue = "creditCardExpirationMonth"
        case "cc-exp-year":
            mappedValue = "creditCardExpirationYear"
        case "birthdate-day":
            mappedValue = "birthdateDay"
        case "birthdate-month":
            mappedValue = "birthdateMonth"
        case "birthdate-year":
            mappedValue = "birthdateYear"
        case "url":
            mappedValue = "URL"
        default:
            mappedValue = autoComplete
        }

        textField.textContentType = UITextContentType(rawValue: mappedValue)
    }

    private func applyAutoCapitalize() {
        guard let autoCapitalize else {
            textField.autocapitalizationType = .sentences
            return
        }

        switch autoCapitalize {
        case "none":
            textField.autocapitalizationType = .none
        case "words":
            textField.autocapitalizationType = .words
        case "sentences":
            textField.autocapitalizationType = .sentences
        case "characters":
            textField.autocapitalizationType = .allCharacters
        default:
            textField.autocapitalizationType = .sentences
        }
    }

    private func applyKeyboardType() {
        switch keyboardType {
        case "ascii-capable":
            textField.keyboardType = .asciiCapable
        case "numbers-and-punctuation":
            textField.keyboardType = .numbersAndPunctuation
        case "url":
            textField.keyboardType = .URL
        case "number-pad":
            textField.keyboardType = .numberPad
        case "phone-pad":
            textField.keyboardType = .phonePad
        case "name-phone-pad":
            textField.keyboardType = .namePhonePad
        case "email-address":
            textField.keyboardType = .emailAddress
        case "decimal-pad":
            textField.keyboardType = .decimalPad
        case "twitter":
            textField.keyboardType = .twitter
        case "web-search":
            textField.keyboardType = .webSearch
        case "visible-password":
            textField.keyboardType = .asciiCapable
        case "numeric":
            textField.keyboardType = .numbersAndPunctuation
        default:
            textField.keyboardType = .default
        }
    }

    private func applyReturnKeyType() {
        switch returnKeyType {
        case "done":
            textField.returnKeyType = .done
        case "go":
            textField.returnKeyType = .go
        case "next":
            textField.returnKeyType = .next
        case "search":
            textField.returnKeyType = .search
        case "send":
            textField.returnKeyType = .send
        case "none":
            textField.returnKeyType = .default
        case "previous":
            textField.returnKeyType = .default
        case "route":
            textField.returnKeyType = .route
        case "yahoo":
            textField.returnKeyType = .yahoo
        case "emergency-call":
            textField.returnKeyType = .emergencyCall
        case "google":
            textField.returnKeyType = .google
        case "join":
            textField.returnKeyType = .join
        default:
            textField.returnKeyType = .default
        }
    }
}

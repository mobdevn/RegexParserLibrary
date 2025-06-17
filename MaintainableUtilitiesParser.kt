object SequentialPageUtilities {
    
    // Keyboard type mapping
    fun getKeyBoardType(keyboard: SequentialPageCaptureKeyboardType?): KeyboardType {
        return keyboard?.android2?.firstOrNull()?.type?.let { type ->
            when (type) {
                KEYBOARD_TYPE_NUMBER_CLEAR -> KeyboardType.NumberPassword
                KEYBOARD_TYPE_PHONE -> KeyboardType.Phone
                else -> KeyboardType.Text
            }
        } ?: KeyboardType.Text
    }
    
    // Component type mapping
    fun addSPCComponentType(type: String?): SequentialPageCaptureComponentType {
        return componentTypeMap[type] ?: INPUT_FIELD
    }
    
    // Input formatting utilities
    fun getFormattedValue(
        userInput: String,
        inputSeparator: String,
        transformationIndex: Int,
        regex: String
    ): String {
        val paddedInput = userInput.padEnd(userInput.length, CHAR_SPACE)
        return buildString {
            if (paddedInput.length > transformationIndex) {
                append(paddedInput.substring(INITIAL_PROD_INDEX, transformationIndex))
                append(inputSeparator)
                append(
                    paddedInput.substring(
                        transformationIndex,
                        minOf(userInput.length, paddedInput.length)
                    )
                )
            } else {
                append(paddedInput)
            }
        }.take(getMaxLengthFromRegex(regex))
    }
    
    fun getFormattedFromRegex(
        userInput: String,
        regex: String,
        transformationType: OutputTransformation?,
        inputTextSeparator: String,
        transformationIndex: Int
    ): String {
        return if (transformationType is InvestmentAccountNumberTransformation) {
            getFormattedValue(
                userInput = userInput,
                inputSeparator = inputTextSeparator,
                transformationIndex = transformationIndex,
                regex = regex
            )
        } else {
            userInput
        }
    }
    
    // Input length validation
    fun checkInputLen(regex: String, transformationType: OutputTransformation?): Int {
        return if (transformationType is SortCodeTransformation) {
            SORT_CODE_MAX_LEN
        } else {
            SequentialPageRegexAnalyser.getRegexMaxLength(regex)
        }
    }
    
    // Investment account utilities
    fun getInputLiteral(regex: String): String {
        return if (regex.contains(SMART_INVESTOR_PREFIX)) {
            SMART_INVESTOR_PREFIX
        } else {
            DEFAULT_LITERAL
        }
    }
    
    fun getLiteralIndex(regex: String, inputSeparator: String): Int {
        return if (inputSeparator.isNotEmpty()) {
            getLiteralInsertionPosition(
                regex = regex,
                literal = inputSeparator.toCharArray()[INITIAL_PROD_INDEX]
            )
        } else {
            INITIAL_PROD_INDEX
        }
    }
    
    // Transformation type factory
    fun getTransformationType(
        contentType: String,
        inputTextSeparator: String = "",
        transformationIndex: Int = -1,
        maxLength: Int = -1
    ): OutputTransformation? {
        return when (contentType) {
            CONTENT_ACCOUNT_NUMBER_TYPE -> AccountNumberTransformation()
            CONTENT_SORT_CODE_TYPE -> SortCodeTransformation()
            SMART_INVESTOR_PREFIX -> InvestmentAccountNumberTransformation(
                transformationLiteral = inputTextSeparator,
                transformationIndex = transformationIndex,
                maxLength = maxLength
            )
            else -> null
        }
    }
    
    // Accessibility utilities
    fun accessibilityTagBuilder(
        viewLabel: String,
        isContinueButtonEnabled: Boolean,
        noRadioOptionSelectedText: String?
    ): String {
        return if (isContinueButtonEnabled) {
            buildString {
                append(viewLabel)
                append(noRadioOptionSelectedText ?: "")
            }
        } else {
            viewLabel
        }
    }
    
    fun contextualInformationForInputField(
        item: SequentialPageFieldCaptureItem,
        accessibilityTitleLabel: String
    ): String {
        val contextualInformation = accessibilityTitleLabel
            .substringAfter(ACCESSIBILITY_SEPARATOR, ACCESSIBILITY_TITLE_LABEL_EMPTY)
            .trim()
        
        return if (item.heading?.isNotEmpty() == true) {
            item.heading
        } else {
            contextualInformation
        }
    }
    
    // Validation utilities
    fun isOnlyInputField(inputFieldsCount: Int): Boolean? {
        return if (inputFieldsCount == 1) false else null
    }
    
    // Regex analysis utilities
    fun getMaxLengthFromRegex(regex: String): Int {
        return SequentialPageRegexAnalyser.getRegexMaxLength(regex)
    }
    
    private fun getLiteralInsertionPosition(regex: String, literal: Char): Int {
        return SequentialPageRegexAnalyser.getLiteralInsertionPosition(regex, literal)
    }
    
    // Date utilities
    fun formatDateForPayload(dateValue: String?): Date? {
        return dateValue?.let { TimeAndDateUtils.str2Date(it, PAY_LOAD_DATE_PATTERN) }
    }
    
    // Payload utilities
    fun shouldExcludeFromPayload(itemKey: String): Boolean {
        return itemKey in excludedPayloadKeys
    }
    
    fun isSpecialActivationFlow(stepId: String?): Boolean {
        return stepId == PENDING_ACTIVATION_CODE_INITIAL
    }
    
    // Product utilities
    fun createProductUIState(
        product: SequentialPageInvestmentProductInfo,
        index: Int
    ): SequentialPageProductPickerUIState {
        return SequentialPageProductPickerUIState(
            productTitle = product.title ?: "",
            productAnalyticsOptionTag = product.analyticsOptionTag,
            productIcon = product.icon,
            productIndex = index,
            productSelectionAccessibilityTag = "${product.title} ${product.description}"
        )
    }
    
    // Error handling utilities
    fun findFirstErrorIndex(items: List<SequentialPageFieldCaptureItem>): Int? {
        return items.indexOfFirst { it.errorState.isError }
            .takeIf { it != -1 } ?: INPUT_FIELD_DEFAULT_INDEX
    }
    
    // Analytics utilities
    fun getAnalyticsActionType(stepId: String?): ActionType {
        return if (stepId == PRODUCT_SELECTION_SAVE) {
            ActionType.NONE_OF_THESE
        } else {
            ActionType.RESEND
        }
    }
    
    fun getAnalyticsLabel(stepId: String?): String {
        return if (stepId == PRODUCT_SELECTION_SAVE) {
            NONE_OF_THESE
        } else {
            RESEND
        }
    }
    
    // Constants and mappings
    private val componentTypeMap = mapOf(
        DATE_TYPE to DATE,
        REVIEW_FIELD_TYPE to REVIEW_FIELD,
        SEQUENTIAL_PAGE_RADIO_INPUT to RADIO_INPUT,
        SEQUENTIAL_PAGE_RADIO_OPTION to RADIO_OPTION,
        SEQUENTIAL_PAGE_HYPERLINK to HYPERLINK,
        SEQUENTIAL_PAGE_INFO_PANEL to INFO_PANEL
    )
    
    private val excludedPayloadKeys = setOf(NONE_OF_THESE_VALUE, LINK_VALUE)
    
    // Constants
    private const val PENDING_ACTIVATION_CODE_INITIAL = "PENDING_ACTIVATION_CODE_INITIAL"
    private const val PRODUCT_SELECTION_SAVE = "PRODUCT_SELECTION_SAVE"
    private const val NONE_OF_THESE_VALUE = "noneOfThese"
    private const val LINK_VALUE = "link"
    private const val SMART_INVESTOR_PREFIX = "1D"
    private const val INITIAL_PROD_INDEX = 0
    private const val CHAR_SPACE = ' '
    private const val DEFAULT_LITERAL = ""
    private const val SORT_CODE_MAX_LEN = 6
    private const val CONTENT_ACCOUNT_NUMBER_TYPE = "ACCOUNT_NUMBER"
    private const val CONTENT_SORT_CODE_TYPE = "SORT_CODE"
    private const val INPUT_FIELD_DEFAULT_INDEX = 0
    private const val PAY_LOAD_DATE_PATTERN = "yyyy-MM-dd"
    private const val ACCESSIBILITY_SEPARATOR = ":"
    private const val ACCESSIBILITY_TITLE_LABEL_EMPTY = ""
    private const val DATE_TYPE = "date"
    private const val REVIEW_FIELD_TYPE = "review"
    private const val SEQUENTIAL_PAGE_RADIO_INPUT = "radio_input"
    private const val SEQUENTIAL_PAGE_RADIO_OPTION = "radio_option"
    private const val SEQUENTIAL_PAGE_HYPERLINK = "hyperlink"
    private const val SEQUENTIAL_PAGE_INFO_PANEL = "info_panel"
    private const val KEYBOARD_TYPE_NUMBER_CLEAR = "number_clear"
    private const val KEYBOARD_TYPE_PHONE = "phone"
    private const val NONE_OF_THESE = "none_of_these"
    private const val RESEND = "resend"
}

// SequentialPageStates.kt
data class SequentialPageStates(
    val uiState: SequentialPageUiState,
    val buttonState: SequentialPageButtonState,
    val productPickerState: SequentialPageProductPickerState,
    val investmentState: SequentialPageInvestmentAccountUIState
)

data class SequentialPageUiState(
    val items: List<SequentialPageFieldCaptureItem> = emptyList(),
    val accordionState: SequentialPageAccordionState = SequentialPageAccordionState(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SequentialPageButtonState(
    val isContinueButtonEnabled: Boolean = false
)

data class SequentialPageProductPickerState(
    val products: List<SequentialPageProductPickerUIState> = emptyList(),
    val selectedProductId: Int = -1
)

data class SequentialPageInvestmentAccountUIState(
    val isInputViewVisible: Boolean = false,
    val isInputViewError: SequentialPageUIErrorState = SequentialPageUIErrorState(),
    val isProductSelectionErrorState: Boolean = false,
    val inputViewLabel: String = "",
    val selectedInvestmentAccountRegex: String = "",
    val inputTextSeparator: String = "",
    val transformation: SequentialPageTransformation? = null,
    val sequentialPageFieldCaptureItem: SequentialPageFieldCaptureItem? = null
)

data class SequentialPageAccordionState(
    val isContentAvailable: Boolean = false,
    val itemList: List<SequentialPageHelpInformation> = emptyList(),
    val itemMapper: Map<String, List<SequentialPageHelpInformation>> = emptyMap()
)

// Actions
// SequentialPageActions.kt
data class SequentialPageActions(
    val onFieldChanged: (String, String) -> Unit,
    val onProductSelected: (Int) -> Unit,
    val onInvestmentProductSelected: (SequentialPageInvestmentProductInfo) -> Unit,
    val onInvestmentAccountChanged: (String) -> Unit,
    val onNextClicked: (SequentialPageInvestmentProductInfo?, String) -> Unit,
    val onBackPressed: (String) -> Unit,
    val onAccordionToggled: (SequentialPageFieldCaptureItem, Boolean) -> Unit,
    val onHyperlinkClicked: () -> Unit
)

//Utils
// SequentialPageUtilities.kt
object SequentialPageUtilities {
    
    fun getKeyBoardType(keyboard: SequentialPageCaptureKeyboardType?): KeyboardType {
        return keyboard?.android2?.firstOrNull()?.type?.let { type ->
            when (type) {
                KEYBOARD_TYPE_NUMBER_CLEAR -> KeyboardType.NumberPassword
                KEYBOARD_TYPE_PHONE -> KeyboardType.Phone
                else -> KeyboardType.Text
            }
        } ?: KeyboardType.Text
    }
    
    fun addSPCComponentType(type: String?): SequentialPageCaptureComponentType {
        return componentTypeMap[type] ?: INPUT_FIELD
    }
    
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
    
    fun checkInputLen(regex: String, transformationType: OutputTransformation?): Int {
        return if (transformationType is SortCodeTransformation) {
            SORT_CODE_MAX_LEN
        } else {
            SequentialPageRegexAnalyser.getRegexMaxLength(regex)
        }
    }
    
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
    
    fun isOnlyInputField(inputFieldsCount: Int): Boolean? {
        return if (inputFieldsCount == 1) false else null
    }
    
    fun getMaxLengthFromRegex(regex: String): Int {
        return SequentialPageRegexAnalyser.getRegexMaxLength(regex)
    }
    
    private fun getLiteralInsertionPosition(regex: String, literal: Char): Int {
        return SequentialPageRegexAnalyser.getLiteralInsertionPosition(regex, literal)
    }
    
    fun formatDateForPayload(dateValue: String?): Date? {
        return dateValue?.let { TimeAndDateUtils.str2Date(it, PAY_LOAD_DATE_PATTERN) }
    }
    
    fun shouldExcludeFromPayload(itemKey: String): Boolean {
        return itemKey in excludedPayloadKeys
    }
    
    fun isSpecialActivationFlow(stepId: String?): Boolean {
        return stepId == PENDING_ACTIVATION_CODE_INITIAL
    }
    
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
    
    fun findFirstErrorIndex(items: List<SequentialPageFieldCaptureItem>): Int? {
        return items.indexOfFirst { it.errorState.isError }
            .takeIf { it != -1 } ?: INPUT_FIELD_DEFAULT_INDEX
    }
    
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


//processor
// SequentialPageFieldProcessor.kt
object SequentialPageFieldProcessor {
    
    fun createFieldCaptureItem(response: SequentialPageCaptureData): SequentialPageFieldCaptureItem {
        val inputFieldType = SequentialPageUtilities.addSPCComponentType(response.type)
        
        return SequentialPageFieldCaptureItem(
            key = response.id,
            regex = response.regex,
            value = response.value,
            heading = response.heading,
            minDate = response.minDate,
            maxDate = response.maxDate,
            keyboardType = SequentialPageUtilities.getKeyBoardType(response.keyboard),
            inputFieldType = inputFieldType,
            errorState = SequentialPageErrorState(),
            title = response.title,
            helpInformation = response.helpInformation,
            contentType = response.contentType,
            radioInputs = response.radioInputs,
            noRadioOptionSelectedText = response.noRadioOptionSelectedText,
            text = response.text,
            action = response.action,
            radioOptions = response.radioOptions
        )
    }
    
    fun processModuleData(dataList: List<SequentialPageCaptureData>): Pair<List<SequentialPageFieldCaptureItem>, Int> {
        var fieldCount = 0
        val items = dataList.mapNotNull { response ->
            createFieldCaptureItem(response).also { item ->
                if (item.inputFieldType == INPUT_FIELD || item.inputFieldType == DATE) {
                    fieldCount++
                }
            }
        }
        return Pair(items, fieldCount)
    }
    
    fun processItemForPayload(
        item: SequentialPageFieldCaptureItem,
        requestDataBuilder: SequentialPageCaptureRequestData.Builder?
    ) {
        with(item) {
            when (type) {
                DATE -> SequentialPageUtilities.formatDateForPayload(value)
                RADIO_OPTION -> value?.let { /* Analytics logging handled elsewhere */ }
            }
            
            key?.let { itemKey ->
                value?.let { itemValue ->
                    if (!SequentialPageUtilities.shouldExcludeFromPayload(itemKey)) {
                        requestDataBuilder?.add(itemKey, itemValue)
                    }
                }
            }
        }
    }
}


//Factory
// SequentialPageTransformationFactory.kt
object SequentialPageTransformationFactory {
    
    fun createTransformation(
        productRegex: String,
        textSeparatorLiteral: String,
        contentType: String?,
        helpText: String?
    ): SequentialPageTransformation {
        val investmentProdLiteral = SequentialPageUtilities.getInputLiteral(productRegex)
        
        return SequentialPageTransformation(
            type = contentType ?: "",
            productLiteral = investmentProdLiteral,
            prodIndex = SequentialPageUtilities.getLiteralIndex(productRegex, investmentProdLiteral),
            transformationIndex = SequentialPageUtilities.getLiteralIndex(productRegex, textSeparatorLiteral),
            outputTransformation = SequentialPageUtilities.getTransformationType(
                contentType = investmentProdLiteral,
                inputTextSeparator = textSeparatorLiteral,
                transformationIndex = SequentialPageUtilities.getLiteralIndex(productRegex, textSeparatorLiteral),
                maxLength = SequentialPageUtilities.getMaxLengthFromRegex(productRegex)
            ),
            transformationLiteral = textSeparatorLiteral,
            maxLength = SequentialPageUtilities.getMaxLengthFromRegex(productRegex)
        )
    }
}


//Mainviewmodel
// SequentialPageCaptureViewModel.kt
class SequentialPageCaptureViewModel(
    private val journeyFrameworkActionBuilder: JourneyFrameworkGenericHandleAction.Builder? = null,
    private val requestDataBuilder: SequentialPageCaptureRequestData.Builder? = null,
    private val analyticsManager: ISequentialPageAnalyticsManager = SequentialPageAnalyticsManager(),
) : ViewModel(), JourneyFrameworkModuleContract.ModuleHelper {

    // Consolidated state management with StateFlow
    private val _states = MutableStateFlow(SequentialPageStates(
        uiState = SequentialPageUiState(),
        buttonState = SequentialPageButtonState(),
        productPickerState = SequentialPageProductPickerState(),
        investmentState = SequentialPageInvestmentAccountUIState()
    ))
    val states: StateFlow<SequentialPageStates> = _states.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Module and validation properties
    lateinit var module: SequentialPageCaptureModule
    var firstErrorIndexField: Int? = null
    private val validators = mutableMapOf<SequentialPageCaptureComponentType, SequentialPageValidators>()
    private var inputFieldsCount = 0
    private var lastSelectedProductId: Int = DEFAULT_LAST_SELECTED_PRODUCT_INDEX
    var selectedProductData by mutableStateOf(SequentialPageInvestmentProductInfo())

    // Actions object for consolidated parameter passing
    val actions = SequentialPageActions(
        onFieldChanged = ::onComponentFieldChanged,
        onProductSelected = ::updateSelectedProductDescription,
        onInvestmentProductSelected = ::updateInvestmentAccountNumber,
        onInvestmentAccountChanged = ::updateInvestmentUserInput,
        onNextClicked = ::onNextButtonClicked,
        onBackPressed = ::onBackPressed,
        onAccordionToggled = ::onAccordionToggled,
        onHyperlinkClicked = ::onClickOfHyperLink
    )

    private fun initializeValidators() {
        validators[DATE] = SequentialPageDateValidator
        validators[RADIO_INPUT] = InvestmentAccountNumberValidator
    }

    override fun startModule(registrationModule: JourneyFrameworkModuleContract.BaseRegistrationModule) {
        this.module = registrationModule as SequentialPageCaptureModule
        
        if (validators.isEmpty()) {
            initializeValidators()
        }
        
        val (processedItems, fieldCount) = SequentialPageFieldProcessor.processModuleData(module.dataList)
        inputFieldsCount = fieldCount
        
        _states.update { currentStates ->
            currentStates.copy(
                uiState = currentStates.uiState.copy(items = processedItems),
                productPickerState = if (module.dataList.isNotEmpty()) {
                    createProductPickerState(module.dataList.first().radioOptions)
                } else currentStates.productPickerState
            )
        }
        
        viewModelScope.launch {
            module.sequentialPageSections.analyticsPageTag?.let { analyticsPageTag ->
                analyticsManager.logViewScreen(analyticsPageTag)
            }
        }
    }

    override fun getModule(): JourneyFrameworkModuleContract.BaseRegistrationModule = module

    fun onComponentFieldChanged(key: String, value: String) {
        val newItems = _states.value.uiState.items.map { item ->
            if (item.key == key) {
                val validator = validators[item.type]
                val isValid = validator?.isValid(
                    module.dataList.find { it.id == item.key },
                    item,
                    selectedProductData
                )
                
                if (isValid == true) {
                    item.copy(
                        value = value,
                        errorState = item.errorState.copy(isError = false)
                    )
                } else {
                    item.copy(
                        value = value,
                        errorState = item.errorState.copy(
                            isError = true,
                            errorMessage = validator?.getErrorMessage() ?: "Invalid input"
                        )
                    )
                }
            } else item
        }
        
        _states.update { currentStates ->
            currentStates.copy(
                uiState = currentStates.uiState.copy(items = newItems)
            )
        }
    }

    fun onNextButtonClicked(
        selectedProduct: SequentialPageInvestmentProductInfo? = null,
        label: String
    ) {
        analyticsManager.logButtonClick(label)
        _loading.update { true }
        
        if (validators.isEmpty()) {
            initializeValidators()
        }
        
        viewModelScope.launch {
            try {
                if (SequentialPageUtilities.isSpecialActivationFlow(
                    module.getAction(ActionType.NEXT)?.stepId
                )) {
                    handleSpecialActivationFlow()
                } else {
                    validatePayloadData(selectedProduct)
                }
            } catch (e: Exception) {
                _error.update { e.message }
            } finally {
                _loading.update { false }
            }
        }
    }

    private fun handleSpecialActivationFlow() {
        module.sequentialPageSections.buttons.forEach { button ->
            launchNextJourney(module.getAction(id = button.id))
        }
    }

    private fun validatePayloadData(selectedProduct: SequentialPageInvestmentProductInfo?) {
        var isValid = true
        val currentItems = _states.value.uiState.items
        
        currentItems.forEachIndexed { index, item ->
            if (item.type != HYPERLINK) {
                updateUIErrorState(false, null, index, item)
                val validator = validators[item.type]
                validator?.isValid(
                    module.dataList.find { it.id == item.key },
                    item,
                    selectedProduct
                )?.let { errorMessage ->
                    isValid = false
                    updateUIErrorState(true, errorMessage, index, item)
                    updateInvestmentAccountErrorState(selectedProduct, errorMessage, item)
                }
            }
        }
        generatePayloadData(isValid)
    }

    private fun generatePayloadData(isValid: Boolean) {
        if (isValid) {
            updateInvestmentAccountsState(false)
            _states.value.uiState.items.forEach { item ->
                SequentialPageFieldProcessor.processItemForPayload(item, requestDataBuilder)
                generateRequestBuilder(item)
            }
            module.sequentialPageSections.buttons.forEach { button ->
                launchNextJourney(module.getAction(id = button.id))
            }
        } else {
            updateErrorIndexField()
        }
    }

    private fun generateRequestBuilder(item: SequentialPageFieldCaptureItem) {
        with(item) {
            if (type == DATE) {
                SequentialPageUtilities.formatDateForPayload(value)
            }
            key?.let { itemKey ->
                value?.let { itemValue ->
                    logSelectedProduct(type, itemValue)
                    addValueToPayload(itemKey, itemValue)
                }
            }
        }
    }

    private fun logSelectedProduct(type: SequentialPageCaptureComponentType, label: String) {
        if (type == RADIO_OPTION) {
            analyticsManager.logProductSelection(label)
        }
    }

    private fun addValueToPayload(itemKey: String, itemValue: String) {
        if (!SequentialPageUtilities.shouldExcludeFromPayload(itemKey)) {
            requestDataBuilder?.add(itemKey, itemValue)
        }
    }

    private fun updateInvestmentAccountErrorState(
        selectedProduct: SequentialPageInvestmentProductInfo?,
        errorMessage: String,
        item: SequentialPageFieldCaptureItem
    ) {
        selectedProduct?.let {
            verifyInvestmentAccountNumber(
                viewType = item.type.toString(),
                isInvestmentProductSelected = it.isProductSelected,
                errorMessage = errorMessage
            )
        }
    }

    private fun updateErrorIndexField() {
        firstErrorIndexField = SequentialPageUtilities.findFirstErrorIndex(_states.value.uiState.items)
    }

    private fun updateUIErrorState(
        isError: Boolean,
        errorMessage: String?,
        index: Int,
        item: SequentialPageFieldCaptureItem
    ) {
        val currentItems = _states.value.uiState.items.toMutableList()
        currentItems[index] = item.copy(
            errorState = SequentialPageErrorState(
                isError = isError,
                errorMessage = errorMessage
            )
        )
        
        _states.update { currentStates ->
            currentStates.copy(
                uiState = currentStates.uiState.copy(items = currentItems)
            )
        }
    }

    fun onClickOfHyperLink() {
        val stepId = module.getAction(ActionType.NEXT)?.stepId
        val actionType = SequentialPageUtilities.getAnalyticsActionType(stepId)
        val label = SequentialPageUtilities.getAnalyticsLabel(stepId)
        
        analyticsManager.logHyperLink(label)
        launchNextJourney(module.getAction(actionType))
    }

    private fun launchNextJourney(action: MvcAction?) {
        action?.let {
            module.attachPayloadToMvcAction(
                requestData = requestDataBuilder?.build(),
                action = action
            )
            journeyFrameworkActionBuilder?.build()?.executeAction(
                action,
                module.id,
                AuthenticationController.CONTROLLER_ID
            )
        }
    }

    // Product management methods
    private fun createProductPickerState(products: List<SequentialPageInvestmentProductInfo?>?): SequentialPageProductPickerState {
        val productUIStates = products?.mapIndexedNotNull { index, productData ->
            productData?.let {
                SequentialPageUtilities.createProductUIState(it, index)
            }
        } ?: emptyList()

        return SequentialPageProductPickerState(products = productUIStates)
    }

    fun updateSelectedProductDescription(productId: Int): String {
        val currentSelectedProduct = updateSelectedProduct(productId)
        
        if (lastSelectedProductId != DEFAULT_LAST_SELECTED_PRODUCT_INDEX) {
            val lastProductDataObject = getSelectedProductFromId(lastSelectedProductId)
            lastProductDataObject?.let { product ->
                val updatedProduct = product.copy(productDescription = null)
                updateProductInList(lastSelectedProductId, updatedProduct)
            }
        }
        
        lastSelectedProductId = productId
        
        _states.update { currentStates ->
            currentStates.copy(
                buttonState = currentStates.buttonState.copy(isContinueButtonEnabled = true)
            )
        }
        
        return currentSelectedProduct?.optionId.toString()
    }

    private fun updateSelectedProduct(productAtIndex: Int): SequentialPageInvestmentProductInfo? {
        val selectedProduct = module.dataList.first().radioOptions?.get(productAtIndex)
        selectedProduct?.description?.let { prodDescription ->
            if (prodDescription.trim().isNotEmpty()) {
                val selectedProductObject = getSelectedProductFromId(productAtIndex)
                selectedProductObject?.let { product ->
                    val updatedProduct = product.copy(productDescription = prodDescription)
                    updateProductInList(productAtIndex, updatedProduct)
                }
            }
        }
        return selectedProduct
    }

    private fun getSelectedProductFromId(id: Int): SequentialPageProductPickerUIState? {
        return _states.value.productPickerState.products.getOrNull(id)
    }

    private fun updateProductInList(index: Int, product: SequentialPageProductPickerUIState) {
        val currentProducts = _states.value.productPickerState.products.toMutableList()
        if (index < currentProducts.size) {
            currentProducts[index] = product
            _states.update { currentStates ->
                currentStates.copy(
                    productPickerState = currentStates.productPickerState.copy(products = currentProducts)
                )
            }
        }
    }

    // Investment account methods
    fun updateInvestmentAccountNumber(
        selectedProduct: SequentialPageInvestmentProductInfo,
        item: SequentialPageFieldCaptureItem
    ) {
        selectedProduct.radioTitle2?.let { 
            logRadioProductSelected(it) 
        }
        updateInvestmentUserInput("")
        updateSelectedInvestmentProduct(selectedProduct, true, item)
    }

    private fun updateInvestmentUserInput(userInput: String) {
        updateInvestmentAccountsState(false)
        selectedProductData = selectedProductData.copy(value = userInput)
    }

    private fun updateSelectedInvestmentProduct(
        selectedInvestmentProduct: SequentialPageInvestmentProductInfo,
        buttonState: Boolean,
        item: SequentialPageFieldCaptureItem
    ) {
        with(selectedInvestmentProduct) {
            selectedProductData = selectedProductData.copy(
                regex = regex,
                invalidInputText = invalidInputText,
                inputTextSeparator = inputTextSeparator,
                isProductSelected = true
            )

            regex?.let { productRegex ->
                inputTextSeparator?.let { textSeparatorLiteral ->
                    val transformation = SequentialPageTransformationFactory.createTransformation(
                        productRegex = productRegex,
                        textSeparatorLiteral = textSeparatorLiteral,
                        contentType = item.contentType,
                        helpText = helpText
                    )
                    
                    _states.update { currentStates ->
                        currentStates.copy(
                            investmentState = currentStates.investmentState.copy(
                                isInputViewVisible = true,
                                inputTextSeparator = textSeparatorLiteral,
                                sequentialPageFieldCaptureItem = item,
                                isInputViewError = SequentialPageUIErrorState(
                                    isError = false,
                                    errorMessage = invalidInputText
                                ),
                                isProductSelectionErrorState = false,
                                inputViewLabel = helpText ?: "",
                                selectedInvestmentAccountRegex = productRegex,
                                transformation = transformation
                            )
                        )
                    }
                }
            }
        }
        updateButtonViewState(buttonState)
    }

    private fun verifyInvestmentAccountNumber(
        viewType: String,
        isInvestmentProductSelected: Boolean,
        errorMessage: String
    ) {
        if (viewType == SEQUENTIAL_PAGE_RADIO_INPUT && !isInvestmentProductSelected) {
            _states.update { currentStates ->
                currentStates.copy(
                    investmentState = currentStates.investmentState.copy(
                        isProductSelectionErrorState = true
                    )
                )
            }
        } else {
            updateInvestmentAccountsState(true, errorMessage)
        }
    }

    private fun updateInvestmentAccountsState(isError: Boolean, errorMessage: String? = null) {
        _states.update { currentStates ->
            currentStates.copy(
                investmentState = currentStates.investmentState.copy(
                    isInputViewError = SequentialPageUIErrorState(
                        isError = isError,
                        errorMessage = errorMessage
                    )
                )
            )
        }
    }

    // Accordion methods
    fun onAccordionToggled(item: SequentialPageFieldCaptureItem, isExpanded: Boolean) {
        logAccordionStateChange(isExpanded)
        
        if (isExpanded) {
            checkForAccordionItems(item)
            updateFirstAccordionItem()
        } else {
            updateAccordion(item)
        }
    }

    fun checkForAccordionItems(item: SequentialPageFieldCaptureItem) {
        _states.update { currentStates ->
            currentStates.copy(
                uiState = currentStates.uiState.copy(
                    accordionState = SequentialPageAccordionState(itemList = emptyList())
                )
            )
        }

        item.helpInformation?.let { accordionItems ->
            updateAccordionState(true)
            item.key?.let { itemKey ->
                updateAccordionMapperContent(itemKey, accordionItems)
            }
        } ?: updateAccordionState(false)
    }

    private fun updateAccordionMapperContent(
        key: String,
        content: List<SequentialPageHelpInformation>
    ) {
        val currentAccordionState = _states.value.uiState.accordionState
        val updatedMapper = if (currentAccordionState.itemList.isEmpty()) {
            mutableMapOf(key to content)
        } else {
            currentAccordionState.itemMapper.toMutableMap().apply {
                this[key] = content
            }
        }
        updateAccordionMapper(updatedMapper)
    }

    private fun updateAccordionMapper(contentMap: MutableMap<String, List<SequentialPageHelpInformation>>) {
        _states.update { currentStates ->
            currentStates.copy(
                uiState = currentStates.uiState.copy(
                    accordionState = SequentialPageAccordionState(itemMapper = contentMap)
                )
            )
        }
    }

    private fun updateAccordionState(state: Boolean) {
        _states.update { currentStates ->
            currentStates.copy(
                uiState = currentStates.uiState.copy(
                    accordionState = currentStates.uiState.accordionState.copy(
                        isContentAvailable = state
                    )
                )
            )
        }
    }

    fun updateFirstAccordionItem() {
        val currentAccordionState = _states.value.uiState.accordionState
        if (currentAccordionState.itemList.isEmpty()) {
            val firstKey = getAccordionItemKey()
            currentAccordionState.itemMapper[firstKey]?.let { helpInfoList ->
                _states.update { currentStates ->
                    currentStates.copy(
                        uiState = currentStates.uiState.copy(
                            accordionState = currentStates.uiState.accordionState.copy(
                                itemList = helpInfoList
                            )
                        )
                    )
                }
            }
        }
    }

    private fun getAccordionItemKey(): String {
        return _states.value.uiState.accordionState.itemMapper.keys.firstOrNull() ?: ""
    }

    fun updateAccordion(item: SequentialPageFieldCaptureItem) {
        val currentAccordionState = _states.value.uiState.accordionState
        currentAccordionState.itemMapper[item.key]?.let { helpInfoList ->
            _states.update { currentStates ->
                currentStates.copy(
                    uiState = currentStates.uiState.copy(
                        accordionState = currentStates.uiState.accordionState.copy(
                            itemList = helpInfoList
                        )
                    )
                )
            }
        }
    }

    // Utility method wrappers for UI
    fun getFormattedValue(
        userInput: String,
        inputSeparator: String,
        transformationIndex: Int,
        regex: String
    ): String = SequentialPageUtilities.getFormattedValue(
        userInput, inputSeparator, transformationIndex, regex
    )

    fun checkInputLen(regex: String, transformationType: OutputTransformation?): Int =
        SequentialPageUtilities.checkInputLen(regex, transformationType)

    fun getFormattedFromRegex(
        userInput: String,
        regex: String,
        transformationType: OutputTransformation?
    ): String {
        val currentState = _states.value.investmentState
        return SequentialPageUtilities.getFormattedFromRegex(
            userInput = userInput,
            regex = regex,
            transformationType = transformationType,
            inputTextSeparator = currentState.inputTextSeparator,
            transformationIndex = currentState.transformation?.transformationIndex ?: -1
        )
    }

    fun accessibilityTagBuilder(viewLabel: String): String =
        SequentialPageUtilities.accessibilityTagBuilder(
            viewLabel = viewLabel,
            isContinueButtonEnabled = _states.value.buttonState.isContinueButtonEnabled,
            noRadioOptionSelectedText = module.sequentialPageSections.sections.first().noRadioOptionSelectedText
        )

    fun updateButtonViewState(state: Boolean) {
        _states.update { currentStates ->
            currentStates.copy(
                buttonState = currentStates.buttonState.copy(isContinueButtonEnabled = state)
            )
        }
    }

    fun onBackPressed(id: String) {
        analyticsManager.logButtonClick("back")
        module.getAction(id = id)?.let { action ->
            module.attachPayloadOfNowAction(
                requestData = JourneyFrameworkGenericHandleAction.EmptyRequest(),
                action = action
            )
            journeyFrameworkActionBuilder?.build()?.executeAction(
                action,
                action.stepId,
                AuthenticationController.CONTROLLER_ID
            )
        }
    }

    fun logRadioProductSelected(label: String) {
        analyticsManager.logProductSelection(label)
    }

    fun logAccordionStateChange(state: Boolean) {
        analyticsManager.logAccordionInteraction(
            if (state) ACCORDION_EXPAND else ACCORDION_COLLAPSED
        )
    }

    fun isOnlyInputField(): Boolean? = SequentialPageUtilities.isOnlyInputField(inputFieldsCount)

    fun contextualInformationForInputField(item: SequentialPageFieldCaptureItem): String =
        SequentialPageUtilities.contextualInformationForInputField(
            item = item,
            accessibilityTitleLabel = module.sequentialPageSections.accessibilityTitleLabel
        )

    fun clearError() {
        _error.update { null }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }

    companion object {
        private const val DEFAULT_LAST_SELECTED_PRODUCT_INDEX = -1
        private const val ACCORDION_EXPAND = "expanded"
        private const val ACCORDION_COLLAPSED = "collapsed"
        private const val SEQUENTIAL_PAGE_RADIO_INPUT = "radio_input"
    }
}


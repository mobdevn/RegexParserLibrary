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

    companion object {
        private const val DEFAULT_LAST_SELECTED_PRODUCT_INDEX = -1
        private const val ACCORDION_EXPAND = "expanded"
        private const val ACCORDION_COLLAPSED = "collapsed"
        private const val SEQUENTIAL_PAGE_RADIO_INPUT = "radio_input"
    }
}

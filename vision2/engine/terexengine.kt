import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SequentialPageCaptureViewModelTest {

    @MockK
    private lateinit var fieldDataProcessor: SequentialPageDataProcessor

    @MockK
    private lateinit var productHandler: SequentialPageSelectedProductProcessor

    @MockK
    private lateinit var investmentAccountHandler: SequentialPageInvestmentAccountProcessor

    @MockK
    private lateinit var payLoadProcessor: SequentialPagePayLoadGenerator

    @MockK
    private lateinit var analyticsManager: SequentialPageAnalyticsEvent

    @MockK
    private lateinit var sequentialPageValidationDelegator: SequentialPageValidationDelegator

    @RelaxedMockK
    private lateinit var journeyActionBuilder: JourneyFrameworkGenericHandleAction.Builder

    @MockK
    private lateinit var moduleDataProcessor: SequentialPageCaptureModuleProcessor

    @MockK
    private lateinit var testModule: SequentialPageCaptureModule

    @MockK
    private lateinit var testAction: MvcAction

    private lateinit var viewModel: SequentialPageCaptureViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val moduleStateFlow = MutableStateFlow<SequentialPageCaptureModule?>(null)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Setup module data processor flow
        every { moduleDataProcessor.dataStateFlow } returns moduleStateFlow

        // Default module setup
        setupDefaultModule()

        viewModel = SequentialPageCaptureViewModel(
            fieldDataProcessor = fieldDataProcessor,
            productHandler = productHandler,
            investmentAccountHandler = investmentAccountHandler,
            payLoadProcessor = payLoadProcessor,
            analyticsManager = analyticsManager,
            sequentialPageValidationDelegator = sequentialPageValidationDelegator,
            journeyActionBuilder = journeyActionBuilder,
            moduleDataProcessor = moduleDataProcessor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun setupDefaultModule() {
        val testSections = SequentialPageSections(analyticsPageTag = "test_page")
        val testDataList = listOf(
            SequentialPageModuleResponse(
                id = "field1",
                regex = "\\d+",
                value = "123",
                heading = "Test Field",
                minDate = null,
                maxDate = null,
                keyboard = "NUMBER",
                type = "INPUT_FIELD",
                invalidInputText = "Invalid input",
                title = "Test Title",
                helpInformation = null,
                contentType = null,
                radioInputs = null,
                noRadioOptionSelectedText = null,
                text = null,
                helpText = null,
                action = null,
                radioOptions = null,
                minDateInvalidText = null,
                maxDateInvalidText = null
            )
        )

        every { testModule.id } returns "test_module"
        every { testModule.sequentialPageSections } returns testSections
        every { testModule.sequentialPageDataList } returns testDataList
        every { testModule.getAction(any()) } returns testAction
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs
    }

    // MARK: - Init Tests

    @Test
    fun `init - should collect module data and start module when available`() = runTest {
        // Given
        val testFieldItem = SequentialPageFieldCaptureItem(
            key = "field1",
            regex = "\\d+",
            value = "123",
            heading = "Test Field",
            minDate = null,
            maxDate = null,
            keyBoardType = KeyboardType.NUMBER,
            type = SequentialPageCaptureComponentType.INPUT_FIELD,
            errorState = SequentialPageUIErrorState(),
            title = "Test Title",
            helpInformation = null,
            contentType = null,
            radioInputs = null,
            noRadioOptionSelectedText = null,
            text = null,
            helpText = null,
            action = null,
            radioOptions = null,
            maxInputLength = 10
        )

        val pageModuleData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = listOf(testFieldItem),
            fieldCount = 1,
            buttonState = true
        )

        every { fieldDataProcessor.processModuleData(any()) } returns pageModuleData
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { analyticsManager.logScreenLoad("test_page") }
        verify { fieldDataProcessor.processModuleData(testModule.sequentialPageDataList) }
        verify { fieldDataProcessor.getAccordionData(any()) }
        verify { fieldDataProcessor.getInvestmentProducts(any()) }
    }

    @Test
    fun `init - should not process when module is null`() = runTest {
        // When
        moduleStateFlow.value = null
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(exactly = 0) { fieldDataProcessor.processModuleData(any()) }
    }

    // MARK: - startModule Tests

    @Test
    fun `startModule - should call onUiUpdateEvent with ModuleStart`() {
        // Given
        val mockModule = mockk<SequentialPageCaptureModule>()
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.startModule(mockModule)

        // Then
        verify { spyViewModel.onUiUpdateEvent(any<ModuleStart>()) }
    }

    // MARK: - getModule Tests

    @Test
    fun `getModule - should return current module`() = runTest {
        // Given
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val result = viewModel.getModule()

        // Then
        assertEquals(testModule, result)
    }

    // MARK: - onUiUpdateEvent Tests

    @Test
    fun `onUiUpdateEvent - ModuleStart should process start module`() {
        // Given
        val event = ModuleStart(testModule)
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify { spyViewModel.processStartModule(testModule) }
    }

    @Test
    fun `onUiUpdateEvent - FieldChanged should process input stream when key is not null`() {
        // Given
        val event = FieldChanged("field1", "test_value", null)
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify { spyViewModel.processInputStream("field1", "test_value", null) }
    }

    @Test
    fun `onUiUpdateEvent - FieldChanged should not process input stream when key is null`() {
        // Given
        val event = FieldChanged(null, "test_value", null)
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify(exactly = 0) { spyViewModel.processInputStream(any(), any(), any()) }
    }

    @Test
    fun `onUiUpdateEvent - InvestmentProductSelected should process selected product`() {
        // Given
        val event = InvestmentProductSelected(1)
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify { spyViewModel.processSelectedProduct(1) }
    }

    @Test
    fun `onUiUpdateEvent - InvestmentAccountSelected should process selected investment account`() {
        // Given
        val mockAccount = mockk<SequentialPageInvestmentAccount>()
        val event = InvestmentAccountSelected(mockAccount, mockk())
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify { spyViewModel.processSelectedInvestmentAccount(mockAccount) }
    }

    @Test
    fun `onUiUpdateEvent - NextClickEvent should process continue event`() {
        // Given
        val event = NextClickEvent("Continue", "next_action", true)
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify { spyViewModel.processContinueEvent("Continue", "next_action", true) }
    }

    @Test
    fun `onUiUpdateEvent - PageBackEvent should process on back event`() {
        // Given
        val event = PageBackEvent("back_action", "Back")
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify { spyViewModel.processOnBackEvent("back_action") }
    }

    @Test
    fun `onUiUpdateEvent - HyperlinkClicked should process hyperlink click event`() {
        // Given
        val event = HyperlinkClicked
        val spyViewModel = spyk(viewModel)

        // When
        spyViewModel.onUiUpdateEvent(event)

        // Then
        verify { spyViewModel.processHyperlinkClickEvent() }
    }

    @Test
    fun `onUiUpdateEvent - ClearError should clear error in state`() = runTest {
        // Given
        val event = ClearError
        
        // Set initial state with error
        val initialState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(error = "Test error")
        )
        viewModel.pageMutableStateFlow.value = initialState

        // When
        viewModel.onUiUpdateEvent(event)

        // Then
        val finalState = viewModel.pageState.first()
        assertEquals(null, finalState.sequentialPageState.error)
    }

    // MARK: - processStartModule Tests

    @Test
    fun `processStartModule - should set module and update state correctly`() = runTest {
        // Given
        val testFieldItem = SequentialPageFieldCaptureItem(
            key = "field1", regex = "\\d+", value = "123", heading = "Test Field",
            minDate = null, maxDate = null, keyBoardType = KeyboardType.NUMBER,
            type = SequentialPageCaptureComponentType.INPUT_FIELD,
            errorState = SequentialPageUIErrorState(), title = "Test Title",
            helpInformation = null, contentType = null, radioInputs = null,
            noRadioOptionSelectedText = null, text = null, helpText = null,
            action = null, radioOptions = null, maxInputLength = 10
        )

        val pageModuleData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = listOf(testFieldItem),
            fieldCount = 1,
            buttonState = true
        )

        val accordionData = mapOf("field1" to listOf(SequentialPageHelpInformation("Help", "Description")))
        val investmentProducts = listOf(
            SequentialPageProductPickerView(
                title = "Product 1", analyticsSelectedProductTag = "product1",
                icon = null, index = 0, selectedProductAccessibilityTag = "Product 1",
                isProductSelected = false, payloadKey = "product_key"
            )
        )

        every { fieldDataProcessor.processModuleData(any()) } returns pageModuleData
        every { fieldDataProcessor.getAccordionData(any()) } returns accordionData
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns investmentProducts
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.processStartModule(testModule)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.pageState.first()
        assertEquals(listOf(testFieldItem), state.sequentialPageState.items)
        assertEquals(accordionData, state.sequentialPageState.accordionState.accordionPair)
        assertEquals(investmentProducts, state.productPickerState.products)
        assertEquals(1, state.inputViewFieldCount)
        assertEquals(testModule.sequentialPageSections, state.sequentialPageSections)
        assertEquals(SequentialPageCaptureComponentType.INPUT_FIELD, state.viewType)

        verify { analyticsManager.logScreenLoad("test_page") }
    }

    @Test
    fun `processStartModule - should handle empty analytics page tag`() = runTest {
        // Given
        val sectionsWithNullTag = SequentialPageSections(analyticsPageTag = null)
        every { testModule.sequentialPageSections } returns sectionsWithNullTag
        
        val pageModuleData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = emptyList(), fieldCount = 0, buttonState = true
        )
        
        every { fieldDataProcessor.processModuleData(any()) } returns pageModuleData
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.processStartModule(testModule)

        // Then
        verify { analyticsManager.logScreenLoad("") }
    }

    // MARK: - processInputStream Tests

    @Test
    fun `processInputStream - should process input and update state with valid input`() = runTest {
        // Given
        val fieldKey = "field1"
        val inputValue = "test_value"
        val mockItem = SequentialPageFieldCaptureItem(
            key = fieldKey, regex = ".*", value = "old_value", heading = null,
            minDate = null, maxDate = null, keyBoardType = null,
            type = SequentialPageCaptureComponentType.INPUT_FIELD,
            errorState = SequentialPageUIErrorState(), title = null,
            helpInformation = null, contentType = null, radioInputs = null,
            noRadioOptionSelectedText = null, text = null, helpText = null,
            action = null, radioOptions = null, maxInputLength = null
        )

        val initialState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(items = listOf(mockItem))
        )
        viewModel.pageMutableStateFlow.value = initialState

        val pageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = inputValue,
            pageItem = mockItem
        )

        val validationResult = SequentialPageFieldValidationResult.ValidationResult(
            isValid = true,
            message = null
        )

        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns pageInputFieldData
        every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns validationResult
        every { testModule.sequentialPageDataList } returns listOf(
            SequentialPageModuleResponse(
                id = fieldKey, regex = null, value = null, heading = null,
                minDate = null, maxDate = null, keyboard = null, type = null,
                invalidInputText = null, title = null, helpInformation = null,
                contentType = null, radioInputs = null, noRadioOptionSelectedText = null,
                text = null, helpText = null, action = null, radioOptions = null,
                minDateInvalidText = null, maxDateInvalidText = null
            )
        )

        // Set the module first
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processInputStream(fieldKey, inputValue, null)

        // Then
        val finalState = viewModel.pageState.first()
        val updatedItem = finalState.sequentialPageState.items.first()
        assertEquals(inputValue, updatedItem.value)
        assertFalse(updatedItem.errorState.isError)

        verify { fieldDataProcessor.getCappedValue(any(), any(), fieldKey, inputValue, null) }
        verify { sequentialPageValidationDelegator.validateField(inputValue, mockItem, null, any()) }
    }

    @Test
    fun `processInputStream - should handle validation error and show error when showErrorsOnInvalidFields is true`() = runTest {
        // Given
        val fieldKey = "field1"
        val inputValue = "invalid_value"
        val errorMessage = "Invalid input"
        
        val mockItem = SequentialPageFieldCaptureItem(
            key = fieldKey, regex = "\\d+", value = "123", heading = null,
            minDate = null, maxDate = null, keyBoardType = null,
            type = SequentialPageCaptureComponentType.INPUT_FIELD,
            errorState = SequentialPageUIErrorState(), title = null,
            helpInformation = null, contentType = null, radioInputs = null,
            noRadioOptionSelectedText = null, text = null, helpText = null,
            action = null, radioOptions = null, maxInputLength = null
        )

        val initialState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(items = listOf(mockItem))
        )
        viewModel.pageMutableStateFlow.value = initialState

        val pageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = inputValue,
            pageItem = mockItem
        )

        val validationResult = SequentialPageFieldValidationResult.ValidationResult(
            isValid = false,
            message = errorMessage
        )

        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns pageInputFieldData
        every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns validationResult
        every { testModule.sequentialPageDataList } returns listOf(
            SequentialPageModuleResponse(
                id = fieldKey, regex = null, value = null, heading = null,
                minDate = null, maxDate = null, keyboard = null, type = null,
                invalidInputText = null, title = null, helpInformation = null,
                contentType = null, radioInputs = null, noRadioOptionSelectedText = null,
                text = null, helpText = null, action = null, radioOptions = null,
                minDateInvalidText = null, maxDateInvalidText = null
            )
        )

        // Set the module and enable error display
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Enable error display by triggering a continue event first
        viewModel.showErrorsOnInvalidFields = true

        // When
        viewModel.processInputStream(fieldKey, inputValue, null)

        // Then
        val finalState = viewModel.pageState.first()
        val updatedItem = finalState.sequentialPageState.items.first()
        assertEquals(inputValue, updatedItem.value)
        assertTrue(updatedItem.errorState.isError)
        assertEquals(errorMessage, updatedItem.errorState.errorMessage)
    }

    // MARK: - processContinueEvent Tests

    @Test
    fun `processContinueEvent - should launch journey when validation passes for OTP_VERIFICATION_STATUS_CHECK`() = runTest {
        // Given
        val label = "Continue"
        val actionId = "continue_action"
        val progressState = true

        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(),
            isPageDataValid = true
        )

        val mockAction = MvcAction(stepId = "OTP_VERIFICATION_STATUS_CHECK")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { testModule.getAction(actionId) } returns mockAction
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processContinueEvent(label, actionId, progressState)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { analyticsManager.logButtonClick(label) }
        verify { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) }
        verify { testModule.getAction(actionId) }
        verify { testModule.attachPayLoadToMvcAction(null, mockAction) }
        verify { mockJourneyAction.executeAction(mockAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
        assertTrue(mockAction.showProgress == progressState)
    }

    @Test
    fun `processContinueEvent - should handle PRODUCT_SELECTION_SAVE with payload creation`() = runTest {
        // Given
        val label = "Save"
        val actionId = "save_action"
        
        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(),
            isPageDataValid = true
        )

        val mockAction = MvcAction(stepId = "PRODUCT_SELECTION_SAVE")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()
        val mockPayload = mockk<SequentialPageCaptureRequestData>()
        val mockSelectedProduct = SequentialPageInvestmentProduct(
            title = "Test Product",
            description = "Test Description",
            analyticsOptionTag = "test_tag",
            icon = null
        )

        val testModuleResponse = SequentialPageModuleResponse(
            id = "product_field", regex = null, value = null, heading = null,
            minDate = null, maxDate = null, keyboard = null, type = null,
            invalidInputText = null, title = null, helpInformation = null,
            contentType = null, radioInputs = null, noRadioOptionSelectedText = null,
            text = null, helpText = null, action = null, radioOptions = null,
            minDateInvalidText = null, maxDateInvalidText = null
        )

        val initialState = SequentialPageState(
            productPickerState = SequentialPageProductPickerState(
                selectedProductData = mockSelectedProduct
            )
        )
        viewModel.pageMutableStateFlow.value = initialState

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { testModule.getAction(actionId) } returns mockAction
        every { testModule.sequentialPageDataList } returns listOf(testModuleResponse)
        every { payLoadProcessor.createPayLoad(any<String>(), any<String>()) } returns mockPayload
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processContinueEvent(label, actionId, false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { payLoadProcessor.createPayLoad("product_field", any()) }
        verify { testModule.attachPayLoadToMvcAction(mockPayload, mockAction) }
        verify { mockJourneyAction.executeAction(mockAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    @Test
    fun `processContinueEvent - should handle default case with items payload`() = runTest {
        // Given
        val label = "Continue"
        val actionId = "continue_action"
        
        val mockItem = SequentialPageFieldCaptureItem(
            key = "field1", regex = null, value = "test", heading = null,
            minDate = null, maxDate = null, keyBoardType = null,
            type = null, errorState = SequentialPageUIErrorState(),
            title = null, helpInformation = null, contentType = null,
            radioInputs = null, noRadioOptionSelectedText = null,
            text = null, helpText = null, action = null, radioOptions = null,
            maxInputLength = null
        )

        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = listOf(mockItem),
            isPageDataValid = true
        )

        val mockAction = MvcAction(stepId = "OTHER_STEP")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()
        val mockPayload = mockk<SequentialPageCaptureRequestData>()

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { testModule.getAction(actionId) } returns mockAction
        every { payLoadProcessor.createPayLoad(listOf(mockItem)) } returns mockPayload
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processContinueEvent(label, actionId, true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { payLoadProcessor.createPayLoad(listOf(mockItem)) }
        verify { testModule.attachPayLoadToMvcAction(mockPayload, mockAction) }
    }

    @Test
    fun `processContinueEvent - should update state and show errors when validation fails`() = runTest {
        // Given
        val label = "Continue"
        val actionId = "continue_action"
        
        val mockErrorItem = SequentialPageFieldCaptureItem(
            key = "field1", regex = null, value = "", heading = null,
            minDate = null, maxDate = null, keyBoardType = null,
            type = null, errorState = SequentialPageUIErrorState(isError = true, errorMessage = "Required"),
            title = null, helpInformation = null, contentType = null,
            radioInputs = null, noRadioOptionSelectedText = null,
            text = null, helpText = null, action = null, radioOptions = null,
            maxInputLength = null
        )

        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = listOf(mockErrorItem),
            isPageDataValid = false
        )

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processContinueEvent(label, actionId, true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.pageState.first()
        assertEquals(listOf(mockErrorItem), finalState.sequentialPageState.items)
        assertEquals(0, finalState.firstErrorInputViewIndex)
        
        // Verify no journey was launched
        verify(exactly = 0) { testModule.attachPayLoadToMvcAction(any(), any()) }
    }

    @Test
    fun `processContinueEvent - should handle null action gracefully`() = runTest {
        // Given
        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(),
            isPageDataValid = true
        )

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { testModule.getAction(any()) } returns null

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processContinueEvent("Continue", "invalid_action", true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(exactly = 0) { testModule.attachPayLoadToMvcAction(any(), any()) }
    }

    // MARK: - processSelectedProduct Tests

    @Test
    fun `processSelectedProduct - should call product handler and update state`() = runTest {
        // Given
        val productId = 1
        val currentState = SequentialPageState()
        val updatedState = currentState.copy(
            productPickerState = SequentialPageProductPickerState(selectedProductId = productId)
        )

        viewModel.pageMutableStateFlow.value = currentState
        every { productHandler.processSelectedProduct(any(), any(), any()) } returns updatedState

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processSelectedProduct(productId)

        // Then
        verify { productHandler.processSelectedProduct(currentState, productId, testModule.sequentialPageDataList) }
        assertEquals(updatedState, viewModel.pageMutableStateFlow.value)
    }

    // MARK: - processSelectedInvestmentAccount Tests

    @Test
    fun `processSelectedInvestmentAccount - should call investment account handler`() {
        // Given
        val mockAccount = mockk<SequentialPageInvestmentAccount>()
        every { investmentAccountHandler.processSelectedInvestmentAccount(any()) } just Runs

        // When
        viewModel.processSelectedInvestmentAccount(mockAccount)

        // Then
        verify { investmentAccountHandler.processSelectedInvestmentAccount(mockAccount) }
    }

    // MARK: - processOnBackEvent Tests

    @Test
    fun `processOnBackEvent - should log button click and launch journey`() = runTest {
        // Given
        val actionId = "back_action"
        val mockAction = MvcAction(stepId = "BACK_STEP")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()
        val mock

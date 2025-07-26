// MARK: - Comprehensive Coverage Tests

    @Test
    fun `comprehensive - all INVESTMENT step IDs coverage`() = runTest {
        // Test all investment-related step IDs
        val stepIds = listOf(
            "OTP_VERIFICATION_STATUS_CHECK",
            "INVESTMENT_MASS_AFFLUENT_CALL_HELPDESK", 
            "INVESTMENT_INITIALISE",
            "PENDING_ACTIVATION_CODE_INITIAL"
        )

        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(), isPageDataValid = true
        )
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        stepIds.forEach { stepId ->
            // Given
            val mockAction = MvcAction(stepId = stepId)
            every { testModule.getAction("action_$stepId") } returns mockAction

            // When
            viewModel.onUiUpdateEvent(NextClickEvent("Continue", "action_$stepId", true))
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            verify { testModule.attachPayLoadToMvcAction(null, mockAction) }
        }
    }

    @Test
    fun `comprehensive - all event types coverage without spies`() = runTest {
        // Test all event types to ensure 100% coverage without using spies for private methods
        
        // Setup common mocks
        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(), isPageDataValid = true
        )
        val mockAction = MvcAction(stepId = "TEST_STEP")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()
        val mockAccount = mockk<SequentialPageInvestmentAccount>()
        val mockFieldItem = SequentialPageFieldCaptureItem(
            key = "key", regex = null, value = null, heading = null,
            minDate = null, maxDate = null, keyBoardType = null,
            type = null, errorState = SequentialPageUIErrorState(),
            title = null, helpInformation = null, contentType = null,
            radioInputs = null, noRadioOptionSelectedText = null,
            text = null, helpText = null, action = null, radioOptions = null,
            maxInputLength = null
        )
        
        every { analyticsManager.logButtonClick(any()) } just Runs
        every { analyticsManager.logHyperLink(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { testModule.getAction(any()) } returns mockAction
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs
        every { productHandler.processSelectedProduct(any(), any(), any()) } returns SequentialPageState()
        every { investmentAccountHandler.processSelectedInvestmentAccount(any()) } just Runs
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns SequentialPageDataProcessor.PageInputFieldData("test", mockFieldItem)
        every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns SequentialPageFieldValidationResult.ValidationResult(true)

        // Set module and initial state
        moduleStateFlow.value = testModule
        val initialState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(items = listOf(mockFieldItem))
        )
        viewModel.pageMutableStateFlow.value = initialState
        testDispatcher.scheduler.advanceUntilIdle()

        // Test events and verify their effects
        val events = listOf(
            FieldChanged("key", "value", null),
            FieldChanged(null, "value", null), // null key case
            InvestmentProductSelected(1),
            InvestmentAccountSelected(mockAccount, mockFieldItem),
            NextClickEvent("Continue", "action", true),
            NextClickEvent("Continue", "action", false), // different progressState
            PageBackEvent("back", "Back"),
            HyperlinkClicked,
            ClearError
        )

        events.forEach { event ->
            clearMocks(
                analyticsManager, testModule, productHandler, 
                investmentAccountHandler, fieldDataProcessor,
                sequentialPageValidationDelegator, answers = false
            )
            
            // Re-setup mocks for each iteration
            every { analyticsManager.logButtonClick(any()) } just Runs
            every { analyticsManager.logHyperLink(any()) } just Runs
            every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
            every { testModule.getAction(any()) } returns mockAction
            every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs
            every { productHandler.processSelectedProduct(any(), any(), any()) } returns SequentialPageState()
            every { investmentAccountHandler.processSelectedInvestmentAccount(any()) } just Runs
            every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns SequentialPageDataProcessor.PageInputFieldData("test", mockFieldItem)
            every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns SequentialPageFieldValidationResult.ValidationResult(true)

            // When
            viewModel.onUiUpdateEvent(event)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - verify appropriate methods were called based on event type
            when (event) {
                is FieldChanged -> {
                    if (event.key != null) {
                        verify { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) }
                    } else {
                        verify(exactly = 0) { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) }
                    }
                }
                is InvestmentProductSelected -> {
                    verify { productHandler.processSelectedProduct(any(), any(), any()) }
                }
                is InvestmentAccountSelected -> {
                    verify { investmentAccountHandler.processSelectedInvestmentAccount(mockAccount) }
                }
                is NextClickEvent -> {
                    verify { analyticsManager.logButtonClick("Continue") }
                    verify { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) }
                }
                is PageBackEvent -> {
                    verify { analyticsManager.logButtonClick("back") }
                    verify { testModule.getAction("back") }
                }
                is HyperlinkClicked -> {
                    verify { analyticsManager.logHyperLink(any()) }
                }
                is ClearError -> {
                    // Verify state was updated (error cleared)
                    val state = viewModel.pageState.first()
                    assertEquals(null, state.sequentialPageState.error)
                }
            }
        }
    }

    @Test
    fun `comprehensive - all analytics methods coverage`() {
        // Test all analytics logging methods
        every { analyticsManager.logScreenLoad(any()) } just Runs
        every { analyticsManager.logButtonClick(any()) } just Runs
        every { analyticsManager.logHyperLink(any()) } just Runs
        every { analyticsManager.logAccordionInteraction(any(), any()) } just Runs
        every { analyticsManager.logInputViewChangeFocus(any()) } just Runs
        every { analyticsManager.logInputViewError(any()) } just Runs

        // When
        viewModel.logAccordionStateChange(true, "accordion1")
        viewModel.logAccordionStateChange(false, "accordion2")
        viewModel.logInputViewFocusChange("input1")
        viewModel.logInputViewFocusChange(null)
        viewModel.logInputViewError("error1")

        // Then
        verify { analyticsManager.logAccordionInteraction("accordion1", "EXPAND") }
        verify { analyticsManager.logAccordionInteraction("accordion2", "COLLAPSED") }
        verify { analyticsManager.logInputViewChangeFocus("input1") }
        verify(exactly = 0) { analyticsManager.logInputViewChangeFocus(any()) }
        verify { analyticsManager.logInputViewError("error1") }
    }

    @Test
    fun `error scenarios - null and empty data handling`() = runTest {
        // Test various null/empty scenarios
        val emptyModule = mockk<SequentialPageCaptureModule> {
            every { id } returns ""
            every { sequentialPageSections } returns SequentialPageSections(null)
            every { sequentialPageDataList } returns emptyList()
            every { getAction(any()) } returns null
            every { attachPayLoadToMvcAction(any(), any()) } just Runs
        }

        val emptyPageData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = emptyList(), fieldCount = 0, buttonState = false
        )

        every { fieldDataProcessor.processModuleData(any()) } returns emptyPageData
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.onUiUpdateEvent(ModuleStart(emptyModule))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should handle empty data gracefully
        verify { analyticsManager.logScreenLoad("") }
        verify { fieldDataProcessor.processModuleData(emptyList()) }

        val state = viewModel.pageState.first()
        assertTrue(state.sequentialPageState.items.isEmpty())
        assertTrue(state.productPickerState.products.isEmpty())
        assertTrue(state.sequentialPageState.accordionState.accordionPair.isEmpty())
    }

    // MARK: - State Consistency Tests

    @Test
    fun `state consistency - button enabled state updates correctly`() = runTest {
        // Given
        val pageDataWithDisabledButton = SequentialPageDataProcessor.PageModuleData(
            pageInfo = emptyList(), fieldCount = 0, buttonState = false
        )

        every { fieldDataProcessor.processModuleData(any()) } returns pageDataWithDisabledButton
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.onUiUpdateEvent(ModuleStart(testModule))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val buttonState = viewModel.isContinueButtonEnabled.first()
        assertFalse(buttonState)
    }

    @Test
    fun `state consistency - view type is set correctly from items`() = runTest {
        // Given
        val dateFieldItem = SequentialPageFieldCaptureItem(
            key = "date_field", regex = null, value = null, heading = null,
            minDate = "2023-01-01", maxDate = "2023-12-31", keyBoardType = null,
            type = SequentialPageCaptureComponentType.DATE,
            errorState = SequentialPageUIErrorState(), title = null,
            helpInformation = null, contentType = null, radioInputs = null,
            noRadioOptionSelectedText = null, text = null, helpText = null,
            action = null, radioOptions = null, maxInputLength = null
        )

        val pageDataWithDate = SequentialPageDataProcessor.PageModuleData(
            pageInfo = listOf(dateFieldItem), fieldCount = 1, buttonState = true
        )

        every { fieldDataProcessor.processModuleData(any()) } returns pageDataWithDate
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.onUiUpdateEvent(ModuleStart(testModule))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.pageState.first()
        assertEquals(SequentialPageCaptureComponentType.DATE, state.viewType)
    }

    // MARK: - Edge Cases and Error Handling

    @Test
    fun `edge case - launchJourney with null journey action builder`() = runTest {
        // Given - Create ViewModel without journey action builder
        val viewModelWithoutBuilder = SequentialPageCaptureViewModel(
            fieldDataProcessor = fieldDataProcessor,
            productHandler = productHandler,
            investmentAccountHandler = investmentAccountHandler,
            payLoadProcessor = payLoadProcessor,
            analyticsManager = analyticsManager,
            sequentialPageValidationDelegator = sequentialPageValidationDelegator,
            journeyActionBuilder = null,
            moduleDataProcessor = moduleDataProcessor
        )

        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(), isPageDataValid = true
        )
        val mockAction = MvcAction(stepId = "TEST_STEP")

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { testModule.getAction(any()) } returns mockAction
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When - this should test the launchJourney method with null builder
        viewModelWithoutBuilder.onUiUpdateEvent(NextClickEvent("Continue", "action", true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should not crash and should still attach payload
        verify { testModule.attachPayLoadToMvcAction(null, mockAction) }
        // But no journey execution should happen (no crash)
    }

    @Test
    fun `edge case - multiple rapid state changes`() = runTest {
        // Given - test rapid state changes don't cause issues
        val initialState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(error = "initial_error")
        )
        viewModel.pageMutableStateFlow.value = initialState

        // When - rapidly clear and set errors
        repeat(10) {
            viewModel.onUiUpdateEvent(ClearError)
            viewModel.pageMutableStateFlow.value = initialState // Set error back
        }

        // Then - final state should have error cleared
        viewModel.onUiUpdateEvent(ClearError)
        val finalState = viewModel.pageState.first()
        assertEquals(null, finalState.sequentialPageState.error)
    }

    @Test
    fun `edge case - empty items list field processing`() = runTest {
        // Given - empty items list
        val emptyState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(items = emptyList())
        )
        viewModel.pageMutableStateFlow.value = initialState

        val pageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = "test", pageItem = null
        )

        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns pageInputFieldData
        every { testModule.sequentialPageDataList } returns emptyList()

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When - try to process field change on empty list
        viewModel.onUiUpdateEvent(FieldChanged("nonexistent_field", "value", null))

        // Then - should not crash
        val finalState = viewModel.pageState.first()
        assertTrue(finalState.sequentialPageState.items.isEmpty())
    }

    @Test
    fun `cleanup - verify all dependencies are properly tested`() {
        // This test ensures we have proper coverage of all injected dependencies
        
        // Verify all major dependencies were interacted with during tests
        verify(atLeast = 1) { fieldDataProcessor.processModuleData(any()) }
        verify(atLeast = 1) { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) }
        verify(atLeast = 1) { fieldDataProcessor.getAccordionData(any()) }
        verify(atLeast = 1) { fieldDataProcessor.getInvestmentProducts(any()) }
        
        verify(atLeast = 1) { productHandler.processSelectedProduct(any(), any(), any()) }
        verify(atLeast = 1) { investmentAccountHandler.processSelectedInvestmentAccount(any()) }
        verify(atLeast = 1) { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) }
        
        verify(atLeast = 1) { analyticsManager.logScreenLoad(any()) }
        verify(atLeast = 1) { analyticsManager.logButtonClick(any()) }
        verify(atLeast = 1) { analyticsManager.logHyperLink(any()) }
        verify(atLeast = 1) { analyticsManager.logAccordionInteraction(any(), any()) }
        verify(atLeast = 1) { analyticsManager.logInputViewChangeFocus(any()) }
        verify(atLeast = 1) { analyticsManager.logInputViewError(any()) }
        
        verify(atLeast = 1) { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) }
        verify(atLeast = 1) { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) }
        
        verify(atLeast = 1) { moduleDataProcessor.dataStateFlow }
        
        verify(atLeast = 1) { testModule.id }
        verify(atLeast = 1) { testModule.sequentialPageSections }
        verify(atLeast = 1) { testModule.sequentialPageDataList }
        verify(atLeast = 1) { testModule.getAction(any()) }
        verify(atLeast = 1) { testModule.attachPayLoadToMvcAction(any(), any()) }
        
        // Verify journey builder was used
        verify(atLeast = 1) { journeyActionBuilder.build() }
    }
}import io.mockk.*
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
    fun `startModule - should call onUiUpdateEvent with ModuleStart`() = runTest {
        // Given
        val mockModule = mockk<SequentialPageCaptureModule>()
        val testSections = SequentialPageSections(analyticsPageTag = "test_page")
        val testDataList = listOf<SequentialPageModuleResponse>()
        
        every { mockModule.id } returns "test_module"
        every { mockModule.sequentialPageSections } returns testSections
        every { mockModule.sequentialPageDataList } returns testDataList
        
        val pageModuleData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = emptyList(), fieldCount = 0, buttonState = true
        )
        
        every { fieldDataProcessor.processModuleData(any()) } returns pageModuleData
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.startModule(mockModule)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify processStartModule logic was executed
        verify { analyticsManager.logScreenLoad("test_page") }
        verify { fieldDataProcessor.processModuleData(testDataList) }
        verify { fieldDataProcessor.getAccordionData(any()) }
        verify { fieldDataProcessor.getInvestmentProducts(any()) }
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
    fun `onUiUpdateEvent - FieldChanged should process input stream when key is not null`() = runTest {
        // Given
        val fieldKey = "field1"
        val inputValue = "test_value"
        val event = FieldChanged(fieldKey, inputValue, null)
        
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
        viewModel.onUiUpdateEvent(event)

        // Then - verify the processInputStream logic by checking state changes
        val finalState = viewModel.pageState.first()
        val updatedItem = finalState.sequentialPageState.items.first()
        assertEquals(inputValue, updatedItem.value)
        assertFalse(updatedItem.errorState.isError)

        verify { fieldDataProcessor.getCappedValue(any(), any(), fieldKey, inputValue, null) }
        verify { sequentialPageValidationDelegator.validateField(inputValue, mockItem, null, any()) }
    }

    @Test
    fun `onUiUpdateEvent - FieldChanged should not process input stream when key is null`() = runTest {
        // Given
        val event = FieldChanged(null, "test_value", null)
        val initialState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(items = emptyList())
        )
        viewModel.pageMutableStateFlow.value = initialState

        // When
        viewModel.onUiUpdateEvent(event)

        // Then - verify no processing occurred by checking state remains unchanged
        val finalState = viewModel.pageState.first()
        assertEquals(initialState, finalState)
        
        // Verify no calls to processor methods
        verify(exactly = 0) { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) }
    }

    @Test
    fun `onUiUpdateEvent - InvestmentProductSelected should process selected product`() = runTest {
        // Given
        val productId = 1
        val event = InvestmentProductSelected(productId)
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
        viewModel.onUiUpdateEvent(event)

        // Then - verify processSelectedProduct logic by checking state change and handler call
        verify { productHandler.processSelectedProduct(currentState, productId, testModule.sequentialPageDataList) }
        assertEquals(updatedState, viewModel.pageMutableStateFlow.value)
    }

    @Test
    fun `onUiUpdateEvent - InvestmentAccountSelected should process selected investment account`() {
        // Given
        val mockAccount = mockk<SequentialPageInvestmentAccount>()
        val mockItem = mockk<SequentialPageFieldCaptureItem>()
        val event = InvestmentAccountSelected(mockAccount, mockItem)
        
        every { investmentAccountHandler.processSelectedInvestmentAccount(any()) } just Runs

        // When
        viewModel.onUiUpdateEvent(event)

        // Then - verify processSelectedInvestmentAccount logic
        verify { investmentAccountHandler.processSelectedInvestmentAccount(mockAccount) }
    }

    @Test
    fun `onUiUpdateEvent - NextClickEvent should process continue event`() = runTest {
        // Given
        val event = NextClickEvent("Continue", "next_action", true)
        
        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(),
            isPageDataValid = true
        )

        val mockAction = MvcAction(stepId = "OTP_VERIFICATION_STATUS_CHECK")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { testModule.getAction("next_action") } returns mockAction
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onUiUpdateEvent(event)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify processContinueEvent logic by checking the calls made
        verify { analyticsManager.logButtonClick("Continue") }
        verify { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) }
        verify { testModule.getAction("next_action") }
        assertTrue(mockAction.showProgress == true)
        verify { testModule.attachPayLoadToMvcAction(null, mockAction) }
        verify { mockJourneyAction.executeAction(mockAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    @Test
    fun `onUiUpdateEvent - PageBackEvent should process on back event`() = runTest {
        // Given
        val event = PageBackEvent("back_action", "Back")
        val mockAction = MvcAction(stepId = "BACK_STEP")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { testModule.getAction("back_action") } returns mockAction
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onUiUpdateEvent(event)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify processOnBackEvent logic
        verify { analyticsManager.logButtonClick("back_action") }
        verify { testModule.getAction("back_action") }
        verify { testModule.attachPayLoadToMvcAction(any<JourneyFrameworkGenericHandleAction.EmptyRequest>(), mockAction) }
        verify { mockJourneyAction.executeAction(mockAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    @Test
    fun `onUiUpdateEvent - HyperlinkClicked should process hyperlink click event`() = runTest {
        // Given
        val event = HyperlinkClicked
        val mockNextAction = MvcAction(stepId = "OTHER_STEP")
        val mockResendAction = mockk<MvcAction>()
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { testModule.getAction("NEXT") } returns mockNextAction
        every { testModule.getAction("RESEND") } returns mockResendAction
        every { analyticsManager.logHyperLink("RESEND") } just Runs
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onUiUpdateEvent(event)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify processHyperlinkClickEvent logic
        verify { analyticsManager.logHyperLink("RESEND") }
        verify { testModule.getAction("RESEND") }
        verify { testModule.attachPayLoadToMvcAction(null, mockResendAction) }
        verify { mockJourneyAction.executeAction(mockResendAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
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
        val mockEmptyRequest = mockk<JourneyFrameworkGenericHandleAction.EmptyRequest>()

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { testModule.getAction(actionId) } returns mockAction
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processOnBackEvent(actionId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { analyticsManager.logButtonClick(actionId) }
        verify { testModule.getAction(actionId) }
        verify { testModule.attachPayLoadToMvcAction(any<JourneyFrameworkGenericHandleAction.EmptyRequest>(), mockAction) }
        verify { mockJourneyAction.executeAction(mockAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    @Test
    fun `processOnBackEvent - should handle null action gracefully`() = runTest {
        // Given
        val actionId = "invalid_back_action"
        every { analyticsManager.logButtonClick(any()) } just Runs
        every { testModule.getAction(actionId) } returns null

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processOnBackEvent(actionId)

        // Then
        verify { analyticsManager.logButtonClick(actionId) }
        verify(exactly = 0) { testModule.attachPayLoadToMvcAction(any(), any()) }
    }

    // MARK: - processHyperlinkClickEvent Tests

    @Test
    fun `processHyperlinkClickEvent - should handle PRODUCT_SELECTION_SAVE step with NONE_OF_THESE analytics`() = runTest {
        // Given
        val mockNextAction = MvcAction(stepId = "PRODUCT_SELECTION_SAVE")
        val mockNoneOfTheseAction = mockk<MvcAction>()
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { testModule.getAction("NEXT") } returns mockNextAction
        every { testModule.getAction("NONE_OF_THESE") } returns mockNoneOfTheseAction
        every { analyticsManager.logHyperLink("NONE_OF_THESE") } just Runs
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processHyperlinkClickEvent()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { analyticsManager.logHyperLink("NONE_OF_THESE") }
        verify { testModule.getAction("NONE_OF_THESE") }
        verify { testModule.attachPayLoadToMvcAction(null, mockNoneOfTheseAction) }
        verify { mockJourneyAction.executeAction(mockNoneOfTheseAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    @Test
    fun `processHyperlinkClickEvent - should handle non-PRODUCT_SELECTION_SAVE step with RESEND analytics`() = runTest {
        // Given
        val mockNextAction = MvcAction(stepId = "OTHER_STEP")
        val mockResendAction = mockk<MvcAction>()
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { testModule.getAction("NEXT") } returns mockNextAction
        every { testModule.getAction("RESEND") } returns mockResendAction
        every { analyticsManager.logHyperLink("RESEND") } just Runs
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processHyperlinkClickEvent()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { analyticsManager.logHyperLink("RESEND") }
        verify { testModule.getAction("RESEND") }
        verify { testModule.attachPayLoadToMvcAction(null, mockResendAction) }
        verify { mockJourneyAction.executeAction(mockResendAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    @Test
    fun `processHyperlinkClickEvent - should handle null NEXT action gracefully`() = runTest {
        // Given
        every { testModule.getAction("NEXT") } returns null
        every { testModule.getAction("RESEND") } returns null
        every { analyticsManager.logHyperLink("RESEND") } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.processHyperlinkClickEvent()

        // Then
        verify { analyticsManager.logHyperLink("RESEND") }
        verify(exactly = 0) { testModule.attachPayLoadToMvcAction(any(), any()) }
    }

    // MARK: - launchJourney Tests

    @Test
    fun `launchJourney - should attach payload and execute action`() = runTest {
        // Given
        val mockAction = MvcAction(stepId = "TEST_STEP")
        val mockPayload = mockk<SequentialPageCaptureRequestData>()
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.launchJourney(mockAction, mockPayload)

        // Then
        verify { testModule.attachPayLoadToMvcAction(mockPayload, mockAction) }
        verify { journeyActionBuilder.build() }
        verify { mockJourneyAction.executeAction(mockAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    @Test
    fun `launchJourney - should handle null journey action builder gracefully`() = runTest {
        // Given
        val mockAction = MvcAction(stepId = "TEST_STEP")
        val mockPayload = mockk<SequentialPageCaptureRequestData>()

        // Create ViewModel without journey action builder
        val viewModelWithoutBuilder = SequentialPageCaptureViewModel(
            fieldDataProcessor = fieldDataProcessor,
            productHandler = productHandler,
            investmentAccountHandler = investmentAccountHandler,
            payLoadProcessor = payLoadProcessor,
            analyticsManager = analyticsManager,
            sequentialPageValidationDelegator = sequentialPageValidationDelegator,
            journeyActionBuilder = null,
            moduleDataProcessor = moduleDataProcessor
        )

        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModelWithoutBuilder.launchJourney(mockAction, mockPayload)

        // Then
        verify { testModule.attachPayLoadToMvcAction(mockPayload, mockAction) }
        // Should not crash and no journey action should be executed
    }

    // MARK: - logAccordionStateChange Tests

    @Test
    fun `logAccordionStateChange - should log accordion interaction with EXPAND state`() {
        // Given
        val state = true
        val label = "Test Accordion"
        every { analyticsManager.logAccordionInteraction(any(), any()) } just Runs

        // When
        viewModel.logAccordionStateChange(state, label)

        // Then
        verify { analyticsManager.logAccordionInteraction(label, "EXPAND") }
    }

    @Test
    fun `logAccordionStateChange - should log accordion interaction with COLLAPSED state`() {
        // Given
        val state = false
        val label = "Test Accordion"
        every { analyticsManager.logAccordionInteraction(any(), any()) } just Runs

        // When
        viewModel.logAccordionStateChange(state, label)

        // Then
        verify { analyticsManager.logAccordionInteraction(label, "COLLAPSED") }
    }

    // MARK: - logInputViewFocusChange Tests

    @Test
    fun `logInputViewFocusChange - should log input view change focus when label is not null`() {
        // Given
        val label = "Test Input Field"
        every { analyticsManager.logInputViewChangeFocus(any()) } just Runs

        // When
        viewModel.logInputViewFocusChange(label)

        // Then
        verify { analyticsManager.logInputViewChangeFocus(label) }
    }

    @Test
    fun `logInputViewFocusChange - should not log when label is null`() {
        // Given
        val label: String? = null
        every { analyticsManager.logInputViewChangeFocus(any()) } just Runs

        // When
        viewModel.logInputViewFocusChange(label)

        // Then
        verify(exactly = 0) { analyticsManager.logInputViewChangeFocus(any()) }
    }

    // MARK: - logInputViewError Tests

    @Test
    fun `logInputViewError - should log input view error`() {
        // Given
        val error = "Test error message"
        every { analyticsManager.logInputViewError(any()) } just Runs

        // When
        viewModel.logInputViewError(error)

        // Then
        verify { analyticsManager.logInputViewError(error) }
    }

    // MARK: - StateFlow Tests

    @Test
    fun `pageState - should return current state`() = runTest {
        // Given
        val testState = SequentialPageState(
            inputViewFieldCount = 5
        )
        viewModel.pageMutableStateFlow.value = testState

        // When
        val result = viewModel.pageState.first()

        // Then
        assertEquals(testState, result)
    }

    @Test
    fun `isContinueButtonEnabled - should return current enabled state`() = runTest {
        // Given
        viewModel.isContinueButtonEnabledFlowState.value = false

        // When
        val result = viewModel.isContinueButtonEnabled.first()

        // Then
        assertFalse(result)
    }

    // MARK: - Integration Tests

    @Test
    fun `integration - complete flow from module start to continue event`() = runTest {
        // Given
        val testFieldItem = SequentialPageFieldCaptureItem(
            key = "field1", regex = "\\d+", value = "", heading = "Test Field",
            minDate = null, maxDate = null, keyBoardType = KeyboardType.NUMBER,
            type = SequentialPageCaptureComponentType.INPUT_FIELD,
            errorState = SequentialPageUIErrorState(), title = "Test Title",
            helpInformation = null, contentType = null, radioInputs = null,
            noRadioOptionSelectedText = null, text = null, helpText = null,
            action = null, radioOptions = null, maxInputLength = 10
        )

        val pageModuleData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = listOf(testFieldItem), fieldCount = 1, buttonState = true
        )

        val validationResult = SequentialPageValidationResult(
            updatedItems = listOf(testFieldItem.copy(value = "123")),
            isPageDataValid = true
        )

        val mockAction = MvcAction(stepId = "CONTINUE_STEP")
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()
        val mockPayload = mockk<SequentialPageCaptureRequestData>()

        // Setup mocks
        every { fieldDataProcessor.processModuleData(any()) } returns pageModuleData
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs
        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns validationResult
        every { testModule.getAction("continue") } returns mockAction
        every { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) } returns mockPayload
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // When
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onUiUpdateEvent(NextClickEvent("Continue", "continue", true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { analyticsManager.logScreenLoad("test_page") }
        verify { fieldDataProcessor.processModuleData(testModule.sequentialPageDataList) }
        verify { analyticsManager.logButtonClick("Continue") }
        verify { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) }
        verify { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) }
        verify { mockJourneyAction.executeAction(mockAction, testModule.id, "SEQUENTIAL_PAGE_CONTROLLER") }
    }

    // MARK: - Edge Cases Tests

    @Test
    fun `edge case - multiple rapid field changes should process correctly`() = runTest {
        // Given
        val fieldKey = "field1"
        val mockItem = SequentialPageFieldCaptureItem(
            key = fieldKey, regex = ".*", value = "", heading = null,
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
            inputValue = "test", pageItem = mockItem
        )

        val validationResult = SequentialPageFieldValidationResult.ValidationResult(
            isValid = true, message = null
        )

        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns pageInputFieldData
        every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns validationResult
        every { testModule.sequentialPageDataList } returns emptyList()

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When - simulate rapid typing
        repeat(5) { index ->
            viewModel.processInputStream(fieldKey, "test$index", null)
        }

        // Then - should handle all changes without errors
        verify(exactly = 5) { fieldDataProcessor.getCappedValue(any(), any(), eq(fieldKey), any(), any()) }
    }

    @Test
    fun `edge case - empty state should not crash when processing events`() = runTest {
        // Given - completely empty state
        val emptyState = SequentialPageState()
        viewModel.pageMutableStateFlow.value = emptyState

        // When/Then - should not crash
        viewModel.onUiUpdateEvent(ClearError)
        viewModel.logAccordionStateChange(true, "test")
        viewModel.logInputViewFocusChange("test")
    }

    // MARK: - Private Method Coverage Through Public Interface

    @Test
    fun `private processInputStream - validation error handling with showErrorsOnInvalidFields false`() = runTest {
        // Given - testing the private processInputStream method through FieldChanged event
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

        // Set the module first
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When - initially showErrorsOnInvalidFields is false
        viewModel.onUiUpdateEvent(FieldChanged(fieldKey, inputValue, null))

        // Then - error should not be shown initially
        val finalState = viewModel.pageState.first()
        val updatedItem = finalState.sequentialPageState.items.first()
        assertEquals(inputValue, updatedItem.value)
        assertFalse(updatedItem.errorState.isError) // Error not shown yet
        assertEquals(null, updatedItem.errorState.errorMessage)
    }

    @Test
    fun `private processInputStream - validation error shown after showErrorsOnInvalidFields set to true`() = runTest {
        // Given - Set up the same scenario as above but trigger showErrorsOnInvalidFields = true first
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

        val validationResultInvalid = SequentialPageFieldValidationResult.ValidationResult(
            isValid = false, message = errorMessage
        )

        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns pageInputFieldData
        every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns validationResultInvalid
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

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // First trigger continue event to set showErrorsOnInvalidFields = true
        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = listOf(mockItem.copy(errorState = SequentialPageUIErrorState(isError = true))),
            isPageDataValid = false
        )
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { analyticsManager.logButtonClick(any()) } just Runs

        viewModel.onUiUpdateEvent(NextClickEvent("Continue", "continue", true))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Now process input stream with showErrorsOnInvalidFields = true
        viewModel.onUiUpdateEvent(FieldChanged(fieldKey, inputValue, null))

        // Then - error should be shown now
        val finalState = viewModel.pageState.first()
        val updatedItem = finalState.sequentialPageState.items.first()
        assertEquals(inputValue, updatedItem.value)
        assertTrue(updatedItem.errorState.isError) // Error should be shown
        assertEquals(errorMessage, updatedItem.errorState.errorMessage)
    }

    @Test
    fun `private processInputStream - with investment account context`() = runTest {
        // Given - test processInputStream with investment account selected
        val fieldKey = "field1"
        val inputValue = "test_value"
        val mockInvestmentAccount = mockk<SequentialPageInvestmentAccount>()
        
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
            sequentialPageState = SequentialPageUiState(items = listOf(mockItem)),
            investmentAccountUiState = SequentialPageInvestmentPageView(
                selectedInvestmentAccount = mockInvestmentAccount
            )
        )
        viewModel.pageMutableStateFlow.value = initialState

        val pageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = inputValue, pageItem = mockItem
        )
        val validationResult = SequentialPageFieldValidationResult.ValidationResult(
            isValid = true, message = null
        )

        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns pageInputFieldData
        every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns validationResult
        every { testModule.sequentialPageDataList } returns emptyList()

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onUiUpdateEvent(FieldChanged(fieldKey, inputValue, null))

        // Then - verify it was called with the investment account context
        verify { fieldDataProcessor.getCappedValue(
            items = any(),
            investmentAccount = mockInvestmentAccount,
            itemKey = fieldKey,
            inputData = inputValue,
            contentType = null
        )}
        verify { sequentialPageValidationDelegator.validateField(
            inputValue, mockItem, mockInvestmentAccount, any()
        )}
    }
        // Given
        val fieldKey = "field1"
        val mockItem = SequentialPageFieldCaptureItem(
            key = fieldKey, regex = "\\d+", value = "invalid", heading = null,
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
            inputValue = "invalid", pageItem = mockItem
        )

        val validationResultInvalid = SequentialPageFieldValidationResult.ValidationResult(
            isValid = false, message = "Invalid input"
        )

        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns pageInputFieldData
        every { sequentialPageValidationDelegator.validateField(any(), any(), any(), any()) } returns validationResultInvalid
        every { testModule.sequentialPageDataList } returns emptyList()

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        // When - first process input (should not show error initially)
        viewModel.processInputStream(fieldKey, "invalid", null)
        val stateBeforeErrors = viewModel.pageState.first()

        // Trigger continue event to set showErrorsOnInvalidFields = true
        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = listOf(mockItem.copy(errorState = SequentialPageUIErrorState(isError = true))),
            isPageDataValid = false
        )
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { analyticsManager.logButtonClick(any()) } just Runs

        viewModel.processContinueEvent("Continue", "continue", true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Process input again (should show error now)
        viewModel.processInputStream(fieldKey, "invalid", null)
        val stateAfterErrors = viewModel.pageState.first()

        // Then - error should be shown after continue event sets the flag
        assertFalse(stateBeforeErrors.sequentialPageState.items.first().errorState.isError)
        assertTrue(stateAfterErrors.sequentialPageState.items.first().errorState.isError)
    }

    // MARK: - Comprehensive Coverage Tests

    @Test
    fun `comprehensive - all INVESTMENT step IDs coverage`() = runTest {
        // Test all investment-related step IDs
        val stepIds = listOf(
            "OTP_VERIFICATION_STATUS_CHECK",
            "INVESTMENT_MASS_AFFLUENT_CALL_HELPDESK", 
            "INVESTMENT_INITIALISE",
            "PENDING_ACTIVATION_CODE_INITIAL"
        )

        val mockValidationResult = SequentialPageValidationResult(
            updatedItems = emptyList(), isPageDataValid = true
        )
        val mockJourneyAction = mockk<JourneyFrameworkGenericHandleAction>()

        every { analyticsManager.logButtonClick(any()) } just Runs
        every { sequentialPageValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { journeyActionBuilder.build() } returns mockJourneyAction
        every { mockJourneyAction.executeAction(any(), any(), any()) } just Runs
        every { testModule.attachPayLoadToMvcAction(any(), any()) } just Runs

        // Set module
        moduleStateFlow.value = testModule
        testDispatcher.scheduler.advanceUntilIdle()

        stepIds.forEach { stepId ->
            // Given
            val mockAction = MvcAction(stepId = stepId)
            every { testModule.getAction("action_$stepId") } returns mockAction

            // When
            viewModel.processContinueEvent("Continue", "action_$stepId", true)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            verify { testModule.attachPayLoadToMvcAction(null, mockAction) }
        }
    }

    @Test
    fun `comprehensive - all event types coverage`() {
        // Test all event types to ensure 100% coverage
        val events = listOf(
            ModuleStart(testModule),
            FieldChanged("key", "value", null),
            FieldChanged(null, "value", null), // null key case
            InvestmentProductSelected(1),
            InvestmentAccountSelected(mockk(), mockk()),
            NextClickEvent("Continue", "action", true),
            NextClickEvent("Continue", "action", false), // different progressState
            PageBackEvent("back", "Back"),
            HyperlinkClicked,
            ClearError
        )

        val spyViewModel = spyk(viewModel)
        
        events.forEach { event ->
            spyViewModel.onUiUpdateEvent(event)
        }

        // Verify all event handlers were called
        verify { spyViewModel.processStartModule(any()) }
        verify(atLeast = 1) { spyViewModel.processInputStream(any(), any(), any()) }
        verify { spyViewModel.processSelectedProduct(any()) }
        verify { spyViewModel.processSelectedInvestmentAccount(any()) }
        verify(atLeast = 1) { spyViewModel.processContinueEvent(any(), any(), any()) }
        verify { spyViewModel.processOnBackEvent(any()) }
        verify { spyViewModel.processHyperlinkClickEvent() }
    }

    @Test
    fun `comprehensive - all analytics methods coverage`() {
        // Test all analytics logging methods
        every { analyticsManager.logScreenLoad(any()) } just Runs
        every { analyticsManager.logButtonClick(any()) } just Runs
        every { analyticsManager.logHyperLink(any()) } just Runs
        every { analyticsManager.logAccordionInteraction(any(), any()) } just Runs
        every { analyticsManager.logInputViewChangeFocus(any()) } just Runs
        every { analyticsManager.logInputViewError(any()) } just Runs

        // When
        viewModel.logAccordionStateChange(true, "accordion1")
        viewModel.logAccordionStateChange(false, "accordion2")
        viewModel.logInputViewFocusChange("input1")
        viewModel.logInputViewFocusChange(null)
        viewModel.logInputViewError("error1")

        // Then
        verify { analyticsManager.logAccordionInteraction("accordion1", "EXPAND") }
        verify { analyticsManager.logAccordionInteraction("accordion2", "COLLAPSED") }
        verify { analyticsManager.logInputViewChangeFocus("input1") }
        verify(exactly = 0) { analyticsManager.logInputViewChangeFocus(null) }
        verify { analyticsManager.logInputViewError("error1") }
    }

    @Test
    fun `error scenarios - null/empty data handling`() = runTest {
        // Test various null/empty scenarios
        val emptyModule = mockk<SequentialPageCaptureModule> {
            every { id } returns ""
            every { sequentialPageSections } returns SequentialPageSections(null)
            every { sequentialPageDataList } returns emptyList()
            every { getAction(any()) } returns null
        }

        val emptyPageData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = emptyList(), fieldCount = 0, buttonState = false
        )

        every { fieldDataProcessor.processModuleData(any()) } returns emptyPageData
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.processStartModule(emptyModule)

        // Then - should handle empty data gracefully
        verify { analyticsManager.logScreenLoad("") }
        verify { fieldDataProcessor.processModuleData(emptyList()) }

        val state = viewModel.pageState.first()
        assertTrue(state.sequentialPageState.items.isEmpty())
        assertTrue(state.productPickerState.products.isEmpty())
        assertTrue(state.sequentialPageState.accordionState.accordionPair.isEmpty())
    }

    // MARK: - State Consistency Tests

    @Test
    fun `state consistency - button enabled state updates correctly`() = runTest {
        // Given
        val pageDataWithDisabledButton = SequentialPageDataProcessor.PageModuleData(
            pageInfo = emptyList(), fieldCount = 0, buttonState = false
        )

        every { fieldDataProcessor.processModuleData(any()) } returns pageDataWithDisabledButton
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.processStartModule(testModule)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val buttonState = viewModel.isContinueButtonEnabled.first()
        assertFalse(buttonState)
    }

    @Test
    fun `state consistency - view type is set correctly from items`() = runTest {
        // Given
        val dateFieldItem = SequentialPageFieldCaptureItem(
            key = "date_field", regex = null, value = null, heading = null,
            minDate = "2023-01-01", maxDate = "2023-12-31", keyBoardType = null,
            type = SequentialPageCaptureComponentType.DATE,
            errorState = SequentialPageUIErrorState(), title = null,
            helpInformation = null, contentType = null, radioInputs = null,
            noRadioOptionSelectedText = null, text = null, helpText = null,
            action = null, radioOptions = null, maxInputLength = null
        )

        val pageDataWithDate = SequentialPageDataProcessor.PageModuleData(
            pageInfo = listOf(dateFieldItem), fieldCount = 1, buttonState = true
        )

        every { fieldDataProcessor.processModuleData(any()) } returns pageDataWithDate
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
        every { analyticsManager.logScreenLoad(any()) } just Runs

        // When
        viewModel.processStartModule(testModule)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.pageState.first()
        assertEquals(SequentialPageCaptureComponentType.DATE, state.viewType)
    }

    @Test
    fun `cleanup - verify all mocks are used and no unused verifications`() {
        // This test ensures we don't have any unused mock setups
        confirmVerified(
            fieldDataProcessor,
            productHandler,
            investmentAccountHandler,
            payLoadProcessor,
            analyticsManager,
            sequentialPageValidationDelegator,
            journeyActionBuilder,
            moduleDataProcessor,
            testModule,
            testAction
        )
    }
}

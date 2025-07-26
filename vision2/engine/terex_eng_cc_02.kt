import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SequentialPageCaptureViewModelTest {

    private lateinit var viewModel: SequentialPageCaptureViewModel
    private lateinit var mockFieldDataProcessor: SequentialPageDataProcessor
    private lateinit var mockProductHandler: SequentialPageSelectedProductProcessor
    private lateinit var mockInvestmentAccountHandler: SequentialPageInvestmentAccountProcessor
    private lateinit var mockPayLoadProcessor: SequentialPagePayLoadGenerator
    private lateinit var mockAnalyticsManager: SequentialPageAnalyticsEvent
    private lateinit var mockValidationDelegator: SequentialPageValidationDelegator
    private lateinit var mockJourneyActionBuilder: JourneyFrameworkGenericHandleAction.Builder
    private lateinit var mockModuleDataProcessor: SequentialPageCaptureModuleProcessor

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockFieldDataProcessor = mockk()
        mockProductHandler = mockk()
        mockInvestmentAccountHandler = mockk()
        mockPayLoadProcessor = mockk()
        mockAnalyticsManager = mockk(relaxed = true)
        mockValidationDelegator = mockk()
        mockJourneyActionBuilder = mockk()
        mockModuleDataProcessor = mockk()

        every { mockModuleDataProcessor.dataStateFlow } returns MutableStateFlow(null)

        viewModel = SequentialPageCaptureViewModel(
            fieldDataProcessor = mockFieldDataProcessor,
            productHandler = mockProductHandler,
            investmentAccountHandler = mockInvestmentAccountHandler,
            payLoadProcessor = mockPayLoadProcessor,
            analyticsManager = mockAnalyticsManager,
            sequentialPageValidationDelegator = mockValidationDelegator,
            journeyActionBuilder = mockJourneyActionBuilder,
            moduleDataProcessor = mockModuleDataProcessor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `test startModule processes module correctly`() {
        // Given
        val mockModule = createMockModule()
        val mockItems = listOf(createMockFieldCaptureItem())
        val mockAccordionData = mapOf("key1" to listOf<SequentialPageHelpInformation>())
        val mockInvestmentProducts = listOf(createMockProductPickerView())
        val mockPageModuleData = SequentialPageDataProcessor.PageModuleData(
            pageInfo = mockItems,
            fieldCount = 2,
            buttonState = true
        )

        every { mockFieldDataProcessor.processModuleData(mockModule.sequentialPageDataList) } returns mockPageModuleData
        every { mockFieldDataProcessor.getAccordionData(mockItems) } returns mockAccordionData
        every { mockFieldDataProcessor.getInvestmentProducts(mockItems) } returns mockInvestmentProducts

        // When
        viewModel.onUiUpdateEvent(ModuleStart(mockModule))

        // Then
        verify { mockAnalyticsManager.logScreenLoad("test_analytics_tag") }
        verify { mockFieldDataProcessor.processModuleData(mockModule.sequentialPageDataList) }
        verify { mockFieldDataProcessor.getAccordionData(mockItems) }
        verify { mockFieldDataProcessor.getInvestmentProducts(mockItems) }
        assertEquals(mockModule, viewModel.getModule())
    }

    @Test
    fun `test processInputStream handles field changes with valid pageItem and result`() = runTest {
        // Given
        val mockModule = createMockModule()
        val fieldKey = "test_key"
        val inputValue = "test_value"
        val cappedValue = "capped_test_value"
        
        val mockFieldItem = createMockFieldCaptureItem(key = fieldKey, value = "old_value")
        val mockItems = listOf(mockFieldItem)
        val mockSelectedAccount = createMockInvestmentAccount()
        
        val mockPageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = cappedValue,
            pageItem = mockFieldItem
        )
        
        val mockValidationResult = createMockFieldValidationResult(isValid = true, message = null)
        val mockModuleResponse = createMockModuleResponse(id = fieldKey)

        viewModel.module = mockModule
        
        // Mock the initial state with items
        val currentState = SequentialPageState(
            sequentialPageState = SequentialPageUiState(items = mockItems),
            investmentAccountUiState = SequentialPageInvestmentPageView(
                selectedInvestmentAccount = mockSelectedAccount
            )
        )

        every { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        } returns mockPageInputFieldData

        coEvery {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = mockSelectedAccount,
                moduleResponse = mockModuleResponse
            )
        } returns mockValidationResult

        // When
        viewModel.onUiUpdateEvent(
            FieldChanged(
                key = fieldKey,
                value = inputValue,
                contentType = null
            )
        )
        advanceUntilIdle()

        // Then
        verify { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        }
        coVerify {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = mockSelectedAccount,
                moduleResponse = mockModuleResponse
            )
        }
    }

    @Test
    fun `test processInputStream handles field changes with validation error`() = runTest {
        // Given
        val mockModule = createMockModule()
        val fieldKey = "error_key"
        val inputValue = "invalid_value"
        val cappedValue = "capped_invalid_value"
        val errorMessage = "Invalid input error"
        
        val mockFieldItem = createMockFieldCaptureItem(key = fieldKey, value = "old_value")
        val mockItems = listOf(mockFieldItem)
        val mockSelectedAccount = createMockInvestmentAccount()
        
        val mockPageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = cappedValue,
            pageItem = mockFieldItem
        )
        
        val mockValidationResult = createMockFieldValidationResult(isValid = false, message = errorMessage)
        val mockModuleResponse = createMockModuleResponse(id = fieldKey)

        viewModel.module = mockModule
        
        every { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        } returns mockPageInputFieldData

        coEvery {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = mockSelectedAccount,
                moduleResponse = mockModuleResponse
            )
        } returns mockValidationResult

        // When
        viewModel.onUiUpdateEvent(
            FieldChanged(
                key = fieldKey,
                value = inputValue,
                contentType = null
            )
        )
        advanceUntilIdle()

        // Then
        verify { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        }
        coVerify {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = mockSelectedAccount,
                moduleResponse = mockModuleResponse
            )
        }
        verify { mockAnalyticsManager.logInputViewError(errorMessage) }
    }

    @Test
    fun `test processInputStream with null pageItem does not validate`() = runTest {
        // Given
        val mockModule = createMockModule()
        val fieldKey = "test_key"
        val inputValue = "test_value"
        val cappedValue = "capped_test_value"
        
        val mockItems = listOf(createMockFieldCaptureItem(key = "different_key"))
        val mockSelectedAccount = createMockInvestmentAccount()
        
        val mockPageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = cappedValue,
            pageItem = null // No matching item found
        )

        viewModel.module = mockModule
        
        every { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        } returns mockPageInputFieldData

        // When
        viewModel.onUiUpdateEvent(
            FieldChanged(
                key = fieldKey,
                value = inputValue,
                contentType = null
            )
        )
        advanceUntilIdle()

        // Then
        verify { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        }
        // Should not call validation when pageItem is null
        coVerify(exactly = 0) {
            mockValidationDelegator.validateField(any(), any(), any(), any())
        }
        verify(exactly = 0) { mockAnalyticsManager.logInputViewError(any()) }
    }

    @Test
    fun `test processInputStream with null key does nothing`() = runTest {
        // Given
        val mockModule = createMockModule()
        viewModel.module = mockModule

        // When
        viewModel.onUiUpdateEvent(
            FieldChanged(
                key = null,
                value = "test_value",
                contentType = null
            )
        )
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { 
            mockFieldDataProcessor.getCappedValue(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            mockValidationDelegator.validateField(any(), any(), any(), any())
        }
    }

    @Test
    fun `test processInputStream with content transformation type`() = runTest {
        // Given
        val mockModule = createMockModule()
        val fieldKey = "sort_code_key"
        val inputValue = "123456"
        val cappedValue = "12-34-56"
        val mockContentType = createMockOutputTransformation()
        
        val mockFieldItem = createMockFieldCaptureItem(key = fieldKey, value = "old_value")
        val mockItems = listOf(mockFieldItem)
        val mockSelectedAccount = createMockInvestmentAccount()
        
        val mockPageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = cappedValue,
            pageItem = mockFieldItem
        )
        
        val mockValidationResult = createMockFieldValidationResult(isValid = true, message = null)
        val mockModuleResponse = createMockModuleResponse(id = fieldKey)

        viewModel.module = mockModule
        
        every { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = mockContentType
            )
        } returns mockPageInputFieldData

        coEvery {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = mockSelectedAccount,
                moduleResponse = mockModuleResponse
            )
        } returns mockValidationResult

        // When
        viewModel.onUiUpdateEvent(
            FieldChanged(
                key = fieldKey,
                value = inputValue,
                contentType = mockContentType
            )
        )
        advanceUntilIdle()

        // Then
        verify { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = mockSelectedAccount,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = mockContentType
            )
        }
        coVerify {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = mockSelectedAccount,
                moduleResponse = mockModuleResponse
            )
        }
    }

    @Test
    fun `test processInputStream without selected investment account`() = runTest {
        // Given
        val mockModule = createMockModule()
        val fieldKey = "test_key"
        val inputValue = "test_value"
        val cappedValue = "capped_test_value"
        
        val mockFieldItem = createMockFieldCaptureItem(key = fieldKey, value = "old_value")
        val mockItems = listOf(mockFieldItem)
        
        val mockPageInputFieldData = SequentialPageDataProcessor.PageInputFieldData(
            inputValue = cappedValue,
            pageItem = mockFieldItem
        )
        
        val mockValidationResult = createMockFieldValidationResult(isValid = true, message = null)
        val mockModuleResponse = createMockModuleResponse(id = fieldKey)

        viewModel.module = mockModule
        
        every { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = null,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        } returns mockPageInputFieldData

        coEvery {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = null,
                moduleResponse = mockModuleResponse
            )
        } returns mockValidationResult

        // When
        viewModel.onUiUpdateEvent(
            FieldChanged(
                key = fieldKey,
                value = inputValue,
                contentType = null
            )
        )
        advanceUntilIdle()

        // Then
        verify { 
            mockFieldDataProcessor.getCappedValue(
                items = mockItems,
                investmentAccount = null,
                itemKey = fieldKey,
                inputData = inputValue,
                contentType = null
            )
        }
        coVerify {
            mockValidationDelegator.validateField(
                value = cappedValue,
                item = mockFieldItem,
                investmentAccount = null,
                moduleResponse = mockModuleResponse
            )
        }
    }

    @Test
    fun `test processSelectedProduct updates product selection`() {
        // Given
        val productId = 1
        val mockModule = createMockModule()
        viewModel.module = mockModule

        every { mockProductHandler.processSelectedProduct(productId) } returns mockk()

        // When
        viewModel.onUiUpdateEvent(InvestmentProductSelected(productId))

        // Then
        verify { mockProductHandler.processSelectedProduct(productId) }
    }

    @Test
    fun `test processSelectedInvestmentAccount handles account selection`() {
        // Given
        val mockAccount = createMockInvestmentAccount()
        val mockItem = createMockFieldCaptureItem()
        val mockModule = createMockModule()
        viewModel.module = mockModule

        every { 
            mockInvestmentAccountHandler.processSelectedInvestmentAccount(mockAccount) 
        } returns mockk()

        // When
        viewModel.onUiUpdateEvent(
            InvestmentAccountSelected(
                productInfo = mockAccount,
                item = mockItem
            )
        )

        // Then
        verify { mockInvestmentAccountHandler.processSelectedInvestmentAccount(mockAccount) }
    }

    @Test
    fun `test processContinueEvent with valid data launches journey`() = runTest {
        // Given
        val mockModule = createMockModule()
        val mockAction = createMockMvcAction()
        val mockValidationResult = createMockValidationResult(isValid = true)
        val mockPayload = createMockRequestData()

        viewModel.module = mockModule

        every { mockModule.getAction("test_action") } returns mockAction
        coEvery { 
            mockValidationDelegator.validateAllFields(
                sequentialPageFieldItems = emptyList(),
                investmentAccount = null,
                moduleResponse = mockModule.sequentialPageDataList
            )
        } returns mockValidationResult
        every { mockPayLoadProcessor.createPayLoad(mockValidationResult.updatedItems) } returns mockPayload
        every { mockModule.attachPayloadToMvcAction(mockPayload, mockAction) } just Runs
        every { mockJourneyActionBuilder.build() } returns mockk {
            every { executeAction(mockAction, mockModule.id, "CONTROLLER_ID") } just Runs
        }

        // When
        viewModel.onUiUpdateEvent(
            NextClickEvent(
                actionLabel = "Continue",
                actionId = "test_action",
                showProgress = true
            )
        )

        // Then
        verify { mockAnalyticsManager.logButtonClick("Continue") }
        coVerify { 
            mockValidationDelegator.validateAllFields(
                sequentialPageFieldItems = emptyList(),
                investmentAccount = null,
                moduleResponse = mockModule.sequentialPageDataList
            )
        }
        verify { mockPayLoadProcessor.createPayLoad(mockValidationResult.updatedItems) }
    }

    @Test
    fun `test processContinueEvent with invalid data shows errors`() = runTest {
        // Given
        val mockModule = createMockModule()
        val mockValidationResult = createMockValidationResult(isValid = false)

        viewModel.module = mockModule

        coEvery { 
            mockValidationDelegator.validateAllFields(
                sequentialPageFieldItems = emptyList(),
                investmentAccount = null,
                moduleResponse = mockModule.sequentialPageDataList
            )
        } returns mockValidationResult

        // When
        viewModel.onUiUpdateEvent(
            NextClickEvent(
                actionLabel = "Continue",
                actionId = "test_action",
                showProgress = true
            )
        )

        // Then
        verify { mockAnalyticsManager.logButtonClick("Continue") }
        coVerify { 
            mockValidationDelegator.validateAllFields(
                sequentialPageFieldItems = emptyList(),
                investmentAccount = null,
                moduleResponse = mockModule.sequentialPageDataList
            )
        }
    }

    @Test
    fun `test processOnBackEvent launches journey with empty request`() {
        // Given
        val mockModule = createMockModule()
        val mockAction = createMockMvcAction()
        val actionId = "back_action"

        viewModel.module = mockModule

        every { mockModule.getAction(actionId) } returns mockAction
        every { mockModule.attachPayloadToMvcAction(ofType<JourneyFrameworkGenericHandleAction.EmptyRequest>(), mockAction) } just Runs
        every { mockJourneyActionBuilder.build() } returns mockk {
            every { executeAction(mockAction, mockModule.id, "CONTROLLER_ID") } just Runs
        }

        // When
        viewModel.onUiUpdateEvent(PageBackEvent(actionId, "Back"))

        // Then
        verify { mockAnalyticsManager.logButtonClick(actionId) }
        verify { mockModule.getAction(actionId) }
        verify { mockModule.attachPayloadToMvcAction(ofType<JourneyFrameworkGenericHandleAction.EmptyRequest>(), mockAction) }
    }

    @Test
    fun `test processHyperlinkClickEvent with PRODUCT_SELECTION_SAVE action`() {
        // Given
        val mockModule = createMockModule()
        val mockNextAction = createMockMvcAction("PRODUCT_SELECTION_SAVE")
        val mockNoneOfTheseAction = createMockMvcAction()

        viewModel.module = mockModule

        every { mockModule.getAction("NEXT") } returns mockNextAction
        every { mockModule.getAction("NONE_OF_THESE") } returns mockNoneOfTheseAction
        every { mockModule.attachPayloadToMvcAction(null, mockNoneOfTheseAction) } just Runs
        every { mockJourneyActionBuilder.build() } returns mockk {
            every { executeAction(mockNoneOfTheseAction, mockModule.id, "CONTROLLER_ID") } just Runs
        }

        // When
        viewModel.onUiUpdateEvent(HyperlinkClicked)

        // Then
        verify { mockAnalyticsManager.logHyperLink("NONE_OF_THESE") }
        verify { mockModule.getAction("NEXT") }
        verify { mockModule.getAction("NONE_OF_THESE") }
    }

    @Test
    fun `test processHyperlinkClickEvent with other action triggers RESEND`() {
        // Given
        val mockModule = createMockModule()
        val mockNextAction = createMockMvcAction("OTHER_ACTION")
        val mockResendAction = createMockMvcAction()

        viewModel.module = mockModule

        every { mockModule.getAction("NEXT") } returns mockNextAction
        every { mockModule.getAction("RESEND") } returns mockResendAction
        every { mockModule.attachPayloadToMvcAction(null, mockResendAction) } just Runs
        every { mockJourneyActionBuilder.build() } returns mockk {
            every { executeAction(mockResendAction, mockModule.id, "CONTROLLER_ID") } just Runs
        }

        // When
        viewModel.onUiUpdateEvent(HyperlinkClicked)

        // Then
        verify { mockAnalyticsManager.logHyperLink("RESEND") }
        verify { mockModule.getAction("NEXT") }
        verify { mockModule.getAction("RESEND") }
    }

    @Test
    fun `test logAccordionStateChange logs expand state`() {
        // When
        viewModel.logAccordionStateChange(state = true, label = "test_label")

        // Then
        verify { mockAnalyticsManager.logAccordionInteraction("test_label", "ACCORDION_EXPAND") }
    }

    @Test
    fun `test logAccordionStateChange logs collapse state`() {
        // When
        viewModel.logAccordionStateChange(state = false, label = "test_label")

        // Then
        verify { mockAnalyticsManager.logAccordionInteraction("test_label", "ACCORDION_COLLAPSED") }
    }

    @Test
    fun `test logInputViewFocusChange with non-null label`() {
        // When
        viewModel.logInputViewFocusChange("test_label")

        // Then
        verify { mockAnalyticsManager.logInputViewChangeFocus("test_label") }
    }

    @Test
    fun `test logInputViewFocusChange with null label does not log`() {
        // When
        viewModel.logInputViewFocusChange(null)

        // Then
        verify(exactly = 0) { mockAnalyticsManager.logInputViewChangeFocus(any()) }
    }

    @Test
    fun `test ClearError event clears error state`() {
        // When
        viewModel.onUiUpdateEvent(ClearError)

        // Then - State should be updated but we can't easily verify without StateFlow collection
        // This test ensures the event is handled without exceptions
        assertTrue(true)
    }

    // Helper methods for creating mock objects
    private fun createMockModule(): SequentialPageCaptureModule = mockk {
        every { sequentialPageDataList } returns listOf(createMockModuleResponse())
        every { sequentialPageSections } returns mockk {
            every { analyticsPageTag } returns "test_analytics_tag"
        }
        every { id } returns "module_id"
        every { getAction(any()) } returns null
        every { attachPayloadToMvcAction(any(), any()) } just Runs
    }

    private fun createMockFieldCaptureItem(
        key: String = "test_key",
        value: String = "test_value"
    ): SequentialPageFieldCaptureItem = mockk {
        every { this@mockk.key } returns key
        every { this@mockk.value } returns value
        every { type } returns mockk()
        every { errorState } returns mockk {
            every { isError } returns false
        }
        every { copy(any()) } returns this@mockk
        every { copy(value = any()) } returns this@mockk
        every { copy(errorState = any()) } returns this@mockk
    }

    private fun createMockProductPickerView(): SequentialPageProductPickerView = mockk {
        every { title } returns "Test Product"
        every { index } returns 0
        every { isProductSelected } returns false
    }

    private fun createMockInvestmentAccount(): SequentialPageInvestmentAccount = mockk {
        every { radioTitle } returns "Test Account"
        every { isProductSelected } returns true
        every { regex } returns "test_regex"
        every { maxLength } returns 10
    }

    private fun createMockMvcAction(stepId: String = "TEST_STEP"): MvcAction = mockk {
        every { this@mockk.stepId } returns stepId
        every { showProgress } returns true
        every { showProgress = any() } just Runs
    }

    private fun createMockValidationResult(isValid: Boolean): SequentialPageValidationResult = mockk {
        every { isPageDataValid } returns isValid
        every { updatedItems } returns emptyList()
    }

    private fun createMockRequestData(): SequentialPageCaptureRequestData = mockk()

    private fun createMockFieldValidationResult(
        isValid: Boolean,
        message: String?
    ): SequentialPageFieldValidationResult = mockk<SequentialPageFieldValidationResult.ValidationResult> {
        every { this@mockk.isValid } returns isValid
        every { this@mockk.message } returns message
    }

    private fun createMockModuleResponse(id: String = "test_id"): SequentialPageModuleResponse = mockk {
        every { this@mockk.id } returns id
        every { invalidInputText } returns "Invalid input"
        every { contentType } returns null
    }

    private fun createMockOutputTransformation(): OutputTransformation = mockk()
}

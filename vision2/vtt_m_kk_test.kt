import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class SequentialPageProcessorTest {

    // Mocks
    private lateinit var mockPageConfig: SequentialPageConfig
    private lateinit var mockPageDataProcessor: SequentialPageDataProcessor
    private lateinit var mockProductProcessor: SequentialPageProductProcessor
    private lateinit var mockInvestmentAccountProcessor: SequentialPageInvestmentAccountProcessor
    private lateinit var mockValidationDelegator: SequentialPageValidationDelegator
    private lateinit var mockPayloadGenerator: SequentialPagePayloadGenerator
    private lateinit var mockModule: SequentialPageCaptureModule

    // Test subject
    private lateinit var processor: SequentialPageProcessor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        mockPageConfig = mockk(relaxed = true)
        mockPageDataProcessor = mockk(relaxed = true)
        mockProductProcessor = mockk(relaxed = true)
        mockInvestmentAccountProcessor = mockk(relaxed = true)
        mockValidationDelegator = mockk(relaxed = true)
        mockPayloadGenerator = mockk(relaxed = true)
        mockModule = mockk(relaxed = true)

        every { mockPageConfig.pageDataProcessor } returns mockPageDataProcessor
        every { mockPageConfig.productProcessor } returns mockProductProcessor
        every { mockPageConfig.investmentAccountProcessor } returns mockInvestmentAccountProcessor
        every { mockPageConfig.validationDelegator } returns mockValidationDelegator
        every { mockPageConfig.payloadGenerator } returns mockPayloadGenerator

        processor = SequentialPageProcessor(mockPageConfig)
    }

    @Test
    fun `processModuleStart returns correct ModuleStartResult`() {
        // Given
        val mockPageData = createMockPageModuleData()
        val mockAccordionData = mapOf("key" to listOf<SequentialPageHelpInformation>())
        val mockProducts = listOf<SequentialPageProductPickerView>()
        val mockSections = mockk<SequentialPageSections>(relaxed = true)
        
        every { mockModule.sequentialPageDataList } returns emptyList()
        every { mockModule.sequentialPageSections } returns mockSections
        every { mockSections.analyticsPageTag } returns "test_analytics_tag"
        
        every { mockPageDataProcessor.processModuleData(any()) } returns mockPageData
        every { mockPageDataProcessor.getAccordionData(any()) } returns mockAccordionData
        every { mockPageDataProcessor.getInvestmentProducts(any()) } returns mockProducts

        // When
        val result = processor.processModuleStart(mockModule)

        // Then
        assertEquals(mockPageData, result.pageData)
        assertEquals(mockAccordionData, result.accordionData)
        assertEquals(mockProducts, result.investmentProducts)
        assertEquals("test_analytics_tag", result.analyticsPageTag)
        assertEquals(mockSections, result.sequentialPageSections)
        
        verify { mockPageDataProcessor.processModuleData(emptyList()) }
        verify { mockPageDataProcessor.getAccordionData(mockPageData.pageInfo) }
        verify { mockPageDataProcessor.getInvestmentProducts(mockPageData.pageInfo) }
    }

    @Test
    fun `processFieldInput returns null when pageItem is null`() {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockCappedValue = PageInputFieldData(
            inputValue = "test_value",
            pageItem = null
        )
        
        every { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns mockCappedValue

        // When
        val result = processor.processFieldInput(
            fieldKey = "test_field",
            userEnteredValue = "test_value",
            contentTransformationType = null,
            currentState = currentState,
            module = mockModule
        )

        // Then
        assertNull(result)
        verify { mockPageDataProcessor.getCappedValue(any(), any(), "test_field", "test_value", null) }
        verify(exactly = 0) { mockValidationDelegator.validateField(any(), any(), any(), any()) }
    }

    @Test
    fun `processFieldInput returns FieldInputResult with validation success`() {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockPageContent = TestDataProvider.createMockSequentialPageContent()
        val mockCappedValue = PageInputFieldData(
            inputValue = "valid_input",
            pageItem = mockPageContent
        )
        val mockValidationResult = TestDataProvider.createMockValidationResult(isValid = true)
        
        every { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns mockCappedValue
        every { mockValidationDelegator.validateField(any(), any(), any(), any()) } returns mockValidationResult
        every { mockModule.sequentialPageDataList } returns listOf(mockk(relaxed = true))

        // When
        val result = processor.processFieldInput(
            fieldKey = "test_field",
            userEnteredValue = "valid_input",
            contentTransformationType = null,
            currentState = currentState,
            module = mockModule
        )

        // Then
        assertNotNull(result)
        assertEquals("test_field", result!!.fieldKey)
        assertEquals(mockCappedValue, result.cappedValue)
        assertEquals(mockValidationResult, result.validationResult)
        assertFalse(result.shouldLogError)
        assertNull(result.errorMessage)
    }

    @Test
    fun `processFieldInput returns FieldInputResult with validation failure`() {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockPageContent = TestDataProvider.createMockSequentialPageContent()
        val mockCappedValue = PageInputFieldData(
            inputValue = "invalid_input",
            pageItem = mockPageContent
        )
        val mockValidationResult = TestDataProvider.createMockValidationResult(isValid = false)
        
        every { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns mockCappedValue
        every { mockValidationDelegator.validateField(any(), any(), any(), any()) } returns mockValidationResult
        every { mockModule.sequentialPageDataList } returns listOf(mockk(relaxed = true))

        // When
        val result = processor.processFieldInput(
            fieldKey = "test_field",
            userEnteredValue = "invalid_input",
            contentTransformationType = null,
            currentState = currentState,
            module = mockModule
        )

        // Then
        assertNotNull(result)
        assertEquals("test_field", result!!.fieldKey)
        assertEquals(mockValidationResult, result.validationResult)
        assertEquals("Validation failed", result.errorMessage)
    }

    @Test
    fun `processProductSelection returns ProductSelectionResult`() {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val moduleDataList = listOf<SequentialPageModuleResponse>()
        val expectedUpdatedState = currentState.copy(
            productPickerState = currentState.productPickerState.copy(
                selectedProductData = SequentialPageSelectedProductData(optionId = "selected_product_123")
            )
        )
        
        every { mockProductProcessor.processSelectedProduct(any(), any(), any()) } returns expectedUpdatedState

        // When
        val result = processor.processProductSelection(
            productId = 123,
            currentState = currentState,
            moduleDataList = moduleDataList
        )

        // Then
        assertEquals(expectedUpdatedState, result.updatedState)
        assertEquals("selected_product_123", result.selectedProductId)
        verify { mockProductProcessor.processSelectedProduct(currentState, 123, moduleDataList) }
    }

    @Test
    fun `processAccountSelection returns AccountSelectionResult with optionId`() {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockAccount = mockk<SequentialPageInvestmentAccount>(relaxed = true)
        val expectedUpdatedState = currentState.copy()
        
        every { mockAccount.optionId } returns "account_option_123"
        every { mockAccount.radioTitle } returns "Test Account"
        every { mockInvestmentAccountProcessor.updateSelectedInvestmentAccount(any(), any()) } returns expectedUpdatedState

        // When
        val result = processor.processAccountSelection(mockAccount, currentState)

        // Then
        assertEquals(expectedUpdatedState, result.updatedState)
        assertEquals("account_option_123", result.analyticsLabel)
        verify { mockInvestmentAccountProcessor.updateSelectedInvestmentAccount(currentState, mockAccount) }
    }

    @Test
    fun `processAccountSelection uses radioTitle when optionId is null`() {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockAccount = mockk<SequentialPageInvestmentAccount>(relaxed = true)
        val expectedUpdatedState = currentState.copy()
        
        every { mockAccount.optionId } returns null
        every { mockAccount.radioTitle } returns "Test Radio Title"
        every { mockInvestmentAccountProcessor.updateSelectedInvestmentAccount(any(), any()) } returns expectedUpdatedState

        // When
        val result = processor.processAccountSelection(mockAccount, currentState)

        // Then
        assertEquals("Test Radio Title", result.analyticsLabel)
    }

    @Test
    fun `processAccountSelection uses default constant when both optionId and radioTitle are null`() {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockAccount = mockk<SequentialPageInvestmentAccount>(relaxed = true)
        val expectedUpdatedState = currentState.copy()
        
        every { mockAccount.optionId } returns null
        every { mockAccount.radioTitle } returns null
        every { mockInvestmentAccountProcessor.updateSelectedInvestmentAccount(any(), any()) } returns expectedUpdatedState

        // When
        val result = processor.processAccountSelection(mockAccount, currentState)

        // Then
        assertEquals("PRODUCT_TITLE_WITH_NO_VALUE", result.analyticsLabel)
    }

    @Test
    fun `processContinueAction returns ValidationFailure when validation fails`() = runBlockingTest {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockValidationResult = TestDataProvider.createMockValidationResult(isValid = false)
        
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult

        // When
        val result = processor.processContinueAction(
            actionId = "NEXT",
            progressState = true,
            currentState = currentState,
            module = mockModule
        )

        // Then
        assertTrue(result is SequentialPageResult.ValidationFailure)
        val validationFailure = result as SequentialPageResult.ValidationFailure
        assertEquals(mockValidationResult.updatedItems, validationFailure.updatedItems)
    }

    @Test
    fun `processContinueAction returns Success for OTP_VERIFICATION_STATUS_CHECK`() = runBlockingTest {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockValidationResult = TestDataProvider.createMockValidationResult(isValid = true)
        val mockAction = TestDataProvider.createMockNvcAction("OTP_VERIFICATION_STATUS_CHECK")
        
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { mockModule.getAction("NEXT") } returns mockAction

        // When
        val result = processor.processContinueAction(
            actionId = "NEXT",
            progressState = true,
            currentState = currentState,
            module = mockModule
        )

        // Then
        assertTrue(result is SequentialPageResult.Success)
        val success = result as SequentialPageResult.Success
        assertEquals(mockAction, success.action)
        assertNull(success.payload)
    }

    @Test
    fun `processContinueAction returns Success for PRODUCT_SELECTION_SAVE with valid data`() = runBlockingTest {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockValidationResult = TestDataProvider.createMockValidationResult(isValid = true)
        val mockAction = TestDataProvider.createMockNvcAction("PRODUCT_SELECTION_SAVE")
        val mockModuleResponse = mockk<SequentialPageModuleResponse>(relaxed = true)
        val mockPayload = mockk<JourneyFrameworkRequestModule.RequestData>(relaxed = true)
        
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { mockModule.getAction("SAVE") } returns mockAction
        every { mockModule.sequentialPageDataList } returns listOf(mockModuleResponse)
        every { mockModuleResponse.id } returns "product_field_id"
        every { mockPayloadGenerator.createPayload(any(), any()) } returns mockPayload

        // When
        val result = processor.processContinueAction(
            actionId = "SAVE",
            progressState = false,
            currentState = currentState,
            module = mockModule
        )

        // Then
        assertTrue(result is SequentialPageResult.Success)
        val success = result as SequentialPageResult.Success
        assertEquals(mockAction, success.action)
        assertEquals(mockPayload, success.payload)
    }

    @Test
    fun `processBackNavigation returns BackNavigationResult`() {
        // Given
        val mockAction = TestDataProvider.createMockNvcAction("BACK_STEP")
        every { mockModule.getAction("BACK") } returns mockAction

        // When
        val result = processor.processBackNavigation("BACK", mockModule)

        // Then
        assertEquals(mockAction, result.action)
        assertTrue(result.payload is JourneyFrameworkGenericHandleAction.EmptyRequest)
    }

    @Test
    fun `processHyperlinkAction returns NONE_OF_THESE for PRODUCT_SELECTION_SAVE`() {
        // Given
        val mockNextAction = TestDataProvider.createMockNvcAction("PRODUCT_SELECTION_SAVE")
        val mockNoneOfTheseAction = TestDataProvider.createMockNvcAction("NONE_OF_THESE_STEP")
        
        every { mockModule.getAction("NEXT") } returns mockNextAction
        every { mockModule.getAction("NONE_OF_THESE") } returns mockNoneOfTheseAction

        // When
        val result = processor.processHyperlinkAction(mockModule)

        // Then
        assertEquals(mockNoneOfTheseAction, result.action)
        assertEquals("NONE_OF_THESE", result.actionType)
        assertNull(result.payload)
    }

    @Test
    fun `processHyperlinkAction returns RESEND for non-PRODUCT_SELECTION_SAVE`() {
        // Given
        val mockNextAction = TestDataProvider.createMockNvcAction("OTHER_STEP")
        val mockResendAction = TestDataProvider.createMockNvcAction("RESEND_STEP")
        
        every { mockModule.getAction("NEXT") } returns mockNextAction
        every { mockModule.getAction("RESEND") } returns mockResendAction

        // When
        val result = processor.processHyperlinkAction(mockModule)

        // Then
        assertEquals(mockResendAction, result.action)
        assertEquals("RESEND", result.actionType)
        assertNull(result.payload)
    }

    @Test
    fun `processContinueAction returns NoAction when module action is null`() = runBlockingTest {
        // Given
        val currentState = TestDataProvider.createMockPageState()
        val mockValidationResult = TestDataProvider.createMockValidationResult(isValid = true)
        
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        every { mockModule.getAction("INVALID_ACTION") } returns null

        // When
        val result = processor.processContinueAction(
            actionId = "INVALID_ACTION",
            progressState = true,
            currentState = currentState,
            module = mockModule
        )

        // Then
        assertTrue(result is SequentialPageResult.NoAction)
    }

    // Helper method
    private fun createMockPageModuleData(): PageModuleData {
        val mockSequentialPageContent = TestDataProvider.createMockSequentialPageContent()
        return PageModuleData(
            pageInfo = listOf(mockSequentialPageContent),
            fieldCount = 1,
            buttonState = true
        )
    }
}

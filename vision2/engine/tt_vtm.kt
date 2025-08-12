import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class SequentialPageCaptureViewModelTest {

    private val testDispatcher = TestCoroutineDispatcher()

    // Mocks
    private lateinit var mockPageConfig: SequentialPageConfig
    private lateinit var mockJourneyActionBuilder: JourneyFrameworkGenericHandleAction.Builder
    private lateinit var mockNavController: NavHostController
    private lateinit var mockProductProcessor: SequentialPageProductProcessor
    private lateinit var mockInvestmentAccountProcessor: SequentialPageInvestmentAccountProcessor
    private lateinit var mockModuleDataProcessor: SequentialPageModuleProcessor
    private lateinit var mockPageDataProcessor: SequentialPageDataProcessor
    private lateinit var mockValidationDelegator: SequentialPageValidationDelegator
    private lateinit var mockPayloadGenerator: SequentialPagePayloadGenerator
    private lateinit var mockAnalyticsManager: SequentialPageAnalyticsEvent
    private lateinit var mockModule: SequentialPageCaptureModule
    private lateinit var mockModuleDataFlow: MutableStateFlow<SequentialPageCaptureModule?>

    // Test subject
    private lateinit var viewModel: SequentialPageCaptureViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)
        
        // Initialize mocks
        mockPageConfig = mockk(relaxed = true)
        mockJourneyActionBuilder = mockk(relaxed = true)
        mockNavController = mockk(relaxed = true)
        mockProductProcessor = mockk(relaxed = true)
        mockInvestmentAccountProcessor = mockk(relaxed = true)
        mockModuleDataProcessor = mockk(relaxed = true)
        mockPageDataProcessor = mockk(relaxed = true)
        mockValidationDelegator = mockk(relaxed = true)
        mockPayloadGenerator = mockk(relaxed = true)
        mockAnalyticsManager = mockk(relaxed = true)
        mockModule = mockk(relaxed = true)
        mockModuleDataFlow = MutableStateFlow(null)

        // Setup mock configuration
        every { mockPageConfig.navController } returns mockNavController
        every { mockPageConfig.journeyActionBuilder } returns mockJourneyActionBuilder
        every { mockPageConfig.productProcessor } returns mockProductProcessor
        every { mockPageConfig.investmentAccountProcessor } returns mockInvestmentAccountProcessor
        every { mockPageConfig.moduleDataProcessor } returns mockModuleDataProcessor
        every { mockPageConfig.pageDataProcessor } returns mockPageDataProcessor
        every { mockPageConfig.validationDelegator } returns mockValidationDelegator
        every { mockPageConfig.payloadGenerator } returns mockPayloadGenerator
        every { mockPageConfig.analyticsManager } returns mockAnalyticsManager
        every { mockModuleDataProcessor.dataStateFlow } returns mockModuleDataFlow

        // Setup common mock responses
        setupCommonMockResponses()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    private fun setupCommonMockResponses() {
        val mockPageData = createMockPageModuleData()
        val mockSequentialPageSections = mockk<SequentialPageSections>(relaxed = true)
        val mockPageDataList = listOf<SequentialPageModuleResponse>()
        
        every { mockModule.sequentialPageSections } returns mockSequentialPageSections
        every { mockModule.sequentialPageDataList } returns mockPageDataList
        every { mockSequentialPageSections.analyticsPageTag } returns "test_page_tag"
        every { mockPageDataProcessor.processModuleData(any()) } returns mockPageData
        every { mockPageDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { mockPageDataProcessor.getInvestmentProducts(any()) } returns emptyList()
    }

    private fun createMockPageModuleData(): PageModuleData {
        val mockSequentialPageContent = mockk<SequentialPageContent>(relaxed = true)
        every { mockSequentialPageContent.pageComponentType } returns mockk<SequentialPageCaptureComponentType>()
        every { mockSequentialPageContent.pageComponentType.toString() } returns "INPUT_FIELD"
        
        return PageModuleData(
            pageInfo = listOf(mockSequentialPageContent),
            fieldCount = 1,
            buttonState = true
        )
    }

    @Test
    fun `test initialization and module data flow collection`() = runBlockingTest {
        // When
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        
        // Emit a module to trigger collection
        mockModuleDataFlow.value = mockModule
        
        // Then
        verify { mockPageConfig.moduleDataProcessor }
        verify { mockModuleDataProcessor.dataStateFlow }
    }

    @Test
    fun `test getModule returns correct module`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        // When
        val result = viewModel.getModule()
        
        // Then
        assertEquals(mockModule, result)
    }

    @Test
    fun `test onUIUpdateEvent with ModuleStart event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        val moduleStartEvent = ModuleStart(mockModule)
        
        // When
        viewModel.onUIUpdateEvent(moduleStartEvent)
        
        // Then
        verify { mockAnalyticsManager.logScreenLoad("test_page_tag") }
        verify { mockPageDataProcessor.processModuleData(any()) }
    }

    @Test
    fun `test onUIUpdateEvent with FieldChanged event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val fieldChangedEvent = FieldChanged(
            key = "test_field",
            value = "test_value",
            contentType = mockk<OutputTransformation>()
        )
        
        val mockPageInputFieldData = PageInputFieldData(
            inputValue = "test_value",
            pageItem = mockk<SequentialPageContent>(relaxed = true)
        )
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isValid } returns true
        
        every { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns mockPageInputFieldData
        every { mockValidationDelegator.validateField(any(), any(), any(), any()) } returns mockValidationResult
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify { mockPageDataProcessor.getCappedValue(any(), any(), "test_field", "test_value", any()) }
        verify { mockValidationDelegator.validateField(any(), any(), any(), any()) }
    }

    @Test
    fun `test onUIUpdateEvent with FieldChanged event - validation error`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val fieldChangedEvent = FieldChanged(
            key = "test_field",
            value = "invalid_value",
            contentType = null
        )
        
        val mockPageContent = mockk<SequentialPageContent>(relaxed = true)
        val mockInputFieldState = mockk<SequentialPageInputFieldState>(relaxed = true)
        val mockPageErrorState = mockk<SequentialPageUiErrorState>(relaxed = true)
        
        every { mockPageContent.pageKey } returns "test_field"
        every { mockPageContent.inputFieldState } returns mockInputFieldState
        every { mockPageContent.pageErrorState } returns mockPageErrorState
        every { mockInputFieldState.copy(any(), any()) } returns mockInputFieldState
        every { mockPageErrorState.copy(any(), any()) } returns mockPageErrorState
        every { mockPageContent.copy(any(), any()) } returns mockPageContent
        
        val mockPageInputFieldData = PageInputFieldData(
            inputValue = "invalid_value",
            pageItem = mockPageContent
        )
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isValid } returns false
        every { mockValidationResult.message } returns "Error message"
        
        every { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns mockPageInputFieldData
        every { mockValidationDelegator.validateField(any(), any(), any(), any()) } returns mockValidationResult
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify { mockAnalyticsManager.logInputViewError("Error message") }
    }

    @Test
    fun `test onUIUpdateEvent with InvestmentProductSelected event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val productSelectedEvent = InvestmentProductSelected(productId = 123)
        
        val mockProductData = mockk<SequentialPageSelectedProductData>(relaxed = true)
        every { mockProductData.optionId } returns "product_option_123"
        
        every { mockProductProcessor.processSelectedProduct(any(), any(), any()) } returns mockk<SequentialPageState>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(productSelectedEvent)
        
        // Then
        verify { mockProductProcessor.processSelectedProduct(any(), 123, any()) }
    }

    @Test
    fun `test onUIUpdateEvent with InvestmentAccountSelected event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val mockInvestmentAccount = mockk<SequentialPageInvestmentAccount>(relaxed = true)
        every { mockInvestmentAccount.optionId } returns "account_123"
        every { mockInvestmentAccount.radioTitle } returns "Test Account"
        
        val accountSelectedEvent = InvestmentAccountSelected(productInfo = mockInvestmentAccount)
        
        every { mockInvestmentAccountProcessor.updateSelectedInvestmentAccount(any(), any()) } returns mockk<SequentialPageState>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(accountSelectedEvent)
        
        // Then
        verify { mockAnalyticsManager.logProductSelection("account_123") }
        verify { mockInvestmentAccountProcessor.updateSelectedInvestmentAccount(any(), mockInvestmentAccount) }
    }

    @Test
    fun `test onUIUpdateEvent with NextClickEvent - validation success with OTP step`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Continue",
            actionId = "NEXT",
            showProgress = true
        )
        
        val mockAction = mockk<NvcAction>(relaxed = true)
        every { mockAction.stepId } returns "OTP_VERIFICATION_STATUS_CHECK"
        every { mockModule.getAction("NEXT") } returns mockAction
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isPageDataValid } returns true
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockAnalyticsManager.logButtonClick("Continue") }
        verify { mockValidationDelegator.validateAllFields(any(), any(), any()) }
        verify { mockModule.attachPayloadToNvcAction(null, mockAction) }
    }

    @Test
    fun `test onUIUpdateEvent with NextClickEvent - validation success with PRODUCT_SELECTION_SAVE`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Save",
            actionId = "SAVE",
            showProgress = false
        )
        
        val mockAction = mockk<NvcAction>(relaxed = true)
        every { mockAction.stepId } returns "PRODUCT_SELECTION_SAVE"
        every { mockModule.getAction("SAVE") } returns mockAction
        
        val mockModuleResponse = mockk<SequentialPageModuleResponse>(relaxed = true)
        every { mockModuleResponse.id } returns "product_id"
        every { mockModule.sequentialPageDataList } returns listOf(mockModuleResponse)
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isPageDataValid } returns true
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        
        val mockPayload = mockk<JourneyFrameworkRequestModule.RequestData>(relaxed = true)
        every { mockPayloadGenerator.createPayload(any(), any()) } returns mockPayload
        
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockPayloadGenerator.createPayload("product_id", any()) }
        verify { mockModule.attachPayloadToNvcAction(mockPayload, mockAction) }
    }

    @Test
    fun `test onUIUpdateEvent with NextClickEvent - validation failure`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Continue",
            actionId = "NEXT",
            showProgress = true
        )
        
        val mockPageContent = mockk<SequentialPageContent>(relaxed = true)
        val mockPageErrorState = mockk<SequentialPageUiErrorState>(relaxed = true)
        every { mockPageContent.pageErrorState } returns mockPageErrorState
        every { mockPageErrorState.isError } returns true
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isPageDataValid } returns false
        every { mockValidationResult.updatedItems } returns listOf(mockPageContent)
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockValidationDelegator.validateAllFields(any(), any(), any()) }
        // Verify that showErrorsOnInvalidFields is set to true and state is updated
    }

    @Test
    fun `test onUIUpdateEvent with PageBackEvent`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val pageBackEvent = PageBackEvent(actionId = "BACK")
        
        val mockAction = mockk<NvcAction>(relaxed = true)
        every { mockModule.getAction("BACK") } returns mockAction
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(pageBackEvent)
        
        // Then
        verify { mockAnalyticsManager.logButtonClick("BACK") }
        verify { mockModule.getAction("BACK") }
    }

    @Test
    fun `test onUIUpdateEvent with HyperLinkClicked - PRODUCT_SELECTION_SAVE scenario`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val hyperLinkEvent = HyperLinkClicked()
        
        val mockNextAction = mockk<NvcAction>(relaxed = true)
        every { mockNextAction.stepId } returns "PRODUCT_SELECTION_SAVE"
        every { mockModule.getAction("NEXT") } returns mockNextAction
        every { mockModule.getAction("NONE_OF_THESE") } returns mockk<NvcAction>(relaxed = true)
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(hyperLinkEvent)
        
        // Then
        verify { mockAnalyticsManager.logHyperLink("NONE_OF_THESE") }
        verify { mockModule.getAction("NONE_OF_THESE") }
    }

    @Test
    fun `test onUIUpdateEvent with HyperLinkClicked - RESEND scenario`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val hyperLinkEvent = HyperLinkClicked()
        
        val mockNextAction = mockk<NvcAction>(relaxed = true)
        every { mockNextAction.stepId } returns "OTHER_STEP"
        every { mockModule.getAction("NEXT") } returns mockNextAction
        every { mockModule.getAction("RESEND") } returns mockk<NvcAction>(relaxed = true)
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(hyperLinkEvent)
        
        // Then
        verify { mockAnalyticsManager.logHyperLink("RESEND") }
        verify { mockModule.getAction("RESEND") }
    }

    @Test
    fun `test logAccordionStateChange with expand state`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        
        // When
        viewModel.logAccordionStateChange(state = true, label = "Test Accordion")
        
        // Then
        verify { mockAnalyticsManager.logAccordionInteraction("Test Accordion", "ACCORDION_EXPAND") }
    }

    @Test
    fun `test logAccordionStateChange with collapsed state`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        
        // When
        viewModel.logAccordionStateChange(state = false, label = "Test Accordion")
        
        // Then
        verify { mockAnalyticsManager.logAccordionInteraction("Test Accordion", "ACCORDION_COLLAPSED") }
    }

    @Test
    fun `test logInputViewFocusChange with non-null label`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        
        // When
        viewModel.logInputViewFocusChange("Test Input")
        
        // Then
        verify { mockAnalyticsManager.logInputViewChangeFocus("Test Input") }
    }

    @Test
    fun `test logInputViewFocusChange with null label`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        
        // When
        viewModel.logInputViewFocusChange(null)
        
        // Then
        verify(exactly = 0) { mockAnalyticsManager.logInputViewChangeFocus(any()) }
    }

    @Test
    fun `test FieldChanged with null key`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        val fieldChangedEvent = FieldChanged(key = null, value = "test", contentType = null)
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify(exactly = 0) { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test NextClickEvent with default else branch`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Continue",
            actionId = "NEXT",
            showProgress = true
        )
        
        val mockAction = mockk<NvcAction>(relaxed = true)
        every { mockAction.stepId } returns "OTHER_STEP_TYPE"
        every { mockModule.getAction("NEXT") } returns mockAction
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isPageDataValid } returns true
        every { mockValidationResult.updatedItems } returns emptyList()
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        
        val mockPayload = mockk<JourneyFrameworkRequestModule.RequestData>(relaxed = true)
        every { mockPayloadGenerator.createPayload(any<List<SequentialPageContent>>()) } returns mockPayload
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockPayloadGenerator.createPayload(emptyList<SequentialPageContent>()) }
        verify { mockModule.attachPayloadToNvcAction(mockPayload, mockAction) }
    }

    @Test
    fun `test processSelectedInvestmentAccount with fallback title`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val mockInvestmentAccount = mockk<SequentialPageInvestmentAccount>(relaxed = true)
        every { mockInvestmentAccount.optionId } returns null
        every { mockInvestmentAccount.radioTitle } returns null
        
        val accountSelectedEvent = InvestmentAccountSelected(productInfo = mockInvestmentAccount)
        
        every { mockInvestmentAccountProcessor.updateSelectedInvestmentAccount(any(), any()) } returns mockk<SequentialPageState>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(accountSelectedEvent)
        
        // Then
        verify { mockAnalyticsManager.logProductSelection("PRODUCT_TITLE_WITH_NO_VALUE") }
    }

    @Test
    fun `test module initialization when dataStateFlow emits null`() = runBlockingTest {
        // Given
        mockModuleDataFlow.value = null
        
        // When
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        
        // Then
        // Verify that no processing occurs when null module is emitted
        verify(exactly = 0) { mockPageDataProcessor.processModuleData(any()) }
    }

    @Test
    fun `test processStartModule with null analytics page tag`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        val mockSequentialPageSections = mockk<SequentialPageSections>(relaxed = true)
        every { mockModule.sequentialPageSections } returns mockSequentialPageSections
        every { mockSequentialPageSections.analyticsPageTag } returns null
        
        val moduleStartEvent = ModuleStart(mockModule)
        
        // When
        viewModel.onUIUpdateEvent(moduleStartEvent)
        
        // Then
        verify(exactly = 0) { mockAnalyticsManager.logScreenLoad(any()) }
    }

    @Test
    fun `test FieldChanged with null pageItem in getCappedValue response`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val fieldChangedEvent = FieldChanged(
            key = "test_field",
            value = "test_value",
            contentType = null
        )
        
        val mockPageInputFieldData = PageInputFieldData(
            inputValue = "test_value",
            pageItem = null // null pageItem
        )
        
        every { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns mockPageInputFieldData
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify { mockPageDataProcessor.getCappedValue(any(), any(), "test_field", "test_value", any()) }
        // Should not call validation when pageItem is null
        verify(exactly = 0) { mockValidationDelegator.validateField(any(), any(), any(), any()) }
    }

    @Test
    fun `test NextClickEvent with null action from module`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Continue",
            actionId = "INVALID_ACTION",
            showProgress = true
        )
        
        every { mockModule.getAction("INVALID_ACTION") } returns null
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isPageDataValid } returns true
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockValidationDelegator.validateAllFields(any(), any(), any()) }
        // Should not proceed with journey launch when action is null
        verify(exactly = 0) { mockJourneyActionBuilder.build() }
    }

    @Test
    fun `test PageBackEvent with null action from module`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val pageBackEvent = PageBackEvent(actionId = "INVALID_BACK")
        
        every { mockModule.getAction("INVALID_BACK") } returns null
        
        // When
        viewModel.onUIUpdateEvent(pageBackEvent)
        
        // Then
        verify { mockAnalyticsManager.logButtonClick("INVALID_BACK") }
        verify { mockModule.getAction("INVALID_BACK") }
        // Should not proceed with journey launch when action is null
        verify(exactly = 0) { mockJourneyActionBuilder.build() }
    }

    @Test
    fun `test NextClickEvent with PRODUCT_SELECTION_SAVE but no valid module data`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Save",
            actionId = "SAVE",
            showProgress = false
        )
        
        val mockAction = mockk<NvcAction>(relaxed = true)
        every { mockAction.stepId } returns "PRODUCT_SELECTION_SAVE"
        every { mockModule.getAction("SAVE") } returns mockAction
        
        // Empty data list - no valid module data
        every { mockModule.sequentialPageDataList } returns emptyList()
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isPageDataValid } returns true
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockValidationDelegator.validateAllFields(any(), any(), any()) }
        // Should still attach null payload and launch journey
        verify { mockModule.attachPayloadToNvcAction(null, mockAction) }
    }

    @Test
    fun `test NextClickEvent with PRODUCT_SELECTION_SAVE and null product option ID`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Save",
            actionId = "SAVE",
            showProgress = false
        )
        
        val mockAction = mockk<NvcAction>(relaxed = true)
        every { mockAction.stepId } returns "PRODUCT_SELECTION_SAVE"
        every { mockModule.getAction("SAVE") } returns mockAction
        
        val mockModuleResponse = mockk<SequentialPageModuleResponse>(relaxed = true)
        every { mockModuleResponse.id } returns "product_id"
        every { mockModule.sequentialPageDataList } returns listOf(mockModuleResponse)
        
        // Mock product picker state with null optionId
        val mockProductData = mockk<SequentialPageSelectedProductData>(relaxed = true)
        every { mockProductData.optionId } returns null
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isPageDataValid } returns true
        every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
        
        every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockValidationDelegator.validateAllFields(any(), any(), any()) }
        // Should attach null payload when optionId is null
        verify { mockModule.attachPayloadToNvcAction(null, mockAction) }
    }

    @Test
    fun `test all INVESTMENT step IDs in NextClickEvent`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val stepIds = listOf(
            "INVESTMENT_MASS_AFFLUENT_CALL_HELPDESK",
            "INVESTMENT_INITIALISE",
            "PENDING_ACTIVATION_CODE_INITIAL"
        )
        
        stepIds.forEach { stepId ->
            val nextClickEvent = NextClickEvent(
                actionLabel = "Continue",
                actionId = "NEXT",
                showProgress = true
            )
            
            val mockAction = mockk<NvcAction>(relaxed = true)
            every { mockAction.stepId } returns stepId
            every { mockModule.getAction("NEXT") } returns mockAction
            
            val mockValidationResult = mockk<ValidationResult>(relaxed = true)
            every { mockValidationResult.isPageDataValid } returns true
            every { mockValidationDelegator.validateAllFields(any(), any(), any()) } returns mockValidationResult
            
            every { mockJourneyActionBuilder.build() } returns mockk<JourneyFrameworkGenericHandleAction>(relaxed = true)
            
            // When
            viewModel.onUIUpdateEvent(nextClickEvent)
            
            // Then
            verify { mockModule.attachPayloadToNvcAction(null, mockAction) }
        }
    }

    @Test
    fun `test state flows are properly exposed`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        
        // When & Then
        assertNotNull(viewModel.pageState)
        assertNotNull(viewModel.isContinueButtonEnabled)
        assertTrue(viewModel.pageState is StateFlow<SequentialPageState>)
        assertTrue(viewModel.isContinueButtonEnabled is StateFlow<Boolean>)
    }

    @Test
    fun `test button state updates correctly during product selection`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        // Initially button should be enabled (from isContinueButtonEnabledFlowState default)
        assertTrue(viewModel.isContinueButtonEnabled.value)
        
        val productSelectedEvent = InvestmentProductSelected(productId = 123)
        every { mockProductProcessor.processSelectedProduct(any(), any(), any()) } returns mockk<SequentialPageState>(relaxed = true)
        
        // When
        viewModel.onUIUpdateEvent(productSelectedEvent)
        
        // Then
        assertTrue(viewModel.isContinueButtonEnabled.value)
    }

    @Test
    fun `test error message logging in field validation`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder)
        viewModel.module = mockModule
        
        val fieldChangedEvent = FieldChanged(
            key = "test_field",
            value = "invalid_value",
            contentType = null
        )
        
        val mockPageContent = mockk<SequentialPageContent>(relaxed = true)
        val mockInputFieldState = mockk<SequentialPageInputFieldState>(relaxed = true)
        val mockPageErrorState = mockk<SequentialPageUiErrorState>(relaxed = true)
        
        every { mockPageContent.pageKey } returns "test_field"
        every { mockPageContent.inputFieldState } returns mockInputFieldState
        every { mockPageContent.pageErrorState } returns mockPageErrorState
        every { mockInputFieldState.copy(any(), any()) } returns mockInputFieldState
        every { mockPageErrorState.copy(any(), any()) } returns mockPageErrorState
        every { mockPageContent.copy(any(), any()) } returns mockPageContent
        
        val mockPageInputFieldData = PageInputFieldData(
            inputValue = "invalid_value",
            pageItem = mockPageContent
        )
        
        val mockValidationResult = mockk<ValidationResult>(relaxed = true)
        every { mockValidationResult.isValid } returns false
        every { mockValidationResult.message } returns null // null message
        
        every { mockPageDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns mockPageInputFieldData
        every { mockValidationDelegator.validateField(any(), any(), any(), any()) } returns mockValidationResult
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        // Should not log error when message is null
        verify(exactly = 0) { mockAnalyticsManager.logInputViewError(any()) }
    }

    // Helper extension function to check string values
    private fun String?.isNotNullStringValue(): Boolean = !this.isNullOrEmpty()
}

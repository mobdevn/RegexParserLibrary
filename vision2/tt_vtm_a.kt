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
    private lateinit var mockProcessor: SequentialPageProcessor
    private lateinit var mockModuleDataProcessor: SequentialPageModuleProcessor
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
        mockProcessor = mockk(relaxed = true)
        mockModuleDataProcessor = mockk(relaxed = true)
        mockAnalyticsManager = mockk(relaxed = true)
        mockModule = mockk(relaxed = true)
        mockModuleDataFlow = MutableStateFlow(null)

        // Setup mock configuration
        every { mockPageConfig.moduleDataProcessor } returns mockModuleDataProcessor
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
        every { mockModule.id } returns "test_module_id"
        every { mockModule.attachPayloadToNvcAction(any(), any()) } just Runs
        every { mockJourneyActionBuilder.build() } returns TestDataProvider.createMockJourneyAction()
    }

    @Test
    fun `test initialization and module data flow collection`() = runBlockingTest {
        // When
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        
        // Emit a module to trigger collection
        mockModuleDataFlow.value = mockModule
        
        // Then
        verify { mockPageConfig.moduleDataProcessor }
        verify { mockModuleDataProcessor.dataStateFlow }
    }

    @Test
    fun `test getModule returns correct module`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        // When
        val result = viewModel.getModule()
        
        // Then
        assertEquals(mockModule, result)
    }

    @Test
    fun `test onUIUpdateEvent with ModuleStart event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        val moduleStartEvent = ModuleStart(mockModule)
        val mockResult = TestDataProvider.createMockModuleStartResult()
        
        every { mockProcessor.processModuleStart(mockModule) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(moduleStartEvent)
        
        // Then
        verify { mockProcessor.processModuleStart(mockModule) }
        verify { mockAnalyticsManager.logScreenLoad("test_analytics_tag") }
    }

    @Test
    fun `test onUIUpdateEvent with FieldChanged event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val fieldChangedEvent = FieldChanged(
            key = "test_field",
            value = "test_value",
            contentType = null
        )
        
        val mockResult = TestDataProvider.createMockFieldInputResult(isValid = true)
        every { mockProcessor.processFieldInput(any(), any(), any(), any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify { mockProcessor.processFieldInput(
            "test_field", 
            "test_value", 
            null, 
            any(), 
            mockModule
        ) }
    }

    @Test
    fun `test onUIUpdateEvent with FieldChanged event - validation error logs analytics`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val fieldChangedEvent = FieldChanged(
            key = "test_field",
            value = "invalid_value",
            contentType = null
        )
        
        val mockResult = TestDataProvider.createMockFieldInputResult(isValid = false)
        every { mockProcessor.processFieldInput(any(), any(), any(), any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify { mockAnalyticsManager.logInputViewError("Validation error") }
    }

    @Test
    fun `test onUIUpdateEvent with InvestmentProductSelected event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val productSelectedEvent = InvestmentProductSelected(productId = 123)
        val mockResult = TestDataProvider.createMockProductSelectionResult()
        
        every { mockProcessor.processProductSelection(any(), any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(productSelectedEvent)
        
        // Then
        verify { mockProcessor.processProductSelection(123, any(), any()) }
        verify { mockAnalyticsManager.logProductSelection("selected_product_123") }
        assertTrue(viewModel.isContinueButtonEnabled.value)
    }

    @Test
    fun `test onUIUpdateEvent with InvestmentAccountSelected event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val mockAccount = TestDataProvider.createMockInvestmentAccounts().first()
        val accountSelectedEvent = InvestmentAccountSelected(productInfo = mockAccount)
        val mockResult = TestDataProvider.createMockAccountSelectionResult()
        
        every { mockProcessor.processAccountSelection(any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(accountSelectedEvent)
        
        // Then
        verify { mockProcessor.processAccountSelection(mockAccount, any()) }
        verify { mockAnalyticsManager.logProductSelection("test_account_label") }
        assertTrue(viewModel.isContinueButtonEnabled.value)
    }

    @Test
    fun `test onUIUpdateEvent with NextClickEvent - validation success`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Continue",
            actionId = "NEXT",
            showProgress = true
        )
        
        val mockResult = TestDataProvider.createMockSequentialPageSuccessResult()
        every { mockProcessor.processContinueAction(any(), any(), any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockAnalyticsManager.logButtonClick("Continue") }
        verify { mockProcessor.processContinueAction("NEXT", true, any(), mockModule) }
        verify { mockModule.attachPayloadToNvcAction(null, mockResult.action) }
    }

    @Test
    fun `test onUIUpdateEvent with NextClickEvent - validation failure`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Continue",
            actionId = "NEXT",
            showProgress = true
        )
        
        val mockResult = TestDataProvider.createMockSequentialPageValidationFailureResult()
        every { mockProcessor.processContinueAction(any(), any(), any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockAnalyticsManager.logButtonClick("Continue") }
        verify { mockProcessor.processContinueAction("NEXT", true, any(), mockModule) }
        // Should not launch journey on validation failure
        verify(exactly = 0) { mockModule.attachPayloadToNvcAction(any(), any()) }
    }

    @Test
    fun `test onUIUpdateEvent with PageBackEvent`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val pageBackEvent = PageBackEvent(actionId = "BACK")
        val mockResult = TestDataProvider.createMockBackNavigationResult()
        
        every { mockProcessor.processBackNavigation(any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(pageBackEvent)
        
        // Then
        verify { mockAnalyticsManager.logButtonClick("BACK") }
        verify { mockProcessor.processBackNavigation("BACK", mockModule) }
        verify { mockModule.attachPayloadToNvcAction(mockResult.payload, mockResult.action) }
    }

    @Test
    fun `test onUIUpdateEvent with HyperLinkClicked event`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val hyperLinkEvent = HyperLinkClicked()
        val mockResult = TestDataProvider.createMockHyperlinkActionResult()
        
        every { mockProcessor.processHyperlinkAction(any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(hyperLinkEvent)
        
        // Then
        verify { mockProcessor.processHyperlinkAction(mockModule) }
        verify { mockAnalyticsManager.logHyperLink("RESEND") }
        verify { mockModule.attachPayloadToNvcAction(mockResult.payload, mockResult.action) }
    }

    @Test
    fun `test logAccordionStateChange with expand state`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        
        // When
        viewModel.logAccordionStateChange(state = true, label = "Test Accordion")
        
        // Then
        verify { mockAnalyticsManager.logAccordionInteraction("Test Accordion", "ACCORDION_EXPAND") }
    }

    @Test
    fun `test logAccordionStateChange with collapsed state`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        
        // When
        viewModel.logAccordionStateChange(state = false, label = "Test Accordion")
        
        // Then
        verify { mockAnalyticsManager.logAccordionInteraction("Test Accordion", "ACCORDION_COLLAPSED") }
    }

    @Test
    fun `test logInputViewFocusChange with non-null label`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        
        // When
        viewModel.logInputViewFocusChange("Test Input")
        
        // Then
        verify { mockAnalyticsManager.logInputViewChangeFocus("Test Input") }
    }

    @Test
    fun `test logInputViewFocusChange with null label`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        
        // When
        viewModel.logInputViewFocusChange(null)
        
        // Then
        verify(exactly = 0) { mockAnalyticsManager.logInputViewChangeFocus(any()) }
    }

    @Test
    fun `test FieldChanged with null key does not process`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        val fieldChangedEvent = FieldChanged(key = null, value = "test", contentType = null)
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify(exactly = 0) { mockProcessor.processFieldInput(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test state flows are properly exposed`() {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        
        // When & Then
        assertNotNull(viewModel.pageState)
        assertNotNull(viewModel.isContinueButtonEnabled)
        assertTrue(viewModel.pageState is StateFlow<SequentialPageState>)
        assertTrue(viewModel.isContinueButtonEnabled is StateFlow<Boolean>)
    }

    @Test
    fun `test button state updates correctly during events`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        // Initially button should be enabled
        assertTrue(viewModel.isContinueButtonEnabled.value)
        
        val productSelectedEvent = InvestmentProductSelected(productId = 123)
        val mockResult = TestDataProvider.createMockProductSelectionResult()
        every { mockProcessor.processProductSelection(any(), any(), any()) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(productSelectedEvent)
        
        // Then
        assertTrue(viewModel.isContinueButtonEnabled.value)
    }

    @Test
    fun `test module start with null analytics tag does not log`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        val moduleStartEvent = ModuleStart(mockModule)
        val mockResult = TestDataProvider.createMockModuleStartResult().copy(analyticsPageTag = null)
        
        every { mockProcessor.processModuleStart(mockModule) } returns mockResult
        
        // When
        viewModel.onUIUpdateEvent(moduleStartEvent)
        
        // Then
        verify { mockProcessor.processModuleStart(mockModule) }
        verify(exactly = 0) { mockAnalyticsManager.logScreenLoad(any()) }
    }

    @Test
    fun `test field input with null result does not update state`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val fieldChangedEvent = FieldChanged(
            key = "test_field",
            value = "test_value",
            contentType = null
        )
        
        every { mockProcessor.processFieldInput(any(), any(), any(), any(), any()) } returns null
        
        // When
        viewModel.onUIUpdateEvent(fieldChangedEvent)
        
        // Then
        verify { mockProcessor.processFieldInput("test_field", "test_value", null, any(), mockModule) }
        // State should not be updated when result is null
    }

    @Test
    fun `test continue action with NoAction result does not launch journey`() = runBlockingTest {
        // Given
        viewModel = SequentialPageCaptureViewModel(mockPageConfig, mockJourneyActionBuilder, mockProcessor)
        viewModel.module = mockModule
        
        val nextClickEvent = NextClickEvent(
            actionLabel = "Continue",
            actionId = "INVALID_ACTION",
            showProgress = true
        )
        
        every { mockProcessor.processContinueAction(any(), any(), any(), any()) } returns SequentialPageResult.NoAction("Invalid action")
        
        // When
        viewModel.onUIUpdateEvent(nextClickEvent)
        
        // Then
        verify { mockAnalyticsManager.logButtonClick("Continue") }
        verify { mockProcessor.processContinueAction("INVALID_ACTION", true, any(), mockModule) }
        verify(exactly = 0) { mockModule.attachPayloadToNvcAction(any(), any()) }
    }
}

    // Helper extension function to check string values
    private fun String?.isNotNullStringValue(): Boolean = !this.isNullOrEmpty()
}

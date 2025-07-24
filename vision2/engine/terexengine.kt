import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Define constants at the top level for better visibility
private const val TEXT_TO_EMPTY = ""

class YourClassTest {

    // region Mocks and System Under Test
    private val analyticsManager: AnalyticsManager = mockk(relaxed = true)
    private val fieldDataProcessor: FieldDataProcessor = mockk()
    private val moduleDataProcessor: ModuleDataProcessor = mockk()

    // Use real state flows for testing, as this is the recommended approach.
    private val isContinueButtonEnabledFlowState = MutableStateFlow(false)
    private val pageMutableStateFlow = MutableStateFlow(createInitialPageState())

    // Mock the dataStateFlow that's used in the class's init block
    private val mockDataStateFlow: StateFlow<SequentialPageCaptureModule?> = MutableStateFlow(null)

    // Class under test
    private lateinit var classUnderTest: YourClass
    // endregion

    @BeforeEach
    fun setup() {
        // Clear all mocks before each test to ensure isolation
        clearAllMocks()

        // Stub the dataStateFlow used in the init block of YourClass
        every { moduleDataProcessor.dataStateFlow } returns mockDataStateFlow

        // Initialize the class with mocked dependencies
        classUnderTest = YourClass(
            analyticsManager = analyticsManager,
            fieldDataProcessor = fieldDataProcessor,
            moduleDataProcessor = moduleDataProcessor,
            isContinueButtonEnabledFlowState = isContinueButtonEnabledFlowState,
            pageMutableStateFlow = pageMutableStateFlow
        )
    }

    //--------------------------------------------------------------------------------
    // region Helper Functions
    //--------------------------------------------------------------------------------

    /**
     * Creates an initial, real PageState object for tests.
     * Only mocks parts that are complex or not relevant to the initial state.
     */
    private fun createInitialPageState(): PageState {
        val accordionState = AccordionState(accordionPair = emptyList())
        val sequentialPageState = SequentialPageState(items = emptyList(), accordionState = accordionState)
        val productPickerState = ProductPickerState(products = emptyList())

        return PageState(
            sequentialPageState = sequentialPageState,
            productPickerState = productPickerState,
            inputViewFieldCount = 0,
            sequentialPageSections = mockk(relaxed = true), // Relaxed mock for simplicity
            viewType = null
        )
    }

    /**
     * Creates a fully mocked PageState object. Useful for placeholder values.
     */
    private fun createMockPageState(): PageState {
        return mockk(relaxed = true)
    }
    
    /**
     * Sets up default behavior for mocked dependencies.
     */
    private fun setupDefaultMocks() {
        every { fieldDataProcessor.processModuleData(any()) } returns Triple(emptyList(), 0, false)
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyList()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
    }
    
    /**
     * Creates a mock SequentialPageCaptureModule with specific stubs.
     * Using a strict mock (no 'relaxed = true') forces all behavior to be explicitly defined.
     */
    private fun createMockModule(analyticsTag: String? = "test_tag"): SequentialPageCaptureModule {
        return mockk {
            every { sequentialPageSections } returns mockk {
                every { analyticsPageTag } returns analyticsTag
            }
            every { sequentialPageDataList } returns emptyList()
        }
    }

    // endregion

    //--------------------------------------------------------------------------------
    // region Tests
    //--------------------------------------------------------------------------------

    @Test
    fun `processStartModule should set module, update state, and log analytics`() = runTest {
        // Arrange
        val mockAnalyticsTag = "test_page_tag"
        val mockModule = createMockModule(analyticsTag = mockAnalyticsTag)
        val mockItems = listOf(mockk<Item>(relaxed = true))
        val mockInputFieldCount = 5
        val mockIsButtonState = true
        val mockAccordionItems = listOf(mockk<AccordionItem>())
        val mockInvestmentProducts = listOf(mockk<InvestmentProduct>())
        
        // Define behavior for the data processor
        every { fieldDataProcessor.processModuleData(any()) } returns Triple(mockItems, mockInputFieldCount, mockIsButtonState)
        every { fieldDataProcessor.getAccordionData(mockItems) } returns mockAccordionItems
        every { fieldDataProcessor.getInvestmentProducts(mockItems) } returns mockInvestmentProducts

        val initialPageState = pageMutableStateFlow.value

        // Act
        classUnderTest.processStartModule(mockModule)

        // Assert
        // Verify interactions
        verify(exactly = 1) { analyticsManager.logScreenLoad(analyticsPageTag = mockAnalyticsTag) }
        verify(exactly = 1) { fieldDataProcessor.processModuleData(any()) }
        verify(exactly = 1) { fieldDataProcessor.getAccordionData(mockItems) }
        verify(exactly = 1) { fieldDataProcessor.getInvestmentProducts(mockItems) }

        // Verify state flow updates by checking their values
        assertEquals(mockIsButtonState, isContinueButtonEnabledFlowState.value)
        assertNotEquals(initialPageState, pageMutableStateFlow.value)
        
        // Verify specific properties of the updated page state
        val updatedPageState = pageMutableStateFlow.value
        assertEquals(mockItems, updatedPageState.sequentialPageState.items)
        assertEquals(mockInputFieldCount, updatedPageState.inputViewFieldCount)
        assertEquals(mockAccordionItems, updatedPageState.sequentialPageState.accordionState.accordionPair)
        assertEquals(mockInvestmentProducts, updatedPageState.productPickerState.products)
    }

    @Test
    fun `processStartModule should use TEXT_TO_EMPTY for analytics tag when it is null`() = runTest {
        // Arrange
        val mockModuleWithNullTag = createMockModule(analyticsTag = null)
        setupDefaultMocks()

        // Act
        classUnderTest.processStartModule(mockModuleWithNullTag)

        // Assert
        verify(exactly = 1) { analyticsManager.logScreenLoad(analyticsPageTag = TEXT_TO_EMPTY) }
    }

    @Test
    fun `processStartModule should correctly determine viewType from items list`() = runTest {
        // Arrange
        // Create simple test objects instead of complex mocks for clarity
        class EmptyType { override fun toString() = "" }
        class ValidType { override fun toString() = "VALID_TYPE" }

        val mockModule = createMockModule()
        val mockItems = listOf(
            mockk<Item>(relaxed = true) { every { type } returns EmptyType() },
            mockk<Item>(relaxed = true) { every { type } returns ValidType() }
        )
        
        every { fieldDataProcessor.processModuleData(any()) } returns Triple(mockItems, 2, true)
        every { fieldDataProcessor.getAccordionData(mockItems) } returns emptyList()
        every { fieldDataProcessor.getInvestmentProducts(mockItems) } returns emptyList()
        
        val initialPageState = pageMutableStateFlow.value

        // Act
        classUnderTest.processStartModule(mockModule)

        // Assert
        val updatedPageState = pageMutableStateFlow.value
        assertNotEquals(initialPageState, updatedPageState, "Page state should have been updated.")
        
        // The viewType should be the first item with a non-empty type string.
        assertTrue(updatedPageState.viewType is ValidType, "viewType should be of type ValidType.")
        assertEquals("VALID_TYPE", updatedPageState.viewType.toString())
    }

    @Test
    fun `init block should not process a null module from dataStateFlow`() = runTest {
        // Arrange: A new instance is created in setup() which subscribes to mockDataStateFlow.
        // We just need to confirm that no processing happens for a null emission.
        
        // Act: The flow already holds null, which the init block will have collected.
        
        // Assert: Verify that startModule (and its subsequent calls) was never invoked.
        verify(exactly = 0) { fieldDataProcessor.processModuleData(any()) }
        verify(exactly = 0) { analyticsManager.logScreenLoad(any()) }
    }

    // endregion
}

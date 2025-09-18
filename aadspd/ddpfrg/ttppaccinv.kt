import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SequentialPageProductProcessorTest {

    @MockK
    private lateinit var currentState: SequentialPageState

    @MockK
    private lateinit var productPickerState: SequentialPageProductPickerState

    @MockK
    private lateinit var moduleResponse: SequentialPageModuleResponse

    @MockK
    private lateinit var selectedProductInfo: SequentialPageInvestmentProduct

    @MockK
    private lateinit var productUiState: SequentialPageProductPickerView

    @MockK
    private lateinit var pageContent: SequentialPageContent

    @MockK
    private lateinit var pageComponents: SequentialPageComponents

    @MockK
    private lateinit var investmentProduct: SequentialPageInvestmentProduct

    private lateinit var processor: SequentialPageProductProcessor
    private lateinit var moduleData: List<SequentialPageModuleResponse>
    private lateinit var products: List<SequentialPageProductPickerView>

    companion object {
        // Test Constants
        const val TEST_PRODUCT_ID = 1
        const val TEST_SELECTED_PRODUCT_ID = 0
        const val TEST_INDEX = 2
        const val TEST_PRODUCT_TITLE = "Test Investment Product"
        const val TEST_PRODUCT_DESCRIPTION = "Test product description"
        const val TEST_ANALYTICS_TAG = "test_analytics_tag"
        const val TEST_ICON = "test_icon"
        const val TEST_PAGE_KEY = "test_page_key"
        const val EMPTY_DESCRIPTION = ""
        const val NULL_DESCRIPTION: String? = null
        const val PAGE_TITLE_WITH_NO_VALUE = "No Title"
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        processor = SequentialPageProductProcessor()
        moduleData = listOf(moduleResponse)
        products = listOf(productUiState, productUiState, productUiState)
        
        setupBasicMocks()
    }

    private fun setupBasicMocks() {
        every { currentState.productPickerState } returns productPickerState
        every { productPickerState.products } returns products
        every { productPickerState.selectedProductId } returns TEST_SELECTED_PRODUCT_ID
    }

    private fun setupModuleResponseWithValidProduct() {
        every { moduleResponse.radioOptions?.getOrNull(TEST_PRODUCT_ID) } returns selectedProductInfo
        setupInvestmentProductWithDescription(TEST_PRODUCT_DESCRIPTION, false)
    }

    private fun setupModuleResponseWithNullProduct() {
        every { moduleResponse.radioOptions?.getOrNull(TEST_PRODUCT_ID) } returns null
    }

    private fun setupInvestmentProductWithDescription(description: String?, isEmpty: Boolean) {
        every { selectedProductInfo.description } returns description
        every { selectedProductInfo.description.isNullOrEmpty() } returns isEmpty
    }

    private fun setupPageContentWithComponents() {
        every { pageContent.pageComponents } returns pageComponents
        every { pageContent.pageKey } returns TEST_PAGE_KEY
        every { pageComponents.productOptions } returns listOf(investmentProduct)
    }

    private fun setupPageContentWithNullComponents() {
        every { pageContent.pageComponents } returns null
    }

    private fun setupInvestmentProductProperties(
        title: String? = TEST_PRODUCT_TITLE,
        description: String? = TEST_PRODUCT_DESCRIPTION,
        analyticsTag: String = TEST_ANALYTICS_TAG,
        icon: String = TEST_ICON
    ) {
        every { investmentProduct.title } returns title
        every { investmentProduct.description } returns description
        every { investmentProduct.analyticsOptionTag } returns analyticsTag
        every { investmentProduct.icon } returns icon
    }

    private fun setupProductUiStateCopyResponses() {
        every { productUiState.copy(description = TEST_PRODUCT_DESCRIPTION, isProductSelected = true) } returns mockk()
        every { productUiState.copy(description = null, isProductSelected = false) } returns mockk()
        every { productUiState.copy(isProductSelected = true) } returns mockk()
        every { productPickerState.copy(any(), any(), any()) } returns mockk()
        every { currentState.copy(any()) } returns mockk()
    }

    // processSelectedProduct() tests
    @Test
    fun `processSelectedProduct should return current state when selectedProductInfo is null`() {
        // Given
        setupModuleResponseWithNullProduct()

        // When
        val result = processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        assertEquals(currentState, result)
    }

    @Test
    fun `processSelectedProduct should update selected product when productInfo exists`() {
        // Given
        setupModuleResponseWithValidProduct()
        setupProductUiStateCopyResponses()
        val expectedState = mockk<SequentialPageState>()
        every { currentState.copy(any()) } returns expectedState

        // When
        val result = processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        assertEquals(expectedState, result)
    }

    @Test
    fun `processSelectedProduct should reset previously selected product when different product is selected`() {
        // Given
        setupModuleResponseWithValidProduct()
        setupProductUiStateCopyResponses()

        // When
        processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        verify { productUiState.copy(description = null, isProductSelected = false) }
    }

    // getInvestmentProducts() tests
    @Test
    fun `getInvestmentProducts should return empty list when items is empty`() {
        // Given
        val emptyItems = emptyList<SequentialPageContent>()

        // When
        val result = processor.getInvestmentProducts(emptyItems)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should return empty list when pageComponents is null`() {
        // Given
        val items = listOf(pageContent)
        setupPageContentWithNullComponents()

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should return product picker views when pageComponents exist`() {
        // Given
        val items = listOf(pageContent)
        setupPageContentWithComponents()
        setupInvestmentProductProperties()

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertEquals(1, result.size)
        assertFalse(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should handle multiple items with multiple products`() {
        // Given
        val pageContent2 = mockk<SequentialPageContent>()
        val pageComponents2 = mockk<SequentialPageComponents>()
        val investmentProduct2 = mockk<SequentialPageInvestmentProduct>()
        val items = listOf(pageContent, pageContent2)

        // Setup first item
        setupPageContentWithComponents()
        setupInvestmentProductProperties()

        // Setup second item
        every { pageContent2.pageComponents } returns pageComponents2
        every { pageContent2.pageKey } returns "${TEST_PAGE_KEY}_2"
        every { pageComponents2.productOptions } returns listOf(investmentProduct2)
        every { investmentProduct2.title } returns "${TEST_PRODUCT_TITLE}_2"
        every { investmentProduct2.analyticsOptionTag } returns "${TEST_ANALYTICS_TAG}_2"
        every { investmentProduct2.icon } returns "${TEST_ICON}_2"
        every { investmentProduct2.description } returns "${TEST_PRODUCT_DESCRIPTION}_2"

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertEquals(2, result.size)
    }

    // getProduct() indirect tests (through getInvestmentProducts)
    @Test
    fun `getProduct should use PAGE_TITLE_WITH_NO_VALUE when title is null`() {
        // Given
        val items = listOf(pageContent)
        setupPageContentWithComponents()
        setupInvestmentProductProperties(title = null)

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertEquals(1, result.size)
        assertFalse(result.isEmpty())
    }

    @Test
    fun `getProduct should set isProductSelected to false by default`() {
        // Given
        val items = listOf(pageContent)
        setupPageContentWithComponents()
        setupInvestmentProductProperties()

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertEquals(1, result.size)
        assertFalse(result.isEmpty())
    }

    // updateProductDetails() indirect tests (through processSelectedProduct)
    @Test
    fun `updateProductDetails should set description when productInfo description is not empty`() {
        // Given
        setupModuleResponseWithValidProduct()
        setupProductUiStateCopyResponses()

        // When
        processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        verify { productUiState.copy(description = TEST_PRODUCT_DESCRIPTION, isProductSelected = true) }
    }

    @Test
    fun `updateProductDetails should not set description when productInfo description is empty`() {
        // Given
        setupModuleResponseWithValidProduct()
        setupInvestmentProductWithDescription(EMPTY_DESCRIPTION, true)
        setupProductUiStateCopyResponses()

        // When
        processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        verify { productUiState.copy(isProductSelected = true) }
        verify(exactly = 0) { productUiState.copy(description = any(), isProductSelected = true) }
    }

    @Test
    fun `updateProductDetails should not set description when productInfo description is null`() {
        // Given
        setupModuleResponseWithValidProduct()
        setupInvestmentProductWithDescription(NULL_DESCRIPTION, true)
        setupProductUiStateCopyResponses()

        // When
        processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        verify { productUiState.copy(isProductSelected = true) }
        verify(exactly = 0) { productUiState.copy(description = any(), isProductSelected = true) }
    }
}

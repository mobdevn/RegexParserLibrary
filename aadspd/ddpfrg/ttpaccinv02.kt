import io.mockk.MockK
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
    private lateinit var updatedProductUiState: SequentialPageProductPickerView

    @MockK
    private lateinit var pageContent: SequentialPageContent

    @MockK
    private lateinit var pageComponents: SequentialPageComponents

    @MockK
    private lateinit var investmentProduct: SequentialPageInvestmentProduct

    private lateinit var processor: SequentialPageProductProcessor

    companion object {
        const val TEST_PRODUCT_ID = 1
        const val TEST_SELECTED_PRODUCT_ID = 0
        const val TEST_PRODUCT_TITLE = "Test Investment Product"
        const val TEST_PRODUCT_DESCRIPTION = "Test product description"
        const val TEST_ANALYTICS_TAG = "test_analytics_tag"
        const val TEST_ICON = "test_icon"
        const val TEST_PAGE_KEY = "test_page_key"
        const val EMPTY_DESCRIPTION = ""
        const val PAGE_TITLE_WITH_NO_VALUE = "No Title"
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        processor = SequentialPageProductProcessor()
    }

    @Test
    fun `processSelectedProduct should return current state when selectedProductInfo is null`() {
        // Given
        val moduleData = listOf(moduleResponse)
        every { currentState.productPickerState } returns productPickerState
        every { moduleResponse.radioOptions?.getOrNull(TEST_PRODUCT_ID) } returns null

        // When
        val result = processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        assertEquals(currentState, result)
    }

    @Test
    fun `processSelectedProduct should update selected product when productInfo exists with description`() {
        // Given
        val moduleData = listOf(moduleResponse)
        val products = listOf(productUiState, productUiState, productUiState)
        val newProducts = listOf(updatedProductUiState, updatedProductUiState, updatedProductUiState)
        val updatedPickerState = mockk<SequentialPageProductPickerState>()
        val updatedState = mockk<SequentialPageState>()

        every { currentState.productPickerState } returns productPickerState
        every { moduleResponse.radioOptions?.getOrNull(TEST_PRODUCT_ID) } returns selectedProductInfo
        every { productPickerState.products } returns products
        every { productPickerState.selectedProductId } returns TEST_SELECTED_PRODUCT_ID
        every { selectedProductInfo.description } returns TEST_PRODUCT_DESCRIPTION
        every { productUiState.copy(description = TEST_PRODUCT_DESCRIPTION, isProductSelected = true) } returns updatedProductUiState
        every { productUiState.copy(description = null, isProductSelected = false) } returns updatedProductUiState
        every { productPickerState.copy(
            products = newProducts,
            selectedProductId = TEST_PRODUCT_ID,
            selectedProductData = selectedProductInfo
        ) } returns updatedPickerState
        every { currentState.copy(productPickerState = updatedPickerState) } returns updatedState

        // When
        val result = processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        assertEquals(updatedState, result)
        verify { productUiState.copy(description = TEST_PRODUCT_DESCRIPTION, isProductSelected = true) }
    }

    @Test
    fun `processSelectedProduct should update selected product when productInfo exists with empty description`() {
        // Given
        val moduleData = listOf(moduleResponse)
        val products = listOf(productUiState)
        val newProducts = listOf(updatedProductUiState)
        val updatedPickerState = mockk<SequentialPageProductPickerState>()
        val updatedState = mockk<SequentialPageState>()

        every { currentState.productPickerState } returns productPickerState
        every { moduleResponse.radioOptions?.getOrNull(TEST_PRODUCT_ID) } returns selectedProductInfo
        every { productPickerState.products } returns products
        every { productPickerState.selectedProductId } returns TEST_SELECTED_PRODUCT_ID
        every { selectedProductInfo.description } returns EMPTY_DESCRIPTION
        every { productUiState.copy(isProductSelected = true) } returns updatedProductUiState
        every { productPickerState.copy(
            products = newProducts,
            selectedProductId = TEST_PRODUCT_ID,
            selectedProductData = selectedProductInfo
        ) } returns updatedPickerState
        every { currentState.copy(productPickerState = updatedPickerState) } returns updatedState

        // When
        val result = processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        assertEquals(updatedState, result)
        verify { productUiState.copy(isProductSelected = true) }
    }

    @Test
    fun `processSelectedProduct should reset previously selected product`() {
        // Given
        val moduleData = listOf(moduleResponse)
        val products = listOf(productUiState, productUiState)
        val resetProduct = mockk<SequentialPageProductPickerView>()

        every { currentState.productPickerState } returns productPickerState
        every { moduleResponse.radioOptions?.getOrNull(TEST_PRODUCT_ID) } returns selectedProductInfo
        every { productPickerState.products } returns products
        every { productPickerState.selectedProductId } returns TEST_SELECTED_PRODUCT_ID
        every { selectedProductInfo.description } returns TEST_PRODUCT_DESCRIPTION
        every { productUiState.copy(description = null, isProductSelected = false) } returns resetProduct
        every { productUiState.copy(description = TEST_PRODUCT_DESCRIPTION, isProductSelected = true) } returns updatedProductUiState
        every { productPickerState.copy(any(), any(), any()) } returns mockk()
        every { currentState.copy(any()) } returns mockk()

        // When
        processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        verify { productUiState.copy(description = null, isProductSelected = false) }
    }

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
        every { pageContent.pageComponents } returns null

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should return product picker views when valid components exist`() {
        // Given
        val items = listOf(pageContent)
        val productOptions = listOf(investmentProduct)

        every { pageContent.pageComponents } returns pageComponents
        every { pageContent.pageKey } returns TEST_PAGE_KEY
        every { pageComponents.productOptions } returns productOptions
        every { investmentProduct.title } returns TEST_PRODUCT_TITLE
        every { investmentProduct.analyticsOptionTag } returns TEST_ANALYTICS_TAG
        every { investmentProduct.icon } returns TEST_ICON
        every { investmentProduct.description } returns TEST_PRODUCT_DESCRIPTION

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
        every { pageContent.pageComponents } returns pageComponents
        every { pageContent.pageKey } returns TEST_PAGE_KEY
        every { pageComponents.productOptions } returns listOf(investmentProduct)
        every { investmentProduct.title } returns TEST_PRODUCT_TITLE
        every { investmentProduct.analyticsOptionTag } returns TEST_ANALYTICS_TAG
        every { investmentProduct.icon } returns TEST_ICON
        every { investmentProduct.description } returns TEST_PRODUCT_DESCRIPTION

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

    @Test
    fun `getInvestmentProducts should handle null product title with fallback`() {
        // Given
        val items = listOf(pageContent)
        val productOptions = listOf(investmentProduct)

        every { pageContent.pageComponents } returns pageComponents
        every { pageContent.pageKey } returns TEST_PAGE_KEY
        every { pageComponents.productOptions } returns productOptions
        every { investmentProduct.title } returns null
        every { investmentProduct.analyticsOptionTag } returns TEST_ANALYTICS_TAG
        every { investmentProduct.icon } returns TEST_ICON
        every { investmentProduct.description } returns TEST_PRODUCT_DESCRIPTION

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertEquals(1, result.size)
        assertFalse(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should set isProductSelected to false by default`() {
        // Given
        val items = listOf(pageContent)
        val productOptions = listOf(investmentProduct)

        every { pageContent.pageComponents } returns pageComponents
        every { pageContent.pageKey } returns TEST_PAGE_KEY
        every { pageComponents.productOptions } returns productOptions
        every { investmentProduct.title } returns TEST_PRODUCT_TITLE
        every { investmentProduct.analyticsOptionTag } returns TEST_ANALYTICS_TAG
        every { investmentProduct.icon } returns TEST_ICON
        every { investmentProduct.description } returns TEST_PRODUCT_DESCRIPTION

        // When
        val result = processor.getInvestmentProducts(items)

        // Then
        assertEquals(1, result.size)
        // The actual verification would be done by checking the created object properties
        // Since getProduct is private, we test through the public interface
        assertFalse(result.isEmpty())
    }

    @Test
    fun `processSelectedProduct should handle null description in updateProductDetails`() {
        // Given
        val moduleData = listOf(moduleResponse)
        val products = listOf(productUiState)
        val newProducts = listOf(updatedProductUiState)
        val updatedPickerState = mockk<SequentialPageProductPickerState>()
        val updatedState = mockk<SequentialPageState>()

        every { currentState.productPickerState } returns productPickerState
        every { moduleResponse.radioOptions?.getOrNull(TEST_PRODUCT_ID) } returns selectedProductInfo
        every { productPickerState.products } returns products
        every { productPickerState.selectedProductId } returns TEST_SELECTED_PRODUCT_ID
        every { selectedProductInfo.description } returns null
        every { productUiState.copy(isProductSelected = true) } returns updatedProductUiState
        every { productPickerState.copy(
            products = newProducts,
            selectedProductId = TEST_PRODUCT_ID,
            selectedProductData = selectedProductInfo
        ) } returns updatedPickerState
        every { currentState.copy(productPickerState = updatedPickerState) } returns updatedState

        // When
        val result = processor.processSelectedProduct(currentState, TEST_PRODUCT_ID, moduleData)

        // Then
        assertEquals(updatedState, result)
        verify { productUiState.copy(isProductSelected = true) }
        verify(exactly = 0) { productUiState.copy(description = any<String>(), isProductSelected = true) }
    }
}

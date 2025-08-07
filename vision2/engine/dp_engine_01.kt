import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class SequentialPageProductProcessorTest {

    private lateinit var processor: SequentialPageProductProcessor
    
    @BeforeEach
    fun setUp() {
        processor = SequentialPageProductProcessor()
    }

    @Test
    fun `processSelectedProduct should return currentState when moduleData is empty`() {
        // Given
        val currentState = mockk<SequentialPageState>()
        val productId = 1
        val moduleData = emptyList<SequentialPageModuleResponse>()
        
        // When
        val result = processor.processSelectedProduct(currentState, productId, moduleData)
        
        // Then
        assertEquals(currentState, result)
    }

    @Test
    fun `processSelectedProduct should return currentState when radioOptions is null`() {
        // Given
        val productPickerState = mockk<SequentialPageProductPickerState>()
        val currentState = mockk<SequentialPageState> {
            every { this@mockk.productPickerState } returns productPickerState
        }
        val moduleResponse = mockk<SequentialPageModuleResponse> {
            every { radioOptions } returns null
        }
        val productId = 1
        val moduleData = listOf(moduleResponse)
        
        // When
        val result = processor.processSelectedProduct(currentState, productId, moduleData)
        
        // Then
        assertEquals(currentState, result)
    }

    @Test
    fun `processSelectedProduct should return currentState when productId is out of bounds`() {
        // Given
        val productPickerState = mockk<SequentialPageProductPickerState>()
        val currentState = mockk<SequentialPageState> {
            every { this@mockk.productPickerState } returns productPickerState
        }
        val radioOptions = listOf<SequentialPageInvestmentProduct>()
        val moduleResponse = mockk<SequentialPageModuleResponse> {
            every { this@mockk.radioOptions } returns radioOptions
        }
        val productId = 1
        val moduleData = listOf(moduleResponse)
        
        // When
        val result = processor.processSelectedProduct(currentState, productId, moduleData)
        
        // Then
        assertEquals(currentState, result)
    }

    @Test
    fun `processSelectedProduct should update product details when valid productId is provided`() {
        // Given
        val productId = 0
        val selectedProductId = 1
        
        val productInfo = mockk<SequentialPageInvestmentProduct> {
            every { description } returns "Test Description"
        }
        
        val currentProduct = mockk<SequentialPageProductPickerView> {
            every { copy(description = any(), isProductSelected = any()) } returns mockk()
        }
        
        val previouslySelectedProduct = mockk<SequentialPageProductPickerView> {
            every { copy(description = null, isProductSelected = false) } returns mockk()
        }
        
        val otherProduct = mockk<SequentialPageProductPickerView>()
        
        val products = listOf(currentProduct, previouslySelectedProduct, otherProduct)
        
        val productPickerState = mockk<SequentialPageProductPickerState> {
            every { this@mockk.products } returns products
            every { this@mockk.selectedProductId } returns selectedProductId
            every { copy(products = any(), selectedProductId = any(), selectedProductData = any()) } returns mockk()
        }
        
        val currentState = mockk<SequentialPageState> {
            every { this@mockk.productPickerState } returns productPickerState
            every { copy(productPickerState = any()) } returns mockk()
        }
        
        val radioOptions = listOf(productInfo)
        val moduleResponse = mockk<SequentialPageModuleResponse> {
            every { this@mockk.radioOptions } returns radioOptions
        }
        val moduleData = listOf(moduleResponse)
        
        // When
        val result = processor.processSelectedProduct(currentState, productId, moduleData)
        
        // Then
        verify { currentProduct.copy(description = "Test Description", isProductSelected = true) }
        verify { previouslySelectedProduct.copy(description = null, isProductSelected = false) }
        verify { productPickerState.copy(products = any(), selectedProductId = productId, selectedProductData = productInfo) }
        verify { currentState.copy(productPickerState = any()) }
        assertNotNull(result)
    }

    @Test
    fun `processSelectedProduct should handle when productId equals selectedProductId`() {
        // Given
        val productId = 0
        val selectedProductId = 0
        
        val productInfo = mockk<SequentialPageInvestmentProduct> {
            every { description } returns null
        }
        
        val currentProduct = mockk<SequentialPageProductPickerView> {
            every { description } returns "existing"
            every { copy(description = any(), isProductSelected = any()) } returns mockk()
        }
        
        val products = listOf(currentProduct)
        
        val productPickerState = mockk<SequentialPageProductPickerState> {
            every { this@mockk.products } returns products
            every { this@mockk.selectedProductId } returns selectedProductId
            every { copy(products = any(), selectedProductId = any(), selectedProductData = any()) } returns mockk()
        }
        
        val currentState = mockk<SequentialPageState> {
            every { this@mockk.productPickerState } returns productPickerState
            every { copy(productPickerState = any()) } returns mockk()
        }
        
        val radioOptions = listOf(productInfo)
        val moduleResponse = mockk<SequentialPageModuleResponse> {
            every { this@mockk.radioOptions } returns radioOptions
        }
        val moduleData = listOf(moduleResponse)
        
        // When
        val result = processor.processSelectedProduct(currentState, productId, moduleData)
        
        // Then
        verify { currentProduct.copy(description = null, isProductSelected = true) }
        assertNotNull(result)
    }

    @Test
    fun `updateProductDetails should set description to productInfo description when it is null`() {
        // Given
        val productInfo = mockk<SequentialPageInvestmentProduct> {
            every { description } returns null
        }
        val productUiState = mockk<SequentialPageProductPickerView> {
            every { description } returns "existing"
            every { copy(description = any(), isProductSelected = any()) } returns mockk()
        }
        
        // When
        processor.processSelectedProduct(
            mockk {
                every { productPickerState } returns mockk {
                    every { products } returns listOf(productUiState)
                    every { selectedProductId } returns 1
                    every { copy(products = any(), selectedProductId = any(), selectedProductData = any()) } returns mockk()
                }
                every { copy(productPickerState = any()) } returns mockk()
            },
            0,
            listOf(mockk { every { radioOptions } returns listOf(productInfo) })
        )
        
        // Then
        verify { productUiState.copy(description = null, isProductSelected = true) }
    }

    @Test
    fun `updateProductDetails should set description to productInfo description when it is empty`() {
        // Given
        val productInfo = mockk<SequentialPageInvestmentProduct> {
            every { description } returns ""
        }
        val productUiState = mockk<SequentialPageProductPickerView> {
            every { description } returns "existing"
            every { copy(description = any(), isProductSelected = any()) } returns mockk()
        }
        
        // When
        processor.processSelectedProduct(
            mockk {
                every { productPickerState } returns mockk {
                    every { products } returns listOf(productUiState)
                    every { selectedProductId } returns 1
                    every { copy(products = any(), selectedProductId = any(), selectedProductData = any()) } returns mockk()
                }
                every { copy(productPickerState = any()) } returns mockk()
            },
            0,
            listOf(mockk { every { radioOptions } returns listOf(productInfo) })
        )
        
        // Then
        verify { productUiState.copy(description = "", isProductSelected = true) }
    }

    @Test
    fun `updateProductDetails should keep existing description when productInfo description has value`() {
        // Given
        val productInfo = mockk<SequentialPageInvestmentProduct> {
            every { description } returns "new description"
        }
        val productUiState = mockk<SequentialPageProductPickerView> {
            every { description } returns "existing description"
            every { copy(description = any(), isProductSelected = any()) } returns mockk()
        }
        
        // When
        processor.processSelectedProduct(
            mockk {
                every { productPickerState } returns mockk {
                    every { products } returns listOf(productUiState)
                    every { selectedProductId } returns 1
                    every { copy(products = any(), selectedProductId = any(), selectedProductData = any()) } returns mockk()
                }
                every { copy(productPickerState = any()) } returns mockk()
            },
            0,
            listOf(mockk { every { radioOptions } returns listOf(productInfo) })
        )
        
        // Then
        verify { productUiState.copy(description = "existing description", isProductSelected = true) }
    }

    @Test
    fun `getInvestmentProducts should return empty list when items is empty`() {
        // Given
        val items = emptyList<SequentialPageContent>()
        
        // When
        val result = processor.getInvestmentProducts(items)
        
        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should return empty list when all items have null radioOptions`() {
        // Given
        val item = mockk<SequentialPageContent> {
            every { radioOptions } returns null
        }
        val items = listOf(item)
        
        // When
        val result = processor.getInvestmentProducts(items)
        
        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should return empty list when all items have empty radioOptions`() {
        // Given
        val item = mockk<SequentialPageContent> {
            every { radioOptions } returns emptyList()
        }
        val items = listOf(item)
        
        // When
        val result = processor.getInvestmentProducts(items)
        
        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getInvestmentProducts should create SequentialPageProductPickerView for each radioOption`() {
        // Given
        val product1 = mockk<SequentialPageInvestmentProduct> {
            every { title } returns "Product 1"
            every { analyticsOptionTag } returns "tag1"
            every { icon } returns "icon1"
            every { description } returns "desc1"
        }
        
        val product2 = mockk<SequentialPageInvestmentProduct> {
            every { title } returns null
            every { analyticsOptionTag } returns "tag2"
            every { icon } returns "icon2"
            every { description } returns "desc2"
        }
        
        val item = mockk<SequentialPageContent> {
            every { radioOptions } returns listOf(product1, product2)
            every { key } returns "item_key"
        }
        val items = listOf(item)
        
        mockkConstructor(SequentialPageProductPickerView::class)
        every { anyConstructed<SequentialPageProductPickerView>() } returns mockk()
        
        // When
        val result = processor.getInvestmentProducts(items)
        
        // Then
        verify {
            constructedWith<SequentialPageProductPickerView>(
                EqMatcher("Product 1"),
                EqMatcher("tag1"),
                EqMatcher("icon1"),
                EqMatcher(0),
                EqMatcher("Product 1 desc1"),
                EqMatcher(false),
                EqMatcher("item_key")
            )
        }
        
        verify {
            constructedWith<SequentialPageProductPickerView>(
                EqMatcher(EMPTY_TEXT),
                EqMatcher("tag2"),
                EqMatcher("icon2"),
                EqMatcher(1),
                EqMatcher("null desc2"),
                EqMatcher(false),
                EqMatcher("item_key")
            )
        }
        
        assertEquals(2, result.size)
    }

    @Test
    fun `getInvestmentProducts should handle multiple items with mixed radioOptions`() {
        // Given
        val product1 = mockk<SequentialPageInvestmentProduct> {
            every { title } returns "Product 1"
            every { analyticsOptionTag } returns "tag1"
            every { icon } returns "icon1"
            every { description } returns "desc1"
        }
        
        val item1 = mockk<SequentialPageContent> {
            every { radioOptions } returns listOf(product1)
            every { key } returns "item1_key"
        }
        
        val item2 = mockk<SequentialPageContent> {
            every { radioOptions } returns null
        }
        
        val product3 = mockk<SequentialPageInvestmentProduct> {
            every { title } returns "Product 3"
            every { analyticsOptionTag } returns "tag3"
            every { icon } returns "icon3"
            every { description } returns "desc3"
        }
        
        val item3 = mockk<SequentialPageContent> {
            every { radioOptions } returns listOf(product3)
            every { key } returns "item3_key"
        }
        
        val items = listOf(item1, item2, item3)
        
        mockkConstructor(SequentialPageProductPickerView::class)
        every { anyConstructed<SequentialPageProductPickerView>() } returns mockk()
        
        // When
        val result = processor.getInvestmentProducts(items)
        
        // Then
        verify {
            constructedWith<SequentialPageProductPickerView>(
                EqMatcher("Product 1"),
                EqMatcher("tag1"),
                EqMatcher("icon1"),
                EqMatcher(0),
                EqMatcher("Product 1 desc1"),
                EqMatcher(false),
                EqMatcher("item1_key")
            )
        }
        
        verify {
            constructedWith<SequentialPageProductPickerView>(
                EqMatcher("Product 3"),
                EqMatcher("tag3"),
                EqMatcher("icon3"),
                EqMatcher(0),
                EqMatcher("Product 3 desc3"),
                EqMatcher(false),
                EqMatcher("item3_key")
            )
        }
        
        assertEquals(2, result.size)
    }

    @Test
    fun `createProductPickerView should handle null payloadKey`() {
        // Given
        val product = mockk<SequentialPageInvestmentProduct> {
            every { title } returns "Test Product"
            every { analyticsOptionTag } returns "test_tag"
            every { icon } returns "test_icon"
            every { description } returns "test_desc"
        }
        
        val item = mockk<SequentialPageContent> {
            every { radioOptions } returns listOf(product)
            every { key } returns null
        }
        
        mockkConstructor(SequentialPageProductPickerView::class)
        every { anyConstructed<SequentialPageProductPickerView>() } returns mockk()
        
        // When
        val result = processor.getInvestmentProducts(listOf(item))
        
        // Then
        verify {
            constructedWith<SequentialPageProductPickerView>(
                EqMatcher("Test Product"),
                EqMatcher("test_tag"),
                EqMatcher("test_icon"),
                EqMatcher(0),
                EqMatcher("Test Product test_desc"),
                EqMatcher(false),
                EqMatcher(null)
            )
        }
        
        assertEquals(1, result.size)
    }

    companion object {
        private const val EMPTY_TEXT = ""
    }
}

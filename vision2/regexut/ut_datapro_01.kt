package com.example.sequential

import androidx.compose.ui.text.input.KeyboardType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SequentialPageDataProcessorTest {

    @MockK
    private lateinit var mockRegexUtil: SequentialPageRegexInteraction

    private lateinit var processor: SequentialPageDataProcessor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        processor = SequentialPageDataProcessor(mockRegexUtil)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `processModuleData should return PageModuleData with correct field count for input fields`() {
        // Given
        val mockResponse = mockk<SequentialPageModuleResponse> {
            every { id } returns "test-id"
            every { type } returns "INPUT_FIELD"
            every { title } returns "Test Title"
            every { contentType } returns "text"
            every { text } returns "Link Text"
            every { action } returns "Submit"
            every { invalidInputText } returns "Invalid input"
            every { heading } returns "Field Label"
            every { value } returns "Initial Value"
            every { regex } returns "[0-9]{4}"
            every { contextualTitleFallback } returns "Fallback Title"
            every { hint } returns "Enter 4 digits"
            every { fieldLabel } returns "Field Label"
            every { minDate } returns null
            every { maxDate } returns null
            every { minDateErrorMessage } returns null
            every { maxDateErrorMessage } returns null
            every { noRadioOptionSelectedText } returns null
            every { investmentProducts } returns null
            every { helpInformation } returns null
            every { investmentAccounts } returns null
        }

        val dataList = listOf(mockResponse, mockResponse)

        every { mockRegexUtil.getMaxLength(any()) } returns 4
        every { mockRegexUtil.getKeyboardType(any()) } returns KeyboardType.Number

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertEquals(2, result.pageInfo.size)
        assertEquals(2, result.fieldCount) // Initial count (0) + 2 input fields
        assertTrue(result.buttonState)
        verify(exactly = 2) { mockRegexUtil.getMaxLength("[0-9]{4}") }
        verify(exactly = 2) { mockRegexUtil.getKeyboardType("[0-9]{4}") }
    }

    @Test
    fun `processModuleData should disable button when radio input is present`() {
        // Given
        val radioResponse = mockk<SequentialPageModuleResponse> {
            every { id } returns "radio-id"
            every { type } returns "RADIO_INPUT"
            every { title } returns "Radio Title"
            every { contentType } returns "radio"
            every { text } returns null
            every { action } returns null
            every { invalidInputText } returns null
            every { heading } returns null
            every { value } returns null
            every { regex } returns null
            every { contextualTitleFallback } returns null
            every { hint } returns null
            every { fieldLabel } returns null
            every { minDate } returns null
            every { maxDate } returns null
            every { minDateErrorMessage } returns null
            every { maxDateErrorMessage } returns null
            every { noRadioOptionSelectedText } returns "Please select an option"
            every { investmentProducts } returns listOf(
                mockk {
                    every { title } returns "Product 1"
                    every { value } returns "100"
                    every { id } returns "prod-1"
                    every { description } returns "Description 1"
                }
            )
            every { helpInformation } returns null
            every { investmentAccounts } returns null
        }

        val dataList = listOf(radioResponse)

        every { mockRegexUtil.getMaxLength(any()) } returns null
        every { mockRegexUtil.getKeyboardType(any()) } returns KeyboardType.Text

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertEquals(1, result.pageInfo.size)
        assertEquals(0, result.fieldCount)
        assertFalse(result.buttonState) // Button should be disabled for radio input
    }

    @Test
    fun `processModuleData should handle date type correctly`() {
        // Given
        val dateResponse = mockk<SequentialPageModuleResponse> {
            every { id } returns "date-id"
            every { type } returns "DATE"
            every { title } returns "Date Title"
            every { contentType } returns "date"
            every { text } returns null
            every { action } returns null
            every { invalidInputText } returns "Invalid date"
            every { heading } returns "Date Label"
            every { value } returns "01/01/2024"
            every { regex } returns null
            every { contextualTitleFallback } returns null
            every { hint } returns "DD/MM/YYYY"
            every { fieldLabel } returns "Birth Date"
            every { minDate } returns "01/01/1900"
            every { maxDate } returns "31/12/2024"
            every { minDateErrorMessage } returns "Date too early"
            every { maxDateErrorMessage } returns "Date too late"
            every { noRadioOptionSelectedText } returns null
            every { investmentProducts } returns null
            every { helpInformation } returns null
            every { investmentAccounts } returns null
        }

        val dataList = listOf(dateResponse)

        every { mockRegexUtil.getMaxLength(any()) } returns null
        every { mockRegexUtil.getKeyboardType(any()) } returns KeyboardType.Number

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertEquals(1, result.pageInfo.size)
        assertEquals(1, result.fieldCount) // Date fields increment field count
        assertTrue(result.buttonState)
        
        val pageContent = result.pageInfo.first()
        assertEquals("01/01/1900", pageContent.fieldContent.minDate)
        assertEquals("31/12/2024", pageContent.fieldContent.maxDate)
        assertEquals("Date too early", pageContent.fieldContent.minDateErrorMessage)
        assertEquals("Date too late", pageContent.fieldContent.maxDateErrorMessage)
    }

    @Test
    fun `getInvestmentProducts should return products from radio input pages`() {
        // Given
        val mockProduct1 = mockk<SequentialPageInvestmentProduct> {
            every { title } returns "Product 1"
            every { value } returns "100"
        }
        val mockProduct2 = mockk<SequentialPageInvestmentProduct> {
            every { title } returns "Product 2"
            every { value } returns "200"
        }

        val pageInfoList = listOf(
            mockk<SequentialPageContent> {
                every { pageComponentType } returns SequentialPageCaptureComponentType.RADIO_INPUT
                every { pageComponents } returns mockk {
                    every { productOptions } returns listOf(mockProduct1, mockProduct2)
                }
            },
            mockk<SequentialPageContent> {
                every { pageComponentType } returns SequentialPageCaptureComponentType.INPUT_FIELD
                every { pageComponents } returns mockk {
                    every { productOptions } returns null
                }
            }
        )

        // When
        val result = processor.getInvestmentProducts(pageInfoList)

        // Then
        assertEquals(2, result.size)
        assertEquals("Product 1", result[0].title)
        assertEquals("Product 2", result[1].title)
    }

    @Test
    fun `getAccordionData should return help information from accordion pages`() {
        // Given
        val mockHelp1 = mockk<SequentialPageHelpInformation> {
            every { title } returns "Help 1"
            every { description } returns "Description 1"
        }
        val mockHelp2 = mockk<SequentialPageHelpInformation> {
            every { title } returns "Help 2"
            every { description } returns "Description 2"
        }

        val pageInfoList = listOf(
            mockk<SequentialPageContent> {
                every { pageComponentType } returns SequentialPageCaptureComponentType.ACCORDION
                every { pageComponents } returns mockk {
                    every { accordionItems } returns listOf(mockHelp1, mockHelp2)
                }
            },
            mockk<SequentialPageContent> {
                every { pageComponentType } returns SequentialPageCaptureComponentType.LABEL
                every { pageComponents } returns mockk {
                    every { accordionItems } returns null
                }
            }
        )

        // When
        val result = processor.getAccordionData(pageInfoList)

        // Then
        assertEquals(2, result.size)
        assertEquals("Help 1", result[0].title)
        assertEquals("Description 2", result[1].description)
    }

    @Test
    fun `capValue should limit input based on maxLength from regex`() {
        // Given
        val userInput = "123456789"
        val regex = "[0-9]{4}"
        every { mockRegexUtil.getMaxLength(regex) } returns 4

        // When
        val result = processor.capValue(userInput, regex)

        // Then
        assertEquals("1234", result)
        verify { mockRegexUtil.getMaxLength(regex) }
    }

    @Test
    fun `capValue should return original value when maxLength is null`() {
        // Given
        val userInput = "123456789"
        val regex = ".*"
        every { mockRegexUtil.getMaxLength(regex) } returns null

        // When
        val result = processor.capValue(userInput, regex)

        // Then
        assertEquals("123456789", result)
    }

    @Test
    fun `transformCase should apply UPPER_CASE transformation`() {
        // Given
        val input = "hello world"
        val transformation = OutputTransformation.UPPER_CASE

        // When
        val result = processor.transformCase(input, transformation)

        // Then
        assertEquals("HELLO WORLD", result)
    }

    @Test
    fun `transformCase should apply LOWER_CASE transformation`() {
        // Given
        val input = "HELLO WORLD"
        val transformation = OutputTransformation.LOWER_CASE

        // When
        val result = processor.transformCase(input, transformation)

        // Then
        assertEquals("hello world", result)
    }

    @Test
    fun `transformCase should apply CAPITALIZE transformation`() {
        // Given
        val input = "hello world"
        val transformation = OutputTransformation.CAPITALIZE

        // When
        val result = processor.transformCase(input, transformation)

        // Then
        assertEquals("Hello World", result)
    }

    @Test
    fun `transformCase should return original value for NO_TRANSFORMATION`() {
        // Given
        val input = "HeLLo WoRLd"
        val transformation = OutputTransformation.NO_TRANSFORMATION

        // When
        val result = processor.transformCase(input, transformation)

        // Then
        assertEquals("HeLLo WoRLd", result)
    }

    @Test
    fun `processModuleData should handle empty data list`() {
        // Given
        val dataList = emptyList<SequentialPageModuleResponse>()

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertEquals(0, result.pageInfo.size)
        assertEquals(0, result.fieldCount)
        assertTrue(result.buttonState)
    }

    @Test
    fun `processModuleData should handle mixed component types`() {
        // Given
        val inputResponse = mockk<SequentialPageModuleResponse>(relaxed = true) {
            every { type } returns "INPUT_FIELD"
        }
        val labelResponse = mockk<SequentialPageModuleResponse>(relaxed = true) {
            every { type } returns "LABEL"
        }
        val accordionResponse = mockk<SequentialPageModuleResponse>(relaxed = true) {
            every { type } returns "ACCORDION"
            every { helpInformation } returns listOf(mockk(relaxed = true))
        }

        val dataList = listOf(inputResponse, labelResponse, accordionResponse)

        every { mockRegexUtil.getMaxLength(any()) } returns 10
        every { mockRegexUtil.getKeyboardType(any()) } returns KeyboardType.Text

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertEquals(3, result.pageInfo.size)
        assertEquals(1, result.fieldCount) // Only INPUT_FIELD increments count
        assertTrue(result.buttonState)
    }

    @Test
    fun `getInvestmentProducts should handle empty page list`() {
        // Given
        val pageInfoList = emptyList<SequentialPageContent>()

        // When
        val result = processor.getInvestmentProducts(pageInfoList)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `getAccordionData should handle empty page list`() {
        // Given
        val pageInfoList = emptyList<SequentialPageContent>()

        // When
        val result = processor.getAccordionData(pageInfoList)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `processModuleData should handle null values in response`() {
        // Given
        val nullResponse = mockk<SequentialPageModuleResponse> {
            every { id } returns null
            every { type } returns "UNKNOWN"
            every { title } returns null
            every { contentType } returns null
            every { text } returns null
            every { action } returns null
            every { invalidInputText } returns null
            every { heading } returns null
            every { value } returns null
            every { regex } returns null
            every { contextualTitleFallback } returns null
            every { hint } returns null
            every { fieldLabel } returns null
            every { minDate } returns null
            every { maxDate } returns null
            every { minDateErrorMessage } returns null
            every { maxDateErrorMessage } returns null
            every { noRadioOptionSelectedText } returns null
            every { investmentProducts } returns null
            every { helpInformation } returns null
            every { investmentAccounts } returns null
        }

        val dataList = listOf(nullResponse)

        every { mockRegexUtil.getMaxLength(null) } returns null
        every { mockRegexUtil.getKeyboardType(null) } returns KeyboardType.Text

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertEquals(1, result.pageInfo.size)
        assertEquals(0, result.fieldCount)
        assertTrue(result.buttonState)
    }
}
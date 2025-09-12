package com.barclays.mobilebanking.feature.sequentialpagecapture.data.processor

import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SequentialPageDataProcessorTest {

    private val mockRegexUtil = mockk<SequentialPageRegexUtility>()
    private lateinit var processor: SequentialPageDataProcessor

    @BeforeEach
    fun setup() {
        clearAllMocks()
        processor = SequentialPageDataProcessor(mockRegexUtil)
    }

    @Test
    fun `processModuleData should return correct metadata with input field count`() {
        // Given
        val regex = "\\d{4}"
        val response1 = createMockModuleResponse(
            id = "page1",
            componentType = "INPUT_FIELD",
            regex = regex
        )
        val response2 = createMockModuleResponse(
            id = "page2",
            componentType = "DATE"
        )
        val dataList = listOf(response1, response2)

        every { mockRegexUtil.getMaxLength(regex) } returns 10
        every { mockRegexUtil.getMaxLength("") } returns 5

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertEquals(2, result.pageInfo.size)
        assertEquals(3, result.fieldCount) // INITIAL_FIELD_COUNT(1) + 2 input fields
        assertTrue(result.buttonState)

        verify { mockRegexUtil.getMaxLength(regex) }
        verify { mockRegexUtil.getMaxLength("") }
    }

    @Test
    fun `processModuleData should disable button when radio options present`() {
        // Given
        val response = createMockModuleResponse(
            id = "page1",
            componentType = "RADIO_INPUT"
        )
        val dataList = listOf(response)

        every { mockRegexUtil.getMaxLength("") } returns 5

        // When
        val result = processor.processModuleData(dataList)

        // Then
        assertFalse(result.buttonState)
        verify { mockRegexUtil.getMaxLength("") }
    }

    @Test
    fun `getCappedValue should handle investment account transformation`() {
        // Given
        val regex = "\\d{4}-\\d{4}"
        val inputData = "12345678"
        val cappedData = "1234-5678"
        
        val investmentAccount = createMockInvestmentAccount(
            regex = regex,
            maxLength = 9,
            hasTransformation = true
        )
        
        val pageContent = createMockPageContent(
            key = "page1",
            maxInputLength = 10
        )
        val items = listOf(pageContent)

        every { mockRegexUtil.formatInput(regex, "123456789") } returns cappedData

        // When
        val result = processor.getCappedValue(
            items = items,
            investmentAccount = investmentAccount,
            itemKey = "page1",
            inputData = inputData,
            contentType = null
        )

        // Then
        assertEquals(pageContent, result.pageItem)
        assertEquals(cappedData, result.inputValue)
        verify { mockRegexUtil.formatInput(regex, "123456789") }
    }

    @Test
    fun `getCappedValue should handle sort code transformation`() {
        // Given
        val sortCodeRegex = "\\d{2}-\\d{2}-\\d{2}"
        val inputData = "123456"
        val expectedRegex = "\\d{2}\\d{2}\\d{2}"
        
        val pageContent = createMockPageContent(
            key = "page1",
            fieldRegex = sortCodeRegex,
            maxInputLength = 8
        )
        val items = listOf(pageContent)
        val contentType = mockk<SortCodeOutputTransformation>()

        every { mockRegexUtil.getMaxLength(expectedRegex) } returns 6

        // When
        val result = processor.getCappedValue(
            items = items,
            investmentAccount = null,
            itemKey = "page1",
            inputData = inputData,
            contentType = contentType
        )

        // Then
        assertEquals("123456", result.inputValue)
        verify { mockRegexUtil.getMaxLength(expectedRegex) }
    }

    @Test
    fun `getCappedValue should handle account number content type`() {
        // Given
        val inputData = "1234567890123"
        val pageContent = createMockPageContent(
            key = "page1",
            maxInputLength = 10,
            contentType = "CONTENT_ACCOUNT_NUMBER_TYPE"
        )
        val items = listOf(pageContent)

        // When
        val result = processor.getCappedValue(
            items = items,
            investmentAccount = null,
            itemKey = "page1",
            inputData = inputData,
            contentType = null
        )

        // Then
        assertEquals("1234567890", result.inputValue)
    }

    @Test
    fun `getAccordionData should return mapped accordion information`() {
        // Given
        val helpInfo1 = createMockHelpInformation("help1")
        val helpInfo2 = createMockHelpInformation("help2")
        
        val pageContent1 = createMockPageContentWithAccordion(
            key = "page1",
            accordionItems = listOf(helpInfo1)
        )
        val pageContent2 = createMockPageContentWithAccordion(
            key = "page2",
            accordionItems = listOf(helpInfo2)
        )
        val items = listOf(pageContent1, pageContent2)

        // When
        val result = processor.getAccordionData(items)

        // Then
        assertEquals(2, result.size)
        assertEquals(listOf(helpInfo1), result["page1"])
        assertEquals(listOf(helpInfo2), result["page2"])
    }

    @Test
    fun `getAccordionData should filter out items without accordion data`() {
        // Given
        val helpInfo = createMockHelpInformation("help1")
        val pageContentWithAccordion = createMockPageContentWithAccordion(
            key = "page1",
            accordionItems = listOf(helpInfo)
        )
        val pageContentWithoutAccordion = createMockPageContent(key = "page2")
        val items = listOf(pageContentWithAccordion, pageContentWithoutAccordion)

        // When
        val result = processor.getAccordionData(items)

        // Then
        assertEquals(1, result.size)
        assertEquals(listOf(helpInfo), result["page1"])
        assertNull(result["page2"])
    }

    @Test
    fun `getCappedValue should return original input when no transformation needed`() {
        // Given
        val inputData = "test123"
        val pageContent = createMockPageContent(
            key = "page1",
            maxInputLength = 10
        )
        val items = listOf(pageContent)

        // When
        val result = processor.getCappedValue(
            items = items,
            investmentAccount = null,
            itemKey = "page1",
            inputData = inputData,
            contentType = null
        )

        // Then
        assertEquals("test123", result.inputValue)
    }

    @Test
    fun `getCappedValue should handle investment account without transformation`() {
        // Given
        val inputData = "12345678"
        val investmentAccount = createMockInvestmentAccount(
            maxLength = 6,
            hasTransformation = false
        )
        val pageContent = createMockPageContent(key = "page1")
        val items = listOf(pageContent)

        // When
        val result = processor.getCappedValue(
            items = items,
            investmentAccount = investmentAccount,
            itemKey = "page1",
            inputData = inputData,
            contentType = null
        )

        // Then
        assertEquals("123456", result.inputValue)
    }

    // Helper methods for creating mock objects
    private fun createMockModuleResponse(
        id: String,
        componentType: String,
        regex: String = "",
        title: String = "Test Title",
        heading: String = "Test Heading"
    ): SequentialPageModuleResponse {
        return mockk<SequentialPageModuleResponse>().also {
            every { it.sequentialPageId } returns id
            every { it.sequentialPageComponentType } returns componentType
            every { it.regex } returns regex
            every { it.title } returns title
            every { it.heading } returns heading
            every { it.sequentialPageInputValue } returns ""
            every { it.contentType } returns ""
            every { it.sequentialPageText } returns ""
            every { it.action } returns ""
            every { it.invalidInputText } returns ""
            every { it.noRadioOptionSelectedText } returns ""
            every { it.radioOptions } returns null
            every { it.helpInformation } returns null
            every { it.radioInputs } returns null
            every { it.helpText } returns ""
            every { it.minDate } returns ""
            every { it.maxDate } returns ""
            every { it.minDateInvalidText } returns ""
            every { it.maxDateInvalidText } returns ""
            every { it.keyboard } returns ""
        }
    }

    private fun createMockInvestmentAccount(
        regex: String = "",
        maxLength: Int = 10,
        hasTransformation: Boolean = false
    ): SequentialPageInvestmentAccount {
        return mockk<SequentialPageInvestmentAccount>().also {
            every { it.regex } returns regex
            every { it.maxLength } returns maxLength
            every { it.hasTransformation } returns hasTransformation
        }
    }

    private fun createMockPageContent(
        key: String,
        fieldRegex: String = "",
        maxInputLength: Int = 10,
        contentType: String = ""
    ): SequentialPageContent {
        val mockInputFieldState = mockk<SequentialPageInputFieldState>().also {
            every { it.fieldRegex } returns fieldRegex
            every { it.maxInputLength } returns maxInputLength
        }

        return mockk<SequentialPageContent>().also {
            every { it.pageKey } returns key
            every { it.inputFieldState } returns mockInputFieldState
            every { it.pageContentType } returns contentType
            every { it.pageComponents } returns null
        }
    }

    private fun createMockPageContentWithAccordion(
        key: String,
        accordionItems: List<SequentialPageHelpInformation>
    ): SequentialPageContent {
        val mockComponents = mockk<SequentialPageComponents>().also {
            every { it.accordionItems } returns accordionItems
        }

        return mockk<SequentialPageContent>().also {
            every { it.pageKey } returns key
            every { it.pageComponents } returns mockComponents
            every { it.inputFieldState } returns null
        }
    }

    private fun createMockHelpInformation(id: String): SequentialPageHelpInformation {
        return mockk<SequentialPageHelpInformation>().also {
            every { it.toString() } returns id
        }
    }
}

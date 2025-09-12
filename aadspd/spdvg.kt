package com.barclays.mobilebanking.feature.sequentialpagecapture.validator

import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SequentialPageValidationDelegatorTest {

    private val mockValidator = mockk<SequentialPageViewValidation>()
    private lateinit var delegator: SequentialPageValidationDelegator

    @BeforeEach
    fun setup() {
        clearAllMocks()
        delegator = SequentialPageValidationDelegator(mockValidator)
    }

    @Test
    fun `validateField should update item with new value and delegate to validator`() {
        // Given
        val value = "test123"
        val pageKey = "page1"
        val originalFieldState = createMockInputFieldState("original")
        val item = createMockPageContent(pageKey, originalFieldState)
        val investmentAccount = createMockInvestmentAccount()
        val moduleResponse = createMockModuleResponse(pageKey)
        
        val updatedFieldState = createMockInputFieldState(value)
        val updatedItem = createMockPageContent(pageKey, updatedFieldState)
        val expectedResult = createMockValidationResult(true, "Success")

        every { item.copy(inputFieldState = updatedFieldState) } returns updatedItem
        every { mockValidator.validate(updatedItem, investmentAccount, moduleResponse) } returns expectedResult

        // When
        val result = delegator.validateField(value, item, investmentAccount, moduleResponse)

        // Then
        assertEquals(expectedResult, result)
        verify { item.copy(inputFieldState = updatedFieldState) }
        verify { mockValidator.validate(updatedItem, investmentAccount, moduleResponse) }
    }

    @Test
    fun `validateField should handle null value`() {
        // Given
        val pageKey = "page1"
        val originalFieldState = createMockInputFieldState("original")
        val item = createMockPageContent(pageKey, originalFieldState)
        val investmentAccount = createMockInvestmentAccount()
        val moduleResponse = createMockModuleResponse(pageKey)
        
        val updatedFieldState = createMockInputFieldState(null)
        val updatedItem = createMockPageContent(pageKey, updatedFieldState)
        val expectedResult = createMockValidationResult(false, "Invalid")

        every { item.copy(inputFieldState = updatedFieldState) } returns updatedItem
        every { mockValidator.validate(updatedItem, investmentAccount, moduleResponse) } returns expectedResult

        // When
        val result = delegator.validateField(null, item, investmentAccount, moduleResponse)

        // Then
        assertEquals(expectedResult, result)
        verify { mockValidator.validate(updatedItem, investmentAccount, moduleResponse) }
    }

    @Test
    fun `validateAllFields should return all valid when all fields pass validation`() {
        // Given
        val item1 = createMockPageContentForValidation("page1", "value1")
        val item2 = createMockPageContentForValidation("page2", "value2")
        val items = listOf(item1, item2)
        
        val moduleResponse1 = createMockModuleResponse("page1")
        val moduleResponse2 = createMockModuleResponse("page2")
        val moduleResponses = listOf(moduleResponse1, moduleResponse2)
        val investmentAccount = createMockInvestmentAccount()

        val validResult1 = createMockValidationResult(true, "Success")
        val validResult2 = createMockValidationResult(true, "Success")
        
        val updatedItem1 = createMockUpdatedPageContent("page1", false, "Success")
        val updatedItem2 = createMockUpdatedPageContent("page2", false, "Success")

        every { mockValidator.validate(any(), investmentAccount, moduleResponse1) } returns validResult1
        every { mockValidator.validate(any(), investmentAccount, moduleResponse2) } returns validResult2
        every { item1.copy(pageErrorState = any()) } returns updatedItem1
        every { item2.copy(pageErrorState = any()) } returns updatedItem2

        // When
        val result = delegator.validateAllFields(items, investmentAccount, moduleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(2, result.updatedItems.size)
        assertEquals(updatedItem1, result.updatedItems[0])
        assertEquals(updatedItem2, result.updatedItems[1])
        
        verify { mockValidator.validate(any(), investmentAccount, moduleResponse1) }
        verify { mockValidator.validate(any(), investmentAccount, moduleResponse2) }
    }

    @Test
    fun `validateAllFields should return invalid when some fields fail validation`() {
        // Given
        val item1 = createMockPageContentForValidation("page1", "value1")
        val item2 = createMockPageContentForValidation("page2", "invalid")
        val items = listOf(item1, item2)
        
        val moduleResponse1 = createMockModuleResponse("page1")
        val moduleResponse2 = createMockModuleResponse("page2")
        val moduleResponses = listOf(moduleResponse1, moduleResponse2)
        val investmentAccount = createMockInvestmentAccount()

        val validResult = createMockValidationResult(true, "Success")
        val invalidResult = createMockValidationResult(false, "Invalid input")
        
        val updatedItem1 = createMockUpdatedPageContent("page1", false, "Success")
        val updatedItem2 = createMockUpdatedPageContent("page2", true, "Invalid input")

        every { mockValidator.validate(any(), investmentAccount, moduleResponse1) } returns validResult
        every { mockValidator.validate(any(), investmentAccount, moduleResponse2) } returns invalidResult
        every { item1.copy(pageErrorState = any()) } returns updatedItem1
        every { item2.copy(pageErrorState = any()) } returns updatedItem2

        // When
        val result = delegator.validateAllFields(items, investmentAccount, moduleResponses)

        // Then
        assertFalse(result.isAllFieldValid)
        assertEquals(2, result.updatedItems.size)
        verify { mockValidator.validate(any(), investmentAccount, moduleResponse1) }
        verify { mockValidator.validate(any(), investmentAccount, moduleResponse2) }
    }

    @Test
    fun `validateAllFields should handle empty module responses`() {
        // Given
        val item1 = createMockPageContentForValidation("page1", "value1")
        val items = listOf(item1)
        val investmentAccount = createMockInvestmentAccount()
        
        val validResult = createMockValidationResult(true, "Success")
        val updatedItem1 = createMockUpdatedPageContent("page1", false, "Success")

        every { mockValidator.validate(any(), investmentAccount, null) } returns validResult
        every { item1.copy(pageErrorState = any()) } returns updatedItem1

        // When
        val result = delegator.validateAllFields(items, investmentAccount, emptyList())

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(1, result.updatedItems.size)
        verify { mockValidator.validate(any(), investmentAccount, null) }
    }

    @Test
    fun `validateAllFields should handle null module responses`() {
        // Given
        val item1 = createMockPageContentForValidation("page1", "value1")
        val items = listOf(item1)
        val investmentAccount = createMockInvestmentAccount()
        
        val validResult = createMockValidationResult(true, "Success")
        val updatedItem1 = createMockUpdatedPageContent("page1", false, "Success")

        every { mockValidator.validate(any(), investmentAccount, null) } returns validResult
        every { item1.copy(pageErrorState = any()) } returns updatedItem1

        // When
        val result = delegator.validateAllFields(items, investmentAccount, null)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(1, result.updatedItems.size)
        verify { mockValidator.validate(any(), investmentAccount, null) }
    }

    @Test
    fun `validateAllFields should match module responses by pageKey`() {
        // Given
        val item1 = createMockPageContentForValidation("page1", "value1")
        val item2 = createMockPageContentForValidation("page2", "value2")
        val items = listOf(item1, item2)
        
        val moduleResponse1 = createMockModuleResponse("page1")
        val moduleResponse3 = createMockModuleResponse("page3") // Different key
        val moduleResponses = listOf(moduleResponse1, moduleResponse3)
        val investmentAccount = createMockInvestmentAccount()

        val validResult = createMockValidationResult(true, "Success")
        val updatedItem1 = createMockUpdatedPageContent("page1", false, "Success")
        val updatedItem2 = createMockUpdatedPageContent("page2", false, "Success")

        every { mockValidator.validate(any(), investmentAccount, moduleResponse1) } returns validResult
        every { mockValidator.validate(any(), investmentAccount, null) } returns validResult
        every { item1.copy(pageErrorState = any()) } returns updatedItem1
        every { item2.copy(pageErrorState = any()) } returns updatedItem2

        // When
        val result = delegator.validateAllFields(items, investmentAccount, moduleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        verify { mockValidator.validate(any(), investmentAccount, moduleResponse1) }
        verify { mockValidator.validate(any(), investmentAccount, null) }
    }

    // Helper methods for creating mock objects
    private fun createMockInputFieldState(value: String?): SequentialPageInputFieldState {
        return mockk<SequentialPageInputFieldState>().also {
            every { it.fieldValue } returns value
            every { it.copy(fieldValue = value) } returns it
        }
    }

    private fun createMockPageContent(
        pageKey: String,
        inputFieldState: SequentialPageInputFieldState?
    ): SequentialPageContent {
        return mockk<SequentialPageContent>().also {
            every { it.pageKey } returns pageKey
            every { it.inputFieldState } returns inputFieldState
        }
    }

    private fun createMockPageContentForValidation(
        pageKey: String,
        value: String
    ): SequentialPageContent {
        val fieldState = createMockInputFieldState(value)
        return mockk<SequentialPageContent>().also {
            every { it.pageKey } returns pageKey
            every { it.inputFieldState } returns fieldState
        }
    }

    private fun createMockUpdatedPageContent(
        pageKey: String,
        hasError: Boolean,
        errorMessage: String
    ): SequentialPageContent {
        val errorState = mockk<SequentialPageUIErrorState>().also {
            every { it.isError } returns hasError
            every { it.errorMessage } returns errorMessage
        }
        
        return mockk<SequentialPageContent>().also {
            every { it.pageKey } returns pageKey
            every { it.pageErrorState } returns errorState
        }
    }

    private fun createMockInvestmentAccount(): SequentialPageInvestmentAccount {
        return mockk<SequentialPageInvestmentAccount>()
    }

    private fun createMockModuleResponse(pageId: String): SequentialPageModuleResponse {
        return mockk<SequentialPageModuleResponse>().also {
            every { it.sequentialPageId } returns pageId
        }
    }

    private fun createMockValidationResult(isValid: Boolean, message: String): ValidationResult {
        return mockk<ValidationResult>().also {
            every { it.isValid } returns isValid
            every { it.message } returns message
        }
    }
}

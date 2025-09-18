package com.barclays.mobilebanking.feature.sequentialpagecapture.validator

import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SequentialPageValidationDelegatorTest {

    companion object {
        // String Constants
        private const val TEST_VALUE_VALID = "validValue123"
        private const val TEST_VALUE_INVALID = "invalid"
        private const val TEST_VALUE_EMPTY = ""
        private const val TEST_PAGE_KEY_1 = "page1"
        private const val TEST_PAGE_KEY_2 = "page2"
        private const val TEST_PAGE_KEY_3 = "page3"
        private const val TEST_ERROR_MESSAGE_VALID = "Success"
        private const val TEST_ERROR_MESSAGE_INVALID = "Invalid input"
        private const val TEST_ERROR_MESSAGE_REQUIRED = "Field is required"
        private const val TEST_ERROR_MESSAGE_NULL_STATE = "No input field state"

        // Numeric Constants
        private const val EXPECTED_SINGLE_ITEM_COUNT = 1
        private const val EXPECTED_DOUBLE_ITEM_COUNT = 2
        private const val EXPECTED_EMPTY_COUNT = 0
        private const val VALIDATION_CALL_COUNT_ONCE = 1
        private const val VALIDATION_CALL_COUNT_TWICE = 2
        private const val VALIDATION_CALL_COUNT_NEVER = 0
    }

    @MockK
    private lateinit var mockValidator: SequentialPageViewValidation

    @MockK
    private lateinit var mockPageContent1: SequentialPageContent

    @MockK
    private lateinit var mockPageContent2: SequentialPageContent

    @MockK
    private lateinit var mockInputFieldState1: SequentialPageInputFieldState

    @MockK
    private lateinit var mockInputFieldState2: SequentialPageInputFieldState

    @MockK
    private lateinit var mockInvestmentAccount: SequentialPageInvestmentAccount

    @MockK
    private lateinit var mockModuleResponse1: SequentialPageModuleResponse

    @MockK
    private lateinit var mockModuleResponse2: SequentialPageModuleResponse

    @MockK
    private lateinit var mockValidationResult: ValidationResult

    @MockK
    private lateinit var mockValidationResultInvalid: ValidationResult

    @MockK
    private lateinit var mockErrorState: SequentialPageUIErrorState

    @MockK
    private lateinit var mockUpdatedPageContent1: SequentialPageContent

    @MockK
    private lateinit var mockUpdatedPageContent2: SequentialPageContent

    private lateinit var delegator: SequentialPageValidationDelegator

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        delegator = SequentialPageValidationDelegator(mockValidator)
        setupCommonMockBehavior()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(
            mockValidator,
            mockPageContent1,
            mockPageContent2,
            mockInputFieldState1,
            mockInputFieldState2,
            mockInvestmentAccount,
            mockModuleResponse1,
            mockModuleResponse2,
            mockValidationResult,
            mockValidationResultInvalid,
            mockErrorState,
            mockUpdatedPageContent1,
            mockUpdatedPageContent2
        )
    }

    private fun setupCommonMockBehavior() {
        // Setup page content mocks
        every { mockPageContent1.pageKey } returns TEST_PAGE_KEY_1
        every { mockPageContent2.pageKey } returns TEST_PAGE_KEY_2
        every { mockPageContent1.inputFieldState } returns mockInputFieldState1
        every { mockPageContent2.inputFieldState } returns mockInputFieldState2

        // Setup input field state mocks
        every { mockInputFieldState1.fieldValue } returns TEST_VALUE_VALID
        every { mockInputFieldState2.fieldValue } returns TEST_VALUE_INVALID

        // Setup module response mocks
        every { mockModuleResponse1.sequentialPageId } returns TEST_PAGE_KEY_1
        every { mockModuleResponse2.sequentialPageId } returns TEST_PAGE_KEY_2

        // Setup validation result mocks
        every { mockValidationResult.isValid } returns true
        every { mockValidationResult.message } returns TEST_ERROR_MESSAGE_VALID
        every { mockValidationResultInvalid.isValid } returns false
        every { mockValidationResultInvalid.message } returns TEST_ERROR_MESSAGE_INVALID

        // Setup error state mock
        every { mockErrorState.isError } returns false
        every { mockErrorState.errorMessage } returns null

        // Setup updated page content mocks
        every { mockUpdatedPageContent1.pageKey } returns TEST_PAGE_KEY_1
        every { mockUpdatedPageContent2.pageKey } returns TEST_PAGE_KEY_2
        every { mockUpdatedPageContent1.pageErrorState } returns mockErrorState
        every { mockUpdatedPageContent2.pageErrorState } returns mockErrorState
    }

    // Tests for validateField method
    @Test
    fun testValidateFieldWithValidValueShouldUpdateItemAndReturnValidResult() {
        // Given
        val updatedInputState = mockk<SequentialPageInputFieldState>()
        val updatedPageContent = mockk<SequentialPageContent>()
        
        every { mockInputFieldState1.copy(fieldValue = TEST_VALUE_VALID) } returns updatedInputState
        every { mockPageContent1.copy(inputFieldState = updatedInputState) } returns updatedPageContent
        every { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResult

        // When
        val result = delegator.validateField(TEST_VALUE_VALID, mockPageContent1, mockInvestmentAccount, mockModuleResponse1)

        // Then
        assertEquals(mockValidationResult, result)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockInputFieldState1.copy(fieldValue = TEST_VALUE_VALID) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockPageContent1.copy(inputFieldState = updatedInputState) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) }
    }

    @Test
    fun testValidateFieldWithNullValueShouldHandleGracefully() {
        // Given
        val updatedInputState = mockk<SequentialPageInputFieldState>()
        val updatedPageContent = mockk<SequentialPageContent>()
        
        every { mockInputFieldState1.copy(fieldValue = null) } returns updatedInputState
        every { mockPageContent1.copy(inputFieldState = updatedInputState) } returns updatedPageContent
        every { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResultInvalid

        // When
        val result = delegator.validateField(null, mockPageContent1, mockInvestmentAccount, mockModuleResponse1)

        // Then
        assertEquals(mockValidationResultInvalid, result)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockInputFieldState1.copy(fieldValue = null) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockPageContent1.copy(inputFieldState = updatedInputState) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) }
    }

    @Test
    fun testValidateFieldWithEmptyValueShouldProcessCorrectly() {
        // Given
        val updatedInputState = mockk<SequentialPageInputFieldState>()
        val updatedPageContent = mockk<SequentialPageContent>()
        
        every { mockInputFieldState1.copy(fieldValue = TEST_VALUE_EMPTY) } returns updatedInputState
        every { mockPageContent1.copy(inputFieldState = updatedInputState) } returns updatedPageContent
        every { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResultInvalid

        // When
        val result = delegator.validateField(TEST_VALUE_EMPTY, mockPageContent1, mockInvestmentAccount, mockModuleResponse1)

        // Then
        assertEquals(mockValidationResultInvalid, result)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockInputFieldState1.copy(fieldValue = TEST_VALUE_EMPTY) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) }
    }

    @Test
    fun testValidateFieldWithNullInputFieldStateShouldHandleGracefully() {
        // Given
        val pageContentWithNullState = mockk<SequentialPageContent>()
        val updatedPageContent = mockk<SequentialPageContent>()
        
        every { pageContentWithNullState.inputFieldState } returns null
        every { pageContentWithNullState.copy(inputFieldState = null) } returns updatedPageContent
        every { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResultInvalid

        // When
        val result = delegator.validateField(TEST_VALUE_VALID, pageContentWithNullState, mockInvestmentAccount, mockModuleResponse1)

        // Then
        assertEquals(mockValidationResultInvalid, result)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { pageContentWithNullState.copy(inputFieldState = null) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(updatedPageContent, mockInvestmentAccount, mockModuleResponse1) }
    }

    @Test
    fun testValidateFieldWithNullInvestmentAccountShouldProcess() {
        // Given
        val updatedInputState = mockk<SequentialPageInputFieldState>()
        val updatedPageContent = mockk<SequentialPageContent>()
        
        every { mockInputFieldState1.copy(fieldValue = TEST_VALUE_VALID) } returns updatedInputState
        every { mockPageContent1.copy(inputFieldState = updatedInputState) } returns updatedPageContent
        every { mockValidator.validate(updatedPageContent, null, mockModuleResponse1) } returns mockValidationResult

        // When
        val result = delegator.validateField(TEST_VALUE_VALID, mockPageContent1, null, mockModuleResponse1)

        // Then
        assertEquals(mockValidationResult, result)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(updatedPageContent, null, mockModuleResponse1) }
    }

    @Test
    fun testValidateFieldWithNullModuleResponseShouldProcess() {
        // Given
        val updatedInputState = mockk<SequentialPageInputFieldState>()
        val updatedPageContent = mockk<SequentialPageContent>()
        
        every { mockInputFieldState1.copy(fieldValue = TEST_VALUE_VALID) } returns updatedInputState
        every { mockPageContent1.copy(inputFieldState = updatedInputState) } returns updatedPageContent
        every { mockValidator.validate(updatedPageContent, mockInvestmentAccount, null) } returns mockValidationResult

        // When
        val result = delegator.validateField(TEST_VALUE_VALID, mockPageContent1, mockInvestmentAccount, null)

        // Then
        assertEquals(mockValidationResult, result)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(updatedPageContent, mockInvestmentAccount, null) }
    }

    // Tests for validateAllFields method
    @Test
    fun testValidateAllFieldsWithEmptyListShouldReturnValidResult() {
        // Given
        val emptyList = emptyList<SequentialPageContent>()
        val emptyModuleResponses = emptyList<SequentialPageModuleResponse>()

        // When
        val result = delegator.validateAllFields(emptyList, mockInvestmentAccount, emptyModuleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(EXPECTED_EMPTY_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_NEVER) { mockValidator.validate(any(), any(), any()) }
    }

    @Test
    fun testValidateAllFieldsWithSingleValidItemShouldReturnValidResult() {
        // Given
        val items = listOf(mockPageContent1)
        val moduleResponses = listOf(mockModuleResponse1)
        val errorState = createMockErrorState(false, null)
        
        every { mockPageContent1.copy(pageErrorState = errorState) } returns mockUpdatedPageContent1
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResult

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, moduleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(EXPECTED_SINGLE_ITEM_COUNT, result.updatedItems.size)
        assertEquals(mockUpdatedPageContent1, result.updatedItems[0])
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) }
    }

    @Test
    fun testValidateAllFieldsWithSingleInvalidItemShouldReturnInvalidResult() {
        // Given
        val items = listOf(mockPageContent1)
        val moduleResponses = listOf(mockModuleResponse1)
        val errorState = createMockErrorState(true, TEST_ERROR_MESSAGE_INVALID)
        
        every { mockPageContent1.copy(pageErrorState = errorState) } returns mockUpdatedPageContent1
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResultInvalid

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, moduleResponses)

        // Then
        assertFalse(result.isAllFieldValid)
        assertEquals(EXPECTED_SINGLE_ITEM_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) }
    }

    @Test
    fun testValidateAllFieldsWithMultipleValidItemsShouldReturnValidResult() {
        // Given
        val items = listOf(mockPageContent1, mockPageContent2)
        val moduleResponses = listOf(mockModuleResponse1, mockModuleResponse2)
        val errorState1 = createMockErrorState(false, null)
        val errorState2 = createMockErrorState(false, null)
        
        every { mockPageContent1.copy(pageErrorState = errorState1) } returns mockUpdatedPageContent1
        every { mockPageContent2.copy(pageErrorState = errorState2) } returns mockUpdatedPageContent2
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResult
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse2) } returns mockValidationResult

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, moduleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(EXPECTED_DOUBLE_ITEM_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse2) }
    }

    @Test
    fun testValidateAllFieldsWithMixedValidityItemsShouldReturnInvalidResult() {
        // Given
        val items = listOf(mockPageContent1, mockPageContent2)
        val moduleResponses = listOf(mockModuleResponse1, mockModuleResponse2)
        val errorState1 = createMockErrorState(false, null)
        val errorState2 = createMockErrorState(true, TEST_ERROR_MESSAGE_INVALID)
        
        every { mockPageContent1.copy(pageErrorState = errorState1) } returns mockUpdatedPageContent1
        every { mockPageContent2.copy(pageErrorState = errorState2) } returns mockUpdatedPageContent2
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResult
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse2) } returns mockValidationResultInvalid

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, moduleResponses)

        // Then
        assertFalse(result.isAllFieldValid)
        assertEquals(EXPECTED_DOUBLE_ITEM_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse2) }
    }

    @Test
    fun testValidateAllFieldsWithNullModuleResponsesShouldProcessWithNullModuleResponse() {
        // Given
        val items = listOf(mockPageContent1)
        val errorState = createMockErrorState(false, null)
        
        every { mockPageContent1.copy(pageErrorState = errorState) } returns mockUpdatedPageContent1
        every { mockValidator.validate(any(), mockInvestmentAccount, null) } returns mockValidationResult

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, null)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(EXPECTED_SINGLE_ITEM_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, null) }
    }

    @Test
    fun testValidateAllFieldsWithMismatchedModuleResponseKeysShouldUseNullForUnmatched() {
        // Given
        val items = listOf(mockPageContent1, mockPageContent2)
        val moduleResponse3 = mockk<SequentialPageModuleResponse>()
        every { moduleResponse3.sequentialPageId } returns TEST_PAGE_KEY_3
        val moduleResponses = listOf(mockModuleResponse1, moduleResponse3) // page2 has no matching response
        
        val errorState1 = createMockErrorState(false, null)
        val errorState2 = createMockErrorState(false, null)
        
        every { mockPageContent1.copy(pageErrorState = errorState1) } returns mockUpdatedPageContent1
        every { mockPageContent2.copy(pageErrorState = errorState2) } returns mockUpdatedPageContent2
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResult
        every { mockValidator.validate(any(), mockInvestmentAccount, null) } returns mockValidationResult

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, moduleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(EXPECTED_DOUBLE_ITEM_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, null) }
    }

    @Test
    fun testValidateAllFieldsWithNullInvestmentAccountShouldProcessCorrectly() {
        // Given
        val items = listOf(mockPageContent1)
        val moduleResponses = listOf(mockModuleResponse1)
        val errorState = createMockErrorState(false, null)
        
        every { mockPageContent1.copy(pageErrorState = errorState) } returns mockUpdatedPageContent1
        every { mockValidator.validate(any(), null, mockModuleResponse1) } returns mockValidationResult

        // When
        val result = delegator.validateAllFields(items, null, moduleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(EXPECTED_SINGLE_ITEM_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), null, mockModuleResponse1) }
    }

    @Test
    fun testValidateAllFieldsWithValidationResultAsNullShouldHandleGracefully() {
        // Given
        val items = listOf(mockPageContent1)
        val moduleResponses = listOf(mockModuleResponse1)
        val nullValidationResult = mockk<ValidationResult>()
        val errorState = createMockErrorState(true, null)
        
        every { nullValidationResult.isValid } returns false
        every { nullValidationResult.message } returns null
        every { mockPageContent1.copy(pageErrorState = errorState) } returns mockUpdatedPageContent1
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) } returns nullValidationResult

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, moduleResponses)

        // Then
        assertFalse(result.isAllFieldValid)
        assertEquals(EXPECTED_SINGLE_ITEM_COUNT, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) }
    }

    // Edge case tests
    @Test
    fun testValidateAllFieldsWithLargeListShouldProcessAllItems() {
        // Given
        val pageContent3 = mockk<SequentialPageContent>()
        val inputFieldState3 = mockk<SequentialPageInputFieldState>()
        val moduleResponse3 = mockk<SequentialPageModuleResponse>()
        val updatedPageContent3 = mockk<SequentialPageContent>()
        
        every { pageContent3.pageKey } returns TEST_PAGE_KEY_3
        every { pageContent3.inputFieldState } returns inputFieldState3
        every { inputFieldState3.fieldValue } returns TEST_VALUE_VALID
        every { moduleResponse3.sequentialPageId } returns TEST_PAGE_KEY_3
        
        val items = listOf(mockPageContent1, mockPageContent2, pageContent3)
        val moduleResponses = listOf(mockModuleResponse1, mockModuleResponse2, moduleResponse3)
        
        val errorState1 = createMockErrorState(false, null)
        val errorState2 = createMockErrorState(false, null)
        val errorState3 = createMockErrorState(false, null)
        
        every { mockPageContent1.copy(pageErrorState = errorState1) } returns mockUpdatedPageContent1
        every { mockPageContent2.copy(pageErrorState = errorState2) } returns mockUpdatedPageContent2
        every { pageContent3.copy(pageErrorState = errorState3) } returns updatedPageContent3
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) } returns mockValidationResult
        every { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse2) } returns mockValidationResult
        every { mockValidator.validate(any(), mockInvestmentAccount, moduleResponse3) } returns mockValidationResult

        // When
        val result = delegator.validateAllFields(items, mockInvestmentAccount, moduleResponses)

        // Then
        assertTrue(result.isAllFieldValid)
        assertEquals(3, result.updatedItems.size)
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse1) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, mockModuleResponse2) }
        verify(exactly = VALIDATION_CALL_COUNT_ONCE) { mockValidator.validate(any(), mockInvestmentAccount, moduleResponse3) }
    }

    // Helper methods
    private fun createMockErrorState(isError: Boolean, errorMessage: String?): SequentialPageUIErrorState {
        return mockk<SequentialPageUIErrorState>().also {
            every { it.isError } returns isError
            every { it.errorMessage } returns errorMessage
        }
    }
}

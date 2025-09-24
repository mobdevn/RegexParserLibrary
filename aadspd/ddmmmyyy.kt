/**
 * Optimized Kotlin function using scope functions for updating page contents with validation
 */

private fun updateItemsWithValidation(
    currentPageContents: List<SequentialPageContent>,
    fieldKey: String,
    cappedValue: SequentialPageProcessedFieldData,
    validationResult: ValidationResult,
    selectAccount: SequentialPageInvestmentAccount?
): List<SequentialPageContent> {
    return currentPageContents.map { item ->
        item.takeIf { it.pageKey == fieldKey }?.let { matchingItem ->
            val showError = !validationResult.isValid && showErrorsOnInvalidFields
            
            matchingItem.copy(
                inputFieldState = matchingItem.inputFieldState?.let { currentState ->
                    // Update existing input field state
                    currentState.copy(
                        fieldValue = cappedValue.inputValue,
                        fieldRegex = currentState.fieldRegex ?: selectAccount?.regex,
                        maxInputLength = currentState.maxInputLength ?: selectAccount?.maxLength
                    )
                } ?: createNewInputFieldState(cappedValue, selectAccount),
                
                pageErrorState = matchingItem.pageErrorState?.copy(
                    isError = showError,
                    errorMessage = validationResult.message.takeIf { showError }
                )
            )
        } ?: item
    }
}

/**
 * Creates a new SequentialPageInputFieldState when no existing state is present
 */
private fun createNewInputFieldState(
    cappedValue: SequentialPageProcessedFieldData,
    selectAccount: SequentialPageInvestmentAccount?
): SequentialPageInputFieldState = SequentialPageInputFieldState(
    prefix = null,
    componentLabel = null,
    fieldValue = cappedValue.inputValue,
    fieldRegex = selectAccount?.regex,
    maxInputLength = selectAccount?.maxLength,
    contextualTitleFallback = null
)

/**
 * Alternative implementation with more explicit scope function usage
 */
private fun updateItemsWithValidationAlternative(
    currentPageContents: List<SequentialPageContent>,
    fieldKey: String,
    cappedValue: SequentialPageProcessedFieldData,
    validationResult: ValidationResult,
    selectAccount: SequentialPageInvestmentAccount?
): List<SequentialPageContent> {
    return (!validationResult.isValid && showErrorsOnInvalidFields).let { showError ->
        currentPageContents.map { item ->
            item.takeIf { it.pageKey == fieldKey }?.also { matchingItem ->
                // Optional: Add logging or side effects here
                println("Updating item with key: ${matchingItem.pageKey}")
            }?.let { matchingItem ->
                matchingItem.copy(
                    inputFieldState = matchingItem.inputFieldState?.apply {
                        // Could add validation or transformation logic here
                    }?.copy(
                        fieldValue = cappedValue.inputValue,
                        fieldRegex = fieldRegex ?: selectAccount?.regex,
                        maxInputLength = maxInputLength ?: selectAccount?.maxLength
                    ) ?: SequentialPageInputFieldState(
                        prefix = null,
                        componentLabel = null,
                        fieldValue = cappedValue.inputValue,
                        fieldRegex = selectAccount?.regex,
                        maxInputLength = selectAccount?.maxLength,
                        contextualTitleFallback = null
                    ),
                    pageErrorState = matchingItem.pageErrorState?.copy(
                        isError = showError,
                        errorMessage = validationResult.message.takeIf { showError }
                    )
                )
            } ?: item
        }
    }
}

/**
 * Most concise version using scope functions
 */
private fun updateItemsWithValidationConcise(
    currentPageContents: List<SequentialPageContent>,
    fieldKey: String,
    cappedValue: SequentialPageProcessedFieldData,
    validationResult: ValidationResult,
    selectAccount: SequentialPageInvestmentAccount?
): List<SequentialPageContent> = currentPageContents.map { item ->
    item.takeUnless { it.pageKey != fieldKey }?.let {
        val showError = !validationResult.isValid && showErrorsOnInvalidFields
        it.copy(
            inputFieldState = it.inputFieldState?.copy(
                fieldValue = cappedValue.inputValue,
                fieldRegex = fieldRegex ?: selectAccount?.regex,
                maxInputLength = maxInputLength ?: selectAccount?.maxLength
            ) ?: createNewInputFieldState(cappedValue, selectAccount),
            pageErrorState = it.pageErrorState?.copy(
                isError = showError,
                errorMessage = validationResult.message.takeIf { showError }
            )
        )
    } ?: item
}

/**
 * Extension function approach for even cleaner code
 */
private fun SequentialPageContent.updateWithValidation(
    fieldKey: String,
    cappedValue: SequentialPageProcessedFieldData,
    validationResult: ValidationResult,
    selectAccount: SequentialPageInvestmentAccount?,
    showErrorsOnInvalidFields: Boolean
): SequentialPageContent = takeIf { pageKey == fieldKey }?.let {
    val showError = !validationResult.isValid && showErrorsOnInvalidFields
    copy(
        inputFieldState = inputFieldState?.copy(
            fieldValue = cappedValue.inputValue,
            fieldRegex = fieldRegex ?: selectAccount?.regex,
            maxInputLength = maxInputLength ?: selectAccount?.maxLength
        ) ?: createNewInputFieldState(cappedValue, selectAccount),
        pageErrorState = pageErrorState?.copy(
            isError = showError,
            errorMessage = validationResult.message.takeIf { showError }
        )
    )
} ?: this

/**
 * Main function using the extension approach
 */
private fun updateItemsWithValidationExtension(
    currentPageContents: List<SequentialPageContent>,
    fieldKey: String,
    cappedValue: SequentialPageProcessedFieldData,
    validationResult: ValidationResult,
    selectAccount: SequentialPageInvestmentAccount?
): List<SequentialPageContent> = currentPageContents.map { 
    it.updateWithValidation(fieldKey, cappedValue, validationResult, selectAccount, showErrorsOnInvalidFields) 
}

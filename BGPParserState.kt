// SequentialPageStates.kt
package com.example.refactored

import java.util.Date

/**
 * A sealed interface to represent the overall state of the page.
 * This is the single source of truth for the UI, covering all possible states.
 */
sealed interface SequentialPageState {
    object Loading : SequentialPageState
    data class Content(
        val items: List<SequentialPageFieldCaptureItem>,
        val buttonState: SequentialPageButtonState,
        val productPickerState: SequentialPageProductPickerState,
        val investmentState: SequentialPageInvestmentAccountUIState,
        val accordionState: SequentialPageAccordionState
    ) : SequentialPageState
    data class Error(val message: String) : SequentialPageState
}

// --- UI Models for Display ---

data class SequentialPageFieldCaptureItem(
    val key: String,
    val value: String?,
    val inputFieldType: ComponentType,
    val contentType: ContentType,
    val regex: String?,
    val heading: String?,
    val title: String?,
    val text: String?,
    val minDate: Date?,
    val maxDate: Date?,
    val keyboardType: KeyboardInputType,
    val errorState: SequentialPageUIErrorState = SequentialPageUIErrorState(),
    val helpInformation: List<SequentialPageHelpInformation>?
)

data class SequentialPageProductPickerUIState(
    val productTitle: String,
    val productIndex: Int,
    val productDescription: String? = null,
    val productSelectionAccessibilityTag: String
)

// --- Sub-state Data Classes ---

data class SequentialPageButtonState(
    val isContinueButtonEnabled: Boolean = false
)

data class SequentialPageProductPickerState(
    val products: List<SequentialPageProductPickerUIState> = emptyList(),
    val selectedProductId: Int = -1
)

data class SequentialPageInvestmentAccountUIState(
    val isInputViewVisible: Boolean = false,
    val isInputViewError: Boolean = false,
    val inputViewErrorMessage: String? = null,
    val isProductSelectionErrorState: Boolean = false,
    val inputViewLabel: String = "",
    val selectedInvestmentAccountRegex: String = "",
    val inputTextSeparator: String = "",
    val transformation: OutputTransformation? = null,
    val selectedProductData: SequentialPageInvestmentProductInfo = SequentialPageInvestmentProductInfo()
)

data class SequentialPageAccordionState(
    val isContentAvailable: Boolean = false,
    val itemList: List<SequentialPageHelpInformation> = emptyList(),
    val itemMapper: Map<String, List<SequentialPageHelpInformation>> = emptyMap()
)

data class SequentialPageUIErrorState(
    val isError: Boolean = false,
    val errorMessage: String? = null
)

// SequentialPageCaptureViewModel.kt
package com.example.refactored

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SequentialPageCaptureViewModel(
    // Dependencies are injected via a ViewModel Factory, ensuring they are non-null.
    private val moduleData: SequentialPageCaptureModule,
    private val generatePayloadUseCase: GeneratePayloadUseCase,
    private val analyticsManager: ISequentialPageAnalyticsManager
    // Other dependencies like journeyFrameworkActionBuilder would be injected here.
) : ViewModel() {

    private val _pageState = MutableStateFlow<SequentialPageState>(SequentialPageState.Loading)
    val pageState: StateFlow<SequentialPageState> = _pageState.asStateFlow()

    // Actions object passed to the UI for event handling.
    val actions = SequentialPageActions(
        onFieldChanged = ::onFieldChanged,
        onProductSelected = ::onProductSelected,
        onInvestmentProductSelected = ::onInvestmentProductSelected,
        onInvestmentAccountChanged = ::onInvestmentAccountChanged,
        onNextClicked = ::onNextClicked,
        onBackPressed = ::onBackPressed,
        onAccordionToggled = ::onAccordionToggled,
        onHyperlinkClicked = ::onHyperlinkClicked
    )

    init {
        initializeState()
    }

    private fun initializeState() {
        viewModelScope.launch {
            // Process raw data into UI models
            val uiItems = moduleData.dataList.map { data ->
                // This mapping logic would be in a dedicated processor/mapper class
                SequentialPageFieldCaptureItem(
                    key = data.id,
                    value = data.value,
                    inputFieldType = ComponentType.fromString(data.type),
                    contentType = ContentType.fromString(data.contentType),
                    keyboardType = KeyboardInputType.fromString(data.keyboard?.android2?.firstOrNull()?.type),
                    regex = data.regex,
                    heading = data.heading,
                    title = data.title,
                    text = data.text,
                    minDate = data.minDate,
                    maxDate = data.maxDate,
                    helpInformation = data.helpInformation
                )
            }

            // Create initial state for sub-components
            val productPickerState = createProductPickerState(moduleData.dataList.firstOrNull()?.radioOptions)

            _pageState.value = SequentialPageState.Content(
                items = uiItems,
                buttonState = SequentialPageButtonState(isContinueButtonEnabled = false),
                productPickerState = productPickerState,
                investmentState = SequentialPageInvestmentAccountUIState(),
                accordionState = SequentialPageAccordionState()
            )

            // Log analytics after state is ready
            moduleData.sequentialPageSections.analyticsPageTag?.let {
                analyticsManager.logViewScreen(it)
            }
        }
    }

    private fun onNextClicked() {
        val currentState = _pageState.value
        if (currentState !is SequentialPageState.Content) return

        _pageState.value = SequentialPageState.Loading // Indicate processing

        viewModelScope.launch {
            // In a real app, a validation use case would run here.
            // val validationResult = validateFieldsUseCase.execute(currentState.items)
            val isValid = true // Placeholder

            if (isValid) {
                val requestBuilder = SequentialPageCaptureRequestData.Builder()
                generatePayloadUseCase.execute(currentState.items, requestBuilder)
                // Now, execute the journey action with the built payload.
                // launchNextJourney(requestBuilder.build())
            } else {
                // Update state with validation errors
                // _pageState.value = currentState.copy(items = validationResult.updatedItems)
                _pageState.value = SequentialPageState.Error("Validation Failed") // Simple error state
            }
        }
    }

    private fun onFieldChanged(key: String, value: String) {
        updateContentState { currentState ->
            val newItems = currentState.items.map { item ->
                if (item.key == key) item.copy(value = value) else item
            }
            currentState.copy(items = newItems)
        }
    }

    private fun createProductPickerState(products: List<SequentialPageInvestmentProductInfo>?): SequentialPageProductPickerState {
        val uiProducts = products?.mapIndexed { index, productInfo ->
            SequentialPageProductPickerUIState(
                productTitle = productInfo.title ?: "",
                productIndex = index,
                productSelectionAccessibilityTag = "${productInfo.title} ${productInfo.description}"
            )
        } ?: emptyList()
        return SequentialPageProductPickerState(products = uiProducts)
    }

    // Other action handlers (onProductSelected, onBackPressed, etc.) would be implemented here.
    // They would all follow the pattern of calling `updateContentState` to produce a new state.
    private fun onProductSelected(productId: Int) { /* ... */ }
    private fun onInvestmentProductSelected(productInfo: SequentialPageInvestmentProductInfo) { /* ... */ }
    private fun onInvestmentAccountChanged(accountNumber: String) { /* ... */ }
    private fun onBackPressed(id: String) { /* ... */ }
    private fun onHyperlinkClicked(action: MvcAction) { /* ... */ }
    private fun onAccordionToggled(item: SequentialPageFieldCaptureItem, isExpanded: Boolean) { /* ... */ }

    /**
     * A helper function to safely update the state only when it's in the Content state.
     */
    private fun updateContentState(update: (currentState: SequentialPageState.Content) -> SequentialPageState.Content) {
        _pageState.update {
            if (it is SequentialPageState.Content) {
                update(it)
            } else {
                it
            }
        }
    }
}

// --- Dummy Interfaces/Classes for Compilation ---
interface ISequentialPageAnalyticsManager {
    fun logViewScreen(tag: String?)
}
class SequentialPageCaptureModule(
    val dataList: List<SequentialPageCaptureData>,
    val sequentialPageSections: SequentialPageSections
)
class SequentialPageSections(val analyticsPageTag: String?)

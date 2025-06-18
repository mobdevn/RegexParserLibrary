// Defines all possible UI events that can be sent to the ViewModel.
sealed class SequentialPageEvent {
    data class OnBackPressed(val actionType: ActionType) : SequentialPageEvent()
    data class OnComponentFieldChanged(val key: String, val value: String) : SequentialPageEvent()
    data class OnFieldFocusChanged(val item: SequentialPageFieldCaptureItem) : SequentialPageEvent()
    data class OnProductSelected(val productIndex: Int) : SequentialPageEvent()
    data class OnInvestmentAccountSelected(val selectedProduct: ProductData, val item: SequentialPageFieldCaptureItem) : SequentialPageEvent()
    data class OnInvestmentAccountNumberChanged(val key: String, val accountNumber: String, val transformation: String?) : SequentialPageEvent()
    object OnHyperlinkClicked : SequentialPageEvent()
    data class OnNextButtonClicked(val productData: ProductData?, val label: String) : SequentialPageEvent()
    data class OnAccordionStateChange(val isExpanded: Boolean) : SequentialPageEvent()
    // Add other events as needed, e.g., OnReviewEditClicked, OnErrorCleared, etc.
}

// This ViewModel manages the UI state and business logic for the Sequential Page Capture flow.
class SequentialPageCaptureViewModel(
    val module: SequentialPageCaptureModule,
    private val timeAndDateUtils: TimeAndDateUtils = TimeAndDateUtils(),
    private val sequentialPageConstants: SequentialPageConstants = SequentialPageConstants(),
    private val analyticsLogger: AnalyticsLogger = DefaultAnalyticsLogger(),
    private val navigationHandler: NavigationHandler = DefaultNavigationHandler()
) : ViewModel() {

    // Represents the entire UI state for the screen.
    private val _uiState = MutableStateFlow(SequentialUiState())
    val uiState: StateFlow<SequentialUiState> = _uiState.asStateFlow()

    // Manages the enabled state of the continue button.
    private val _isContinueButtonEnabled = MutableStateFlow(false)
    val isContinueButtonEnabled: StateFlow<Boolean> = _isContinueButtonEnabled.asStateFlow()

    // Manages the accessibility tag for the continue button.
    private val _buttonAccessibilityLabel = MutableStateFlow("")
    val buttonAccessibilityLabel: StateFlow<String> = _buttonAccessibilityLabel.asStateFlow()

    // Tracks the selected product data for button actions.
    private val _selectedProductData = MutableStateFlow<ProductData?>(null)
    val selectedProductData: StateFlow<ProductData?> = _selectedProductData.asStateFlow()

    // Private SharedFlow for one-time events like showing a toast or navigating to a specific screen that shouldn't be part of State.
    private val _oneShotEvents = MutableSharedFlow<SequentialPageOneShotEvent>()
    val oneShotEvents: SharedFlow<SequentialPageOneShotEvent> = _oneShotEvents.asSharedFlow()

    // Assuming firstErrorIndexField is an internal state or derived from _uiState
    var firstErrorIndexField: Int? = null // This should ideally be part of uiState for observation

    init {
        _uiState.update {
            it.copy(
                items = module.sequentialPageSections.sections.map { section ->
                    SequentialPageFieldCaptureItem(
                        key = section.id,
                        type = section.type,
                        value = "",
                        heading = section.headingLabel,
                        helpText = section.helpText,
                        regex = section.validationRegex,
                        contentType = section.contentType,
                        minDate = section.minDate,
                        maxDate = section.maxDate,
                        keyboardType = section.keyboardType,
                        errorState = ErrorState(isError = false, errorMessage = null)
                    )
                },
                accordionItemsList = module.sequentialPageSections.sequentialPageHelpInformation?.map { info ->
                    SequentialPageHelpInformation(info.title, info.text)
                } ?: emptyList(),
                productList = module.sequentialPageSections.products?.map { product ->
                    ProductData(product.id, product.label, product.description)
                } ?: emptyList()
            )
        }
        _buttonAccessibilityLabel.value = module.sequentialPageSections.buttons.firstOrNull()?.text ?: "Continue"
        updateContinueButtonState(isFormValid())
    }

    // Main entry point for all UI events.
    fun onEvent(event: SequentialPageEvent) {
        viewModelScope.launch {
            when (event) {
                is SequentialPageEvent.OnBackPressed -> handleOnBackPressed(event.actionType)
                is SequentialPageEvent.OnComponentFieldChanged -> handleOnComponentFieldChanged(event.key, event.value)
                is SequentialPageEvent.OnFieldFocusChanged -> handleOnFieldFocusChange(event.item)
                is SequentialPageEvent.OnProductSelected -> handleOnProductSelected(event.productIndex)
                is SequentialPageEvent.OnInvestmentAccountSelected -> handleOnInvestmentAccountSelected(event.selectedProduct, event.item)
                is SequentialPageEvent.OnInvestmentAccountNumberChanged -> handleOnInvestmentAccountNumberChanged(event.key, event.accountNumber, event.transformation)
                is SequentialPageEvent.OnHyperlinkClicked -> handleOnHyperlinkClicked()
                is SequentialPageEvent.OnNextButtonClicked -> handleOnNextButtonClicked(event.productData, event.label)
                is SequentialPageEvent.OnAccordionStateChange -> handleOnAccordionStateChange(event.isExpanded)
            }
        }
    }

    // --- Private Event Handlers (Less than 10 functions) ---

    private fun handleOnBackPressed(actionType: ActionType) {
        navigationHandler.handleBackNavigation(actionType)
    }

    private fun handleOnComponentFieldChanged(key: String, value: String) {
        _uiState.update { currentState ->
            val updatedItems = currentState.items.map { item ->
                if (item.key == key) {
                    val error = validateInput(item, value)
                    item.copy(value = value, errorState = error)
                } else {
                    item
                }
            }
            currentState.copy(items = updatedItems)
        }
        updateContinueButtonState(isFormValid())
    }

    private fun handleOnFieldFocusChange(item: SequentialPageFieldCaptureItem) {
        _uiState.update { currentState ->
            val updatedAccordion = module.sequentialPageSections.sequentialPageHelpInformation?.firstOrNull {
                it.title == item.heading
            }
            currentState.copy(
                accordionItemsList = updatedAccordion?.let { listOf(it) } ?: emptyList(),
                isAccordionContentAvailable = updatedAccordion != null
            )
        }
        analyticsLogger.logEvent("accordion_state_change", mapOf("is_expanded" to true, "reason" to "field_focus"))
    }

    private fun handleOnProductSelected(productIndex: Int) {
        val selectedProduct = uiState.value.productList.getOrNull(productIndex)
        _selectedProductData.value = selectedProduct
        updateContinueButtonState(isFormValid())

        _uiState.update { currentState ->
            val productDescription = selectedProduct?.description
            if (!productDescription.isNullOrEmpty()) {
                currentState.copy(
                    accordionItemsList = listOf(SequentialPageHelpInformation(title = selectedProduct.label, text = productDescription)),
                    isAccordionContentAvailable = true
                )
            } else {
                currentState.copy(
                    accordionItemsList = emptyList(),
                    isAccordionContentAvailable = false
                )
            }
        }
        selectedProduct?.id?.let {
            analyticsLogger.logEvent("radio_product_selected", mapOf("journey_id" to it))
        }
    }

    private fun handleOnInvestmentAccountSelected(selectedProduct: ProductData, item: SequentialPageFieldCaptureItem) {
        handleOnComponentFieldChanged(item.key, selectedProduct.id)
        _selectedProductData.value = selectedProduct
    }

    private fun handleOnInvestmentAccountNumberChanged(key: String, accountNumber: String, transformation: String?) {
        val formattedAccNumber = timeAndDateUtils.getFormattedFromRegex(
            userInput = accountNumber,
            regex = Investment_ACCOUNT_NUMBER_REGEX.toString(),
            transformationType = transformation
        )
        handleOnComponentFieldChanged(key, formattedAccNumber)
    }

    private fun handleOnHyperlinkClicked() {
        navigationHandler.handleHyperlinkClick()
    }

    private fun handleOnNextButtonClicked(productData: ProductData?, label: String) {
        // Perform final validation or data submission logic
        if (isFormValid()) {
            _oneShotEvents.tryEmit(SequentialPageOneShotEvent.NavigateToNextScreen(productData))
        } else {
            _oneShotEvents.tryEmit(SequentialPageOneShotEvent.ShowErrorToast("Please complete all required fields."))
        }
        analyticsLogger.logEvent("next_button_clicked", mapOf("button_label" to label, "is_valid" to isFormValid()))
    }

    private fun handleOnAccordionStateChange(isExpanded: Boolean) {
        analyticsLogger.logEvent("accordion_state_change", mapOf("is_expanded" to isExpanded))
    }

    // --- Private Helper Functions (Less than 10 functions) ---

    private fun validateInput(item: SequentialPageFieldCaptureItem, value: String): ErrorState {
        val regexPattern = item.regex?.toRegex()
        return if (regexPattern != null && !regexPattern.matches(value)) {
            ErrorState(isError = true, errorMessage = "Invalid format")
        } else {
            ErrorState(isError = false, errorMessage = null)
        }
    }

    private fun isFormValid(): Boolean {
        return uiState.value.items.all { !it.errorState.isError && it.value.isNotEmpty() }
    }

    private fun updateContinueButtonState(
        isEnabled: Boolean,
        noRadioOptionsSelectedText: String? = null
    ) {
        _isContinueButtonEnabled.value = isEnabled
        _buttonAccessibilityLabel.value = if (isEnabled) {
            module.sequentialPageSections.buttons.firstOrNull()?.text ?: "Continue"
        } else {
            noRadioOptionsSelectedText ?: "Please complete the selection"
        }
    }

    // This method needs to be implemented or provided by the module.
    // It's a placeholder based on the original code's usage.
    fun contextualInformationForInputField(item: SequentialPageFieldCaptureItem): String? {
        // Dummy implementation: returns item's heading as contextual info
        return "Contextual info for: ${item.heading}"
    }

    fun getDateRange(start: String?, endInclusive: String?): LongRange {
        val startDate = start?.let { minDate ->
            timeAndDateUtils.strToDate(
                minDate,
                timeAndDateUtils.pattern[sequentialPageConstants.DATE_TYPE_YYYY_MM_DD]
            )?.time
        } ?: Long.MIN_VALUE
        val endDate = endInclusive?.let { maxDate ->
            timeAndDateUtils.strToDate(
                maxDate,
                timeAndDateUtils.pattern[sequentialPageConstants.DATE_TYPE_YYYY_MM_DD]
            )?.time
        } ?: Long.MAX_VALUE
        return LongRange(startDate, endDate)
    }
}

// Model classes (should be in a separate 'model' package)
package com.example.sequentialpage.model

data class SequentialUiState(
    val items: List<SequentialPageFieldCaptureItem> = emptyList(),
    val accordionItemsList: List<SequentialPageHelpInformation> = emptyList(),
    val isAccordionContentAvailable: Boolean = false,
    val productList: List<ProductData> = emptyList()
)

data class SequentialPageFieldCaptureItem(
    val key: String,
    val type: String,
    val value: String,
    val heading: String?,
    val helpText: String?,
    val regex: String?,
    val contentType: String?,
    val minDate: String?,
    val maxDate: String?,
    val keyboardType: String?,
    val errorState: ErrorState
)

data class ErrorState(
    val isError: Boolean,
    val errorMessage: String?
)

data class SequentialPageHelpInformation(
    val title: String?,
    val text: String?
)

data class ProductData(
    val id: String,
    val label: String?,
    val description: String?
)

// Dummy module for ViewModel initialization
data class SequentialPageCaptureModule(
    val sequentialPageSections: SequentialPageSections
)

data class SequentialPageSections(
    val title: String,
    val accessibilityHeadingLabel: String,
    val sections: List<SequentialPageSection>,
    val subheading: String? = null,
    val sequentialPageHelpInformation: List<SequentialPageHelpInformation>? = null,
    val buttons: List<ButtonDefinition> = emptyList(),
    val products: List<ProductDefinition>? = null
)

data class SequentialPageSection(
    val id: String,
    val type: String,
    val headingLabel: String?,
    val helpText: String?,
    val validationRegex: String?,
    val contentType: String?,
    val minDate: String?,
    val maxDate: String?,
    val keyboardType: String?
)

data class ButtonDefinition(
    val text: String,
    val style: String
)

data class ProductDefinition(
    val id: String,
    val label: String,
    val description: String?
)

sealed class SequentialPageOneShotEvent {
    data class ShowErrorToast(val message: String) : SequentialPageOneShotEvent()
    data class NavigateToNextScreen(val productData: ProductData?) : SequentialPageOneShotEvent()
}

// Dummy UI related classes (should be in a separate 'ui' package)
package com.example.sequentialpage.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Dummy imports and interfaces from original code for compilation
interface BlueprintButtonStyle
interface BlueprintSize
interface InvestmentViewState
interface BlueprintDatePickerInitialDisplayedDate
interface BlueprintDateKeyboardOptions
interface SequentialPageInputField
interface SequentialPageProductPickerPanel
interface SequentialPageInvestmentAccountView
interface SequentialPageInfoPanel
interface BlueprintScaffold
interface BlueprintTheme
interface BlueprintStandardNavBarSlot
interface BlueprintText
interface BlueprintIconButton
interface BlueprintSeparator
interface AnalyticsLogger {
    fun logEvent(eventName: String, params: Map<String, Any>)
}
interface NavigationHandler {
    fun handleBackNavigation(actionType: ActionType)
    fun handleHyperlinkClick()
}

class DefaultNavigationHandler : NavigationHandler {
    override fun handleBackNavigation(actionType: ActionType) {
        println("Navigating back with action: $actionType")
    }

    override fun handleHyperlinkClick() {
        println("Hyperlink clicked!")
    }
}

class DefaultAnalyticsLogger : AnalyticsLogger {
    override fun logEvent(eventName: String, params: Map<String, Any>) {
        println("Logging event: $eventName with params: $params")
    }
}

object BlueprintTheme {
    val colors = Colors()
}

object LocalTypography {
    val current = Typography()
}

object LocalSpacing {
    val current = Spacing()
}

object BlueprintIcons {
    object Outlined {
        val Back = Any()
    }
}

class Colors {
    val title = Any()
    val interactive = Any()
}

class Typography {
    val large = Any()
    val medium = Any()
}

class Spacing {
    val medium = 16.dp // Example value
    val extraSmall = 4.dp // Example value
}

object SequentialPageConstants {
    const val DATE_TYPE_YYYY_MM_DD = "yyyy-MM-dd"
    const val PAGE_WEIGHT = 1f
}

object Investment_ACCOUNT_NUMBER_REGEX

object UKDateFormat

object RADIO_OPTION: String = "RADIO_OPTION"
object DATE: String = "DATE"
object INPUT_FIELD: String = "INPUT_FIELD"
object INFO_PANEL: String = "INFO_PANEL"
object RADIO_INPUT: String = "RADIO_INPUT"
object HYPERLINK: String = "HYPERLINK"

object FIRST_FIELD_INDEX: Int = 0

object Done: Any() // Represents ImeAction.Done

object PRIMARY_BUTTON_TYPE: String = "PRIMARY"
object SECONDARY_BUTTON_TYPE: String = "SECONDARY"

object BlueprintDatePickerInitialDisplayedDate {
    object UpperBound: Any()
}

// Dummy functions/classes to allow compilation in this context
data class MvcAction(val type: Any)

enum class ActionType { BACK }

fun getAction(actionType: ActionType): MvcAction? = MvcAction(actionType)
fun configureBlueprintTheme(): Any = Any()
fun getContainerColor(sections: SequentialPageSections): Any = Any()
fun heading(): String = ""
// These should ideally be in a composable scope or provided via composition local
fun rememberCoroutineScope(): Any = Any()
fun rememberScrollState(): Any = Any()
fun isBlueprintDarkTheme(): Boolean = true
fun spacedBy(value: Any): Any = Any()
fun blueprintWindowSizePadding(): Modifier = Modifier.Companion
fun fillMaxWidth(): Modifier = Modifier.Companion
fun focusRequester(focusRequester: FocusRequester): Modifier = Modifier.Companion
fun clickable(onClick: () -> Unit, enabled: Boolean): Modifier = Modifier.Companion
fun Modifier.verticalScroll(scrollState: Any): Modifier = this // Dummy
fun Modifier.weight(weight: Float): Modifier = this // Dummy
fun Modifier.then(other: Modifier): Modifier = this // Dummy
fun Modifier.semantics(block: Any.() -> Unit): Modifier = this // Dummy
fun Modifier.clearAndSetSemantics(block: Any.() -> Unit): Modifier = this // Dummy

fun blueprintPrimaryButton(): BlueprintButtonStyle { return object : BlueprintButtonStyle {} }
fun blueprintSecondaryButton(): BlueprintButtonStyle { return object : BlueprintButtonStyle {} }
fun BlueprintButton(modifier: Modifier, enabled: Boolean, content: @Composable (Any, Any) -> Unit, onClick: () -> Unit, style: BlueprintButtonStyle) {}
fun BlueprintDatePickerField(modifier: Modifier, label: String?, hint: String?, date: Any?, onDateChanged: (Any, Any) -> Unit, keyboardOptions: Any, showDateFormatError: Boolean, isError: Boolean, errorMessage: String?, dateFormat: Any, dateRange: LongRange, initialDisplayedDate: Any) {}
fun SequentialPageInputField(modifier: Modifier, item: SequentialPageFieldCaptureItem, onValueChanged: (String, String, String?) -> Unit, onViewFocusChanged: (String) -> Unit, outputTransformation: String?, state: SequentialPageInputFieldState, fieldValue: String?, contextualTitleFallback: String?, componentLabel: String?) {}
fun SequentialPageInfoPanel(spcSections: SequentialPageSections) {}
fun SequentialPageInvestmentAccountView(modifier: Modifier, item: SequentialPageFieldCaptureItem, onSelectedInvestmentAccount: (ProductData) -> Unit, onValueChanged: (String, String, String?) -> Unit, investmentViewState: InvestmentViewState?, inputFieldState: SequentialPageInputFieldState) {}
fun SequentialPageProductPickerPanel(label: String?, productList: List<ProductData>, onProductSelected: (Int) -> Unit) {}
fun SequentialPageReviewPanel(spcSections: SequentialPageSections, onEditButtonClicked: (String) -> Unit) {}
fun BlueprintIconButton(modifier: Modifier, icon: Any, accessibility: BlueprintIconButtonAccessibility, onClick: () -> Unit) {}
data class BlueprintIconButtonAccessibility(val contentDescription: String)
fun BlueprintStandardNavBarSlot(title: @Composable () -> Unit, navigationIcon: @Composable (() -> Unit)?) {}
fun BlueprintText(modifier: Modifier, text: String, style: Any, color: Any, fontWeight: Any? = null, fontSize: Any? = null, textDecoration: Any? = null, textAlign: Any? = null) {}
fun BlueprintSeparator() {}
fun Any.contentDescription(description: String) {}
fun Any.traversalIndex(index: Float) {}

// Dummy TimeAndDateUtils for compilation
class TimeAndDateUtils {
    fun strToDateWithLenientCheck(value: String?, pattern: String): Any? = null
    fun formatDate(date: Any?, formatPattern: String): String = ""
    fun strToDate(dateString: String, pattern: String): Any? = null
    val pattern = mapOf(SequentialPageConstants.DATE_TYPE_YYYY_MM_DD to "yyyy-MM-dd")

    fun getTransformationType(contentType: String): String? = null
    fun checkInputLen(regex: String, transformationType: String?): Int = 0
    fun getFormattedFromRegex(userInput: String, regex: String, transformationType: String?): String = userInput
}

// Main UI Composables
package com.example.sequentialpage.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import com.example.sequentialpage.SequentialPageCaptureViewModel
import com.example.sequentialpage.SequentialPageEvent
import com.example.sequentialpage.model.SequentialPageFieldCaptureItem
import com.example.sequentialpage.model.SequentialPageSections
import com.example.sequentialpage.ui.*
import com.example.sequentialpage.ui.components.* // Import all new components
import com.example.sequentialpage.ui.spacers.spacedBy // Assuming this is a custom spacing function
import kotlinx.coroutines.flow.collectLatest // For SharedFlow

// Main screen composable, orchestrating the different sections.
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SequentialPageCaptureScreen(
    viewModel: SequentialPageCaptureViewModel,
    // Provide a lambda for handling one-shot events, e.g., showing a Toast or navigating.
    onOneShotEvent: (SequentialPageOneShotEvent) -> Unit
) {
    // Collect states from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val isContinueButtonEnabled by viewModel.isContinueButtonEnabled.collectAsState()
    val buttonAccessibilityLabel by viewModel.buttonAccessibilityLabel.collectAsState()
    val selectedProductData by viewModel.selectedProductData.collectAsState()
    val scrollState = rememberScrollState()

    // Handle one-shot events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.oneShotEvents.collectLatest { event ->
            onOneShotEvent(event)
        }
    }

    BlueprintTheme(
        isDarkTheme = isBlueprintDarkTheme(),
        app = configureBlueprintTheme()
    ) {
        BlueprintScaffold(
            useWindowSizePadding = false,
            containerColor = getContainerColor(viewModel.module.sequentialPageSections),
            navBar = {
                SequentialPageHeader(
                    viewModel = viewModel,
                    onEvent = viewModel::onEvent
                )
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(paddingValues)
                        .padding(
                            top = LocalSpacing.current.medium,
                            bottom = LocalSpacing.current.medium
                        ),
                    verticalArrangement = spacedBy(LocalSpacing.current.extraSmall)
                ) {
                    SequentialPageContent(
                        viewModel = viewModel,
                        scrollState = scrollState,
                        modifier = Modifier.weight(SequentialPageConstants.PAGE_WEIGHT),
                        onEvent = viewModel::onEvent // Pass onEvent to content
                    )

                    // Buttons section at the bottom
                    SequentialPageButtons(
                        spcSections = viewModel.module.sequentialPageSections,
                        onNextButtonClicked = { product, label ->
                            viewModel.onEvent(SequentialPageEvent.OnNextButtonClicked(product, label))
                        },
                        buttonState = isContinueButtonEnabled,
                        scrollState = scrollState,
                        accessibilityTag = buttonAccessibilityLabel, // Directly use the state for accessibility
                        selectedProductData = selectedProductData
                    )
                }
            }
        )
    }
}

// Composable for the header/navigation bar section.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SequentialPageHeader(viewModel: SequentialPageCaptureViewModel, onEvent: (SequentialPageEvent) -> Unit) {
    with(viewModel.module) {
        BlueprintStandardNavBarSlot(
            title = {
                BlueprintText(
                    modifier = Modifier.semantics {
                        this.contentDescription =
                            viewModel.module.sequentialPageSections.accessibilityHeadingLabel
                        heading()
                    },
                    style = LocalTypography.current.medium,
                    text = viewModel.module.sequentialPageSections.title
                )
            },
            navigationIcon = {
                getAction(ActionType.BACK)?.let {
                    BlueprintIconButton(
                        modifier = Modifier
                            .semantics {
                                traversalIndex = BackArrowIndex
                            },
                        icon = BlueprintIcons.Outlined.Back,
                        accessibility = BlueprintIconButtonAccessibility(
                            contentDescription = "back"
                        )
                    ) {
                        onEvent(SequentialPageEvent.OnBackPressed(ActionType.BACK))
                    }
                }
            }
        )
    }
}

// Composable for the main scrollable content area.
@Composable
fun SequentialPageContent(
    viewModel: SequentialPageCaptureViewModel,
    scrollState: Any,
    modifier: Modifier = Modifier,
    onEvent: (SequentialPageEvent) -> Unit // Pass onEvent down
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequesters = remember {
        List(uiState.items.size) { FocusRequester() }
    }

    LaunchedEffect(Unit) {
        viewModel.firstErrorIndexField?.let { index ->
            coroutineScope.launch {
                focusRequesters[index].requestFocus()
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState),
        verticalArrangement = spacedBy(LocalSpacing.current.medium)
    ) {
        MainHeadingAndSubHeading(viewModel.module.sequentialPageSections)

        uiState.items.forEachIndexed { index, item ->
            SequentialPageFieldContainer(
                item = item,
                viewModel = viewModel, // ViewModel itself passed, but individual event calls are made via onEvent
                focusRequester = focusRequesters[index],
                contextualTitleFallback = viewModel.contextualInformationForInputField(item),
                onEvent = onEvent // Pass onEvent to field containers
            )
        }

        if (uiState.isAccordionContentAvailable) {
            uiState.accordionItemsList.forEach { info ->
                SequentialPageAccordionSection(
                    info = info,
                    onStateChange = { isExpanded -> onEvent(SequentialPageEvent.OnAccordionStateChange(isExpanded)) }
                )
            }
        }
    }
}


// UI Components (should be in a separate 'ui.components' package)
package com.example.sequentialpage.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.example.sequentialpage.SequentialPageCaptureViewModel
import com.example.sequentialpage.SequentialPageEvent
import com.example.sequentialpage.model.ProductData
import com.example.sequentialpage.model.SequentialPageFieldCaptureItem
import com.example.sequentialpage.model.SequentialPageHelpInformation
import com.example.sequentialpage.model.SequentialPageSections
import com.example.sequentialpage.ui.*
import com.example.sequentialpage.ui.spacers.spacedBy

// Composable for the main heading and subheading of the page.
@Composable
fun MainHeadingAndSubHeading(spcSections: SequentialPageSections) {
    // Main heading
    BlueprintText(
        modifier = Modifier
            .blueprintWindowSizePadding()
            .fillMaxWidth()
            .semantics {
                this.contentDescription = spcSections.accessibilityHeadingLabel
                heading()
            },
        style = LocalTypography.current.large,
        text = spcSections.title,
        color = BlueprintTheme.colors.title,
        fontWeight = FontWeight.Medium
    )

    // Subheading if available
    spcSections.subheading?.takeUnless { it.isEmpty() }?.let { subHeading ->
        BlueprintText(
            modifier = Modifier
                .blueprintWindowSizePadding()
                .fillMaxWidth(),
            text = subHeading,
        )
    }
}

// A container composable for each field, applying common modifiers.
@Composable
fun SequentialPageFieldContainer(
    item: SequentialPageFieldCaptureItem,
    viewModel: SequentialPageCaptureViewModel, // Still need viewModel for utility functions
    focusRequester: FocusRequester,
    contextualTitleFallback: String?,
    onEvent: (SequentialPageEvent) -> Unit // Pass onEvent down
) {
    Column(
        modifier = Modifier.focusRequester(focusRequester)
    ) {
        when (item.type) {
            DATE -> DateInputField(item, viewModel, onEvent)
            INPUT_FIELD -> GeneralInputField(item, viewModel, contextualTitleFallback, onEvent)
            INFO_PANEL -> SequentialPageInfoPanel(viewModel.module.sequentialPageSections)
            RADIO_INPUT -> InvestmentAccountInput(item, viewModel, contextualTitleFallback, onEvent)
            RADIO_OPTION -> ProductSelectionInput(item, viewModel, onEvent)
            HYPERLINK -> HyperlinkSection(item, onEvent)
            "REVIEW_FIELD" -> SequentialPageReviewPanel(viewModel.module.sequentialPageSections, { id -> onEvent(SequentialPageEvent.OnBackPressed(ActionType.BACK)) })
            else -> {
                // Handle unknown type or log error
            }
        }
    }
}

// Composable for Date input field.
@Composable
fun DateInputField(
    item: SequentialPageFieldCaptureItem,
    viewModel: SequentialPageCaptureViewModel, // ViewModel for utility access
    onEvent: (SequentialPageEvent) -> Unit
) {
    val dateValue by remember(item.value) { mutableStateOf(item.value) }

    BlueprintDatePickerField(
        modifier = Modifier.blueprintWindowSizePadding(),
        label = item.heading,
        hint = item.helpText,
        date = viewModel.timeAndDateUtils.strToDateWithLenientCheck(dateValue, "yyyy-MM-dd"),
        onDateChanged = { date, _ ->
            item.key?.let { key ->
                onEvent(SequentialPageEvent.OnComponentFieldChanged(key, viewModel.timeAndDateUtils.formatDate(date, "yyyy-MM-dd")))
            }
        },
        keyboardOptions = BlueprintDateKeyboardOptions(
            imeAction = Done,
            keyboardType = item.keyboardType
        ),
        showDateFormatError = false,
        isError = item.errorState.isError,
        errorMessage = item.errorState.errorMessage,
        dateFormat = UKDateFormat,
        dateRange = viewModel.getDateRange(item.minDate, item.maxDate),
        initialDisplayedDate = BlueprintDatePickerInitialDisplayedDate.UpperBound
    )
}

// Composable for general text input fields.
@Composable
fun GeneralInputField(
    item: SequentialPageFieldCaptureItem,
    viewModel: SequentialPageCaptureViewModel, // ViewModel for utility access
    contextualTitleFallback: String?,
    onEvent: (SequentialPageEvent) -> Unit
) {
    val fieldValue by remember(item.value) { mutableStateOf(item.value) }

    SequentialPageInputField(
        modifier = Modifier,
        item = item,
        onValueChanged = { key, value, transformation ->
            onEvent(SequentialPageEvent.OnComponentFieldChanged(key, value))
        },
        onViewFocusChanged = {
            onEvent(SequentialPageEvent.OnFieldFocusChanged(item))
        },
        outputTransformation = viewModel.timeAndDateUtils.getTransformationType(item.contentType ?: ""),
        state = SequentialPageInputFieldState(
            fieldValue = fieldValue,
            contextualTitleFallback = contextualTitleFallback,
            componentLabel = item.heading.takeUnless { it.isNullOrEmpty() }
        )
    )
}

// Composable for Investment Account input (RADIO_INPUT type).
@Composable
fun InvestmentAccountInput(
    item: SequentialPageFieldCaptureItem,
    viewModel: SequentialPageCaptureViewModel, // ViewModel for utility access
    contextualTitleFallback: String?,
    onEvent: (SequentialPageEvent) -> Unit
) {
    SequentialPageInvestmentAccountView(
        modifier = Modifier,
        item = item,
        onSelectedInvestmentAccount = { selectedProduct ->
            onEvent(SequentialPageEvent.OnInvestmentAccountSelected(selectedProduct, item))
        },
        onValueChanged = { key, accountNumber, transformation ->
            onEvent(SequentialPageEvent.OnInvestmentAccountNumberChanged(key, accountNumber, transformation))
        },
        investmentViewState = InvestmentViewStates.DEFAULT,
        inputFieldState = SequentialPageInputFieldState(
            fieldValue = INVESTMENT_ACCOUNT_DEFAULT_LABEL,
            contextualTitleFallback = contextualTitleFallback,
            componentLabel = InvestmentViewStates.InputViewLabel
        )
    )
}

// Composable for Product Selection (RADIO_OPTION type).
@Composable
fun ProductSelectionInput(
    item: SequentialPageFieldCaptureItem,
    viewModel: SequentialPageCaptureViewModel, // ViewModel for productList access
    onEvent: (SequentialPageEvent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    SequentialPageProductPickerPanel(
        label = item.heading,
        productList = uiState.productList,
        onProductSelected = { productIndex ->
            onEvent(SequentialPageEvent.OnProductSelected(productIndex))
            item.key?.let { key ->
                uiState.productList.getOrNull(productIndex)?.id?.let { productId ->
                    onEvent(SequentialPageEvent.OnComponentFieldChanged(key, productId))
                }
            }
        }
    )
}

// Composable for Hyperlink display.
@Composable
fun HyperlinkSection(
    item: SequentialPageFieldCaptureItem,
    onEvent: (SequentialPageEvent) -> Unit
) {
    item.heading?.let { label ->
        BlueprintText(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = label
                }
                .clickable(
                    enabled = true,
                    onClick = {
                        onEvent(SequentialPageEvent.OnHyperlinkClicked)
                    }
                ),
            style = LocalTypography.current.medium,
            text = label,
            color = BlueprintTheme.colors.interactive,
            fontWeight = FontWeight.W500,
            fontSize = BlueprintSize.FontSizeMedium,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center
        )
    }
}

// Composable for a single accordion section.
@Composable
fun SequentialPageAccordionSection(info: SequentialPageHelpInformation, onStateChange: (Boolean) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        info.title?.let { accordionTitle ->
            info.text?.let { accordionDesc ->
                BlueprintText(text = accordionTitle, style = LocalTypography.current.medium)
                if (isExpanded) {
                    BlueprintText(text = accordionDesc, style = LocalTypography.current.medium)
                }
            }
        }
        BlueprintButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            onClick = {
                isExpanded = !isExpanded
                onStateChange(isExpanded)
            },
            content = { _, _ -> BlueprintText(text = if (isExpanded) "Collapse" else "Expand") },
            style = blueprintSecondaryButton()
        )
    }
}

// Composable for the buttons section at the bottom of the screen.
@Composable
fun SequentialPageButtons(
    spcSections: SequentialPageSections,
    onNextButtonClicked: (ProductData?, String) -> Unit,
    buttonState: Boolean,
    scrollState: Any,
    accessibilityTag: String, // Directly receiving the string now
    selectedProductData: ProductData?
) {
    Column {
        if ((scrollState as? ScrollState)?.maxValue ?: 0 > 0) {
            BlueprintSeparator()
        }
        spcSections.buttons.forEach { button ->
            SequentialPageActionButton(
                onNextButtonClicked = { productData ->
                    onNextButtonClicked(productData, button.text)
                },
                viewState = buttonState,
                // Pass the button's specific accessibility tag or the dynamic one
                accessibilityTag = accessibilityTag, // Use the passed dynamic tag
                label = button.text,
                productData = selectedProductData,
                style = button.style
            )
        }
    }
}

// Composable for a single action button.
@Composable
fun SequentialPageActionButton(
    onNextButtonClicked: (ProductData?) -> Unit,
    viewState: Boolean,
    accessibilityTag: String, // Directly receiving the string now
    label: String,
    productData: ProductData?,
    style: String?
) {
    BlueprintButton(
        modifier = Modifier
            .padding(top = LocalSpacing.current.medium)
            .fillMaxWidth()
            .blueprintWindowSizePadding(),
        enabled = viewState,
        content = { _, _ ->
            BlueprintText(
                modifier = Modifier.semantics {
                    contentDescription = accessibilityTag
                },
                text = label,
                color = BlueprintTheme.colors.interactive
            )
        },
        onClick = {
            onNextButtonClicked(productData)
        },
        style = getButtonStyle(style)
    )
}

// Helper function to determine button style.
@Composable
fun getButtonStyle(style: String?): BlueprintButtonStyle {
    return when (style) {
        PRIMARY_BUTTON_TYPE -> blueprintPrimaryButton()
        SECONDARY_BUTTON_TYPE -> blueprintSecondaryButton()
        else -> blueprintPrimaryButton()
    }
}

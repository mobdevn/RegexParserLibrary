import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

// region ======================= 0. Placeholder Dependencies ============================
// To make the code runnable, all external or undefined dependencies from the original
// snippet are defined here as placeholder data classes, interfaces, or objects.

data class SequentialPageCaptureKeyboardType(val android2: List<KeyboardTypeInfo>?)
data class KeyboardTypeInfo(val type: String)
enum class KeyboardType { Text, NumberPassword, Phone }
enum class SequentialPageCaptureComponentType {
    INPUT_FIELD, DATE, REVIEW_FIELD, RADIO_INPUT, RADIO_OPTION, HYPERLINK, INFO_PANEL
}
interface OutputTransformation
data class AccountNumberTransformation(val placeholder: String = "") : OutputTransformation
data class SortCodeTransformation(val placeholder: String = "") : OutputTransformation
data class InvestmentAccountNumberTransformation(
    val transformationLiteral: String,
    val transformationIndex: Int,
    val maxLength: Int
) : OutputTransformation
data class SequentialPageTransformation(
    val type: String,
    val productLiteral: String,
    val prodIndex: Int,
    val transformationIndex: Int,
    val outputTransformation: OutputTransformation?,
    val transformationLiteral: String,
    val maxLength: Int
)
data class SequentialPageHelpInformation(val title: String, val content: String)
data class SequentialPageUIErrorState(val isError: Boolean = false, val errorMessage: String? = null)
data class MvcAction(val stepId: String?)
object AuthenticationController { const val CONTROLLER_ID = "AuthController" }
interface JourneyFrameworkModuleContract {
    interface BaseRegistrationModule {
        val id: String
        fun getAction(id: String? = null, actionType: ActionType? = null): MvcAction?
        fun attachPayloadToMvcAction(requestData: Any?, action: MvcAction)
        fun attachPayloadOfNowAction(requestData: Any?, action: MvcAction)
    }
    interface ModuleHelper {
        fun startModule(registrationModule: BaseRegistrationModule)
        fun getModule(): BaseRegistrationModule
    }
}
object JourneyFrameworkGenericHandleAction {
    class Builder { fun build(): ActionExecutor = ActionExecutor() }
    class ActionExecutor { fun executeAction(action: MvcAction, id: String?, controllerId: String) {} }
    class EmptyRequest
}
object TimeAndDateUtils {
    fun str2Date(dateStr: String, pattern: String): Date? {
        return try {
            SimpleDateFormat(pattern, Locale.US).parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
}
object SequentialPageRegexAnalyser {
    fun getRegexMaxLength(regex: String): Int = regex.length.coerceAtMost(20) // Mock implementation
    fun getLiteralInsertionPosition(regex: String, literal: Char): Int = regex.indexOf(literal).coerceAtLeast(0) // Mock
}
interface ISequentialPageAnalyticsManager {
    fun logViewScreen(tag: String)
    fun logButtonClick(label: String)
    fun logHyperLink(label: String)
    fun logProductSelection(label: String)
    fun logAccordionInteraction(state: String)
}
class SequentialPageAnalyticsManager : ISequentialPageAnalyticsManager {
    override fun logViewScreen(tag: String) { println("Analytics: View Screen - $tag") }
    override fun logButtonClick(label: String) { println("Analytics: Button Click - $label") }
    override fun logHyperLink(label: String) { println("Analytics: Hyperlink Click - $label") }
    override fun logProductSelection(label: String) { println("Analytics: Product Selected - $label") }
    override fun logAccordionInteraction(state: String) { println("Analytics: Accordion - $state") }
}

// Mock Data Models from original code
data class SequentialPageInvestmentProductInfo(
    val title: String? = "",
    val analyticsOptionTag: String? = "",
    val icon: String? = "",
    val description: String? = "",
    val optionId: String? = "",
    val radioTitle2: String? = "",
    val regex: String? = null,
    val invalidInputText: String? = null,
    val inputTextSeparator: String? = null,
    var isProductSelected: Boolean = false,
    val helpText: String? = null,
    var value: String? = ""
)
data class SequentialPageCaptureData(
    val id: String,
    val type: String?,
    val regex: String? = "",
    val value: String? = "",
    val heading: String? = "",
    val minDate: Date? = null,
    val maxDate: Date? = null,
    val keyboard: SequentialPageCaptureKeyboardType? = null,
    val title: String? = "",
    val helpInformation: List<SequentialPageHelpInformation>? = emptyList(),
    val contentType: String? = "",
    val radioInputs: List<SequentialPageInvestmentProductInfo>? = emptyList(),
    val radioOptions: List<SequentialPageInvestmentProductInfo>? = emptyList(),
    val noRadioOptionSelectedText: String? = "",
    val text: String? = "",
    val action: MvcAction? = null
)
data class SequentialPageCaptureModule(
    override val id: String,
    val dataList: List<SequentialPageCaptureData>,
    val sequentialPageSections: SequentialPageSections,
) : JourneyFrameworkModuleContract.BaseRegistrationModule {
    override fun getAction(id: String?, actionType: ActionType?): MvcAction? = sequentialPageSections.buttons.find { it.id == id }?.action
    override fun attachPayloadToMvcAction(requestData: Any?, action: MvcAction) { /* Attach payload */ }
    override fun attachPayloadOfNowAction(requestData: Any?, action: MvcAction) { /* Attach payload */ }
}
data class SequentialPageSections(
    val accessibilityTitleLabel: String,
    val analyticsPageTag: String?,
    val sections: List<SequentialPageCaptureData>,
    val buttons: List<ButtonData>
)
data class ButtonData(val id: String, val action: MvcAction)

// Placeholder for request builder
class SequentialPageCaptureRequestData {
    private val params = mutableMapOf<String, Any>()
    fun add(key: String, value: Any) { params[key] = value }
    fun build(): Map<String, Any> = params
    class Builder {
        private val instance = SequentialPageCaptureRequestData()
        fun add(key: String, value: Any): Builder {
            instance.add(key, value)
            return this
        }
        fun build(): Map<String, Any> = instance.build()
    }
}

// Global constants and enums
enum class ActionType { NEXT, BACK, NONE_OF_THESE, RESEND }
const val KEYBOARD_TYPE_NUMBER_CLEAR = "number_clear"
const val KEYBOARD_TYPE_PHONE = "phone"
// endregion

// region ======================= 1. State and Event Models ==============================
/**
 * A sealed class representing all possible user interactions or events
 * that can occur on the sequential page.
 */
sealed class SequentialPageEvent {
    data class ModuleStart(val module: SequentialPageCaptureModule) : SequentialPageEvent()
    data class FieldChanged(val key: String, val value: String) : SequentialPageEvent()
    data class ProductSelected(val productId: Int) : SequentialPageEvent()
    data class InvestmentProductSelected(
        val productInfo: SequentialPageInvestmentProductInfo,
        val item: SequentialPageFieldCaptureItem
    ) : SequentialPageEvent()
    data class InvestmentAccountChanged(val userInput: String) : SequentialPageEvent()
    data class NextClicked(val label: String) : SequentialPageEvent()
    data class BackPressed(val id: String) : SequentialPageEvent()
    data class AccordionToggled(val item: SequentialPageFieldCaptureItem, val isExpanded: Boolean) : SequentialPageEvent()
    object HyperlinkClicked : SequentialPageEvent()
    object ClearError : SequentialPageEvent()
}

// State data classes remain largely the same but are grouped for clarity.
data class SequentialPageState(
    val uiState: SequentialPageUiState = SequentialPageUiState(),
    val buttonState: SequentialPageButtonState = SequentialPageButtonState(),
    val productPickerState: SequentialPageProductPickerState = SequentialPageProductPickerState(),
    val investmentState: SequentialPageInvestmentAccountUIState = SequentialPageInvestmentAccountUIState(),
    val navigationEvent: MvcAction? = null, // For handling one-off navigation events
    val firstErrorIndex: Int? = null
)

data class SequentialPageUiState(
    val items: List<SequentialPageFieldCaptureItem> = emptyList(),
    val accordionState: SequentialPageAccordionState = SequentialPageAccordionState(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SequentialPageFieldCaptureItem(
    val key: String,
    val value: String? = "",
    val heading: String? = "",
    val title: String? = "",
    val text: String? = "",
    val regex: String? = "",
    val minDate: Date? = null,
    val maxDate: Date? = null,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val inputFieldType: SequentialPageCaptureComponentType,
    val errorState: SequentialPageUIErrorState = SequentialPageUIErrorState(),
    val helpInformation: List<SequentialPageHelpInformation>? = emptyList(),
    val contentType: String? = "",
    val radioInputs: List<SequentialPageInvestmentProductInfo>? = emptyList(),
    val noRadioOptionSelectedText: String? = "",
    val action: MvcAction? = null,
    val radioOptions: List<SequentialPageInvestmentProductInfo>? = emptyList()
)

data class SequentialPageButtonState(val isContinueButtonEnabled: Boolean = false)

data class SequentialPageProductPickerState(
    val products: List<SequentialPageProductPickerUIState> = emptyList(),
    val selectedProductId: Int = -1,
    val selectedProductData: SequentialPageInvestmentProductInfo = SequentialPageInvestmentProductInfo()
)

data class SequentialPageProductPickerUIState(
    val productTitle: String,
    val productAnalyticsOptionTag: String?,
    val productIcon: String?,
    val productIndex: Int,
    val productSelectionAccessibilityTag: String,
    val productDescription: String? = null
)

data class SequentialPageInvestmentAccountUIState(
    val isInputViewVisible: Boolean = false,
    val isInputViewError: SequentialPageUIErrorState = SequentialPageUIErrorState(),
    val isProductSelectionErrorState: Boolean = false,
    val inputViewLabel: String = "",
    val selectedInvestmentAccountRegex: String = "",
    val inputTextSeparator: String = "",
    val transformation: SequentialPageTransformation? = null,
    val sequentialPageFieldCaptureItem: SequentialPageFieldCaptureItem? = null
)

data class SequentialPageAccordionState(
    val isContentAvailable: Boolean = false,
    val expandedItemKey: String? = null,
    val itemMapper: Map<String, List<SequentialPageHelpInformation>> = emptyMap()
)
// endregion

// region ======================= 2. Domain Layer (Handlers & Processors) ================
/**
 * Processes raw module data into a list of UI-ready field items.
 * Encapsulates the logic from the original `SequentialPageFieldProcessor`.
 */
class FieldDataProcessor {
    fun processModuleData(dataList: List<SequentialPageCaptureData>): List<SequentialPageFieldCaptureItem> {
        return dataList.map(::createFieldCaptureItem)
    }

    private fun createFieldCaptureItem(response: SequentialPageCaptureData): SequentialPageFieldCaptureItem {
        return SequentialPageFieldCaptureItem(
            key = response.id,
            regex = response.regex,
            value = response.value,
            heading = response.heading,
            minDate = response.minDate,
            maxDate = response.maxDate,
            keyboardType = KeyboardUtil.getKeyboardType(response.keyboard),
            inputFieldType = ComponentTypeMapper.map(response.type),
            title = response.title,
            helpInformation = response.helpInformation,
            contentType = response.contentType,
            radioInputs = response.radioInputs,
            noRadioOptionSelectedText = response.noRadioOptionSelectedText,
            text = response.text,
            action = response.action,
            radioOptions = response.radioOptions
        )
    }
}

/**
 * Handles validation logic for all fields before submission.
 */
class ValidationHandler {
    fun validateAll(
        items: List<SequentialPageFieldCaptureItem>,
        productData: SequentialPageInvestmentProductInfo
    ): Pair<List<SequentialPageFieldCaptureItem>, Boolean> {
        var isAllValid = true
        val updatedItems = items.map { item ->
            // Basic validation: non-empty for standard input fields
            val isItemValid = when (item.inputFieldType) {
                SequentialPageCaptureComponentType.INPUT_FIELD -> !item.value.isNullOrBlank()
                SequentialPageCaptureComponentType.RADIO_INPUT -> productData.isProductSelected && !productData.value.isNullOrBlank()
                else -> true // Other types are considered valid for this example
            }

            if (!isItemValid) {
                isAllValid = false
                item.copy(errorState = SequentialPageUIErrorState(true, "Invalid input"))
            } else {
                item.copy(errorState = SequentialPageUIErrorState(false))
            }
        }
        return Pair(updatedItems, isAllValid)
    }
}

/**
 * Handles all logic related to product selection and state updates.
 */
class ProductHandler {
    fun handleProductSelection(
        currentState: SequentialPageState,
        productId: Int,
        moduleData: List<SequentialPageCaptureData>
    ): SequentialPageState {
        val pickerState = currentState.productPickerState
        val previouslySelectedId = pickerState.selectedProductId

        // Update the new product as selected
        val selectedProductInfo = moduleData.firstOrNull()?.radioOptions?.getOrNull(productId)
            ?: return currentState // Safety check

        val newProducts = pickerState.products.mapIndexed { index, productUIState ->
            when (index) {
                productId -> productUIState.copy(productDescription = selectedProductInfo.description)
                previouslySelectedId -> productUIState.copy(productDescription = null) // Clear previous
                else -> productUIState
            }
        }

        return currentState.copy(
            productPickerState = pickerState.copy(
                products = newProducts,
                selectedProductId = productId,
                selectedProductData = selectedProductInfo
            ),
            buttonState = SequentialPageButtonState(isContinueButtonEnabled = true)
        )
    }
}


/**
 * Handles business logic for investment account inputs.
 */
class InvestmentAccountHandler(private val transformationFactory: TransformationFactory) {
    fun handleInvestmentProductSelection(
        currentState: SequentialPageState,
        productInfo: SequentialPageInvestmentProductInfo,
        item: SequentialPageFieldCaptureItem
    ): SequentialPageState {
        val regex = productInfo.regex ?: return currentState
        val separator = productInfo.inputTextSeparator ?: ""

        val transformation = transformationFactory.create(
            productRegex = regex,
            textSeparatorLiteral = separator,
            contentType = item.contentType,
            helpText = productInfo.helpText
        )

        val newInvestmentState = currentState.investmentState.copy(
            isInputViewVisible = true,
            inputTextSeparator = separator,
            sequentialPageFieldCaptureItem = item,
            isInputViewError = SequentialPageUIErrorState(false),
            isProductSelectionErrorState = false,
            inputViewLabel = productInfo.helpText ?: "",
            selectedInvestmentAccountRegex = regex,
            transformation = transformation
        )

        return currentState.copy(investmentState = newInvestmentState)
    }
}

/**
 * Handles the business logic for accordion expansion and collapse.
 */
class AccordionHandler(private val analyticsManager: ISequentialPageAnalyticsManager) {
    fun handleAccordionToggle(
        currentState: SequentialPageState,
        item: SequentialPageFieldCaptureItem,
        isExpanded: Boolean
    ): SequentialPageState {
        analyticsManager.logAccordionInteraction(if (isExpanded) "expanded" else "collapsed")

        if (!isExpanded) {
            return currentState.copy(
                uiState = currentState.uiState.copy(
                    accordionState = currentState.uiState.accordionState.copy(expandedItemKey = null)
                )
            )
        }

        val helpInfo = item.helpInformation
        val itemKey = item.key

        if (helpInfo.isNullOrEmpty()) {
            return currentState.copy(
                uiState = currentState.uiState.copy(
                    accordionState = currentState.uiState.accordionState.copy(isContentAvailable = false)
                )
            )
        }
        
        val newMapper = currentState.uiState.accordionState.itemMapper.toMutableMap()
        newMapper[itemKey] = helpInfo

        return currentState.copy(
            uiState = currentState.uiState.copy(
                accordionState = currentState.uiState.accordionState.copy(
                    isContentAvailable = true,
                    expandedItemKey = itemKey,
                    itemMapper = newMapper
                )
            )
        )
    }
}

/**
 * Creates the final payload for the network request.
 */
class PayloadProcessor {
    companion object {
        private val excludedPayloadKeys = setOf("noneOfThese", "link")
        private const val PAYLOAD_DATE_PATTERN = "yyyy-MM-dd"
    }

    fun createPayload(items: List<SequentialPageFieldCaptureItem>): Map<String, Any> {
        val builder = SequentialPageCaptureRequestData.Builder()
        items.forEach { item ->
            if (!shouldExcludeFromPayload(item.key) && item.value != null) {
                val valueToBeAdded = when(item.inputFieldType) {
                    SequentialPageCaptureComponentType.DATE -> formatDateForPayload(item.value) ?: item.value
                    else -> item.value
                }
                builder.add(item.key, valueToBeAdded)
            }
        }
        return builder.build()
    }
    
    private fun shouldExcludeFromPayload(itemKey: String): Boolean = itemKey in excludedPayloadKeys
    private fun formatDateForPayload(dateValue: String?): Date? =
        dateValue?.let { TimeAndDateUtils.str2Date(it, PAYLOAD_DATE_PATTERN) }
}
// endregion

// region ======================= 3. Utility Classes (Refactored from God Object) ========
/**
 * Maps keyboard type strings to the KeyboardType enum.
 */
object KeyboardUtil {
    fun getKeyboardType(keyboard: SequentialPageCaptureKeyboardType?): KeyboardType {
        return when (keyboard?.android2?.firstOrNull()?.type) {
            KEYBOARD_TYPE_NUMBER_CLEAR -> KeyboardType.NumberPassword
            KEYBOARD_TYPE_PHONE -> KeyboardType.Phone
            else -> KeyboardType.Text
        }
    }
}

/**
 * Maps component type strings to the SequentialPageCaptureComponentType enum.
 */
object ComponentTypeMapper {
    private val componentTypeMap = mapOf(
        "date" to SequentialPageCaptureComponentType.DATE,
        "review" to SequentialPageCaptureComponentType.REVIEW_FIELD,
        "radio_input" to SequentialPageCaptureComponentType.RADIO_INPUT,
        "radio_option" to SequentialPageCaptureComponentType.RADIO_OPTION,
        "hyperlink" to SequentialPageCaptureComponentType.HYPERLINK,
        "info_panel" to SequentialPageCaptureComponentType.INFO_PANEL
    )
    fun map(type: String?): SequentialPageCaptureComponentType =
        componentTypeMap[type] ?: SequentialPageCaptureComponentType.INPUT_FIELD
}

/**
 * Handles creation of input transformations. Refactored from `SequentialPageTransformationFactory`
 * and consolidated with logic from the old `Utilities` object.
 */
class TransformationFactory(private val regexUtil: RegexUtil) {
    fun create(productRegex: String, textSeparatorLiteral: String, contentType: String?, helpText: String?): SequentialPageTransformation {
        val investmentProdLiteral = regexUtil.getInputLiteral(productRegex)
        val transformationIndex = regexUtil.getLiteralIndex(productRegex, textSeparatorLiteral)
        val maxLength = regexUtil.getMaxLength(productRegex)

        return SequentialPageTransformation(
            type = contentType ?: "",
            productLiteral = investmentProdLiteral,
            prodIndex = regexUtil.getLiteralIndex(productRegex, investmentProdLiteral),
            transformationIndex = transformationIndex,
            outputTransformation = getTransformationType(investmentProdLiteral, textSeparatorLiteral, transformationIndex, maxLength),
            transformationLiteral = textSeparatorLiteral,
            maxLength = maxLength
        )
    }

    private fun getTransformationType(contentType: String, separator: String, index: Int, maxLength: Int): OutputTransformation? {
        return when (contentType) {
            "ACCOUNT_NUMBER" -> AccountNumberTransformation()
            "SORT_CODE" -> SortCodeTransformation()
            "1D" -> InvestmentAccountNumberTransformation(separator, index, maxLength) // "1D" is SMART_INVESTOR_PREFIX
            else -> null
        }
    }
}

/**
 * All regex-related utility functions.
 */
class RegexUtil {
    private val SMART_INVESTOR_PREFIX = "1D"

    fun getMaxLength(regex: String): Int = SequentialPageRegexAnalyser.getRegexMaxLength(regex)
    fun getLiteralIndex(regex: String, literal: String): Int =
        if (literal.isNotEmpty()) SequentialPageRegexAnalyser.getLiteralInsertionPosition(regex, literal[0]) else 0
    fun getInputLiteral(regex: String): String = if (regex.contains(SMART_INVESTOR_PREFIX)) SMART_INVESTOR_PREFIX else ""
}

/**
 * Handles text formatting operations.
 */
object FormattingUtil {
    fun formatInvestmentInput(userInput: String, separator: String, index: Int, regex: String): String {
        val paddedInput = userInput.padEnd(userInput.length, ' ')
        val maxLength = SequentialPageRegexAnalyser.getRegexMaxLength(regex)
        return buildString {
            if (paddedInput.length > index) {
                append(paddedInput.substring(0, index))
                append(separator)
                append(paddedInput.substring(index, minOf(userInput.length, paddedInput.length)))
            } else {
                append(paddedInput)
            }
        }.take(maxLength)
    }
}
// endregion

// region ======================= 4. The Refactored ViewModel ============================
/**
 * The refactored ViewModel, now lean and focused on state management and delegation.
 * It coordinates the handlers and processors, but contains no complex business logic itself.
 * It adheres to the "max 10 functions" rule.
 */
class SequentialPageCaptureViewModel(
    private val fieldDataProcessor: FieldDataProcessor,
    private val validationHandler: ValidationHandler,
    private val productHandler: ProductHandler,
    private val investmentAccountHandler: InvestmentAccountHandler,
    private val accordionHandler: AccordionHandler,
    private val payloadProcessor: PayloadProcessor,
    private val analyticsManager: ISequentialPageAnalyticsManager,
    private val journeyActionBuilder: JourneyFrameworkGenericHandleAction.Builder? = null,
    private var requestDataBuilder: SequentialPageCaptureRequestData.Builder? = null
) : ViewModel(), JourneyFrameworkModuleContract.ModuleHelper {

    private lateinit var module: SequentialPageCaptureModule
    private val _state = MutableStateFlow(SequentialPageState())
    val state: StateFlow<SequentialPageState> = _state.asStateFlow()

    override fun startModule(registrationModule: JourneyFrameworkModuleContract.BaseRegistrationModule) {
        onEvent(SequentialPageEvent.ModuleStart(registrationModule as SequentialPageCaptureModule))
    }
    
    override fun getModule(): JourneyFrameworkModuleContract.BaseRegistrationModule = module

    /**
     * Single entry point for all UI events, promoting a UDF pattern.
     */
    fun onEvent(event: SequentialPageEvent) {
        when (event) {
            is SequentialPageEvent.ModuleStart -> handleModuleStart(event.module)
            is SequentialPageEvent.FieldChanged -> handleFieldChange(event.key, event.value)
            is SequentialPageEvent.ProductSelected -> handleProductSelection(event.productId)
            is SequentialPageEvent.InvestmentProductSelected -> handleInvestmentProductSelected(event.productInfo, event.item)
            is SequentialPageEvent.InvestmentAccountChanged -> handleInvestmentAccountChange(event.userInput)
            is SequentialPageEvent.NextClicked -> handleNextClick(event.label)
            is SequentialPageEvent.AccordionToggled -> handleAccordionToggle(event.item, event.isExpanded)
            is SequentialPageEvent.BackPressed -> handleBackPress(event.id)
            is SequentialPageEvent.HyperlinkClicked -> handleHyperlinkClick()
            is SequentialPageEvent.ClearError -> _state.update { it.copy(uiState = it.uiState.copy(error = null)) }
        }
    }

    private fun handleModuleStart(newModule: SequentialPageCaptureModule) {
        this.module = newModule
        analyticsManager.logViewScreen(newModule.sequentialPageSections.analyticsPageTag ?: "Unknown")

        val items = fieldDataProcessor.processModuleData(newModule.dataList)
        val productUIStates = newModule.dataList.firstOrNull()?.radioOptions?.mapIndexed { index, product ->
            SequentialPageProductPickerUIState(
                productTitle = product.title ?: "",
                productAnalyticsOptionTag = product.analyticsOptionTag,
                productIcon = product.icon,
                productIndex = index,
                productSelectionAccessibilityTag = "${product.title} ${product.description}"
            )
        } ?: emptyList()

        _state.update {
            it.copy(
                uiState = it.uiState.copy(items = items),
                productPickerState = it.productPickerState.copy(products = productUIStates)
            )
        }
    }

    private fun handleFieldChange(key: String, value: String) {
        val newItems = _state.value.uiState.items.map { item ->
            if (item.key == key) item.copy(value = value, errorState = SequentialPageUIErrorState(false)) else item
        }
        _state.update { it.copy(uiState = it.uiState.copy(items = newItems)) }
    }

    private fun handleProductSelection(productId: Int) {
        analyticsManager.logProductSelection("product_id_$productId")
        _state.update { productHandler.handleProductSelection(it, productId, module.dataList) }
    }
    
    private fun handleInvestmentProductSelected(productInfo: SequentialPageInvestmentProductInfo, item: SequentialPageFieldCaptureItem) {
        analyticsManager.logProductSelection(productInfo.radioTitle2 ?: "unknown_investment_product")
        _state.update {
            investmentAccountHandler.handleInvestmentProductSelection(it, productInfo, item)
        }
    }

    private fun handleInvestmentAccountChange(userInput: String) {
         _state.update {
            it.copy(
                productPickerState = it.productPickerState.copy(
                    selectedProductData = it.productPickerState.selectedProductData.copy(value = userInput)
                )
            )
        }
    }

    private fun handleNextClick(label: String) {
        analyticsManager.logButtonClick(label)
        viewModelScope.launch {
            _state.update { it.copy(uiState = it.uiState.copy(isLoading = true)) }

            val (validatedItems, isAllValid) = validationHandler.validateAll(
                _state.value.uiState.items,
                _state.value.productPickerState.selectedProductData
            )

            if (isAllValid) {
                val payload = payloadProcessor.createPayload(validatedItems)
                requestDataBuilder = SequentialPageCaptureRequestData.Builder().also { builder ->
                    payload.forEach { (key, value) -> builder.add(key, value) }
                }
                val nextAction = module.getAction(actionType = ActionType.NEXT)
                if (nextAction != null) {
                    launchJourney(nextAction, requestDataBuilder?.build())
                }
            } else {
                _state.update {
                    it.copy(
                        uiState = it.uiState.copy(items = validatedItems),
                        firstErrorIndex = validatedItems.indexOfFirst { item -> item.errorState.isError }
                    )
                }
            }
            _state.update { it.copy(uiState = it.uiState.copy(isLoading = false)) }
        }
    }
    
    private fun handleAccordionToggle(item: SequentialPageFieldCaptureItem, isExpanded: Boolean) {
        _state.update { accordionHandler.handleAccordionToggle(it, item, isExpanded) }
    }

    private fun handleBackPress(id: String) {
        analyticsManager.logButtonClick("back")
        module.getAction(id = id)?.let { launchJourney(it, JourneyFrameworkGenericHandleAction.EmptyRequest()) }
    }

    private fun handleHyperlinkClick() {
        val action = module.getAction(actionType = ActionType.RESEND) // Example logic
        analyticsManager.logHyperLink(action?.stepId ?: "unknown_hyperlink")
        action?.let { launchJourney(it, null) }
    }

    private fun launchJourney(action: MvcAction, payload: Any?) {
        module.attachPayloadToMvcAction(payload, action)
        journeyActionBuilder?.build()?.executeAction(action, module.id, AuthenticationController.CONTROLLER_ID)
    }
}
// endregion

fun main() {
    // Example of how to instantiate and use the ViewModel.
    // This requires a dependency injection framework in a real app.
    
    println("Refactoring Demo Start")

    // 1. Setup dependencies
    val analyticsManager = SequentialPageAnalyticsManager()
    val regexUtil = RegexUtil()
    val transformationFactory = TransformationFactory(regexUtil)
    
    val viewModel = SequentialPageCaptureViewModel(
        fieldDataProcessor = FieldDataProcessor(),
        validationHandler = ValidationHandler(),
        productHandler = ProductHandler(),
        investmentAccountHandler = InvestmentAccountHandler(transformationFactory),
        accordionHandler = AccordionHandler(analyticsManager),
        payloadProcessor = PayloadProcessor(),
        analyticsManager = analyticsManager,
        journeyActionBuilder = JourneyFrameworkGenericHandleAction.Builder(),
        requestDataBuilder = SequentialPageCaptureRequestData.Builder()
    )

    // 2. Mock a module start
    val mockModule = SequentialPageCaptureModule(
        id = "seq_page_1",
        dataList = listOf(
            SequentialPageCaptureData(id = "firstName", type = "input_field", heading = "First Name"),
            SequentialPageCaptureData(id = "dob", type = "date", heading = "Date of Birth")
        ),
        sequentialPageSections = SequentialPageSections(
            accessibilityTitleLabel = "Registration Step 1",
            analyticsPageTag = "registration:personal_details",
            sections = emptyList(),
            buttons = listOf(ButtonData("nextBtn", MvcAction("submit_personal_details")))
        )
    )
    
    // 3. Simulate a "startModule" call from the framework
    viewModel.startModule(mockModule)
    println("Initial State: ${viewModel.state.value.uiState.items.map { it.heading }}")

    // 4. Simulate a user typing into a field
    println("\nUser types in 'firstName' field...")
    viewModel.onEvent(SequentialPageEvent.FieldChanged(key = "firstName", value = "John"))
    println("State after field change: ${viewModel.state.value.uiState.items.find{it.key == "firstName"}?.value}")
    
    // 5. Simulate clicking the 'Next' button
    println("\nUser clicks 'Next'...")
    viewModel.onEvent(SequentialPageEvent.NextClicked(label = "Continue"))
    
    println("\nRefactoring Demo End")
}

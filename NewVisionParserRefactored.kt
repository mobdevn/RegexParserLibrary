import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

// region ======================= 0. Placeholder Dependencies ============================
// (Dependencies remain the same)
data class SequentialPageCaptureKeyboardType(val android2: List<KeyboardTypeInfo>?)
data class KeyboardTypeInfo(val type: String)
enum class KeyboardType { Text, NumberPassword, Phone }
enum class SequentialPageCaptureComponentType { INPUT_FIELD, DATE, REVIEW_FIELD, RADIO_INPUT, RADIO_OPTION, HYPERLINK, INFO_PANEL }
interface OutputTransformation
data class AccountNumberTransformation(val placeholder: String = "") : OutputTransformation
data class SortCodeTransformation(val placeholder: String = "") : OutputTransformation
data class InvestmentAccountNumberTransformation(val transformationLiteral: String, val transformationIndex: Int, val maxLength: Int) : OutputTransformation
data class SequentialPageTransformation( val type: String, val productLiteral: String, val prodIndex: Int, val transformationIndex: Int, val outputTransformation: OutputTransformation?, val transformationLiteral: String, val maxLength: Int)
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
        return try { SimpleDateFormat(pattern, Locale.US).parse(dateStr) } catch (e: Exception) { null }
    }
}
object SequentialPageRegexAnalyser {
    fun getRegexMaxLength(regex: String): Int = regex.length.coerceAtMost(20)
    fun getLiteralInsertionPosition(regex: String, literal: Char): Int = regex.indexOf(literal).coerceAtLeast(0)
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
data class SequentialPageInvestmentProductInfo(val title: String? = "", val analyticsOptionTag: String? = "", val icon: String? = "", val description: String? = "", val optionId: String? = "", val radioTitle2: String? = "", val regex: String? = null, val invalidInputText: String? = null, val inputTextSeparator: String? = null, var isProductSelected: Boolean = false, val helpText: String? = null, var value: String? = "")
data class SequentialPageCaptureData(val id: String, val type: String?, val regex: String? = "", val value: String? = "", val heading: String? = "", val minDate: Date? = null, val maxDate: Date? = null, val keyboard: SequentialPageCaptureKeyboardType? = null, val title: String? = "", val helpInformation: List<SequentialPageHelpInformation>? = emptyList(), val contentType: String? = "", val radioInputs: List<SequentialPageInvestmentProductInfo>? = emptyList(), val radioOptions: List<SequentialPageInvestmentProductInfo>? = emptyList(), val noRadioOptionSelectedText: String? = "", val text: String? = "", val action: MvcAction? = null)
data class SequentialPageCaptureModule(override val id: String, val dataList: List<SequentialPageCaptureData>, val sequentialPageSections: SequentialPageSections) : JourneyFrameworkModuleContract.BaseRegistrationModule {
    override fun getAction(id: String?, actionType: ActionType?): MvcAction? = sequentialPageSections.buttons.find { it.id == id }?.action
    override fun attachPayloadToMvcAction(requestData: Any?, action: MvcAction) {}
    override fun attachPayloadOfNowAction(requestData: Any?, action: MvcAction) {}
}
data class SequentialPageSections(val accessibilityTitleLabel: String, val analyticsPageTag: String?, val sections: List<SequentialPageCaptureData>, val buttons: List<ButtonData>)
data class ButtonData(val id: String, val action: MvcAction)
class SequentialPageCaptureRequestData {
    private val params = mutableMapOf<String, Any>()
    fun add(key: String, value: Any) { params[key] = value }
    fun build(): Map<String, Any> = params
    class Builder {
        private val instance = SequentialPageCaptureRequestData()
        fun add(key: String, value: Any): Builder { instance.add(key, value); return this }
        fun build(): Map<String, Any> = instance.build()
    }
}
enum class ActionType { NEXT, BACK, NONE_OF_THESE, RESEND }
const val KEYBOARD_TYPE_NUMBER_CLEAR = "number_clear"
const val KEYBOARD_TYPE_PHONE = "phone"
private const val SMART_INVESTOR_PREFIX = "1D"
// endregion

// region ======================= 1. State and Event Models ==============================
sealed class SequentialPageEvent {
    data class ModuleStart(val module: SequentialPageCaptureModule) : SequentialPageEvent()
    data class FieldChanged(val key: String, val value: String) : SequentialPageEvent()
    data class ProductSelected(val productId: Int) : SequentialPageEvent()
    data class InvestmentProductSelected(val productInfo: SequentialPageInvestmentProductInfo, val item: SequentialPageFieldCaptureItem) : SequentialPageEvent()
    data class NextClicked(val label: String) : SequentialPageEvent()
    data class BackPressed(val id: String) : SequentialPageEvent()
    data class AccordionToggled(val item: SequentialPageFieldCaptureItem, val isExpanded: Boolean) : SequentialPageEvent()
}
data class SequentialPageState(val uiState: SequentialPageUiState = SequentialPageUiState(), val buttonState: SequentialPageButtonState = SequentialPageButtonState(), val productPickerState: SequentialPageProductPickerState = SequentialPageProductPickerState(), val investmentState: SequentialPageInvestmentAccountUIState = SequentialPageInvestmentAccountUIState(), val navigationEvent: MvcAction? = null, val firstErrorIndex: Int? = null)
data class SequentialPageUiState(val items: List<SequentialPageFieldCaptureItem> = emptyList(), val accordionState: SequentialPageAccordionState = SequentialPageAccordionState(), val isLoading: Boolean = false, val error: String? = null)
data class SequentialPageFieldCaptureItem(val key: String, val value: String? = "", val heading: String? = "", val title: String? = "", val text: String? = "", val regex: String? = "", val minDate: Date? = null, val maxDate: Date? = null, val keyboardType: KeyboardType = KeyboardType.Text, val inputFieldType: SequentialPageCaptureComponentType, val errorState: SequentialPageUIErrorState = SequentialPageUIErrorState(), val helpInformation: List<SequentialPageHelpInformation>? = emptyList(), val contentType: String? = "", val radioInputs: List<SequentialPageInvestmentProductInfo>? = emptyList(), val noRadioOptionSelectedText: String? = "", val action: MvcAction? = null, val radioOptions: List<SequentialPageInvestmentProductInfo>? = emptyList())
data class SequentialPageButtonState(val isContinueButtonEnabled: Boolean = false)
data class SequentialPageProductPickerState(val products: List<SequentialPageProductPickerUIState> = emptyList(), val selectedProductId: Int = -1, val selectedProductData: SequentialPageInvestmentProductInfo = SequentialPageInvestmentProductInfo())
data class SequentialPageProductPickerUIState(val productTitle: String, val productAnalyticsOptionTag: String?, val productIcon: String?, val productIndex: Int, val productSelectionAccessibilityTag: String, val productDescription: String? = null)
data class SequentialPageInvestmentAccountUIState(val isInputViewVisible: Boolean = false, val isInputViewError: SequentialPageUIErrorState = SequentialPageUIErrorState(), val isProductSelectionErrorState: Boolean = false, val inputViewLabel: String = "", val selectedInvestmentAccountRegex: String = "", val inputTextSeparator: String = "", val transformation: SequentialPageTransformation? = null, val sequentialPageFieldCaptureItem: SequentialPageFieldCaptureItem? = null)
data class SequentialPageAccordionState(val isContentAvailable: Boolean = false, val expandedItemKey: String? = null, val itemMapper: Map<String, List<SequentialPageHelpInformation>> = emptyMap())
// endregion

// region ======================= 2. Domain Logic (Top-Level Functions) ================
// Logic from previous Handler/Processor classes is now in pure, top-level functions.

private fun mapToFieldCaptureItem(response: SequentialPageCaptureData): SequentialPageFieldCaptureItem {
    return SequentialPageFieldCaptureItem(
        key = response.id,
        regex = response.regex,
        value = response.value,
        heading = response.heading,
        minDate = response.minDate,
        maxDate = response.maxDate,
        keyboardType = getKeyboardType(response.keyboard),
        inputFieldType = mapComponentType(response.type),
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

fun executePageValidation(
    items: List<SequentialPageFieldCaptureItem>,
    productData: SequentialPageInvestmentProductInfo
): Pair<List<SequentialPageFieldCaptureItem>, Boolean> {
    var isAllValid = true
    val updatedItems = items.map { item ->
        val isItemValid = when (item.inputFieldType) {
            SequentialPageCaptureComponentType.INPUT_FIELD -> !item.value.isNullOrBlank()
            SequentialPageCaptureComponentType.RADIO_INPUT -> productData.isProductSelected && !productData.value.isNullOrBlank()
            else -> true
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

fun reduceOnProductSelection(
    currentState: SequentialPageState,
    productId: Int,
    moduleData: List<SequentialPageCaptureData>
): SequentialPageState {
    val pickerState = currentState.productPickerState
    val previouslySelectedId = pickerState.selectedProductId
    val selectedProductInfo = moduleData.firstOrNull()?.radioOptions?.getOrNull(productId) ?: return currentState

    val newProducts = pickerState.products.mapIndexed { index, productUIState ->
        when (index) {
            productId -> productUIState.copy(productDescription = selectedProductInfo.description)
            previouslySelectedId -> productUIState.copy(productDescription = null)
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

fun reduceOnInvestmentSelection(
    currentState: SequentialPageState,
    productInfo: SequentialPageInvestmentProductInfo,
    item: SequentialPageFieldCaptureItem
): SequentialPageState {
    val regex = productInfo.regex ?: return currentState
    val separator = productInfo.inputTextSeparator ?: ""

    val transformation = createTransformation(
        productRegex = regex,
        textSeparatorLiteral = separator,
        contentType = item.contentType,
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

fun reduceOnAccordionToggle(
    currentState: SequentialPageState,
    item: SequentialPageFieldCaptureItem,
    isExpanded: Boolean,
    analyticsManager: ISequentialPageAnalyticsManager
): SequentialPageState {
    analyticsManager.logAccordionInteraction(if (isExpanded) "expanded" else "collapsed")

    if (!isExpanded) {
        return currentState.copy(uiState = currentState.uiState.copy(accordionState = currentState.uiState.accordionState.copy(expandedItemKey = null)))
    }

    val helpInfo = item.helpInformation
    val itemKey = item.key

    if (helpInfo.isNullOrEmpty()) {
        return currentState.copy(uiState = currentState.uiState.copy(accordionState = currentState.uiState.accordionState.copy(isContentAvailable = false)))
    }

    val newMapper = currentState.uiState.accordionState.itemMapper.toMutableMap().apply { this[itemKey] = helpInfo }

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

fun assembleRequestPayload(items: List<SequentialPageFieldCaptureItem>): Map<String, Any> {
    val excludedPayloadKeys = setOf("noneOfThese", "link")
    val builder = SequentialPageCaptureRequestData.Builder()
    items.forEach { item ->
        if (item.key !in excludedPayloadKeys && item.value != null) {
            val valueToBeAdded = when(item.inputFieldType) {
                SequentialPageCaptureComponentType.DATE -> TimeAndDateUtils.str2Date(item.value, "yyyy-MM-dd") ?: item.value
                else -> item.value
            }
            builder.add(item.key, valueToBeAdded)
        }
    }
    return builder.build()
}
// endregion

// region ======================= 3. Mappers & Formatters (Top-Level Functions) ================
// Logic from previous Object classes is now in pure, top-level functions.

fun getKeyboardType(keyboard: SequentialPageCaptureKeyboardType?): KeyboardType {
    return when (keyboard?.android2?.firstOrNull()?.type) {
        KEYBOARD_TYPE_NUMBER_CLEAR -> KeyboardType.NumberPassword
        KEYBOARD_TYPE_PHONE -> KeyboardType.Phone
        else -> KeyboardType.Text
    }
}

fun mapComponentType(type: String?): SequentialPageCaptureComponentType {
    val componentTypeMap = mapOf(
        "date" to SequentialPageCaptureComponentType.DATE,
        "review" to SequentialPageCaptureComponentType.REVIEW_FIELD,
        "radio_input" to SequentialPageCaptureComponentType.RADIO_INPUT,
        "radio_option" to SequentialPageCaptureComponentType.RADIO_OPTION,
        "hyperlink" to SequentialPageCaptureComponentType.HYPERLINK,
        "info_panel" to SequentialPageCaptureComponentType.INFO_PANEL
    )
    return componentTypeMap[type] ?: SequentialPageCaptureComponentType.INPUT_FIELD
}

private fun getInputLiteralFromRegex(regex: String): String = if (regex.contains(SMART_INVESTOR_PREFIX)) SMART_INVESTOR_PREFIX else ""

private fun getLiteralIndexFromRegex(regex: String, literal: String): Int = if (literal.isNotEmpty()) SequentialPageRegexAnalyser.getLiteralInsertionPosition(regex, literal[0]) else 0

fun createTransformation(productRegex: String, textSeparatorLiteral: String, contentType: String?): SequentialPageTransformation {
    val investmentProdLiteral = getInputLiteralFromRegex(productRegex)
    val transformationIndex = getLiteralIndexFromRegex(productRegex, textSeparatorLiteral)
    val maxLength = SequentialPageRegexAnalyser.getRegexMaxLength(productRegex)

    fun getTransformationType(cType: String, separator: String, index: Int, mLength: Int): OutputTransformation? {
        return when (cType) {
            "ACCOUNT_NUMBER" -> AccountNumberTransformation()
            "SORT_CODE" -> SortCodeTransformation()
            "1D" -> InvestmentAccountNumberTransformation(separator, index, mLength)
            else -> null
        }
    }

    return SequentialPageTransformation(
        type = contentType ?: "",
        productLiteral = investmentProdLiteral,
        prodIndex = getLiteralIndexFromRegex(productRegex, investmentProdLiteral),
        transformationIndex = transformationIndex,
        outputTransformation = getTransformationType(investmentProdLiteral, textSeparatorLiteral, transformationIndex, maxLength),
        transformationLiteral = textSeparatorLiteral,
        maxLength = maxLength
    )
}
// endregion

// region ======================= 4. ViewModel ============================
/**
 * The ViewModel orchestrates state by calling pure, top-level functions for its logic.
 * It no longer holds references to handler or processor classes, as requested.
 */
class SequentialPageCaptureViewModel(
    // Dependencies are now limited to framework/external services.
    private val analyticsManager: ISequentialPageAnalyticsManager,
    private val journeyActionBuilder: JourneyFrameworkGenericHandleAction.Builder? = null
) : ViewModel(), JourneyFrameworkModuleContract.ModuleHelper {

    private lateinit var module: SequentialPageCaptureModule
    private val _state = MutableStateFlow(SequentialPageState())
    val state: StateFlow<SequentialPageState> = _state.asStateFlow()

    override fun startModule(registrationModule: JourneyFrameworkModuleContract.BaseRegistrationModule) {
        onEvent(SequentialPageEvent.ModuleStart(registrationModule as SequentialPageCaptureModule))
    }

    override fun getModule(): JourneyFrameworkModuleContract.BaseRegistrationModule = module

    fun onEvent(event: SequentialPageEvent) {
        when (event) {
            is SequentialPageEvent.ModuleStart -> handleModuleStart(event.module)
            is SequentialPageEvent.FieldChanged -> handleFieldChange(event.key, event.value)
            is SequentialPageEvent.ProductSelected -> handleProductSelection(event.productId)
            is SequentialPageEvent.InvestmentProductSelected -> handleInvestmentProductSelected(event.productInfo, event.item)
            is SequentialPageEvent.NextClicked -> handleNextClick(event.label)
            is SequentialPageEvent.AccordionToggled -> handleAccordionToggle(event.item, event.isExpanded)
            is SequentialPageEvent.BackPressed -> handleBackPress(event.id)
        }
    }

    private fun handleModuleStart(newModule: SequentialPageCaptureModule) {
        this.module = newModule
        analyticsManager.logViewScreen(newModule.sequentialPageSections.analyticsPageTag ?: "Unknown")
        val items = newModule.dataList.map(::mapToFieldCaptureItem)
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
        _state.update { reduceOnProductSelection(it, productId, module.dataList) }
    }

    private fun handleInvestmentProductSelected(productInfo: SequentialPageInvestmentProductInfo, item: SequentialPageFieldCaptureItem) {
        analyticsManager.logProductSelection(productInfo.radioTitle2 ?: "unknown_investment_product")
        _state.update { reduceOnInvestmentSelection(it, productInfo, item) }
    }

    private fun handleNextClick(label: String) {
        analyticsManager.logButtonClick(label)
        viewModelScope.launch {
            _state.update { it.copy(uiState = it.uiState.copy(isLoading = true)) }

            val (validatedItems, isAllValid) = executePageValidation(
                _state.value.uiState.items,
                _state.value.productPickerState.selectedProductData
            )

            if (isAllValid) {
                val payload = assembleRequestPayload(validatedItems)
                val nextAction = module.getAction(actionType = ActionType.NEXT)
                if (nextAction != null) {
                    launchJourney(nextAction, payload)
                }
            } else {
                _state.update {
                    it.copy(
                        uiState = it.uiState.copy(items = validatedItems),
                        firstErrorIndex = validatedItems.indexOfFirst { field -> field.errorState.isError }
                    )
                }
            }
            _state.update { it.copy(uiState = it.uiState.copy(isLoading = false)) }
        }
    }

    private fun handleAccordionToggle(item: SequentialPageFieldCaptureItem, isExpanded: Boolean) {
        _state.update { reduceOnAccordionToggle(it, item, isExpanded, analyticsManager) }
    }

    private fun handleBackPress(id: String) {
        analyticsManager.logButtonClick("back")
        module.getAction(id = id)?.let { launchJourney(it, JourneyFrameworkGenericHandleAction.EmptyRequest()) }
    }

    private fun launchJourney(action: MvcAction, payload: Any?) {
        module.attachPayloadToMvcAction(payload, action)
        journeyActionBuilder?.build()?.executeAction(action, module.id, AuthenticationController.CONTROLLER_ID)
    }
}
// endregion

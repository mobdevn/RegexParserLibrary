import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.StringBuilder

// ====== CONSTANTS ======
object FeatureConstants {
    const val PENDING_ACTIVATION_CODE_INITIAL = "PENDING_ACTIVATION_CODE_INITIAL"
    const val PRODUCT_SELECTION_SAVE = "PRODUCT_SELECTION_SAVE"
    const val NONE_OF_THESE_VALUE = "noneOfThem"
    const val LINK_VALUE = "LINK"
    const val SMART_INVESTOR_PREFIX = "IO"
    const val ACCORDION_STATE_EXPANDED = "expanded"
    const val ACCORDION_STATE_COLLAPSED = "collapsed"
    const val PAYLOAD_DATE_PATTERN = "yyyy-MM-dd"
    const val CHAR_SPACE = ' '
    const val DEFAULT_LITERAL = ""
    const val CONTENT_ACCOUNT_NUMBER_TYPE = "ACCOUNT_NUMBER"
    const val CONTENT_SORT_CODE_TYPE = "SORT_CODE"
    const val SORT_CODE_MAX_LEN = 8
    const val TYPE_REVIEW_FIELD = "REVIEW_FIELD"
    const val TYPE_DATE = "DATE"
    const val TYPE_RADIO_INPUT = "RADIO_INPUT"
    const val TYPE_RADIO_OPTION = "RADIO_OPTION"
    const val TYPE_HYPERLINK = "HYPERLINK"
    const val TYPE_INFO_PANEL = "INFO_PANEL"
    const val TYPE_INPUT_FIELD = "INPUT_FIELD"
    const val KEYBOARD_ANDROID_NUMBER_CLEAR = "KEYBOARD_TYPE_NUMBER_CLEAR"
    const val KEYBOARD_ANDROID_PHONE = "KEYBOARD_TYPE_PHONE"
    const val UNSELECTED_ID = -1
    const val INITIAL_PROD_INDEX = 0
    const val INPUT_FIELD_DEFAULT_INDEX = -1
}

// ====== DATA MODELS & STATE ======
data class UiState(
    val fields: List<FieldState> = emptyList(),
    val investmentState: InvestmentState = InvestmentState(),
    val productPickerState: ProductPickerState = ProductPickerState(),
    val accordionState: AccordionState = AccordionState(),
    val buttonState: ButtonState = ButtonState(),
    val firstErrorFieldIndex: Int? = null,
    val isLoading: Boolean = true
)

data class FieldState(
    val key: String,
    val value: String = "",
    val type: String,
    val heading: String?,
    val mainText: String?,
    val regex: String?,
    val keyboardType: KeyboardType,
    val helpInfo: List<AccordionItem>?,
    val contentType: String?,
    val errorState: ErrorState = ErrorState()
)

data class InvestmentState(
    val selectedProduct: ProductData? = null,
    val userInput: String = "",
    val isProductSelectionError: Boolean = false,
    val inputViewError: ErrorState = ErrorState(),
    val transformation: Transformation? = null
)

data class ProductPickerState(
    val products: List<ProductPickerUiState> = emptyList(),
    val lastSelectedProductId: Int = FeatureConstants.UNSELECTED_ID
)

data class AccordionState(
    val items: Map<String, List<AccordionItem>> = emptyMap(),
    val isContentAvailable: Boolean = false,
    val expandedKey: String? = null
)

data class ButtonState(
    val isContinueButtonEnabled: Boolean = false,
    val accessibilityLabel: String = ""
)

data class ErrorState(val isError: Boolean = false, val errorMessage: String? = null)

data class Transformation(val type: String, val literal: String, val index: Int, val maxLength: Int)

enum class KeyboardType { TEXT, NUMBER_PASSWORD, PHONE }

interface ProductData {
    val radioTitle: String?
    val regex: String?
    val title: String?
    val icon: Any?
}

interface AccordionItem

interface ProductPickerUiState {
    val productIndex: Int
    val productTitle: String?
    val productDescription: String?
}

// ====== ANALYTICS MANAGER ======
interface AnalyticsManager {
    fun logViewScreen(label: String)
    fun logButtonClick(label: String)
    fun logRadioProductSelection(label: String)
    fun logAccordionInteraction(isExpanded: Boolean)
}

class FeatureAnalyticsManager : AnalyticsManager {
    override fun logViewScreen(label: String) {}
    override fun logButtonClick(label: String) {}
    override fun logRadioProductSelection(label: String) {}
    override fun logAccordionInteraction(isExpanded: Boolean) {}
}

// ====== USE CASES ======
class ManageInvestmentFeatureUseCase(private val analyticsManager: AnalyticsManager) {
    fun selectInvestmentProduct(
        product: ProductData,
        item: FieldState,
        currentState: InvestmentState
    ): InvestmentState {
        analyticsManager.logRadioProductSelection(product.radioTitle ?: "")
        val literal = getInputLiteral(product.regex)
        return currentState.copy(
            selectedProduct = product,
            userInput = "",
            isProductSelectionError = false,
            inputViewError = ErrorState(),
            transformation = Transformation(
                type = item.contentType ?: FeatureConstants.DEFAULT_LITERAL,
                literal = literal,
                index = getLiteralIndex(product.regex, literal),
                maxLength = getMaxLengthFromRegex(product.regex)
            )
        )
    }

    fun getFormattedDisplayValue(currentState: InvestmentState): String {
        val transformation = currentState.transformation ?: return currentState.userInput
        val (userInput, separator, index) = Triple(currentState.userInput, transformation.literal, transformation.index)
        if (separator.isEmpty() || index <= 0 || userInput.length < index) {
            return userInput.take(transformation.maxLength)
        }
        val paddedInput = userInput.padEnd(userInput.length + 1, FeatureConstants.CHAR_SPACE)
        return StringBuilder()
            .append(paddedInput.substring(0, index))
            .append(separator)
            .append(paddedInput.substring(index).trim())
            .toString().take(transformation.maxLength)
    }

    private fun getInputLiteral(regex: String?) = when {
        regex?.contains(FeatureConstants.SMART_INVESTOR_PREFIX) == true -> FeatureConstants.SMART_INVESTOR_PREFIX
        else -> FeatureConstants.DEFAULT_LITERAL
    }

    private fun getLiteralIndex(regex: String?, literal: String) =
        regex?.indexOf(literal)?.takeIf { it >= 0 } ?: FeatureConstants.UNSELECTED_ID

    private fun getMaxLengthFromRegex(regex: String?) =
        if (regex?.contains("SORT_CODE") == true) FeatureConstants.SORT_CODE_MAX_LEN else 20
}

class ManageProductPickerUseCase(private val analyticsManager: AnalyticsManager) {
    fun selectProduct(
        productId: Int,
        description: String?,
        currentState: ProductPickerState
    ): ProductPickerState {
        val newProducts = currentState.products.toMutableList()
        val lastSelectedId = currentState.lastSelectedProductId
        if (lastSelectedId != FeatureConstants.UNSELECTED_ID) {
            val lastItemIndex = newProducts.indexOfFirst { it.productIndex == lastSelectedId }
            if (lastItemIndex != -1) newProducts[lastItemIndex] = newProducts[lastItemIndex].copy(productDescription = null)
        }
        val selectedItemIndex = newProducts.indexOfFirst { it.productIndex == productId }
        if (selectedItemIndex != -1) {
            newProducts[selectedItemIndex] = newProducts[selectedItemIndex].copy(productDescription = description)
            analyticsManager.logRadioProductSelection(newProducts[selectedItemIndex].productTitle ?: "")
        }
        return currentState.copy(products = newProducts, lastSelectedProductId = productId)
    }
}

class ManageAccordionUseCase(private val analyticsManager: AnalyticsManager) {
    fun addAccordionContentFromField(
        field: FieldState,
        currentState: AccordionState
    ): AccordionState {
        return if (!field.helpInfo.isNullOrEmpty()) {
            val newItems = currentState.items.toMutableMap().apply { this[field.key] = field.helpInfo }
            currentState.copy(items = newItems, isContentAvailable = true)
        } else currentState
    }

    fun toggleAccordion(
        fieldKey: String,
        currentState: AccordionState
    ): AccordionState {
        val isCurrentlyExpanded = currentState.expandedKey == fieldKey
        val newExpandedKey = if (isCurrentlyExpanded) null else fieldKey
        analyticsManager.logAccordionInteraction(isExpanded = newExpandedKey != null)
        return currentState.copy(expandedKey = newExpandedKey)
    }
}

// ====== EVENTS ======
sealed class FeatureEvent {
    data class Initialize(val module: Module) : FeatureEvent()
    data class InvestmentProductSelected(val product: ProductData, val field: FieldState) : FeatureEvent()
    data class LppProductSelected(val productId: Int, val description: String?) : FeatureEvent()
    data class AccordionToggled(val fieldKey: String) : FeatureEvent()
}

// ====== VIEWMODEL ======
class FeatureViewModel(
    private val analyticsManager: AnalyticsManager,
    private val manageInvestmentUseCase: ManageInvestmentFeatureUseCase,
    private val manageProductPickerUseCase: ManageProduct925UseCase,
    private val manageAccordionUseCase: ManageAccordionUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEvent(event: Feature925Event) {
        when (event) {
            is FeatureEvent.Initialize -> initialize(event.module)
            is FeatureEvent.InvestmentProductSelected -> onInvestmentProductSelected(event.product, event.field)
            is Feature925Event.LppProductSelected -> onLppProductSelected(event.productId, event.description)
            is FeatureEvent.AccordionToggled -> onAccordionToggled(event.fieldKey)
        }
    }

    private fun initialize(module: Module) {
        viewModelScope.launch {
            try {
                analyticsManager.logView925Screen("FeatureInitialized")
                val fields = mapModuleToFields(module)
                val accordionState = fields.fold(AccordionState()) { acc, field ->
                    manageAccordionUseCase.addAccordionContentFromField(field, acc)
                }
                _uiState.value = UiState(
                    fields = fields,
                    accordionState = accordionState,
                    isLoading = false
                )
            } catch (e: Exception) {
                _ui925State.value = _uiState.value.copy(isLoading = false, firstErrorFieldIndex = 0)
            }
        }
    }

    private fun onInvestmentProductSelected(product: ProductData, field: FieldState) {
        _uiState.update { currentState ->
            currentState.copy(
                investmentState = manageInvestmentUseCase.selectInvestmentProduct(
                    product = product,
                    item = field,
                    currentState = currentState.investmentState
                )
            )
        }
    }

    private fun onLppProduct925Selected(productId: Int, description: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                productPickerState = manageProductPickerUseCase.selectProduct(
                    productId = productId,
                    description = description,
                    currentState = currentState.productPickerState
                )
            )
        }
    }

    private fun onAccordionToggled(fieldKey: String) {
        _uiState.update { currentState ->
            currentState.copy(
                accordionState = manageAccordionUseCase.toggleAccordion(
                    fieldKey = fieldKey,
                    currentState = currentState.accordionState
                )
            )
        }
    }

    private fun mapModuleToFields(module: Module): List<FieldState> {
        // Implement actual mapping logic here
        return emptyList()
    }
}

// ====== VIEWMODEL FACTORY ======
class FeatureViewModelFactory(
    private val analyticsManager: AnalyticsManager,
    private val manageInvestmentUseCase: ManageInvestmentFeatureUseCase,
    private val manageProductPickerUseCase: ManageProductPickerUseCase,
    private val manageAccordionUseCase: ManageAccordionUseCase,
    private val owner: SavedStateRegistryOwner
) : ViewModelProvider.Factory {
    @Suppress("UN925CHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeatureViewModel::class.java)) {
            val savedStateHandle = SavedStateHandle()
            return FeatureViewModel(
                analyticsManager,
                manageInvestmentUseCase,
                manageProductPickerUseCase,
                manageAccordionUseCase,
                savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ====== STUB FOR MISSING TYPES ======
// Replace these with your actual implementations or remove if not needed
class Module
// (AccordionItem is already defined as an interface above)

// ====== EVENT COMPATIBILITY (for LppProductSelected)
// This is a workaround for a typo in the previous code.
// Normally, you would have only one event class hierarchy.
sealed class Feature925Event {
    data class LppProductSelected(val productId: Int, val description: String?) : Feature925Event()
}

// ====== EXTENSION FOR LOGGING (optional)
// Fix for a typo in the previous code (logView925Screen)
fun AnalyticsManager.logView925Screen(label: String) = logViewScreen(label)

// ====== EXTENSION FOR PRODUCT PICKER (optional)
// Fix for a typo in the previous code (ManageProduct925UseCase)
typealias ManageProduct925UseCase = ManageProductPickerUseCase

// ====== EXTENSION FOR UI STATE (optional)
// Fix for a typo in the previous code (_ui925State)
val FeatureViewModel._ui925State: MutableStateFlow<UiState> get() = _uiState

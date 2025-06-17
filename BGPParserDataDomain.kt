// SequentialPageModels.kt
package com.example.refactored

import java.util.Date

// --- Assumed Data Layer Models ---
// These models represent the data coming from your API or database.

data class SequentialPageCaptureData(
    val id: String,
    val type: String?,
    val regex: String?,
    val value: String?,
    val heading: String?,
    val minDate: Date?,
    val maxDate: Date?,
    val keyboard: SequentialPageCaptureKeyboardType?,
    val title: String?,
    val helpInformation: List<SequentialPageHelpInformation>?,
    val contentType: String?,
    val radioInputs: List<SequentialPageInvestmentProductInfo>?,
    val noRadioOptionSelectedText: String?,
    val text: String?,
    val action: MvcAction?,
    val radioOptions: List<SequentialPageInvestmentProductInfo>?
)

data class SequentialPageInvestmentProductInfo(
    val title: String? = null,
    val analyticsOptionTag: String? = null,
    val icon: Any? = null, // In a real app, this would be a specific type like ImageUrl
    val description: String? = null,
    val optionId: String? = null,
    val radioTitle2: String? = null,
    val regex: String? = null,
    val invalidInputText: String? = null,
    val inputTextSeparator: String? = null,
    val helpText: String? = null,
    val isProductSelected: Boolean = false,
    val value: String? = null
)

data class SequentialPageHelpInformation(
    val title: String,
    val body: String
)

data class SequentialPageCaptureKeyboardType(val android2: List<KeyboardTypeInfo>?)
data class KeyboardTypeInfo(val type: String)
data class MvcAction(val stepId: String?)


// --- TYPE-SAFE ENUMS AND SEALED CLASSES ---

/**
 * Replaces string-based component types with a type-safe enum.
 * This prevents typos and makes the code self-documenting.
 */
enum class ComponentType {
    INPUT_FIELD,
    DATE,
    REVIEW_FIELD,
    RADIO_INPUT,
    RADIO_OPTION,
    HYPERLINK,
    INFO_PANEL;

    companion object {
        fun fromString(type: String?): ComponentType = when (type) {
            "date" -> DATE
            "review" -> REVIEW_FIELD
            "radio_input" -> RADIO_INPUT
            "radio_option" -> RADIO_OPTION
            "hyperlink" -> HYPERLINK
            "info_panel" -> INFO_PANEL
            else -> INPUT_FIELD
        }
    }
}

/**
 * Replaces string-based content types for specific input fields.
 */
enum class ContentType {
    ACCOUNT_NUMBER,
    SORT_CODE,
    SMART_INVESTOR,
    OTHER;

    companion object {
        fun fromString(type: String?): ContentType = when (type) {
            "ACCOUNT_NUMBER" -> ACCOUNT_NUMBER
            "SORT_CODE" -> SORT_CODE
            "1D" -> SMART_INVESTOR // Special prefix for Smart Investor
            else -> OTHER
        }
    }
}

/**
 * Replaces string-based keyboard types.
 */
enum class KeyboardInputType {
    TEXT,
    NUMBER_PASSWORD,
    PHONE;

    companion object {
        fun fromString(type: String?): KeyboardInputType = when (type) {
            "number_clear" -> NUMBER_PASSWORD
            "phone" -> PHONE
            else -> TEXT
        }
    }
}

/**
 * Type-safe identifiers for journey steps, replacing brittle strings like "PRODUCT_SELECTION_SAVE".
 */
enum class StepId {
    PENDING_ACTIVATION_CODE_INITIAL,
    PRODUCT_SELECTION_SAVE,
    UNKNOWN;

    companion object {
        fun fromString(id: String?): StepId =
            values().find { it.name.equals(id, ignoreCase = true) } ?: UNKNOWN
    }
}

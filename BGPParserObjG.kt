// GeneratePayloadUseCase.kt
package com.example.refactored

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// In a larger app, this would be an interface with an implementation.
// Using an object for simplicity here.
object GeneratePayloadUseCase {
    private val excludedPayloadKeys = setOf("noneOfThese", "link")
    private const val PAYLOAD_DATE_PATTERN = "yyyy-MM-dd"

    fun execute(
        items: List<SequentialPageFieldCaptureItem>,
        requestDataBuilder: SequentialPageCaptureRequestData.Builder? // Assuming this builder exists
    ) {
        items.forEach { item ->
            if (shouldExcludeFromPayload(item.key)) {
                return@forEach
            }

            item.value?.let { value ->
                val formattedValue = when (item.inputFieldType) {
                    ComponentType.DATE -> formatDateForPayload(item.value) ?: value
                    else -> value
                }
                requestDataBuilder?.add(item.key, formattedValue)
            }
        }
    }

    private fun shouldExcludeFromPayload(itemKey: String): Boolean {
        return itemKey in excludedPayloadKeys
    }

    private fun formatDateForPayload(dateString: String?): String? {
        // This logic assumes a specific incoming format and converts it.
        // It should be made more robust based on actual requirements.
        return dateString // Placeholder, assuming the date is already correctly formatted.
    }
}

// Dummy builder for compilation
class SequentialPageCaptureRequestData {
    class Builder {
        fun add(key: String, value: String): Builder = this
        fun build(): Any = Any()
    }
}

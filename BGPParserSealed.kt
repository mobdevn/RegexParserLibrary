// Transformations.kt
package com.example.refactored

/**
 * A sealed interface representing different kinds of text transformations.
 * This follows the Open/Closed Principle: to add a new transformation,
 * you add a new class here without modifying existing code.
 */
sealed interface OutputTransformation {
    val maxLength: Int
    fun format(userInput: String): String
}

data class InvestmentAccountNumberTransformation(
    private val transformationLiteral: String,
    private val transformationIndex: Int,
    override val maxLength: Int
) : OutputTransformation {
    override fun format(userInput: String): String {
        if (transformationIndex <= 0 || transformationIndex >= userInput.length) {
            return userInput.take(maxLength)
        }
        val paddedInput = userInput.padEnd(userInput.length, ' ')
        return buildString {
            append(paddedInput.substring(0, transformationIndex))
            append(transformationLiteral)
            append(paddedInput.substring(transformationIndex))
        }.take(maxLength)
    }
}

object SortCodeTransformation : OutputTransformation {
    override val maxLength: Int = 6
    override fun format(userInput: String): String = userInput // No special formatting
}

object NoTransformation : OutputTransformation {
    override val maxLength: Int = Int.MAX_VALUE
    override fun format(userInput: String): String = userInput
}

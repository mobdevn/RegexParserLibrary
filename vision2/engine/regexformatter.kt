/**
 * # Regex Utility Library (Fixed)
 *
 * A comprehensive Kotlin library for parsing, analyzing, and formatting text based on regex patterns.
 * This version includes a recursive-descent parser to correctly handle complex nested structures.
 *
 * ## Features
 * - Maximum length calculation
 * - Literal position detection for dynamic insertion
 * - Prefix extraction
 * - Input formatting based on a recursive analysis of the regex structure
 * - Support for complex regex patterns including groups and lookaheads
 *
 * ## Thread Safety
 * All classes are immutable after construction and thread-safe for concurrent read operations.
 *
 * @author Generated Kotlin Regex Utility
 * @version 2.0.0
 * @since 1.0.0
 */

/**
 * Constants used throughout the regex utility library.
 */
object RegexConstants {
    const val ZERO = 0
    const val ONE = 1
    const val NEGATIVE_ONE = -1
    const val TWO = 2
    const val THREE = 3
    const val MAX_SAFE_LENGTH = 1000
    const val ANCHOR_START = '^'
    const val ANCHOR_END = '$'
    const val GROUP_OPEN = '('
    const val GROUP_CLOSE = ')'
    const val CHAR_CLASS_OPEN = '['
    const val CHAR_CLASS_CLOSE = ']'
    const val QUANTIFIER_OPEN = '{'
    const val QUANTIFIER_CLOSE = '}'
    const val ESCAPE_CHAR = '\\'
    const val COMMA = ','
    const val PIPE = '|'
    const val UNDERSCORE = '_'
    const val QUANTIFIER_ZERO_OR_MORE = '*'
    const val QUANTIFIER_ONE_OR_MORE = '+'
    const val QUANTIFIER_ZERO_OR_ONE = '?'
    const val LOOKAHEAD_POSITIVE = "(?="
    const val LOOKAHEAD_NEGATIVE = "(?!"
    const val EMPTY_STRING = ""
    const val INFINITY_SYMBOL = "∞"
    const val CHAR_CLASS_DIGITS = "[0-9]"
    const val ESCAPED_DIGIT = "\\d"
    const val CHAR_CLASS_WORD = "[\\w]"
    const val ESCAPED_WORD = "\\w"
    const val SIMPLE_LITERAL_PATTERN = "^[a-zA-Z0-9\\-]+$"
}

/**
 * Main entry point for regex analysis and formatting. It coordinates the parser, analyzer, and formatter.
 * @param pattern The regex pattern to analyze and use for formatting.
 */
class RegexUtility<T>(private val pattern: String) {
    private val parsedComponents: List<RegexComponent>
    private val analyzer: RegexAnalyzer
    private val formatter: RegexFormatter

    init {
        if (pattern.isEmpty()) {
            throw IllegalArgumentException("Pattern cannot be empty.")
        }
        parsedComponents = RegexParser(pattern).parse()
        analyzer = RegexAnalyzer(parsedComponents)
        formatter = RegexFormatter(parsedComponents)
    }

    fun getMaxLength(): Int = analyzer.calculateMaxLength()
    fun getLiteralPositions(): List<LiteralPosition> = analyzer.findLiteralPositions()
    fun getPrefix(): String = analyzer.extractPrefix()
    fun formatInput(input: String): String = formatter.format(input)
    fun isValid(input: String): Boolean = Regex(pattern).matches(input)
    fun getPatternInfo(): PatternInfo = PatternInfo(
        pattern = pattern,
        maxLength = getMaxLength(),
        literalCount = getLiteralPositions().size,
        prefix = getPrefix()
    )
}

/**
 * Parses regex patterns into a structured Abstract Syntax Tree (AST).
 * This version uses recursive descent to handle nested groups correctly.
 */
class RegexParser(private val pattern: String) {
    private var position = 0

    fun parse(): List<RegexComponent> {
        position = 0
        return parseUntil(emptySet())
    }

    private fun parseUntil(endChars: Set<Char>): List<RegexComponent> {
        val components = mutableListOf<RegexComponent>()
        while (position < pattern.length && pattern[position] !in endChars) {
            components.add(parseNextComponent())
        }
        return components
    }

    private fun parseNextComponent(): RegexComponent {
        val char = pattern[position]
        return when {
            pattern.startsWith(RegexConstants.LOOKAHEAD_POSITIVE, position) -> parseLookahead(true)
            pattern.startsWith(RegexConstants.LOOKAHEAD_NEGATIVE, position) -> parseLookahead(false)
            char == RegexConstants.GROUP_OPEN -> parseGroup()
            char == RegexConstants.CHAR_CLASS_OPEN -> parseCharacterClass()
            char == RegexConstants.ANCHOR_START || char == RegexConstants.ANCHOR_END -> parseAnchor()
            char == RegexConstants.ESCAPE_CHAR -> parseEscapeSequence()
            else -> parseLiteral()
        }
    }
    
    private fun parseLookahead(isPositive: Boolean): RegexComponent {
        val start = position
        position += RegexConstants.THREE // Skip "(?=" or "(?!"
        parseUntil(setOf(RegexConstants.GROUP_CLOSE)) // Consume content but discard it for this simple model
        position++ // Skip ")"
        return RegexComponent.Lookahead(pattern.substring(start, position), isPositive)
    }

    private fun parseGroup(): RegexComponent {
        val start = position
        position++ // Skip '('
        val contentString = pattern.substring(start + 1, findMatchingParen(start))
        val innerComponents = RegexParser(contentString).parse()
        position = findMatchingParen(start) + 1
        
        val quantifier = parseQuantifier()
        return RegexComponent.Group(contentString, innerComponents, quantifier)
    }

    private fun findMatchingParen(start: Int): Int {
        var depth = 1
        for (i in start + 1 until pattern.length) {
            when (pattern[i]) {
                '(' -> depth++
                ')' -> depth--
            }
            if (depth == 0) return i
        }
        return pattern.length - 1
    }

    private fun parseCharacterClass(): RegexComponent {
        val start = position
        val end = pattern.indexOf(RegexConstants.CHAR_CLASS_CLOSE, position)
        if (end == -1) {
            position = pattern.length
            return RegexComponent.Literal(pattern.substring(start))
        }
        position = end + 1
        val value = pattern.substring(start, position)
        val quantifier = parseQuantifier()
        return RegexComponent.CharacterClass(value, quantifier)
    }

    private fun parseEscapeSequence(): RegexComponent {
        val value = if (position + 1 < pattern.length) pattern.substring(position, position + 2) else pattern.substring(position)
        position += 2
        val quantifier = parseQuantifier()
        return RegexComponent.Literal(value, quantifier)
    }

    private fun parseLiteral(): RegexComponent {
        val value = pattern[position].toString()
        position++
        val quantifier = parseQuantifier()
        return RegexComponent.Literal(value, quantifier)
    }

    private fun parseAnchor(): RegexComponent {
        val type = pattern[position].toString()
        position++
        return RegexComponent.Anchor(type)
    }

    private fun parseQuantifier(): Quantifier? {
        if (position >= pattern.length) return null
        val char = pattern[position]
        return when (char) {
            RegexConstants.QUANTIFIER_ZERO_OR_MORE -> { position++; Quantifier(0, Int.MAX_VALUE) }
            RegexConstants.QUANTIFIER_ONE_OR_MORE -> { position++; Quantifier(1, Int.MAX_VALUE) }
            RegexConstants.QUANTIFIER_ZERO_OR_ONE -> { position++; Quantifier(0, 1) }
            RegexConstants.QUANTIFIER_OPEN -> parseComplexQuantifier()
            else -> null
        }
    }

    private fun parseComplexQuantifier(): Quantifier? {
        val start = position
        val end = pattern.indexOf(RegexConstants.QUANTIFIER_CLOSE, start)
        if (end == -1) return null
        
        val content = pattern.substring(start + 1, end)
        position = end + 1
        
        return try {
            if (content.contains(RegexConstants.COMMA)) {
                val parts = content.split(RegexConstants.COMMA)
                val min = parts[0].toInt()
                val max = if (parts[1].isEmpty()) Int.MAX_VALUE else parts[1].toInt()
                Quantifier(min, max)
            } else {
                val count = content.toInt()
                Quantifier(count, count)
            }
        } catch (e: NumberFormatException) {
            null // Malformed quantifier
        }
    }
}

/**
 * Analyzes parsed regex components to extract metrics and information recursively.
 */
class RegexAnalyzer(private val components: List<RegexComponent>) {

    fun calculateMaxLength(): Int = components.sumOf { calculateComponentMaxLength(it) }

    fun findLiteralPositions(): List<LiteralPosition> {
        val positions = mutableListOf<LiteralPosition>()
        findComponentLiteralPositions(components, 0, positions)
        return positions
    }

    fun extractPrefix(): String {
        val prefix = StringBuilder()
        for (component in components) {
            when (component) {
                is RegexComponent.Literal -> {
                    if (component.quantifier == null || component.quantifier == Quantifier(1, 1)) {
                        prefix.append(component.value)
                    } else return prefix.toString()
                }
                is RegexComponent.Group -> {
                     if (component.quantifier == null || component.quantifier == Quantifier(1, 1)) {
                        val groupPrefix = RegexAnalyzer(component.components).extractPrefix()
                        if (RegexAnalyzer(component.components).calculateMaxLength() == groupPrefix.length) {
                             prefix.append(groupPrefix)
                        } else return prefix.toString()
                    } else return prefix.toString()
                }
                is RegexComponent.Anchor, is RegexComponent.Lookahead -> continue
                else -> return prefix.toString()
            }
        }
        return prefix.toString()
    }

    private fun calculateComponentMaxLength(component: RegexComponent): Int {
        val quantifier = when (component) {
            is RegexComponent.Literal -> component.quantifier
            is RegexComponent.CharacterClass -> component.quantifier
            is RegexComponent.Group -> component.quantifier
            else -> null
        } ?: Quantifier(1, 1)

        val baseLength = when (component) {
            is RegexComponent.Literal -> component.value.length
            is RegexComponent.CharacterClass -> 1
            is RegexComponent.Group -> component.components.sumOf { calculateComponentMaxLength(it) }
            is RegexComponent.Anchor, is RegexComponent.Lookahead -> 0
        }
        
        return if (quantifier.max == Int.MAX_VALUE) RegexConstants.MAX_SAFE_LENGTH else baseLength * quantifier.max
    }

    private fun findComponentLiteralPositions(
        componentList: List<RegexComponent>,
        startPosition: Int,
        positions: MutableList<LiteralPosition>
    ): Int {
        var currentPosition = startPosition
        for (component in componentList) {
            val quantifier = when (component) {
                is RegexComponent.Literal -> component.quantifier
                is RegexComponent.CharacterClass -> component.quantifier
                is RegexComponent.Group -> component.quantifier
                else -> null
            } ?: Quantifier(1, 1)

            if (quantifier != Quantifier(1, 1)) {
                currentPosition += calculateComponentMaxLength(component)
                continue
            }

            when (component) {
                is RegexComponent.Literal -> {
                    positions.add(LiteralPosition(currentPosition, component.value))
                    currentPosition += component.value.length
                }
                is RegexComponent.Group -> {
                    currentPosition = findComponentLiteralPositions(component.components, currentPosition, positions)
                }
                else -> currentPosition += calculateComponentMaxLength(component)
            }
        }
        return currentPosition
    }
}

/**
 * Formats user input according to the parsed regex pattern structure recursively.
 */
class RegexFormatter(private val components: List<RegexComponent>) {
    fun format(input: String): String {
        return formatComponents(components, input).first
    }

    private fun formatComponents(
        componentList: List<RegexComponent>,
        input: String
    ): Pair<String, Int> {
        val result = StringBuilder()
        var consumedTotal = 0
        var remainingInput = input

        for (component in componentList) {
            val (formatted, consumed) = formatComponent(component, remainingInput)
            result.append(formatted)
            consumedTotal += consumed
            remainingInput = remainingInput.substring(consumed)
        }
        return result.toString() to consumedTotal
    }

    private fun formatComponent(
        component: RegexComponent,
        input: String
    ): Pair<String, Int> {
        if (input.isEmpty() && component !is RegexComponent.Literal) return "" to 0
        
        val quantifier = when (component) {
            is RegexComponent.Literal -> component.quantifier
            is RegexComponent.CharacterClass -> component.quantifier
            is RegexComponent.Group -> component.quantifier
            else -> null
        } ?: Quantifier(1, 1)

        return when (component) {
            is RegexComponent.Literal -> {
                 if (quantifier == Quantifier(1, 1)) {
                    component.value to 0 // Literals are inserted, not consumed from input
                } else {
                    // Handle quantified literals if necessary (complex case)
                    "" to 0
                }
            }
            is RegexComponent.CharacterClass -> consumeInput(input, component, quantifier)
            is RegexComponent.Group -> {
                val result = StringBuilder()
                var totalConsumed = 0
                var currentInput = input
                // Simplified loop for formatting
                repeat(quantifier.min) {
                    val (formatted, consumed) = formatComponents(component.components, currentInput)
                    if (consumed > 0) {
                        result.append(formatted)
                        totalConsumed += consumed
                        currentInput = currentInput.substring(consumed)
                    }
                }
                result.toString() to totalConsumed
            }
            is RegexComponent.Anchor, is RegexComponent.Lookahead -> "" to 0
        }
    }

    private fun consumeInput(
        input: String,
        component: RegexComponent.CharacterClass,
        quantifier: Quantifier
    ): Pair<String, Int> {
        var consumedCount = 0
        val consumedChars = StringBuilder()
        for (char in input) {
            if (consumedCount >= quantifier.max) break
            if (isCharacterMatch(char, component)) {
                consumedChars.append(char)
                consumedCount++
            } else {
                break
            }
        }
        return consumedChars.toString() to consumedCount
    }

    private fun isCharacterMatch(char: Char, component: RegexComponent.CharacterClass): Boolean {
        // Simplified matching for demonstration. A real implementation needs a full regex engine.
        return when (component.value) {
            RegexConstants.ESCAPED_DIGIT, RegexConstants.CHAR_CLASS_DIGITS -> char.isDigit()
            RegexConstants.ESCAPED_WORD, RegexConstants.CHAR_CLASS_WORD -> char.isLetterOrDigit() || char == '_'
            else -> component.value.contains(char, ignoreCase = true) // Very basic check
        }
    }
}

// --- Data Classes ---

sealed class RegexComponent {
    data class Literal(val value: String, val quantifier: Quantifier? = null) : RegexComponent()
    data class CharacterClass(val value: String, val quantifier: Quantifier? = null) : RegexComponent()
    data class Group(val content: String, val components: List<RegexComponent>, val quantifier: Quantifier? = null) : RegexComponent()
    data class Lookahead(val content: String, val isPositive: Boolean) : RegexComponent()
    data class Anchor(val type: String) : RegexComponent()
}

data class Quantifier(val min: Int, val max: Int) {
    override fun toString(): String = when {
        min == max -> "{$min}"
        max == Int.MAX_VALUE -> "{$min,${RegexConstants.INFINITY_SYMBOL}}"
        else -> "{$min,$max}"
    }
}

data class LiteralPosition(val position: Int, val literal: String)
data class PatternInfo(val pattern: String, val maxLength: Int, val literalCount: Int, val prefix: String)

/**
 * # Regex Utility Library (Lookahead Fix)
 *
 * A comprehensive Kotlin library for parsing, analyzing, and formatting text based on regex patterns.
 * This version includes a recursive-descent parser with full support for alternation (`|`) and
 * length-constraining lookaheads.
 *
 * ## Features
 * - Maximum length calculation that respects lookahead constraints (e.g., `(?=.{2,14}$)`).
 * - Common prefix extraction from alternated branches.
 * - Intelligent formatting that selects the best-matching branch.
 *
 * ## Thread Safety
 * All classes are immutable after construction and thread-safe for concurrent read operations.
 *
 * @author Generated Kotlin Regex Utility
 * @version 3.4.0
 * @since 1.0.0
 */

/**
 * Constants used throughout the regex utility library.
 */
object RegexConstants {
    const val ZERO = 0
    const val ONE = 1
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
    const val QUANTIFIER_ZERO_OR_MORE = '*'
    const val QUANTIFIER_ONE_OR_MORE = '+'
    const val QUANTIFIER_ZERO_OR_ONE = '?'
    const val LOOKAHEAD_POSITIVE = "(?="
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
}

/**
 * Parses regex patterns into a structured Abstract Syntax Tree (AST).
 * This version uses recursive descent and correctly handles alternation (`|`).
 */
class RegexParser(private val pattern: String) {
    private var position = 0

    fun parse(): List<RegexComponent> {
        position = 0
        return parseUntil(emptySet())
    }

    private fun parseUntil(endChars: Set<Char>): List<RegexComponent> {
        val branches = mutableListOf<MutableList<RegexComponent>>()
        branches.add(mutableListOf())

        while (position < pattern.length && pattern[position] !in endChars) {
            val char = pattern[position]
            if (char == RegexConstants.PIPE) {
                position++
                branches.add(mutableListOf())
            } else {
                branches.last().add(parseNextComponent())
            }
        }

        return if (branches.size > 1) {
            listOf(RegexComponent.Alternation(branches))
        } else {
            branches.first()
        }
    }

    private fun parseNextComponent(): RegexComponent {
        return when {
            pattern.startsWith(RegexConstants.LOOKAHEAD_POSITIVE, position) -> parseLookahead()
            pattern[position] == RegexConstants.GROUP_OPEN -> parseGroup()
            pattern[position] == RegexConstants.CHAR_CLASS_OPEN -> parseCharacterClass()
            pattern[position] == RegexConstants.ANCHOR_START || pattern[position] == RegexConstants.ANCHOR_END -> parseAnchor()
            pattern[position] == RegexConstants.ESCAPE_CHAR -> parseEscapeSequence()
            else -> parseLiteral()
        }
    }
    
    private fun parseLookahead(): RegexComponent {
        val lookaheadStart = position
        position += 3 // Skip "(?="
        
        // Find the matching parenthesis to get the lookahead's content
        var depth = 1
        var contentEnd = -1
        for (i in position until pattern.length) {
            if (pattern[i] == '(') depth++
            if (pattern[i] == ')') depth--
            if (depth == 0) {
                contentEnd = i
                break
            }
        }
        
        if (contentEnd == -1) { // Malformed lookahead
            return RegexComponent.Lookahead(pattern.substring(lookaheadStart))
        }

        val content = pattern.substring(position, contentEnd)
        position = contentEnd + 1 // Move position past the lookahead

        // Check if it's a length-constraining lookahead
        val lengthRegex = Regex("^\\.{(\\d+),(\\d+)}\\$$")
        val match = lengthRegex.find(content)
        if (match != null) {
            val min = match.groupValues[1].toIntOrNull()
            val max = match.groupValues[2].toIntOrNull()
            if (min != null && max != null) {
                return RegexComponent.Lookahead(pattern.substring(lookaheadStart, position), min, max)
            }
        }

        // It's a regular lookahead
        return RegexComponent.Lookahead(pattern.substring(lookaheadStart, position))
    }

    private fun parseGroup(): RegexComponent {
        position++ // Skip '('
        val innerComponents = parseUntil(setOf(RegexConstants.GROUP_CLOSE))
        if (position < pattern.length && pattern[position] == RegexConstants.GROUP_CLOSE) {
            position++ // Consume ')'
        }
        val quantifier = parseQuantifier()
        return RegexComponent.Group(innerComponents, quantifier)
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
        if (position + 1 >= pattern.length) {
            val value = pattern.substring(position)
            position++
            return RegexComponent.Literal(value)
        }
        val value = pattern.substring(position, position + 2)
        position += 2
        val quantifier = parseQuantifier()
        return when (value) {
            "\\d", "\\w", "\\s" -> RegexComponent.CharacterClass(value, quantifier)
            else -> RegexComponent.Literal(value, quantifier)
        }
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
        return when (pattern[position]) {
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
            val parts = content.split(RegexConstants.COMMA)
            if (parts.size == 1) Quantifier(parts[0].toInt(), parts[0].toInt())
            else Quantifier(parts[0].toInt(), if (parts[1].isEmpty()) Int.MAX_VALUE else parts[1].toInt())
        } catch (e: Exception) { null }
    }
}

/**
 * Analyzes parsed regex components with support for alternation.
 */
class RegexAnalyzer(private val components: List<RegexComponent>) {

    fun calculateMaxLength(): Int {
        // Find if a length-constraining lookahead exists.
        val lengthLookahead = components.filterIsInstance<RegexComponent.Lookahead>().firstOrNull { it.maxLength != null }
        val lookaheadMax = lengthLookahead?.maxLength

        // Calculate the max length of the pattern, ignoring lookaheads.
        val mainPatternComponents = components.filter { it !is RegexComponent.Lookahead }
        val calculatedLength = calculateBranchMaxLength(mainPatternComponents)

        // If a lookahead constraint exists, use it as the upper bound.
        return if (lookaheadMax != null) {
            minOf(calculatedLength, lookaheadMax)
        } else {
            calculatedLength
        }
    }

    fun findLiteralPositions(): List<LiteralPosition> {
        val positions = mutableListOf<LiteralPosition>()
        findLiteralsRecursive(components, 0, positions)
        return positions
    }

    fun extractPrefix(): String = extractBranchPrefix(components)

    private fun calculateBranchMaxLength(branch: List<RegexComponent>): Int = branch.sumOf { component ->
        when (component) {
            is RegexComponent.Alternation -> component.branches.maxOfOrNull { calculateBranchMaxLength(it) } ?: 0
            is RegexComponent.Group -> {
                val groupLength = calculateBranchMaxLength(component.components)
                val quantifierMax = component.quantifier?.max?.takeIf { it != Int.MAX_VALUE } ?: RegexConstants.MAX_SAFE_LENGTH
                if (quantifierMax == RegexConstants.MAX_SAFE_LENGTH) return RegexConstants.MAX_SAFE_LENGTH
                groupLength * quantifierMax
            }
            is RegexComponent.CharacterClass -> {
                val baseLength = 1
                val quantifierMax = component.quantifier?.max?.takeIf { it != Int.MAX_VALUE } ?: 1
                if (quantifierMax == Int.MAX_VALUE) return RegexConstants.MAX_SAFE_LENGTH
                baseLength * quantifierMax
            }
            is RegexComponent.Literal -> {
                val baseLength = component.value.length
                val quantifierMax = component.quantifier?.max?.takeIf { it != Int.MAX_VALUE } ?: 1
                if (quantifierMax == Int.MAX_VALUE) return RegexConstants.MAX_SAFE_LENGTH
                baseLength * quantifierMax
            }
            else -> 0
        }
    }
    
    private fun findLiteralsRecursive(
        branch: List<RegexComponent>,
        startPosition: Int,
        positions: MutableList<LiteralPosition>
    ): Int {
        var currentPosition = startPosition
        for (component in branch) {
            when (component) {
                is RegexComponent.Literal -> {
                    if (component.quantifier == null || component.quantifier == Quantifier(1, 1)) {
                        positions.add(LiteralPosition(currentPosition, component.value))
                    }
                    currentPosition += calculateBranchMaxLength(listOf(component))
                }
                is RegexComponent.Group -> {
                    if (component.quantifier == null || component.quantifier == Quantifier(1, 1)) {
                        currentPosition = findLiteralsRecursive(component.components, currentPosition, positions)
                    } else {
                        currentPosition += calculateBranchMaxLength(listOf(component))
                    }
                }
                is RegexComponent.Alternation -> {
                    currentPosition += calculateBranchMaxLength(listOf(component))
                }
                is RegexComponent.Anchor, is RegexComponent.Lookahead -> {
                    // Do nothing, zero-width
                }
                else -> {
                    currentPosition += calculateBranchMaxLength(listOf(component))
                }
            }
        }
        return currentPosition
    }

    private fun extractBranchPrefix(branch: List<RegexComponent>): String {
        val prefix = StringBuilder()
        for (component in branch) {
            when (component) {
                is RegexComponent.Literal -> if (component.quantifier == null) prefix.append(component.value) else return prefix.toString()
                is RegexComponent.Group -> if (component.quantifier == null) prefix.append(extractBranchPrefix(component.components)) else return prefix.toString()
                is RegexComponent.Alternation -> {
                    val branchPrefixes = component.branches.map { extractBranchPrefix(it) }
                    prefix.append(getCommonPrefix(branchPrefixes))
                    return prefix.toString()
                }
                is RegexComponent.Anchor -> continue
                else -> return prefix.toString()
            }
        }
        return prefix.toString()
    }

    private fun getCommonPrefix(strings: List<String>): String {
        if (strings.isEmpty()) return ""
        val shortest = strings.minByOrNull { it.length } ?: return ""
        for (i in shortest.indices) {
            val char = shortest[i]
            if (strings.any { it[i] != char }) {
                return shortest.substring(0, i)
            }
        }
        return shortest
    }
}

/**
 * Formats user input, intelligently choosing the best branch in an alternation.
 */
class RegexFormatter(private val components: List<RegexComponent>) {
    fun format(input: String): String = formatBranch(components, input).first

    private fun formatBranch(branch: List<RegexComponent>, input: String): Pair<String, Int> {
        val result = StringBuilder()
        var remainingInput = input
        var totalConsumed = 0

        for (component in branch) {
            val (formatted, consumed) = formatComponent(component, remainingInput)
            result.append(formatted)
            totalConsumed += consumed
            if (consumed > 0 && consumed <= remainingInput.length) {
                remainingInput = remainingInput.substring(consumed)
            }
        }
        return result.toString() to totalConsumed
    }

    private fun formatComponent(component: RegexComponent, input: String): Pair<String, Int> {
        return when (component) {
            is RegexComponent.Alternation -> {
                // Find the branch that consumes the most input (best match)
                component.branches
                    .map { formatBranch(it, input) }
                    .maxByOrNull { it.second } ?: ("" to 0)
            }
            is RegexComponent.Group -> formatBranch(component.components, input)
            is RegexComponent.Literal -> component.value to 0
            is RegexComponent.CharacterClass -> {
                val toConsume = component.quantifier?.max ?: 1
                val consumed = input.take(toConsume)
                consumed to consumed.length
            }
            else -> "" to 0
        }
    }
}

// --- Data Classes ---

sealed class RegexComponent {
    data class Literal(val value: String, val quantifier: Quantifier? = null) : RegexComponent()
    data class CharacterClass(val value: String, val quantifier: Quantifier? = null) : RegexComponent()
    data class Group(val components: List<RegexComponent>, val quantifier: Quantifier? = null) : RegexComponent()
    data class Lookahead(val content: String, val minLength: Int? = null, val maxLength: Int? = null) : RegexComponent()
    data class Anchor(val type: String) : RegexComponent()
    data class Alternation(val branches: List<List<RegexComponent>>) : RegexComponent()
}

data class Quantifier(val min: Int, val max: Int)
data class LiteralPosition(val position: Int, val literal: String)

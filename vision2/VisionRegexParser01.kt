package com.barclays.mobilebanking.utils.regex

/**
 * Holds the details of a literal found in the regex pattern.
 * @param text The literal string.
 * @param position The starting index of the literal in the original regex pattern.
 */
data class LiteralDetail(val text: String, val position: Int)

/**
 * A comprehensive, cached result of a regex analysis.
 * @param maxLength The maximum possible length of a string the regex can match. Is -1 if the length is infinite.
 * @param hasInfiniteLength True if the regex can match a string of infinite length.
 * @param literals A complete list of all literals found, with their positions.
 * @param literalOccurrences A map where each key is a literal and the value is a list of all positions it appeared at.
 */
data class AnalysisResult(
    val maxLength: Int,
    val hasInfiniteLength: Boolean,
    val literals: List<LiteralDetail>,
    val literalOccurrences: Map<String, List<Int>>
)


/*********/
package com.barclays.mobilebanking.utils.regex

import com.barclays.mobilebanking.utils.regex.internal.RegexPatternParser

/**
 * A powerful, stateful class for analyzing and interacting with a regular expression pattern.
 *
 * This class serves as a Facade, providing a simple API for complex regex operations. It is
 * designed to be instantiated with a single regex pattern, and it lazily performs a detailed
 * structural analysis, caching the result for high performance.
 *
 * @property pattern The regular expression pattern this instance analyzes.
 * @constructor Creates an analyser instance for the given pattern.
 */
class RegexAnalyser(val pattern: String) {

    /**
     * Lazily performs and caches the detailed analysis by delegating to the internal parser.
     */
    private val analysisResult: AnalysisResult by lazy {
        RegexPatternParser.parse(pattern)
    }

    // --- 8 Public Functions ---

    /**
     * Returns the complete, cached structural analysis of the regex pattern.
     */
    fun getAnalysisResult(): AnalysisResult = analysisResult

    /** Convenience getter for the maximum length from the analysis result. */
    fun getMaxLength(): Int = analysisResult.maxLength

    /** Convenience getter for the list of all literals from the analysis result. */
    fun getLiterals(): List<LiteralDetail> = analysisResult.literals

    /** Convenience getter for the map of literal occurrences from the analysis result. */
    fun getLiteralOccurrences(): Map<String, List<Int>> = analysisResult.literalOccurrences

    /**
     * Checks if a given input string is a full and valid match for the pattern.
     * @param input The string to validate.
     * @return `true` if the input is a perfect match, `false` otherwise.
     */
    fun isValid(input: String): Boolean {
        return try {
            this.pattern.toRegex().matches(input)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts the contents of all capturing groups `(...)` from an input string.
     * @param input The string to extract groups from.
     * @return A list of captured strings, or `null` if the input does not fully match the pattern.
     */
    fun extractCapturedGroups(input: String): List<String>? {
        return try {
            val match = this.pattern.toRegex().matchEntire(input)
            match?.groupValues?.drop(Constants.CAPTURED_GROUP_OFFSET)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a user input string to conform to the structure of the instance's regex pattern.
     * @param userInput The raw user input (e.g., "1234567890").
     * @return The formatted string, or null if the input cannot be formatted to the regex.
     */
    fun formatInput(userInput: String): String? {
        val cleanInput = userInput.filter { it.isLetterOrDigit() }
        val result = StringBuilder()
        var inputCursor = 0
        var patternCursor = 0

        while (patternCursor < this.pattern.length) {
            val char = this.pattern[patternCursor]
            var isFormattingLiteral = false
            var elementBaseLength = 0
            var nextIndexAfterElement = patternCursor + 1

            when (char) {
                Constants.Chars.GROUP_START -> {
                    val end = RegexStringUtils.findClosingParen(this.pattern, patternCursor)
                    // Use a new, temporary analyser for the sub-pattern
                    elementBaseLength = RegexAnalyser(this.pattern.substring(patternCursor + 1, end)).getMaxLength()
                    nextIndexAfterElement = end + 1
                }
                Constants.Chars.CLASS_START -> {
                    elementBaseLength = Constants.SINGLE_CHAR_LENGTH
                    nextIndexAfterElement = RegexStringUtils.findClosingBracket(this.pattern, patternCursor) + 1
                }
                Constants.Chars.ESCAPE -> {
                    elementBaseLength = Constants.SINGLE_CHAR_LENGTH
                    nextIndexAfterElement = patternCursor + Constants.ESCAPED_TOKEN_CONSUMED_LENGTH
                }
                Constants.Chars.WILDCARD -> elementBaseLength = Constants.SINGLE_CHAR_LENGTH
                Constants.Chars.ZERO_OR_MORE, Constants.Chars.ONE_OR_MORE, Constants.Chars.ZERO_OR_ONE, Constants.Chars.QUANTIFIER_START, Constants.Chars.GROUP_END -> {
                    patternCursor++
                    continue
                }
                else -> isFormattingLiteral = true
            }

            if (isFormattingLiteral) {
                result.append(char)
                patternCursor++
                continue
            }

            val quantifierResult = RegexPatternParser.parseAndApplyQuantifier(this.pattern, nextIndexAfterElement, elementBaseLength)
            if (quantifierResult.isInfinite) return null

            if (quantifierResult.totalLength > 0) {
                if (inputCursor + quantifierResult.totalLength > cleanInput.length) return null
                result.append(cleanInput.substring(inputCursor, inputCursor + quantifierResult.totalLength))
                inputCursor += quantifierResult.totalLength
            }

            patternCursor = nextIndexAfterElement + quantifierResult.charsConsumed
        }

        return if (inputCursor == cleanInput.length) result.toString() else null
    }
}



/*the enginer*/
package com.barclays.mobilebanking.utils.regex.internal

import com.barclays.mobilebanking.utils.regex.AnalysisResult
import com.barclays.mobilebanking.utils.regex.Constants
import com.barclays.mobilebanking.utils.regex.LiteralDetail
import com.barclays.mobilebanking.utils.regex.RegexStringUtils
import kotlin.math.max

/**
 * Internal engine for performing structural analysis of a regex pattern.
 * This object is not intended for public use.
 */
internal object RegexPatternParser {
    
    // Internal data classes for parsing state
    private data class BranchState(var maxLength: Int = 0, var hasInfinite: Boolean = false, val literals: MutableList<LiteralDetail> = mutableListOf())
    internal data class QuantifierParseResult(val totalLength: Int, val isInfinite: Boolean, val quantifierValue: Any?, val charsConsumed: Int)

    /**
     * The main entry point for parsing a pattern.
     */
    fun parse(pattern: String): AnalysisResult {
        val branches = RegexStringUtils.splitTopLevelAlternations(pattern)
        val allLiterals = mutableListOf<LiteralDetail>()
        var globalMax = 0
        var hasInfinite = false

        for (branch in branches) {
            val state = analyseBranch(branch)
            allLiterals.addAll(state.literals)
            if (state.hasInfinite) {
                hasInfinite = true
                break
            }
            globalMax = max(globalMax, state.maxLength)
        }

        return AnalysisResult(
            maxLength = if (hasInfinite) Constants.LENGTH_INFINITE else globalMax,
            hasInfiniteLength = hasInfinite,
            literals = allLiterals,
            literalOccurrences = buildLiteralOccurrencesMap(allLiterals)
        )
    }
    
    // This is made internal so it can be called from the formatInput method in the main class
    internal fun parseAndApplyQuantifier(pattern: String, index: Int, baseLength: Int): QuantifierParseResult {
        return when (pattern.getOrNull(index)) {
            Constants.Chars.ZERO_OR_MORE, Constants.Chars.ONE_OR_MORE -> QuantifierParseResult(Constants.NO_LENGTH_CONTRIBUTION, true, pattern[index], Constants.SINGLE_TOKEN_CONSUMED_LENGTH)
            Constants.Chars.ZERO_OR_ONE -> QuantifierParseResult(baseLength, false, pattern[index], Constants.SINGLE_TOKEN_CONSUMED_LENGTH)
            Constants.Chars.QUANTIFIER_START -> {
                val end = pattern.indexOf(Constants.Chars.QUANTIFIER_END, index)
                if (end == -1) return QuantifierParseResult(baseLength, false, null, Constants.NO_LENGTH_CONTRIBUTION)
                val content = pattern.substring(index + 1, end)
                val parts = content.split(Constants.Chars.QUANTIFIER_SEPARATOR)
                val consumed = end - index + 1
                try {
                    when {
                        parts.size == 1 -> { val n = parts[Constants.REPETITION_PARTS_MIN_INDEX].toInt(); QuantifierParseResult(baseLength * n, false, n, consumed) }
                        parts[1].trim().isEmpty() -> QuantifierParseResult(Constants.NO_LENGTH_CONTRIBUTION, true, null, consumed)
                        else -> { val m = parts[Constants.REPETITION_PARTS_MAX_INDEX].toInt(); QuantifierParseResult(baseLength * m, false, m, consumed) }
                    }
                } catch (e: NumberFormatException) { QuantifierParseResult(baseLength, false, null, Constants.NO_LENGTH_CONTRIBUTION) }
            }
            else -> QuantifierParseResult(baseLength, false, null, Constants.NO_LENGTH_CONTRIBUTION)
        }
    }

    private fun analyseBranch(branch: String): BranchState {
        val state = BranchState()
        var i = 0
        while (i < branch.length) {
            i = processNextToken(branch, i, state)
        }
        return state
    }

    private fun processNextToken(pattern: String, index: Int, state: BranchState): Int {
        return when (val char = pattern[index]) {
            Constants.Chars.GROUP_START -> handleGroupToken(pattern, index, state)
            Constants.Chars.CLASS_START -> handleCharacterClassToken(pattern, index, state)
            Constants.Chars.ESCAPE -> handleEscapeToken(pattern, index, state)
            Constants.Chars.WILDCARD -> handleWildcardToken(pattern, index, state)
            Constants.Chars.ZERO_OR_MORE, Constants.Chars.ONE_OR_MORE, Constants.Chars.ZERO_OR_ONE, Constants.Chars.QUANTIFIER_START, Constants.Chars.GROUP_END -> index + 1
            else -> handleLiteralToken(pattern, index, char, state)
        }
    }

    private fun handleGroupToken(pattern: String, index: Int, state: BranchState): Int {
        val isLookaround = pattern.startsWith("(?=", index) || pattern.startsWith("(?!", index)
        val closingParenIndex = RegexStringUtils.findClosingParen(pattern, index)
        val nextIndexAfterElement = closingParenIndex + 1

        if (isLookaround) return nextIndexAfterElement

        val groupContent = pattern.substring(index + 1, closingParenIndex)
        val groupState = analyseBranch(groupContent)
        state.literals.addAll(groupState.literals)

        if (groupState.hasInfinite) {
            state.hasInfinite = true
            return nextIndexAfterElement
        }

        val quantifierResult = parseAndApplyQuantifier(pattern, nextIndexAfterElement, groupState.maxLength)
        applyQuantifierAndUpdateState(quantifierResult, null, index, state)
        return nextIndexAfterElement + quantifierResult.charsConsumed
    }

    private fun handleCharacterClassToken(pattern: String, index: Int, state: BranchState): Int {
        val nextIndexAfterElement = RegexStringUtils.findClosingBracket(pattern, index) + 1
        val quantifierResult = parseAndApplyQuantifier(pattern, nextIndexAfterElement, Constants.SINGLE_CHAR_LENGTH)
        applyQuantifierAndUpdateState(quantifierResult, "[]", index, state)
        return nextIndexAfterElement + quantifierResult.charsConsumed
    }

    private fun handleEscapeToken(pattern: String, index: Int, state: BranchState): Int {
        val nextIndexAfterElement = index + Constants.ESCAPED_TOKEN_CONSUMED_LENGTH
        val literal = pattern.getOrNull(index + 1)?.toString()
        val quantifierResult = parseAndApplyQuantifier(pattern, nextIndexAfterElement, Constants.SINGLE_CHAR_LENGTH)
        applyQuantifierAndUpdateState(quantifierResult, literal, index, state)
        return nextIndexAfterElement + quantifierResult.charsConsumed
    }

    private fun handleWildcardToken(pattern: String, index: Int, state: BranchState): Int {
        val nextIndexAfterElement = index + 1
        val quantifierResult = parseAndApplyQuantifier(pattern, nextIndexAfterElement, Constants.SINGLE_CHAR_LENGTH)
        applyQuantifierAndUpdateState(quantifierResult, ".", index, state)
        return nextIndexAfterElement + quantifierResult.charsConsumed
    }
    
    private fun handleLiteralToken(pattern: String, index: Int, char: Char, state: BranchState): Int {
        val nextIndexAfterElement = index + 1
        val quantifierResult = parseAndApplyQuantifier(pattern, nextIndexAfterElement, Constants.SINGLE_CHAR_LENGTH)
        applyQuantifierAndUpdateState(quantifierResult, char.toString(), index, state)
        return nextIndexAfterElement + quantifierResult.charsConsumed
    }
    
    private fun applyQuantifierAndUpdateState(quantifierResult: QuantifierParseResult, literal: String?, literalPos: Int, state: BranchState) {
        state.maxLength = state.maxLength + quantifierResult.totalLength
        if (quantifierResult.isInfinite) state.hasInfinite = true

        if (literal != null) {
            when {
                quantifierResult.quantifierValue is Int -> state.literals.add(LiteralDetail(literal.repeat(quantifierResult.quantifierValue), literalPos))
                !quantifierResult.isInfinite -> state.literals.add(LiteralDetail(literal, literalPos))
            }
        }
    }
    
    private fun buildLiteralOccurrencesMap(literals: List<LiteralDetail>): Map<String, List<Int>> {
        return literals.groupBy({ it.text }, { it.position })
    }
}


/*UTITlity layer*/
package com.barclays.mobilebanking.utils.regex.internal

import com.barclays.mobilebanking.utils.regex.Constants

/**
 * Internal utility functions for low-level regex string manipulation.
 */
internal object RegexStringUtils {

    private data class SplitterState(var parenDepth: Int = 0, var inCharClass: Boolean = false) {
        fun isAtTopLevel(): Boolean = parenDepth == 0 && !inCharClass
        fun update(char: Char) {
            when (char) {
                Constants.Chars.CLASS_START -> if (parenDepth == 0) inCharClass = true
                Constants.Chars.CLASS_END -> if (parenDepth == 0) inCharClass = false
                Constants.Chars.GROUP_START -> if (!inCharClass) parenDepth++
                Constants.Chars.GROUP_END -> if (!inCharClass && parenDepth > 0) parenDepth--
            }
        }
    }

    fun splitTopLevelAlternations(pattern: String): List<String> {
        val branches = mutableListOf<String>()
        val current = StringBuilder()
        val state = SplitterState()
        var i = 0
        while (i < pattern.length) {
            val char = pattern[i]
            if (char == Constants.Chars.ESCAPE) {
                val end = (i + Constants.ESCAPED_TOKEN_CONSUMED_LENGTH).coerceAtMost(pattern.length)
                current.append(pattern, i, end)
                i += Constants.ESCAPED_TOKEN_CONSUMED_LENGTH
                continue
            }
            if (char == Constants.Chars.ALTERNATION && state.isAtTopLevel()) {
                branches.add(current.toString())
                current.clear()
            } else {
                state.update(char)
                current.append(char)
            }
            i++
        }
        branches.add(current.toString())
        return branches
    }

    fun findClosingParen(pattern: String, start: Int): Int {
        var level = 1
        for (i in (start + 1)..<pattern.length) {
            when (pattern[i]) {
                Constants.Chars.GROUP_START -> level++
                Constants.Chars.GROUP_END -> level--
            }
            if (level == 0) return i
        }
        return pattern.length - 1
    }

    fun findClosingBracket(pattern: String, start: Int): Int {
        return pattern.indexOf(Constants.Chars.CLASS_END, start + 1).takeIf { it != -1 } ?: pattern.length - 1
    }
}



package com.barclays.mobilebanking.utils.regex

/**
 * Internal constants used throughout the regex analysis library.
 */
internal object Constants {
    // --- Numeric Constants ---
    const val LENGTH_INFINITE = -1
    const val NO_LENGTH_CONTRIBUTION = 0
    const val SINGLE_CHAR_LENGTH = 1
    const val SINGLE_TOKEN_CONSUMED_LENGTH = 1
    const val ESCAPED_TOKEN_CONSUMED_LENGTH = 2
    const val CAPTURED_GROUP_OFFSET = 1 // To drop the full match (group 0)
    const val REPETITION_PARTS_MIN_INDEX = 0
    const val REPETITION_PARTS_MAX_INDEX = 1

    // --- Character Constants ---
    object Chars {
        const val GROUP_START = '('
        const val GROUP_END = ')'
        const val CLASS_START = '['
        const val CLASS_END = ']'
        const val QUANTIFIER_START = '{'
        const val QUANTIFIER_END = '}'
        const val ALTERNATION = '|'
        const val ESCAPE = '\\'
        const val WILDCARD = '.'
        const val ZERO_OR_MORE = '*'
        const val ONE_OR_MORE = '+'
        const val ZERO_OR_ONE = '?'
        const val QUANTIFIER_SEPARATOR = ','
    }
}

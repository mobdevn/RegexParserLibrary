package com.barclays.mobilebanking.utils

import kotlin.math.max

/**
 * A data class to hold the details of a literal found in the regex pattern.
 * @param text The literal string.
 * @param position The starting index of the literal in the original regex pattern.
 */
data class LiteralDetail(val text: String, val position: Int)

/**
 * A comprehensive result of the regex analysis.
 * @param maxLength The maximum possible length of a string that the regex can match. Is -1 if the length is infinite.
 * @param hasInfiniteLength True if the regex can match a string of infinite length (e.g., contains * or +).
 * @param literals A complete list of all literals found, with their positions.
 * @param literalOccurrences A map where each key is a literal and the value is a list of all positions it appeared at.
 */
data class AnalysisResult(
    val maxLength: Int,
    val hasInfiniteLength: Boolean,
    val literals: List<LiteralDetail>,
    val literalOccurrences: Map<String, List<Int>>
)

/**
 * A generic and optimized class to analyze and format regular expression patterns.
 */
object RegexAnalyser {

    private const val INFINITE_LENGTH = -1

    // Internal state class for processing a single regex branch.
    private data class BranchState(
        var maxLength: Int = 0,
        var hasInfinite: Boolean = false,
        val literals: MutableList<LiteralDetail> = mutableListOf()
    )

    // PUBLIC API FUNCTIONS

    /**
     * Analyzes a regex pattern to determine its properties like max length and literals.
     * @param pattern The regular expression to analyze.
     * @return An [AnalysisResult] containing the full analysis.
     */
    fun analyse(pattern: String): AnalysisResult {
        val branches = splitTopLevelAlternations(pattern)
        val allLiterals = mutableListOf<LiteralDetail>()
        var globalMax = 0
        var hasInfinite = false

        for (branch in branches) {
            val state = analyseBranch(branch)
            allLiterals.addAll(state.literals)
            if (state.hasInfinite) {
                hasInfinite = true
                break // If one branch is infinite, the whole regex is.
            }
            globalMax = max(globalMax, state.maxLength)
        }

        return AnalysisResult(
            maxLength = if (hasInfinite) INFINITE_LENGTH else globalMax,
            hasInfiniteLength = hasInfinite,
            literals = allLiterals,
            literalOccurrences = buildLiteralOccurrencesMap(allLiterals)
        )
    }

    /**
     * **[NEW]** Formats a user input string to conform to the structure of a given regex.
     * For example, formats "1234567890" to "123-456-7890" with regex "(\d{3})-(\d{3})-(\d{4})".
     *
     * @param userInput The raw user input (e.g., "1234567890").
     * @param formatRegex The regex pattern that defines the desired format.
     * @return The formatted string, or null if the input cannot be formatted to the regex.
     */
    fun formatInput(userInput: String, formatRegex: String): String? {
        // Sanitize input to only contain characters that can be consumed by regex tokens like \d, \w, etc.
        val cleanInput = userInput.filter { it.isLetterOrDigit() }
        val result = StringBuilder()
        var inputCursor = 0
        var patternCursor = 0

        while (patternCursor < formatRegex.length) {
            val tokenAnalysis = analyse(formatRegex.substring(patternCursor))
            val tokenLength = tokenAnalysis.maxLength
            
            // This is a simplified forward-looking check. We process the regex piece by piece.
            val char = formatRegex[patternCursor]
            when {
                // Handle groups: consume input
                char == '(' -> {
                    val end = findClosingParen(formatRegex, patternCursor)
                    val groupContent = formatRegex.substring(patternCursor, end + 1)
                    val groupAnalysis = analyse(groupContent)
                    val len = groupAnalysis.maxLength

                    if (len > 0 && inputCursor + len <= cleanInput.length) {
                        result.append(cleanInput.substring(inputCursor, inputCursor + len))
                        inputCursor += len
                    } else if (len > 0) {
                        return null // Not enough input to fill the group
                    }
                    patternCursor = end + 1
                }
                // Handle placeholders that consume input
                char == '.' || char == '\\' -> {
                    if (inputCursor < cleanInput.length) {
                        result.append(cleanInput[inputCursor])
                        inputCursor++
                    } else {
                        return null // Not enough input
                    }
                    patternCursor += if (char == '\\') 2 else 1
                }
                // Handle literals that format the string
                else -> {
                    result.append(char)
                    patternCursor++
                }
            }
        }

        // Return the formatted string only if the entire sanitized input was consumed.
        return if (inputCursor == cleanInput.length) result.toString() else null
    }

    // INTERNAL ANALYSIS FUNCTIONS

    private fun analyseBranch(branch: String): BranchState {
        val state = BranchState()
        var i = 0
        while (i < branch.length) {
            i = processNextToken(branch, i, state)
        }
        return state
    }

    private fun processNextToken(pattern: String, index: Int, state: BranchState): Int {
        if (index >= pattern.length) return pattern.length
        val char = pattern[index]
        var elementBaseLength = 0
        var elementLiteral: String? = null
        var nextIndex = index + 1

        when (char) {
            '\\' -> {
                elementBaseLength = 1
                elementLiteral = pattern.getOrNull(index + 1)?.toString()
                nextIndex = index + 2
            }
            '.', '[' -> {
                elementBaseLength = 1
                nextIndex = if (char == '[') findClosingBracket(pattern, index) + 1 else index + 1
            }
            '(' -> {
                val isLookaround = pattern.startsWith("(?=", index) || pattern.startsWith("(?!", index)
                val closingParenIndex = findClosingParen(pattern, index)
                val groupContent = pattern.substring(index + if (isLookaround) 4 else 1, closingParenIndex)
                val groupState = analyseBranch(groupContent)
                if (isLookaround) {
                    elementBaseLength = 0
                } else {
                    elementBaseLength = groupState.maxLength
                    state.literals.addAll(groupState.literals)
                    if (groupState.hasInfinite) {
                        state.hasInfinite = true
                        return closingParenIndex + 1
                    }
                }
                nextIndex = closingParenIndex + 1
            }
            '*', '+', '?', '{', ')' -> return index + 1
            else -> {
                elementBaseLength = 1
                elementLiteral = char.toString()
            }
        }

        val (totalLength, hasInfinite, quantifier, charsConsumed) = parseAndApplyQuantifier(pattern, nextIndex, elementBaseLength)
        state.maxLength += totalLength
        if (hasInfinite) state.hasInfinite = true

        if (elementLiteral != null) {
            when {
                quantifier is Int -> state.literals.add(LiteralDetail(elementLiteral.repeat(quantifier), index))
                !hasInfinite -> state.literals.add(LiteralDetail(elementLiteral, index))
            }
        }
        return nextIndex + charsConsumed
    }

    private fun parseAndApplyQuantifier(pattern: String, index: Int, baseLength: Int): Quadruple<Int, Boolean, Any?, Int> {
        return when (pattern.getOrNull(index)) {
            '*', '+' -> Quadruple(0, true, pattern[index], 1)
            '?' -> Quadruple(baseLength, false, pattern[index], 1)
            '{' -> {
                val end = pattern.indexOf('}', index)
                if (end == -1) return Quadruple(baseLength, false, null, 0)
                val content = pattern.substring(index + 1, end)
                val parts = content.split(',')
                val consumed = end - index + 1
                try {
                    if (parts.size == 1) {
                        val n = parts[0].toInt()
                        Quadruple(baseLength * n, false, n, consumed)
                    } else if (parts[1].trim().isEmpty()) {
                        Quadruple(0, true, null, consumed)
                    } else {
                        val m = parts[1].toInt()
                        Quadruple(baseLength * m, false, m, consumed)
                    }
                } catch (e: NumberFormatException) { Quadruple(baseLength, false, null, 0) }
            }
            else -> Quadruple(baseLength, false, null, 0)
        }
    }

    // UTILITY FUNCTIONS

    private fun splitTopLevelAlternations(pattern: String): List<String> {
        val branches = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0
        var inCharClass = false
        var inEscape = false
        pattern.forEach { char ->
            if (inEscape) {
                current.append(char)
                inEscape = false
                return@forEach
            }
            when (char) {
                '\\' -> inEscape = true
                '[' -> if (parenDepth == 0) inCharClass = true
                ']' -> if (parenDepth == 0) inCharClass = false
                '(' -> if (!inCharClass) parenDepth++
                ')' -> if (!inCharClass && parenDepth > 0) parenDepth--
                '|' -> if (parenDepth == 0 && !inCharClass) {
                    branches.add(current.toString())
                    current = StringBuilder()
                    return@forEach
                }
            }
            current.append(char)
        }
        branches.add(current.toString())
        return branches
    }

    private fun findClosingParen(pattern: String, start: Int): Int {
        var level = 1
        for (i in (start + 1)..<pattern.length) {
            if (pattern[i] == '(') level++
            if (pattern[i] == ')') level--
            if (level == 0) return i
        }
        return pattern.length - 1
    }

    private fun findClosingBracket(pattern: String, start: Int): Int {
        return pattern.indexOf(']', start + 1).takeIf { it != -1 } ?: pattern.length - 1
    }

    private fun buildLiteralOccurrencesMap(literals: List<LiteralDetail>): Map<String, List<Int>> {
        return literals.groupBy({ it.text }, { it.position })
    }
}

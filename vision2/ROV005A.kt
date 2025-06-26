package com.barclays.mobilebanking.utils.regex

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min

// --- Public Data Models ---
data class LiteralDetail(val text: String, val position: Int)
data class AnalysisResult(
    val maxLength: Int,
    val hasInfiniteLength: Boolean,
    val literals: List<LiteralDetail>,
    val literalOccurrences: Map<String, List<Int>>
)
data class TransformationResult(
    val formattedText: String,
    val newCursorPosition: Int
)

// --- Primary Public Classes ---

/**
 * Thread-safe regex input formatter with lazy initialization and caching optimizations.
 * Designed for real-time input formatting based on regex patterns for UI components.
 */
class RegexInputFormatter(private val pattern: String) {
    private val analyser = RegexAnalyser(pattern)
    private val mutex = Mutex()
    
    private val formattingSteps: List<RegexPatternParser.FormattingStep> by lazy {
        RegexPatternParser.buildFormattingSteps(pattern)
    }
    
    private val prefixCache: String by lazy {
        computePrefix()
    }
    
    private val inputFilter: (Char) -> Boolean = { char ->
        char.isLetterOrDigit() || char in "-_.@" || char == '+'
    }

    fun getMaxLength(): Int = analyser.getMaxLength()
    fun getPrefix(): String = prefixCache
    fun getExpectedFormat(): String = analyser.getExpectedFormat()

    private fun computePrefix(): String {
        val prefix = StringBuilder()
        for (step in formattingSteps) {
            when (step) {
                is RegexPatternParser.FormattingStep.Insert -> prefix.append(step.literal)
                is RegexPatternParser.FormattingStep.Consume -> break
            }
        }
        return prefix.toString()
    }

    /**
     * Formats the input text in a thread-safe manner.
     */
    suspend fun formatAsync(currentText: String): TransformationResult = mutex.withLock {
        formatInternal(currentText)
    }

    /**
     * Formats the input text.
     */
    fun format(currentText: String): TransformationResult = formatInternal(currentText)

    private fun formatInternal(currentText: String): TransformationResult {
        val cleanInput = currentText.filter(inputFilter)
        val prefix = getPrefix()
        
        if (cleanInput.isEmpty()) {
            return TransformationResult(prefix, prefix.length)
        }

        val formatted = StringBuilder()
        var inputCursor = 0
        var literalOffset = 0

        for (step in formattingSteps) {
            if (inputCursor >= cleanInput.length) break
            
            when (step) {
                is RegexPatternParser.FormattingStep.Consume -> {
                    val availableInput = cleanInput.length - inputCursor
                    val charsToConsume = min(availableInput, step.length)
                    
                    if (charsToConsume > 0) {
                        formatted.append(cleanInput, inputCursor, inputCursor + charsToConsume)
                        inputCursor += charsToConsume
                    }
                }
                is RegexPatternParser.FormattingStep.Insert -> {
                    if (inputCursor < cleanInput.length || formatted.isEmpty() || currentText == prefix) {
                        formatted.append(step.literal)
                        literalOffset += step.literal.length
                    }
                }
            }
        }

        val maxLength = getMaxLength()
        if (maxLength > 0 && formatted.length > maxLength) {
            formatted.setLength(maxLength)
        }
        
        val newCursorPosition = min(inputCursor + literalOffset, formatted.length)
        
        return TransformationResult(
            formattedText = formatted.toString(),
            newCursorPosition = newCursorPosition
        )
    }
}

/**
 * Enhanced regex analyzer with caching and robust error handling.
 * Provides detailed structural information about a regex pattern.
 */
class RegexAnalyser(val pattern: String) {
    val analysisResult: AnalysisResult by lazy { 
        try {
            RegexPatternParser.parse(pattern)
        } catch (e: Throwable) {
            // Fallback for any error, including StackOverflow
            AnalysisResult(
                maxLength = Constants.LENGTH_INFINITE,
                hasInfiniteLength = true,
                literals = emptyList(),
                literalOccurrences = emptyMap()
            )
        }
    }
    
    private val compiledRegex: Regex? by lazy {
        try { pattern.toRegex() } catch (e: Exception) { null }
    }

    fun getMaxLength(): Int = analysisResult.maxLength
    fun getLiterals(): List<LiteralDetail> = analysisResult.literals
    fun hasLiteral(literal: String): Boolean = analysisResult.literalOccurrences.containsKey(literal)
    fun isValid(input: String): Boolean = compiledRegex?.matches(input) ?: false
    
    fun getExpectedFormat(): String {
        return try {
            val steps = RegexPatternParser.buildFormattingSteps(pattern)
            steps.joinToString("") {
                when (it) {
                    is RegexPatternParser.FormattingStep.Insert -> it.literal
                    is RegexPatternParser.FormattingStep.Consume -> "x".repeat(it.length)
                }
            }
        } catch (e: Exception) { "Invalid pattern" }
    }
}

// --- Internal Implementation ---

private object Constants {
    const val LENGTH_INFINITE = -1; const val NO_LENGTH_CONTRIBUTION = 0; const val SINGLE_CHAR_LENGTH = 1
    const val SINGLE_TOKEN_CONSUMED_LENGTH = 1; const val ESCAPED_TOKEN_CONSUMED_LENGTH = 2
    const val CAPTURED_GROUP_OFFSET = 1
    
    object Chars {
        const val GROUP_START = '('; const val GROUP_END = ')'; const val CLASS_START = '['
        const val CLASS_END = ']'; const val QUANTIFIER_START = '{'; const val QUANTIFIER_END = '}'
        const val ALTERNATION = '|'; const val ESCAPE = '\\'; const val WILDCARD = '.'
        const val ANCHOR_START = '^'; const val ANCHOR_END = '$'
    }
}

private object RegexPatternParser {
    data class BranchState(var maxLength: Int = 0, var hasInfinite: Boolean = false)
    data class QuantifierParseResult(val totalLength: Int, val isInfinite: Boolean, val charsConsumed: Int)
    sealed class FormattingStep {
        data class Consume(val length: Int) : FormattingStep()
        data class Insert(val literal: String) : FormattingStep()
    }
    
    private val lookaheadConstraintRegex = Regex("""\(\?=\^.{(\d*),(\d*)}\$\)""")

    fun parse(pattern: String): AnalysisResult {
        val (lookaheadMax, patternWithoutLookahead) = extractLookaheadLengthConstraint(pattern)
        val branches = RegexStringUtils.splitTopLevelAlternations(patternWithoutLookahead)
        var globalMax = 0
        var hasInfinite = false
        val allLiterals = mutableListOf<LiteralDetail>()
        
        for (branch in branches) {
            val (state, literals) = analyseBranch(branch)
            allLiterals.addAll(literals)
            if (state.hasInfinite) hasInfinite = true
            globalMax = max(globalMax, state.maxLength)
        }
        
        val finalMaxLength = when {
            hasInfinite && lookaheadMax != null -> lookaheadMax
            hasInfinite -> Constants.LENGTH_INFINITE
            else -> globalMax
        }
        
        return AnalysisResult(finalMaxLength, hasInfinite && lookaheadMax == null, allLiterals, buildLiteralOccurrencesMap(allLiterals))
    }

    fun buildFormattingSteps(pattern: String): List<FormattingStep> {
        return try {
            val steps = mutableListOf<FormattingStep>()
            val (_, patternToFormat) = extractLookaheadLengthConstraint(pattern)
            var cursor = 0
            while (cursor < patternToFormat.length) {
                cursor = processFormattingToken(patternToFormat, cursor, steps)
            }
            steps
        } catch (e: Exception) { emptyList() }
    }
    
    private fun processFormattingToken(pattern: String, cursor: Int, steps: MutableList<FormattingStep>): Int {
        if (cursor >= pattern.length) return cursor
        val char = pattern[cursor]
        
        return when (char) {
            Constants.Chars.ANCHOR_START, Constants.Chars.ANCHOR_END -> cursor + 1
            Constants.Chars.GROUP_START -> {
                val end = RegexStringUtils.findClosingParen(pattern, cursor)
                val groupContent = pattern.substring(cursor + 1, end)
                if (isGroupPurelyLiteral(groupContent)) {
                    steps.add(FormattingStep.Insert(groupContent))
                } else {
                    steps.addAll(buildFormattingSteps(groupContent))
                }
                end + 1
            }
            Constants.Chars.CLASS_START, Constants.Chars.ESCAPE, Constants.Chars.WILDCARD -> {
                val (baseLength, nextIndex) = getConsumingTokenLength(pattern, cursor)
                val quantifierResult = parseAndApplyQuantifier(pattern, nextIndex, baseLength)
                if (quantifierResult.isInfinite) return pattern.length
                if (quantifierResult.totalLength > 0) steps.add(FormattingStep.Consume(quantifierResult.totalLength))
                nextIndex + quantifierResult.charsConsumed
            }
            else -> {
                if (!isQuantifierChar(char)) steps.add(FormattingStep.Insert(char.toString()))
                cursor + 1
            }
        }
    }
    
    private fun isQuantifierChar(char: Char): Boolean = char in "*+?{}"
    private fun isGroupPurelyLiteral(groupContent: String): Boolean = !groupContent.any { it in "[]\\." || groupContent.contains("\\d") || groupContent.contains("\\w") || groupContent.contains("\\s") }

    private fun extractLookaheadLengthConstraint(pattern: String): Pair<Int?, String> {
        val match = lookaheadConstraintRegex.find(pattern)
        return if (match != null) Pair(match.groupValues.getOrNull(2)?.toIntOrNull(), pattern.replace(match.value, "")) else Pair(null, pattern)
    }
    
    private fun getConsumingTokenLength(pattern: String, index: Int): Pair<Int, Int> {
        return when (pattern[index]) {
            Constants.Chars.GROUP_START -> {
                val end = RegexStringUtils.findClosingParen(pattern, index)
                val groupContent = pattern.substring(index + 1, end)
                // Non-recursive length analysis to prevent stack overflow
                Pair(analyseBranch(groupContent).first.maxLength, end + 1)
            }
            Constants.Chars.CLASS_START -> Pair(Constants.SINGLE_CHAR_LENGTH, RegexStringUtils.findClosingBracket(pattern, index) + 1)
            Constants.Chars.ESCAPE -> Pair(Constants.SINGLE_CHAR_LENGTH, index + Constants.ESCAPED_TOKEN_CONSUMED_LENGTH)
            else -> Pair(Constants.SINGLE_CHAR_LENGTH, index + 1)
        }
    }
    
    private fun parseAndApplyQuantifier(pattern: String, index: Int, baseLength: Int): QuantifierParseResult {
        return when (pattern.getOrNull(index)) {
            '*', '+' -> QuantifierParseResult(Constants.NO_LENGTH_CONTRIBUTION, true, Constants.SINGLE_TOKEN_CONSUMED_LENGTH)
            '?' -> QuantifierParseResult(baseLength, false, Constants.SINGLE_TOKEN_CONSUMED_LENGTH)
            '{' -> parseExplicitQuantifier(pattern, index, baseLength)
            else -> QuantifierParseResult(baseLength, false, 0)
        }
    }
    
    private fun parseExplicitQuantifier(pattern: String, index: Int, baseLength: Int): QuantifierParseResult {
        val end = pattern.indexOf(Constants.Chars.QUANTIFIER_END, index)
        if (end == -1) return QuantifierParseResult(baseLength, false, 0)
        val consumed = end - index + 1
        val content = pattern.substring(index + 1, end)
        val parts = content.split(',')
        return try {
            when {
                parts.size == 1 -> QuantifierParseResult(baseLength * parts[0].toInt(), false, consumed)
                parts.size == 2 && parts[1].trim().isEmpty() -> QuantifierParseResult(Constants.NO_LENGTH_CONTRIBUTION, true, consumed)
                parts.size == 2 -> QuantifierParseResult(baseLength * parts[1].toInt(), false, consumed)
                else -> QuantifierParseResult(baseLength, false, 0)
            }
        } catch (e: NumberFormatException) { QuantifierParseResult(baseLength, false, 0) }
    }

    private fun analyseBranch(branch: String): Pair<BranchState, List<LiteralDetail>> {
        val state = BranchState()
        val literals = mutableListOf<LiteralDetail>()
        var i = 0
        while (i < branch.length) {
            i = processNextToken(branch, i, state, literals)
        }
        return Pair(state, literals)
    }

    private fun processNextToken(pattern: String, index: Int, state: BranchState, literals: MutableList<LiteralDetail>): Int {
        val char = pattern[index]
        if (isQuantifierChar(char)) return index + 1
        
        val (baseLength, nextIndex) = getConsumingTokenLength(pattern, index)
        val quantifier = parseAndApplyQuantifier(pattern, nextIndex, baseLength)
        
        if (quantifier.isInfinite) state.hasInfinite = true
        else state.maxLength += quantifier.totalLength
        
        if (char !in "()[]\\.") {
            literals.add(LiteralDetail(char.toString(), index))
        }
        return nextIndex + quantifier.charsConsumed
    }
    
    private fun buildLiteralOccurrencesMap(literals: List<LiteralDetail>): Map<String, List<Int>> {
        return literals.groupBy({ it.text }, { it.position })
    }
}

private object RegexStringUtils {
    fun splitTopLevelAlternations(pattern: String): List<String> {
        val branches = mutableListOf<String>(); val current = StringBuilder(); var parenDepth = 0; var inCharClass = false; var inEscape = false
        for (char in pattern) {
            if (inEscape) { current.append(char); inEscape = false; continue }
            when (char) {
                Constants.Chars.ESCAPE -> { inEscape = true; current.append(char) }
                Constants.Chars.CLASS_START -> { if (parenDepth == 0) inCharClass = true; current.append(char) }
                Constants.Chars.CLASS_END -> { if (parenDepth == 0) inCharClass = false; current.append(char) }
                Constants.Chars.GROUP_START -> { if (!inCharClass) parenDepth++; current.append(char) }
                Constants.Chars.GROUP_END -> { if (!inCharClass && parenDepth > 0) parenDepth--; current.append(char) }
                Constants.Chars.ALTERNATION -> { if (parenDepth == 0 && !inCharClass) { branches.add(current.toString()); current.clear(); continue }; current.append(char) }
                else -> current.append(char)
            }
        }
        branches.add(current.toString()); return branches
    }
    
    fun findClosingParen(pattern: String, start: Int): Int {
        var level = 1; var inEscape = false
        for (i in start + 1 until pattern.length) {
            if (inEscape) { inEscape = false; continue }
            when (pattern[i]) {
                Constants.Chars.ESCAPE -> inEscape = true
                Constants.Chars.GROUP_START -> level++
                Constants.Chars.GROUP_END -> { level--; if (level == 0) return i }
            }
        }
        return pattern.length - 1
    }
    
    fun findClosingBracket(pattern: String, start: Int): Int {
        var inEscape = false
        for (i in start + 1 until pattern.length) {
            if (inEscape) { inEscape = false; continue }
            when (pattern[i]) {
                Constants.Chars.ESCAPE -> inEscape = true
                Constants.Chars.CLASS_END -> return i
            }
        }
        return pattern.length - 1
    }
}

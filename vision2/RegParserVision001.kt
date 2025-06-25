package com.barclays.mobilebanking.utils.regex

import kotlin.math.max

// --- Public Data Models ---
data class LiteralDetail(val text: String, val position: Int)
data class AnalysisResult(val maxLength: Int, val hasInfiniteLength: Boolean, val literals: List<LiteralDetail>, val literalOccurrences: Map<String, List<Int>>)
data class TransformationResult(val formattedText: String, val newCursorPosition: Int)

// --- Primary Public Classes ---

/**
 * A class designed to provide real-time input formatting based on a regex pattern.
 * This is the main class to use for UI formatting tasks like a `VisualTransformation`.
 */
class RegexInputFormatter(pattern: String) {
    private val analyser = RegexAnalyser(pattern)
    private val formattingSteps = RegexPatternParser.buildFormattingSteps(pattern)

    fun getMaxLength(): Int = analyser.getMaxLength()

    fun getPrefix(): String {
        val prefix = StringBuilder()
        for (step in formattingSteps) {
            if (step is RegexPatternParser.FormattingStep.Insert) prefix.append(step.literal)
            else if (step is RegexPatternParser.FormattingStep.Consume) break
        }
        return prefix.toString()
    }

    fun format(currentText: String): TransformationResult {
        val cleanInput = currentText.filter { it.isLetterOrDigit() }
        val prefix = getPrefix()
        if (cleanInput.isEmpty()) {
            return TransformationResult(prefix, prefix.length) // First return for empty state
        }

        val formatted = StringBuilder()
        var inputCursor = 0
        var literalOffset = 0

        for (step in formattingSteps) {
            if (inputCursor >= cleanInput.length) break
            when (step) {
                is RegexPatternParser.FormattingStep.Consume -> {
                    val charsToConsume = minOf(cleanInput.length - inputCursor, step.length)
                    formatted.append(cleanInput, inputCursor, inputCursor + charsToConsume)
                    inputCursor += charsToConsume
                }
                is RegexPatternParser.FormattingStep.Insert -> {
                    if (inputCursor < cleanInput.length) {
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
        
        return TransformationResult(
            formattedText = formatted.toString(),
            newCursorPosition = minOf(inputCursor + literalOffset, formatted.length)
        )
    }
}

/**
 * A lean class for analyzing a regex pattern. Its sole responsibility is to provide
 * detailed analysis results like max length, literals, etc., on demand.
 */
class RegexAnalyser(val pattern: String) {
    val analysisResult: AnalysisResult by lazy { RegexPatternParser.parse(pattern) }

    fun getMaxLength(): Int = analysisResult.maxLength
    fun getLiterals(): List<LiteralDetail> = analysisResult.literals
    fun hasLiteral(literal: String): Boolean = analysisResult.literalOccurrences.containsKey(literal)
    fun isValid(input: String): Boolean = try { pattern.toRegex().matches(input) } catch (e: Exception) { false }
}

// --- Internal Implementation (Private top-level objects) ---

private object Constants {
    const val LENGTH_INFINITE = -1; const val NO_LENGTH_CONTRIBUTION = 0; const val SINGLE_CHAR_LENGTH = 1
    const val SINGLE_TOKEN_CONSUMED_LENGTH = 1; const val ESCAPED_TOKEN_CONSUMED_LENGTH = 2
    object Chars {
        const val GROUP_START = '('; const val GROUP_END = ')'; const val CLASS_START = '['; const val CLASS_END = ']'
        const val QUANTIFIER_START = '{'; const val QUANTIFIER_END = '}'; const val ALTERNATION = '|'
        const val ESCAPE = '\\'; const val WILDCARD = '.'; const val ANCHOR_START = '^'; const val ANCHOR_END = '$'
    }
}

private object RegexPatternParser {
    data class BranchState(var maxLength: Int = 0, var hasInfinite: Boolean = false, val literals: MutableList<LiteralDetail> = mutableListOf())
    data class QuantifierParseResult(val totalLength: Int, val isInfinite: Boolean)
    sealed class FormattingStep {
        data class Consume(val length: Int) : FormattingStep()
        data class Insert(val literal: String) : FormattingStep()
    }
    
    private val lookaheadConstraintRegex = "\\(\\?=\\^\\.\\{\\d*,(\\d+)}\\$\\)".toRegex()

    fun parse(pattern: String): AnalysisResult {
        val (lookaheadMax, patternWithoutLookahead) = extractLookaheadLengthConstraint(pattern)
        val branches = RegexStringUtils.splitTopLevelAlternations(patternWithoutLookahead)
        val allLiterals = mutableListOf<LiteralDetail>()
        var globalMax = 0; var hasInfinite = false
        for (branch in branches) {
            val state = analyseBranch(branch)
            allLiterals.addAll(state.literals); if (state.hasInfinite) hasInfinite = true; globalMax = max(globalMax, state.maxLength)
        }
        val finalMaxLength = when { hasInfinite && lookaheadMax != null -> lookaheadMax; hasInfinite -> Constants.LENGTH_INFINITE; else -> globalMax }
        return AnalysisResult(finalMaxLength, hasInfinite && lookaheadMax == null, allLiterals, buildLiteralOccurrencesMap(allLiterals))
    }

    fun buildFormattingSteps(pattern: String): List<FormattingStep> {
        val steps = mutableListOf<FormattingStep>(); val (_, patternToFormat) = extractLookaheadLengthConstraint(pattern)
        var cursor = 0
        while (cursor < patternToFormat.length) {
            val char = patternToFormat[cursor]; var advanced = false
            when (char) {
                Constants.Chars.ANCHOR_START, Constants.Chars.ANCHOR_END -> { cursor++; advanced = true }
                Constants.Chars.GROUP_START -> {
                    val end = RegexStringUtils.findClosingParen(patternToFormat, cursor)
                    steps.addAll(buildFormattingSteps(patternToFormat.substring(cursor + 1, end)))
                    cursor = end + 1; advanced = true
                }
                Constants.Chars.CLASS_START, Constants.Chars.ESCAPE, Constants.Chars.WILDCARD -> {
                    val (baseLength, nextIndex) = getConsumingTokenLength(patternToFormat, cursor)
                    val quantifierResult = parseAndApplyQuantifier(patternToFormat, nextIndex, baseLength)
                    if (quantifierResult.isInfinite || quantifierResult.totalLength < 0) return emptyList()
                    if (quantifierResult.totalLength > 0) steps.add(FormattingStep.Consume(quantifierResult.totalLength))
                    
                    // Correctly advance cursor past the quantifier
                    var tempCursor = nextIndex
                    if (patternToFormat.getOrNull(tempCursor) == '{') {
                        tempCursor = patternToFormat.indexOf('}', tempCursor) + 1
                    } else if (patternToFormat.getOrNull(tempCursor) in "*+?") {
                        tempCursor++
                    }
                    cursor = tempCursor

                    advanced = true
                }
            }
            if (!advanced) { if ("*+?{}".indexOf(char) == -1) steps.add(FormattingStep.Insert(char.toString())); cursor++ }
        }
        return steps
    }

    private fun extractLookaheadLengthConstraint(pattern: String): Pair<Int?, String> {
        val match = lookaheadConstraintRegex.find(pattern)
        return if (match != null) Pair(match.groupValues.getOrNull(1)?.toIntOrNull(), pattern.replace(match.value, "")) else Pair(null, pattern)
    }

    private fun getConsumingTokenLength(pattern: String, index: Int): Pair<Int, Int> {
        return when (pattern[index]) {
            Constants.Chars.GROUP_START -> { val end = RegexStringUtils.findClosingParen(pattern, index); Pair(analyseBranch(pattern.substring(index + 1, end)).maxLength, end + 1) }
            Constants.Chars.CLASS_START -> Pair(Constants.SINGLE_CHAR_LENGTH, RegexStringUtils.findClosingBracket(pattern, index) + 1)
            Constants.Chars.ESCAPE -> Pair(Constants.SINGLE_CHAR_LENGTH, index + Constants.ESCAPED_TOKEN_CONSUMED_LENGTH)
            else -> Pair(Constants.SINGLE_CHAR_LENGTH, index + 1)
        }
    }
    
    private fun parseAndApplyQuantifier(pattern: String, index: Int, baseLength: Int): QuantifierParseResult {
        return when (pattern.getOrNull(index)) {
            '*', '+' -> QuantifierParseResult(Constants.NO_LENGTH_CONTRIBUTION, true)
            '?' -> QuantifierParseResult(baseLength, false)
            '{' -> {
                val end = pattern.indexOf(Constants.Chars.QUANTIFIER_END, index)
                if (end == -1) return QuantifierParseResult(baseLength, false)
                val parts = pattern.substring(index + 1, end).split(',')
                try {
                    when {
                        parts.size == 1 -> QuantifierParseResult(baseLength * parts[0].toInt(), false)
                        parts[1].trim().isEmpty() -> QuantifierParseResult(Constants.NO_LENGTH_CONTRIBUTION, true)
                        else -> QuantifierParseResult(baseLength * parts[1].toInt(), false)
                    }
                } catch (e: NumberFormatException) { QuantifierParseResult(baseLength, false) }
            }
            else -> QuantifierParseResult(baseLength, false)
        }
    }

    private fun analyseBranch(branch: String): BranchState {
        val state = BranchState(); var i = 0; while (i < branch.length) { i = processNextToken(branch, i, state) }; return state
    }

    private fun processNextToken(pattern: String, index: Int, state: BranchState): Int {
        val char = pattern[index]
        if ("*+?{}".indexOf(char) != -1) return index + 1 // Skip quantifiers
        
        val (baseLength, nextIndex) = getConsumingTokenLength(pattern, index)
        val quantifier = parseAndApplyQuantifier(pattern, nextIndex, baseLength)
        
        if (quantifier.isInfinite) state.hasInfinite = true else state.maxLength += quantifier.totalLength
        
        // Literal handling for analysis
        if (char !in "()[]\\.") {
            // Find all literals, even inside groups. This is a simple approximation.
            pattern.substring(index, nextIndex).forEachIndexed { i, c ->
                 if (c !in "()[]\\.") {
                     state.literals.add(LiteralDetail(c.toString(), index + i))
                 }
            }
        }
        
        // Correctly advance cursor past token AND its potential quantifier
        var tempCursor = nextIndex
        if (pattern.getOrNull(tempCursor) == '{') {
            tempCursor = pattern.indexOf('}', tempCursor) + 1
        } else if (pattern.getOrNull(tempCursor) in "*+?") {
            tempCursor++
        }
        return tempCursor
    }
    
    private fun buildLiteralOccurrencesMap(literals: List<LiteralDetail>): Map<String, List<Int>> = literals.groupBy({ it.text }, { it.position })
}

private object RegexStringUtils {
    fun splitTopLevelAlternations(pattern: String): List<String> {
        val branches=mutableListOf<String>();val current=StringBuilder();var parenDepth=0;var inCharClass=false;var inEscape=false;pattern.forEach{char->if(inEscape){current.append(char);inEscape=false;return@forEach}
        when(char){Constants.Chars.ESCAPE->inEscape=true;Constants.Chars.CLASS_START->if(parenDepth==0)inCharClass=true;Constants.Chars.CLASS_END->if(parenDepth==0)inCharClass=false;Constants.Chars.GROUP_START->if(!inCharClass)parenDepth++;Constants.Chars.GROUP_END->if(!inCharClass&&parenDepth>0)parenDepth--;Constants.Chars.ALTERNATION->if(parenDepth==0&&!inCharClass){branches.add(current.toString());current.clear();return@forEach}
        else->current.append(char)}};branches.add(current.toString());return branches
    }
    fun findClosingParen(pattern: String, start: Int): Int { var level=1; for(i in start+1 until pattern.length){when(pattern[i]){Constants.Chars.GROUP_START->level++;Constants.Chars.GROUP_END->level--};if(level==0)return i}; return pattern.length-1 }
    fun findClosingBracket(pattern: String, start: Int): Int = pattern.indexOf(Constants.Chars.CLASS_END,start+1).takeIf{it!=-1}?:pattern.length-1
}

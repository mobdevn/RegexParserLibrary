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
 * Designed for real-time input formatting based on regex patterns.
 */
class RegexInputFormatter(private val pattern: String) {
    private val analyser = RegexAnalyser(pattern)
    private val mutex = Mutex()
    
    // Lazy initialization for expensive operations
    private val formattingSteps: List<RegexPatternParser.FormattingStep> by lazy {
        RegexPatternParser.buildFormattingSteps(pattern)
    }
    
    // Cached prefix to avoid repeated calculations
    private val prefixCache: String by lazy {
        computePrefix()
    }
    
    // Enhanced input filter supporting more characters
    private val inputFilter: (Char) -> Boolean = { char ->
        char.isLetterOrDigit() || char in "-_.@"
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

    suspend fun formatAsync(currentText: String): TransformationResult = mutex.withLock {
        formatInternal(currentText)
    }

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
        var totalFormatted = 0

        for (step in formattingSteps) {
            if (inputCursor >= cleanInput.length) break
            
            when (step) {
                is RegexPatternParser.FormattingStep.Consume -> {
                    val availableInput = cleanInput.length - inputCursor
                    val charsToConsume = min(availableInput, step.length)
                    
                    if (charsToConsume > 0) {
                        formatted.append(cleanInput, inputCursor, inputCursor + charsToConsume)
                        inputCursor += charsToConsume
                        totalFormatted += charsToConsume
                    }
                }
                is RegexPatternParser.FormattingStep.Insert -> {
                    // Insert literals when we have input or at the beginning
                    if (inputCursor > 0 || formatted.isEmpty()) {
                        formatted.append(step.literal)
                        literalOffset += step.literal.length
                    }
                }
            }
        }

        // Apply max length constraint
        val maxLength = getMaxLength()
        if (maxLength > 0 && formatted.length > maxLength) {
            formatted.setLength(maxLength)
        }
        
        // Improved cursor position calculation
        val newCursorPosition = calculateCursorPosition(inputCursor, literalOffset, formatted.length)
        
        return TransformationResult(
            formattedText = formatted.toString(),
            newCursorPosition = newCursorPosition
        )
    }
    
    private fun calculateCursorPosition(inputCursor: Int, literalOffset: Int, formattedLength: Int): Int {
        return min(inputCursor + literalOffset, formattedLength)
    }
}

/**
 * Enhanced regex analyzer with caching and better error handling.
 */
class RegexAnalyser(val pattern: String) {
    private val analysisResult: AnalysisResult by lazy { 
        try {
            RegexPatternParser.parse(pattern)
        } catch (e: Exception) {
            // Fallback to basic analysis on error
            AnalysisResult(
                maxLength = Constants.LENGTH_INFINITE,
                hasInfiniteLength = true,
                literals = emptyList(),
                literalOccurrences = emptyMap()
            )
        }
    }
    
    // Cached compiled regex for validation
    private val compiledRegex: Regex? by lazy {
        try {
            pattern.toRegex()
        } catch (e: Exception) {
            null
        }
    }

    fun getMaxLength(): Int = analysisResult.maxLength
    fun getLiterals(): List<LiteralDetail> = analysisResult.literals
    fun hasLiteral(literal: String): Boolean = analysisResult.literalOccurrences.containsKey(literal)
    
    fun isValid(input: String): Boolean {
        return compiledRegex?.matches(input) ?: false
    }
    
    fun getExpectedFormat(): String {
        val format = StringBuilder()
        try {
            val steps = RegexPatternParser.buildFormattingSteps(pattern)
            for (step in steps) {
                when (step) {
                    is RegexPatternParser.FormattingStep.Insert -> format.append(step.literal)
                    is RegexPatternParser.FormattingStep.Consume -> format.append("x".repeat(step.length))
                }
            }
        } catch (e: Exception) {
            return "Invalid pattern"
        }
        return format.toString()
    }
}

// --- Internal Implementation ---

private object Constants {
    const val LENGTH_INFINITE = -1
    const val NO_LENGTH_CONTRIBUTION = 0
    const val SINGLE_CHAR_LENGTH = 1
    const val SINGLE_TOKEN_CONSUMED_LENGTH = 1
    const val ESCAPED_TOKEN_CONSUMED_LENGTH = 2
    const val CAPTURED_GROUP_OFFSET = 1
    
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
        const val ANCHOR_START = '^'
        const val ANCHOR_END = '$'
    }
    
    object CharacterClasses {
        val DIGIT = setOf('0','1','2','3','4','5','6','7','8','9')
        val WORD = DIGIT + ('a'..'z').toSet() + ('A'..'Z').toSet() + setOf('_')
        val WHITESPACE = setOf(' ', '\t', '\n', '\r')
    }
}

private object RegexPatternParser {
    data class BranchState(
        var maxLength: Int = 0,
        var hasInfinite: Boolean = false,
        val literals: MutableList<LiteralDetail> = mutableListOf()
    )
    
    data class QuantifierParseResult(
        val totalLength: Int,
        val isInfinite: Boolean,
        val charsConsumed: Int
    )
    
    sealed class FormattingStep {
        data class Consume(val length: Int) : FormattingStep()
        data class Insert(val literal: String) : FormattingStep()
    }
    
    // Improved regex patterns for lookahead detection
    private val lookaheadConstraintRegex = Regex("""(?:\(\?=\^\.\{(\d+),(\d+)\}\$\)|\(\?=\^\.\{(\d+)\}\$\))""")

    fun parse(pattern: String): AnalysisResult {
        return try {
            val (lookaheadMax, patternWithoutLookahead) = extractLookaheadLengthConstraint(pattern)
            val branches = RegexStringUtils.splitTopLevelAlternations(patternWithoutLookahead)
            val allLiterals = mutableListOf<LiteralDetail>()
            var globalMax = 0
            var hasInfinite = false
            
            for (branch in branches) {
                val state = analyseBranch(branch)
                allLiterals.addAll(state.literals)
                if (state.hasInfinite) hasInfinite = true
                globalMax = max(globalMax, state.maxLength)
            }
            
            val finalMaxLength = when {
                hasInfinite && lookaheadMax != null -> lookaheadMax
                hasInfinite -> Constants.LENGTH_INFINITE
                else -> globalMax
            }
            
            AnalysisResult(
                maxLength = finalMaxLength,
                hasInfiniteLength = hasInfinite && lookaheadMax == null,
                literals = allLiterals,
                literalOccurrences = buildLiteralOccurrencesMap(allLiterals)
            )
        } catch (e: Exception) {
            // Return safe defaults on parsing error
            AnalysisResult(
                maxLength = Constants.LENGTH_INFINITE,
                hasInfiniteLength = true,
                literals = emptyList(),
                literalOccurrences = emptyMap()
            )
        }
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
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun processFormattingToken(
        pattern: String, 
        cursor: Int, 
        steps: MutableList<FormattingStep>
    ): Int {
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
                
                if (quantifierResult.isInfinite || quantifierResult.totalLength < 0) {
                    return pattern.length // Stop processing on infinite or invalid length
                }
                
                if (quantifierResult.totalLength > 0) {
                    steps.add(FormattingStep.Consume(quantifierResult.totalLength))
                }
                
                nextIndex + quantifierResult.charsConsumed
            }
            
            else -> {
                if (!isQuantifierChar(char)) {
                    steps.add(FormattingStep.Insert(char.toString()))
                }
                cursor + 1
            }
        }
    }
    
    private fun isQuantifierChar(char: Char): Boolean = char in "*+?{}"
    
    private fun isGroupPurelyLiteral(groupContent: String): Boolean {
        val specialChars = setOf('[', ']', '\\', '.', '*', '+', '?', '{', '}', '|', '^', '$')
        return !groupContent.any { it in specialChars } && 
               !groupContent.contains("\\d") && 
               !groupContent.contains("\\w") && 
               !groupContent.contains("\\s")
    }

    private fun extractLookaheadLengthConstraint(pattern: String): Pair<Int?, String> {
        val match = lookaheadConstraintRegex.find(pattern)
        return if (match != null) {
            val maxLength = match.groupValues.getOrNull(2)?.toIntOrNull() 
                ?: match.groupValues.getOrNull(3)?.toIntOrNull()
            Pair(maxLength, pattern.replace(match.value, ""))
        } else {
            Pair(null, pattern)
        }
    }
    
    private fun getConsumingTokenLength(pattern: String, index: Int): Pair<Int, Int> {
        return when (pattern[index]) {
            Constants.Chars.GROUP_START -> {
                val end = RegexStringUtils.findClosingParen(pattern, index)
                val groupContent = pattern.substring(index + 1, end)
                
                // Use direct calculation instead of recursive analyzer
                val groupLength = calculateGroupLength(groupContent)
                Pair(groupLength, end + 1)
            }
            Constants.Chars.CLASS_START -> {
                Pair(Constants.SINGLE_CHAR_LENGTH, RegexStringUtils.findClosingBracket(pattern, index) + 1)
            }
            Constants.Chars.ESCAPE -> {
                Pair(Constants.SINGLE_CHAR_LENGTH, index + Constants.ESCAPED_TOKEN_CONSUMED_LENGTH)
            }
            else -> Pair(Constants.SINGLE_CHAR_LENGTH, index + 1)
        }
    }
    
    private fun calculateGroupLength(groupContent: String): Int {
        // Simple heuristic for group length calculation
        return when {
            groupContent.isEmpty() -> 0
            isGroupPurelyLiteral(groupContent) -> groupContent.length
            else -> Constants.SINGLE_CHAR_LENGTH
        }
    }
    
    private fun parseAndApplyQuantifier(pattern: String, index: Int, baseLength: Int): QuantifierParseResult {
        return when (val char = pattern.getOrNull(index)) {
            '*', '+' -> QuantifierParseResult(
                totalLength = Constants.NO_LENGTH_CONTRIBUTION,
                isInfinite = true,
                charsConsumed = Constants.SINGLE_TOKEN_CONSUMED_LENGTH
            )
            '?' -> QuantifierParseResult(
                totalLength = baseLength,
                isInfinite = false,
                charsConsumed = Constants.SINGLE_TOKEN_CONSUMED_LENGTH
            )
            '{' -> parseExplicitQuantifier(pattern, index, baseLength)
            else -> QuantifierParseResult(
                totalLength = baseLength,
                isInfinite = false,
                charsConsumed = 0
            )
        }
    }
    
    private fun parseExplicitQuantifier(pattern: String, index: Int, baseLength: Int): QuantifierParseResult {
        val end = pattern.indexOf(Constants.Chars.QUANTIFIER_END, index)
        if (end == -1) return QuantifierParseResult(baseLength, false, 0)
        
        val consumed = end - index + 1
        val quantifierContent = pattern.substring(index + 1, end)
        val parts = quantifierContent.split(',')
        
        return try {
            when {
                parts.size == 1 -> {
                    val count = parts[0].toInt()
                    QuantifierParseResult(baseLength * count, false, consumed)
                }
                parts.size == 2 && parts[1].trim().isEmpty() -> {
                    QuantifierParseResult(Constants.NO_LENGTH_CONTRIBUTION, true, consumed)
                }
                parts.size == 2 -> {
                    val maxCount = parts[1].toInt()
                    QuantifierParseResult(baseLength * maxCount, false, consumed)
                }
                else -> QuantifierParseResult(baseLength, false, 0)
            }
        } catch (e: NumberFormatException) {
            QuantifierParseResult(baseLength, false, 0)
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
        val char = pattern[index]
        
        if (isQuantifierChar(char)) {
            return index + 1
        }
        
        val (baseLength, nextIndex) = getConsumingTokenLength(pattern, index)
        val quantifier = parseAndApplyQuantifier(pattern, nextIndex, baseLength)
        
        if (quantifier.isInfinite) {
            state.hasInfinite = true
        } else {
            state.maxLength += quantifier.totalLength
        }
        
        // Add literal if it's a simple character
        if (char !in "()[]\\.") {
            state.literals.add(LiteralDetail(char.toString(), index))
        }
        
        return nextIndex + quantifier.charsConsumed
    }
    
    private fun buildLiteralOccurrencesMap(literals: List<LiteralDetail>): Map<String, List<Int>> {
        return literals.groupBy({ it.text }, { it.position })
    }
}

private object RegexStringUtils {
    
    fun splitTopLevelAlternations(pattern: String): List<String> {
        val branches = mutableListOf<String>()
        val current = StringBuilder()
        var parenDepth = 0
        var inCharClass = false
        var inEscape = false
        
        for (char in pattern) {
            if (inEscape) {
                current.append(char)
                inEscape = false
                continue
            }
            
            when (char) {
                Constants.Chars.ESCAPE -> {
                    inEscape = true
                    current.append(char)
                }
                Constants.Chars.CLASS_START -> {
                    if (parenDepth == 0) inCharClass = true
                    current.append(char)
                }
                Constants.Chars.CLASS_END -> {
                    if (parenDepth == 0) inCharClass = false
                    current.append(char)
                }
                Constants.Chars.GROUP_START -> {
                    if (!inCharClass) parenDepth++
                    current.append(char)
                }
                Constants.Chars.GROUP_END -> {
                    if (!inCharClass && parenDepth > 0) parenDepth--
                    current.append(char)
                }
                Constants.Chars.ALTERNATION -> {
                    if (parenDepth == 0 && !inCharClass) {
                        branches.add(current.toString())
                        current.clear()
                        continue
                    }
                    current.append(char)
                }
                else -> current.append(char)
            }
        }
        
        branches.add(current.toString())
        return branches
    }
    
    fun findClosingParen(pattern: String, start: Int): Int {
        var level = 1
        var inEscape = false
        
        for (i in start + 1 until pattern.length) {
            if (inEscape) {
                inEscape = false
                continue
            }
            
            when (pattern[i]) {
                Constants.Chars.ESCAPE -> inEscape = true
                Constants.Chars.GROUP_START -> level++
                Constants.Chars.GROUP_END -> {
                    level--
                    if (level == 0) return i
                }
            }
        }
        
        return pattern.length - 1
    }
    
    fun findClosingBracket(pattern: String, start: Int): Int {
        var inEscape = false
        
        for (i in start + 1 until pattern.length) {
            if (inEscape) {
                inEscape = false
                continue
            }
            
            when (pattern[i]) {
                Constants.Chars.ESCAPE -> inEscape = true
                Constants.Chars.CLASS_END -> return i
            }
        }
        
        return pattern.length - 1
    }
}

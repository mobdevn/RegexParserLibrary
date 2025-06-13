data class RegexLiteral(
    val value: String,
    val patternPosition: Int,
    val inputPosition: Int
)

data class RegexAnalysis(
    val maxLength: Int,
    val literals: List<RegexLiteral>,
    val hasInfiniteQuantifiers: Boolean
)

object RegexAnalyzer {
    fun analyzeRegex(pattern: String): RegexAnalysis {
        RegexHelper.extractLookaheadMaxLength(pattern)?.let { lookaheadMax ->
            return RegexAnalysis(
                maxLength = lookaheadMax,
                literals = emptyList(),
                hasInfiniteQuantifiers = false
            )
        }
        val branches = RegexHelper.splitTopLevelAlternations(pattern)
        var globalMax = 0
        var hasInfinite = false
        val allLiterals = mutableListOf<RegexLiteral>()
        for (branch in branches) {
            val state = RegexHelper.RegexParseState()
            var i = 0
            while (i < branch.length) {
                i += RegexHelper.processRegexChar(branch, i, state)
            }
            if (state.hasInfinite) hasInfinite = true
            if (state.maxLength > globalMax) globalMax = state.maxLength
            allLiterals.addAll(state.literals)
        }
        return RegexAnalysis(
            maxLength = if (hasInfinite) RegexHelper.INFINITE_LENGTH else globalMax,
            literals = allLiterals,
            hasInfiniteQuantifiers = hasInfinite
        )
    }

    fun getRegexMaxLength(pattern: String): Int = analyzeRegex(pattern).maxLength

    fun getRegexLiterals(pattern: String): List<RegexLiteral> = analyzeRegex(pattern).literals

    fun formatStringForRegex(input: String, pattern: String): String {
        val analysis = analyzeRegex(pattern)
        val literals = analysis.literals.sortedBy { it.inputPosition }
        val maxLen = analysis.maxLength
        val sb = StringBuilder(input)
        var offset = 0
        for (literal in literals) {
            val pos = literal.inputPosition + offset
            val end = pos + literal.value.length
            val current = if (pos < sb.length && end <= sb.length) sb.substring(pos, end) else ""
            if (pos > sb.length) {
                sb.append(literal.value)
                offset += literal.value.length
            } else if (current != literal.value) {
                sb.insert(pos, literal.value)
                offset += literal.value.length
            }
        }
        if (maxLen > 0 && sb.length != maxLen) {
            return when {
                sb.length > maxLen -> sb.substring(0, maxLen)
                else -> sb.append("0".repeat(maxLen - sb.length)).toString()
            }
        }
        return sb.toString()
    }

    fun getLiteralPositions(pattern: String, literal: String): List<Int> =
        analyzeRegex(pattern).literals.filter { it.value == literal }.map { it.inputPosition }

    fun getFirstLiteralPosition(pattern: String, literal: String): Int =
        analyzeRegex(pattern).literals.firstOrNull { it.value == literal }?.inputPosition ?: -1
}

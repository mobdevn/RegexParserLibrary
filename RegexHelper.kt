object RegexHelper {
    // --- Constants ---
    const val CONSUMED_ESCAPE = 2
    const val CONSUMED_SINGLE = 1
    const val QUANTIFIER_SEPARATOR = ','
    const val INFINITE_LENGTH = -1
    const val ESCAPED_CLASSES = "dwsDWS"
    const val META_CHARS = "^$|[]()"

    data class RegexParseState(
        var maxLength: Int = 0,
        var inputPos: Int = 0,
        var hasInfinite: Boolean = false,
        val literals: MutableList<RegexLiteral> = mutableListOf()
    )

    fun extractLookaheadMaxLength(pattern: String): Int? {
        val lookaheadRegex = Regex("""\(\?=\^\.{(\d+),(\d+)}\$""")
        val singleLenRegex = Regex("""\(\?=\^\.{(\d+)}\$""")
        lookaheadRegex.find(pattern)?.let {
            return it.groupValues[2].toIntOrNull()
        }
        singleLenRegex.find(pattern)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        return null
    }

    fun splitTopLevelAlternations(pattern: String): List<String> {
        val branches = mutableListOf<StringBuilder>()
        var current = StringBuilder()
        var depth = 0
        var inEscape = false
        for (c in pattern) {
            if (inEscape) {
                current.append(c)
                inEscape = false
                continue
            }
            when (c) {
                '\\' -> {
                    current.append(c)
                    inEscape = true
                }
                '(' -> {
                    current.append(c)
                    depth++
                }
                ')' -> {
                    current.append(c)
                    depth--
                }
                '|' -> if (depth == 0) {
                    branches.add(current)
                    current = StringBuilder()
                } else {
                    current.append(c)
                }
                else -> current.append(c)
            }
        }
        branches.add(current)
        return branches.map { it.toString() }
    }

    fun processRegexChar(pattern: String, i: Int, state: RegexParseState): Int {
        val c = pattern[i]
        return when {
            c == '\\' && i + 1 < pattern.length -> handleEscape(pattern, i, state)
            c == '*' || c == '+' -> handleStarOrPlus(state)
            c == '?' -> CONSUMED_SINGLE
            c == '{' -> handleCurly(pattern, i, state)
            c == '.' -> handleDot(state)
            c in META_CHARS -> CONSUMED_SINGLE
            else -> handleLiteral(c, i, state)
        }
    }

    private fun handleEscape(pattern: String, i: Int, state: RegexParseState): Int {
        val escapedChar = pattern[i + 1]
        if (escapedChar !in ESCAPED_CLASSES) {
            state.literals.add(RegexLiteral(escapedChar.toString(), i, state.inputPos))
        }
        state.maxLength += CONSUMED_SINGLE
        state.inputPos += CONSUMED_SINGLE
        return CONSUMED_ESCAPE
    }

    private fun handleStarOrPlus(state: RegexParseState): Int {
        state.hasInfinite = true
        return CONSUMED_SINGLE
    }

    private fun handleCurly(pattern: String, i: Int, state: RegexParseState): Int {
        val close = pattern.indexOf('}', i)
        val consumed = if (close > i) close - i + CONSUMED_SINGLE else CONSUMED_SINGLE
        if (close > i) {
            val quant = pattern.substring(i + CONSUMED_SINGLE, close)
            val parts = quant.split(QUANTIFIER_SEPARATOR)
            val maxQ = when {
                parts.size > 1 && parts[1].isNotEmpty() -> parts[1].toIntOrNull()
                else -> parts[0].toIntOrNull()
            }
            if (maxQ == null) state.hasInfinite = true
            else state.maxLength += maxQ - CONSUMED_SINGLE
        }
        return consumed
    }

    private fun handleDot(state: RegexParseState): Int {
        state.maxLength += CONSUMED_SINGLE
        state.inputPos += CONSUMED_SINGLE
        return CONSUMED_SINGLE
    }

    private fun handleLiteral(c: Char, i: Int, state: RegexParseState): Int {
        state.literals.add(RegexLiteral(c.toString(), i, state.inputPos))
        state.maxLength += CONSUMED_SINGLE
        state.inputPos += CONSUMED_SINGLE
        return CONSUMED_SINGLE
    }
}

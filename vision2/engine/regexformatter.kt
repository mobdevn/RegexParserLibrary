import kotlin.math.max

/**
 * A production-ready, generic class to parse a regular expression and perform formatting and analysis.
 * It is designed to handle common patterns used in input masks (e.g., phone numbers, credit cards, IDs).
 * The class is immutable and thread-safe after construction.
 *
 * The parsing logic is executed lazily on first access and the result is cached for performance.
 *
 * @param pattern The regular expression string to be parsed.
 * @see RegexPart
 * @see RegexVisualTransformation
 */
class RegexFormatter(private val pattern: String) {

    /**
     * The parsed representation of the regex pattern.
     */
    private val parts: List<RegexPart> by lazy { parseRegexPattern() }

    private companion object {
        /** Represents an infinite maximum length for a regex (e.g., due to '*' or '+'). */
        const val INFINITE_LENGTH = -1L

        // --- Character & Token Constants ---
        const val CHAR_GROUP_START = '('
        const val CHAR_GROUP_END = ')'
        const val CHAR_CLASS_START = '['
        const val CHAR_CLASS_END = ']'
        const val ANCHOR_START_OF_LINE = '^'
        const val ANCHOR_END_OF_LINE = '$'
        const val ESCAPE_CHAR = '\\'
        const val ANY_CHAR_DOT = '.'
        const val ALTERNATION_CHAR = '|'

        // --- Special Group Constants ---
        const val SPECIAL_GROUP_PREFIX = "(?"
        const val NON_CAPTURING_GROUP_MARKER = ':'
        const val LOOKAHEAD_POSITIVE_MARKER = '='
        const val LOOKAHEAD_NEGATIVE_MARKER = '!'
        const val LOOKBEHIND_MARKER = '<'

        // --- Quantifier Constants ---
        const val QUANTIFIER_OPTIONAL = '?'
        const val QUANTIFIER_ZERO_OR_MORE = '*'
        const val QUANTIFIER_ONE_OR_MORE = '+'
        const val QUANTIFIER_RANGE_START = '{'
        const val QUANTIFIER_RANGE_END = '}'
        const val QUANTIFIER_RANGE_DELIMITER = ','
        private const val QUANTIFIER_REGEX_PATTERN = """(.*?)([$QUANTIFIER_OPTIONAL$QUANTIFIER_ZERO_OR_MORE$QUANTIFIER_ONE_OR_MORE]|\{$QUANTIFIER_RANGE_START}\d+(?:,$QUANTIFIER_RANGE_DELIMITER(\d*))?\}$QUANTIFIER_RANGE_END})$"""
        private val QUANTIFIER_REGEX = Regex(QUANTIFIER_REGEX_PATTERN)

        // --- Parsing Helper Constants ---
        private const val PLACEHOLDER_DEFINING_CHARS = "$CHAR_CLASS_START$CHAR_CLASS_END$ESCAPE_CHAR$ANY_CHAR_DOT$QUANTIFIER_OPTIONAL$QUANTIFIER_ZERO_OR_MORE$QUANTIFIER_ONE_OR_MORE$ALTERNATION_CHAR"
        private const val TOKEN_DELIMITING_CHARS = "$ANY_CHAR_DOT$ALTERNATION_CHAR$QUANTIFIER_OPTIONAL$QUANTIFIER_ZERO_OR_MORE$QUANTIFIER_ONE_OR_MORE$QUANTIFIER_RANGE_START$QUANTIFIER_RANGE_END$CHAR_CLASS_START$CHAR_CLASS_END$CHAR_GROUP_START$CHAR_GROUP_END$ESCAPE_CHAR"
    }

    // --- Public API ---

    fun getPrefix(): String {
        val prefix = StringBuilder()
        for (part in parts) {
            when (part) {
                is RegexPart.Literal -> prefix.append(part.value)
                is RegexPart.Anchor -> continue
                is RegexPart.Placeholder -> break
            }
        }
        return prefix.toString()
    }

    fun findInsertionPosition(): Int = getPrefix().length

    fun getMaxLength(): Long {
        var totalLength = 0L
        for (part in parts) {
            when (part) {
                is RegexPart.Literal -> totalLength += part.value.length
                is RegexPart.Placeholder -> {
                    if (part.length < 0) return INFINITE_LENGTH
                    totalLength += part.length
                }
                is RegexPart.Anchor -> { /* No length */ }
            }
        }
        return totalLength
    }

    fun getPlaceholderCount(): Long {
        var count = 0L
        for (part in parts) {
            if (part is RegexPart.Placeholder) {
                if (part.length < 0) return INFINITE_LENGTH
                count += part.length
            }
        }
        return count
    }

    fun format(userInput: String): String {
        val result = StringBuilder()
        var inputIndex = 0
        val cleanInput = userInput.filter { it.isLetterOrDigit() }

        for (part in parts) {
            if (inputIndex >= cleanInput.length && part is RegexPart.Placeholder) break
            when (part) {
                is RegexPart.Literal -> result.append(part.value)
                is RegexPart.Placeholder -> {
                    val length = part.length.takeIf { it > 0 } ?: cleanInput.length
                    val takeCount = minOf(length, cleanInput.length - inputIndex)
                    result.append(cleanInput.substring(inputIndex, inputIndex + takeCount))
                    inputIndex += takeCount
                }
                is RegexPart.Anchor -> { /* No length */ }
            }
        }
        return result.toString()
    }

    // --- Private Parsing Logic ---

    private fun parseRegexPattern(): List<RegexPart> {
        val result = mutableListOf<RegexPart>()
        var i = 0
        while (i < pattern.length) {
            val tokenEnd = findNextToken(i)
            if (tokenEnd <= i) break // Failsafe for malformed patterns
            result.add(createPartFromToken(pattern.substring(i, tokenEnd)))
            i = tokenEnd
        }
        return result
    }

    private fun createPartFromToken(token: String): RegexPart {
        return when (token.firstOrNull()) {
            ANCHOR_START_OF_LINE, ANCHOR_END_OF_LINE -> RegexPart.Anchor(token)
            ESCAPE_CHAR, CHAR_CLASS_START, ANY_CHAR_DOT -> parsePlaceholder(token, isGroup = false)
            CHAR_GROUP_START -> {
                val isLookaround = token.startsWith(SPECIAL_GROUP_PREFIX) && token.any { it in "$LOOKAHEAD_POSITIVE_MARKER$LOOKAHEAD_NEGATIVE_MARKER$LOOKBEHIND_MARKER" }
                if (isLookaround) {
                    RegexPart.Anchor(token)
                } else if (isGroupPurelyLiteral(token)) {
                    val content = token.substring(1, token.length - 1)
                        .removePrefix("$NON_CAPTURING_GROUP_MARKER")
                    RegexPart.Literal(content)
                } else {
                    parsePlaceholder(token, isGroup = true)
                }
            }
            else -> RegexPart.Literal(token)
        }
    }

    private fun isGroupPurelyLiteral(groupToken: String): Boolean {
        if (!groupToken.startsWith(CHAR_GROUP_START) || !groupToken.endsWith(CHAR_GROUP_END)) return false
        val content = groupToken.substring(1, groupToken.length - 1)
        return content.none { it in PLACEHOLDER_DEFINING_CHARS }
    }

    private fun parsePlaceholder(token: String, isGroup: Boolean): RegexPart.Placeholder {
        if (isGroup && token.contains(ALTERNATION_CHAR)) {
            val innerContent = token.removeSurrounding(CHAR_GROUP_START.toString(), CHAR_GROUP_END.toString())
            var maxLength = 0L

            // Use the new, robust splitter instead of the naive split('|')
            splitOnTopLevelAlternation(innerContent).forEach { alt ->
                val altLength = RegexFormatter(alt).getMaxLength()
                if (altLength == INFINITE_LENGTH) return RegexPart.Placeholder(-1, token)
                maxLength = max(maxLength, altLength)
            }
            return RegexPart.Placeholder(maxLength.toInt(), token)
        }

        val quantifierMatch = QUANTIFIER_REGEX.find(token)
        if (quantifierMatch != null) {
            val (base, quantifier, maxStr) = quantifierMatch.destructured
            val length = when (quantifier.first()) {
                QUANTIFIER_OPTIONAL -> 1
                QUANTIFIER_ZERO_OR_MORE, QUANTIFIER_ONE_OR_MORE -> -1
                QUANTIFIER_RANGE_START -> maxStr.ifEmpty {
                    quantifier.removeSurrounding(QUANTIFIER_RANGE_START.toString(), QUANTIFIER_RANGE_END.toString()).split(QUANTIFIER_RANGE_DELIMITER)[0]
                }.toIntOrNull() ?: 1
                else -> 1
            }
            return RegexPart.Placeholder(length, base)
        }
        return RegexPart.Placeholder(1, token)
    }
    
    private fun splitOnTopLevelAlternation(input: String): List<String> {
        val parts = mutableListOf<String>()
        var balance = 0
        var lastSplitIndex = 0
        input.forEachIndexed { i, char ->
            when (char) {
                CHAR_GROUP_START -> balance++
                CHAR_GROUP_END -> balance--
                ALTERNATION_CHAR -> {
                    if (balance == 0) {
                        parts.add(input.substring(lastSplitIndex, i))
                        lastSplitIndex = i + 1
                    }
                }
            }
        }
        parts.add(input.substring(lastSplitIndex))
        return parts
    }

    private fun findNextToken(startIndex: Int): Int {
        return when (pattern.getOrNull(startIndex)) {
            ESCAPE_CHAR -> startIndex + 2
            CHAR_CLASS_START -> pattern.indexOf(CHAR_CLASS_END, startIndex).takeIf { it != -1 }?.let { it + 1 } ?: (startIndex + 1)
            CHAR_GROUP_START -> findGroupEnd(startIndex) + 1
            ANCHOR_END_OF_LINE, ANCHOR_START_OF_LINE -> startIndex + 1
            null -> startIndex
            else -> findLiteralEnd(startIndex)
        }
    }

    private fun findLiteralEnd(startIndex: Int): Int {
        var endIndex = startIndex
        while (endIndex < pattern.length && pattern[endIndex] !in TOKEN_DELIMITING_CHARS) {
            endIndex++
        }
        return endIndex
    }

    private fun findGroupEnd(startIndex: Int): Int {
        var balance = 1 // Start with 1 to account for the opening parenthesis at startIndex
        var i = startIndex + 1 // Start checking from the character after the opening parenthesis

        while (i < pattern.length) {
            when (pattern[i]) {
                ESCAPE_CHAR -> {
                    i += 2 // Skip both the escape character and the character it escapes
                    continue
                }
                CHAR_GROUP_START -> balance++
                CHAR_GROUP_END -> balance--
            }

            if (balance == 0) {
                return i // Found the matching closing parenthesis
            }
            i++
        }
        return pattern.length - 1 // Fallback if no closing parenthesis is found
    }
}

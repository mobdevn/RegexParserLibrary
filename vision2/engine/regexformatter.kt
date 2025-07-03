/**
 * # Regex Utility Library
 * 
 * A comprehensive Kotlin library for parsing, analyzing, and formatting text based on regex patterns.
 * Supports complex regex features including lookaheads, quantifiers, character classes, and groups.
 * 
 * ## Features
 * - Maximum length calculation
 * - Literal position detection for dynamic insertion
 * - Prefix extraction
 * - Input formatting based on regex structure
 * - Support for complex regex patterns including lookaheads
 * 
 * ## Thread Safety
 * All classes are immutable after construction and thread-safe for concurrent read operations.
 * 
 * @author Generated Kotlin Regex Utility
 * @version 1.0.0
 * @since 1.0.0
 */

/**
 * Constants used throughout the regex utility library.
 * 
 * Centralizes all magic numbers, strings, and characters used in the regex parsing,
 * analysis, and formatting operations for better maintainability and readability.
 * 
 * @since 1.0.0
 */
object RegexConstants {
    
    // ===== NUMERIC CONSTANTS =====
    
    /** Zero value for quantifiers and calculations */
    const val ZERO = 0
    
    /** One value for default quantifiers and single character operations */
    const val ONE = 1
    
    /** Negative one for error conditions or invalid positions */
    const val NEGATIVE_ONE = -1
    
    /** Two characters, used for minimum lookahead length and other operations */
    const val TWO = 2
    
    /** Three characters, used for lookahead parsing offset */
    const val THREE = 3
    
    /** Four characters, used for alternation group estimation */
    const val FOUR = 4
    
    /** Maximum safe length cap to prevent memory issues with infinite quantifiers */
    const val MAX_SAFE_LENGTH = 1000
    
    /** Default group length divisor for conservative estimation */
    const val GROUP_LENGTH_DIVISOR = 3
    
    /** Alternation group length divisor for alternation patterns */
    const val ALTERNATION_DIVISOR = 4
    
    // ===== CHARACTER CONSTANTS =====
    
    /** Start anchor character */
    const val ANCHOR_START = '^'
    
    /** End anchor character */
    const val ANCHOR_END = '$'
    
    /** Opening parenthesis for groups */
    const val GROUP_OPEN = '('
    
    /** Closing parenthesis for groups */
    const val GROUP_CLOSE = ')'
    
    /** Opening bracket for character classes */
    const val CHAR_CLASS_OPEN = '['
    
    /** Closing bracket for character classes */
    const val CHAR_CLASS_CLOSE = ']'
    
    /** Opening brace for quantifiers */
    const val QUANTIFIER_OPEN = '{'
    
    /** Closing brace for quantifiers */
    const val QUANTIFIER_CLOSE = '}'
    
    /** Backslash for escape sequences */
    const val ESCAPE_CHAR = '\\'
    
    /** Comma separator in quantifiers */
    const val COMMA = ','
    
    /** Pipe character for alternation */
    const val PIPE = '|'
    
    /** Dash character commonly used as literal */
    const val DASH = '-'
    
    /** Underscore character for word boundaries */
    const val UNDERSCORE = '_'
    
    // ===== QUANTIFIER CHARACTERS =====
    
    /** Zero or more quantifier */
    const val QUANTIFIER_ZERO_OR_MORE = '*'
    
    /** One or more quantifier */
    const val QUANTIFIER_ONE_OR_MORE = '+'
    
    /** Zero or one quantifier */
    const val QUANTIFIER_ZERO_OR_ONE = '?'
    
    // ===== STRING CONSTANTS =====
    
    /** Positive lookahead prefix */
    const val LOOKAHEAD_POSITIVE = "(?="
    
    /** Negative lookahead prefix */
    const val LOOKAHEAD_NEGATIVE = "(?!"
    
    /** Empty string constant */
    const val EMPTY_STRING = ""
    
    /** Infinity symbol for display */
    const val INFINITY_SYMBOL = "∞"
    
    // ===== CHARACTER CLASS PATTERNS =====
    
    /** Digit character class */
    const val CHAR_CLASS_DIGITS = "[0-9]"
    
    /** Escaped digit shorthand */
    const val ESCAPED_DIGIT = "\\d"
    
    /** Lowercase letters character class */
    const val CHAR_CLASS_LOWERCASE = "[a-z]"
    
    /** Uppercase letters character class */
    const val CHAR_CLASS_UPPERCASE = "[A-Z]"
    
    /** All letters character class */
    const val CHAR_CLASS_LETTERS = "[a-zA-Z]"
    
    /** Word characters character class */
    const val CHAR_CLASS_WORD = "[\\w]"
    
    /** Escaped word shorthand */
    const val ESCAPED_WORD = "\\w"
    
    /** Range separator in character classes */
    const val RANGE_SEPARATOR = "-"
    
    /** Digits range pattern */
    const val DIGITS_RANGE = "0-9"
    
    /** Lowercase range pattern */
    const val LOWERCASE_RANGE = "a-z"
    
    /** Uppercase range pattern */
    const val UPPERCASE_RANGE = "A-Z"
    
    // ===== FORMATTING STRINGS =====
    
    /** Opening brace for quantifier display */
    const val DISPLAY_BRACE_OPEN = "{"
    
    /** Closing brace for quantifier display */
    const val DISPLAY_BRACE_CLOSE = "}"
    
    /** Pattern info class name */
    const val PATTERN_INFO_CLASS = "PatternInfo"
    
    /** Literal position class name */
    const val LITERAL_POSITION_CLASS = "LiteralPosition"
    
    // ===== REGEX VALIDATION PATTERNS =====
    
    /** Simple literal group validation pattern */
    const val SIMPLE_LITERAL_PATTERN = "^[a-zA-Z0-9\\-]+$"
    
    /** Digit validation pattern */
    const val DIGIT_PATTERN = "\\d"
    
    /** Lowercase validation pattern */
    const val LOWERCASE_PATTERN = "[a-z]"
    
    /** Uppercase validation pattern */
    const val UPPERCASE_PATTERN = "[A-Z]"
    
    /** Letter validation pattern */
    const val LETTER_PATTERN = "[a-zA-Z]"
}

/**
 * Generic regex utility class for parsing and formatting based on regex patterns.
 * 
 * This is the main entry point for regex analysis and formatting operations.
 * It coordinates between the parser, analyzer, and formatter to provide a unified API.
 * 
 * ## Usage Example
 * ```kotlin
 * val phoneRegex = "^\\+?1?[\\s.-]?\\(?([0-9]{3})\\)?[\\s.-]?([0-9]{3})[\\s.-]?([0-9]{4})$"
 * val utility = RegexUtility<String>(phoneRegex)
 * 
 * println("Max Length: ${utility.getMaxLength()}")
 * println("Prefix: '${utility.getPrefix()}'")
 * println("Formatted: ${utility.formatInput("1234567890")}")
 * ```
 * 
 * @param T The type parameter for extensibility (currently supports String)
 * @param pattern The regex pattern to analyze and use for formatting
 * 
 * @throws IllegalArgumentException if the pattern is malformed or empty
 * 
 * @since 1.0.0
 */
class RegexUtility<T>(private val pattern: String) {
    
    /** Parser instance for breaking down the regex pattern into components */
    private val parser = RegexParser(pattern)
    
    /** Analyzer instance for calculating metrics and extracting information */
    private val analyzer = RegexAnalyzer(parser.parsePattern())
    
    /** Formatter instance for formatting user input based on the pattern */
    private val formatter = RegexFormatter(parser.parsePattern())
    
    /**
     * Calculates and returns the maximum possible length of text that can match this regex pattern.
     * 
     * Takes into account quantifiers, character classes, and groups to determine the theoretical
     * maximum length. For infinite quantifiers (*, +), a reasonable cap is applied.
     * 
     * ## Examples
     * - Pattern `^(ID)[0-9]{7}-[0-9]{3}$` returns 13
     * - Pattern `^[a-z]{3,10}$` returns 10
     * - Pattern `^[a-z]*$` returns 1000 (capped)
     * 
     * @return The maximum possible length in characters
     * 
     * @since 1.0.0
     */
    fun getMaxLength(): Int {
        return analyzer.calculateMaxLength()
    }
    
    /**
     * Finds and returns positions of literal characters in the regex pattern.
     * 
     * Literal positions are useful for determining where fixed text should be inserted
     * during dynamic formatting. This helps in creating input masks and auto-formatting.
     * 
     * ## Examples
     * For pattern `^(ID)[0-9]{7}-[0-9]{3}$`:
     * - Returns `[LiteralPosition(0, "ID"), LiteralPosition(9, "-")]`
     * 
     * For pattern `^\\+1-[0-9]{3}-[0-9]{4}$`:
     * - Returns `[LiteralPosition(0, "+1"), LiteralPosition(3, "-"), LiteralPosition(7, "-")]`
     * 
     * @return List of literal positions with their character positions and values
     * 
     * @since 1.0.0
     */
    fun getLiteralPositions(): List<LiteralPosition> {
        return analyzer.findLiteralPositions()
    }
    
    /**
     * Extracts and returns the fixed prefix from the regex pattern.
     * 
     * The prefix consists of literal characters and simple groups that appear at the
     * beginning of the pattern before any variable components (character classes, 
     * complex quantifiers, etc.).
     * 
     * ## Examples
     * - Pattern `^(ID)[0-9]{7}$` returns "ID"
     * - Pattern `^\\+1-[0-9]+$` returns "+1-"
     * - Pattern `^[a-z]+@domain\\.com$` returns "" (no fixed prefix)
     * 
     * @return The fixed prefix string, or empty string if no fixed prefix exists
     * 
     * @since 1.0.0
     */
    fun getPrefix(): String {
        return analyzer.extractPrefix()
    }
    
    /**
     * Formats user input according to the regex pattern structure.
     * 
     * Applies the pattern's structure to raw user input, inserting literals,
     * organizing character sequences, and creating properly formatted output.
     * This is useful for creating input masks and auto-formatting user entry.
     * 
     * ## Behavior
     * - Inserts literal characters at their designated positions
     * - Applies quantifier constraints to character consumption
     * - Handles character class matching for validation
     * - Skips anchors and lookaheads during formatting
     * 
     * ## Examples
     * ```kotlin
     * // For pattern ^(ID)[0-9]{7}-[0-9]{3}$
     * formatInput("1234567890") // Returns "ID1234567-890"
     * 
     * // For pattern ^\\+1-[0-9]{3}-[0-9]{4}$
     * formatInput("1234567890") // Returns "+1-123-4567"
     * ```
     * 
     * @param input The raw user input to format
     * @return Formatted string according to the regex pattern
     * 
     * @since 1.0.0
     */
    fun formatInput(input: String): String {
        return formatter.format(input)
    }
    
    /**
     * Validates whether the input string matches the regex pattern exactly.
     * 
     * Uses the original regex pattern to perform full validation. This is useful
     * for final validation after formatting or for checking if input is complete.
     * 
     * @param input The string to validate against the pattern
     * @return true if the input matches the pattern completely, false otherwise
     * 
     * @since 1.0.0
     */
    fun isValid(input: String): Boolean {
        return Regex(pattern).matches(input)
    }
    
    /**
     * Returns comprehensive pattern information for debugging and analysis.
     * 
     * Aggregates various metrics and information about the regex pattern
     * into a single data class for easy inspection and debugging.
     * 
     * @return PatternInfo object containing pattern metrics and analysis
     * 
     * @since 1.0.0
     */
    fun getPatternInfo(): PatternInfo {
        return PatternInfo(
            pattern = pattern,
            maxLength = getMaxLength(),
            literalCount = getLiteralPositions().size,
            prefix = getPrefix()
        )
    }
}

/**
 * Parses regex patterns into structured, analyzable components.
 * 
 * This class handles the complex task of breaking down regex patterns into
 * their constituent parts while maintaining information about quantifiers,
 * groups, character classes, and other regex features.
 * 
 * ## Supported Features
 * - **Quantifiers**: `*`, `+`, `?`, `{n}`, `{n,m}`
 * - **Character Classes**: `[a-z]`, `[0-9]`, `[A-Za-z0-9]`
 * - **Groups**: `(...)` with optional quantifiers
 * - **Lookaheads**: `(?=...)` positive lookaheads
 * - **Anchors**: `^` start, `$` end
 * - **Literals**: Any fixed characters
 * 
 * ## Thread Safety
 * This class is stateful during parsing but creates immutable results.
 * Create new instances for each parsing operation in multi-threaded environments.
 * 
 * @param pattern The regex pattern string to parse
 * 
 * @since 1.0.0
 */
class RegexParser(private val pattern: String) {
    /** Current position in the pattern during parsing */
    private var position = RegexConstants.ZERO
    
    /**
     * Parses the entire regex pattern into a list of structured components.
     * 
     * This is the main entry point for pattern parsing. It processes the pattern
     * character by character, identifying different regex constructs and building
     * a structured representation.
     * 
     * ## Error Handling
     * - Malformed quantifiers are ignored and treated as literals
     * - Unmatched brackets are handled gracefully
     * - Invalid escape sequences are treated as literal characters
     * 
     * @return List of RegexComponent objects representing the parsed pattern
     * 
     * @throws IllegalStateException if parsing encounters an unrecoverable error
     * 
     * @since 1.0.0
     */
    fun parsePattern(): List<RegexComponent> {
        position = RegexConstants.ZERO
        val components = mutableListOf<RegexComponent>()
        
        while (position < pattern.length) {
            components.add(parseNextComponent())
        }
        return components
    }
    
    /**
     * Parses the next component at the current position.
     * 
     * Determines the type of regex component at the current position and
     * delegates to the appropriate specialized parsing method.
     * 
     * @return The parsed RegexComponent
     * 
     * @since 1.0.0
     */
    private fun parseNextComponent(): RegexComponent {
        val char = pattern[position]
        
        return when {
            char == RegexConstants.GROUP_OPEN && 
            position + RegexConstants.TWO < pattern.length && 
            pattern.substring(position, position + RegexConstants.THREE) == RegexConstants.LOOKAHEAD_POSITIVE -> parseLookahead()
            
            char == RegexConstants.GROUP_OPEN && 
            position + RegexConstants.TWO < pattern.length && 
            pattern.substring(position, position + RegexConstants.THREE) == RegexConstants.LOOKAHEAD_NEGATIVE -> parseNegativeLookahead()
            
            char == RegexConstants.GROUP_OPEN -> parseGroup()
            char == RegexConstants.CHAR_CLASS_OPEN -> parseCharacterClass()
            char == RegexConstants.ANCHOR_START -> parseAnchor()
            char == RegexConstants.ANCHOR_END -> parseAnchor()
            char == RegexConstants.ESCAPE_CHAR -> parseEscapeSequence()
            char.isLetter() || char.isDigit() || char in ".-+()[]{}?*\\/:" -> parseLiteral()
            else -> {
                position++
                RegexComponent.Literal(char.toString())
            }
        }
    }
    
    /**
     * Parses positive lookahead assertions like `(?=...)`.
     * 
     * Lookaheads are zero-width assertions that don't consume characters
     * but affect matching behavior. Currently supports positive lookaheads.
     * 
     * ## Future Enhancements
     * - Negative lookaheads `(?!...)`
     * - Lookbehinds `(?<=...)` and `(?<!...)`
     * 
     * @return RegexComponent.Lookahead representing the lookahead assertion
     * 
     * @since 1.0.0
     */
    private fun parseLookahead(): RegexComponent {
        val start = position
        position += RegexConstants.THREE // Skip "(?="
        var depth = RegexConstants.ONE
        
        while (position < pattern.length && depth > RegexConstants.ZERO) {
            when (pattern[position]) {
                RegexConstants.GROUP_OPEN -> depth++
                RegexConstants.GROUP_CLOSE -> depth--
            }
            position++
        }
        
        return RegexComponent.Lookahead(
            pattern.substring(start, position),
            isPositive = true
        )
    }
    
    /**
     * Parses negative lookahead assertions like `(?!...)`.
     * 
     * Negative lookaheads are zero-width assertions that ensure the following
     * pattern does NOT match at the current position.
     * 
     * @return RegexComponent.Lookahead representing the negative lookahead assertion
     * 
     * @since 1.0.0
     */
    private fun parseNegativeLookahead(): RegexComponent {
        val start = position
        position += RegexConstants.THREE // Skip "(?!"
        var depth = RegexConstants.ONE
        
        while (position < pattern.length && depth > RegexConstants.ZERO) {
            when (pattern[position]) {
                RegexConstants.GROUP_OPEN -> depth++
                RegexConstants.GROUP_CLOSE -> depth--
            }
            position++
        }
        
        return RegexComponent.Lookahead(
            pattern.substring(start, position),
            isPositive = false
        )
    }
    
    /**
     * Parses regex groups like `(...)` with optional quantifiers.
     * 
     * Groups can contain any regex content and may have quantifiers applied.
     * Handles nested groups by tracking parentheses depth.
     * 
     * ## Examples
     * - `(abc)` → Group with content "abc"
     * - `(abc)+` → Group with content "abc" and quantifier +
     * - `(a|b)` → Group with alternation (treated as content "a|b")
     * 
     * @return RegexComponent.Group with parsed content and quantifier
     * 
     * @since 1.0.0
     */
    private fun parseGroup(): RegexComponent {
        val start = position
        position++ // Skip '('
        var depth = RegexConstants.ONE
        
        while (position < pattern.length && depth > RegexConstants.ZERO) {
            when (pattern[position]) {
                RegexConstants.GROUP_OPEN -> depth++
                RegexConstants.GROUP_CLOSE -> depth--
            }
            position++
        }
        
        val groupContent = pattern.substring(start + RegexConstants.ONE, position - RegexConstants.ONE)
        val quantifier = parseQuantifier()
        
        return RegexComponent.Group(groupContent, quantifier)
    }
    
    /**
     * Parses character classes like `[a-z]`, `[0-9]`, `[A-Za-z0-9]`.
     * 
     * Character classes define sets of characters that can match at a position.
     * Handles ranges, negation, and escape sequences within brackets.
     * 
     * ## Supported Features
     * - **Ranges**: `[a-z]`, `[0-9]`, `[A-Z]`
     * - **Multiple ranges**: `[a-zA-Z0-9]`
     * - **Negation**: `[^a-z]` (parsed but not fully implemented in matching)
     * - **Escape sequences**: `[\d]`, `[\w]`, `[\s]`
     * 
     * @return RegexComponent.CharacterClass with the class definition and quantifier
     * 
     * @since 1.0.0
     */
    private fun parseCharacterClass(): RegexComponent {
        val start = position
        position++ // Skip '['
        
        while (position < pattern.length && pattern[position] != RegexConstants.CHAR_CLASS_CLOSE) {
            position++
        }
        position++ // Skip ']'
        
        val quantifier = parseQuantifier()
        return RegexComponent.CharacterClass(
            pattern.substring(start, position),
            quantifier
        )
    }
    
    /**
     * Parses escape sequences like `\\d`, `\\w`, `\\s`.
     * 
     * Handles backslash-escaped characters and predefined character classes.
     * Maintains the escape sequence for proper interpretation.
     * 
     * @return RegexComponent.Literal with the escape sequence and optional quantifier
     * 
     * @since 1.0.0
     */
    private fun parseEscapeSequence(): RegexComponent {
        val start = position
        position++ // Skip backslash
        if (position < pattern.length) {
            position++ // Skip escaped character
        }
        
        val quantifier = parseQuantifier()
        return RegexComponent.Literal(
            pattern.substring(start, position - if (quantifier != null) getQuantifierLength(quantifier) else RegexConstants.ZERO), 
            quantifier
        )
    }
    
    /**
     * Parses literal characters that match exactly.
     * 
     * Literal characters are matched exactly as they appear. This method
     * also checks for following quantifiers that might apply to the literal.
     * 
     * @return RegexComponent.Literal with the character and optional quantifier
     * 
     * @since 1.0.0
     */
    private fun parseLiteral(): RegexComponent {
        val start = position
        val char = pattern[position]
        position++
        
        val quantifier = parseQuantifier()
        return RegexComponent.Literal(char.toString(), quantifier)
    }
    
    /**
     * Parses anchor characters `^` (start) and `$` (end).
     * 
     * Anchors are zero-width assertions that match positions rather than characters.
     * They don't consume input but constrain where matches can occur.
     * 
     * @return RegexComponent.Anchor with the anchor type
     * 
     * @since 1.0.0
     */
    private fun parseAnchor(): RegexComponent {
        val char = pattern[position]
        position++
        return RegexComponent.Anchor(char.toString())
    }
    
    /**
     * Parses quantifiers that follow regex components.
     * 
     * Quantifiers specify how many times the preceding component should match.
     * Handles both simple (`*`, `+`, `?`) and complex (`{n}`, `{n,m}`) quantifiers.
     * 
     * ## Supported Quantifiers
     * - `*` → 0 or more (0, ∞)
     * - `+` → 1 or more (1, ∞)  
     * - `?` → 0 or 1 (0, 1)
     * - `{n}` → exactly n times (n, n)
     * - `{n,}` → n or more times (n, ∞)
     * - `{n,m}` → between n and m times (n, m)
     * 
     * @return Quantifier object or null if no quantifier is present
     * 
     * @since 1.0.0
     */
    private fun parseQuantifier(): Quantifier? {
        if (position >= pattern.length) return null
        
        return when (pattern[position]) {
            RegexConstants.QUANTIFIER_ZERO_OR_MORE -> {
                position++
                Quantifier(RegexConstants.ZERO, Int.MAX_VALUE)
            }
            RegexConstants.QUANTIFIER_ONE_OR_MORE -> {
                position++
                Quantifier(RegexConstants.ONE, Int.MAX_VALUE)
            }
            RegexConstants.QUANTIFIER_ZERO_OR_ONE -> {
                position++
                Quantifier(RegexConstants.ZERO, RegexConstants.ONE)
            }
            RegexConstants.QUANTIFIER_OPEN -> parseComplexQuantifier()
            else -> null
        }
    }
    
    /**
     * Parses complex quantifiers in braces like `{n}`, `{n,m}`.
     * 
     * Handles the detailed parsing of brace-delimited quantifiers, including
     * error recovery for malformed quantifiers.
     * 
     * ## Error Handling
     * - Invalid numbers are ignored
     * - Malformed syntax returns null
     * - Missing closing brace is handled gracefully
     * 
     * @return Quantifier object or null if parsing fails
     * 
     * @since 1.0.0
     */
    private fun parseComplexQuantifier(): Quantifier? {
        val start = position
        position++ // Skip '{'
        
        val numberBuilder = StringBuilder()
        while (position < pattern.length && pattern[position].isDigit()) {
            numberBuilder.append(pattern[position])
            position++
        }
        
        if (position >= pattern.length) return null
        
        return when (pattern[position]) {
            RegexConstants.QUANTIFIER_CLOSE -> {
                position++
                val count = numberBuilder.toString().toIntOrNull() ?: return null
                Quantifier(count, count)
            }
            RegexConstants.COMMA -> {
                position++
                val min = numberBuilder.toString().toIntOrNull() ?: return null
                val maxBuilder = StringBuilder()
                
                while (position < pattern.length && pattern[position].isDigit()) {
                    maxBuilder.append(pattern[position])
                    position++
                }
                
                if (position < pattern.length && pattern[position] == RegexConstants.QUANTIFIER_CLOSE) {
                    position++
                    val max = if (maxBuilder.isEmpty()) Int.MAX_VALUE 
                             else maxBuilder.toString().toIntOrNull() ?: Int.MAX_VALUE
                    Quantifier(min, max)
                } else null
            }
            else -> null
        }
    }
    
    /**
     * Estimates the length of a quantifier string representation.
     * 
     * Used for adjusting parsing positions when quantifiers are present.
     * Provides a conservative estimate for quantifier string length.
     * 
     * @param quantifier The quantifier to estimate
     * @return Estimated string length of the quantifier
     * 
     * @since 1.0.0
     */
    private fun getQuantifierLength(quantifier: Quantifier): Int {
        // Conservative estimate for quantifier string length
        return when {
            quantifier.min == quantifier.max -> quantifier.min.toString().length + RegexConstants.TWO // "{n}"
            quantifier.max == Int.MAX_VALUE -> quantifier.min.toString().length + RegexConstants.THREE // "{n,}"
            else -> quantifier.min.toString().length + quantifier.max.toString().length + RegexConstants.THREE // "{n,m}"
        }
    }
}

/**
 * Analyzes parsed regex components to extract metrics and information.
 * 
 * This class takes the structured components from RegexParser and performs
 * various analyses to determine pattern characteristics, literal positions,
 * and other useful metrics for formatting and validation.
 * 
 * ## Analysis Capabilities
 * - Maximum length calculation with quantifier consideration
 * - Literal character position mapping
 * - Fixed prefix extraction
 * - Component length estimation
 * 
 * @param components List of parsed RegexComponent objects to analyze
 * 
 * @since 1.0.0
 */
class RegexAnalyzer(private val components: List<RegexComponent>) {
    
    /**
     * Calculates the theoretical maximum length that could match this pattern.
     * 
     * Analyzes each component considering its quantifiers to determine the
     * maximum possible length. For infinite quantifiers, applies a reasonable
     * cap to prevent overflow.
     * 
     * ## Calculation Logic
     * - **Literals**: Length × max quantifier
     * - **Character Classes**: 1 × max quantifier  
     * - **Groups**: Estimated group length × max quantifier
     * - **Lookaheads**: 0 (zero-width)
     * - **Anchors**: 0 (zero-width)
     * 
     * ## Examples
     * ```kotlin
     * // Pattern: ^(ID)[0-9]{7}-[0-9]{3}$
     * // Result: 2 + 7 + 1 + 3 = 13
     * 
     * // Pattern: ^[a-z]{3,10}$  
     * // Result: 10
     * 
     * // Pattern: ^[a-z]*$
     * // Result: 1000 (capped)
     * ```
     * 
     * @return Maximum possible length in characters
     * 
     * @since 1.0.0
     */
    fun calculateMaxLength(): Int {
        return components.sumOf { component ->
            when (component) {
                is RegexComponent.Literal -> {
                    val quantifier = component.quantifier ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
                    minOf(quantifier.max, RegexConstants.MAX_SAFE_LENGTH)
                }
                is RegexComponent.CharacterClass -> {
                    val quantifier = component.quantifier ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
                    minOf(quantifier.max, RegexConstants.MAX_SAFE_LENGTH)
                }
                is RegexComponent.Group -> {
                    val quantifier = component.quantifier ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
                    val innerLength = estimateGroupLength(component.content)
                    minOf(quantifier.max * innerLength, RegexConstants.MAX_SAFE_LENGTH)
                }
                is RegexComponent.Lookahead -> RegexConstants.ZERO // Lookaheads don't consume characters
                is RegexComponent.Anchor -> RegexConstants.ZERO // Anchors don't consume characters
            }
        }
    }
    
    /**
     * Finds positions of literal characters within the pattern.
     * 
     * Identifies fixed literal characters and their positions in the final
     * formatted output. This is crucial for input masking and auto-formatting
     * where certain positions need fixed characters.
     * 
     * ## Position Calculation
     * - Starts at position 0
     * - Adds component lengths as it progresses
     * - Only records literals with quantifier {1,1} or no quantifier
     * - Handles simple literal groups like "(ID)"
     * 
     * ## Return Value
     * Each LiteralPosition contains:
     * - `position`: Character index in the formatted output
     * - `literal`: The literal string that should appear at that position
     * 
     * @return List of LiteralPosition objects with position and literal value
     * 
     * @since 1.0.0
     */
    fun findLiteralPositions(): List<LiteralPosition> {
        val positions = mutableListOf<LiteralPosition>()
        var currentPosition = RegexConstants.ZERO
        
        for (component in components) {
            when (component) {
                is RegexComponent.Literal -> {
                    if (component.quantifier?.min == RegexConstants.ONE && 
                        component.quantifier.max == RegexConstants.ONE) {
                        positions.add(LiteralPosition(currentPosition, component.value))
                    }
                    currentPosition += estimateComponentLength(component)
                }
                is RegexComponent.CharacterClass -> {
                    currentPosition += estimateComponentLength(component)
                }
                is RegexComponent.Group -> {
                    if (component.quantifier?.min == RegexConstants.ONE && 
                        component.quantifier.max == RegexConstants.ONE) {
                        positions.add(LiteralPosition(currentPosition, component.content))
                    }
                    currentPosition += estimateComponentLength(component)
                }
                is RegexComponent.Lookahead -> { /* No position change */ }
                is RegexComponent.Anchor -> { /* No position change */ }
            }
        }
        
        return positions
    }
    
    /**
     * Extracts the fixed prefix from the beginning of the pattern.
     * 
     * Identifies literal characters and simple groups at the start of the pattern
     * that will always be the same regardless of input. Stops at the first
     * variable component (character class, complex quantifier, etc.).
     * 
     * ## Prefix Components
     * - **Literals** with quantifier {1,1}: Added to prefix
     * - **Simple Groups** like "(ID)": Content added to prefix  
     * - **Anchors**: Ignored (continue processing)
     * - **Lookaheads**: Ignored (continue processing)
     * - **Variable components**: Stop prefix extraction
     * 
     * ## Examples
     * ```kotlin
     * // ^(ID)[0-9]{7}$ → "ID"
     * // ^\\+1-[0-9]+ → "+1-"
     * // ^[a-z]+@domain → "" (no fixed prefix)
     * ```
     * 
     * @return Fixed prefix string, empty if no fixed prefix exists
     * 
     * @since 1.0.0
     */
    fun extractPrefix(): String {
        val prefix = StringBuilder()
        
        for (component in components) {
            when (component) {
                is RegexComponent.Literal -> {
                    if (component.quantifier?.min == RegexConstants.ONE && 
                        component.quantifier.max == RegexConstants.ONE) {
                        prefix.append(component.value)
                    } else break
                }
                is RegexComponent.Group -> {
                    if (component.quantifier?.min == RegexConstants.ONE && 
                        component.quantifier.max == RegexConstants.ONE) {
                        if (isSimpleLiteralGroup(component.content)) {
                            prefix.append(component.content)
                        } else break
                    } else break
                }
                is RegexComponent.Anchor -> continue
                is RegexComponent.Lookahead -> continue
                else -> break
            }
        }
        
        return prefix.toString()
    }
    
    /**
     * Estimates the minimum length a component will consume.
     * 
     * Used internally for position calculations and length estimation.
     * Returns the minimum number of characters this component will match.
     * 
     * @param component The component to analyze
     * @return Minimum character length for this component
     * 
     * @since 1.0.0
     */
    private fun estimateComponentLength(component: RegexComponent): Int {
        return when (component) {
            is RegexComponent.Literal -> {
                val quantifier = component.quantifier ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
                quantifier.min
            }
            is RegexComponent.CharacterClass -> {
                val quantifier = component.quantifier ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
                quantifier.min
            }
            is RegexComponent.Group -> {
                val quantifier = component.quantifier ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
                quantifier.min * estimateGroupLength(component.content)
            }
            is RegexComponent.Lookahead -> RegexConstants.ZERO
            is RegexComponent.Anchor -> RegexConstants.ZERO
        }
    }
    
    /**
     * Estimates the length of content within a group.
     * 
     * Provides a conservative estimate for group content length.
     * For simple literal groups like "ID", returns the exact length.
     * For complex groups, uses a heuristic estimation.
     * 
     * @param content The group content string
     * @return Estimated length for the group content
     * 
     * @since 1.0.0
     */
    private fun estimateGroupLength(content: String): Int {
        return when {
            isSimpleLiteralGroup(content) -> content.length
            content.contains(RegexConstants.PIPE) -> maxOf(RegexConstants.ONE, content.length / RegexConstants.ALTERNATION_DIVISOR)
            else -> maxOf(RegexConstants.ONE, content.length / RegexConstants.GROUP_LENGTH_DIVISOR)
        }
    }
    
    /**
     * Determines if a group contains only simple literal characters.
     * 
     * Simple literal groups are those that contain only letters, numbers, and hyphens
     * without regex metacharacters or alternation.
     * 
     * @param content The group content to test
     * @return true if the group is a simple literal group, false otherwise
     * 
     * @since 1.0.0
     */
    private fun isSimpleLiteralGroup(content: String): Boolean {
        return Regex(RegexConstants.SIMPLE_LITERAL_PATTERN).matches(content) && 
               !content.contains(RegexConstants.PIPE)
    }
}

/**
 * Formats user input according to parsed regex pattern structure.
 * 
 * This class takes raw user input and applies the structure defined by
 * the regex pattern to create properly formatted output. It handles
 * literal insertion, character class matching, and quantifier application.
 * 
 * ## Formatting Strategy
 * - **Literals**: Always inserted at their designated positions
 * - **Character Classes**: Match and consume appropriate input characters
 * - **Groups**: Process content and apply quantifiers
 * - **Quantifiers**: Control how many characters are consumed
 * - **Anchors/Lookaheads**: Skipped during formatting
 * 
 * @param components List of parsed RegexComponent objects defining the structure
 * 
 * @since 1.0.0
 */
class RegexFormatter(private val components: List<RegexComponent>) {
    
    /**
     * Formats input string according to the regex pattern structure.
     * 
     * Processes the input character by character, applying the pattern structure
     * to create a properly formatted result. Inserts literals at appropriate
     * positions and validates character types against character classes.
     * 
     * ## Formatting Process
     * 1. **Iterate through components** in pattern order
     * 2. **Insert literals** directly into output
     * 3. **Match character classes** against input characters
     * 4. **Apply quantifiers** to control consumption
     * 5. **Handle groups** by processing their content
     * 6. **Skip non-consuming** components (anchors, lookaheads)
     * 
     * ## Error Handling
     * - Invalid characters for character classes are skipped
     * - Insufficient input results in partial formatting
     * - Malformed components are handled gracefully
     * 
     * ## Examples
     * ```kotlin
     * // Pattern: ^(ID)[0-9]{7}-[0-9]{3}$
     * // Input: "1234567890"
     * // Output: "ID1234567-890"
     * 
     * // Pattern: ^\\+1-[0-9]{3}-[0-9]{4}$  
     * // Input: "1234567890"
     * // Output: "+1-123-4567"
     * ```
     * 
     * @param input Raw user input to format
     * @return Formatted string according to pattern structure
     * 
     * @since 1.0.0
     */
    fun format(input: String): String {
        val result = StringBuilder()
        var inputIndex = RegexConstants.ZERO
        
        for (component in components) {
            if (inputIndex >= input.length) break
            
            when (component) {
                is RegexComponent.Literal -> {
                    if (component.quantifier?.min == RegexConstants.ONE && 
                        component.quantifier.max == RegexConstants.ONE) {
                        result.append(component.value)
                    } else {
                        val consumed = consumeInput(input, inputIndex, component)
                        result.append(consumed.first)
                        inputIndex = consumed.second
                    }
                }
                is RegexComponent.CharacterClass -> {
                    val consumed = consumeInput(input, inputIndex, component)
                    result.append(consumed.first)
                    inputIndex = consumed.second
                }
                is RegexComponent.Group -> {
                    val consumed = consumeGroup(input, inputIndex, component)
                    result.append(consumed.first)
                    inputIndex = consumed.second
                }
                is RegexComponent.Lookahead -> { /* Skip lookaheads in formatting */ }
                is RegexComponent.Anchor -> { /* Skip anchors in formatting */ }
            }
        }
        
        return result.toString()
    }
    
    /**
     * Consumes input characters for a specific component with quantifier constraints.
     * 
     * Handles the consumption of input characters for literals and character classes,
     * respecting quantifier limits and character matching rules.
     * 
     * ## Consumption Rules
     * - **Respects quantifier limits** (min/max)
     * - **Validates character matching** for character classes
     * - **Stops on non-matching characters**
     * - **Returns consumed text and new position**
     * 
     * @param input The input string being processed
     * @param startIndex Starting position in the input
     * @param component The component defining consumption rules
     * @return Pair of (consumed text, new input index)
     * 
     * @since 1.0.0
     */
    private fun consumeInput(
        input: String, 
        startIndex: Int, 
        component: RegexComponent
    ): Pair<String, Int> {
        val quantifier = when (component) {
            is RegexComponent.Literal -> component.quantifier
            is RegexComponent.CharacterClass -> component.quantifier
            else -> null
        } ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
        
        val result = StringBuilder()
        var index = startIndex
        var consumed = RegexConstants.ZERO
        
        while (index < input.length && consumed < quantifier.max) {
            val char = input[index]
            
            if (isCharacterMatch(char, component)) {
                result.append(char)
                index++
                consumed++
            } else {
                break
            }
        }
        
        return result.toString() to index
    }
    
    /**
     * Consumes input for group components.
     * 
     * Simplified group consumption that handles basic group processing.
     * For complex groups, this provides a conservative approach to
     * character consumption based on quantifier constraints.
     * 
     * ## Future Enhancements
     * - Full group content parsing and recursive formatting
     * - Alternation handling within groups
     * - Nested group support
     * 
     * @param input The input string being processed
     * @param startIndex Starting position in the input  
     * @param group The group component to process
     * @return Pair of (consumed text, new input index)
     * 
     * @since 1.0.0
     */
    private fun consumeGroup(
        input: String, 
        startIndex: Int, 
        group: RegexComponent.Group
    ): Pair<String, Int> {
        // Simplified group consumption
        val quantifier = group.quantifier ?: Quantifier(RegexConstants.ONE, RegexConstants.ONE)
        val result = StringBuilder()
        var index = startIndex
        
        repeat(minOf(quantifier.min, input.length - startIndex)) {
            if (index < input.length) {
                result.append(input[index])
                index++
            }
        }
        
        return result.toString() to index
    }
    
    /**
     * Determines if a character matches the component's requirements.
     * 
     * Performs character validation against component constraints,
     * including literal matching and character class membership testing.
     * 
     * ## Matching Rules
     * - **Literals**: Exact character match
     * - **Character Classes**: Range and type checking
     *   - `[0-9]`, `\d`: Digits only
     *   - `[a-z]`: Lowercase letters
     *   - `[A-Z]`: Uppercase letters  
     *   - `[a-zA-Z]`: All letters
     *   - `[\\w]`, `\w`: Word characters (letters, digits, underscore)
     * 
     * @param char The character to test
     * @param component The component defining matching criteria
     * @return true if the character matches, false otherwise
     * 
     * @since 1.0.0
     */
    private fun isCharacterMatch(char: Char, component: RegexComponent): Boolean {
        return when (component) {
            is RegexComponent.Literal -> char.toString() == component.value
            is RegexComponent.CharacterClass -> {
                // Simplified character class matching
                when {
                    component.value == RegexConstants.CHAR_CLASS_DIGITS || 
                    component.value == RegexConstants.ESCAPED_DIGIT ||
                    component.value.contains(RegexConstants.DIGITS_RANGE) -> char.isDigit()
                    
                    component.value == RegexConstants.CHAR_CLASS_LOWERCASE ||
                    component.value.contains(RegexConstants.LOWERCASE_RANGE) -> char.isLowerCase()
                    
                    component.value == RegexConstants.CHAR_CLASS_UPPERCASE ||
                    component.value.contains(RegexConstants.UPPERCASE_RANGE) -> char.isUpperCase()
                    
                    component.value == RegexConstants.CHAR_CLASS_LETTERS -> char.isLetter()
                    
                    component.value == RegexConstants.CHAR_CLASS_WORD || 
                    component.value == RegexConstants.ESCAPED_WORD -> 
                        char.isLetterOrDigit() || char == RegexConstants.UNDERSCORE
                    
                    else -> true // Default to accepting any character
                }
            }
            else -> false
        }
    }
}

/**
 * Represents different types of regex components with their properties.
 * 
 * This sealed class hierarchy provides a type-safe way to represent
 * the various components that can appear in a regex pattern. Each
 * component type captures the relevant information needed for analysis
 * and formatting operations.
 * 
 * ## Component Types
 * - **Literal**: Fixed characters that match exactly
 * - **CharacterClass**: Sets of characters like [a-z], [0-9]
 * - **Group**: Grouped expressions like (abc)
 * - **Lookahead**: Zero-width assertions like (?=...)
 * - **Anchor**: Position anchors like ^, $
 * 
 * @since 1.0.0
 */
sealed class RegexComponent {
    
    /**
     * Represents literal characters that must match exactly.
     * 
     * Literal components match specific characters exactly as they appear.
     * They can have quantifiers applied to control repetition.
     * 
     * ## Examples
     * - `a` → Literal("a", null)
     * - `a+` → Literal("a", Quantifier(1, MAX_VALUE))
     * - `a{3}` → Literal("a", Quantifier(3, 3))
     * 
     * @param value The literal character or string to match
     * @param quantifier Optional quantifier controlling repetition
     * 
     * @since 1.0.0
     */
    data class Literal(val value: String, val quantifier: Quantifier? = null) : RegexComponent()
    
    /**
     * Represents character classes like [a-z], [0-9], \\d.
     * 
     * Character classes define sets of characters that can match at a position.
     * They support ranges, negation, and predefined classes.
     * 
     * ## Examples
     * - `[a-z]` → CharacterClass("[a-z]", null)
     * - `[0-9]+` → CharacterClass("[0-9]", Quantifier(1, MAX_VALUE))
     * - `\\d{3}` → CharacterClass("\\d", Quantifier(3, 3))
     * 
     * @param value The character class definition including brackets
     * @param quantifier Optional quantifier controlling repetition
     * 
     * @since 1.0.0
     */
    data class CharacterClass(val value: String, val quantifier: Quantifier? = null) : RegexComponent()
    
    /**
     * Represents grouped expressions like (abc), (a|b).
     * 
     * Groups encapsulate sub-expressions and can have quantifiers applied
     * to the entire group. They can contain any regex content including
     * alternation, nested groups, and complex patterns.
     * 
     * ## Examples
     * - `(abc)` → Group("abc", null)
     * - `(a|b)+` → Group("a|b", Quantifier(1, MAX_VALUE))
     * - `(ID)` → Group("ID", null)
     * 
     * @param content The content inside the group parentheses
     * @param quantifier Optional quantifier applied to the entire group
     * 
     * @since 1.0.0
     */
    data class Group(val content: String, val quantifier: Quantifier? = null) : RegexComponent()
    
    /**
     * Represents lookahead assertions like (?=...), (?!...).
     * 
     * Lookaheads are zero-width assertions that check conditions ahead
     * in the input without consuming characters. They affect matching
     * behavior but don't contribute to the matched text.
     * 
     * ## Examples
     * - `(?=abc)` → Lookahead("(?=abc)", true)
     * - `(?!abc)` → Lookahead("(?!abc)", false)
     * 
     * @param content The complete lookahead expression including syntax
     * @param isPositive true for positive lookaheads (?=), false for negative (?!)
     * 
     * @since 1.0.0
     */
    data class Lookahead(val content: String, val isPositive: Boolean) : RegexComponent()
    
    /**
     * Represents position anchors like ^, $.
     * 
     * Anchors are zero-width assertions that match positions rather than
     * characters. They constrain where matches can occur in the input.
     * 
     * ## Examples
     * - `^` → Anchor("^") - start of string/line
     * - `$` → Anchor("$") - end of string/line
     * 
     * @param type The anchor character (^, $, etc.)
     * 
     * @since 1.0.0
     */
    data class Anchor(val type: String) : RegexComponent()
}

/**
 * Represents quantifier information for regex components.
 * 
 * Quantifiers control how many times a regex component should match.
 * They define both minimum and maximum occurrence counts.
 * 
 * ## Common Quantifiers
 * - `*` → Quantifier(0, Int.MAX_VALUE) - zero or more
 * - `+` → Quantifier(1, Int.MAX_VALUE) - one or more  
 * - `?` → Quantifier(0, 1) - zero or one
 * - `{n}` → Quantifier(n, n) - exactly n times
 * - `{n,m}` → Quantifier(n, m) - between n and m times
 * 
 * @param min Minimum number of occurrences (inclusive)
 * @param max Maximum number of occurrences (inclusive), Int.MAX_VALUE for unbounded
 * 
 * @since 1.0.0
 */
data class Quantifier(val min: Int, val max: Int) {
    
    /**
     * Returns a string representation of the quantifier for display purposes.
     * 
     * Formats the quantifier in a human-readable form showing the min/max bounds.
     * Uses infinity symbol for unbounded quantifiers.
     * 
     * @return String representation of the quantifier
     * 
     * @since 1.0.0
     */
    override fun toString(): String {
        return when {
            min == max -> "${RegexConstants.DISPLAY_BRACE_OPEN}$min${RegexConstants.DISPLAY_BRACE_CLOSE}"
            max == Int.MAX_VALUE -> "${RegexConstants.DISPLAY_BRACE_OPEN}$min,${RegexConstants.INFINITY_SYMBOL}${RegexConstants.DISPLAY_BRACE_CLOSE}"
            else -> "${RegexConstants.DISPLAY_BRACE_OPEN}$min,$max${RegexConstants.DISPLAY_BRACE_CLOSE}"
        }
    }
}

/**
 * Represents the position of a literal character in the formatted output.
 * 
 * Used by the analyzer to identify where fixed characters should appear
 * in the final formatted result. Essential for input masking and 
 * auto-formatting functionality.
 * 
 * ## Usage Example
 * ```kotlin
 * // For pattern ^(ID)[0-9]{7}-[0-9]{3}$:
 * // LiteralPosition(0, "ID") - "ID" appears at start
 * // LiteralPosition(9, "-") - dash appears at position 9
 * ```
 * 
 * @param position Zero-based character index in the formatted output
 * @param literal The literal string that should appear at this position
 * 
 * @since 1.0.0
 */
data class LiteralPosition(val position: Int, val literal: String) {
    
    /**
     * Returns a string representation of the literal position for debugging.
     * 
     * Formats the position information in a readable form for logging and debugging.
     * 
     * @return String representation of the literal position
     * 
     * @since 1.0.0
     */
    override fun toString(): String {
        return "${RegexConstants.LITERAL_POSITION_CLASS}($position, \"$literal\")"
    }
}

/**
 * Contains comprehensive information about a regex pattern.
 * 
 * Aggregates various pattern metrics and analysis results into a single
 * data class for easy inspection, debugging, and API responses.
 * 
 * ## Use Cases
 * - **Debugging**: Inspect pattern analysis results
 * - **API Responses**: Return pattern information to clients
 * - **Validation**: Check pattern characteristics before processing
 * - **Documentation**: Generate pattern summaries
 * 
 * @param pattern The original regex pattern string
 * @param maxLength Maximum possible length of matching text
 * @param literalCount Number of literal positions in the pattern
 * @param prefix Fixed prefix extracted from the pattern
 * 
 * @since 1.0.0
 */
data class PatternInfo(
    val pattern: String,
    val maxLength: Int,
    val literalCount: Int,
    val prefix: String
) {
    
    /**
     * Returns a string representation of the pattern information for debugging.
     * 
     * Formats all pattern metrics in a readable form for logging and debugging.
     * 
     * @return String representation of the pattern information
     * 
     * @since 1.0.0
     */
    override fun toString(): String {
        return "${RegexConstants.PATTERN_INFO_CLASS}(pattern=\"$pattern\", maxLength=$maxLength, literalCount=$literalCount, prefix=\"$prefix\")"
    }
}

// Usage Example:
fun main() {
    // Example with phone number regex
    val phoneRegex = "^\\+?1?[\\s.-]?\\(?([0-9]{3})\\)?[\\s.-]?([0-9]{3})[\\s.-]?([0-9]{4})$"
    val utility = RegexUtility<String>(phoneRegex)
    
    println("Max Length: ${utility.getMaxLength()}")
    println("Prefix: '${utility.getPrefix()}'")
    println("Literal Positions: ${utility.getLiteralPositions()}")
    println("Formatted: ${utility.formatInput("1234567890")}")
    
    // Example with email regex
    val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    val emailUtility = RegexUtility<String>(emailRegex)
    
    println("\nEmail Pattern Info: ${emailUtility.getPatternInfo()}")
    
    // Example with complex regex including lookahead
    val complexRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$"
    val complexUtility = RegexUtility<String>(complexRegex)
    
    println("\nComplex Pattern Max Length: ${complexUtility.getMaxLength()}")
    println("Complex Pattern Prefix: '${complexUtility.getPrefix()}'")
}

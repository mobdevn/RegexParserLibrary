/**
 * A sealed class representing a parsed component of a regular expression pattern.
 * This model separates the parsing logic from the formatting and analysis logic.
 */
sealed class RegexPart {
    /**
     * Represents a literal string that should appear as-is in the output (e.g., "-", "/", "ID").
     * @property value The literal string.
     */
    data class Literal(val value: String) : RegexPart()

    /**
     * Represents a placeholder for user input that corresponds to a part of the regex
     * that matches characters (e.g., "\\d{2}", "[A-Z]+", ".").
     *
     * @property length The maximum number of characters this placeholder consumes. -1 for unbounded.
     * @property token A string representing the character class (e.g., "\\d", "[A-Z]").
     */
    data class Placeholder(val length: Int, val token: String) : RegexPart()

    /**
     * Represents a zero-width assertion like an anchor or a lookaround.
     * These components match positions, not characters, and have no length.
     * (e.g., "^", "$", "(?=...)", "(?<!...)").
     *
     * @property value The string representation of the anchor.
     */
    data class Anchor(val value: String) : RegexPart()
}

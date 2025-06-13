fun extractMaxLength(regexString: String): Int? {
    // This regex finds {n,m} or {n} quantifiers.
    // It is composed of two main patterns separated by an OR operator (|):
    //
    // Pattern 1: `\\{\\s*\\d+\\s*,\\s*(\\d+)\\s*\\}`
    //   - Matches quantifiers of the form `{min,max}`, like `{2,14}`.
    //   - It allows for optional whitespace (`\\s*`) around the numbers and comma.
    //   - `(\\d+)`: This is the first capturing group. It specifically captures the 'max' number.
    //
    // Pattern 2: `\\{\\s*(\\d+)\\s*\\}`
    //   - Matches quantifiers of the form `{n}`, like `{10}`.
    //   - It also allows for optional whitespace.
    //   - `(\\d+)`: This is the second capturing group. It captures 'n', which represents both the minimum and maximum length.
    //
    val parserRegex = Regex("\\{\\s*\\d+\\s*,\\s*(\\d+)\\s*\\}|\\{\\s*(\\d+)\\s*\\}")

    // We search for the first occurrence of a pattern that matches our parser.
    // If no match is found (e.g., the regex is `[a-z]+`), we return null.
    val matchResult = parserRegex.find(regexString) ?: return null

    // When a match is found, we inspect the captured groups.
    // `matchResult.groupValues[1]` corresponds to the first capturing group (`max` from `{min,max}`).
    // `matchResult.groupValues[2]` corresponds to the second capturing group (`n` from `{n}`).
    // Only one of these groups will contain a value for any given match.
    //
    // We prioritize the first group. If it's not empty, we use it. Otherwise, we fall back to the second group.
    val maxLengthString = matchResult.groupValues[1].takeIf { it.isNotEmpty() }
        ?: matchResult.groupValues[2]

    // Convert the extracted string to an integer. Returns null if the string is not a valid number.
    return maxLengthString.toIntOrNull()
}

private fun extractLookaheadMaxLength(pattern: String): Int? {
    // Single comprehensive regex to match both range and single length patterns
    val lookaheadRegex = Regex("""\(\?\=\^\.\{(\d+)(?:,(\d+))?\}\$""")
    val match = lookaheadRegex.find(pattern)
    
    return if (match != null) {
        // If group 2 exists (range pattern), use it; otherwise use group 1 (single length)
        val maxLength = match.groupValues[2].takeIf { it.isNotEmpty() } ?: match.groupValues[1]
        maxLength.toIntOrNull()
    } else null
}

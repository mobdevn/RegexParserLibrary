# RegexParserLibrary
RegexParserLibrary

import kotlin.text.Regex

/**
 * Extracts the maximum length quantifier from a regex string.
 *
 * This function searches for curly brace quantifiers like {n,m} or {n}
 * and returns the maximum length specified. It handles optional spaces
 * within the braces.
 *
 * @param regexString The regular expression string to parse.
 * @return The maximum length as an Int, or null if no max length quantifier is found.
 */
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

/**
 * The main function demonstrates the usage of extractMaxLength
 * with various types of regular expressions.
 */
fun main() {
    val regexList = listOf(
        // Original case from the user
        "(?=^.{2,14}$)^([a-zA-Z][\\-\\'\\s]?)+$",
        // Case with an exact length specified
        "^[a-z]{10}$",
        // Case with spaces inside the quantifier
        "\\d{ 5, 20 }",
        // A complex regex with a quantifier in the middle
        "id-[0-9]{4,8}-end",
        // A regex that specifies a minimum but no maximum length
        "\\w{5,}",
        // A regex with no curly brace quantifier at all
        "^[a-zA-Z]+$"
    )

    println("--- Testing Regex Max Length Extractor ---")
    println("==========================================")
    regexList.forEach { regex ->
        val maxLength = extractMaxLength(regex)
        println("Regex: \"$regex\"")
        if (maxLength != null) {
            println(" -> Extracted Max Length: $maxLength")
        } else {
            println(" -> No max length found.")
        }
        println("------------------------------------------")
    }
}

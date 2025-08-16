import io.mockk.*

class SequentialPageRegexAnalyzerTest {

    private lateinit var analyzer: SequentialPageRegexAnalyzer

    fun setup() {
        clearAllMocks()
    }

    fun `calculateMaxLength returns calculated length when no lookahead`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "test"
        }
        val components = listOf(literal)
        analyzer = SequentialPageRegexAnalyzer(components)

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 4) // "test".length = 4
    }

    fun `calculateMaxLength returns minimum when lookahead has maxLength`() {
        // Given
        val lookahead = mockk<SequentialPageRegexComponent.Lookahead> {
            every { maxLength } returns 5
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "testlongvalue"
        }
        val components = listOf(lookahead, literal)
        analyzer = SequentialPageRegexAnalyzer(components)

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 5) // min(13, 5) = 5
    }

    fun `calculateMaxLength filters out lookaheads from main calculation`() {
        // Given
        val lookahead = mockk<SequentialPageRegexComponent.Lookahead> {
            every { maxLength } returns null
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "ab"
        }
        val components = listOf(lookahead, literal)
        analyzer = SequentialPageRegexAnalyzer(components)

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 2) // Only literal contributes
    }

    fun `calculateBranchMaxLength handles Alternation`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "ab"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "abcd"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(listOf(literal1), listOf(literal2))
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 4) // max(2, 4) = 4
    }

    fun `calculateBranchMaxLength handles Group with safe length quantifier`() {
        // Given
        val safeQuantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns Int.MAX_VALUE
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "a"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { quantifier } returns safeQuantifier
            every { components } returns listOf(literal)
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(group))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 1000) // MAX_SAFE_LENGTH
    }

    fun `calculateBranchMaxLength handles Group with normal quantifier`() {
        // Given
        val quantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns 3
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "ab"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { quantifier } returns quantifier
            every { components } returns listOf(literal)
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(group))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 6) // 2 * 3 = 6
    }

    fun `calculateBranchMaxLength handles Group with null quantifier`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "ab"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { quantifier } returns null
            every { components } returns listOf(literal)
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(group))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 2) // 2 * 1 = 2
    }

    fun `calculateBranchMaxLength handles CharacterClass with safe length quantifier`() {
        // Given
        val safeQuantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns Int.MAX_VALUE
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns safeQuantifier
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(charClass))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 1000) // MAX_SAFE_LENGTH
    }

    fun `calculateBranchMaxLength handles CharacterClass with normal quantifier`() {
        // Given
        val quantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns 5
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns quantifier
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(charClass))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 5) // 1 * 5 = 5
    }

    fun `calculateBranchMaxLength handles CharacterClass with null quantifier`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns null
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(charClass))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 1) // 1 * 1 = 1
    }

    fun `calculateBranchMaxLength handles Literal with safe length quantifier`() {
        // Given
        val safeQuantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns Int.MAX_VALUE
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns safeQuantifier
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 1000) // MAX_SAFE_LENGTH
    }

    fun `calculateBranchMaxLength handles Literal with normal quantifier`() {
        // Given
        val quantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns 2
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns quantifier
            every { value } returns "abc"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 6) // 3 * 2 = 6
    }

    fun `calculateBranchMaxLength handles Anchor and Lookahead components`() {
        // Given
        val anchor = mockk<SequentialPageRegexComponent.Anchor>()
        val lookahead = mockk<SequentialPageRegexComponent.Lookahead> {
            every { maxLength } returns null
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(anchor, lookahead, literal))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 4) // 0 + 0 + 4 = 4
    }

    fun `calculateBranchMaxLength handles unknown component types`() {
        // Given
        val unknownComponent = mockk<SequentialPageRegexComponent>()
        analyzer = SequentialPageRegexAnalyzer(listOf(unknownComponent))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 0) // REGEX_DEFAULT_LENGTH
    }

    fun `calculateBranchMaxLength returns 0 for empty Alternation branches`() {
        // Given
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns emptyList()
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 0) // REGEX_DEFAULT_LENGTH when maxOfOrNull returns null
    }

    fun `findLiteralPositions finds positions for basic quantifiers`() {
        // Given
        val basicQuantifier = SequentialPageRegexQuantifier(1, 1)
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns basicQuantifier
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 1)
        assert(result[0].position == 0)
        assert(result[0].literal == "test")
    }

    fun `findLiteralPositions finds positions for null quantifiers`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "hello"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 1)
        assert(result[0].position == 0)
        assert(result[0].literal == "hello")
    }

    fun `findLiteralPositions skips literals with non-basic quantifiers`() {
        // Given
        val nonBasicQuantifier = mockk<SequentialPageRegexQuantifier> {
            every { min } returns 2
            every { max } returns 5
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns nonBasicQuantifier
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 0)
    }

    fun `findLiteralPositions handles Group with basic quantifier recursively`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "inner"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { quantifier } returns null
            every { components } returns listOf(literal)
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(group))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 1)
        assert(result[0].position == 0)
        assert(result[0].literal == "inner")
    }

    fun `findLiteralPositions skips Group with non-basic quantifier`() {
        // Given
        val nonBasicQuantifier = mockk<SequentialPageRegexQuantifier> {
            every { min } returns 2
            every { max } returns 3
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "inner"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { quantifier } returns nonBasicQuantifier
            every { components } returns listOf(literal)
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(group))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 0)
    }

    fun `findLiteralPositions handles Alternation components`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "alt"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(listOf(literal))
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 0) // Alternations don't contribute to literal positions
    }

    fun `findLiteralPositions handles Anchor and Lookahead components`() {
        // Given
        val anchor = mockk<SequentialPageRegexComponent.Anchor>()
        val lookahead = mockk<SequentialPageRegexComponent.Lookahead> {
            every { maxLength } returns null
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(anchor, lookahead))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 0) // Zero-width components don't affect positions
    }

    fun `findLiteralPositions handles unknown component types`() {
        // Given
        val unknownComponent = mockk<SequentialPageRegexComponent>()
        analyzer = SequentialPageRegexAnalyzer(listOf(unknownComponent))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 0)
    }

    fun `findLiteralPositions calculates correct positions for multiple components`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "ab"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "cd"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal1, literal2))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 2)
        assert(result[0].position == 0)
        assert(result[0].literal == "ab")
        assert(result[1].position == 2)
        assert(result[1].literal == "cd")
    }

    fun `extractPrefix extracts from Literal without quantifier`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "prefix"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "prefix")
    }

    fun `extractPrefix stops at Literal with quantifier`() {
        // Given
        val quantifier = mockk<SequentialPageRegexQuantifier>()
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "start"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns quantifier
            every { value } returns "stop"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal1, literal2))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "start")
    }

    fun `extractPrefix extracts from Group without quantifier`() {
        // Given
        val innerLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "inner"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { quantifier } returns null
            every { components } returns listOf(innerLiteral)
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(group))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "inner")
    }

    fun `extractPrefix stops at Group with quantifier`() {
        // Given
        val quantifier = mockk<SequentialPageRegexQuantifier>()
        val innerLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "inner"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { quantifier } returns quantifier
            every { components } returns listOf(innerLiteral)
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(group))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "")
    }

    fun `extractPrefix handles Alternation and finds common prefix`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "common"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "com"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(listOf(literal1), listOf(literal2))
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "com") // Common prefix of "common" and "com"
    }

    fun `extractPrefix skips Anchor components`() {
        // Given
        val anchor = mockk<SequentialPageRegexComponent.Anchor>()
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(anchor, literal))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "test")
    }

    fun `extractPrefix stops at unknown component types`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "before"
        }
        val unknownComponent = mockk<SequentialPageRegexComponent>()
        analyzer = SequentialPageRegexAnalyzer(listOf(literal, unknownComponent))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "before")
    }

    fun `extractPrefix returns empty string for empty components`() {
        // Given
        analyzer = SequentialPageRegexAnalyzer(emptyList())

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "")
    }

    fun `getCommonPrefix returns empty string for empty alternation branches`() {
        // Given - Test indirectly through alternation
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns emptyList()
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "")
    }

    fun `getCommonPrefix returns common prefix of multiple strings`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "testing"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "test"
        }
        val literal3 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "tester"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(listOf(literal1), listOf(literal2), listOf(literal3))
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "test")
    }

    fun `getCommonPrefix returns shortest string when complete prefix`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "a"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "abc"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(listOf(literal1), listOf(literal2))
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "a")
    }

    fun `getCommonPrefix returns empty string when no common prefix`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "abc"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "def"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(listOf(literal1), listOf(literal2))
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(alternation))

        // When
        val result = analyzer.extractPrefix()

        // Then
        assert(result == "")
    }

    fun `getMaxQuantifier returns max value when quantifier exists`() {
        // Given
        val quantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns 5
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns quantifier
            every { value } returns "a"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 5) // 1 * 5 = 5
    }

    fun `getMaxQuantifier returns REGEX_MAX_LENGTH when quantifier is null`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "a"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 1) // 1 * 1 = 1 (REGEX_MAX_LENGTH = 1)
    }

    fun `checkForSafeLength returns true when max is MAX_VALUE`() {
        // Given
        val safeQuantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns Int.MAX_VALUE
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns safeQuantifier
            every { value } returns "a"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 1000) // MAX_SAFE_LENGTH
    }

    fun `checkForSafeLength returns false when max is not MAX_VALUE`() {
        // Given
        val normalQuantifier = mockk<SequentialPageRegexQuantifier> {
            every { max } returns 3
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns normalQuantifier
            every { value } returns "a"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.calculateMaxLength()

        // Then
        assert(result == 3) // 1 * 3 = 3 (normal quantifier)
    }

    fun `isBasicQuantifier returns true for null quantifier`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns null
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 1) // Literal is included because quantifier is basic
    }

    fun `isBasicQuantifier returns true for basic quantifier`() {
        // Given
        val basicQuantifier = SequentialPageRegexQuantifier(1, 1)
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns basicQuantifier
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 1) // Literal is included because quantifier is basic
    }

    fun `isBasicQuantifier returns false for non-basic quantifier`() {
        // Given
        val nonBasicQuantifier = SequentialPageRegexQuantifier(2, 3)
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { quantifier } returns nonBasicQuantifier
            every { value } returns "test"
        }
        analyzer = SequentialPageRegexAnalyzer(listOf(literal))

        // When
        val result = analyzer.findLiteralPositions()

        // Then
        assert(result.size == 0) // Literal is not included because quantifier is not basic
    }
}

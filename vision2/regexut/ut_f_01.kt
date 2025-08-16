import io.mockk.*

class SequentialPageRegexFormatterTest {

    private lateinit var formatter: SequentialPageRegexFormatter

    fun setup() {
        clearAllMocks()
    }

    // Real regex pattern tests
    fun `formats complex ID pattern correctly`() {
        // Given - Pattern: "^(ID)[0-9]{7}-[0-9]{3}$"
        val anchor1 = mockk<SequentialPageRegexComponent.Anchor>()
        val idLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "ID"
        }
        val idGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(idLiteral)
        }
        val digits1 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(7, 7)
        }
        val dashLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val digits2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val anchor2 = mockk<SequentialPageRegexComponent.Anchor>()
        val components = listOf(anchor1, idGroup, digits1, dashLiteral, digits2, anchor2)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("1234567890")

        // Then
        assert(result == "ID1234567-890")
    }

    fun `formats phone number pattern correctly`() {
        // Given - Pattern: "+1-(555)-123-4567"
        val plusLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "+"
        }
        val oneLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "1"
        }
        val dash1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val openParen = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "("
        }
        val areaCode = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val closeParen = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns ")"
        }
        val dash2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val exchange = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val dash3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val number = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val components = listOf(plusLiteral, oneLiteral, dash1, openParen, areaCode, closeParen, dash2, exchange, dash3, number)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("5551234567")

        // Then
        assert(result == "+1-(555)-123-4567")
    }

    fun `formats email pattern with alternation correctly`() {
        // Given - Pattern with alternation: "(gmail|yahoo|hotmail)@"
        val gmailLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "gmail"
        }
        val yahooLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "yahoo"
        }
        val hotmailLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "hotmail"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(gmailLiteral),
                listOf(yahooLiteral),
                listOf(hotmailLiteral)
            )
        }
        val domainGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(alternation)
        }
        val atSymbol = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "@"
        }
        val components = listOf(domainGroup, atSymbol)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("testuser")

        // Then
        assert(result == "hotmail@") // Should pick the longest matching branch
    }

    fun `formats date pattern with groups correctly`() {
        // Given - Pattern: "([0-9]{2})/([0-9]{2})/([0-9]{4})"
        val month = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val monthGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(month)
        }
        val slash1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "/"
        }
        val day = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val dayGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(day)
        }
        val slash2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "/"
        }
        val year = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val yearGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(year)
        }
        val components = listOf(monthGroup, slash1, dayGroup, slash2, yearGroup)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("12252024")

        // Then
        assert(result == "12/25/2024")
    }

    fun `formats pattern with variable length character class`() {
        // Given - Pattern with variable quantifier: "[A-Z][a-z]{2,8}"
        val firstChar = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns null
        }
        val restChars = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 8)
        }
        val components = listOf(firstChar, restChars)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("HelloWorld")

        // Then
        assert(result == "HHelloWor") // First char + max 8 chars from quantifier
    }

    fun `formats credit card pattern with spaces`() {
        // Given - Pattern: "[0-9]{4} [0-9]{4} [0-9]{4} [0-9]{4}"
        val group1 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val space1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns " "
        }
        val group2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val space2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns " "
        }
        val group3 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val space3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns " "
        }
        val group4 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val components = listOf(group1, space1, group2, space2, group3, space3, group4)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("1234567890123456")

        // Then
        assert(result == "1234 5678 9012 3456")
    }

    fun `formats pattern with optional groups`() {
        // Given - Pattern with optional group: "Mr\\.? [A-Z][a-z]+"
        val title = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "Mr"
        }
        val dot = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "."
        }
        val space = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns " "
        }
        val firstChar = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 1)
        }
        val restChars = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, Int.MAX_VALUE)
        }
        val components = listOf(title, dot, space, firstChar, restChars)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("Smith")

        // Then
        assert(result == "Mr. Smith") // Should format with title and name
    }

    // Edge case and component-specific tests
    fun `format returns formatted string from formatBranch`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "test"
        }
        val components = listOf(literal)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "test")
    }

    fun `formatBranch handles empty branch list`() {
        // Given
        val components = emptyList<SequentialPageRegexComponent>()
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "")
    }

    fun `formatBranch processes multiple components sequentially`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "Hello"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "World"
        }
        val components = listOf(literal1, literal2)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "HelloWorld")
    }

    fun `formatBranch handles input consumption correctly`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val charClass2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val components = listOf(charClass, literal, charClass2)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("12345")

        // Then
        assert(result == "123-45")
    }

    fun `formatBranch handles zero consumption without updating remaining input`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "prefix"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val components = listOf(literal, charClass)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abc")

        // Then
        assert(result == "prefixab")
    }

    fun `formatBranch handles consumption greater than remaining input length`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(10, 10)
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "end"
        }
        val components = listOf(charClass, literal)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abc")

        // Then
        assert(result == "abcend") // Should consume all available input
    }

    fun `formatComponent handles Alternation with multiple branches`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "short"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "verylongtext"
        }
        val literal3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "medium"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(literal1),
                listOf(literal2),
                listOf(literal3)
            )
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "verylongtext") // Should pick branch with highest consumption
    }

    fun `formatComponent handles Alternation with no branches`() {
        // Given
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns emptyList()
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "") // Should return default values
    }

    fun `formatComponent handles Group component`() {
        // Given
        val innerLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "grouped"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(innerLiteral)
        }
        formatter = SequentialPageRegexFormatter(listOf(group))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "grouped")
    }

    fun `formatComponent handles Literal component`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "literal"
        }
        formatter = SequentialPageRegexFormatter(listOf(literal))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "literal")
    }

    fun `formatComponent handles CharacterClass with null quantifier`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns null
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("hello")

        // Then
        assert(result == "h") // Should consume 1 character (INCREMENT_CONSTANT)
    }

    fun `formatComponent handles CharacterClass with specific quantifier`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 5)
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("hello")

        // Then
        assert(result == "hello") // Should consume max 5 characters, but input is only 5
    }

    fun `formatComponent handles CharacterClass with quantifier max greater than input`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 10)
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("abc")

        // Then
        assert(result == "abc") // Should consume all available input
    }

    fun `formatComponent handles CharacterClass with empty input`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 3)
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("")

        // Then
        assert(result == "") // Should return empty string for empty input
    }

    fun `formatComponent handles unknown component type`() {
        // Given
        val unknownComponent = mockk<SequentialPageRegexComponent>()
        formatter = SequentialPageRegexFormatter(listOf(unknownComponent))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "") // Should return default values for unknown component
    }

    fun `formatComponent Alternation handles equal consumption branches`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "same1"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "same2"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(literal1),
                listOf(literal2)
            )
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "same1" || result == "same2") // Either is acceptable for equal consumption
    }

    fun `formatBranch handles complex nested structure`() {
        // Given
        val innerLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "nested"
        }
        val innerGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(innerLiteral)
        }
        val outerGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(innerGroup)
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val components = listOf(outerGroup, charClass)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("hello")

        // Then
        assert(result == "nestedhe")
    }

    fun `formatBranch handles multiple CharacterClass components with consumption`() {
        // Given
        val charClass1 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val charClass2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val charClass3 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 1)
        }
        val components = listOf(charClass1, charClass2, charClass3)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abcdefgh")

        // Then
        assert(result == "abcdefg") // 2 + 3 + 1 = 6 characters consumed
    }

    fun `formatBranch handles Alternation with nested Groups`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "option1"
        }
        val group1 = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(literal1)
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "option2longer"
        }
        val group2 = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(literal2)
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(group1),
                listOf(group2)
            )
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "option2longer") // Should pick the branch with higher consumption
    }

    fun `formatBranch with mixed component types and consumption patterns`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "start"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "middle"
        }
        val charClass2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val literal3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "end"
        }
        val components = listOf(literal1, charClass, literal2, charClass2, literal3)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abcdefghijk")

        // Then
        assert(result == "startabcmiddledeend")
    }

    fun `formatComponent handles all component types in sequence`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "lit"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 1)
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(
                mockk<SequentialPageRegexComponent.Literal> {
                    every { value } returns "grp"
                }
            )
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(mockk<SequentialPageRegexComponent.Literal> {
                    every { value } returns "alt"
                })
            )
        }
        val components = listOf(literal, charClass, group, alternation)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("x")

        // Then
        assert(result == "litxgrpalt")
    }

    fun `formatBranch handles edge case with zero length input`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "test"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(5, 5)
        }
        val components = listOf(literal, charClass)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("")

        // Then
        assert(result == "test") // Literal should still be added, charClass gets empty string
    }

    fun `formatBranch handles consumption edge case with exact input length`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(5, 5)
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "end"
        }
        val components = listOf(charClass, literal)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("12345")

        // Then
        assert(result == "12345end") // Should consume exactly all input
    }

    fun `formatComponent handles Alternation with single branch`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "single"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(listOf(literal))
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "single")
    }

    fun `formatComponent handles nested alternations`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "a"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "bb"
        }
        val innerAlternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(literal1),
                listOf(literal2)
            )
        }
        val literal3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "ccc"
        }
        val outerAlternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(innerAlternation),
                listOf(literal3)
            )
        }
        formatter = SequentialPageRegexFormatter(listOf(outerAlternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "ccc") // Should pick the branch with highest consumption
    }

    fun `formatBranch handles all zero consumption components`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "a"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "b"
        }
        val literal3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "c"
        }
        val components = listOf(literal1, literal2, literal3)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "abc") // All literals should be concatenated
    }
}import io.mockk.*

class SequentialPageRegexFormatterTest {

    private lateinit var formatter: SequentialPageRegexFormatter

    fun setup() {
        clearAllMocks()
    }

    // Real regex pattern tests
    fun `formats complex ID pattern correctly`() {
        // Given - Pattern: "^(ID)[0-9]{7}-[0-9]{3}$"
        val anchor1 = mockk<SequentialPageRegexComponent.Anchor>()
        val idLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "ID"
        }
        val idGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(idLiteral)
        }
        val digits1 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(7, 7)
        }
        val dashLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val digits2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val anchor2 = mockk<SequentialPageRegexComponent.Anchor>()
        val components = listOf(anchor1, idGroup, digits1, dashLiteral, digits2, anchor2)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("1234567890")

        // Then
        assert(result == "ID1234567-890")
    }

    fun `formats phone number pattern correctly`() {
        // Given - Pattern: "+1-(555)-123-4567"
        val plusLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "+"
        }
        val oneLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "1"
        }
        val dash1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val openParen = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "("
        }
        val areaCode = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val closeParen = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns ")"
        }
        val dash2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val exchange = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val dash3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val number = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val components = listOf(plusLiteral, oneLiteral, dash1, openParen, areaCode, closeParen, dash2, exchange, dash3, number)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("5551234567")

        // Then
        assert(result == "+1-(555)-123-4567")
    }

    fun `formats email pattern with alternation correctly`() {
        // Given - Pattern with alternation: "(gmail|yahoo|hotmail)@"
        val gmailLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "gmail"
        }
        val yahooLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "yahoo"
        }
        val hotmailLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "hotmail"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(gmailLiteral),
                listOf(yahooLiteral),
                listOf(hotmailLiteral)
            )
        }
        val domainGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(alternation)
        }
        val atSymbol = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "@"
        }
        val components = listOf(domainGroup, atSymbol)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("testuser")

        // Then
        assert(result == "hotmail@") // Should pick the longest matching branch
    }

    fun `formats date pattern with groups correctly`() {
        // Given - Pattern: "([0-9]{2})/([0-9]{2})/([0-9]{4})"
        val month = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val monthGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(month)
        }
        val slash1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "/"
        }
        val day = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val dayGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(day)
        }
        val slash2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "/"
        }
        val year = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(4, 4)
        }
        val yearGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(year)
        }
        val components = listOf(monthGroup, slash1, dayGroup, slash2, yearGroup)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("12252024")

        // Then
        assert(result == "12/25/2024")
    }

    fun `formats pattern with variable length character class`() {
        // Given - Pattern with variable quantifier: "[A-Z][a-z]{2,8}"
        val firstChar = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns null
        }
        val restChars = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 8)
        }
        val components = listOf(firstChar, restChars)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("HelloWorld")

        // Then
        assert(result == "HHelloWor") // First char + max 8 chars from quantifier
    }

    // Edge case and component-specific tests
    fun `format returns formatted string from formatBranch`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "test"
        }
        val components = listOf(literal)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "test")
    }

    fun `formatBranch handles empty branch list`() {
        // Given
        val components = emptyList<SequentialPageRegexComponent>()
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "")
    }

    fun `formatBranch processes multiple components sequentially`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "Hello"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "World"
        }
        val components = listOf(literal1, literal2)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "HelloWorld")
    }

    fun `formatBranch handles input consumption correctly`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "-"
        }
        val charClass2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val components = listOf(charClass, literal, charClass2)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("12345")

        // Then
        assert(result == "123-45")
    }

    fun `formatBranch handles zero consumption without updating remaining input`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "prefix"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val components = listOf(literal, charClass)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abc")

        // Then
        assert(result == "prefixab")
    }

    fun `formatBranch handles consumption greater than remaining input length`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(10, 10)
        }
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "end"
        }
        val components = listOf(charClass, literal)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abc")

        // Then
        assert(result == "abcend") // Should consume all available input
    }

    fun `formatComponent handles Alternation with multiple branches`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "short"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "verylongtext"
        }
        val literal3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "medium"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(literal1),
                listOf(literal2),
                listOf(literal3)
            )
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "verylongtext") // Should pick branch with highest consumption
    }

    fun `formatComponent handles Alternation with no branches`() {
        // Given
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns emptyList()
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "") // Should return default values
    }

    fun `formatComponent handles Group component`() {
        // Given
        val innerLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "grouped"
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(innerLiteral)
        }
        formatter = SequentialPageRegexFormatter(listOf(group))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "grouped")
    }

    fun `formatComponent handles Literal component`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "literal"
        }
        formatter = SequentialPageRegexFormatter(listOf(literal))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "literal")
    }

    fun `formatComponent handles CharacterClass with null quantifier`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns null
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("hello")

        // Then
        assert(result == "h") // Should consume 1 character (INCREMENT_CONSTANT)
    }

    fun `formatComponent handles CharacterClass with specific quantifier`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 5)
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("hello")

        // Then
        assert(result == "hello") // Should consume max 5 characters, but input is only 5
    }

    fun `formatComponent handles CharacterClass with quantifier max greater than input`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 10)
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("abc")

        // Then
        assert(result == "abc") // Should consume all available input
    }

    fun `formatComponent handles CharacterClass with empty input`() {
        // Given
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 3)
        }
        formatter = SequentialPageRegexFormatter(listOf(charClass))

        // When
        val result = formatter.format("")

        // Then
        assert(result == "") // Should return empty string for empty input
    }

    fun `formatComponent handles unknown component type`() {
        // Given
        val unknownComponent = mockk<SequentialPageRegexComponent>()
        formatter = SequentialPageRegexFormatter(listOf(unknownComponent))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "") // Should return default values for unknown component
    }

    fun `formatComponent Alternation handles equal consumption branches`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "same1"
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "same2"
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(literal1),
                listOf(literal2)
            )
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "same1" || result == "same2") // Either is acceptable for equal consumption
    }

    fun `formatBranch handles complex nested structure`() {
        // Given
        val innerLiteral = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "nested"
        }
        val innerGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(innerLiteral)
        }
        val outerGroup = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(innerGroup)
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val components = listOf(outerGroup, charClass)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("hello")

        // Then
        assert(result == "nestedhe")
    }

    fun `formatBranch handles multiple CharacterClass components with consumption`() {
        // Given
        val charClass1 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val charClass2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val charClass3 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 1)
        }
        val components = listOf(charClass1, charClass2, charClass3)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abcdefgh")

        // Then
        assert(result == "abcdefg") // 2 + 3 + 1 = 6 characters consumed
    }

    fun `formatBranch handles Alternation with nested Groups`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "option1"
        }
        val group1 = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(literal1)
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "option2longer"
        }
        val group2 = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(literal2)
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(group1),
                listOf(group2)
            )
        }
        formatter = SequentialPageRegexFormatter(listOf(alternation))

        // When
        val result = formatter.format("input")

        // Then
        assert(result == "option2longer") // Should pick the branch with higher consumption
    }

    fun `formatBranch with mixed component types and consumption patterns`() {
        // Given
        val literal1 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "start"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(3, 3)
        }
        val literal2 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "middle"
        }
        val charClass2 = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(2, 2)
        }
        val literal3 = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "end"
        }
        val components = listOf(literal1, charClass, literal2, charClass2, literal3)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("abcdefghijk")

        // Then
        assert(result == "startabcmiddledeend")
    }

    fun `formatComponent handles all component types in sequence`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "lit"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(1, 1)
        }
        val group = mockk<SequentialPageRegexComponent.Group> {
            every { components } returns listOf(
                mockk<SequentialPageRegexComponent.Literal> {
                    every { value } returns "grp"
                }
            )
        }
        val alternation = mockk<SequentialPageRegexComponent.Alternation> {
            every { branches } returns listOf(
                listOf(mockk<SequentialPageRegexComponent.Literal> {
                    every { value } returns "alt"
                })
            )
        }
        val components = listOf(literal, charClass, group, alternation)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("x")

        // Then
        assert(result == "litxgrpalt")
    }

    fun `formatBranch handles edge case with zero length input`() {
        // Given
        val literal = mockk<SequentialPageRegexComponent.Literal> {
            every { value } returns "test"
        }
        val charClass = mockk<SequentialPageRegexComponent.CharacterClass> {
            every { quantifier } returns SequentialPageRegexQuantifier(5, 5)
        }
        val components = listOf(literal, charClass)
        formatter = SequentialPageRegexFormatter(components)

        // When
        val result = formatter.format("")

        // Then
        assert(result == "test") // Literal should still be added, charClass gets empty string
    }
}

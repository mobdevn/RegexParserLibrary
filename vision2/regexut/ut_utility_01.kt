import io.mockk.*

class SequentialPageRegexUtilityTest {

    private lateinit var utility: SequentialPageRegexUtility
    private lateinit var mockParser: SequentialPageRegexParser
    private lateinit var mockAnalyzer: SequentialPageRegexAnalyzer
    private lateinit var mockFormatter: SequentialPageRegexFormatter

    fun setup() {
        clearAllMocks()
        
        // Mock the parser to return predefined components
        mockParser = mockk<SequentialPageRegexParser>()
        mockAnalyzer = mockk<SequentialPageRegexAnalyzer>()
        mockFormatter = mockk<SequentialPageRegexFormatter>()
    }

    fun `getMaxLength delegates to analyzer correctly`() {
        // Given
        val pattern = "^(ID)[0-9]{7}-[0-9]{3}$"
        val expectedMaxLength = 13
        
        // Mock components for the pattern
        val components = listOf(
            mockk<SequentialPageRegexComponent.Anchor>(),
            mockk<SequentialPageRegexComponent.Group>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Anchor>()
        )

        // Use mockkConstructor to mock the internal components
        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns expectedMaxLength

        utility = SequentialPageRegexUtility(pattern)

        // When
        val result = utility.getMaxLength()

        // Then
        assert(result == expectedMaxLength)
        verify { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() }
    }

    fun `getLiteralPositions delegates to analyzer correctly`() {
        // Given
        val pattern = "^(ID)[0-9]{7}-[0-9]{3}$"
        val expectedPositions = listOf(
            SequentialPageRegexLiteralPosition(0, "ID"),
            SequentialPageRegexLiteralPosition(9, "-")
        )
        
        val components = listOf(mockk<SequentialPageRegexComponent.Literal>())

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns expectedPositions

        utility = SequentialPageRegexUtility(pattern)

        // When
        val result = utility.getLiteralPositions()

        // Then
        assert(result.size == 2)
        assert(result[0].position == 0)
        assert(result[0].literal == "ID")
        assert(result[1].position == 9)
        assert(result[1].literal == "-")
        verify { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() }
    }

    fun `getPrefix delegates to analyzer correctly`() {
        // Given
        val pattern = "^(ID)[0-9]{7}-[0-9]{3}$"
        val expectedPrefix = "ID"
        
        val components = listOf(mockk<SequentialPageRegexComponent.Group>())

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns expectedPrefix

        utility = SequentialPageRegexUtility(pattern)

        // When
        val result = utility.getPrefix()

        // Then
        assert(result == expectedPrefix)
        verify { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() }
    }

    fun `formatInput delegates to formatter correctly`() {
        // Given
        val pattern = "^(ID)[0-9]{7}-[0-9]{3}$"
        val input = "1234567890"
        val expectedFormatted = "ID1234567-890"
        
        val components = listOf(mockk<SequentialPageRegexComponent.Group>())

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns expectedFormatted

        utility = SequentialPageRegexUtility(pattern)

        // When
        val result = utility.formatInput(input)

        // Then
        assert(result == expectedFormatted)
        verify { anyConstructed<SequentialPageRegexFormatter>().format(input) }
    }

    fun `isValid uses Regex matches correctly for valid input`() {
        // Given
        val pattern = "^[0-9]{3}-[0-9]{2}-[0-9]{4}$"
        val validInput = "123-45-6789"
        
        val components = listOf(mockk<SequentialPageRegexComponent.CharacterClass>())

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components

        utility = SequentialPageRegexUtility(pattern)

        // When
        val result = utility.isValid(validInput)

        // Then
        assert(result == true)
    }

    fun `isValid uses Regex matches correctly for invalid input`() {
        // Given
        val pattern = "^[0-9]{3}-[0-9]{2}-[0-9]{4}$"
        val invalidInput = "invalid-input"
        
        val components = listOf(mockk<SequentialPageRegexComponent.CharacterClass>())

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components

        utility = SequentialPageRegexUtility(pattern)

        // When
        val result = utility.isValid(invalidInput)

        // Then
        assert(result == false)
    }

    fun `utility initialization creates all required components`() {
        // Given
        val pattern = "test-pattern"
        val components = listOf(mockk<SequentialPageRegexComponent.Literal>())

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components

        // When
        utility = SequentialPageRegexUtility(pattern)

        // Then
        verify { SequentialPageRegexParser(pattern) }
        verify { anyConstructed<SequentialPageRegexParser>().parse() }
        verify { SequentialPageRegexAnalyzer(components) }
        verify { SequentialPageRegexFormatter(components) }
    }

    fun `all methods work with complex pattern`() {
        // Given
        val pattern = "(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])[A-Za-z0-9]{8,20}"
        val input = "TestPass123"
        val components = listOf(
            mockk<SequentialPageRegexComponent.Lookahead>(),
            mockk<SequentialPageRegexComponent.Lookahead>(),
            mockk<SequentialPageRegexComponent.Lookahead>(),
            mockk<SequentialPageRegexComponent.CharacterClass>()
        )
        val expectedMaxLength = 20
        val expectedPrefix = ""
        val expectedPositions = emptyList<SequentialPageRegexLiteralPosition>()
        val expectedFormatted = "TestPass123"

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns expectedMaxLength
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns expectedPositions
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns expectedPrefix
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns expectedFormatted

        utility = SequentialPageRegexUtility(pattern)

        // When & Then
        assert(utility.getMaxLength() == expectedMaxLength)
        assert(utility.getLiteralPositions().isEmpty())
        assert(utility.getPrefix() == expectedPrefix)
        assert(utility.formatInput(input) == expectedFormatted)
        assert(utility.isValid(input) == true)
    }

    fun `utility handles email pattern correctly`() {
        // Given
        val pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        val input = "test@example.com"
        val components = listOf(
            mockk<SequentialPageRegexComponent.Anchor>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Anchor>()
        )
        val expectedMaxLength = 50
        val expectedPositions = listOf(
            SequentialPageRegexLiteralPosition(10, "@"),
            SequentialPageRegexLiteralPosition(18, ".")
        )
        val expectedPrefix = ""
        val expectedFormatted = "test@example.com"

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns expectedMaxLength
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns expectedPositions
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns expectedPrefix
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns expectedFormatted

        utility = SequentialPageRegexUtility(pattern)

        // When & Then
        assert(utility.getMaxLength() == expectedMaxLength)
        assert(utility.getLiteralPositions().size == 2)
        assert(utility.getLiteralPositions()[0].literal == "@")
        assert(utility.getLiteralPositions()[1].literal == ".")
        assert(utility.getPrefix() == expectedPrefix)
        assert(utility.formatInput(input) == expectedFormatted)
        assert(utility.isValid(input) == true)
    }

    fun `utility handles phone pattern correctly`() {
        // Given
        val pattern = "^\\+1-\\([0-9]{3}\\)-[0-9]{3}-[0-9]{4}$"
        val input = "5551234567"
        val components = listOf(
            mockk<SequentialPageRegexComponent.Anchor>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Anchor>()
        )
        val expectedMaxLength = 17
        val expectedPositions = listOf(
            SequentialPageRegexLiteralPosition(0, "+"),
            SequentialPageRegexLiteralPosition(1, "1"),
            SequentialPageRegexLiteralPosition(2, "-"),
            SequentialPageRegexLiteralPosition(3, "("),
            SequentialPageRegexLiteralPosition(7, ")"),
            SequentialPageRegexLiteralPosition(8, "-"),
            SequentialPageRegexLiteralPosition(12, "-")
        )
        val expectedPrefix = "+1-("
        val expectedFormatted = "+1-(555)-123-4567"

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns expectedMaxLength
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns expectedPositions
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns expectedPrefix
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns expectedFormatted

        utility = SequentialPageRegexUtility(pattern)

        // When & Then
        assert(utility.getMaxLength() == expectedMaxLength)
        assert(utility.getLiteralPositions().size == 7)
        assert(utility.getPrefix() == expectedPrefix)
        assert(utility.formatInput(input) == expectedFormatted)
        assert(utility.isValid("+1-(555)-123-4567") == true)
    }

    fun `utility handles alternation pattern correctly`() {
        // Given
        val pattern = "^(Mr|Mrs|Dr)\\. [A-Z][a-z]+$"
        val input = "Smith"
        val components = listOf(
            mockk<SequentialPageRegexComponent.Anchor>(),
            mockk<SequentialPageRegexComponent.Group>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.CharacterClass>(),
            mockk<SequentialPageRegexComponent.Anchor>()
        )
        val expectedMaxLength = 1000 // MAX_SAFE_LENGTH due to unlimited quantifier
        val expectedPositions = listOf(
            SequentialPageRegexLiteralPosition(2, "."),
            SequentialPageRegexLiteralPosition(3, " ")
        )
        val expectedPrefix = ""
        val expectedFormatted = "Mr. Smith"

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns expectedMaxLength
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns expectedPositions
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns expectedPrefix
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns expectedFormatted

        utility = SequentialPageRegexUtility(pattern)

        // When & Then
        assert(utility.getMaxLength() == expectedMaxLength)
        assert(utility.getLiteralPositions().size == 2)
        assert(utility.getPrefix() == expectedPrefix)
        assert(utility.formatInput(input) == expectedFormatted)
        assert(utility.isValid("Mr. Smith") == true)
    }

    fun `utility handles edge cases correctly`() {
        // Given
        val pattern = ""
        val input = ""
        val components = emptyList<SequentialPageRegexComponent>()

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns 0
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns emptyList()
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns ""
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns ""

        utility = SequentialPageRegexUtility(pattern)

        // When & Then
        assert(utility.getMaxLength() == 0)
        assert(utility.getLiteralPositions().isEmpty())
        assert(utility.getPrefix() == "")
        assert(utility.formatInput(input) == "")
        assert(utility.isValid("") == true) // Empty pattern matches empty string
    }

    fun `utility handles date pattern with groups`() {
        // Given
        val pattern = "^([0-9]{2})/([0-9]{2})/([0-9]{4})$"
        val input = "12252024"
        val components = listOf(
            mockk<SequentialPageRegexComponent.Anchor>(),
            mockk<SequentialPageRegexComponent.Group>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.Group>(),
            mockk<SequentialPageRegexComponent.Literal>(),
            mockk<SequentialPageRegexComponent.Group>(),
            mockk<SequentialPageRegexComponent.Anchor>()
        )
        val expectedMaxLength = 10
        val expectedPositions = listOf(
            SequentialPageRegexLiteralPosition(2, "/"),
            SequentialPageRegexLiteralPosition(5, "/")
        )
        val expectedPrefix = ""
        val expectedFormatted = "12/25/2024"

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns expectedMaxLength
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns expectedPositions
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns expectedPrefix
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns expectedFormatted

        utility = SequentialPageRegexUtility(pattern)

        // When & Then
        assert(utility.getMaxLength() == expectedMaxLength)
        assert(utility.getLiteralPositions().size == 2)
        assert(utility.getPrefix() == expectedPrefix)
        assert(utility.formatInput(input) == expectedFormatted)
        assert(utility.isValid("12/25/2024") == true)
    }

    fun `utility handles pattern with lookaheads`() {
        // Given
        val pattern = "(?=.*[A-Z])(?=.*[0-9])[A-Za-z0-9]{6,10}"
        val input = "Password123"
        val components = listOf(
            mockk<SequentialPageRegexComponent.Lookahead>(),
            mockk<SequentialPageRegexComponent.Lookahead>(),
            mockk<SequentialPageRegexComponent.CharacterClass>()
        )
        val expectedMaxLength = 10 // Limited by character class quantifier max
        val expectedPositions = emptyList<SequentialPageRegexLiteralPosition>()
        val expectedPrefix = ""
        val expectedFormatted = "Password12"

        mockkConstructor(SequentialPageRegexParser::class)
        mockkConstructor(SequentialPageRegexAnalyzer::class)
        mockkConstructor(SequentialPageRegexFormatter::class)

        every { anyConstructed<SequentialPageRegexParser>().parse() } returns components
        every { anyConstructed<SequentialPageRegexAnalyzer>().calculateMaxLength() } returns expectedMaxLength
        every { anyConstructed<SequentialPageRegexAnalyzer>().findLiteralPositions() } returns expectedPositions
        every { anyConstructed<SequentialPageRegexAnalyzer>().extractPrefix() } returns expectedPrefix
        every { anyConstructed<SequentialPageRegexFormatter>().format(input) } returns expectedFormatted

        utility = SequentialPageRegexUtility(pattern)

        // When & Then
        assert(utility.getMaxLength() == expectedMaxLength)
        assert(utility.getLiteralPositions().isEmpty())
        assert(utility.getPrefix() == expectedPrefix)
        assert(utility.formatInput(input) == expectedFormatted)
        assert(utility.isValid("Password123") == true)
    }

    fun tearDown() {
        // Clean up constructor mocks
        unmockkConstructor(SequentialPageRegexParser::class)
        unmockkConstructor(SequentialPageRegexAnalyzer::class)
        unmockkConstructor(SequentialPageRegexFormatter::class)
        clearAllMocks()
    }
}

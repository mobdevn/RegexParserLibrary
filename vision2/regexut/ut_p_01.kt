
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SequentialPageRegexParserTest {

    private lateinit var parser: SequentialPageRegexParser

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `parse empty pattern returns empty list`() {
        parser = SequentialPageRegexParser("")
        val result = parser.parse()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse simple literal returns literal component`() {
        parser = SequentialPageRegexParser("a")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Literal)
        assertEquals("a", (result[0] as SequentialPageRegexComponent.Literal).value)
        assertNull((result[0] as SequentialPageRegexComponent.Literal).quantifier)
    }

    @Test
    fun `parse multiple literals returns multiple literal components`() {
        parser = SequentialPageRegexParser("abc")
        val result = parser.parse()
        
        assertEquals(3, result.size)
        assertTrue(result.all { it is SequentialPageRegexComponent.Literal })
        assertEquals("a", (result[0] as SequentialPageRegexComponent.Literal).value)
        assertEquals("b", (result[1] as SequentialPageRegexComponent.Literal).value)
        assertEquals("c", (result[2] as SequentialPageRegexComponent.Literal).value)
    }

    @Test
    fun `parse literal with zero or more quantifier`() {
        parser = SequentialPageRegexParser("a*")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNotNull(literal.quantifier)
        assertEquals(0, literal.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, literal.quantifier!!.max)
    }

    @Test
    fun `parse literal with one or more quantifier`() {
        parser = SequentialPageRegexParser("a+")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNotNull(literal.quantifier)
        assertEquals(1, literal.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, literal.quantifier!!.max)
    }

    @Test
    fun `parse literal with zero or one quantifier`() {
        parser = SequentialPageRegexParser("a?")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNotNull(literal.quantifier)
        assertEquals(0, literal.quantifier!!.min)
        assertEquals(1, literal.quantifier!!.max)
    }

    @Test
    fun `parse literal with exact count quantifier`() {
        parser = SequentialPageRegexParser("a{3}")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNotNull(literal.quantifier)
        assertEquals(3, literal.quantifier!!.min)
        assertEquals(3, literal.quantifier!!.max)
    }

    @Test
    fun `parse literal with range quantifier`() {
        parser = SequentialPageRegexParser("a{2,5}")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNotNull(literal.quantifier)
        assertEquals(2, literal.quantifier!!.min)
        assertEquals(5, literal.quantifier!!.max)
    }

    @Test
    fun `parse literal with open-ended range quantifier`() {
        parser = SequentialPageRegexParser("a{2,}")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNotNull(literal.quantifier)
        assertEquals(2, literal.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, literal.quantifier!!.max)
    }

    @Test
    fun `parse invalid quantifier returns null quantifier`() {
        parser = SequentialPageRegexParser("a{abc}")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNull(literal.quantifier)
    }

    @Test
    fun `parse incomplete quantifier returns literal with remaining text`() {
        parser = SequentialPageRegexParser("a{3")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals("a", literal.value)
        assertNull(literal.quantifier)
    }

    @Test
    fun `parse character class returns character class component`() {
        parser = SequentialPageRegexParser("[0-9]")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.CharacterClass)
        assertEquals("[0-9]", (result[0] as SequentialPageRegexComponent.CharacterClass).value)
    }

    @Test
    fun `parse character class with quantifier`() {
        parser = SequentialPageRegexParser("[0-9]+")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val charClass = result[0] as SequentialPageRegexComponent.CharacterClass
        assertEquals("[0-9]", charClass.value)
        assertNotNull(charClass.quantifier)
        assertEquals(1, charClass.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, charClass.quantifier!!.max)
    }

    @Test
    fun `parse incomplete character class returns literal`() {
        parser = SequentialPageRegexParser("[0-9")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Literal)
        assertEquals("[0-9", (result[0] as SequentialPageRegexComponent.Literal).value)
    }

    @Test
    fun `parse group returns group component`() {
        parser = SequentialPageRegexParser("(abc)")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Group)
        val group = result[0] as SequentialPageRegexComponent.Group
        assertEquals(3, group.components.size)
        assertTrue(group.components.all { it is SequentialPageRegexComponent.Literal })
    }

    @Test
    fun `parse group with quantifier`() {
        parser = SequentialPageRegexParser("(abc)*")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val group = result[0] as SequentialPageRegexComponent.Group
        assertNotNull(group.quantifier)
        assertEquals(0, group.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, group.quantifier!!.max)
    }

    @Test
    fun `parse nested groups`() {
        parser = SequentialPageRegexParser("((a)b)")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val outerGroup = result[0] as SequentialPageRegexComponent.Group
        assertEquals(2, outerGroup.components.size)
        assertTrue(outerGroup.components[0] is SequentialPageRegexComponent.Group)
        assertTrue(outerGroup.components[1] is SequentialPageRegexComponent.Literal)
    }

    @Test
    fun `parse incomplete group handles missing closing parenthesis`() {
        parser = SequentialPageRegexParser("(abc")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Group)
        val group = result[0] as SequentialPageRegexComponent.Group
        assertEquals(3, group.components.size)
    }

    @Test
    fun `parse escape sequences returns character class for special sequences`() {
        parser = SequentialPageRegexParser("\\d")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.CharacterClass)
        assertEquals("\\d", (result[0] as SequentialPageRegexComponent.CharacterClass).value)
    }

    @Test
    fun `parse escape sequences returns literal for regular escapes`() {
        parser = SequentialPageRegexParser("\\.")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Literal)
        assertEquals("\\.", (result[0] as SequentialPageRegexComponent.Literal).value)
    }

    @Test
    fun `parse escape at end of pattern`() {
        parser = SequentialPageRegexParser("a\\")
        val result = parser.parse()
        
        assertEquals(2, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Literal)
        assertTrue(result[1] is SequentialPageRegexComponent.Literal)
        assertEquals("a", (result[0] as SequentialPageRegexComponent.Literal).value)
        assertEquals("\\", (result[1] as SequentialPageRegexComponent.Literal).value)
    }

    @Test
    fun `parse incomplete escape sequence at end`() {
        parser = SequentialPageRegexParser("\\")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Literal)
        assertEquals("\\", (result[0] as SequentialPageRegexComponent.Literal).value)
    }

    @Test
    fun `parse start anchor`() {
        parser = SequentialPageRegexParser("^abc")
        val result = parser.parse()
        
        assertEquals(4, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Anchor)
        assertEquals("^", (result[0] as SequentialPageRegexComponent.Anchor).type)
    }

    @Test
    fun `parse end anchor`() {
        parser = SequentialPageRegexParser("abc$")
        val result = parser.parse()
        
        assertEquals(4, result.size)
        assertTrue(result[3] is SequentialPageRegexComponent.Anchor)
        assertEquals("$", (result[3] as SequentialPageRegexComponent.Anchor).type)
    }

    @Test
    fun `parse alternation returns alternation component`() {
        parser = SequentialPageRegexParser("a|b")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Alternation)
        val alternation = result[0] as SequentialPageRegexComponent.Alternation
        assertEquals(2, alternation.branches.size)
        assertEquals(1, alternation.branches[0].size)
        assertEquals(1, alternation.branches[1].size)
    }

    @Test
    fun `parse complex alternation with multiple branches`() {
        parser = SequentialPageRegexParser("a|b|c")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val alternation = result[0] as SequentialPageRegexComponent.Alternation
        assertEquals(3, alternation.branches.size)
    }

    @Test
    fun `parse alternation with groups`() {
        parser = SequentialPageRegexParser("(ab)|(cd)")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val alternation = result[0] as SequentialPageRegexComponent.Alternation
        assertEquals(2, alternation.branches.size)
        assertTrue(alternation.branches[0][0] is SequentialPageRegexComponent.Group)
        assertTrue(alternation.branches[1][0] is SequentialPageRegexComponent.Group)
    }

    @Test
    fun `parse lookahead returns lookahead component`() {
        parser = SequentialPageRegexParser("(?=abc)")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Lookahead)
        assertEquals("abc", (result[0] as SequentialPageRegexComponent.Lookahead).content)
    }

    @Test
    fun `parse lookahead with quantifier information`() {
        parser = SequentialPageRegexParser("(?=abc{2,5})")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val lookahead = result[0] as SequentialPageRegexComponent.Lookahead
        assertEquals("abc{2,5}", lookahead.content)
        assertEquals(2, lookahead.minLength)
        assertEquals(5, lookahead.maxLength)
    }

    @Test
    fun `parse lookahead with nested groups`() {
        parser = SequentialPageRegexParser("(?=(a(b)c))")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val lookahead = result[0] as SequentialPageRegexComponent.Lookahead
        assertEquals("(a(b)c)", lookahead.content)
    }

    @Test
    fun `parse incomplete lookahead`() {
        parser = SequentialPageRegexParser("(?=abc")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Lookahead)
        assertEquals("abc", (result[0] as SequentialPageRegexComponent.Lookahead).content)
    }

    @Test
    fun `parse complex pattern with all components`() {
        parser = SequentialPageRegexParser("^(ID)[0-9]{7}-[0-9]{3}$")
        val result = parser.parse()
        
        assertEquals(6, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Anchor) // ^
        assertTrue(result[1] is SequentialPageRegexComponent.Group) // (ID)
        assertTrue(result[2] is SequentialPageRegexComponent.CharacterClass) // [0-9]{7}
        assertTrue(result[3] is SequentialPageRegexComponent.Literal) // -
        assertTrue(result[4] is SequentialPageRegexComponent.CharacterClass) // [0-9]{3}
        assertTrue(result[5] is SequentialPageRegexComponent.Anchor) // $
    }

    @Test
    fun `parse pattern with escape sequences and quantifiers`() {
        parser = SequentialPageRegexParser("\\d+\\.\\w*")
        val result = parser.parse()
        
        assertEquals(3, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.CharacterClass) // \d+
        assertTrue(result[1] is SequentialPageRegexComponent.Literal) // \.
        assertTrue(result[2] is SequentialPageRegexComponent.CharacterClass) // \w*
        
        // Verify quantifiers
        val digitClass = result[0] as SequentialPageRegexComponent.CharacterClass
        assertEquals(1, digitClass.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, digitClass.quantifier!!.max)
        
        val wordClass = result[2] as SequentialPageRegexComponent.CharacterClass
        assertEquals(0, wordClass.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, wordClass.quantifier!!.max)
    }

    @Test
    fun `parse pattern at end with no quantifier`() {
        parser = SequentialPageRegexParser("a")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertNull(literal.quantifier)
    }

    @Test
    fun `parse quantifier with empty max value`() {
        parser = SequentialPageRegexParser("a{2,}")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        val literal = result[0] as SequentialPageRegexComponent.Literal
        assertEquals(2, literal.quantifier!!.min)
        assertEquals(Int.MAX_VALUE, literal.quantifier!!.max)
    }

    @Test
    fun `parse multiple escape sequences`() {
        parser = SequentialPageRegexParser("\\d\\w\\s")
        val result = parser.parse()
        
        assertEquals(3, result.size)
        assertTrue(result.all { it is SequentialPageRegexComponent.CharacterClass })
        assertEquals("\\d", (result[0] as SequentialPageRegexComponent.CharacterClass).value)
        assertEquals("\\w", (result[1] as SequentialPageRegexComponent.CharacterClass).value)
        assertEquals("\\s", (result[2] as SequentialPageRegexComponent.CharacterClass).value)
    }

    @Test
    fun `parse alternation with empty branches`() {
        parser = SequentialPageRegexParser("|")
        val result = parser.parse()
        
        assertEquals(1, result.size)
        assertTrue(result[0] is SequentialPageRegexComponent.Alternation)
        val alternation = result[0] as SequentialPageRegexComponent.Alternation
        assertEquals(2, alternation.branches.size)
        assertTrue(alternation.branches[0].isEmpty())
        assertTrue(alternation.branches[1].isEmpty())
    }

    @Test
    fun `parse single branch does not create alternation`() {
        parser = SequentialPageRegexParser("abc")
        val result = parser.parse()
        
        assertEquals(3, result.size)
        assertTrue(result.none { it is SequentialPageRegexComponent.Alternation })
    }
}

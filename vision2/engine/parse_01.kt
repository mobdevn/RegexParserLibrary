internal class SequentialPageRegexParser(private val pattern: String) {
    private var position = INITIAL_POSITION

    fun parse(): List<SequentialPageRegexComponent> {
        position = INITIAL_POSITION
        return parseUntil(emptySet())
    }

    private fun parseUntil(endChars: Set<Char>): List<SequentialPageRegexComponent> =
        mutableListOf<MutableList<SequentialPageRegexComponent>>().apply {
            add(mutableListOf())
        }.also { branches ->
            while (position < pattern.length && pattern[position] !in endChars) {
                when (pattern[position]) {
                    PIPE -> {
                        position++
                        branches.add(mutableListOf())
                    }
                    else -> branches.last().add(parseNextComponent())
                }
            }
        }.let { branches ->
            if (branches.size > INCREMENT_CONSTANT) {
                listOf(Alternation(branches))
            } else {
                branches.first()
            }
        }

    private fun parseNextComponent(): SequentialPageRegexComponent =
        pattern[position].let { currentChar ->
            when {
                pattern.startsWith(LOOKAHEAD_POSITIVE, position) -> parseLookahead()
                currentChar == GROUP_OPEN -> parseGroup()
                currentChar == CHAR_CLASS_OPEN -> parseCharacterClass()
                currentChar == ESCAPE_CHAR -> parseEscapeSequence()
                currentChar in setOf(ANCHOR_START, ANCHOR_END) -> parseAnchor()
                else -> parseLiteral()
            }
        }

    private fun parseLookahead(): SequentialPageRegexComponent {
        position += LOOK_AHEAD_SKIP_INDEX // Skip "(?="
        
        return mutableListOf<Char>().apply {
            var depth = 1
            while (position < pattern.length && depth > 0) {
                when (pattern[position]) {
                    GROUP_OPEN -> depth++
                    GROUP_CLOSE -> depth--
                }
                if (depth > 0) add(pattern[position])
                position++
            }
        }.let { chars ->
            val content = chars.joinToString("")
            
            // Extract quantifier information from content using regex
            val quantifierRegex = "\\{(\\d+),(\\d+)\\}".toRegex()
            quantifierRegex.find(content)?.let { match ->
                val (min, max) = match.destructured
                Lookahead(content, min.toIntOrNull(), max.toIntOrNull())
            } ?: Lookahead(content)
        }
    }

    private fun parseGroup(): SequentialPageRegexComponent {
        position++ // Skip '('
        
        return parseUntil(setOf(GROUP_CLOSE)).also {
            if (position < pattern.length && pattern[position] == GROUP_CLOSE) {
                position++ // Consume ')'
            }
        }.let { innerComponents ->
            Group(innerComponents, parseQuantifier())
        }
    }

    private fun parseCharacterClass(): SequentialPageRegexComponent =
        position.let { start ->
            pattern.indexOf(CHAR_CLASS_CLOSE, position)
                .takeIf { it != CONTENT_END }
                ?.also { end -> position = end + INCREMENT_CONSTANT }
                ?.let { end -> pattern.substring(start, position) }
                ?: pattern.substring(start).also { position = pattern.length }
        }.let { value ->
            SequentialPageRegexComponent.CharacterClass(value, parseQuantifier())
        }

    private fun parseEscapeSequence(): SequentialPageRegexComponent =
        if (position + INCREMENT_CONSTANT >= pattern.length) {
            pattern.substring(position).also { position++ }
        } else {
            pattern.substring(position, position + SECOND_INDEX).also { position += SECOND_INDEX }
        }.let { value ->
            val quantifier = parseQuantifier()
            when (value) {
                "\\d", "\\w", "\\s" -> SequentialPageRegexComponent.CharacterClass(value, quantifier)
                else -> Literal(value, quantifier)
            }
        }

    private fun parseLiteral(): SequentialPageRegexComponent =
        pattern[position].toString().also { position++ }.let { value ->
            Literal(value, parseQuantifier())
        }

    private fun parseAnchor(): SequentialPageRegexComponent =
        pattern[position].toString().also { position++ }.let { type ->
            Anchor(type)
        }

    private fun parseQuantifier(): SequentialPageRegexQuantifier? =
        if (position >= pattern.length) null
        else when (pattern[position]) {
            QUANTIFIER_ZERO_OR_MORE -> {
                position++
                SequentialPageRegexQuantifier(ZERO, Int.MAX_VALUE)
            }
            QUANTIFIER_ONE_OR_MORE -> {
                position++
                SequentialPageRegexQuantifier(INCREMENT_CONSTANT, Int.MAX_VALUE)
            }
            QUANTIFIER_ZERO_OR_ONE -> {
                position++
                SequentialPageRegexQuantifier(ZERO, INCREMENT_CONSTANT)
            }
            QUANTIFIER_OPEN -> parseComplexQuantifier()
            else -> null
        }

    private fun parseComplexQuantifier(): SequentialPageRegexQuantifier? =
        pattern.indexOf(QUANTIFIER_CLOSE, position)
            .takeIf { it != CONTENT_END }
            ?.let { end ->
                pattern.substring(position + 1, end).also {
                    position = end + INCREMENT_CONSTANT
                }.split(COMMA).let { parts ->
                    runCatching {
                        when (parts.size) {
                            INCREMENT_CONSTANT -> {
                                val value = parts.first().toInt()
                                SequentialPageRegexQuantifier(value, value)
                            }
                            else -> SequentialPageRegexQuantifier(
                                min = parts.first().toInt(),
                                max = parts[FIRST_INDEX].takeIf { it.isNotEmpty() }?.toInt() ?: Int.MAX_VALUE
                            )
                        }
                    }.getOrNull()
                }
            }
}
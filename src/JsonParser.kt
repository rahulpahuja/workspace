/**
 * Minimal JSON reader (objects, arrays, strings, numbers, booleans, null).
 * Returns Map, List, String, Double, Boolean or null. Used to read Chrome's "Local State".
 */
object JsonParser {
    fun parse(text: String): Any? = Reader(text).readValue()

    private class Reader(private val s: String) {
        private var pos = 0

        fun readValue(): Any? {
            skipWhitespace()
            return when (val c = s.getOrNull(pos) ?: error("Unexpected end of JSON")) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readLiteral(if (c == 't') "true" else "false", c == 't')
                'n' -> readLiteral("null", null)
                else -> readNumber()
            }
        }

        private fun readObject(): Map<String, Any?> {
            val map = linkedMapOf<String, Any?>()
            pos++ // {
            skipWhitespace()
            if (s[pos] == '}') { pos++; return map }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                map[key] = readValue()
                skipWhitespace()
                if (s[pos] == ',') { pos++; continue }
                expect('}')
                return map
            }
        }

        private fun readArray(): List<Any?> {
            val list = mutableListOf<Any?>()
            pos++ // [
            skipWhitespace()
            if (s[pos] == ']') { pos++; return list }
            while (true) {
                list.add(readValue())
                skipWhitespace()
                if (s[pos] == ',') { pos++; continue }
                expect(']')
                return list
            }
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> when (val e = s[pos++]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> { sb.append(s.substring(pos, pos + 4).toInt(16).toChar()); pos += 4 }
                        else -> sb.append(e)
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun readNumber(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] in "+-.eE")) pos++
            return s.substring(start, pos).toDouble()
        }

        private fun readLiteral(word: String, value: Any?): Any? {
            require(s.startsWith(word, pos)) { "Invalid literal at $pos" }
            pos += word.length
            return value
        }

        private fun expect(c: Char) {
            skipWhitespace()
            require(s.getOrNull(pos) == c) { "Expected '$c' at $pos" }
            pos++
        }

        private fun skipWhitespace() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }
    }
}

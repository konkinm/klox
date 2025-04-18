import model.Token
import model.TokenType
import model.TokenType.*

class Scanner(val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        val c: Char = advance()
        when (c) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '+' -> addToken(PLUS)
            '-' -> addToken(MINUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '=' -> addToken(if(match('=')) EQUAL_EQUAL else EQUAL)
            else -> error(line, "Unexpected character: $c")
        }
    }

    private fun advance(): Char {
        return source.elementAt(current++)
    }

    private fun match(expected: Char): Boolean {
        if(isAtEnd()) return false
        if (source.elementAt(current) != expected) return false

        current++
        return true
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text: String = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }
}
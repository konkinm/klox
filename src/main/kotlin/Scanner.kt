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
            '!' -> addToken(if(match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if(match('=')) EQUAL_EQUAL else EQUAL)
            '>' -> addToken(if(match('=')) GREATER_EQUAL else GREATER)
            '<' -> addToken(if(match('=')) LESS_EQUAL else LESS)
            '/' -> if(match('/')) {
                while (peek() != '\n' && !isAtEnd()) advance()
            } else addToken(SLASH)
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            else -> if(isDigit(c)) {
                number()
            } else if (isAlpha(c)) {
                identifier()
            } else {
                syntaxError(line, "Unexpected character: $c")
            }
        }
    }

    private fun isDigit(c: Char?): Boolean {
        return if (c != null) {
            return c >= '0' && c <= '9'
        } else false
    }

    private fun isAlpha(c: Char?): Boolean {
        return if (c != null) {
            (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    c == '_'
        } else false
    }

    private fun isAlphaNumeric(c: Char?): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()

        addToken(IDENTIFIER)
    }

    private fun number() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && peekNext()?.isDigit() == true) {
            advance()

            while (peek()?.isDigit() == true) advance()
        }

        addToken(NUMBER, source.substring(start, current).toDouble())
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            syntaxError(line, "Unterminated string.")
            return
        }

        advance() // the closing "

        // trim quotes
        val value = source.substring(start + 1, current - 1)
        addToken(STRING, value)
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

    private fun peek(): Char? {
        if (isAtEnd()) return null
        return source.elementAt(current)
    }

    private fun peekNext(): Char? {
        if (current + 1 >= source.length) return null
        return source.elementAt(current + 1)
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text: String = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }
}
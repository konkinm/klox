import model.Expr
import model.Stmt
import model.Token
import model.TokenType
import model.TokenType.BANG
import model.TokenType.BANG_EQUAL
import model.TokenType.EQUAL_EQUAL
import model.TokenType.FALSE
import model.TokenType.GREATER
import model.TokenType.GREATER_EQUAL
import model.TokenType.LEFT_PAREN
import model.TokenType.LESS
import model.TokenType.LESS_EQUAL
import model.TokenType.MINUS
import model.TokenType.NIL
import model.TokenType.NUMBER
import model.TokenType.PLUS
import model.TokenType.SLASH
import model.TokenType.STAR
import model.TokenType.STRING
import model.TokenType.TRUE

class ParseError: RuntimeException()

class Parser(val tokens: List<Token>) {
    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            statements.add(statement())
        }
        return statements
    }

    private fun statement(): Stmt {
        if (match(TokenType.PRINT)) return printStatement()

        return expressionStatement()
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")

        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()

        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr? {
        return equality()
    }

    private fun equality(): Expr? {
        var expr = comparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()

            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr? {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = previous()
            val right = term()

            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr? {
        var expr = factor()

        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr? {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr? {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)
        if (match(NIL)) return Expr.Literal(null)

        if (match(NUMBER, STRING)) return Expr.Literal(previous()?.literal)

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw parseError(peek(), "Expect expression.")
    }

    private fun consume(type: TokenType, message: String): Token? {
        if (check(type)) return advance()

        throw parseError(peek(), message)
    }

    private fun parseError(token: Token, message: String): ParseError {
        error(token, message)
        return ParseError()
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type: TokenType in types) {
            if (check(type)) {
                advance()
                return true
            }
        }

        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token? {
        return tokens[current - 1]
    }

    private fun advance(): Token? {
        if (!isAtEnd()) current++
        return previous()
    }
}
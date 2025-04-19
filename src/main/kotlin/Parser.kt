import model.Expr
import model.Stmt
import model.Token
import model.TokenType
import model.TokenType.AND
import model.TokenType.BANG
import model.TokenType.BANG_EQUAL
import model.TokenType.CLASS
import model.TokenType.ELSE
import model.TokenType.EQUAL
import model.TokenType.EQUAL_EQUAL
import model.TokenType.FALSE
import model.TokenType.FOR
import model.TokenType.FUN
import model.TokenType.GREATER
import model.TokenType.GREATER_EQUAL
import model.TokenType.IDENTIFIER
import model.TokenType.IF
import model.TokenType.LEFT_BRACE
import model.TokenType.LEFT_PAREN
import model.TokenType.LESS
import model.TokenType.LESS_EQUAL
import model.TokenType.MINUS
import model.TokenType.NIL
import model.TokenType.NUMBER
import model.TokenType.OR
import model.TokenType.PLUS
import model.TokenType.PRINT
import model.TokenType.RETURN
import model.TokenType.RIGHT_BRACE
import model.TokenType.RIGHT_PAREN
import model.TokenType.SEMICOLON
import model.TokenType.SLASH
import model.TokenType.STAR
import model.TokenType.STRING
import model.TokenType.TRUE
import model.TokenType.VAR
import model.TokenType.WHILE

class ParseError: RuntimeException()

class Parser(val tokens: List<Token>) {
    private var current: Int = 0

    fun parse(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()

        try {
            while (!isAtEnd()) {
                statements.add(declaration())
            }
        } catch (_: ParseError) {}

        return statements
    }

    private fun declaration(): Stmt? {
        try {
            if (match(VAR)) return varDeclaration()

            return statement()
        } catch (_: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null
        if (match(EQUAL)) initializer = expression()

        consume(SEMICOLON, "Expect ';' after variable declaration.")

        return Stmt.Var(name, initializer)
    }

    fun parseSingleExpression(): Expr? {
        return try {
            expression()
        } catch (_: ParseError) {
            null
        }
    }

    private fun statement(): Stmt {
        if (match(IF)) return ifStatement()
        if (match(PRINT)) return printStatement()
        if (match(WHILE)) return whileStatement()
        if (match(LEFT_BRACE)) return Stmt.Block(block())

        return expressionStatement()
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(ELSE)) elseBranch = statement()

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun block(): List<Stmt> {
        val statements: MutableList<Stmt> = mutableListOf()

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            val declaration = declaration()
            if (declaration != null) statements.add(declaration)
        }

        consume(RIGHT_BRACE, "Expect '}' after block.")

        return statements
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")

        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()

        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr? {
        return assignment()
    }

    private fun assignment(): Expr? {
        val expr = or()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) return Expr.Assign(expr.name, value)

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or(): Expr? {
        var expr = and()

        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr? {
        var expr = equality()

        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
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

        if (match(IDENTIFIER)) return Expr.Variable(previous())

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
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

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous()?.type == SEMICOLON) {
                when (peek().type) {
                    CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                    else -> error("Unreachable")
                }
            }
        }
    }
}
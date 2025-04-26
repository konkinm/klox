import model.Expr
import model.Stmt
import model.Token
import model.TokenType
import model.TokenType.AND
import model.TokenType.BANG
import model.TokenType.BANG_EQUAL
import model.TokenType.BREAK
import model.TokenType.CLASS
import model.TokenType.COMMA
import model.TokenType.DOT
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
import model.TokenType.THIS
import model.TokenType.TRUE
import model.TokenType.VAR
import model.TokenType.WHILE

class ParseError : RuntimeException()

class Parser(val tokens: List<Token>) {
    private var current: Int = 0
    private var loopDepth: Int = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt?>()

        try {
            while (!isAtEnd()) {
                statements.add(declaration())
            }
        } catch (_: ParseError) {
        }

        return statements.mapNotNull { it }
    }

    fun parseSingleExpression(): Expr? {
        return try {
            expression()
        } catch (_: ParseError) {
            null
        }
    }

    private fun declaration(): Stmt? {
        try {
            if (match(CLASS)) return classDeclaration()
            if (match(FUN)) return function("function")
            if (match(VAR)) return varDeclaration()

            return statement()
        } catch (_: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt? {
        val name = consume(IDENTIFIER, "Expect class name.")

        var superclass: Expr.Variable? = null
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.")
            superclass = Expr.Variable(previous())
        }

        consume(LEFT_BRACE, "Expect '{' before class  body.")

        val methods: MutableList<Stmt.Function> = mutableListOf()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.")

        return Stmt.Class(name, superclass, methods)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(IDENTIFIER, "Expect $kind name.")
        consume(LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters: MutableList<Token> = mutableListOf()
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) error(peek(), "Can't have more than 255 parameters.")

                parameters.add(consume(IDENTIFIER, "Expect parameter name."))
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.")
        consume(LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()

        return Stmt.Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null
        if (match(EQUAL)) initializer = expression()

        consume(SEMICOLON, "Expect ';' after variable declaration.")

        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        if (match(BREAK)) return breakStatement()
        if (match(FOR)) return forStatement()
        if (match(IF)) return ifStatement()
        if (match(PRINT)) return printStatement()
        if (match(RETURN)) return returnStatement()
        if (match(WHILE)) return whileStatement()
        if (match(LEFT_BRACE)) return Stmt.Block(block())

        return expressionStatement()
    }

    private fun breakStatement(): Stmt {
        if (loopDepth == 0) error(previous(), "Must be inside a loop to use break.")

        consume(SEMICOLON, "Expect ';' after 'break'.")

        return Stmt.Break()
    }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer = if (match(SEMICOLON)) null
        else if (match(VAR)) varDeclaration()
        else expressionStatement()

        var condition: Expr? = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after loop condition.")

        val increment: Expr? = if (!check(RIGHT_PAREN)) expression() else null
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        try {
            loopDepth++
            var body: Stmt = statement()

            if (increment != null) {
                body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
            }

            if (condition == null) condition = Expr.Literal(true)
            body = Stmt.While(condition, body)

            if (initializer != null) body = Stmt.Block(listOf(initializer, body))

            return body
        } finally {
            loopDepth--
        }
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

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")

        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(SEMICOLON)) value = expression()

        consume(SEMICOLON, "Expect ';' after return value.")

        return Stmt.Return(keyword, value)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        try {
            loopDepth++
            val body = statement()

            return Stmt.While(condition, body)
        } finally {
            loopDepth--
        }
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

    private fun expressionStatement(): Stmt {
        val expr = expression()

        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                return Expr.Assign(expr.name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()

            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = previous()
            val right = term()

            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    private fun call(): Expr {
        var expr: Expr = primary()

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if(match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'.")
                expr = Expr.Get(expr, name)
            } else break
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments: MutableList<Expr> = mutableListOf()

        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) error(peek(), "Can't have more than 255 arguments.")
                arguments.add(expression())
            } while (match(COMMA))
        }

        val paren: Token = consume(RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)
        if (match(NIL)) return Expr.Literal(null)

        if (match(NUMBER, STRING)) return Expr.Literal(previous().literal)

        if (match(THIS)) return Expr.This(previous())

        if (match(IDENTIFIER)) return Expr.Variable(previous())

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw parseError(peek(), "Expect expression.")
    }

    private fun consume(type: TokenType, message: String): Token {
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

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return
            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> {}
            }

            advance()
        }
    }
}
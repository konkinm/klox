import model.Expr
import model.LoxCallable
import model.LoxFunction
import model.Stmt
import model.Token
import model.TokenType.BANG
import model.TokenType.BANG_EQUAL
import model.TokenType.EQUAL_EQUAL
import model.TokenType.GREATER
import model.TokenType.GREATER_EQUAL
import model.TokenType.LESS
import model.TokenType.LESS_EQUAL
import model.TokenType.MINUS
import model.TokenType.OR
import model.TokenType.PLUS
import model.TokenType.SLASH
import model.TokenType.STAR


class Interpreter(
    private val globals: Environment = defaultGlobalEnvironment,
    private val locals: MutableMap<Expr, Int> = HashMap(),
): Expr.Visitor<Any?>, Stmt.Visitor<Void?> {
    private var environment = globals

    fun interpret(statements: List<Stmt?>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    fun interpret(expression: Expr?) {
        try {
            val value = evaluate(expression)
            println(stringify(value))
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    private fun execute(statement: Stmt?) {
        statement?.accept(this)
    }

    fun resolve(expr: Expr, depth: Int) {
        locals.put(expr, depth)
    }

    private fun stringify(value: Any?): String {
        if (value == null) return "nil"

        if (value is Double) {
            val text = value.toString()
            if (text.endsWith(".0")) return text.substring(0, text.length - 2)
        }

        return value.toString()
    }

    private fun isEqual(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) return true
        if (left == null) return false

        return left == right
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals.get(expr)
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }

        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        when (expr.operator.type) {
            MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) - (right as Double)
            }
            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) / (right as Double)
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) * (right as Double)
            }
            PLUS -> {
                if (left is Double && right is Double) {
                    return left + right
                }

                if (left is String && right is String) {
                    return left + right
                }

                throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
            }
            GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double > right as Double
            }
            GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) >= (right as Double)
            }
            LESS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) < (right as Double)
            }
            LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) <= (right as Double)
            }
            BANG_EQUAL -> {
                return !isEqual(left, right)
            }
            EQUAL_EQUAL -> {
                return isEqual(left, right)
            }
            else -> null
        }

        return null
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val arguments: MutableList<Any?> = mutableListOf()

        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is LoxCallable)
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        if (arguments.size != callee.arity())
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")

        return callee.call(this, arguments)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right: Any? = evaluate(expr.right)

        when (expr.operator.type) {
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                return -(right as Double)
            }
            BANG -> return !isTruthy(right)
            else -> null
        }

        return null
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr?): Any? {
        val distance = locals.get(expr)
        if (distance != null) {
            return environment.getAt(distance, name.lexeme)
        } else {
            return globals.getByToken(name)
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(
        operator: Token,
        left: Any?, right: Any?,
    ) {
        if (left is Double && right is Double) return

        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun isTruthy(any: Any?): Boolean {
        if (any == null) return false
        if (any is Boolean) return any
        return true
    }

    private fun evaluate(expression: Expr?): Any? {
        return expression?.accept(this)
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Void? {
        executeBlock(stmt.statements, Environment(enclosing = environment))
        return null
    }

    override fun visitBreakStmt(stmt: Stmt.Break): Void? {
        throw BreakException()
    }

    fun executeBlock(statements: List<Stmt?>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            for (statement: Stmt? in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): Void? {
        evaluate(stmt.expression)
        return null
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Void? {
        val function = LoxFunction(stmt, environment)
        environment.define(stmt.name.lexeme, function)
        return null
    }

    override fun visitIfStmt(stmt: Stmt.If): Void? {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null)
            execute(stmt.elseBranch)
        return null
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Void? {
        val value = evaluate(stmt.expression)
        println(stringify(value))
        return null
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Void? {
        var value: Any? = null
        if (stmt.value != null) value = evaluate(stmt.value)

        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var): Void? {
        var value: Any? = null
        if (stmt.initializer != null) value = evaluate(stmt.initializer)

        environment.define(stmt.name.lexeme, value)
        return null
    }

    override fun visitWhileStmt(stmt: Stmt.While): Void? {
        try {
            while (isTruthy(evaluate(stmt.condition))) execute(stmt.body)
        } catch (_: BreakException) {}
        return null
    }
}

class RuntimeError(operator: Token?, message: String): RuntimeException(message) {
    val token = operator
}

class Return(val value: Any?):
    RuntimeException(null, null, false, false)

private class BreakException: RuntimeException()

private val defaultGlobalEnvironment = Environment().apply {
    define("clock", object: LoxCallable {
        override fun arity(): Int = 0

        override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
            return System.currentTimeMillis() / 1000.0
        }

        override fun toString(): String {
            return "<native fn>"
        }
    })
}
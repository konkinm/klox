import model.Expr
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
import model.TokenType.PLUS
import model.TokenType.SLASH
import model.TokenType.STAR

class Interpreter: Expr.Visitor<Any>, Stmt.Visitor<Unit> {
    fun interpret(statements: List<Stmt>) {
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

    private fun execute(statement: Stmt) {
        statement.accept(this)
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

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        when (expr.operator?.type) {
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

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right: Any? = evaluate(expr.right)

        when (expr.operator?.type) {
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                return -(right as Double)
            }
            BANG -> return !isTruthy(right)
            else -> null
        }

        return null
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number")
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

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }
}

class RuntimeError(operator: Token, message: String): RuntimeException(message) {
    val token = operator
}

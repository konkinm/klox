package tool

import model.Expr
import model.Stmt
import model.Token


class AstPrinter: Expr.Visitor<String>, Stmt.Visitor<String> {
    fun print(stmt: Stmt?): String {
        return stmt?.accept(this) ?: ""
    }

    fun print(expr: Expr?): String {
        return expr?.accept(this) ?: ""
    }

    override fun visitAssignExpr(expr: Expr.Assign): String? {
        return parenthesize2( "=", expr.name?.lexeme, expr.value)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize( expr.operator?.lexeme,
            expr.left, expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call): String? {
        return parenthesize2("call", expr.callee, expr.arguments)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize( "group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        if (expr.value == null) return "nil"
        return expr.value.toString()
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String? {
        return parenthesize( expr.operator?.lexeme, expr.left, expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize( expr.operator?.lexeme, expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String? {
        return expr.name?.lexeme
    }

    override fun visitBlockStmt(stmt: Stmt.Block): String? {
        val sb = StringBuilder()
        sb.append("(block ")

        for (statement in stmt.statements) {
            sb.append(statement.accept(this))
        }

        sb.append(")")
        return sb.toString()
    }

    override fun visitBreakStmt(stmt: Stmt.Break): String? {
        return "break"
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): String? {
        return parenthesize( ";", stmt.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If): String? {
        if (stmt.elseBranch == null) {
            return parenthesize2( "if", stmt.condition, stmt.thenBranch)
        }

        return parenthesize2( "if-else", stmt.condition, stmt.thenBranch,
            stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Stmt.Print): String? {
        return parenthesize( "print", stmt.expression)
    }

    override fun visitVarStmt(stmt: Stmt.Var): String? {
        if (stmt.initializer == null) {
            return parenthesize2( "var", stmt.name)
        }

        return parenthesize2( "var", stmt.name, "=", stmt.initializer)
    }

    override fun visitWhileStmt(stmt: Stmt.While): String? {
        return parenthesize2( "while", stmt.condition, stmt.body)
    }

    private fun parenthesize(name: String?, vararg exprs: Expr?): String {
        val sb = StringBuilder()

        sb.apply {
            append("(")
            append(name)
            for (expr: Expr? in exprs) {
                append(" ")
                append(expr?.accept(this@AstPrinter))
            }
            append(")")
        }

        return sb.toString()
    }

    private fun parenthesize2(name: String, vararg parts: Any?): String {
        val sb = StringBuilder()

        sb.append("(")
        sb.append(name)
        transform(sb, *parts)
        sb.append(")")

        return sb.toString()
    }

    private fun transform(sb: StringBuilder, vararg parts: Any?) {
        for (part in parts) {
            sb.append(" ")
            when(part) {
                is Expr -> sb.append(part.accept(this))
                is Stmt -> sb.append(part.accept(this))
                is Token -> sb.append(part.lexeme)
                is Array<*> -> transform(sb, *part)
                else -> sb.append(part)
            }
        }
    }
}
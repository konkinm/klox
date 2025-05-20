import model.Expr
import model.Expr.Assign
import model.Expr.Logical
import model.Stmt
import model.Stmt.While
import model.Token
import java.util.Stack

private enum class FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD,
}

private enum class ClassType {
    NONE,
    CLASS,
    SUBCLASS
}

class Resolver(
    private val interpreter: Interpreter,
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack<MutableMap<String, Boolean>>(),
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private var currentFunctionType = FunctionType.NONE
    private var currentClassType = ClassType.NONE

    fun resolve(statements: List<Stmt>) {
        for (statement in statements) {
            resolve(statement)
        }
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope: MutableMap<String, Boolean> = scopes.peek()

        if (scope.containsKey(name.lexeme)) {
            error(name, "Already a variable with this name in this scope.")
        }

        scope.put(name.lexeme, false)
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek().put(name.lexeme, true)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunctionType = currentFunctionType
        currentFunctionType = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()
        currentFunctionType = enclosingFunctionType
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClassType = currentClassType
        currentClassType = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if ((stmt.superclass != null) && (stmt.name.lexeme == stmt.superclass.name.lexeme)) {
            error(stmt.superclass.name, "A class can't inherit from itself.")
        }

        if (stmt.superclass != null) {
            currentClassType = ClassType.SUBCLASS
            resolve(stmt.superclass)
        }

        if (stmt.superclass != null) {
            beginScope()
            scopes.peek().put("super", true)
        }

        beginScope()
        scopes.peek().put("this", true)

        for (method in stmt.methods) {

            val declarationType = if (method.name.lexeme == "init")
                FunctionType.INITIALIZER else FunctionType.METHOD

            resolveFunction(method, declarationType)
        }

        endScope()

        if (stmt.superclass != null) endScope()

        currentClassType = enclosingClassType
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) resolve(stmt.elseBranch)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunctionType == FunctionType.NONE) {
            error(stmt.keyword, "Can't return from top-level code.")
        }

        if (stmt.value != null) {
            if (currentFunctionType == FunctionType.INITIALIZER) {
                error(stmt.keyword, "Can't return a value from an initializer.")
            }
            resolve(stmt.value)
        }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitAssignExpr(expr: Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)

        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
    }

    override fun visitLogicalExpr(expr: Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSuperExpr(expr: Expr.Super) {
        if (currentClassType == ClassType.NONE) {
            error(expr.keyword, "Can't use 'super' outside of a class.")
        } else if (currentClassType != ClassType.SUBCLASS) {
            error(expr.keyword, "Can't use 'super' in a class with no superclass.")
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitThisExpr(expr: Expr.This) {
        if (currentClassType == ClassType.NONE) {
            error(expr.keyword, "Can't use 'this' outside of a class.")
        }

        resolveLocal(expr, expr.keyword)

    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() &&
            scopes.peek().get(expr.name.lexeme) == java.lang.Boolean.FALSE
        ) {
            error(
                expr.name,
                "Can't read local variable in its own initializer."
            )
        }

        resolveLocal(expr, expr.name)
    }

}
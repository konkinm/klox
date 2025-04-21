package model

import Environment
import Interpreter

class LoxFunction(private val declaration: Stmt.Function): LoxCallable {
    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(enclosing = interpreter.globals)
        arguments.forEachIndexed { index, argument ->
            environment.define(declaration.params[index]?.lexeme, argument)
        }

        interpreter.executeBlock(declaration.body, environment)
        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name?.lexeme}>"
    }
}
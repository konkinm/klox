package model

import Environment
import Interpreter
import Return

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean,
): LoxCallable {
    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(enclosing = closure)
        arguments.forEachIndexed { index, argument ->
            environment.define(declaration.params[index].lexeme, argument)
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) return closure.getAt(0, "this")

            return returnValue.value
        }

        if (isInitializer) return closure.getAt(0, "this")

        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(enclosing = closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }
}
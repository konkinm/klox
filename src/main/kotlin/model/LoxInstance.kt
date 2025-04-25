package model

import RuntimeError

class LoxInstance(
    private val klass: LoxClass,
    private val fields: MutableMap<String, Any?> = mutableMapOf()
) {
    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) return fields[name.lexeme]

        val method = klass.findMethod(name.lexeme)
        if (method != null) return method.bind(this)

        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    fun set(name: Token, value: Any?) {
        fields.put(name.lexeme, value)
    }

    override fun toString(): String {
        return "${klass.name} instance"
    }
}
import model.Token

class Environment(
    val values: MutableMap<String, Any?> = mutableMapOf(),
    val enclosing: Environment? = null,
) {

    fun define(name: String?, value: Any?) {
        if (name != null) values.put(name, value)
    }

    fun ancestor(distance: Int): Environment? {
        var environment: Environment? = this
        for (i in 0..<distance) {
            environment = environment!!.enclosing
        }

        return environment
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance)?.values?.get(name)
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance)!!.values.put(name.lexeme, value)
    }

    fun getByToken(name: Token?): Any? {
        if (name != null && values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }

        if (name != null && enclosing != null) return enclosing.getByToken(name)

        throw RuntimeError(name, "Undefined variable '${name?.lexeme}'.")
    }

    fun assign(name: Token?, value: Any?) {
        if (name != null && values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value)
            return
        }

        if (name != null && enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeError(name, "Undefined variable '${name?.lexeme}'.")
    }

    override fun toString(): String {
        var result: String = values.toString()
        if (enclosing != null) {
            result += " -> $enclosing"
        }

        return result
    }
}
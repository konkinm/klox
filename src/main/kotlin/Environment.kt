import model.Token

data class Environment(
    val values: MutableMap<String, Any?> = mutableMapOf(),
    val enclosing: Environment? = null
) {

    fun define(name: String?, value: Any?) {
        if (name != null) values.put(name, value)
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
}
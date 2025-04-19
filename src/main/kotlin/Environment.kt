import model.Token

data class Environment(val values: MutableMap<String, Any?> = mutableMapOf()) {
    fun define(name: String?, value: Any?) {
        if (name != null) values.put(name, value)
    }

    fun getByTokenLexeme(name: Token?): Any? {
        if (name != null && values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }

        throw RuntimeError(name, "Undefined variable '${name?.lexeme}'.")
    }

    fun assign(name: Token?, value: Any?) {
        if (name != null && value != null && values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value)
            return
        }

        throw RuntimeError(name, "Undefined variable '${name?.lexeme}'.")
    }
}
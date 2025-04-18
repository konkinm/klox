package model

data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int
) {
    override fun toString(): String {
        return "${type.name} $lexeme $literal"
    }
}

enum class TokenType {
    LEFT_PAREN, RIGHT_PAREN,

    EOF
}
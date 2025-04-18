import model.Token
import model.TokenType
import tool.AstPrinter
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println("Usage: ./your_program.cmd [tokenize|parse] <filename>")
        exitProcess(1)
    }

    val command = args[0]
    val filename = args[1]

    val fileContents = File(filename).readText()

    when (command) {
        "tokenize" -> tokenizeFile(fileContents)
        "parse" -> parseFile(fileContents)
        "evaluate" -> evaluateFile(fileContents)
        else -> {
            System.err.println("Unknown command: ${command}")
            exitProcess(1)
        }
    }

    if (hadError) exitProcess(65) // exit with syntax error
    if (hadRuntimeError) exitProcess(70) // exit with runtime error
}

fun tokenizeFile(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()

    for (token in tokens) {
        println(token)
    }
}


fun parseFile(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()
    val expression = Parser(tokens).parse()

    println(AstPrinter().print(expression))
}

fun evaluateFile(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()
    val expression = Parser(tokens).parse()
    val interpreter = Interpreter()

    interpreter.interpret(expression)
}

fun syntaxError(line: Int, message: String) {
    report(line, "", message)
}

fun report(line: Int, where: String, message: String) {
    System.err.println(
        "[line $line] Error$where: $message"
    )
    hadError = true
}

fun error(token: Token, message: String) {
    if (token.type == TokenType.EOF) {
        report(token.line, " at end", message)
    } else {
        report(token.line, " at '" + token.lexeme + "'", message)
    }
}

fun runtimeError(error: RuntimeError) {
    System.err.println(error.message + "\n[line " + error.token.line + "]")
    hadRuntimeError = true
}

var hadError: Boolean = false
var hadRuntimeError: Boolean = false
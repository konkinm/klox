import model.Token
import model.TokenType
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println("Usage: ./your_program.cmd run <filename>")
        exitProcess(1)
    }

    val command = args[0]
    val filename = args[1]

    val fileContents = File(filename).readText()

    when (command) {
        "tokenize" -> tokenizeProgram(fileContents)
        "parse" -> parseProgram(fileContents)
        "evaluate" -> evalProgram(fileContents)
        "run" -> runProgram(fileContents)
        else -> {
            System.err.println("Unknown command: $command")
            exitProcess(1)
        }
    }

    if (hadError) exitProcess(65) // exit with compile error
    if (hadRuntimeError) exitProcess(70) // exit with runtime error
}

fun tokenizeProgram(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()

    for (token in tokens) {
        println(token)
    }
}

fun parseProgram(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()
    val expression = Parser(tokens).parseSingleExpression()

    println(AstPrinter().print(expression))
}

fun evalProgram(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()
    val expression = Parser(tokens).parseSingleExpression()
    if (expression != null) Interpreter().interpret(expression)
}

fun runProgram(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()
    if (hadError) return
    val statements = Parser(tokens).parse()
    if (hadError) return
    val interpreter = Interpreter()
    Resolver(interpreter).resolve(statements)
    if (hadError) return
    interpreter.interpret(statements)
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

fun error(token: Token?, message: String) {
    if (token?.type == TokenType.EOF) {
        report(token.line, " at end", message)
    } else {
        if (token != null)
            report(token.line, " at '" + token.lexeme + "'", message)
        else System.err.println("Token is null")
    }
}

fun runtimeError(error: RuntimeError) {
    System.err.println("[line ${error.token?.line}] ${error.message}")
    hadRuntimeError = true
}

var hadError: Boolean = false
var hadRuntimeError: Boolean = false
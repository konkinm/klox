import model.Token
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
        else -> {
            System.err.println("Unknown command: ${command}")
            exitProcess(1)
        }
    }

    if (hadError) exitProcess(65) // exit with syntax error
}

fun tokenizeFile(source: String) {
    val scanner = Scanner(source)
    val tokens: List<Token> = scanner.scanTokens()

    for (token in tokens) {
        println(token)
    }
}

fun parseFile(fileContents: String): String {
    TODO("Not implemented")
}

fun syntaxError(line: Int, message: String) {
    report(line, "", message)
}

fun report(line: Int, where: String, message: String) {
    System.err.println(
        "[line $line] Error: $message"
    )
    hadError = true
}

var hadError: Boolean = false
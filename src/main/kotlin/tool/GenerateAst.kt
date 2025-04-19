package tool

import java.io.PrintWriter
import java.nio.file.Files
import java.util.Locale.getDefault
import kotlin.io.path.Path
import kotlin.system.exitProcess

private val exprTypes = mapOf(
    "Assign" to "val name: Token?, val value: Expr?",
    "Binary" to "val left: Expr?, val operator: Token?, val right: Expr?",
    "Grouping" to "val expression: Expr?",
    "Literal" to "val value: Any?",
    "Logical" to "val left: Expr?, val operator: Token?, val right: Expr?",
    "Unary" to "val operator: Token?, val right: Expr?",
    "Variable" to "val name: Token?",
)

private val stmtTypes = mapOf(
    "Block" to "val statements: List<Stmt>",
    "Expression" to "val expression: Expr?",
    "If" to "val condition: Expr?, val thenBranch: Stmt?, val elseBranch: Stmt?",
    "Print" to "val expression: Expr?",
    "Var" to "val name: Token?, val initializer: Expr?",
    "While" to "val condition: Expr?, val body: Stmt?",
)

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(65)
    }
    val outputDir: String = args[0]

    defineAst(outputDir, "Expr", exprTypes)
    defineAst(outputDir, "Stmt", stmtTypes)
}

fun defineAst(outputDir: String, baseName: String, types: Map<String, String>) {
    val name = "$outputDir/${baseName}.kt"
    val path = Path(name)
    if (Files.exists(Path(name))) {
        Files.delete(path)
    }
    val writer = PrintWriter(name, "UTF-8")

    writer.println("package model")
    writer.println()
    writer.println("abstract class $baseName {")

    defineVisitor(writer, baseName, types)

    // The base accept() method.
    writer.println()
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R?")
    writer.println()

    for (type in types) {
        val className = type.key.trim()
        val fields = type.value
        defineType(writer, baseName, className, fields)
    }

    writer.println("}")
    writer.close()
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: Map<String, String>) {
    writer.println("    interface Visitor<R> {")

    for (type in types) {
        val typeName: String = type.key
        writer.println("        fun visit$typeName$baseName(${baseName.lowercase(getDefault())}: $typeName): R?")
    }

    writer.println("  }")
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    writer.print("    data class $className($fieldList) : $baseName() {")

    // Visitor pattern.
    writer.println()
    writer.println("        override fun <R> accept(visitor: Visitor<R>) : R? {")
    writer.println("          return visitor.visit$className$baseName(this)")
    writer.println("        }")

    writer.println("    }")
    writer.println()
}

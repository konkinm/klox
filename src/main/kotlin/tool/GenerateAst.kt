package tool

import java.io.PrintWriter
import java.util.Locale.getDefault
import kotlin.system.exitProcess

private val exprTypes = mapOf(
    "Binary" to "val left: Expr, val operator: Token, val right: Expr",
    "Grouping" to "val expression: Expr",
    "Literal" to "val value: Any?",
    "Unary" to "val operator: Token, val right: Expr"
)

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(65)
    }
    val outputDir: String = args[0]

    defineAst(outputDir, "Expr", exprTypes)
}

fun defineAst(outputDir: String, baseName: String, types: Map<String, String>) {
    val path = "$outputDir/${baseName}_.kt"
    val writer = PrintWriter(path, "UTF-8")

    writer.println("package model")
    writer.println()
    writer.println("abstract class $baseName {")

    defineVisitor(writer, baseName, types)

    // The base accept() method.
    writer.println()
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")
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
        writer.println("        fun visit$typeName$baseName(${baseName.lowercase(getDefault())}: $typeName): R")
    }

    writer.println("  }")
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    writer.print("    data class $className($fieldList) : $baseName() {")

    // Visitor pattern.
    writer.println()
    writer.println("        override fun <R> accept(visitor: Visitor<R>) : R {")
    writer.println("          return visitor.visit$className$baseName(this)")
    writer.println("        }")

    writer.println("    }")
    writer.println()
}

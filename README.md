# klox
Lox language interpreter in Kotlin (Following ["Crafting Interpreters"](http://www.craftinginterpreters.com/) book).

## Syntax Grammar
```
program        → declaration* EOF ;
```

### Declarations
```
declaration    → classDecl
| funDecl
| varDecl
| statement ;

classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )?
"{" function* "}" ;
funDecl        → "fun" function ;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
```

### Statements
```
statement      → exprStmt
| forStmt
| ifStmt
| printStmt
| returnStmt
| whileStmt
| block 
| break ;

exprStmt       → expression ";" ;
forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
expression? ";"
expression? ")" statement ;
ifStmt         → "if" "(" expression ")" statement
( "else" statement )? ;
printStmt      → "print" expression ";" ;
returnStmt     → "return" expression? ";" ;
whileStmt      → "while" "(" expression ")" statement ;
block          → "{" declaration* "}" ;
break          -> "break" ";" ;
```

### Expressions
```
expression     → assignment ;

assignment     → ( call "." )? IDENTIFIER "=" assignment
| logic_or ;

logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;

unary          → ( "!" | "-" ) unary | call ;
call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
primary        → "true" | "false" | "nil" | "this"
| NUMBER | STRING | IDENTIFIER | "(" expression ")"
| "super" "." IDENTIFIER ;
```

### Utility rules
```
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
arguments      → expression ( "," expression )* ;
```

## Lexical Grammar
```
NUMBER         → DIGIT+ ( "." DIGIT+ )? ;
STRING         → "\"" <any char except "\"">* "\"" ;
IDENTIFIER     → ALPHA ( ALPHA | DIGIT )* ;
ALPHA          → "a" ... "z" | "A" ... "Z" | "_" ;
DIGIT          → "0" ... "9" ;
```
---

## Test suite usage

Requirements: Python >=3.13.3, JDK 21

1. Build klox: `mvn -B package`
2. Go to *testSuite* folder
3. Run `python generate.py`
4. Run `pytest --lox "java -jar ../target/klox.jar run"
`

Acknowledge: test suite borrowed from [this](https://github.com/montreal91/kind-kestrel) repo and modified a bit.
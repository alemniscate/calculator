package calculator

import java.math.BigInteger

fun main() {

    var vars = Variable()
    var input = readLine()!!
    while (input != "/exit") {
        when {
            input == "" -> {
            }
            input.first() == '/' -> Command(input)
            isLet(input) -> vars.add(input)
            else -> {
                var nullflag = true
                var errorflag = false
                val tokenList = TokenList(input, vars)
                when (tokenList.result) {
                    "error" -> println("Invalid expression")
                    "unknown variable" -> println("Unknown variable")
                    "ok" -> {
                        val rpn = Rpn(tokenList.tokens, vars)
                        when (rpn.result) {
                            "unbalance element" -> println("Invalid expression")
                            "ok" -> {
                                val calc = Calculate(rpn.queue, vars)
                                println(calc.getResultValue())
                            }
                        }
                    }
                }
            }
        }
        input = readLine()!!
    }
    println("Bye!")
}

class Calculate(rpn: Queue, vars: Variable) {
    /*
        If the incoming element is a number, push it into the stack (the whole number, not a single digit!).
        If the incoming element is the name of a variable, push its value into the stack.
        If the incoming element is an operator, then pop twice to get two numbers and perform the operation; push the result on the stack.
        When the expression ends, the number on the top of the stack is a final result
    */
    val stack = Stack()
    var result = "ok"
    var value = ""

    init {
        for (token in rpn.queue) {
            if (token.type == "number") {
                stack.push(token)
                continue
            }
            if (token.type == "variable") {
                val value = vars.getValue(token.word).toString()
                var tokenS = Token(value, "number")
                stack.push(tokenS)
                continue
            }
            if (token.type == "operator") {
//                var operand2 = stack.pop().word.toDouble()
                val operand2B = stack.pop().word.toBigInteger()
                if (stack.isEmpty()) {
                    val operand2 = when (token.word) {
                        "+" -> operand2B
                        "-" -> -operand2B
                        else -> operand2B
                    }
                    stack.push(Token(operand2.toString(), "number"))
                } else {
 //                   var operand1 = stack.pop().word.toDouble()
                    val operand1B = stack.pop().word.toBigInteger()
                    val operand1 = when (token.word) {
                        "+" -> operand1B + operand2B
                        "-" -> operand1B - operand2B
                        "*" -> operand1B * operand2B
                        "/" -> operand1B / operand2B
  //                      "^" -> Math.pow(operand1B, operand2B)
                        else -> operand1B
                    }
                    stack.push(Token(operand1.toString(), "number"))
                }
                continue
            }
        }
    }

    fun getResultValue(): String {
        var value = stack.pop().word
        if (value.length > 2) {
            if (value.substring(value.length - 2, value.length) == ".0") {
                value = value.substring(0, value.length - 2)
            }
        }
        return value
    }
}

class Rpn(tokens: MutableList<Token>, vars: Variable) {
    val stack = Stack()
    val queue = Queue()
    var result = "ok"

    /*
        1.Add operands (numbers and variables) to the result (postfix notation) as they arrive.
        2.If the stack is empty or contains a left parenthesis on top, push the incoming operator on the stack.
        3.If the incoming operator has higher precedence than the top of the stack, push it on the stack.
        4.If the incoming operator has lower or equal precedence than or to the top of the stack, pop the stack and add operators to the result until you see an operator that has a smaller precedence or a left parenthesis on the top of the stack; then add the incoming operator to the stack.
        5.If the incoming element is a left parenthesis, push it on the stack.
        6.If the incoming element is a right parenthesis, pop the stack and add operators to the result until you see a left parenthesis. Discard the pair of parentheses.
        7.At the end of the expression, pop the stack and add all operators to the result.
        No parentheses should remain on the stack. Otherwise, the expression has unbalance
     */
    init {
        loop@ for (token in tokens) {
            //1
            if (token.type == "number" || token.type == "variable") {
                queue.enqueue(token)
                continue
            }
            //2
            if (stack.isEmpty() || stack.peek().word == "(") {
                stack.push(token)
                continue;
            }
            //3
            if (token.type == "operator") {
                val levelT = getlevel(token.word)
                var tokenS = stack.peek()
                var levelS = getlevel(tokenS.word)
                if (levelT > levelS) {
                    stack.push(token)
                    continue
                }
                //4
                while (levelT <= levelS || tokenS.word != "(") {
                    queue.enqueue(stack.pop())
                    if (stack.isEmpty()) break
                    tokenS = stack.peek()
                    levelS = getlevel(tokenS.type)
                }
                stack.push(token)
                continue
            }
            //5
            if (token.type == "parentheses" && token.word == "(") {
                stack.push(token)
                continue
            }
            //6
            if (token.type == "parentheses" && token.word == ")") {
                if (stack.isEmpty()) {
                    result = "unbalance element"
                    break@loop
                }
                var tokenS = stack.pop()
                while (tokenS.word != "(") {
                    queue.enqueue(tokenS)
                    if (stack.isEmpty()) {
                        result = "unbalance element"
                        break@loop
                    }
                    tokenS = stack.pop()
                }
                continue
            }
        }

        while (!stack.isEmpty()) {
            val tokenS = stack.pop()
            if (tokenS.type == "parentheses") {
                result = "unbalance element"
                break
            } else {
                queue.enqueue(tokenS)
            }
        }
    }

    private fun getlevel(type: String): Int {
        return when (type) {
            "variable" -> 0
            "number" -> 0
            "+" -> 1
            "-" -> 1
            "*" -> 2
            "/" -> 2
            "^" -> 3
            else -> 0
        }
    }
}

data class Token(val word: String, val type: String)

class TokenList(input: String, vars: Variable) {
    val tokens = mutableListOf<Token>()
    var result = "empty"

    init {
        val inputex = input + "\n"
        var oldchtype = ""
        var word = ""
        var errflag = false
        for (ch in inputex) {
            val chtype = when (ch) {
                ' ', '\n' -> "separator"
                in '0'..'9' -> "number"
                '+', '-', '*', '/', '^' -> "operator"
                '(', ')' -> "parentheses"
                in 'a'..'z' -> "variable"
                in 'A'..'Z' -> "variable"
                else -> "separator"
            }
            if (oldchtype == "" || chtype == oldchtype) {
                if (chtype != "separator") {
                    oldchtype = chtype
                    word += ch
                }
            } else {
                val type = when {
                    isVariable(word) -> {
                        val value = vars.getValue(word)
                        if (value == null) {
                            "unknown variable"
                        } else {
                            "variable"
                        }
                    }
                    isOperator(word) -> {
                        val operator = word.first()
                        when (operator) {
                            '-' -> {
                                if (operator == '-' && word.length % 2 == 0) {
                                    word = "+"
                                } else {
                                    word = "-"
                                }
                            }
                            '+' -> {
                                word = "+"
                            }
                        }
                        if (word.length > 1) {
                            "error"
                        } else {
                            "operator"
                        }
                    }
                    isNumber(word) -> {
                        "number"
                    }
                    isParentheses(word) -> {
                        "parentheses"
                    }
                    word == "" -> ""
                    else -> "error"
                }

                if (type == "error" || type == "unknown variable") {
                    result = type
                    errflag = true
                } else {
                    if (word != "") {
                        if (type != "parentheses") {
                            tokens.add(Token(word, type))
                        } else {
                            val firstchar = word.substring(0, 1)
                            repeat(word.length) {
                                tokens.add(Token(firstchar, type))
                            }
                        }
                    }

                    if (chtype == "separator") {
                        word = ""
                    } else {
                        word = ch.toString()
                    }

                    oldchtype = chtype
                }
            }
            if (errflag) break
        }
        if (!errflag) result = "ok"
    }


    private fun isNumber(word: String): Boolean {
        if (word == "") return false
        val firstcheck = when (word.first()) {
            '+', '-' -> true
            in '0'..'9' -> true
            else -> false
        }
        if (!firstcheck) return false
        if (word.length == 1) return true

        for (ch in word.substring(1)) {
            if (ch !in '0'..'9') return false
        }
        return true
    }

    private fun isOperator(word: String): Boolean {
        if (word == "") return false
        val firstchar = word.first()
        val firstcheck = when (firstchar) {
            '+', '-', '*', '/', '^' -> true
            else -> false
        }
        if (!firstcheck) return false
        if (firstchar.toString().repeat(word.length) == word) return true
        return false
    }

    private fun isParentheses(word: String): Boolean {
        if (word == "") return false
        val firstchar = word.first()
        val firstcheck = when (firstchar) {
            '(', ')' -> true
            else -> false
        }
        if (!firstcheck) return false
        if (firstchar.toString().repeat(word.length) == word) return true
        return false
    }

    private fun isVariable(word: String): Boolean {
        if (word == "") return false
        for (ch in word.toLowerCase()) {
            if (ch !in 'a'..'z') {
                return false
            }
        }
        return true
    }
}

fun isLet(input: String): Boolean {
    for (ch in input) {
        if (ch == '=') return true
    }
    return false
}

class Variable {
    val vars = mutableMapOf<String, String>()

    fun add(input: String) {
        var eqcount = 0
        for (ch in input) {
            if (ch == '=') eqcount++
        }
        if (eqcount > 1) {
            println("Invalid assignment")
            return
        }
        val strs = input.split("=", " ")
        var key = ""
        var value = ""
        for (str in strs) {
            if (str == "") continue
            if (key == "") {
                key = str
                continue
            }
            if (value == "") {
                value = str
                continue
            }
        }

        if (!isAlphabet(key)) {
            println("Invalid identifier")
            return
        }

        if (isAlphabet(value)) {
            if (vars.containsKey(value)) {
                vars[key] = vars[value]!!
            } else {
                println("Unknown variable")
            }
            return
        }

        if (!isNumeric(value)) {
            println("Invalid assignment")
            return
        }

        vars[key] = value
    }

    fun getValue(key: String): String? {
        if (!vars.containsKey(key)) return null
        return vars[key]!!
    }


    private fun isNumeric(str: String): Boolean {
        for (ch in str) {
            if (ch !in '0'..'9') return false
        }
        return true
    }

    private fun isAlphabet(str: String): Boolean {
        for (ch in str.toLowerCase()) {
            if (ch !in 'a'..'z') return false
        }
        return true
    }
}

class Command(command: String) {
    val result = when (command) {
        "/help" -> {
            println("The program calculates the sum of numbers")
            true
        }
        else -> {
            println("Unknown command")
            false
        }
    }
}

class Stack {
    val stack = mutableListOf<Token>()

    fun push(token: Token) {
        stack.add(token)
    }

    fun pop(): Token {
        return stack.removeAt(stack.lastIndex)
    }

    fun peek(): Token {
        return stack.last()
    }

    fun isEmpty(): Boolean {
        return stack.isEmpty()
    }
}

class Queue {
    val queue = mutableListOf<Token>()

    fun enqueue(token: Token) {
        queue.add(token)
    }

    fun dequeue(): Token {
        return queue.removeAt(0)
    }
}

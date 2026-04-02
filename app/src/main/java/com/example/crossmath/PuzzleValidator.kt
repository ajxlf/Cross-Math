package com.example.crossmath

enum class EquationState { INCOMPLETE, CORRECT, INCORRECT }

fun evaluateEquation(eq: Equation, userInputs: Map<String, String>): EquationState {
    val eqCells = cells(eq)

    val leftKey   = "${eqCells[0].first},${eqCells[0].second}" // matches how GameScreen and ViewModel store user inputs
    val rightKey  = "${eqCells[2].first},${eqCells[2].second}"
    val resultKey = "${eqCells[4].first},${eqCells[4].second}"

    val leftVal = when (eq.blankPosition) { // only looks up userInputs for the position that is actually blank in this equation
        BlankPosition.LEFT   -> userInputs[leftKey]?.toIntOrNull()
        else                 -> eq.left
    }
    val rightVal = when (eq.blankPosition) {
        BlankPosition.RIGHT  -> userInputs[rightKey]?.toIntOrNull()
        else                 -> eq.right
    }
    val resultVal = when (eq.blankPosition) {
        BlankPosition.RESULT -> userInputs[resultKey]?.toIntOrNull()
        else                 -> eq.result
    }

    if (leftVal == null || rightVal == null || resultVal == null) { // if any value is null the blank hasn't been filled yet it returns incomplete
        return EquationState.INCOMPLETE
    }

    val computed = when (eq.op) {
        "+"  -> leftVal + rightVal
        "-"  -> leftVal - rightVal
        "×"  -> leftVal * rightVal
        "/"  -> if (rightVal != 0) leftVal / rightVal else return EquationState.INCORRECT
        else -> return EquationState.INCORRECT
    }

    return if (computed == resultVal) EquationState.CORRECT else EquationState.INCORRECT
}
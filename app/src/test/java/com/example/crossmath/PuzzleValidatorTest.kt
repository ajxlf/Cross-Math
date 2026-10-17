package com.example.crossmath

import org.junit.Assert.assertEquals
import org.junit.Test

class PuzzleValidatorTest {

    @Test
    fun evaluatesCorrectAnswersForEachArithmeticOperator() {
        val addition = Equation(0, 0, Direction.HORIZONTAL, 3, "+", 4, 7, BlankPosition.RESULT)
        val subtraction = Equation(0, 0, Direction.HORIZONTAL, 8, "-", 3, 5, BlankPosition.RESULT)
        val multiplication = Equation(0, 0, Direction.HORIZONTAL, 3, "×", 4, 12, BlankPosition.RESULT)
        val division = Equation(0, 0, Direction.HORIZONTAL, 12, "/", 3, 4, BlankPosition.RESULT)

        assertEquals(EquationState.CORRECT, evaluateEquation(addition, mapOf("0,4" to "7")))
        assertEquals(EquationState.CORRECT, evaluateEquation(subtraction, mapOf("0,4" to "5")))
        assertEquals(EquationState.CORRECT, evaluateEquation(multiplication, mapOf("0,4" to "12")))
        assertEquals(EquationState.CORRECT, evaluateEquation(division, mapOf("0,4" to "4")))
    }

    @Test
    fun reportsIncompleteWhenBlankIsMissingOrNotANumber() {
        val equation = Equation(0, 0, Direction.HORIZONTAL, 3, "+", 4, 7, BlankPosition.LEFT)

        assertEquals(EquationState.INCOMPLETE, evaluateEquation(equation, emptyMap()))
        assertEquals(
            EquationState.INCOMPLETE,
            evaluateEquation(equation, mapOf("0,0" to "not-a-number"))
        )
    }

    @Test
    fun reportsIncorrectForWrongAnswers() {
        val equation = Equation(0, 0, Direction.HORIZONTAL, 3, "+", 4, 7, BlankPosition.LEFT)

        assertEquals(EquationState.INCORRECT, evaluateEquation(equation, mapOf("0,0" to "4")))
    }

    @Test
    fun usesCorrectCoordinatesForVerticalEquations() {
        val equation = Equation(
            startRow = 1,
            startCol = 4,
            direction = Direction.VERTICAL,
            left = 2,
            op = "+",
            right = 3,
            result = 5,
            blankPosition = BlankPosition.RIGHT
        )

        assertEquals(EquationState.CORRECT, evaluateEquation(equation, mapOf("3,4" to "3")))
    }

    @Test
    fun treatsDivisionByZeroAsIncorrect() {
        val equation = Equation(0, 0, Direction.HORIZONTAL, 12, "/", 0, 0, BlankPosition.RESULT)

        assertEquals(EquationState.INCORRECT, evaluateEquation(equation, mapOf("0,4" to "0")))
    }
}

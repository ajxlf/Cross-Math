package com.example.crossmath

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PuzzleViewModel(private val requestedEquationCount: Int? = null) : ViewModel() {  // ViewModel holds the puzzle state so it survives rotation

    private var generatedPuzzle = generatePuzzleWithEquations(requestedEquationCount)   // generates the first puzzle immediately when the ViewModel is created

    var puzzle: Puzzle by mutableStateOf(generatedPuzzle.first)                 // private set means only this ViewModel can change these, the UI just reads them
        private set
    var equations: List<Equation> by mutableStateOf(generatedPuzzle.second)
        private set
    var userInputs: Map<String, String> by mutableStateOf(mapOf())
        private set
    var score: Int by mutableIntStateOf(0)
        private set

    private var blankCellToEquations: Map<String, List<Int>> = buildBlankMap(equations) // used in recalculateScore to know which equations to check when a cell changes

    fun newPuzzle(requestedEquationCount: Int? = null) {
        val result = generatePuzzleWithEquations(requestedEquationCount ?: this.requestedEquationCount)
        puzzle     = result.first
        equations  = result.second
        userInputs = mapOf()
        score      = 0
        blankCellToEquations = buildBlankMap(equations)
    }

    fun setInput(key: String, value: String) {
        userInputs = userInputs + (key to value) // create a new map so Compose detects the change
        recalculateScore()
    }

    private fun recalculateScore() {
        val eqStates = equations.map { evaluateEquation(it, userInputs) }
        var total = 0
        for ((_, eqIndices) in blankCellToEquations) {  // for each blank cell, count how many of its equations are currently correct
            total += eqIndices.count { idx -> eqStates[idx] == EquationState.CORRECT }
        }
        score = total
    }

    private fun buildBlankMap(equations: List<Equation>): Map<String, List<Int>> {
        val map = mutableMapOf<String, MutableList<Int>>()
        equations.forEachIndexed { index, eq ->
            val eqCells = cells(eq)
            val numberPositions = listOf(eqCells[0], eqCells[2], eqCells[4]) // positions 0, 2, 4 are the number cells

            numberPositions.forEach { pos ->
                val key = "${pos.first},${pos.second}" // only track cells that are actually blank (editable) in the puzzle

                if (puzzle.cellAt(pos.first, pos.second)?.type == CellType.EMPTY_INPUT) {
                    map.getOrPut(key) { mutableListOf() }.add(index)
                }
            }
        }
        return map
    }
}

// Factory pattern required for ViewModel with constructor arguments
// Adapted from: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
class PuzzleViewModelFactory(private val equationCount: Int?) : ViewModelProvider.Factory { // custom factory needed because PuzzleViewModel has a constructor argument (equationCount)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {     //default ViewModelProvider can't handle that without a factory
        @Suppress("UNCHECKED_CAST")
        return PuzzleViewModel(equationCount) as T
    }
}
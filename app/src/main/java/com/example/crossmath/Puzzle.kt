package com.example.crossmath

enum class CellType {
    EMPTY_INPUT,
    GIVEN_NUMBER,
    OPERATOR,
    EQUALS,
    BLOCK
}

data class PuzzleCell(
    val row: Int,
    val col: Int,
    val type: CellType,
    val value: String? = null
)

data class Puzzle(
    val rows: Int,
    val cols: Int,
    val cells: List<PuzzleCell>
) {
    fun cellAt(row: Int, col: Int): PuzzleCell? =
        cells.firstOrNull { it.row == row && it.col == col }
}
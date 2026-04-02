package com.example.crossmath

import kotlin.math.abs
import kotlin.random.Random

enum class Direction { HORIZONTAL, VERTICAL }
enum class BlankPosition { LEFT, RIGHT, RESULT }

data class Equation(
    val startRow: Int,
    val startCol: Int,
    val direction: Direction,
    val left: Int,
    val op: String,
    val right: Int,
    val result: Int,
    val blankPosition: BlankPosition
)

fun generatePuzzleWithEquations(requestedEquationCount: Int? = null): Pair<Puzzle, List<Equation>> {

    val rows: Int
    val cols: Int

    if (requestedEquationCount != null) { //grid size is derived from the requested equation count, more equations need a bigger grid
        val area = (requestedEquationCount * 15).coerceIn(121, 400) // 121 = 11x11 minimum
        val side = Math.sqrt(area.toDouble()).toInt().coerceIn(11, 20)
        rows = side
        cols = side
    } else {
        rows = Random.nextInt(11, 21)
        cols = Random.nextInt(11, 21)
    }

    val minEq = (rows * cols) / 20 // equation count range is based on grid area — roughly 1 equation per 10-20 cells
    val maxEq = (rows * cols) / 10

    val equationCount = requestedEquationCount
        ?: Random.nextInt(minEq.coerceAtLeast(6), (maxEq.coerceAtLeast(7)) + 1)

    fun attempt(): Pair<Puzzle, List<Equation>>? {
        val placed = mutableListOf<Equation>()
        var attempts = 0

        while (placed.isEmpty() && attempts < 500) {
            attempts++
            val dir = if (Random.nextBoolean()) Direction.HORIZONTAL else Direction.VERTICAL
            val startRow = if (dir == Direction.VERTICAL) Random.nextInt(0, rows - 4) else Random.nextInt(0, rows)
            val startCol = if (dir == Direction.HORIZONTAL) Random.nextInt(0, cols - 4) else Random.nextInt(0, cols)
            val eq = randomEquation(startRow, startCol, dir) ?: continue
            if (cells(eq).any { (r, c) -> r !in 0 until rows || c !in 0 until cols }) continue
            placed.add(eq)
        }

        if (placed.isEmpty()) return null

        attempts = 0
        while (placed.size < equationCount && attempts < 5000) {
            attempts++
            val eq = if (Random.nextFloat() < 0.85f) { // tries to place a new equation that crosses an existing one 85% of the time
                tryIntersectingEquation(placed, rows, cols)
            } else {
                val dir = if (Random.nextBoolean()) Direction.HORIZONTAL else Direction.VERTICAL
                val startRow = if (dir == Direction.VERTICAL) Random.nextInt(0, rows - 4) else Random.nextInt(0, rows)
                val startCol = if (dir == Direction.HORIZONTAL) Random.nextInt(0, cols - 4) else Random.nextInt(0, cols)
                randomEquation(startRow, startCol, dir)
            } ?: continue
            if (cells(eq).any { (r, c) -> r !in 0 until rows || c !in 0 until cols }) continue
            if (isCompatible(eq, placed, rows, cols)) placed.add(eq)
        }

        if (placed.size < (requestedEquationCount ?: 3)) return null

        val puzzle = buildPuzzle(rows, cols, placed)

        return Pair(puzzle, placed)

    }

    repeat(20) { // try to place equations up to 20 times before giving up and falling back to a default puzzle
        val result = attempt()
        if (result != null) return result
    }

    return generatePuzzleWithEquations()
}

private fun tryIntersectingEquation(
    placed: List<Equation>,
    rows: Int,
    cols: Int
): Equation? {

    val target = placed.random()
    val targetCells = cells(target)

    val numberPositions = listOf(0, 2, 4).map { targetCells[it] }
    val crossPos = numberPositions.random() // pick a random number position on an existing equation to be the crossing point for the new one
    val crossRow = crossPos.first
    val crossCol = crossPos.second

    val crossValue = when (targetCells.indexOf(crossPos)) {
        0 -> target.left
        2 -> target.right
        4 -> target.result
        else -> return null
    }

    val newDir = if (target.direction == Direction.HORIZONTAL) Direction.VERTICAL else Direction.HORIZONTAL // the new equation must run in the opposite direction to the one it crosses

    val numberIndices = listOf(0, 2, 4).shuffled()
    for (newIndex in numberIndices) {

        val startRow = if (newDir == Direction.VERTICAL) crossRow - newIndex else crossRow
        val startCol = if (newDir == Direction.HORIZONTAL) crossCol - newIndex else crossCol

        if (newDir == Direction.VERTICAL && startRow !in 0..rows - 5) continue
        if (newDir == Direction.HORIZONTAL && startCol !in 0..cols - 5) continue
        if (startRow !in 0 until rows || startCol !in 0 until cols) continue

        val eq = randomEquationWithConstraint(startRow, startCol, newDir, newIndex, crossValue)
            ?: continue

        if (cells(eq).any { (r, c) -> r !in 0 until rows || c !in 0 until cols }) continue
        if (isCompatible(eq, placed, rows, cols)) return eq
    }

    return null
}

private fun randomEquationWithConstraint(
    startRow: Int,
    startCol: Int,
    dir: Direction,
    fixedIndex: Int,
    fixedValue: Int
): Equation? {
    val ops = listOf("+", "-", "×", "/")
    repeat(50) {
        val op = ops.random()
        val allowedBlanks = when (fixedIndex) {
            0 -> listOf(BlankPosition.RIGHT, BlankPosition.RESULT)
            2 -> listOf(BlankPosition.LEFT,  BlankPosition.RESULT)
            4 -> listOf(BlankPosition.LEFT,  BlankPosition.RIGHT)
            else -> BlankPosition.entries.toList()
        }
        val blank = allowedBlanks.random()
        val triple = when (fixedIndex) {
            0 -> generateWithFixedLeft(fixedValue, op)
            2 -> generateWithFixedRight(fixedValue, op)
            4 -> generateWithFixedResult(fixedValue, op)
            else -> null
        } ?: return@repeat

        val (left, right, result) = triple
        return Equation(startRow, startCol, dir, left, op, right, result, blank)
    }
    return null
}

private fun generateWithFixedLeft(left: Int, op: String): Triple<Int, Int, Int>? {
    repeat(20) {
        val right = Random.nextInt(1, 13)
        val result: Int = when (op) {
            "+"  -> left + right
            "-"  -> if (left > right) left - right else return@repeat
            "×"  -> left * right
            "/"  -> if (left % right == 0) left / right else return@repeat
            else -> return@repeat
        }
        if (result in 1..99) return Triple(left, right, result)
    }
    return null
}

private fun generateWithFixedRight(right: Int, op: String): Triple<Int, Int, Int>? {
    repeat(20) {
        val left = Random.nextInt(1, 13)
        val result: Int = when (op) {
            "+"  -> left + right
            "-"  -> if (left > right) left - right else return@repeat
            "×"  -> left * right
            "/"  -> if (left % right == 0) left / right else return@repeat
            else -> return@repeat
        }
        if (result in 1..99) return Triple(left, right, result)
    }
    return null
}

private fun generateWithFixedResult(result: Int, op: String): Triple<Int, Int, Int>? {
    repeat(50) {
        val left = Random.nextInt(1, 13)
        val right: Int = when (op) {
            "+"  -> result - left
            "-"  -> left - result
            "×"  -> if (result % left == 0) result / left else return@repeat
            "/"  -> left / result
            else -> return@repeat
        }
        if (right !in 1..12) return@repeat
        val check = when (op) {
            "+"  -> left + right
            "-"  -> left - right
            "×"  -> left * right
            "/"  -> if (right != 0 && left % right == 0) left / right else return@repeat
            else -> return@repeat
        }
        if (check == result) return Triple(left, right, result)
    }
    return null
}

private fun isCompatible(
    eq: Equation,
    placed: List<Equation>,
    rows: Int,
    cols: Int
): Boolean {
    val newMap   = buildCellMap(eq)
    val newCells = cells(eq)

    for (existing in placed) {
        val existingMap   = buildCellMap(existing)
        val existingCells = cells(existing)

        for ((pos, newEntry) in newMap) {
            if (pos !in existingMap) continue
            val (_, newValue) = newEntry
            val (_, existingValue) = existingMap[pos]!!

            if (existing.direction == eq.direction) return false
            if (newValue != existingValue) return false

            val newIndex      = newCells.indexOf(pos)
            val existingIndex = existingCells.indexOf(pos)
            val numberIndices = setOf(0, 2, 4)
            if (newIndex !in numberIndices || existingIndex !in numberIndices) return false
        }

        if (existing.direction == eq.direction) {
            if (eq.direction == Direction.HORIZONTAL) {
                if (abs(eq.startRow - existing.startRow) == 1) { // two equations in the same direction can't be adjacent rows/cols
                    val newCols   = (eq.startCol..eq.startCol + 4).toSet()
                    val existCols = (existing.startCol..existing.startCol + 4).toSet()
                    if (newCols.intersect(existCols).isNotEmpty()) return false
                }
            } else {
                if (abs(eq.startCol - existing.startCol) == 1) {
                    val newRows   = (eq.startRow..eq.startRow + 4).toSet()
                    val existRows = (existing.startRow..existing.startRow + 4).toSet()
                    if (newRows.intersect(existRows).isNotEmpty()) return false
                }
            }
        }

        if (eq.direction == Direction.HORIZONTAL && existing.direction == Direction.HORIZONTAL
            && eq.startRow == existing.startRow) {
            if (eq.startCol == existing.startCol + 5) return false
            if (existing.startCol == eq.startCol + 5) return false
        }
        if (eq.direction == Direction.VERTICAL && existing.direction == Direction.VERTICAL
            && eq.startCol == existing.startCol) {
            if (eq.startRow == existing.startRow + 5) return false
            if (existing.startRow == eq.startRow + 5) return false
        }

        if (eq.direction != existing.direction) {
            val newFirst   = newCells[0]
            val newLast    = newCells[4]
            val existFirst = existingCells[0]
            val existLast  = existingCells[4]

            val newTips = if (eq.direction == Direction.HORIZONTAL)
                listOf(Pair(newFirst.first, newFirst.second - 1),
                    Pair(newLast.first,  newLast.second  + 1))
            else
                listOf(Pair(newFirst.first - 1, newFirst.second),
                    Pair(newLast.first  + 1, newLast.second ))

            val existTips = if (existing.direction == Direction.HORIZONTAL)
                listOf(Pair(existFirst.first, existFirst.second - 1),
                    Pair(existLast.first,  existLast.second  + 1))
            else
                listOf(Pair(existFirst.first - 1, existFirst.second),
                    Pair(existLast.first  + 1, existLast.second ))

            for (tip in newTips) {
                if (tip in existingMap && tip !in newMap) return false
            }
            for (tip in existTips) {
                if (tip in newMap && tip !in existingMap) return false
            }
        }
    }

    return true
}

private fun buildCellMap(eq: Equation): Map<Pair<Int,Int>, Pair<CellType, String>> {
    val positions = cells(eq)
    val entries = listOf(
        eq.left.toString()   to CellType.GIVEN_NUMBER,
        eq.op                to CellType.OPERATOR,
        eq.right.toString()  to CellType.GIVEN_NUMBER,
        "="                  to CellType.EQUALS,
        eq.result.toString() to CellType.GIVEN_NUMBER
    )
    return positions.zip(entries).associate { (pos, entry) ->
        val (value, type) = entry
        pos to Pair(type, value)
    }
}

fun cells(eq: Equation): List<Pair<Int, Int>> {
    return (0..4).map { i ->
        if (eq.direction == Direction.HORIZONTAL)
            Pair(eq.startRow, eq.startCol + i)
        else
            Pair(eq.startRow + i, eq.startCol)
    }
}

private fun randomEquation(
    startRow: Int,
    startCol: Int,
    dir: Direction
): Equation? {
    val ops = listOf("+", "-", "×", "/")
    val op = ops.random()
    val blank = BlankPosition.entries.random()
    val (left, right, result) = generateOperands(op) ?: return null
    return Equation(startRow, startCol, dir, left, op, right, result, blank)
}

private fun generateOperands(op: String): Triple<Int, Int, Int>? {
    repeat(50) {
        val left = Random.nextInt(1, 13)
        val right = Random.nextInt(1, 13)
        val result: Int = when (op) {
            "+"  -> left + right
            "-"  -> if (left > right) left - right else return@repeat
            "×"  -> left * right
            "/"  -> if (left % right == 0) left / right else return@repeat
            else -> return@repeat
        }
        if (result in 1..99) return Triple(left, right, result)
    }
    return null
}

private fun buildPuzzle(rows: Int, cols: Int, equations: List<Equation>): Puzzle {
    data class CellIntention(val type: CellType, val value: String?)

    val intentions = mutableMapOf<Pair<Int,Int>, MutableList<CellIntention>>()

    for (eq in equations) {
        val positions = cells(eq)
        val entries = listOf(
            eq.left.toString()   to CellType.GIVEN_NUMBER,
            eq.op                to CellType.OPERATOR,
            eq.right.toString()  to CellType.GIVEN_NUMBER,
            "="                  to CellType.EQUALS,
            eq.result.toString() to CellType.GIVEN_NUMBER
        )
        entries.forEachIndexed { i, (value, type) ->
            val pos = positions[i]
            val isBlank = when (eq.blankPosition) {
                BlankPosition.LEFT   -> i == 0
                BlankPosition.RIGHT  -> i == 2
                BlankPosition.RESULT -> i == 4
            }
            intentions.getOrPut(pos) { mutableListOf() }
                .add(CellIntention(if (isBlank) CellType.EMPTY_INPUT else type, if (isBlank) null else value))
        }
    }

    val cellMap = mutableMapOf<Pair<Int,Int>, PuzzleCell>()
    for ((pos, intentionList) in intentions) {
        val (r, c) = pos
        val resolved = intentionList.firstOrNull { it.type == CellType.EMPTY_INPUT }
            ?: intentionList.first()
        cellMap[pos] = PuzzleCell(r, c, resolved.type, resolved.value)
    }

    return Puzzle(rows, cols, cellMap.values.toList())
}
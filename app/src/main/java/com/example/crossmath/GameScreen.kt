@file:Suppress("AssignedValueIsNeverRead")

package com.example.crossmath

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class GameScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameScreenContent() }
    }
}

@Composable
fun GameScreenContent() {
    val context  = LocalContext.current
    val activity = context as? ComponentActivity

    val requestedCount = remember {
        activity?.intent?.getIntExtra("EQUATION_COUNT", -1)?.takeIf { it > 0 } // read the equation count passed from MainActivity
    }

    // Retrieve (or create) the ViewModel scoped to this Activity, using a custom factory
    // Source: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-getting-started
    val viewModel: PuzzleViewModel = viewModel( // Here so puzzle/score/userInputs are not lost when the device rotates`
        key     = "puzzle_$requestedCount",
        factory = PuzzleViewModelFactory(requestedCount)
    )

    val puzzle     = viewModel.puzzle
    val equations  = viewModel.equations
    val userInputs = viewModel.userInputs
    val score      = viewModel.score

    // rememberSaveable persists these through rotation unlike a regular remember
    var selectedCell         by rememberSaveable { mutableStateOf<String?>(null) }
    var showExitConfirm      by rememberSaveable { mutableStateOf(false) }
    var showNewPuzzleConfirm by rememberSaveable { mutableStateOf(false) }

    var timerEnabled  by rememberSaveable { mutableStateOf(false) }
    var timeRemaining by rememberSaveable { mutableIntStateOf(60) }
    var gameOver      by rememberSaveable { mutableStateOf(false) }

    var showWinDialog by rememberSaveable { mutableStateOf(false) }

    // Countdown timer using LaunchedEffect and coroutine delay
    // Adapted from: https://developer.android.com/develop/ui/compose/side-effects#launchedeffect
    LaunchedEffect(timerEnabled) {
        if (timerEnabled) {
            timeRemaining = 60
            gameOver = false
            while (timeRemaining > 0) {
                delay(1000L) // fires roughly every second, works for the coursework but could be more precise
                timeRemaining--
            }
            gameOver = true
            selectedCell = null
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L) //fires roughly every second, works for the coursework but can probably be done better
            if (timerEnabled && !gameOver) {
                if (timeRemaining > 0) {
                    timeRemaining--
                }
                if (timeRemaining == 0) {
                    gameOver = true
                    selectedCell = null
                }
            }
        }
    }

    val allSolved = equations.isNotEmpty() && // true only when every equation in the puzzle is correct — triggers the win dialog
            equations.all { evaluateEquation(it, userInputs) == EquationState.CORRECT }

    val cellHighlights: Map<String, EquationState> = remember(userInputs) { // recompute highlights every time userInputs changes — this is what drives green/red colouring
        val map = mutableMapOf<String, EquationState>()
        for (eq in equations) {
            val state = evaluateEquation(eq, userInputs)
            if (state != EquationState.INCOMPLETE) {             // only colour cells that belong to a complete (correct or incorrect) equation
                for ((r, c) in cells(eq)) map["$r,$c"] = state
            }
        }
        map
    }

    val hasProgress = userInputs.isNotEmpty() && !allSolved

    LaunchedEffect(allSolved) { // show the win dialog as soon as allSolved becomes true
        if (allSolved) {
            showWinDialog = true
            selectedCell  = null
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar — compact single row in landscape, full bar in portrait
        if (isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (timerEnabled) {
                    Text(
                        text  = if (gameOver) "GAME OVER!" else "⏱ $timeRemaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (gameOver || timeRemaining <= 10) Color(0xFFF44336) else Color(0xFF212121)
                    )
                } else {
                    Text(
                        text  = "Cross Math",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("60s Timer", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = timerEnabled,
                    onCheckedChange = { checked ->
                        if (!gameOver || !checked) {
                            timerEnabled = checked
                            if (checked) {
                                timeRemaining = 60
                                gameOver = false
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text  = "Score: $score",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (timerEnabled) {
                    Text(
                        text     = if (gameOver) "GAME OVER!" else "⏱ $timeRemaining",
                        style    = MaterialTheme.typography.titleMedium,
                        color    = if (gameOver || timeRemaining <= 10) Color(0xFFF44336) else Color(0xFF212121),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                } else {
                    Text(
                        text     = "Cross Math",
                        style    = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
                Text(
                    text     = "Score: $score",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text("60s Timer", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = timerEnabled,
                    onCheckedChange = { checked ->
                        if (!gameOver || !checked) {
                            timerEnabled = checked
                            if (checked) {
                                timeRemaining = 60
                                gameOver = false
                            }
                        }
                    }
                )
            }
        }

        // Grid + buttons
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // rememberSaveable with ScrollState.Saver preserves scroll position across rotation
                        // Source: https://developer.android.com/reference/kotlin/androidx/compose/foundation/ScrollState
                        .verticalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) })
                        .horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) })
                        .verticalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) })
                        .horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }),
                    contentAlignment = Alignment.TopCenter
                ) {
                    BoxWithConstraints(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // BoxWithConstraints exposes maxWidth so we can size cells relative to available space
                        // Adapted from: https://developer.android.com/develop/ui/compose/layouts/adaptive
                        val cellFromWidth = this.maxWidth / puzzle.cols // cell size is calculated from available width divided by number of columns, so it's never too big or small
                        val cellSize: Dp  = cellFromWidth.coerceIn(20.dp, 24.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            for (row in 0 until puzzle.rows) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier              = Modifier.fillMaxWidth()
                                ) {
                                    for (col in 0 until puzzle.cols) {
                                        val cell = puzzle.cellAt(row, col)
                                        val key  = "$row,$col"
                                        PuzzleCellView(
                                            cell      = cell,
                                            userValue = userInputs[key] ?: "",
                                            highlight = cellHighlights[key],
                                            cellSize  = cellSize,
                                            onClick   = {
                                                if (!gameOver && !allSolved && cell?.type == CellType.EMPTY_INPUT) { // don't allow clicking cells if the timer has expired or the puzzle is already complete
                                                    selectedCell = key
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    if (!allSolved && !gameOver) {
                        Button(
                            onClick = {
                                if (hasProgress) showNewPuzzleConfirm = true
                                else {
                                    viewModel.newPuzzle()
                                    selectedCell = null
                                    timerEnabled = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("New Puzzle") }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (allSolved) {
                        Text(
                            text     = "Return to menu to start a new game",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = Color(0xFF4CAF50),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (hasProgress && !gameOver) showExitConfirm = true
                            else activity?.finish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Return to Menu") }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) })
                    .horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }),
                contentAlignment = Alignment.TopCenter
            ) {
                BoxWithConstraints(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val cellFromWidth = this.maxWidth / puzzle.cols
                    val cellSize: Dp  = cellFromWidth.coerceIn(20.dp, 38.dp)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        for (row in 0 until puzzle.rows) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier              = Modifier.fillMaxWidth()
                            ) {
                                for (col in 0 until puzzle.cols) {
                                    val cell = puzzle.cellAt(row, col)
                                    val key  = "$row,$col"
                                    PuzzleCellView(
                                        cell      = cell,
                                        userValue = userInputs[key] ?: "",
                                        highlight = cellHighlights[key],
                                        cellSize  = cellSize,
                                        onClick   = {
                                            if (!gameOver && !allSolved && cell?.type == CellType.EMPTY_INPUT) {
                                                selectedCell = key
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                if (!allSolved && !gameOver) {
                    Button(
                        onClick = {
                            if (hasProgress) showNewPuzzleConfirm = true
                            else {
                                viewModel.newPuzzle()
                                selectedCell = null
                                timerEnabled = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) { Text("New Puzzle") }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (allSolved) {
                    Text(
                        text     = "Return to menu to start a new game",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = Color(0xFF4CAF50),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (hasProgress && !gameOver) showExitConfirm = true
                        else activity?.finish()
                    },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) { Text("Return to Menu") }
            }
        }
    }

    if (showWinDialog && allSolved) {
        AlertDialog(
            onDismissRequest = { showWinDialog = false },
            title            = { Text("Puzzle Complete!") },
            text             = { Text("Well done! You scored $score points.") },
            confirmButton    = {
                TextButton(onClick = {
                    showWinDialog = false
                    activity?.finish()
                }) { Text("Return to Menu") }
            },
            dismissButton = {
                TextButton(onClick = { showWinDialog = false }) { Text("Close") }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title            = { Text("Return to Menu?") },
            text             = { Text("You haven't completed the puzzle.\nCurrent progress will be lost.\nContinue?") },
            confirmButton    = {
                TextButton(onClick = {
                    showExitConfirm = false
                    activity?.finish()
                }) { Text("Yes, Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewPuzzleConfirm) {
        AlertDialog(
            onDismissRequest = { showNewPuzzleConfirm = false },
            title            = { Text("Start New Puzzle?") },
            text             = { Text("You haven't completed the puzzle.\nStarting a new puzzle will clear your current answers.\nContinue?") },
            confirmButton    = {
                TextButton(onClick = {
                    showNewPuzzleConfirm = false
                    viewModel.newPuzzle()
                    selectedCell = null
                    timerEnabled = false
                }) { Text("Yes, New Puzzle") }
            },
            dismissButton = {
                TextButton(onClick = { showNewPuzzleConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (!gameOver && !allSolved) { // only show the number pad if the game is still active and a cell is selected
        selectedCell?.let { key ->
            NumberPadDialog(
                currentValue  = userInputs[key] ?: "",
                onDismiss     = { selectedCell = null },
                onValueChange = { newVal: String ->
                    viewModel.setInput(key, newVal)
                    selectedCell = null
                }
            )
        }
    }

}

@Composable
fun PuzzleCellView(
    cell: PuzzleCell?,
    userValue: String,
    highlight: EquationState?,
    cellSize: Dp,
    onClick: () -> Unit
) {
    val isBlock = cell == null || cell.type == CellType.BLOCK

    val bgColor = when {
        isBlock                              -> Color(0xFF1A1A1A)
        highlight == EquationState.CORRECT   -> Color(0xFF81C784)
        highlight == EquationState.INCORRECT -> Color(0xFFE57373)
        else                                 -> Color.White
    }

    val borderColor = when {
        isBlock                              -> Color.Transparent
        highlight == EquationState.CORRECT   -> Color(0xFF4CAF50)
        highlight == EquationState.INCORRECT -> Color(0xFFF44336)
        else                                 -> Color(0xFFCCCCCC)
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .padding(1.dp)
            .background(bgColor)
            .then(if (!isBlock) Modifier.border(1.dp, borderColor) else Modifier)
            .then(if (cell?.type == CellType.EMPTY_INPUT) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (!isBlock) {
            when (cell.type) {
                CellType.GIVEN_NUMBER,
                CellType.OPERATOR,
                CellType.EQUALS -> Text(
                    text     = cell.value ?: "",
                    fontSize = (cellSize.value * 0.3f).sp,
                    color    = Color(0xFF212121),
                    maxLines = 1
                )
                CellType.EMPTY_INPUT -> Text(
                    text     = userValue,
                    fontSize = (cellSize.value * 0.3f).sp,
                    color    = Color(0xFF1565C0),
                    maxLines = 1
                )
                else -> {}
            }
        }
    }
}

@Composable
fun NumberPadDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit
) {
    var input by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape          = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text     = input.ifEmpty { "Enter a number" },
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                listOf("1","2","3","4","5","6","7","8","9").chunked(3).forEach { rowDigits ->
                    Row {
                        rowDigits.forEach { digit ->
                            Button(
                                onClick  = { input += digit },
                                modifier = Modifier.padding(4.dp).size(56.dp)
                            ) { Text(digit) }
                        }
                    }
                }

                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick  = { if (input.isNotEmpty()) input = input.dropLast(1) },
                        modifier = Modifier.padding(4.dp).size(56.dp)
                    ) { Text("⌫") }
                    Button(
                        onClick  = { if (input.isNotEmpty()) input += "0" },
                        modifier = Modifier.padding(4.dp).size(56.dp)
                    ) { Text("0") }
                    Button(
                        onClick  = { if (input.isNotEmpty()) onValueChange(input) },
                        modifier = Modifier.padding(4.dp).size(56.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("✓") }
                }
            }
        }
    }
}
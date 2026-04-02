//https://youtu.be/17h6r9IAeDQ <- demo video
@file:Suppress("AssignedValueIsNeverRead")

package com.example.crossmath

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DisplayMenu() }
    }
}

@Composable
fun DisplayMenu() {
    var openDialog    by remember { mutableStateOf(false) }
    var equationInput by remember { mutableStateOf("") }
    var inputError    by remember { mutableStateOf("") }
    val context = LocalContext.current

    val maxEquations = (20 * 20) / 10

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE // check device orientation so we can show a tighter layout in landscape

    if (isLandscape) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to the \nCross Math \ngame!",
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = equationInput,
                onValueChange = {
                    equationInput = it.filter { c -> c.isDigit() } // filter out non-digit characters so the user can only type numbers into this field
                    inputError = ""
                },
                label = { Text("Number of equations (optional)") },
                // Restrict keyboard to numeric input only
                // Source: https://developer.android.com/develop/ui/compose/text/user-input
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = inputError.isNotEmpty(),
                supportingText = { if (inputError.isNotEmpty()) Text(inputError) },
                modifier = Modifier.fillMaxWidth(0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val count = equationInput.toIntOrNull()
                        when {
                            equationInput.isEmpty() -> { // if left blank, it just starts a random game with no equation count preference
                                context.startActivity(Intent(context, GameScreen::class.java))
                            }
                            count == null || count < 1 -> {
                                inputError = "Please enter a number between 1 and $maxEquations."
                            }
                            count > maxEquations -> {
                                inputError = "Maximum is $maxEquations equations. Please enter a smaller number."
                            }
                            else -> {
                                context.startActivity(
                                    Intent(context, GameScreen::class.java).apply { // pass the equation count to GameScreen via an Intent extra so it can generate the right size puzzle
                                        putExtra("EQUATION_COUNT", count)
                                    }
                                )
                            }
                        }
                    }
                ) { Text("New Game") }

                Button(
                    onClick = {
                        context.startActivity(Intent(context, AdvancedLevel::class.java))
                    }
                ) { Text("Advanced Level") }

                Button(onClick = { openDialog = true }) { Text("About") }
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to the \nCross Math \ngame!",
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(80.dp))

            OutlinedTextField(
                value = equationInput,
                onValueChange = {
                    equationInput = it.filter { c -> c.isDigit() }
                    inputError = ""
                },
                label = { Text("Number of equations (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = inputError.isNotEmpty(),
                supportingText = { if (inputError.isNotEmpty()) Text(inputError) },
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val count = equationInput.toIntOrNull()
                    when {
                        equationInput.isEmpty() -> {
                            context.startActivity(Intent(context, GameScreen::class.java))
                        }
                        count == null || count < 1 -> {
                            inputError = "Please enter a number between 1 and $maxEquations."
                        }
                        count > maxEquations -> {
                            inputError = "Maximum is $maxEquations equations. Please enter a smaller number."
                        }
                        else -> {
                            context.startActivity(
                                Intent(context, GameScreen::class.java).apply {
                                    putExtra("EQUATION_COUNT", count)
                                }
                            )
                        }
                    }
                }
            ) { Text("New Game") }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(context, AdvancedLevel::class.java))
                }
            ) { Text("Advanced Level") }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { openDialog = true }) { Text("About") }
        }
    }

    if (openDialog) {
        AlertDialog(
            title = { Text("'Student Name'\n'Student ID'") },
            text = {
                Text(
                    "I confirm that I understand what plagiarism is and have read and\n" +
                            "understood the section on Assessment Offences in the Essential\n" +
                            "Information for Students. The work that I have submitted is\n" +
                            "entirely my own. Any work from other authors is duly referenced\n" +
                            "and acknowledged."
                )
            },
            onDismissRequest = { }, // onDismissRequest is empty here intentionally — the user must press Confirm to acknowledge the statement
            confirmButton = {
                TextButton(onClick = { openDialog = false }) { Text("Confirm") }
            }
        )
    }
}

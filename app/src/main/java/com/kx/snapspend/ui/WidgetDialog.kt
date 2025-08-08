package com.kx.snapspend.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun WidgetDialog(
    collectionName: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // This AlertDialog is the key to making it a pop-up
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to $collectionName") },
        text = {
            Column {
                Text("Enter the amount for your expense.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount (RM)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done // Important for the keyboard action
                    ),
                    // This handles the "Enter" key on the keyboard
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val amount = text.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                onConfirm(amount)
                                keyboardController?.hide()
                            }
                        }
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                // Automatically request focus and show the keyboard
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = text.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
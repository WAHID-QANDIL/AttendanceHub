package org.wahid.attendancehub.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PermissionDialog(
    modifier: Modifier = Modifier,
    permission: String,
    isPermissionDeclined: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmRequest:@Composable () -> Unit,
    onGoToAppSettingsClick:() -> Unit,

    ) {

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = onConfirmRequest,
        title = { Text("Request permission") },
        text = {Text("You have to grant the $permission permission, to continue using the app")},
        modifier = modifier,
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onGoToAppSettingsClick) {
                Text("Go to App Settings")
            }
        },

    )

}
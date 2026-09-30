package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.permissions.AppPermission

@Composable
fun PermissionPromptDialog(
    permission: AppPermission,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "${permission.title} Permission",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "${permission.usageDescription}\n\nNotice: This app never scans or accesses private data in the background.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.testTag("grant_permission_button")
            ) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_permission_button")
            ) {
                Text("Not Now")
            }
        }
    )
}

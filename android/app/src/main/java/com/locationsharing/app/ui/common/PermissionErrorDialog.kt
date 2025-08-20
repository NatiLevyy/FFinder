package com.locationsharing.app.ui.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.locationsharing.app.R
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Dialog for displaying permission denied errors with helpful actions
 */
@Composable
fun PermissionErrorDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onFixRules: () -> Unit = {}
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Permission denied",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Your account is authenticated, but server rules blocked this action. Please update Firestore rules to allow writing your own location, then retry.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            },
            dismissButton = {
                TextButton(onClick = onFixRules) {
                    Text("Fix Rules")
                }
            }
        )
    }
}

@Preview
@Composable
fun PermissionErrorDialogPreview() {
    FFinderTheme {
        PermissionErrorDialog(
            isVisible = true,
            onDismiss = {},
            onRetry = {},
            onFixRules = {}
        )
    }
}
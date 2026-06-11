package com.example.stylemate.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.stylemate.R

/**
 * Dialog giải thích lý do cần quyền trước khi thực sự request.
 *
 * Tuân thủ Android Best Practice:
 * 1. Gọi shouldShowRequestPermissionRationale() để kiểm tra
 * 2. Nếu true → hiện rationale dialog giải thích
 * 3. User đồng ý → launch permission request
 * 4. User từ chối → không request, fallback gracefully
 *
 * @param title Tiêu đề dialog
 * @param message Nội dung giải thích
 * @param icon Icon đại diện cho permission
 * @param onGrant Khi user đồng ý cấp quyền
 * @param onDeny Khi user từ chối
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    icon: ImageVector? = null,
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onGrant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.grant_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text(
                    stringResource(R.string.deny_button),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Dialog hướng dẫn user vào Settings để bật quyền thủ công
 * (khi user đã từ chối vĩnh viễn - shouldShowRequestPermissionRationale() == false)
 */
@Composable
fun PermissionSettingsRedirectDialog(
    title: String,
    message: String,
    icon: ImageVector? = null,
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onGoToSettings,
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.settings_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.deny_button),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
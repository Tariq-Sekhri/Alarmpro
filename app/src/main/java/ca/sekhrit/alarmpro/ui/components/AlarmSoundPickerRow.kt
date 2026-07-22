package ca.sekhrit.alarmpro.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import ca.sekhrit.alarmpro.util.AlarmSoundUtils

@Composable
fun AlarmSoundPickerRow(
    soundName: String,
    pickerUri: Uri?,
    onSoundPicked: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onSoundPicked(AlarmSoundUtils.parsePickerResult(result.data))
        }
    }
    val audioFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some document providers grant access without supporting persistable permissions.
            }
            onSoundPicked(uri)
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Choose alarm sound") },
            text = { Text("Choose a system ringtone or select any audio file on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSourceDialog = false
                        ringtoneLauncher.launch(AlarmSoundUtils.createPickerIntent(context, pickerUri))
                    }
                ) {
                    Text("System ringtone")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showSourceDialog = false
                            audioFileLauncher.launch(arrayOf("audio/*"))
                        }
                    ) {
                        Text("Audio file")
                    }
                    TextButton(onClick = { showSourceDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    OutlinedButton(
        onClick = { showSourceDialog = true },
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
        ) {
            Text(soundName, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Change alarm sound"
            )
        }
    }
}

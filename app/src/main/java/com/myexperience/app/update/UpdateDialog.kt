package com.myexperience.app.update

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myexperience.app.ui.theme.Pink
import kotlinx.coroutines.launch

/** Dialog globale che segue lo stato di [AppUpdater]: nuova versione, download, installazione. */
@Composable
fun UpdateDialog() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by AppUpdater.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        when (val s = state) {
            UpdateState.UpToDate -> {
                Toast.makeText(ctx, "MyExperience è aggiornata (v${AppUpdater.currentVersion})", Toast.LENGTH_SHORT).show()
                AppUpdater.dismiss()
            }
            is UpdateState.Failed -> {
                Toast.makeText(ctx, s.message, Toast.LENGTH_SHORT).show()
                AppUpdater.dismiss()
            }
            else -> Unit
        }
    }

    when (val s = state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = { AppUpdater.dismiss() },
            icon = { Icon(Icons.Rounded.SystemUpdate, null, tint = Pink) },
            title = { Text("Nuova versione ${s.release.version}") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text("Hai la v${AppUpdater.currentVersion}. Vuoi scaricare e installare l'aggiornamento?")
                    if (s.release.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(s.release.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { scope.launch { AppUpdater.download(ctx.applicationContext, s.release) } }) {
                    Text("Aggiorna")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { AppUpdater.skip(ctx, s.release.version) }) { Text("Salta") }
                    TextButton(onClick = { AppUpdater.dismiss() }) { Text("Più tardi") }
                }
            },
        )
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Download v${s.release.version}") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
            },
            confirmButton = {},
        )
        is UpdateState.ReadyToInstall -> AlertDialog(
            onDismissRequest = { AppUpdater.dismiss() },
            title = { Text("Pronto da installare") },
            text = {
                Text(
                    if (AppUpdater.canInstall(ctx)) "L'aggiornamento v${s.release.version} è stato scaricato."
                    else "Consenti a MyExperience di installare app, poi torna qui e tocca Installa."
                )
            },
            confirmButton = { TextButton(onClick = { AppUpdater.install(ctx, s.apk) }) { Text("Installa") } },
            dismissButton = { TextButton(onClick = { AppUpdater.dismiss() }) { Text("Chiudi") } },
        )
        else -> Unit
    }
}

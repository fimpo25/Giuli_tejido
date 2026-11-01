package com.giuli.tejido.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giuli.tejido.data.TimeSession
import com.giuli.tejido.ui.ConfirmDialog
import com.giuli.tejido.ui.formatClock
import com.giuli.tejido.ui.formatDateTime
import com.giuli.tejido.ui.formatDuration
import com.giuli.tejido.ui.formatHoursMinutes

@Composable
fun TimerTab(
    runningSince: Long?,
    trackedMs: Long,
    sessions: List<TimeSession>,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onAddManual: (Long) -> Unit,
    onDeleteSession: (TimeSession) -> Unit,
    onReset: () -> Unit,
) {
    val running = runningSince != null
    var showManual by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<TimeSession?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "clock") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    formatDuration(trackedMs),
                    style = TextStyle(
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Light,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (runningSince != null) "En marcha desde las ${formatClock(runningSince)}" else "En pausa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = if (running) onPause else onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = if (running) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    Icon(
                        if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            running -> "Pausar"
                            trackedMs > 0 -> "Continuar"
                            else -> "Iniciar"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showManual = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ajustar")
                    }
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        enabled = trackedMs > 0 || running,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reiniciar")
                    }
                }
                Text(
                    "El cronómetro sigue contando aunque cierres la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            HorizontalDivider()
        }

        item(key = "header") {
            Text(
                if (sessions.isEmpty()) "Sin sesiones registradas" else "Sesiones (${sessions.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        items(sessions, key = { it.id }) { s ->
            ListItem(
                headlineContent = {
                    Text(
                        (if (s.durationMs < 0) "−" else "") + formatDuration(kotlin.math.abs(s.durationMs)),
                        fontWeight = FontWeight.Medium,
                    )
                },
                supportingContent = {
                    Text(
                        if (s.manual) "Ajuste manual · ${formatDateTime(s.startedAt)}"
                        else "${formatDateTime(s.startedAt)} → ${formatClock(s.startedAt + s.durationMs)}",
                    )
                },
                trailingContent = {
                    IconButton(onClick = { sessionToDelete = s }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar sesión")
                    }
                },
            )
        }
    }

    if (showManual) {
        ManualTimeDialog(
            onDismiss = { showManual = false },
            onConfirm = {
                showManual = false
                onAddManual(it)
            },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Reiniciar tiempo",
            text = "Se borran todas las sesiones y el contador vuelve a 0:00:00.",
            confirmText = "Reiniciar",
            onConfirm = {
                confirmReset = false
                onReset()
            },
            onDismiss = { confirmReset = false },
        )
    }
    sessionToDelete?.let { s ->
        ConfirmDialog(
            title = "Borrar sesión",
            text = "Se descuentan ${formatHoursMinutes(s.durationMs)} del total.",
            confirmText = "Borrar",
            onConfirm = {
                sessionToDelete = null
                onDeleteSession(s)
            },
            onDismiss = { sessionToDelete = null },
        )
    }
}

@Composable
private fun ManualTimeDialog(onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var subtract by rememberSaveable { mutableStateOf(false) }

    val h = hours.toLongOrNull() ?: 0L
    val m = minutes.toLongOrNull() ?: 0L
    val totalMs = (h * 60 + m) * 60_000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar tiempo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Por si te olvidaste de iniciar o pausar el cronómetro.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !subtract, onClick = { subtract = false }, label = { Text("Sumar") })
                    FilterChip(selected = subtract, onClick = { subtract = true }, label = { Text("Restar") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { v -> hours = v.filter { it.isDigit() }.take(4) },
                        label = { Text("Horas") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { v -> minutes = v.filter { it.isDigit() }.take(4) },
                        label = { Text("Minutos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = totalMs > 0,
                onClick = { onConfirm(if (subtract) -totalMs else totalMs) },
            ) { Text(if (subtract) "Restar" else "Sumar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

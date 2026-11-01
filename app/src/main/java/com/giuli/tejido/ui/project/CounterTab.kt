package com.giuli.tejido.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giuli.tejido.data.Counter
import com.giuli.tejido.ui.ConfirmDialog
import com.giuli.tejido.ui.EmptyState
import com.giuli.tejido.ui.TextInputDialog

@Composable
fun CounterTab(vm: ProjectViewModel) {
    val counters by vm.counters.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Counter?>(null) }
    var setting by remember { mutableStateOf<Counter?>(null) }
    var deleting by remember { mutableStateOf<Counter?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (counters.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Loop,
                title = "Sin contadores",
                subtitle = "Agregá uno para contar vueltas, puntos o repeticiones del patrón.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(counters, key = { it.id }) { c ->
                    CounterCard(
                        counter = c,
                        onBump = { delta -> vm.bump(c, delta) },
                        onRename = { renaming = c },
                        onSet = { setting = c },
                        onReset = { vm.setCounter(c, 0) },
                        onDelete = { deleting = c },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Contador") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    if (showAdd) {
        TextInputDialog(
            title = "Nuevo contador",
            label = "Nombre (ej: Puntos, Repeticiones)",
            confirmText = "Agregar",
            onConfirm = {
                showAdd = false
                vm.addCounter(it)
            },
            onDismiss = { showAdd = false },
        )
    }
    renaming?.let { c ->
        TextInputDialog(
            title = "Renombrar contador",
            label = "Nombre",
            initial = c.name,
            onConfirm = {
                renaming = null
                vm.renameCounter(c, it)
            },
            onDismiss = { renaming = null },
        )
    }
    setting?.let { c ->
        SetValueDialog(
            initial = c.value,
            onConfirm = {
                setting = null
                vm.setCounter(c, it)
            },
            onDismiss = { setting = null },
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            title = "Eliminar contador",
            text = "Se elimina \"${c.name}\".",
            confirmText = "Eliminar",
            onConfirm = {
                deleting = null
                vm.deleteCounter(c)
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CounterCard(
    counter: Counter,
    onBump: (Int) -> Unit,
    onRename: () -> Unit,
    onSet: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    counter.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Renombrar") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("Fijar valor") },
                            leadingIcon = { Icon(Icons.Default.Pin, null) },
                            onClick = { menuOpen = false; onSet() },
                        )
                        DropdownMenuItem(
                            text = { Text("Poner en 0") },
                            leadingIcon = { Icon(Icons.Default.RestartAlt, null) },
                            onClick = { menuOpen = false; onReset() },
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
            Text(
                counter.value.toString(),
                style = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBump(-1)
                    },
                    enabled = counter.value > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Restar", modifier = Modifier.size(28.dp))
                }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBump(1)
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(64.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Sumar", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SetValueDialog(initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fijar valor") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() }.take(7) },
                label = { Text("Valor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = value != null, onClick = { onConfirm(value ?: 0) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

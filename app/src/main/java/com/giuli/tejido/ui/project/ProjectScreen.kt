package com.giuli.tejido.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.TopAppBarDefaults
import com.giuli.tejido.data.CostItem
import com.giuli.tejido.data.Project
import com.giuli.tejido.data.TimeSession
import com.giuli.tejido.data.subtotal
import com.giuli.tejido.ui.ColorPickerDialog
import com.giuli.tejido.ui.ConfirmDialog
import com.giuli.tejido.ui.theme.ProjectTheme
import com.giuli.tejido.ui.formatDuration
import com.giuli.tejido.ui.formatMoney
import com.giuli.tejido.ui.rememberNow

private data class TabDef(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabDef("Tiempo", Icons.Default.Timer),
    TabDef("Vueltas", Icons.Default.Loop),
    TabDef("Notas", Icons.Default.Collections),
    TabDef("Costos", Icons.Default.AttachMoney),
)

@Composable
fun ProjectScreen(onBack: () -> Unit, vm: ProjectViewModel = viewModel()) {
    val project by vm.project.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val costs by vm.costs.collectAsStateWithLifecycle()

    val p = project
    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    ProjectTheme(colorIndex = p.colorIndex) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ProjectContent(p = p, sessions = sessions, costs = costs, vm = vm, onBack = onBack)
        }
    }
}

@Composable
private fun ProjectContent(
    p: Project,
    sessions: List<TimeSession>,
    costs: List<CostItem>,
    vm: ProjectViewModel,
    onBack: () -> Unit,
) {
    val running = p.runningSince != null
    val now = rememberNow(running)
    val trackedMs = sessions.sumOf { it.durationMs } +
        (p.runningSince?.let { (now - it).coerceAtLeast(0) } ?: 0L)
    val totalCost = costs.sumOf { it.subtotal() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar proyecto") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    showEdit = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Cambiar color") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    showColor = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar proyecto") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SummaryStrip(trackedMs = trackedMs, totalCost = totalCost, running = running)
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, def ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(def.label, maxLines = 1) },
                        icon = { Icon(def.icon, contentDescription = null) },
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (tab) {
                    0 -> TimerTab(
                        runningSince = p.runningSince,
                        trackedMs = trackedMs,
                        sessions = sessions,
                        onStart = { vm.start() },
                        onPause = { vm.pause() },
                        onAddManual = { vm.addManual(it) },
                        onDeleteSession = { vm.deleteSession(it) },
                        onReset = { vm.resetTime() },
                    )
                    1 -> CounterTab(vm)
                    2 -> NotesTab(vm)
                    else -> CostsTab(vm = vm, costs = costs, trackedMs = trackedMs, totalCost = totalCost)
                }
            }
        }
    }

    if (showEdit) {
        EditProjectDialog(
            initialName = p.name,
            initialDescription = p.description,
            onDismiss = { showEdit = false },
            onSave = { name, desc ->
                showEdit = false
                vm.rename(name, desc)
            },
        )
    }
    if (showColor) {
        ColorPickerDialog(
            initial = p.colorIndex,
            onConfirm = {
                showColor = false
                vm.setColor(it)
            },
            onDismiss = { showColor = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Eliminar proyecto",
            text = "Se borran \"${p.name}\" y todo su tiempo, contadores, notas, fotos y costos. No se puede deshacer.",
            confirmText = "Eliminar",
            onConfirm = {
                confirmDelete = false
                vm.deleteProject(onDone = onBack)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SummaryStrip(trackedMs: Long, totalCost: Double, running: Boolean) {
    val hours = trackedMs / 3_600_000.0
    val perHour = if (trackedMs >= 60_000) formatMoney(totalCost / hours) else "—"
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Stat("Tiempo", formatDuration(trackedMs) + if (running) " ●" else "", Modifier.weight(1f))
            Stat("Costo total", formatMoney(totalCost), Modifier.weight(1f))
            Stat("Costo / hora", perHour, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EditProjectDialog(
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var desc by rememberSaveable { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar proyecto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), desc.trim()) }) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

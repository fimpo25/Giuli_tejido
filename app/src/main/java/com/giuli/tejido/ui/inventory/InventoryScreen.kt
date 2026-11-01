package com.giuli.tejido.ui.inventory

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giuli.tejido.data.AppDatabase
import com.giuli.tejido.data.InventoryRow
import com.giuli.tejido.data.PriceHistory
import com.giuli.tejido.ui.ConfirmDialog
import com.giuli.tejido.ui.EmptyState
import com.giuli.tejido.ui.formatDate
import com.giuli.tejido.ui.formatDateTime
import com.giuli.tejido.ui.formatMoney
import com.giuli.tejido.ui.numberForInput
import com.giuli.tejido.ui.parseNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()

    val items: StateFlow<List<InventoryRow>> = dao.observeInventory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun history(itemId: Long): Flow<List<PriceHistory>> = dao.observePriceHistory(itemId)

    fun create(name: String, unit: String, price: Double) = viewModelScope.launch {
        dao.createInventoryItem(name, unit, price)
    }

    fun update(id: Long, name: String, unit: String, price: Double, onUpdated: (Int) -> Unit) {
        viewModelScope.launch { onUpdated(dao.updateInventoryItem(id, name, unit, price)) }
    }

    fun delete(id: Long) = viewModelScope.launch { dao.deleteInventoryItem(id) }
}

@Composable
fun InventoryScreen(onBack: () -> Unit, vm: InventoryViewModel = viewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableLongStateOf(0L) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filtered = items.filter { query.isBlank() || it.item.name.contains(query.trim(), ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingId = 0L
                    editorOpen = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Artículo") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Inventory2,
                    title = "El inventario está vacío",
                    subtitle = "Cargá tus materiales con su precio. Después los elegís dentro de cada proyecto " +
                        "y, si cambia el precio acá, se actualiza en todos los proyectos.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "search") {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text("Buscar artículo") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(filtered, key = { it.item.id }) { row ->
                        ListItem(
                            headlineContent = { Text(row.item.name) },
                            supportingContent = {
                                val usage = when (row.projectCount) {
                                    0 -> "Sin usar"
                                    1 -> "En 1 proyecto"
                                    else -> "En ${row.projectCount} proyectos"
                                }
                                Text("$usage · actualizado ${formatDate(row.item.updatedAt)}")
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatMoney(row.item.currentPrice),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (row.item.unit.isNotBlank()) {
                                        Text(
                                            "por ${row.item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.clickable {
                                editingId = row.item.id
                                editorOpen = true
                            },
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (editorOpen) {
        val existing = items.firstOrNull { it.item.id == editingId }
        key(editingId) {
            InventoryEditorDialog(
                existing = existing,
                vm = vm,
                onDismiss = { editorOpen = false },
                onSave = { name, unit, price ->
                    editorOpen = false
                    if (existing == null) {
                        vm.create(name, unit, price)
                    } else {
                        vm.update(existing.item.id, name, unit, price) { updated ->
                            if (updated > 0) {
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (updated == 1) "Precio actualizado en 1 proyecto"
                                        else "Precio actualizado en $updated proyectos",
                                    )
                                }
                            }
                        }
                    }
                },
                onDelete = { editorOpen = false; existing?.let { vm.delete(it.item.id) } },
            )
        }
    }
}

@Composable
private fun InventoryEditorDialog(
    existing: InventoryRow?,
    vm: InventoryViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, Double) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.item?.name ?: "") }
    var unit by rememberSaveable { mutableStateOf(existing?.item?.unit ?: "") }
    var price by rememberSaveable { mutableStateOf(existing?.item?.currentPrice?.let { numberForInput(it) } ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val priceValue = parseNumber(price)
    val valid = name.isNotBlank() && priceValue != null && priceValue >= 0
    val history: List<PriceHistory> = if (existing != null) {
        val flow = remember(existing.item.id) { vm.history(existing.item.id) }
        flow.collectAsStateWithLifecycle(emptyList()).value
    } else {
        emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nuevo artículo" else "Editar artículo") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej: Ovillo merino 100 g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Precio") },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        isError = price.isNotBlank() && priceValue == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.3f),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unidad") },
                        placeholder = { Text("ovillo") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (existing != null && existing.projectCount > 0) {
                    Text(
                        "Se usa en ${existing.projectCount} proyecto(s). Si cambiás el precio, se actualiza en todos " +
                            "y queda registrado en el histórico de cada uno.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (history.isNotEmpty()) {
                    Text(
                        "Histórico de precios",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.heightIn(max = 260.dp)) {
                        history.forEachIndexed { i, h ->
                            val previous = history.getOrNull(i + 1)
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    formatDateTime(h.changedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    formatMoney(h.price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (previous != null && previous.price != 0.0) {
                                    val pct = (h.price - previous.price) / previous.price * 100
                                    Text(
                                        (if (pct > 0) "  +" else "  ") +
                                            String.format(java.util.Locale("es", "AR"), "%.1f %%", pct),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (pct > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(name.trim(), unit.trim(), priceValue ?: 0.0) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Eliminar del inventario",
            text = "Se elimina \"${existing.item.name}\" y su histórico. En los proyectos que lo usan queda con el " +
                "último precio, pero ya no se actualiza.",
            confirmText = "Eliminar",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

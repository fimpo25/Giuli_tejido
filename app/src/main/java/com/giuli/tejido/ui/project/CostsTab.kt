package com.giuli.tejido.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giuli.tejido.data.CostChange
import com.giuli.tejido.data.CostItem
import com.giuli.tejido.data.InventoryRow
import com.giuli.tejido.data.subtotal
import com.giuli.tejido.ui.formatDateTime
import com.giuli.tejido.ui.formatHoursMinutes
import com.giuli.tejido.ui.formatMoney
import com.giuli.tejido.ui.formatQty
import com.giuli.tejido.ui.numberForInput
import com.giuli.tejido.ui.parseNumber
import java.util.Locale

@Composable
fun CostsTab(vm: ProjectViewModel, costs: List<CostItem>, trackedMs: Long, totalCost: Double) {
    val inventory by vm.inventory.collectAsStateWithLifecycle()
    val changes by vm.costChanges.collectAsStateWithLifecycle()
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableLongStateOf(0L) }

    val hours = trackedMs / 3_600_000.0
    val perHour = if (trackedMs >= 60_000) totalCost / hours else null

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "summary") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SummaryRow("Costo total", formatMoney(totalCost), big = true)
                        SummaryRow("Tiempo trabajado", formatHoursMinutes(trackedMs))
                        SummaryRow(
                            "Costo por hora",
                            perHour?.let { formatMoney(it) } ?: "— (registrá tiempo primero)",
                        )
                    }
                }
            }
            if (costs.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Agregá los materiales: lana, agujas, botones, etc. Podés elegirlos del inventario.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }
            items(costs, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.article) },
                    supportingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.inventoryItemId != null) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = "Del inventario",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text("${formatQty(item.quantity)} × ${formatMoney(item.unitPrice)}")
                        }
                    },
                    trailingContent = {
                        Text(
                            formatMoney(item.subtotal()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable {
                        editingId = item.id
                        editorOpen = true
                    },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }

            if (changes.isNotEmpty()) {
                item(key = "history-header") {
                    Row(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Histórico de precios",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                items(changes, key = { "chg-${it.id}" }) { change -> CostChangeRow(change) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = {
                editingId = 0L
                editorOpen = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Artículo") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    if (editorOpen) {
        val existing = costs.firstOrNull { it.id == editingId }
        key(editingId) {
            CostEditorDialog(
                existing = existing,
                inventory = inventory,
                onDismiss = { editorOpen = false },
                onSave = { article, unitPrice, qty, linkId, addToInv, invUnit ->
                    editorOpen = false
                    vm.saveCost(existing, article, unitPrice, qty, linkId, addToInv, invUnit)
                },
                onDelete = if (existing == null) null else {
                    {
                        editorOpen = false
                        vm.deleteCost(existing)
                        Unit
                    }
                },
            )
        }
    }
}

@Composable
private fun CostChangeRow(change: CostChange) {
    val pct = if (change.oldUnitPrice != 0.0) {
        (change.newUnitPrice - change.oldUnitPrice) / change.oldUnitPrice * 100
    } else {
        null
    }
    val up = change.newUnitPrice > change.oldUnitPrice
    ListItem(
        headlineContent = {
            Text(
                "${change.article}: ${formatMoney(change.oldUnitPrice)} → ${formatMoney(change.newUnitPrice)}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                formatDateTime(change.changedAt) +
                    (if (change.source == "inventario") " · desde inventario" else " · manual") +
                    "\nTotal del proyecto: ${formatMoney(change.totalBefore)} → ${formatMoney(change.totalAfter)}",
            )
        },
        trailingContent = {
            if (pct != null) {
                Text(
                    (if (up) "+" else "") + String.format(Locale("es", "AR"), "%.1f %%", pct),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (up) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun SummaryRow(label: String, value: String, big: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun CostEditorDialog(
    existing: CostItem?,
    inventory: List<InventoryRow>,
    onDismiss: () -> Unit,
    onSave: (article: String, unitPrice: Double, qty: Double, linkId: Long?, addToInventory: Boolean, unit: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var linkedId by rememberSaveable { mutableStateOf(existing?.inventoryItemId) }
    var article by rememberSaveable { mutableStateOf(existing?.article ?: "") }
    var unit by rememberSaveable { mutableStateOf(existing?.let { numberForInput(it.unitPrice) } ?: "") }
    var qty by rememberSaveable { mutableStateOf(existing?.let { numberForInput(it.quantity) } ?: "1") }
    var addToInventory by rememberSaveable { mutableStateOf(false) }
    var invUnit by rememberSaveable { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }

    val linkedItem = linkedId?.let { id -> inventory.firstOrNull { it.item.id == id }?.item }
    val unitValue = linkedItem?.currentPrice ?: parseNumber(unit)
    val qtyValue = parseNumber(qty)
    val valid = article.isNotBlank() && unitValue != null && unitValue >= 0 && qtyValue != null && qtyValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nuevo artículo" else "Editar artículo") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (linkedItem != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(linkedItem.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    formatMoney(linkedItem.currentPrice) +
                                        (if (linkedItem.unit.isNotBlank()) " / ${linkedItem.unit}" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "El precio se actualiza solo cuando cambia en el inventario.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { picking = true }) { Text("Cambiar") }
                        TextButton(onClick = {
                            unit = numberForInput(linkedItem.currentPrice)
                            article = linkedItem.name
                            linkedId = null
                        }) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Desvincular")
                        }
                    }
                } else {
                    FilledTonalButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Elegir del inventario")
                    }
                    OutlinedTextField(
                        value = article,
                        onValueChange = { article = it },
                        label = { Text("Artículo") },
                        placeholder = { Text("Ej: Ovillo merino 100 g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (linkedItem == null) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Precio unitario") },
                            prefix = { Text("$ ") },
                            singleLine = true,
                            isError = unit.isNotBlank() && parseNumber(unit) == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        isError = qty.isNotBlank() && qtyValue == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (linkedItem == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { addToInventory = !addToInventory },
                    ) {
                        Checkbox(checked = addToInventory, onCheckedChange = { addToInventory = it })
                        Text("Guardar también en el inventario")
                    }
                    if (addToInventory) {
                        OutlinedTextField(
                            value = invUnit,
                            onValueChange = { invUnit = it },
                            label = { Text("Unidad (opcional)") },
                            placeholder = { Text("ovillo, metro, unidad…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Text(
                    "Subtotal: " + if (unitValue != null && qtyValue != null) formatMoney(unitValue * qtyValue) else "—",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        linkedItem?.name ?: article.trim(),
                        unitValue ?: 0.0,
                        qtyValue ?: 0.0,
                        linkedItem?.id ?: linkedId,
                        linkedId == null && addToInventory,
                        invUnit.trim(),
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )

    if (picking) {
        InventoryPickerDialog(
            inventory = inventory,
            onPick = { row ->
                picking = false
                linkedId = row.item.id
                article = row.item.name
                unit = numberForInput(row.item.currentPrice)
                addToInventory = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun InventoryPickerDialog(
    inventory: List<InventoryRow>,
    onPick: (InventoryRow) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = inventory.filter { query.isBlank() || it.item.name.contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir del inventario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (inventory.isEmpty()) {
                    Text(
                        "El inventario está vacío. Agregá artículos desde la pantalla Inventario, " +
                            "o marcá \"Guardar también en el inventario\" al cargar un artículo.",
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Buscar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.item.id }) { row ->
                            ListItem(
                                headlineContent = { Text(row.item.name) },
                                supportingContent = {
                                    Text(
                                        formatMoney(row.item.currentPrice) +
                                            (if (row.item.unit.isNotBlank()) " / ${row.item.unit}" else ""),
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                                modifier = Modifier.clickable { onPick(row) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

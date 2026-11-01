package com.giuli.tejido.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.giuli.tejido.data.CostItem
import com.giuli.tejido.data.subtotal
import com.giuli.tejido.ui.formatHoursMinutes
import com.giuli.tejido.ui.formatMoney
import com.giuli.tejido.ui.formatQty
import com.giuli.tejido.ui.numberForInput
import com.giuli.tejido.ui.parseNumber

@Composable
fun CostsTab(vm: ProjectViewModel, costs: List<CostItem>, trackedMs: Long, totalCost: Double) {
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
                        "Agregá los materiales: lana, agujas, botones, etc.",
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
                        Text("${formatQty(item.quantity)} × ${formatMoney(item.unitPrice)}")
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
        androidx.compose.runtime.key(editingId) {
            CostEditorDialog(
                existing = existing,
                onDismiss = { editorOpen = false },
                onSave = { article, unit, qty ->
                    editorOpen = false
                    vm.saveCost(existing, article, unit, qty)
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
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var article by rememberSaveable { mutableStateOf(existing?.article ?: "") }
    var unit by rememberSaveable { mutableStateOf(existing?.let { numberForInput(it.unitPrice) } ?: "") }
    var qty by rememberSaveable { mutableStateOf(existing?.let { numberForInput(it.quantity) } ?: "1") }

    val unitValue = parseNumber(unit)
    val qtyValue = parseNumber(qty)
    val valid = article.isNotBlank() && unitValue != null && unitValue >= 0 && qtyValue != null && qtyValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nuevo artículo" else "Editar artículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = article,
                    onValueChange = { article = it },
                    label = { Text("Artículo") },
                    placeholder = { Text("Ej: Ovillo merino 100 g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Precio unitario") },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        isError = unit.isNotBlank() && unitValue == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.4f),
                    )
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
                Text(
                    "Subtotal: " + if (unitValue != null && qtyValue != null) formatMoney(unitValue * qtyValue) else "—",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(article.trim(), unitValue ?: 0.0, qtyValue ?: 0.0) },
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
}

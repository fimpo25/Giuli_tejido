package com.giuli.tejido.ui.project

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.giuli.tejido.data.Note
import com.giuli.tejido.data.PhotoStore
import com.giuli.tejido.ui.ConfirmDialog
import com.giuli.tejido.ui.EmptyState
import com.giuli.tejido.ui.PhotoViewer
import com.giuli.tejido.ui.formatDateTime
import java.io.File

@Composable
fun NotesTab(vm: ProjectViewModel) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableLongStateOf(0L) }
    var viewing by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Collections,
                title = "Todavía no hay notas",
                subtitle = "Sumá fotos del avance y comentarios para seguir el proyecto.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onPhotoClick = { viewing = it },
                        onEdit = {
                            editingId = note.id
                            editorOpen = true
                        },
                        onDelete = { deleting = note },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = {
                editingId = 0L
                editorOpen = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nota / foto") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    if (editorOpen) {
        val existing = notes.firstOrNull { it.id == editingId }
        key(editingId) {
            NoteEditorDialog(existing = existing, vm = vm, onDismiss = { editorOpen = false })
        }
    }
    viewing?.let { path -> PhotoViewer(path = path, onDismiss = { viewing = null }) }
    deleting?.let { note ->
        ConfirmDialog(
            title = "Eliminar nota",
            text = if (note.photoPath != null) "Se borran la nota y su foto." else "Se borra la nota.",
            confirmText = "Eliminar",
            onConfirm = {
                deleting = null
                vm.deleteNote(note)
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onPhotoClick: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val photo = note.photoPath
        if (photo != null) {
            AsyncImage(
                model = File(photo),
                contentDescription = "Foto de la nota",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clickable { onPhotoClick(photo) },
            )
        }
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
            ) {
                if (note.text.isNotBlank()) {
                    Text(note.text, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    formatDateTime(note.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(existing: Note?, vm: ProjectViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val original = existing?.photoPath
    var text by rememberSaveable { mutableStateOf(existing?.text ?: "") }
    var photo by rememberSaveable { mutableStateOf(original) }
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Reemplaza la foto del borrador y borra la anterior si era nueva (no guardada).
    fun setPhoto(newPath: String?) {
        val prev = photo
        if (prev != null && prev != original && prev != newPath) PhotoStore.delete(prev)
        photo = newPath
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            busy = true
            vm.importPhoto(uri) { path ->
                busy = false
                if (path != null) setPhoto(path)
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCamera
        pendingCamera = null
        if (path != null) {
            if (ok && File(path).length() > 0) setPhoto(path) else PhotoStore.delete(path)
        }
    }

    val cancel: () -> Unit = {
        val p = photo
        if (p != null && p != original) PhotoStore.delete(p)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(if (existing == null) "Nueva nota" else "Editar nota") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val current = photo
                if (current != null) {
                    Box {
                        AsyncImage(
                            model = File(current),
                            contentDescription = "Foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                        IconButton(
                            onClick = { setPhoto(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar foto", tint = Color.White)
                        }
                    }
                }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Copiando foto…")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galería")
                    }
                    OutlinedButton(
                        onClick = {
                            val file = PhotoStore.newFile(context)
                            pendingCamera = file.absolutePath
                            camera.launch(PhotoStore.uriFor(context, file))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cámara")
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Comentario") },
                    placeholder = { Text("Ej: Terminé el canesú, cambié a aguja 4") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = (text.isNotBlank() || photo != null) && !busy,
                onClick = {
                    vm.saveNote(existing, text.trim(), photo)
                    if (original != null && original != photo) PhotoStore.delete(original)
                    onDismiss()
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = cancel) { Text("Cancelar") } },
    )
}

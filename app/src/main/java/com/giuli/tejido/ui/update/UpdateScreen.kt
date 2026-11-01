package com.giuli.tejido.ui.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giuli.tejido.BuildConfig
import com.giuli.tejido.update.InstallResult
import com.giuli.tejido.update.Release
import com.giuli.tejido.update.UpdateManager
import com.giuli.tejido.update.UpdateState
import java.util.Locale

@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by UpdateManager.state.collectAsStateWithLifecycle()
    var installMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (state is UpdateState.Idle || state is UpdateState.Error || state is UpdateState.UpToDate) {
            UpdateManager.check()
        }
    }

    fun install(state: UpdateState.Downloaded) {
        installMessage = when (UpdateManager.install(context, state.file)) {
            InstallResult.STARTED -> null
            InstallResult.NEEDS_PERMISSION ->
                "Activá \"Permitir desde esta fuente\" para Giuli Tejido, volvé y tocá Instalar."
            InstallResult.FAILED -> "No se pudo abrir el instalador."
        }
    }

    // Al terminar la descarga, lanzar el instalador automáticamente una vez.
    val downloaded = state as? UpdateState.Downloaded
    LaunchedEffect(downloaded?.file) {
        if (downloaded != null) install(downloaded)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Actualizaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Versión instalada", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${BuildConfig.VERSION_NAME}  (compilación ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            when (val s = state) {
                UpdateState.Idle, UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Buscando nuevas versiones…")
                }

                is UpdateState.UpToDate -> StatusRow(
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                    text = if (s.latestTag == null) "Todavía no hay versiones publicadas en GitHub."
                    else "Tenés la última versión (${s.latestTag}).",
                )

                is UpdateState.Available -> ReleaseCard(s.release) {
                    Button(
                        onClick = { UpdateManager.download(context, s.release) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Descargar e instalar")
                    }
                }

                is UpdateState.Downloading -> ReleaseCard(s.release) {
                    Text("Descargando… ${(s.progress * 100).toInt()} %")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                }

                is UpdateState.Downloaded -> ReleaseCard(s.release) {
                    Button(onClick = { install(s) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.InstallMobile, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Instalar")
                    }
                    installMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                is UpdateState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusRow(
                        icon = { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
                        text = s.message,
                    )
                    val release = s.release
                    if (release != null) {
                        Button(onClick = { UpdateManager.download(context, release) }) {
                            Text("Reintentar descarga")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { UpdateManager.check() },
                enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Buscar actualizaciones")
            }

            Text(
                "Las versiones nuevas se publican automáticamente en GitHub cada vez que se actualiza el repo. " +
                    "Al instalar encima, los proyectos y fotos se conservan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.RELEASES_PAGE)))
                }
            }) { Text("Ver versiones en GitHub") }
        }
    }
}

@Composable
private fun StatusRow(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReleaseCard(release: Release, actions: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Nueva versión disponible", style = MaterialTheme.typography.labelLarge)
            Text(release.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            val meta = buildList {
                if (release.publishedAt.length >= 10) {
                    val d = release.publishedAt.substring(0, 10).split("-")
                    if (d.size == 3) add("${d[2]}/${d[1]}/${d[0]}")
                }
                if (release.sizeBytes > 0) {
                    add(String.format(Locale.US, "%.1f MB", release.sizeBytes / 1_048_576.0))
                }
            }.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall)
            }
            if (release.notes.isNotBlank()) {
                Text(
                    "Novedades",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(release.notes, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            actions()
        }
    }
}

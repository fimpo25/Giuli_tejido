package com.giuli.tejido.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.giuli.tejido.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class Release(
    val tag: String,
    val versionCode: Int,
    val name: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val publishedAt: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val latestTag: String?) : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Downloaded(val release: Release, val file: File) : UpdateState
    data class Error(val message: String, val release: Release? = null) : UpdateState
}

enum class InstallResult { STARTED, NEEDS_PERMISSION, FAILED }

/**
 * Consulta los Releases del repo de GitHub. Cada compilación en GitHub Actions publica
 * un Release con tag v1.0.<n>, donde <n> es el versionCode del APK.
 */
object UpdateManager {
    const val REPO = "fimpo25/Giuli_tejido"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"
    private const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var autoChecked = false

    fun autoCheck() {
        if (autoChecked) return
        autoChecked = true
        check()
    }

    fun check() {
        val current = _state.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return
        scope.launch {
            _state.value = UpdateState.Checking
            _state.value = try {
                val latest = fetchLatest()
                when {
                    latest == null -> UpdateState.UpToDate(null)
                    latest.versionCode > BuildConfig.VERSION_CODE -> UpdateState.Available(latest)
                    else -> UpdateState.UpToDate(latest.tag)
                }
            } catch (e: Exception) {
                UpdateState.Error("No se pudo consultar GitHub (${e.message ?: e.javaClass.simpleName}).")
            }
        }
    }

    private fun fetchLatest(): Release? {
        val conn = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "GiuliTejido-Android")
        }
        try {
            val code = conn.responseCode
            if (code == 404) return null // todavía no hay releases
            if (code !in 200..299) throw IOException("HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.getString("browser_download_url")
                    size = a.optLong("size", 0L)
                    break
                }
            }
            if (apkUrl == null) throw IOException("el release $tag no tiene APK")
            val versionCode = Regex("(\\d+)\\s*$").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return Release(
                tag = tag,
                versionCode = versionCode,
                name = if (json.isNull("name")) tag else json.getString("name").ifBlank { tag },
                notes = if (json.isNull("body")) "" else json.getString("body").trim(),
                apkUrl = apkUrl,
                sizeBytes = size,
                publishedAt = if (json.isNull("published_at")) "" else json.getString("published_at"),
            )
        } finally {
            conn.disconnect()
        }
    }

    fun download(context: Context, release: Release) {
        if (_state.value is UpdateState.Downloading) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                _state.value = UpdateState.Downloading(release, 0f)
                val dir = File(appContext.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val file = File(dir, "giuli-tejido-${release.tag}.apk")
                val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "GiuliTejido-Android")
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) throw IOException("HTTP $code")
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
                    conn.inputStream.use { input ->
                        file.outputStream().use { out ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            var lastPct = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                out.write(buffer, 0, read)
                                done += read
                                if (total > 0) {
                                    val pct = (done * 100 / total).toInt().coerceIn(0, 100)
                                    if (pct != lastPct) {
                                        lastPct = pct
                                        _state.value = UpdateState.Downloading(release, pct / 100f)
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                _state.value = UpdateState.Downloaded(release, file)
            } catch (e: Exception) {
                _state.value = UpdateState.Error(
                    "Error al descargar (${e.message ?: e.javaClass.simpleName}).",
                    release,
                )
            }
        }
    }

    fun install(context: Context, file: File): InstallResult {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(intent)
                InstallResult.NEEDS_PERMISSION
            } catch (e: ActivityNotFoundException) {
                InstallResult.FAILED
            }
        }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            InstallResult.STARTED
        } catch (e: Exception) {
            InstallResult.FAILED
        }
    }
}

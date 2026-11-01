package com.giuli.tejido.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Las fotos se copian dentro de la app para que no se pierdan si se borran de la galería. */
object PhotoStore {
    private fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    fun newFile(context: Context): File =
        File(dir(context), "IMG_${System.currentTimeMillis()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun importFrom(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val file = newFile(context)
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
        input.use { inp -> file.outputStream().use { out -> inp.copyTo(out) } }
        if (file.length() > 0) file.absolutePath else {
            file.delete()
            null
        }
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}

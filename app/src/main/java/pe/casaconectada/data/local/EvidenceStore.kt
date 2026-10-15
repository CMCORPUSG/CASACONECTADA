package pe.casaconectada.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Las imágenes se copian al almacenamiento privado; no se guarda un URI temporal. */
@Singleton
class EvidenceStore @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun save(uri: Uri): String = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "evidencias").apply { mkdirs() }
        val file = File(directory, UUID.randomUUID().toString() + ".jpg")
        try {
            context.contentResolver.openInputStream(uri).use { source ->
                requireNotNull(source) { "No se puede abrir la fotografía" }
                file.outputStream().use { target -> source.copyTo(target) }
            }
            file.absolutePath
        } catch (ex: Exception) {
            file.delete()
            throw ex
        }
    }
}

package com.giuli.tejido.ui.project

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.giuli.tejido.data.AppDatabase
import com.giuli.tejido.data.CostItem
import com.giuli.tejido.data.Counter
import com.giuli.tejido.data.Note
import com.giuli.tejido.data.PhotoStore
import com.giuli.tejido.data.Project
import com.giuli.tejido.data.TimeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectViewModel(
    private val app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    val projectId: Long = checkNotNull(savedStateHandle.get<Long>("projectId"))
    private val dao = AppDatabase.get(app).dao()

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val project: StateFlow<Project?> = dao.observeProject(projectId).state(null)
    val sessions: StateFlow<List<TimeSession>> = dao.observeSessions(projectId).state(emptyList())
    val counters: StateFlow<List<Counter>> = dao.observeCounters(projectId).state(emptyList())
    val notes: StateFlow<List<Note>> = dao.observeNotes(projectId).state(emptyList())
    val costs: StateFlow<List<CostItem>> = dao.observeCosts(projectId).state(emptyList())

    // ---------- Proyecto ----------

    fun rename(name: String, description: String) = viewModelScope.launch {
        dao.renameProject(projectId, name, description)
    }

    fun deleteProject(onDone: () -> Unit) = viewModelScope.launch {
        val photos = dao.photoPathsOf(projectId)
        dao.deleteProject(projectId)
        photos.forEach { PhotoStore.delete(it) }
        onDone()
    }

    // ---------- Cronómetro ----------

    fun start() = viewModelScope.launch { dao.startTimer(projectId, System.currentTimeMillis()) }

    fun pause() = viewModelScope.launch { dao.pauseTimer(projectId, System.currentTimeMillis()) }

    /** [deltaMs] positivo suma tiempo, negativo resta. */
    fun addManual(deltaMs: Long) = viewModelScope.launch {
        if (deltaMs != 0L) {
            dao.insertSession(
                TimeSession(
                    projectId = projectId,
                    startedAt = System.currentTimeMillis(),
                    durationMs = deltaMs,
                    manual = true,
                ),
            )
        }
    }

    fun deleteSession(session: TimeSession) = viewModelScope.launch { dao.deleteSession(session.id) }

    fun resetTime() = viewModelScope.launch { dao.resetTimer(projectId) }

    // ---------- Contadores ----------

    fun addCounter(name: String) = viewModelScope.launch {
        dao.insertCounter(Counter(projectId = projectId, name = name))
    }

    fun bump(counter: Counter, delta: Int) = viewModelScope.launch { dao.bumpCounter(counter.id, delta) }

    fun setCounter(counter: Counter, value: Int) = viewModelScope.launch {
        dao.setCounterValue(counter.id, value.coerceAtLeast(0))
    }

    fun renameCounter(counter: Counter, name: String) = viewModelScope.launch {
        dao.renameCounter(counter.id, name)
    }

    fun deleteCounter(counter: Counter) = viewModelScope.launch { dao.deleteCounter(counter.id) }

    // ---------- Notas ----------

    fun importPhoto(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = runCatching { PhotoStore.importFrom(app, uri) }.getOrNull()
            onResult(path)
        }
    }

    fun saveNote(existing: Note?, text: String, photoPath: String?) = viewModelScope.launch {
        if (existing == null) {
            dao.insertNote(Note(projectId = projectId, text = text, photoPath = photoPath))
        } else {
            dao.updateNote(existing.copy(text = text, photoPath = photoPath))
        }
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        dao.deleteNote(note)
        PhotoStore.delete(note.photoPath)
    }

    // ---------- Costos ----------

    fun saveCost(existing: CostItem?, article: String, unitPrice: Double, quantity: Double) =
        viewModelScope.launch {
            if (existing == null) {
                dao.insertCost(
                    CostItem(projectId = projectId, article = article, unitPrice = unitPrice, quantity = quantity),
                )
            } else {
                dao.updateCost(existing.copy(article = article, unitPrice = unitPrice, quantity = quantity))
            }
        }

    fun deleteCost(item: CostItem) = viewModelScope.launch { dao.deleteCost(item) }
}

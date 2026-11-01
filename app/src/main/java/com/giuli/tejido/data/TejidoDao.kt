package com.giuli.tejido.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TejidoDao {

    // ---------- Proyectos ----------

    @Query(
        """
        SELECT p.*,
            (SELECT COALESCE(SUM(s.durationMs), 0) FROM time_sessions s WHERE s.projectId = p.id) AS trackedMs,
            (SELECT COALESCE(SUM(c.unitPrice * c.quantity), 0.0) FROM cost_items c WHERE c.projectId = p.id) AS totalCost,
            (SELECT n.photoPath FROM notes n WHERE n.projectId = p.id AND n.photoPath IS NOT NULL
                ORDER BY n.createdAt DESC LIMIT 1) AS coverPhoto
        FROM projects p
        ORDER BY (p.runningSince IS NULL), p.createdAt DESC
        """
    )
    abstract fun observeSummaries(): Flow<List<ProjectSummary>>

    @Query("SELECT * FROM projects WHERE id = :id")
    abstract fun observeProject(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    abstract suspend fun getProject(id: Long): Project?

    @Insert
    abstract suspend fun insertProject(project: Project): Long

    @Query("UPDATE projects SET name = :name, description = :description WHERE id = :id")
    abstract suspend fun renameProject(id: Long, name: String, description: String)

    @Query("UPDATE projects SET runningSince = :since WHERE id = :id")
    abstract suspend fun setRunningSince(id: Long, since: Long?)

    @Transaction
    open suspend fun createProject(name: String, description: String): Long {
        val id = insertProject(Project(name = name, description = description))
        insertCounter(Counter(projectId = id, name = "Vueltas"))
        return id
    }

    @Query("SELECT photoPath FROM notes WHERE projectId = :projectId AND photoPath IS NOT NULL")
    abstract suspend fun photoPathsOf(projectId: Long): List<String>

    @Query("DELETE FROM projects WHERE id = :id")
    abstract suspend fun deleteProjectRow(id: Long)

    @Query("DELETE FROM time_sessions WHERE projectId = :projectId")
    abstract suspend fun deleteSessionsOf(projectId: Long)

    @Query("DELETE FROM counters WHERE projectId = :projectId")
    abstract suspend fun deleteCountersOf(projectId: Long)

    @Query("DELETE FROM notes WHERE projectId = :projectId")
    abstract suspend fun deleteNotesOf(projectId: Long)

    @Query("DELETE FROM cost_items WHERE projectId = :projectId")
    abstract suspend fun deleteCostsOf(projectId: Long)

    @Transaction
    open suspend fun deleteProject(id: Long) {
        deleteSessionsOf(id)
        deleteCountersOf(id)
        deleteNotesOf(id)
        deleteCostsOf(id)
        deleteProjectRow(id)
    }

    // ---------- Cronómetro ----------

    @Query("SELECT * FROM time_sessions WHERE projectId = :projectId ORDER BY startedAt DESC, id DESC")
    abstract fun observeSessions(projectId: Long): Flow<List<TimeSession>>

    @Insert
    abstract suspend fun insertSession(session: TimeSession): Long

    @Query("DELETE FROM time_sessions WHERE id = :id")
    abstract suspend fun deleteSession(id: Long)

    @Transaction
    open suspend fun startTimer(projectId: Long, now: Long) {
        val p = getProject(projectId) ?: return
        if (p.runningSince == null) setRunningSince(projectId, now)
    }

    @Transaction
    open suspend fun pauseTimer(projectId: Long, now: Long) {
        val p = getProject(projectId) ?: return
        val since = p.runningSince ?: return
        val duration = (now - since).coerceAtLeast(0)
        if (duration >= 1000) {
            insertSession(TimeSession(projectId = projectId, startedAt = since, durationMs = duration))
        }
        setRunningSince(projectId, null)
    }

    @Transaction
    open suspend fun resetTimer(projectId: Long) {
        deleteSessionsOf(projectId)
        setRunningSince(projectId, null)
    }

    // ---------- Contadores ----------

    @Query("SELECT * FROM counters WHERE projectId = :projectId ORDER BY createdAt, id")
    abstract fun observeCounters(projectId: Long): Flow<List<Counter>>

    @Insert
    abstract suspend fun insertCounter(counter: Counter): Long

    @Query("UPDATE counters SET value = MAX(0, value + :delta) WHERE id = :id")
    abstract suspend fun bumpCounter(id: Long, delta: Int)

    @Query("UPDATE counters SET value = :value WHERE id = :id")
    abstract suspend fun setCounterValue(id: Long, value: Int)

    @Query("UPDATE counters SET name = :name WHERE id = :id")
    abstract suspend fun renameCounter(id: Long, name: String)

    @Query("DELETE FROM counters WHERE id = :id")
    abstract suspend fun deleteCounter(id: Long)

    // ---------- Notas ----------

    @Query("SELECT * FROM notes WHERE projectId = :projectId ORDER BY createdAt DESC, id DESC")
    abstract fun observeNotes(projectId: Long): Flow<List<Note>>

    @Insert
    abstract suspend fun insertNote(note: Note): Long

    @Update
    abstract suspend fun updateNote(note: Note)

    @Delete
    abstract suspend fun deleteNote(note: Note)

    // ---------- Costos ----------

    @Query("SELECT * FROM cost_items WHERE projectId = :projectId ORDER BY createdAt, id")
    abstract fun observeCosts(projectId: Long): Flow<List<CostItem>>

    @Insert
    abstract suspend fun insertCost(item: CostItem): Long

    @Update
    abstract suspend fun updateCost(item: CostItem)

    @Delete
    abstract suspend fun deleteCost(item: CostItem)
}

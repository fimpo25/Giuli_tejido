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

    @Query("UPDATE projects SET colorIndex = :colorIndex WHERE id = :id")
    abstract suspend fun setProjectColor(id: Long, colorIndex: Int)

    @Transaction
    open suspend fun createProject(name: String, description: String, colorIndex: Int = 0): Long {
        val id = insertProject(Project(name = name, description = description, colorIndex = colorIndex))
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

    @Query("DELETE FROM cost_changes WHERE projectId = :projectId")
    abstract suspend fun deleteCostChangesOf(projectId: Long)

    @Transaction
    open suspend fun deleteProject(id: Long) {
        deleteSessionsOf(id)
        deleteCountersOf(id)
        deleteNotesOf(id)
        deleteCostsOf(id)
        deleteCostChangesOf(id)
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

    @Query("SELECT * FROM cost_items WHERE id = :id")
    abstract suspend fun getCost(id: Long): CostItem?

    @Query("SELECT COALESCE(SUM(unitPrice * quantity), 0.0) FROM cost_items WHERE projectId = :projectId")
    abstract suspend fun projectTotal(projectId: Long): Double

    @Insert
    abstract suspend fun insertCostChange(change: CostChange): Long

    @Query("SELECT * FROM cost_changes WHERE projectId = :projectId ORDER BY changedAt DESC, id DESC")
    abstract fun observeCostChanges(projectId: Long): Flow<List<CostChange>>

    /**
     * Guarda un artículo de costo del proyecto. Si cambia el precio unitario de uno existente,
     * deja registrado el cambio en el histórico del proyecto.
     */
    @Transaction
    open suspend fun saveCostItem(
        projectId: Long,
        existingId: Long?,
        article: String,
        unitPrice: Double,
        quantity: Double,
        inventoryItemId: Long?,
        addToInventory: Boolean,
        unit: String,
    ) {
        val now = System.currentTimeMillis()
        var linkId = inventoryItemId
        if (linkId == null && addToInventory) {
            linkId = createInventoryItem(article, unit, unitPrice)
        }
        val existing = existingId?.let { getCost(it) }
        if (existing == null) {
            insertCost(
                CostItem(
                    projectId = projectId,
                    article = article,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    inventoryItemId = linkId,
                ),
            )
            return
        }
        if (existing.unitPrice != unitPrice) {
            val before = projectTotal(projectId)
            val after = before - existing.unitPrice * existing.quantity + unitPrice * quantity
            insertCostChange(
                CostChange(
                    projectId = projectId,
                    costItemId = existing.id,
                    article = article,
                    oldUnitPrice = existing.unitPrice,
                    newUnitPrice = unitPrice,
                    quantity = quantity,
                    totalBefore = before,
                    totalAfter = after,
                    changedAt = now,
                    source = if (linkId != null) "inventario" else "manual",
                ),
            )
        }
        updateCost(
            existing.copy(article = article, unitPrice = unitPrice, quantity = quantity, inventoryItemId = linkId),
        )
    }

    // ---------- Inventario ----------

    @Query(
        """
        SELECT i.*,
            (SELECT COUNT(DISTINCT c.projectId) FROM cost_items c WHERE c.inventoryItemId = i.id) AS projectCount
        FROM inventory_items i
        ORDER BY i.name COLLATE NOCASE
        """
    )
    abstract fun observeInventory(): Flow<List<InventoryRow>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    abstract suspend fun getInventoryItem(id: Long): InventoryItem?

    @Insert
    abstract suspend fun insertInventoryItem(item: InventoryItem): Long

    @Query(
        "UPDATE inventory_items SET name = :name, unit = :unit, currentPrice = :price, updatedAt = :updatedAt WHERE id = :id",
    )
    abstract suspend fun updateInventoryFields(id: Long, name: String, unit: String, price: Double, updatedAt: Long)

    @Insert
    abstract suspend fun insertPriceHistory(entry: PriceHistory): Long

    @Query("SELECT * FROM price_history WHERE itemId = :itemId ORDER BY changedAt DESC, id DESC")
    abstract fun observePriceHistory(itemId: Long): Flow<List<PriceHistory>>

    @Query("SELECT * FROM cost_items WHERE inventoryItemId = :itemId")
    abstract suspend fun costsLinkedTo(itemId: Long): List<CostItem>

    @Query("UPDATE cost_items SET unitPrice = :price WHERE inventoryItemId = :itemId")
    abstract suspend fun updateLinkedPrices(itemId: Long, price: Double)

    @Query("UPDATE cost_items SET article = :name WHERE inventoryItemId = :itemId")
    abstract suspend fun renameLinkedCosts(itemId: Long, name: String)

    @Query("UPDATE cost_items SET inventoryItemId = NULL WHERE inventoryItemId = :itemId")
    abstract suspend fun unlinkCosts(itemId: Long)

    @Query("DELETE FROM price_history WHERE itemId = :itemId")
    abstract suspend fun deletePriceHistoryOf(itemId: Long)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    abstract suspend fun deleteInventoryRow(id: Long)

    @Transaction
    open suspend fun createInventoryItem(name: String, unit: String, price: Double): Long {
        val now = System.currentTimeMillis()
        val id = insertInventoryItem(
            InventoryItem(name = name, unit = unit, currentPrice = price, createdAt = now, updatedAt = now),
        )
        insertPriceHistory(PriceHistory(itemId = id, price = price, changedAt = now))
        return id
    }

    /**
     * Actualiza un artículo del inventario. Si cambia el precio, lo guarda en su histórico,
     * actualiza el precio en todos los proyectos que lo usan y deja registrado el cambio en cada uno.
     * Devuelve cuántos proyectos se actualizaron.
     */
    @Transaction
    open suspend fun updateInventoryItem(id: Long, name: String, unit: String, price: Double): Int {
        val old = getInventoryItem(id) ?: return 0
        val now = System.currentTimeMillis()
        val priceChanged = old.currentPrice != price
        updateInventoryFields(id, name, unit, price, if (priceChanged) now else old.updatedAt)
        if (name != old.name) renameLinkedCosts(id, name)
        if (!priceChanged) return 0

        insertPriceHistory(PriceHistory(itemId = id, price = price, changedAt = now))
        val linked = costsLinkedTo(id).filter { it.unitPrice != price }
        val byProject = linked.groupBy { it.projectId }
        for ((projectId, items) in byProject) {
            var total = projectTotal(projectId)
            for (c in items) {
                val after = total - c.unitPrice * c.quantity + price * c.quantity
                insertCostChange(
                    CostChange(
                        projectId = projectId,
                        costItemId = c.id,
                        article = name,
                        oldUnitPrice = c.unitPrice,
                        newUnitPrice = price,
                        quantity = c.quantity,
                        totalBefore = total,
                        totalAfter = after,
                        changedAt = now,
                        source = "inventario",
                    ),
                )
                total = after
            }
        }
        updateLinkedPrices(id, price)
        return byProject.size
    }

    @Transaction
    open suspend fun deleteInventoryItem(id: Long) {
        unlinkCosts(id)
        deletePriceHistoryOf(id)
        deleteInventoryRow(id)
    }
}

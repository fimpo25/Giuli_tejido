package com.giuli.tejido.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Momento en que se puso en marcha el cronómetro; null si está en pausa. */
    val runningSince: Long? = null,
    /** Índice en la paleta de colores de proyecto. */
    @ColumnInfo(defaultValue = "0") val colorIndex: Int = 0,
)

/** Un tramo de tiempo trabajado (de "iniciar" a "pausar"), o un ajuste manual. */
@Entity(tableName = "time_sessions", indices = [Index("projectId")])
data class TimeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val startedAt: Long,
    val durationMs: Long,
    val manual: Boolean = false,
)

@Entity(tableName = "counters", indices = [Index("projectId")])
data class Counter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val value: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notes", indices = [Index("projectId")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val text: String,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cost_items", indices = [Index("projectId"), Index("inventoryItemId")])
data class CostItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val article: String,
    val unitPrice: Double,
    val quantity: Double,
    val createdAt: Long = System.currentTimeMillis(),
    /** Si viene del inventario, su precio se actualiza cuando cambia allí. */
    val inventoryItemId: Long? = null,
)

fun CostItem.subtotal(): Double = unitPrice * quantity

/** Artículo del inventario general (fuera de los proyectos). */
@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String = "",
    val currentPrice: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Cada precio que tuvo un artículo del inventario. */
@Entity(tableName = "price_history", indices = [Index("itemId")])
data class PriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val price: Double,
    val changedAt: Long = System.currentTimeMillis(),
)

/** Registro de un cambio de precio dentro de un proyecto, con el total antes y después. */
@Entity(tableName = "cost_changes", indices = [Index("projectId")])
data class CostChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val costItemId: Long,
    val article: String,
    val oldUnitPrice: Double,
    val newUnitPrice: Double,
    val quantity: Double,
    val totalBefore: Double,
    val totalAfter: Double,
    val changedAt: Long = System.currentTimeMillis(),
    /** "inventario" o "manual" */
    val source: String,
)

data class InventoryRow(
    @Embedded val item: InventoryItem,
    val projectCount: Int,
)

data class ProjectSummary(
    @Embedded val project: Project,
    val trackedMs: Long,
    val totalCost: Double,
    val coverPhoto: String?,
)

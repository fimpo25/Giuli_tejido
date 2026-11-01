package com.giuli.tejido.data

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

@Entity(tableName = "cost_items", indices = [Index("projectId")])
data class CostItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val article: String,
    val unitPrice: Double,
    val quantity: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

fun CostItem.subtotal(): Double = unitPrice * quantity

data class ProjectSummary(
    @Embedded val project: Project,
    val trackedMs: Long,
    val totalCost: Double,
    val coverPhoto: String?,
)

package com.bakersoftware.plannextday.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["scheduledDate"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val scheduledDate: String,
    val completed: Boolean = false,
    val accounted: Boolean = false
)

@Entity(tableName = "weeks")
data class WeekEntity(
    @PrimaryKey val weekStart: String,
    val maxMisses: Int,
    val completedCount: Int = 0,
    val missedCount: Int = 0,
    val finalized: Boolean = false,
    val recapAcknowledged: Boolean = false
)

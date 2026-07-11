package com.bakersoftware.plannextday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY id ASC")
    fun observeForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE scheduledDate = :date")
    suspend fun countForDate(date: String): Int

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :id AND scheduledDate = :date")
    suspend fun deleteFutureTask(id: Long, date: String)

    @Query("DELETE FROM tasks WHERE scheduledDate = :date")
    suspend fun deleteForDate(date: String)

    @Query("UPDATE tasks SET completed = :completed WHERE id = :id AND scheduledDate = :date")
    suspend fun setCompleted(id: Long, date: String, completed: Boolean)

    @Query("SELECT * FROM tasks WHERE scheduledDate < :today AND accounted = 0")
    suspend fun unaccountedBefore(today: String): List<TaskEntity>

    @Query("UPDATE tasks SET accounted = 1 WHERE id IN (:ids)")
    suspend fun markAccounted(ids: List<Long>)
}

@Dao
interface WeekDao {
    @Query("SELECT * FROM weeks WHERE weekStart = :weekStart LIMIT 1")
    suspend fun get(weekStart: String): WeekEntity?

    @Query("SELECT * FROM weeks WHERE weekStart = :weekStart LIMIT 1")
    fun observe(weekStart: String): Flow<WeekEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(week: WeekEntity)

    @Query("UPDATE weeks SET finalized = 1 WHERE weekStart < :currentWeekStart")
    suspend fun finalizeBefore(currentWeekStart: String)

    @Query("SELECT * FROM weeks WHERE finalized = 1 AND recapAcknowledged = 0 ORDER BY weekStart DESC LIMIT 1")
    fun observePendingRecap(): Flow<WeekEntity?>

    @Query("UPDATE weeks SET recapAcknowledged = 1 WHERE weekStart = :weekStart")
    suspend fun acknowledgeRecap(weekStart: String)
}

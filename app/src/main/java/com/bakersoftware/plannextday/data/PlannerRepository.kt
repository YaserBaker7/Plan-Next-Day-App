package com.bakersoftware.plannextday.data

import androidx.room.withTransaction
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PlannerRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val tasks = database.taskDao()
    private val weeks = database.weekDao()

    fun tasksFor(date: LocalDate): Flow<List<TaskEntity>> =
        tasks.observeForDate(date.toString())

    fun weekFor(weekStart: LocalDate): Flow<WeekEntity?> =
        weeks.observe(weekStart.toString())

    fun pendingRecap(): Flow<WeekEntity?> = weeks.observePendingRecap()

    suspend fun reconcile(today: LocalDate = LocalDate.now()) {
        val currentWeekStart = today.startOfWeek()
        val settings = settingsRepository.settings.first()
        database.withTransaction {
            ensureWeek(currentWeekStart, settings.nextWeekMissLimit)
            val staleTasks = tasks.unaccountedBefore(today.toString())
            staleTasks.groupBy { LocalDate.parse(it.scheduledDate).startOfWeek() }
                .forEach { (weekStart, weekTasks) ->
                    val week = ensureWeek(weekStart, settings.nextWeekMissLimit)
                    weeks.upsert(
                        week.copy(
                            completedCount = week.completedCount + weekTasks.count { it.completed },
                            missedCount = week.missedCount + weekTasks.count { !it.completed }
                        )
                    )
                    tasks.markAccounted(weekTasks.map { it.id })
                }
            weeks.finalizeBefore(currentWeekStart.toString())
        }
    }

    suspend fun addTomorrowTask(title: String, today: LocalDate) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        tasks.insert(TaskEntity(title = cleanTitle, scheduledDate = today.plusDays(1).toString()))
        settingsRepository.markPlannedBefore()
    }

    suspend fun deleteTomorrowTask(id: Long, today: LocalDate) {
        tasks.deleteFutureTask(id, today.plusDays(1).toString())
    }

    suspend fun restoreTomorrowTask(task: TaskEntity) {
        tasks.insert(task.copy(id = 0))
    }

    suspend fun copyTodayToTomorrow(today: LocalDate) {
        val destination = today.plusDays(1).toString()
        val todayTasks = tasksForSnapshot(today)
        database.withTransaction {
            tasks.deleteForDate(destination)
            todayTasks.forEach { task ->
                tasks.insert(TaskEntity(title = task.title, scheduledDate = destination))
            }
        }
        if (todayTasks.isNotEmpty()) settingsRepository.markPlannedBefore()
    }

    suspend fun setCompleted(id: Long, completed: Boolean, today: LocalDate) {
        tasks.setCompleted(id, today.toString(), completed)
    }

    suspend fun acknowledgeRecap(weekStart: String) {
        weeks.acknowledgeRecap(weekStart)
    }

    private suspend fun tasksForSnapshot(date: LocalDate): List<TaskEntity> =
        tasks.observeForDate(date.toString()).first()

    private suspend fun ensureWeek(weekStart: LocalDate, maxMisses: Int): WeekEntity {
        return weeks.get(weekStart.toString()) ?: WeekEntity(
            weekStart = weekStart.toString(),
            maxMisses = maxMisses
        ).also { weeks.upsert(it) }
    }
}

fun LocalDate.startOfWeek(): LocalDate =
    with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

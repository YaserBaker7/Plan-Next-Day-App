package com.bakersoftware.plannextday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bakersoftware.plannextday.data.AppDatabase
import com.bakersoftware.plannextday.data.AppSettings
import com.bakersoftware.plannextday.data.PlannerRepository
import com.bakersoftware.plannextday.data.SettingsRepository
import com.bakersoftware.plannextday.data.TaskEntity
import com.bakersoftware.plannextday.data.ThemeChoice
import com.bakersoftware.plannextday.data.WeekEntity
import com.bakersoftware.plannextday.data.startOfWeek
import com.bakersoftware.plannextday.reminder.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

data class PlannerUiState(
    val today: LocalDate = LocalDate.now(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val tomorrowTasks: List<TaskEntity> = emptyList(),
    val week: WeekEntity? = null,
    val settings: AppSettings = AppSettings(),
    val pendingRecap: WeekEntity? = null,
    val ready: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val repository = PlannerRepository(AppDatabase.get(application), settingsRepository)
    private val scheduler = ReminderScheduler(application)
    private val currentDate = MutableStateFlow(LocalDate.now())

    private val todayTasks = currentDate.flatMapLatest { repository.tasksFor(it) }
    private val tomorrowTasks = currentDate.flatMapLatest { repository.tasksFor(it.plusDays(1)) }
    private val currentWeek = currentDate
        .map { it.startOfWeek() }
        .flatMapLatest { repository.weekFor(it) }

    val uiState = combine(
        todayTasks,
        tomorrowTasks,
        currentWeek,
        settingsRepository.settings
    ) { today, tomorrow, week, settings ->
        PlannerUiState(
            today = currentDate.value,
            todayTasks = today,
            tomorrowTasks = tomorrow,
            week = week,
            settings = settings,
            ready = true
        )
    }.combine(repository.pendingRecap()) { state, recap ->
        state.copy(pendingRecap = recap)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlannerUiState()
    )

    init {
        refreshForToday()
    }

    fun refreshForToday() {
        viewModelScope.launch {
            val today = LocalDate.now()
            currentDate.value = today
            repository.reconcile(today)
            scheduler.scheduleNext(settingsRepository.settings.first())
        }
    }

    fun addTomorrowTask(title: String) {
        viewModelScope.launch {
            repository.addTomorrowTask(title, currentDate.value)
        }
    }

    fun deleteTomorrowTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTomorrowTask(task.id, currentDate.value)
        }
    }

    fun restoreTomorrowTask(task: TaskEntity) {
        viewModelScope.launch { repository.restoreTomorrowTask(task) }
    }

    fun copyTodayToTomorrow() {
        viewModelScope.launch { repository.copyTodayToTomorrow(currentDate.value) }
    }

    fun setCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            repository.setCompleted(task.id, completed, currentDate.value)
        }
    }

    fun acknowledgeRecap(week: WeekEntity) {
        viewModelScope.launch { repository.acknowledgeRecap(week.weekStart) }
    }

    fun setTheme(theme: ThemeChoice) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            scheduler.scheduleNext(settingsRepository.settings.first())
        }
    }

    fun setReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderMinutes(minutes)
            scheduler.scheduleNext(settingsRepository.settings.first())
        }
    }

    fun setNextWeekMissLimit(limit: Int) {
        viewModelScope.launch { settingsRepository.setNextWeekMissLimit(limit) }
    }
}

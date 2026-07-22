package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TaskDatabase
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TaskFilter {
    ALL,      // الكل
    PENDING,  // المتبقية
    COMPLETED // المكتملة
}

data class CurrentTimeState(
    val dayName: String = "",
    val fullDate: String = "",
    val timeString: String = "",
    val rawCalendar: Calendar = Calendar.getInstance()
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    val selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val searchQuery = MutableStateFlow("")

    val currentTimeState = MutableStateFlow(CurrentTimeState())

    init {
        val db = TaskDatabase.getDatabase(application)
        repository = TaskRepository(db.taskDao())

        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }

        // Live time synchronization loop
        viewModelScope.launch {
            while (true) {
                updateTimeState()
                delay(1000)
            }
        }
    }

    private fun updateTimeState() {
        val calendar = Calendar.getInstance()
        val arabicLocale = Locale("ar")

        val dayFormat = SimpleDateFormat("EEEE", arabicLocale)
        val dateFormat = SimpleDateFormat("d MMMM yyyy", arabicLocale)
        val timeFormat = SimpleDateFormat("hh:mm:ss a", arabicLocale)

        currentTimeState.value = CurrentTimeState(
            dayName = dayFormat.format(calendar.time),
            fullDate = dateFormat.format(calendar.time),
            timeString = timeFormat.format(calendar.time),
            rawCalendar = calendar
        )
    }

    val tasks: StateFlow<List<TaskEntity>> = combine(
        repository.allTasks,
        selectedFilter,
        searchQuery
    ) { allTaskList, filter, query ->
        allTaskList
            .filter { task ->
                when (filter) {
                    TaskFilter.ALL -> true
                    TaskFilter.PENDING -> !task.isCompleted
                    TaskFilter.COMPLETED -> task.isCompleted
                }
            }
            .filter { task ->
                if (query.isBlank()) true
                else task.content.contains(query, ignoreCase = true) ||
                        task.category.contains(query, ignoreCase = true)
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val taskStats = repository.allTasks.map { list ->
        val total = list.size
        val completed = list.count { it.isCompleted }
        val pending = total - completed
        val percentage = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0
        TaskStats(total, completed, pending, percentage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    fun addTask(
        content: String,
        timeSchedule: String,
        hasNotification: Boolean,
        category: String,
        priority: String
    ) {
        viewModelScope.launch {
            val currentList = tasks.value
            val nextSerial = if (currentList.isNotEmpty()) currentList.maxOf { it.serialIndex } + 1 else 1
            val newTask = TaskEntity(
                serialIndex = nextSerial,
                content = content.trim(),
                isCompleted = false,
                timeSchedule = timeSchedule.ifBlank { "08:00 ص" },
                hasNotification = hasNotification,
                category = category.ifBlank { "عام" },
                priority = priority.ifBlank { "متوسطة" }
            )
            repository.insert(newTask)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun toggleCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleCompletion(task.id, task.isCompleted)
        }
    }

    fun toggleNotification(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleNotification(task.id, task.hasNotification)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    fun autoScheduleTasks() {
        viewModelScope.launch {
            val allList = repository.allTasks
            // Auto schedule pending tasks with chronologically spaced hours (e.g., 08:00 AM, 10:00 AM, 12:00 PM...)
            val list = tasks.value
            val defaultTimes = listOf(
                "08:00 ص", "09:30 ص", "11:00 ص", "01:30 م",
                "03:30 م", "05:00 م", "07:00 م", "08:30 م", "10:00 م"
            )

            list.forEachIndexed { index, task ->
                val assignedTime = defaultTimes.getOrElse(index % defaultTimes.size) { "08:00 ص" }
                val updated = task.copy(
                    serialIndex = index + 1,
                    timeSchedule = assignedTime
                )
                repository.update(updated)
            }
        }
    }
}

data class TaskStats(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val percentage: Int = 0
)

package com.example.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.TaskEntity

enum class AppScreen {
    WELCOME,
    HOME,
    TASKS
}

@Composable
fun TaskScreen(
    viewModel: TaskViewModel
) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    val tasks by viewModel.tasks.collectAsState()
    val timeState by viewModel.currentTimeState.collectAsState()
    val stats by viewModel.taskStats.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    // Enforce Full Arabic Right-To-Left Layout Direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (currentScreen) {
            AppScreen.WELCOME -> {
                WelcomeScreen(
                    onStartClick = { currentScreen = AppScreen.HOME }
                )
            }
            AppScreen.HOME -> {
                HomeScreen(
                    stats = stats,
                    onNavigateToTasks = { currentScreen = AppScreen.TASKS },
                    onAddTaskClick = { showAddDialog = true },
                    onNavigateToWelcome = { currentScreen = AppScreen.WELCOME }
                )
            }
            AppScreen.TASKS -> {
                TaskListScreen(
                    tasks = tasks,
                    timeState = timeState,
                    stats = stats,
                    selectedFilter = selectedFilter,
                    searchQuery = searchQuery,
                    onFilterSelect = { viewModel.selectedFilter.value = it },
                    onSearchChange = { viewModel.searchQuery.value = it },
                    onAutoSchedule = { viewModel.autoScheduleTasks() },
                    onClearCompleted = { viewModel.clearCompleted() },
                    onToggleCompletion = { viewModel.toggleCompletion(it) },
                    onToggleNotification = { viewModel.toggleNotification(it) },
                    onEditTask = { taskToEdit = it },
                    onDeleteTask = { taskToDelete = it },
                    onAddTaskClick = { showAddDialog = true },
                    onBackToHome = { currentScreen = AppScreen.HOME },
                    onNavigateToWelcome = { currentScreen = AppScreen.WELCOME }
                )
            }
        }

        // Dialogs
        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onSave = { content, timeSchedule, hasNotification, category, priority ->
                    viewModel.addTask(content, timeSchedule, hasNotification, category, priority)
                    showAddDialog = false
                }
            )
        }

        taskToEdit?.let { task ->
            AddTaskDialog(
                initialTask = task,
                onDismiss = { taskToEdit = null },
                onSave = { content, timeSchedule, hasNotification, category, priority ->
                    viewModel.updateTask(
                        task.copy(
                            content = content,
                            timeSchedule = timeSchedule,
                            hasNotification = hasNotification,
                            category = category,
                            priority = priority
                        )
                    )
                    taskToEdit = null
                }
            )
        }

        taskToDelete?.let { task ->
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                title = { Text("حذف المهمة") },
                text = { Text("هل أنت تأكد من رغبتك في حذف مهمة: \"${task.content}\"؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTask(task)
                            taskToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

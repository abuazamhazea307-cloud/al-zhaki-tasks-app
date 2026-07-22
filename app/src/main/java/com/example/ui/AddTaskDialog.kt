package com.example.ui

import androidx.compose.runtime.Composable
import com.example.data.TaskEntity

@Composable
fun AddTaskDialog(
    initialTask: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSave: (content: String, timeSchedule: String, hasNotification: Boolean, category: String, priority: String) -> Unit
) {
    TaskDialog(
        initialTask = initialTask,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

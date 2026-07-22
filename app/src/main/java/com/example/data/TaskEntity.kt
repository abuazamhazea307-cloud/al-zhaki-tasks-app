package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serialIndex: Int = 1,
    val content: String,
    val isCompleted: Boolean = false,
    val timeSchedule: String = "08:00 ص",
    val hasNotification: Boolean = true,
    val category: String = "عام",
    val priority: String = "متوسطة",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

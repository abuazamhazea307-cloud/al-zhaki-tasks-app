package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun insert(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun update(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun delete(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun toggleCompletion(taskId: Int, currentStatus: Boolean) {
        taskDao.updateCompletionStatus(taskId, !currentStatus)
    }

    suspend fun toggleNotification(taskId: Int, currentStatus: Boolean) {
        taskDao.updateNotificationStatus(taskId, !currentStatus)
    }

    suspend fun clearCompleted() {
        taskDao.clearCompletedTasks()
    }

    suspend fun prepopulateIfEmpty() {
        if (taskDao.getTaskCount() == 0) {
            val initialTasks = listOf(
                TaskEntity(
                    serialIndex = 1,
                    content = "صلاة الفجر والأذكار اليومية",
                    isCompleted = true,
                    timeSchedule = "05:00 ص",
                    hasNotification = true,
                    category = "عبادة",
                    priority = "عالية"
                ),
                TaskEntity(
                    serialIndex = 2,
                    content = "مراجعة جدول الأعمال والأولويات اليومية",
                    isCompleted = false,
                    timeSchedule = "08:00 ص",
                    hasNotification = true,
                    category = "تخطيط",
                    priority = "عالية"
                ),
                TaskEntity(
                    serialIndex = 3,
                    content = "إنجاز المشروع الرئيسي وإرسال التقارير المطلوبة",
                    isCompleted = false,
                    timeSchedule = "10:30 ص",
                    hasNotification = true,
                    category = "عمل",
                    priority = "عالية"
                ),
                TaskEntity(
                    serialIndex = 4,
                    content = "قراءة 20 صفحة من كتاب في التطوير الشخصي",
                    isCompleted = false,
                    timeSchedule = "04:30 م",
                    hasNotification = false,
                    category = "تعلم",
                    priority = "متوسطة"
                ),
                TaskEntity(
                    serialIndex = 5,
                    content = "ممارسة التمارين الرياضية أو المشي الخفيف",
                    isCompleted = false,
                    timeSchedule = "06:30 م",
                    hasNotification = true,
                    category = "صحة",
                    priority = "متوسطة"
                ),
                TaskEntity(
                    serialIndex = 6,
                    content = "مراجعة إنجازات اليوم والتحضير للغد",
                    isCompleted = false,
                    timeSchedule = "09:00 م",
                    hasNotification = true,
                    category = "شخصي",
                    priority = "عادية"
                )
            )
            taskDao.insertTasks(initialTasks)
        }
    }
}

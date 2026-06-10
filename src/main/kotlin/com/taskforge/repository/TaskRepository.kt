package com.taskforge.repository

import com.taskforge.model.TaskExecution

/**
 * Repository for persisting task execution records.
 */
interface TaskRepository {
    fun save(execution: TaskExecution)
    fun findById(id: String): TaskExecution?
}

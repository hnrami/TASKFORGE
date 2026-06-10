package com.taskforge.repository

import com.taskforge.model.WorkflowExecution

/**
 * Placeholder for execution persistence operations.
 */
interface ExecutionRepository {
    fun save(execution: WorkflowExecution)
    fun findById(id: String): WorkflowExecution?
}

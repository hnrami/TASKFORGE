package com.taskforge.repository

import com.taskforge.model.WorkflowDefinition

/**
 * Repository for persisting workflow definitions.
 */
interface WorkflowRepository {
    fun save(definition: WorkflowDefinition)
    fun findById(id: String): WorkflowDefinition?
    fun findAll(): List<WorkflowDefinition>
    fun delete(id: String)
}

package com.taskforge.repository.impl

import com.taskforge.model.WorkflowDefinition
import com.taskforge.repository.WorkflowRepository

/**
 * In-memory implementation of WorkflowRepository.
 * For production, replace with JPA repository backed by database.
 */
class InMemoryWorkflowRepository : WorkflowRepository {
    private val workflows = mutableMapOf<String, WorkflowDefinition>()

    override fun save(definition: WorkflowDefinition) {
        workflows[definition.id] = definition
    }

    override fun findById(id: String): WorkflowDefinition? {
        return workflows[id]
    }

    override fun findAll(): List<WorkflowDefinition> {
        return workflows.values.toList()
    }

    override fun delete(id: String) {
        workflows.remove(id)
    }
}

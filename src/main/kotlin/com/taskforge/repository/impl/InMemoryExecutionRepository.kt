package com.taskforge.repository.impl

import com.taskforge.model.WorkflowExecution
import com.taskforge.repository.ExecutionRepository

/**
 * In-memory implementation of ExecutionRepository.
 * For production, replace with JPA repository backed by database.
 */
class InMemoryExecutionRepository : ExecutionRepository {
    private val executions = mutableMapOf<String, WorkflowExecution>()

    override fun save(execution: WorkflowExecution) {
        executions[execution.id] = execution
    }

    override fun findById(id: String): WorkflowExecution? {
        return executions[id]
    }

    fun findByDefinitionId(definitionId: String): List<WorkflowExecution> {
        return executions.values.filter { it.definitionId == definitionId }
    }

    fun findAll(): List<WorkflowExecution> {
        return executions.values.toList()
    }
}

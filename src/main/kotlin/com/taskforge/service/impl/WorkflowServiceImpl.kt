package com.taskforge.service.impl

import com.taskforge.model.WorkflowDefinition
import com.taskforge.service.WorkflowService
import com.taskforge.repository.WorkflowRepository
import com.taskforge.exception.TaskForgeException

/**
 * Service for managing workflow definitions.
 */
class WorkflowServiceImpl(private val workflowRepository: WorkflowRepository) : WorkflowService {

    override fun createWorkflow(definition: WorkflowDefinition) {
        // Validate workflow has ID
        if (definition.id.isBlank()) {
            throw TaskForgeException("Workflow ID cannot be empty")
        }

        // Check if workflow already exists
        if (workflowRepository.findById(definition.id) != null) {
            throw TaskForgeException("Workflow with ID '${definition.id}' already exists")
        }

        workflowRepository.save(definition)
    }

    override fun getWorkflow(id: String): WorkflowDefinition? {
        return workflowRepository.findById(id)
    }
}

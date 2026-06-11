package com.taskforge.service.impl

import com.taskforge.model.WorkflowExecution
import com.taskforge.service.ExecutionService
import com.taskforge.engine.ExecutionEngine
import com.taskforge.repository.WorkflowRepository
import com.taskforge.repository.ExecutionRepository
import com.taskforge.repository.impl.InMemoryExecutionRepository
import com.taskforge.exception.TaskForgeException

/**
 * Service for managing workflow execution.
 */
class ExecutionServiceImpl(
    private val executionEngine: ExecutionEngine,
    private val workflowRepository: WorkflowRepository,
    private val executionRepository: ExecutionRepository
) : ExecutionService {

    override fun startExecution(workflowId: String): WorkflowExecution {
        // Get the workflow definition
        val definition = workflowRepository.findById(workflowId)
            ?: throw TaskForgeException("Workflow not found: $workflowId")

        // Start execution through engine
        val execution = executionEngine.startWorkflow(definition)

        return execution
    }

    override fun getExecution(id: String): WorkflowExecution? {
        return executionRepository.findById(id)
    }

    override fun cancelExecution(id: String): WorkflowExecution = executionEngine.cancel(id)

    override fun resolveApproval(id: String, taskId: String, approved: Boolean): WorkflowExecution =
        executionEngine.resolveApproval(id, taskId, approved)
}

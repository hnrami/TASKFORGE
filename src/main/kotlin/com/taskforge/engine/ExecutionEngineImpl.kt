package com.taskforge.engine

import com.taskforge.exception.TaskForgeException
import com.taskforge.handler.TaskHandlerRegistry
import com.taskforge.model.*
import com.taskforge.repository.WorkflowRepository
import com.taskforge.repository.ExecutionRepository
import java.time.Instant
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Orchestrates workflow execution using DAG scheduling.
 * 
 * Execution model:
 * 1. Validate the DAG structure
 * 2. Create a WorkflowExecution instance
 * 3. Loop until completion:
 *    - Find ready tasks using DagScheduler
 *    - Execute ready tasks in parallel (one per task initially for simplicity)
 *    - Track execution state
 *    - Handle failures based on retry policy
 * 4. Mark workflow as SUCCESS or FAILED
 */
class ExecutionEngineImpl(
    private val dagValidator: DagValidator,
    private val dagScheduler: DagSchedulerImpl,
    private val stateManager: StateManager,
    private val handlerRegistry: TaskHandlerRegistry,
    private val workflowRepository: WorkflowRepository,
    private val executionRepository: ExecutionRepository
) : ExecutionEngine {

    override fun startWorkflow(definition: WorkflowDefinition): WorkflowExecution {
        // Validate DAG structure first
        dagValidator.validate(definition)

        // Create execution instance
        val execution = WorkflowExecution(
            id = UUID.randomUUID().toString(),
            definitionId = definition.id,
            status = ExecutionStatus.CREATED,
            startedAt = null,
            finishedAt = null,
            tasks = emptyList()
        )

        // Store execution
        executionRepository.save(execution)

        // Start execution asynchronously
        executeWorkflow(definition, execution.id)

        return execution
    }

    /**
     * Execute workflow asynchronously to avoid blocking.
     */
    private fun executeWorkflow(definition: WorkflowDefinition, executionId: String) {
        thread(start = true) {
            try {
                executeWorkflowSync(definition, executionId)
            } catch (e: Exception) {
                markExecutionFailed(executionId, e.message ?: "Unknown error")
            }
        }
    }

    private fun executeWorkflowSync(definition: WorkflowDefinition, executionId: String) {
        var execution = executionRepository.findById(executionId)
            ?: throw TaskForgeException("Execution not found: $executionId")

        // Mark as running
        execution = execution.copy(
            status = ExecutionStatus.RUNNING,
            startedAt = Instant.now()
        )
        executionRepository.save(execution)

        val sharedContext = TaskContext()
        val maxIterations = definition.tasks.size * 10 // Prevent infinite loops

        var iteration = 0
        while (iteration < maxIterations) {
            iteration++

            execution = executionRepository.findById(executionId)
                ?: throw TaskForgeException("Execution lost: $executionId")

            // Check if workflow is complete
            if (isWorkflowComplete(execution, definition)) {
                break
            }

            // Find ready tasks
            val readyTaskIds = dagScheduler.findReadyTasksForExecution(definition, execution)

            if (readyTaskIds.isEmpty()) {
                // No tasks ready, check if we're in a bad state
                if (execution.tasks.isNotEmpty()) {
                    // We have tasks but none are ready - check for failures
                    if (execution.tasks.any { it.status == TaskStatus.FAILED }) {
                        break // Workflow failed
                    }
                }
                break // No progress possible
            }

            // Execute ready tasks
            for (taskId in readyTaskIds) {
                val taskDef = definition.tasks.find { it.id == taskId }
                    ?: throw TaskForgeException("Task definition not found: $taskId")

                val handler = handlerRegistry.getHandler(taskDef.type)
                    ?: throw TaskForgeException("No handler for task type: ${taskDef.type}")

                // Create task execution record
                val taskExecution = TaskExecution(
                    id = UUID.randomUUID().toString(),
                    taskDefinitionId = taskId,
                    status = TaskStatus.RUNNING,
                    attempt = 0,
                    startedAt = Instant.now()
                )

                // Update execution with running task
                execution = execution.copy(
                    tasks = execution.tasks + taskExecution
                )
                executionRepository.save(execution)

                // Execute task
                val result = try {
                    handler.execute(taskDef, sharedContext)
                } catch (e: Exception) {
                    TaskResult(
                        status = TaskStatus.FAILED,
                        output = emptyMap(),
                        message = "Exception: ${e.message}"
                    )
                }

                // Update task execution with result
                val completedExecution = taskExecution.copy(
                    status = result.status,
                    finishedAt = Instant.now(),
                    result = result,
                    attempt = taskExecution.attempt + 1
                )

                // Update main execution
                execution = execution.copy(
                    tasks = execution.tasks.map { 
                        if (it.id == taskExecution.id) completedExecution else it 
                    }
                )
                executionRepository.save(execution)

                // Update context with task output if successful
                if (result.status == TaskStatus.SUCCESS) {
                    result.output.forEach { (key, value) ->
                        sharedContext.set("task.${taskId}.$key", value)
                    }
                }
            }

            // Small delay to prevent CPU spinning
            Thread.sleep(100)
        }

        // Mark workflow as complete
        val finalStatus = if (execution.tasks.all { it.status == TaskStatus.SUCCESS || it.status == TaskStatus.SKIPPED }) {
            ExecutionStatus.SUCCESS
        } else {
            ExecutionStatus.FAILED
        }

        execution = execution.copy(
            status = finalStatus,
            finishedAt = Instant.now()
        )
        executionRepository.save(execution)
    }

    private fun isWorkflowComplete(execution: WorkflowExecution, definition: WorkflowDefinition): Boolean {
        if (execution.tasks.isEmpty()) {
            return false // Hasn't started
        }

        val allTasksAttempted = execution.tasks.size == definition.tasks.size

        return allTasksAttempted && execution.tasks.all { 
            it.status == TaskStatus.SUCCESS || 
            it.status == TaskStatus.FAILED || 
            it.status == TaskStatus.SKIPPED
        }
    }

    private fun markExecutionFailed(executionId: String, reason: String) {
        try {
            val execution = executionRepository.findById(executionId)
            if (execution != null) {
                executionRepository.save(
                    execution.copy(
                        status = ExecutionStatus.FAILED,
                        finishedAt = Instant.now()
                    )
                )
            }
        } catch (e: Exception) {
            // Log error but don't throw
            System.err.println("Failed to mark execution as failed: $reason - ${e.message}")
        }
    }
}

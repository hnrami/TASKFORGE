package com.taskforge.engine

import com.taskforge.model.WorkflowExecution
import com.taskforge.model.ExecutionStatus
import com.taskforge.model.TaskStatus

/**
 * Manages workflow execution state transitions.
 */
class StateManagerImpl : StateManager {

    override fun updateWorkflowState(execution: WorkflowExecution) {
        // Update workflow status based on task states
        when {
            execution.tasks.isEmpty() -> {
                // No tasks yet, still running
            }
            execution.tasks.any { it.status == TaskStatus.FAILED } -> {
                // If any task failed, workflow is failed
                // (unless failure is handled by retry or skip logic)
            }
            execution.tasks.all { it.status == TaskStatus.SUCCESS || it.status == TaskStatus.SKIPPED } -> {
                // All tasks completed successfully or were skipped
                // (workflow will be marked successful in engine)
            }
            execution.tasks.any { it.status == TaskStatus.RUNNING } -> {
                // Workflow is still running
            }
        }
    }
}

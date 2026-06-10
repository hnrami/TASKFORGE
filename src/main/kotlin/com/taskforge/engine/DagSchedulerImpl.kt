package com.taskforge.engine

import com.taskforge.model.WorkflowDefinition
import com.taskforge.model.WorkflowExecution
import com.taskforge.model.TaskStatus

/**
 * Schedules tasks in a workflow based on DAG execution rules.
 * A task is ready when all its dependencies have completed successfully.
 */
class DagSchedulerImpl : DagScheduler {

    /**
     * Finds all tasks that are ready to execute initially.
     * For initial scheduling (no execution context):
     * - A task is ready only if it has NO dependencies
     * 
     * Returns task definition IDs (not execution IDs) that can start immediately.
     */
    override fun findReadyTasks(definition: WorkflowDefinition): List<String> {
        return definition.tasks
            .filter { task -> task.dependsOn.isEmpty() }
            .map { it.id }
    }

    /**
     * Finds ready tasks for a running execution, excluding already executed tasks.
     * Used during workflow execution to determine what to schedule next.
     */
    fun findReadyTasksForExecution(definition: WorkflowDefinition, execution: WorkflowExecution): List<String> {
        val executedTaskIds = execution.tasks.map { it.taskDefinitionId }.toSet()
        
        return definition.tasks
            .filter { task ->
                task.id !in executedTaskIds &&
                allDependenciesSatisfied(task.id, definition, execution)
            }
            .map { it.id }
    }

    private fun allDependenciesSatisfied(
        taskId: String,
        definition: WorkflowDefinition,
        execution: WorkflowExecution
    ): Boolean {
        val task = definition.tasks.find { it.id == taskId } ?: return false
        
        // If task has no dependencies, it's ready
        if (task.dependsOn.isEmpty()) {
            return true
        }
        
        for (depId in task.dependsOn) {
            val depExecution = execution.tasks.find { it.taskDefinitionId == depId }
            
            // If dependency hasn't been executed yet, not ready
            if (depExecution == null) {
                return false
            }
            
            // If dependency didn't succeed, not ready (skip or fail)
            if (depExecution.status != TaskStatus.SUCCESS) {
                return false
            }
        }
        
        return true
    }
}

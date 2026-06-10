package com.taskforge.engine

import com.taskforge.exception.TaskForgeException
import com.taskforge.model.WorkflowDefinition
import com.taskforge.handler.TaskHandlerRegistry

/**
 * Validates workflow DAG structure.
 * Checks for:
 * - Cycles (prevents infinite loops)
 * - Invalid dependencies (missing referenced tasks)
 * - Unregistered task types (ensures handlers exist)
 */
class DagValidatorImpl(private val handlerRegistry: TaskHandlerRegistry) : DagValidator {

    override fun validate(definition: WorkflowDefinition) {
        if (definition.tasks.isEmpty()) {
            return // Empty workflows are valid
        }

        // Check for cycles using DFS
        checkForCycles(definition)
        
        // Check for invalid dependencies
        checkDependencies(definition)
        
        // Check for unregistered task types
        checkTaskTypes(definition)
    }

    private fun checkForCycles(definition: WorkflowDefinition) {
        val taskIds = definition.tasks.map { it.id }.toSet()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        for (task in definition.tasks) {
            if (task.id !in visited) {
                dfs(task.id, taskIds, visited, visiting, definition)
            }
        }
    }

    private fun dfs(
        taskId: String,
        taskIds: Set<String>,
        visited: MutableSet<String>,
        visiting: MutableSet<String>,
        definition: WorkflowDefinition
    ) {
        if (taskId in visiting) {
            throw TaskForgeException("Cycle detected in workflow DAG involving task: $taskId")
        }

        if (taskId in visited) {
            return
        }

        visiting.add(taskId)

        val task = definition.tasks.find { it.id == taskId }
        if (task != null) {
            for (dependency in task.dependsOn) {
                if (dependency in taskIds) {
                    dfs(dependency, taskIds, visited, visiting, definition)
                }
            }
        }

        visiting.remove(taskId)
        visited.add(taskId)
    }

    private fun checkDependencies(definition: WorkflowDefinition) {
        val taskIds = definition.tasks.map { it.id }.toSet()
        
        for (task in definition.tasks) {
            for (dependency in task.dependsOn) {
                if (dependency !in taskIds) {
                    throw TaskForgeException(
                        "Task '${task.id}' depends on non-existent task: $dependency"
                    )
                }
            }
        }
    }

    private fun checkTaskTypes(definition: WorkflowDefinition) {
        for (task in definition.tasks) {
            if (handlerRegistry.getHandler(task.type) == null) {
                throw TaskForgeException(
                    "No handler registered for task type: ${task.type}"
                )
            }
        }
    }
}

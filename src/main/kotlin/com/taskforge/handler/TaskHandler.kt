package com.taskforge.handler

import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskResult

/**
 * Contract for all task handlers.
 * Implementations should handle a concrete TaskDefinition subtype.
 */
interface TaskHandler<T : TaskDefinition> {
    fun handle(definition: T, context: TaskContext): TaskResult
    fun supports(type: String): Boolean
}

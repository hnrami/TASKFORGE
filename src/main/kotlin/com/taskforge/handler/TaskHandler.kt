package com.taskforge.handler

import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskResult

/**
 * Contract for all task handlers.
 * Task handlers are registered by type and executed through the engine without
 * any concrete task-type logic inside the execution engine.
 */
interface TaskHandler {
    fun type(): String
    fun execute(definition: TaskDefinition, context: TaskContext): TaskResult
    fun cancel()
}

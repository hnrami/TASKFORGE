package com.taskforge.handler.impl

import com.taskforge.handler.TaskHandler
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus

class ApprovalTaskHandler : TaskHandler {
    override fun type(): String = "approval"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult =
        TaskResult(TaskStatus.WAITING_APPROVAL, message = "Waiting for approval")

    override fun cancel() = Unit
}

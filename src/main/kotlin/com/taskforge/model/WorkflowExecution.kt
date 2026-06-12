package com.taskforge.model

import java.time.Instant

data class WorkflowExecution(
    val id: String,
    val definitionId: String,
    val status: ExecutionStatus,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val tasks: List<TaskExecution> = emptyList()
)

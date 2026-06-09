package com.taskforge.model

import java.time.Instant

data class TaskExecution(
    val id: String,
    val taskDefinitionId: String,
    val status: TaskStatus,
    val attempt: Int = 0,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val result: TaskResult? = null
)

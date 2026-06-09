package com.taskforge.model

data class TaskResult(
    val status: TaskStatus,
    val output: Map<String, Any?> = emptyMap(),
    val message: String? = null
)

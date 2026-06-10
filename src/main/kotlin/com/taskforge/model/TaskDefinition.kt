package com.taskforge.model

data class TaskDefinition(
    val id: String,
    val type: String,
    val name: String? = null,
    val config: Map<String, Any?> = emptyMap(),
    val dependsOn: List<String> = emptyList(),
    val retryPolicy: RetryPolicy? = null
)

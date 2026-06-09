package com.taskforge.model

data class WorkflowDefinition(
    val id: String,
    val name: String,
    val tasks: List<TaskDefinition>
)

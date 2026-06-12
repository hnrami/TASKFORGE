package com.taskforge.model
import java.util.UUID

data class WorkflowDefinition(

    val id: String = UUID.randomUUID().toString(),

    val name: String,

    val tasks: List<TaskDefinition> = emptyList()
)

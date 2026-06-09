package com.taskforge.model

abstract class TaskDefinition(
    open val id: String,
    open val type: String,
    open val name: String? = null
)

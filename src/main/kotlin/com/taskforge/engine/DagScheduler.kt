package com.taskforge.engine

import com.taskforge.model.WorkflowDefinition

interface DagScheduler {
    fun findReadyTasks(definition: WorkflowDefinition): List<String>
}

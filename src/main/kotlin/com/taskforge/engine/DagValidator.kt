package com.taskforge.engine

import com.taskforge.model.WorkflowDefinition

interface DagValidator {
    fun validate(definition: WorkflowDefinition)
}

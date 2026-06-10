package com.taskforge.engine

import com.taskforge.model.WorkflowExecution

interface StateManager {
    fun updateWorkflowState(execution: WorkflowExecution)
}

package com.taskforge.service

import com.taskforge.model.WorkflowDefinition

interface WorkflowService {
    fun createWorkflow(definition: WorkflowDefinition)
    fun getWorkflow(id: String): WorkflowDefinition?
}

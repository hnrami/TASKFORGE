package com.taskforge.service

import com.taskforge.model.WorkflowExecution

interface ExecutionService {
    fun startExecution(workflowId: String): WorkflowExecution
    fun getExecution(id: String): WorkflowExecution?
}

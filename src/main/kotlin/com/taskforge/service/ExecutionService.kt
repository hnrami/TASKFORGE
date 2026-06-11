package com.taskforge.service

import com.taskforge.model.WorkflowExecution

interface ExecutionService {
    fun startExecution(workflowId: String): WorkflowExecution
    fun getExecution(id: String): WorkflowExecution?
    fun cancelExecution(id: String): WorkflowExecution
    fun resolveApproval(id: String, taskId: String, approved: Boolean): WorkflowExecution
}

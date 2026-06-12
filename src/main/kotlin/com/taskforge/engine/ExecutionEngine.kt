package com.taskforge.engine

import com.taskforge.model.WorkflowDefinition
import com.taskforge.model.WorkflowExecution

/**
 * Execution engine contract. Implementations must not depend on concrete task types.
 */
interface ExecutionEngine {
    fun startWorkflow(definition: WorkflowDefinition): WorkflowExecution
    fun cancel(executionId: String): WorkflowExecution
    fun resolveApproval(executionId: String, taskId: String, approved: Boolean): WorkflowExecution
}

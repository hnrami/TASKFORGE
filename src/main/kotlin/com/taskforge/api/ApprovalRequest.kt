package com.taskforge.api

data class ApprovalRequest(
    val taskId: String,
    val approved: Boolean,
    val approver: String? = null
)

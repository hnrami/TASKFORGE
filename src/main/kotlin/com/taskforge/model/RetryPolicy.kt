package com.taskforge.model

data class RetryPolicy(
    val maxAttempts: Int = 0,
    val backoffMillis: Long = 0L
)

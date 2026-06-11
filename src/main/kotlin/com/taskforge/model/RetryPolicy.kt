package com.taskforge.model

data class RetryPolicy(
    val maxAttempts: Int = 1,
    val backoffMillis: Long = 0L
)

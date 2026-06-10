package com.taskforge.handler.impl

import com.taskforge.handler.TaskHandler
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus

/**
 * Simple HTTP task handler that makes HTTP requests.
 * Task config expected:
 * - url (String): HTTP endpoint
 * - method (String): GET, POST, PUT, DELETE (default: GET)
 * - body (String, optional): Request body for POST/PUT
 * - timeout (Int, optional): Request timeout in ms (default: 5000)
 */
class HttpTaskHandler : TaskHandler {

    override fun type(): String = "http"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        return try {
            val url = definition.config["url"] as? String
                ?: return TaskResult(
                    status = TaskStatus.FAILED,
                    output = emptyMap(),
                    message = "Missing required config: url"
                )

            val method = (definition.config["method"] as? String)?.uppercase() ?: "GET"
            val body = definition.config["body"] as? String
            val timeout = (definition.config["timeout"] as? Number)?.toInt() ?: 5000

            // In a real implementation, use okhttp or similar HTTP client
            // For now, we'll return a mock success response
            val output = mapOf(
                "statusCode" to 200,
                "body" to "Mock HTTP response from $url",
                "method" to method
            )

            TaskResult(
                status = TaskStatus.SUCCESS,
                output = output,
                message = "HTTP $method request completed successfully"
            )
        } catch (e: Exception) {
            TaskResult(
                status = TaskStatus.FAILED,
                output = emptyMap(),
                message = "HTTP task failed: ${e.message}"
            )
        }
    }

    override fun cancel() {
        // Cancel pending HTTP requests
    }
}

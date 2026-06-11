package com.taskforge.handler.impl

import com.taskforge.handler.TaskHandler
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class HttpTaskHandler : TaskHandler {
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    private val requests = ConcurrentHashMap.newKeySet<CompletableFuture<*>>()

    override fun type(): String = "http"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        val url = definition.config["url"] as? String
            ?: return TaskResult(TaskStatus.FAILED, message = "Missing required config: url")
        val method = (definition.config["method"] as? String)?.uppercase() ?: "GET"
        val body = definition.config["body"]?.toString()
        val timeoutMillis = (definition.config["timeoutMillis"] as? Number)?.toLong() ?: 5_000
        val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(timeoutMillis))
        @Suppress("UNCHECKED_CAST")
        (definition.config["headers"] as? Map<String, Any?>)?.forEach { (key, value) ->
            builder.header(key, value.toString())
        }
        val publisher = if (body == null) HttpRequest.BodyPublishers.noBody()
        else HttpRequest.BodyPublishers.ofString(body)
        val request = builder.method(method, publisher).build()
        val future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        requests.add(future)
        return try {
            val response = future.join()
            val output = mapOf("statusCode" to response.statusCode(), "responseBody" to response.body())
            when (response.statusCode()) {
                in 200..299 -> TaskResult(TaskStatus.SUCCESS, output)
                in 500..599 -> TaskResult(
                    TaskStatus.FAILED,
                    output,
                    "HTTP ${response.statusCode()}",
                    retryable = true
                )
                else -> TaskResult(TaskStatus.FAILED, output, "HTTP ${response.statusCode()}")
            }
        } catch (exception: Exception) {
            TaskResult(TaskStatus.FAILED, message = "HTTP request failed: ${exception.message}", retryable = true)
        } finally {
            requests.remove(future)
        }
    }

    override fun cancel() {
        requests.forEach { it.cancel(true) }
    }
}

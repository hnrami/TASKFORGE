package com.taskforge.api

import com.taskforge.service.ExecutionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/executions")
class ExecutionController(private val executionService: ExecutionService) {

    @PostMapping("/workflows/{workflowId}")
    fun startExecution(@PathVariable workflowId: String): ResponseEntity<Any> {
        return try {
            val execution = executionService.startExecution(workflowId)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "id" to execution.id,
                    "definitionId" to execution.definitionId,
                    "status" to execution.status.name,
                    "message" to "Execution started successfully"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                mapOf("error" to (e.message ?: "Failed to start execution"))
            )
        }
    }

    @GetMapping("/{id}")
    fun getExecution(@PathVariable id: String): ResponseEntity<Any> {
        val execution = executionService.getExecution(id)
        return if (execution != null) {
            ResponseEntity.ok(
                mapOf(
                    "id" to execution.id,
                    "definitionId" to execution.definitionId,
                    "status" to execution.status.name,
                    "startedAt" to (execution.startedAt?.toString() ?: "Not started"),
                    "finishedAt" to (execution.finishedAt?.toString() ?: "Not finished"),
                    "taskCount" to execution.tasks.size
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to "Execution not found: $id")
            )
        }
    }

    @GetMapping("/{id}/details")
    fun getExecutionDetails(@PathVariable id: String): ResponseEntity<Any> {
        val execution = executionService.getExecution(id)
        return if (execution != null) {
            ResponseEntity.ok(execution)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to "Execution not found: $id")
            )
        }
    }
}

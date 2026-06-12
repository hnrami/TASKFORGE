package com.taskforge.api

import com.taskforge.model.WorkflowDefinition
import com.taskforge.service.WorkflowService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workflows")
class WorkflowController(private val workflowService: WorkflowService) {

    @PostMapping
    fun createWorkflow(@RequestBody definition: WorkflowDefinition): ResponseEntity<Map<String, String>> {
        return try {
            workflowService.createWorkflow(definition)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf("id" to definition.id, "message" to "Workflow created successfully")
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                mapOf("error" to (e.message ?: "Failed to create workflow"))
            )
        }
    }

    @GetMapping("/{id}")
    fun getWorkflow(@PathVariable id: String): ResponseEntity<Any> {
        val workflow = workflowService.getWorkflow(id)
        return if (workflow != null) {
            ResponseEntity.ok(workflow)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("error" to "Workflow not found: $id")
            )
        }
    }

    @GetMapping
    fun listWorkflows(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf("message" to "Workflow list endpoint (not yet implemented)")
        )
    }
}

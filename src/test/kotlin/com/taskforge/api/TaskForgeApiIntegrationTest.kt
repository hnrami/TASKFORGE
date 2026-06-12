package com.taskforge.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskforge.model.TaskStatus
import com.taskforge.service.ExecutionService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
class TaskForgeApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val executionService: ExecutionService
) {
    @Test
    fun `workflow APIs create validate and retrieve`() {
        val id = "api-workflow-${System.nanoTime()}"
        val valid = """{"id":"$id","name":"API workflow","tasks":[{"id":"notify","type":"notification","config":{"channel":"email","recipient":"dev@example.com","message":"done"}}]}"""
        mockMvc.post("/api/workflows") {
            contentType = MediaType.APPLICATION_JSON
            content = valid
        }.andExpect { status { isCreated() } }
        mockMvc.get("/api/workflows/$id").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id) }
        }

        val duplicate = """{"id":"duplicate-${System.nanoTime()}","name":"invalid","tasks":[{"id":"same","type":"notification"},{"id":"same","type":"notification"}]}"""
        mockMvc.post("/api/workflows") {
            contentType = MediaType.APPLICATION_JSON
            content = duplicate
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Duplicate task id: same") }
        }
    }

    @Test
    fun `execution approval and cancellation APIs work`() {
        val approvalWorkflowId = "approval-${System.nanoTime()}"
        createWorkflow("""{"id":"$approvalWorkflowId","name":"approval","tasks":[{"id":"gate","type":"approval"},{"id":"notify","type":"notification","dependsOn":["gate"],"config":{"channel":"email","recipient":"dev@example.com","message":"approved"}}]}""")
        val approvalExecutionId = startExecution(approvalWorkflowId)
        awaitTask(approvalExecutionId, "gate", TaskStatus.WAITING_APPROVAL)
        mockMvc.post("/api/executions/$approvalExecutionId/approval") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"taskId":"gate","approved":true,"approver":"tester"}"""
        }.andExpect { status { isOk() } }

        val cancelWorkflowId = "cancel-${System.nanoTime()}"
        val command = if (System.getProperty("os.name").lowercase().contains("windows")) "Start-Sleep -Seconds 10" else "sleep 10"
        createWorkflow("""{"id":"$cancelWorkflowId","name":"cancel","tasks":[{"id":"long","type":"script","config":{"command":"$command"}}]}""")
        val cancelExecutionId = startExecution(cancelWorkflowId)
        mockMvc.post("/api/executions/$cancelExecutionId/cancel").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
        }
    }

    private fun createWorkflow(json: String) {
        mockMvc.post("/api/workflows") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect { status { isCreated() } }
    }

    private fun startExecution(workflowId: String): String {
        val response = mockMvc.post("/api/workflows/$workflowId/executions")
            .andExpect { status { isCreated() } }
            .andReturn().response.contentAsString
        return objectMapper.readTree(response)["id"].asText()
    }

    private fun awaitTask(executionId: String, taskId: String, status: TaskStatus) {
        repeat(150) {
            val task = executionService.getExecution(executionId)?.tasks?.first { it.taskDefinitionId == taskId }
            if (task?.status == status) return
            Thread.sleep(20)
        }
        assertEquals(status, executionService.getExecution(executionId)?.tasks?.first { it.taskDefinitionId == taskId }?.status)
    }
}

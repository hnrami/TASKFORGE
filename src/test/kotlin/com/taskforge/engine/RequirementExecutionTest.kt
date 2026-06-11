package com.taskforge.engine

import com.taskforge.handler.TaskHandler
import com.taskforge.handler.TaskHandlerRegistry
import com.taskforge.handler.impl.ApprovalTaskHandler
import com.taskforge.model.ExecutionStatus
import com.taskforge.model.RetryPolicy
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus
import com.taskforge.model.WorkflowDefinition
import com.taskforge.model.WorkflowExecution
import com.taskforge.repository.impl.InMemoryExecutionRepository
import com.taskforge.repository.impl.InMemoryWorkflowRepository
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RequirementExecutionTest {
    @Test
    fun `multiple root tasks execute concurrently`() {
        val handler = ConfigurableHandler()
        val fixture = fixture(handler)
        val started = System.currentTimeMillis()
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "configurable", config = mapOf("delay" to 300)),
            TaskDefinition("B", "configurable", config = mapOf("delay" to 300)),
            TaskDefinition("C", "configurable", config = mapOf("delay" to 300))
        ))

        val final = fixture.await(execution.id)
        val elapsed = System.currentTimeMillis() - started
        assertEquals(ExecutionStatus.SUCCESS, final.status)
        assertTrue(elapsed < 750, "Expected concurrent execution, elapsed=${elapsed}ms")
    }

    @Test
    fun `failure skips downstream tasks`() {
        val fixture = fixture(ConfigurableHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "configurable"),
            TaskDefinition("B", "configurable", config = mapOf("fail" to true), dependsOn = listOf("A")),
            TaskDefinition("C", "configurable", dependsOn = listOf("B"))
        ))

        val final = fixture.await(execution.id)
        assertEquals(ExecutionStatus.FAILED, final.status)
        assertEquals(TaskStatus.SUCCESS, final.task("A").status)
        assertEquals(TaskStatus.FAILED, final.task("B").status)
        assertEquals(TaskStatus.SKIPPED, final.task("C").status)
    }

    @Test
    fun `retry eventually succeeds and tracks attempts`() {
        val handler = RetryHandler(failuresBeforeSuccess = 1)
        val fixture = fixture(handler)
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "retry", retryPolicy = RetryPolicy(maxAttempts = 3))
        ))

        val final = fixture.await(execution.id)
        assertEquals(TaskStatus.SUCCESS, final.task("A").status)
        assertEquals(2, final.task("A").attempt)
    }

    @Test
    fun `retry exhaustion fails task and skips downstream`() {
        val fixture = fixture(RetryHandler(failuresBeforeSuccess = 10), ConfigurableHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "retry", retryPolicy = RetryPolicy(maxAttempts = 2)),
            TaskDefinition("B", "configurable", dependsOn = listOf("A"))
        ))

        val final = fixture.await(execution.id)
        assertEquals(TaskStatus.FAILED, final.task("A").status)
        assertEquals(2, final.task("A").attempt)
        assertEquals(TaskStatus.SKIPPED, final.task("B").status)
    }

    @Test
    fun `timeout fails task and skips downstream`() {
        val fixture = fixture(ConfigurableHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "configurable", config = mapOf("delay" to 2_000), timeoutSeconds = 1),
            TaskDefinition("B", "configurable", dependsOn = listOf("A"))
        ))

        val final = fixture.await(execution.id, 4_000)
        assertEquals(TaskStatus.FAILED, final.task("A").status)
        assertTrue(final.task("A").result?.message.orEmpty().contains("timed out"))
        assertEquals(TaskStatus.SKIPPED, final.task("B").status)
    }

    @Test
    fun `task output is resolved in downstream config`() {
        val consumer = CapturingHandler()
        val fixture = fixture(OutputHandler(), consumer)
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "output"),
            TaskDefinition(
                "B",
                "capture",
                config = mapOf("value" to "{{A.version}}"),
                dependsOn = listOf("A")
            )
        ))

        fixture.await(execution.id)
        assertEquals("1.0", consumer.values["B"])
    }

    @Test
    fun `missing output reference fails task clearly`() {
        val fixture = fixture(CapturingHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "capture", config = mapOf("value" to "{{unknown.value}}"))
        ))

        val final = fixture.await(execution.id)
        assertEquals(TaskStatus.FAILED, final.task("A").status)
        assertTrue(final.task("A").result?.message.orEmpty().contains("Unknown output reference"))
    }

    @Test
    fun `false condition skips task`() {
        val fixture = fixture(OutputHandler(), ConfigurableHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("A", "output"),
            TaskDefinition("B", "configurable", dependsOn = listOf("A"), condition = "{{A.enabled}} == true"),
            TaskDefinition("C", "configurable", dependsOn = listOf("A"), condition = "{{A.enabled}} == false")
        ))

        val final = fixture.await(execution.id)
        assertEquals(TaskStatus.SKIPPED, final.task("B").status)
        assertEquals(TaskStatus.SUCCESS, final.task("C").status)
    }

    @Test
    fun `approval accepted continues workflow`() {
        val fixture = fixture(ApprovalTaskHandler(), ConfigurableHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("approval", "approval"),
            TaskDefinition("after", "configurable", dependsOn = listOf("approval"))
        ))
        fixture.awaitStatus(execution.id, "approval", TaskStatus.WAITING_APPROVAL)

        fixture.engine.resolveApproval(execution.id, "approval", true)
        val final = fixture.await(execution.id)
        assertEquals(ExecutionStatus.SUCCESS, final.status)
        assertEquals(TaskStatus.SUCCESS, final.task("after").status)
    }

    @Test
    fun `approval rejected fails workflow and skips downstream`() {
        val fixture = fixture(ApprovalTaskHandler(), ConfigurableHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("approval", "approval"),
            TaskDefinition("after", "configurable", dependsOn = listOf("approval"))
        ))
        fixture.awaitStatus(execution.id, "approval", TaskStatus.WAITING_APPROVAL)

        fixture.engine.resolveApproval(execution.id, "approval", false)
        val final = fixture.await(execution.id)
        assertEquals(ExecutionStatus.FAILED, final.status)
        assertEquals(TaskStatus.SKIPPED, final.task("after").status)
    }

    @Test
    fun `approval timeout fails workflow`() {
        val fixture = fixture(ApprovalTaskHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("approval", "approval", timeoutSeconds = 1)
        ))

        val final = fixture.await(execution.id, 3_000)
        assertEquals(ExecutionStatus.FAILED, final.status)
        assertTrue(final.task("approval").result?.message.orEmpty().contains("Approval timed out"))
    }

    @Test
    fun `cancellation marks pending and running tasks cancelled`() {
        val fixture = fixture(ConfigurableHandler())
        val execution = fixture.engine.startWorkflow(workflow(
            TaskDefinition("running", "configurable", config = mapOf("delay" to 2_000)),
            TaskDefinition("pending", "configurable", dependsOn = listOf("running"))
        ))
        fixture.awaitStatus(execution.id, "running", TaskStatus.RUNNING)

        val cancelled = fixture.engine.cancel(execution.id)
        assertEquals(ExecutionStatus.CANCELLED, cancelled.status)
        assertEquals(TaskStatus.CANCELLED, cancelled.task("running").status)
        assertEquals(TaskStatus.CANCELLED, cancelled.task("pending").status)
    }

    private fun fixture(vararg handlers: TaskHandler): Fixture {
        val registry = TaskHandlerRegistry()
        handlers.forEach(registry::register)
        val executions = InMemoryExecutionRepository()
        val workflows = InMemoryWorkflowRepository()
        return Fixture(
            ExecutionEngineImpl(
                DagValidatorImpl(registry),
                DagSchedulerImpl(),
                StateManagerImpl(),
                registry,
                workflows,
                executions
            ),
            executions
        )
    }

    private fun workflow(vararg tasks: TaskDefinition) =
        WorkflowDefinition("workflow-${System.nanoTime()}", "test", tasks.toList())

    private data class Fixture(
        val engine: ExecutionEngine,
        val executions: InMemoryExecutionRepository
    ) {
        fun await(id: String, timeoutMillis: Long = 3_000): WorkflowExecution {
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                val execution = executions.findById(id)!!
                if (execution.status in setOf(ExecutionStatus.SUCCESS, ExecutionStatus.FAILED, ExecutionStatus.CANCELLED)) {
                    return execution
                }
                Thread.sleep(20)
            }
            error("Execution did not finish: $id")
        }

        fun awaitStatus(id: String, taskId: String, status: TaskStatus) {
            repeat(150) {
                if (executions.findById(id)?.tasks?.firstOrNull { task -> task.taskDefinitionId == taskId }?.status == status) return
                Thread.sleep(20)
            }
            error("Task $taskId did not reach $status")
        }
    }

    private class ConfigurableHandler : TaskHandler {
        override fun type() = "configurable"
        override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
            Thread.sleep((definition.config["delay"] as? Number)?.toLong() ?: 0)
            return if (definition.config["fail"] == true) {
                TaskResult(TaskStatus.FAILED, message = "configured failure")
            } else TaskResult(TaskStatus.SUCCESS)
        }
        override fun cancel() = Unit
    }

    private class RetryHandler(private val failuresBeforeSuccess: Int) : TaskHandler {
        private val attempts = AtomicInteger()
        override fun type() = "retry"
        override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult =
            if (attempts.incrementAndGet() <= failuresBeforeSuccess) {
                TaskResult(TaskStatus.FAILED, retryable = true)
            } else TaskResult(TaskStatus.SUCCESS)
        override fun cancel() = Unit
    }

    private class OutputHandler : TaskHandler {
        override fun type() = "output"
        override fun execute(definition: TaskDefinition, context: TaskContext) =
            TaskResult(TaskStatus.SUCCESS, mapOf("version" to "1.0", "enabled" to false))
        override fun cancel() = Unit
    }

    private class CapturingHandler : TaskHandler {
        val values = ConcurrentHashMap<String, Any?>()
        override fun type() = "capture"
        override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
            values[definition.id] = definition.config["value"]
            return TaskResult(TaskStatus.SUCCESS)
        }
        override fun cancel() = Unit
    }

    private fun WorkflowExecution.task(id: String) = tasks.first { it.taskDefinitionId == id }
}

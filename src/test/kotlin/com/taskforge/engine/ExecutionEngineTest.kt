package com.taskforge.engine

import com.taskforge.handler.TaskHandler
import com.taskforge.handler.TaskHandlerRegistry
import com.taskforge.model.*
import com.taskforge.repository.impl.InMemoryWorkflowRepository
import com.taskforge.repository.impl.InMemoryExecutionRepository
import com.taskforge.exception.TaskForgeException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

@DisplayName("ExecutionEngine Integration Tests")
class ExecutionEngineTest {

    private lateinit var registry: TaskHandlerRegistry
    private lateinit var dagValidator: DagValidator
    private lateinit var dagScheduler: DagSchedulerImpl
    private lateinit var stateManager: StateManager
    private lateinit var workflowRepository: InMemoryWorkflowRepository
    private lateinit var executionRepository: InMemoryExecutionRepository
    private lateinit var engine: ExecutionEngine

    @BeforeEach
    fun setup() {
        registry = TaskHandlerRegistry()
        workflowRepository = InMemoryWorkflowRepository()
        executionRepository = InMemoryExecutionRepository()

        // Register mock task handler
        registry.register(MockSuccessHandler())

        dagValidator = DagValidatorImpl(registry)
        dagScheduler = DagSchedulerImpl()
        stateManager = StateManagerImpl()

        engine = ExecutionEngineImpl(
            dagValidator = dagValidator,
            dagScheduler = dagScheduler,
            stateManager = stateManager,
            handlerRegistry = registry,
            workflowRepository = workflowRepository,
            executionRepository = executionRepository
        )
    }

    @Test
    @DisplayName("should execute simple single-task workflow")
    fun testExecuteSingleTaskWorkflow() {
        val definition = WorkflowDefinition(
            id = "wf1",
            name = "Simple Workflow",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock-success",
                    name = "Task 1",
                    config = mapOf("delay" to 100),
                    dependsOn = emptyList()
                )
            )
        )

        val execution = engine.startWorkflow(definition)

        assertNotNull(execution.id)
        assertEquals(definition.id, execution.definitionId)
        
        // Give execution time to complete
        Thread.sleep(500)
        
        val finalExecution = executionRepository.findById(execution.id)
        assertNotNull(finalExecution)
        assertEquals(ExecutionStatus.SUCCESS, finalExecution.status)
        assertEquals(1, finalExecution.tasks.size)
    }

    @Test
    @DisplayName("should execute two-task workflow with dependency")
    fun testExecuteWorkflowWithDependency() {
        val definition = WorkflowDefinition(
            id = "wf2",
            name = "Dependent Tasks Workflow",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock-success",
                    name = "Task 1",
                    config = mapOf("delay" to 100),
                    dependsOn = emptyList()
                ),
                TaskDefinition(
                    id = "task2",
                    type = "mock-success",
                    name = "Task 2 (depends on task1)",
                    config = mapOf("delay" to 100),
                    dependsOn = listOf("task1")
                )
            )
        )

        val execution = engine.startWorkflow(definition)
        
        // Give execution time to complete
        Thread.sleep(1000)
        
        val finalExecution = executionRepository.findById(execution.id)
        assertNotNull(finalExecution)
        assertEquals(ExecutionStatus.SUCCESS, finalExecution.status)
        assertEquals(2, finalExecution.tasks.size)
        
        // Verify task1 executed before task2
        val task1Execution = finalExecution.tasks.find { it.taskDefinitionId == "task1" }
        val task2Execution = finalExecution.tasks.find { it.taskDefinitionId == "task2" }
        
        assertNotNull(task1Execution)
        assertNotNull(task2Execution)
        assertEquals(TaskStatus.SUCCESS, task1Execution.status)
        assertEquals(TaskStatus.SUCCESS, task2Execution.status)
    }

    @Test
    @DisplayName("should fail workflow when unregistered task type is encountered")
    fun testFailWhenTaskTypeNotRegistered() {
        val definition = WorkflowDefinition(
            id = "wf3",
            name = "Unknown Task Type Workflow",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "unknown-type",
                    name = "Unknown Task",
                    config = emptyMap(),
                    dependsOn = emptyList()
                )
            )
        )

        assertFailsWith<TaskForgeException> {
            engine.startWorkflow(definition)
        }
    }

    @Test
    @DisplayName("should detect cycles in workflow")
    fun testDetectCyclesInWorkflow() {
        val definition = WorkflowDefinition(
            id = "wf4",
            name = "Cyclic Workflow",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock-success",
                    name = "Task 1",
                    config = emptyMap(),
                    dependsOn = listOf("task2")
                ),
                TaskDefinition(
                    id = "task2",
                    type = "mock-success",
                    name = "Task 2",
                    config = emptyMap(),
                    dependsOn = listOf("task1")
                )
            )
        )

        assertFailsWith<TaskForgeException> {
            engine.startWorkflow(definition)
        }
    }
}

// Mock task handler for testing
class MockSuccessHandler : TaskHandler {
    override fun type(): String = "mock-success"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        val delay = (definition.config["delay"] as? Number)?.toLong() ?: 0
        if (delay > 0) {
            Thread.sleep(delay)
        }
        
        return TaskResult(
            status = TaskStatus.SUCCESS,
            output = mapOf(
                "taskId" to definition.id,
                "timestamp" to System.currentTimeMillis()
            ),
            message = "Mock task completed successfully"
        )
    }

    override fun cancel() {
        // No-op for mock
    }
}

package com.taskforge.engine

import com.taskforge.exception.TaskForgeException
import com.taskforge.handler.TaskHandler
import com.taskforge.handler.TaskHandlerRegistry
import com.taskforge.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@DisplayName("DAG Validator Tests")
class DagValidatorTest {

    private lateinit var registry: TaskHandlerRegistry
    private lateinit var validator: DagValidator

    @BeforeEach
    fun setup() {
        registry = TaskHandlerRegistry()
        validator = DagValidatorImpl(registry)

        // Register handlers
        registry.register(MockHandler())
    }

    @Test
    @DisplayName("should accept valid workflow with no dependencies")
    fun testValidateSimpleWorkflow() {
        val definition = WorkflowDefinition(
            id = "wf1",
            name = "Simple Workflow",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock",
                    name = "Task 1",
                    config = emptyMap(),
                    dependsOn = emptyList()
                )
            )
        )

        validator.validate(definition) // Should not throw
    }

    @Test
    @DisplayName("should accept valid workflow with dependencies")
    fun testValidateWorkflowWithDependencies() {
        val definition = WorkflowDefinition(
            id = "wf2",
            name = "Workflow with Dependencies",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock",
                    name = "Task 1",
                    config = emptyMap(),
                    dependsOn = emptyList()
                ),
                TaskDefinition(
                    id = "task2",
                    type = "mock",
                    name = "Task 2",
                    config = emptyMap(),
                    dependsOn = listOf("task1")
                )
            )
        )

        validator.validate(definition) // Should not throw
    }

    @Test
    @DisplayName("should reject workflow with invalid dependency")
    fun testValidateWorkflowWithInvalidDependency() {
        val definition = WorkflowDefinition(
            id = "wf3",
            name = "Workflow with Invalid Dependency",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock",
                    name = "Task 1",
                    config = emptyMap(),
                    dependsOn = listOf("nonexistent")
                )
            )
        )

        assertFailsWith<TaskForgeException> {
            validator.validate(definition)
        }
    }

    @Test
    @DisplayName("should reject workflow with cycle")
    fun testValidateWorkflowWithCycle() {
        val definition = WorkflowDefinition(
            id = "wf4",
            name = "Workflow with Cycle",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock",
                    name = "Task 1",
                    config = emptyMap(),
                    dependsOn = listOf("task2")
                ),
                TaskDefinition(
                    id = "task2",
                    type = "mock",
                    name = "Task 2",
                    config = emptyMap(),
                    dependsOn = listOf("task1")
                )
            )
        )

        assertFailsWith<TaskForgeException> {
            validator.validate(definition)
        }
    }

    @Test
    @DisplayName("should reject workflow with unregistered task type")
    fun testValidateWorkflowWithUnregisteredType() {
        val definition = WorkflowDefinition(
            id = "wf5",
            name = "Workflow with Unknown Type",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "unknown",
                    name = "Unknown Task",
                    config = emptyMap(),
                    dependsOn = emptyList()
                )
            )
        )

        assertFailsWith<TaskForgeException> {
            validator.validate(definition)
        }
    }

    @Test
    @DisplayName("should accept empty workflow")
    fun testValidateEmptyWorkflow() {
        val definition = WorkflowDefinition(
            id = "wf6",
            name = "Empty Workflow",
            tasks = emptyList()
        )

        validator.validate(definition) // Should not throw
    }

    @Test
    @DisplayName("should reject duplicate task ids with clear error")
    fun testDuplicateTaskIds() {
        val definition = WorkflowDefinition(
            id = "duplicate",
            name = "Duplicate",
            tasks = listOf(
                TaskDefinition("same", "mock"),
                TaskDefinition("same", "mock")
            )
        )

        val error = assertFailsWith<TaskForgeException> { validator.validate(definition) }
        kotlin.test.assertTrue(error.message.orEmpty().contains("Duplicate task id: same"))
    }
}

@DisplayName("DAG Scheduler Tests")
class DagSchedulerTest {

    private lateinit var scheduler: DagSchedulerImpl

    @BeforeEach
    fun setup() {
        scheduler = DagSchedulerImpl()
    }

    @Test
    @DisplayName("should find all tasks in independent workflow")
    fun testFindReadyTasksIndependent() {
        val definition = WorkflowDefinition(
            id = "wf1",
            name = "Independent Tasks",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock",
                    name = "Task 1",
                    config = emptyMap(),
                    dependsOn = emptyList()
                ),
                TaskDefinition(
                    id = "task2",
                    type = "mock",
                    name = "Task 2",
                    config = emptyMap(),
                    dependsOn = emptyList()
                )
            )
        )

        val ready = scheduler.findReadyTasks(definition)
        assert(ready.size == 2)
        assert(ready.contains("task1"))
        assert(ready.contains("task2"))
    }

    @Test
    @DisplayName("should find only tasks with satisfied dependencies")
    fun testFindReadyTasksWithDependencies() {
        val definition = WorkflowDefinition(
            id = "wf2",
            name = "Dependent Tasks",
            tasks = listOf(
                TaskDefinition(
                    id = "task1",
                    type = "mock",
                    name = "Task 1",
                    config = emptyMap(),
                    dependsOn = emptyList()
                ),
                TaskDefinition(
                    id = "task2",
                    type = "mock",
                    name = "Task 2",
                    config = emptyMap(),
                    dependsOn = listOf("task1")
                )
            )
        )

        val ready = scheduler.findReadyTasks(definition)
        assert(ready.size == 1)
        assert(ready.contains("task1"))
        assert(!ready.contains("task2"))
    }
}

class MockHandler : TaskHandler {
    override fun type(): String = "mock"
    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        return TaskResult(TaskStatus.SUCCESS, emptyMap())
    }
    override fun cancel() {}
}

package com.taskforge.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("TaskContext Tests")
class TaskContextTest {

    @Test
    @DisplayName("should set and get string values")
    fun testSetAndGetString() {
        val context = TaskContext()
        context.set("key1", "value1")
        
        val retrieved: String? = context.get("key1")
        assertEquals("value1", retrieved)
    }

    @Test
    @DisplayName("should set and get integer values")
    fun testSetAndGetInt() {
        val context = TaskContext()
        context.set("number", 42)
        
        val retrieved: Int? = context.get("number")
        assertEquals(42, retrieved)
    }

    @Test
    @DisplayName("should set and get map values")
    fun testSetAndGetMap() {
        val context = TaskContext()
        val mapValue = mapOf("nested" to "value")
        context.set("map", mapValue)
        
        val retrieved: Map<String, Any?>? = context.get("map")
        assertNotNull(retrieved)
        assertEquals("value", retrieved["nested"])
    }

    @Test
    @DisplayName("should return null for unset keys")
    fun testGetUnsetKey() {
        val context = TaskContext()
        val retrieved: String? = context.get("nonexistent")
        
        kotlin.test.assertNull(retrieved)
    }

    @Test
    @DisplayName("should allow overwriting values")
    fun testOverwrite() {
        val context = TaskContext()
        context.set("key", "value1")
        context.set("key", "value2")
        
        val retrieved: String? = context.get("key")
        assertEquals("value2", retrieved)
    }
}

@DisplayName("TaskResult Tests")
class TaskResultTest {

    @Test
    @DisplayName("should create successful task result")
    fun testSuccessResult() {
        val result = TaskResult(TaskStatus.SUCCESS)
        
        assertEquals(TaskStatus.SUCCESS, result.status)
        assertTrue(result.output.isEmpty())
    }

    @Test
    @DisplayName("should create failed task result with message")
    fun testFailedResult() {
        val result = TaskResult(TaskStatus.FAILED, message = "Task failed")
        
        assertEquals(TaskStatus.FAILED, result.status)
        assertEquals("Task failed", result.message)
    }

    @Test
    @DisplayName("should include output in task result")
    fun testResultWithOutput() {
        val output = mapOf("status" to "200", "body" to "OK")
        val result = TaskResult(TaskStatus.SUCCESS, output = output)
        
        assertEquals(output, result.output)
        assertEquals("200", result.output["status"])
    }
}

@DisplayName("TaskDefinition Tests")
class TaskDefinitionTest {

    @Test
    @DisplayName("should create task with minimal properties")
    fun testMinimalTask() {
        val task = TaskDefinition(
            id = "task1",
            type = "http"
        )
        
        assertEquals("task1", task.id)
        assertEquals("http", task.type)
        assertTrue(task.dependsOn.isEmpty())
        assertTrue(task.config.isEmpty())
    }

    @Test
    @DisplayName("should create task with dependencies")
    fun testTaskWithDependencies() {
        val task = TaskDefinition(
            id = "task2",
            type = "script",
            dependsOn = listOf("task1")
        )
        
        assertEquals(listOf("task1"), task.dependsOn)
    }

    @Test
    @DisplayName("should create task with config")
    fun testTaskWithConfig() {
        val config = mapOf("url" to "http://example.com", "timeout" to 5000)
        val task = TaskDefinition(
            id = "http-task",
            type = "http",
            config = config
        )
        
        assertEquals(config, task.config)
    }
}

@DisplayName("WorkflowDefinition Tests")
class WorkflowDefinitionTest {

    @Test
    @DisplayName("should create workflow with multiple tasks")
    fun testWorkflowCreation() {
        val task1 = TaskDefinition("t1", "script")
        val task2 = TaskDefinition("t2", "http", dependsOn = listOf("t1"))
        
        val workflow = WorkflowDefinition(
            id = "wf1",
            name = "Deploy",
            tasks = listOf(task1, task2)
        )
        
        assertEquals("wf1", workflow.id)
        assertEquals("Deploy", workflow.name)
        assertEquals(2, workflow.tasks.size)
    }

    @Test
    @DisplayName("should handle empty workflow")
    fun testEmptyWorkflow() {
        val workflow = WorkflowDefinition(
            id = "empty",
            name = "Empty",
            tasks = emptyList()
        )
        
        assertTrue(workflow.tasks.isEmpty())
    }
}

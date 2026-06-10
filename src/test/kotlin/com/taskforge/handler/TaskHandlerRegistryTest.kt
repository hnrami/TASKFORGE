package com.taskforge.handler

import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

@DisplayName("TaskHandlerRegistry Tests")
class TaskHandlerRegistryTest {

    private val registry = TaskHandlerRegistry()

    @Test
    @DisplayName("should register and retrieve a handler by type")
    fun testRegisterAndRetrieveHandler() {
        val mockHandler = MockTaskHandler()
        
        registry.register(mockHandler)
        
        val retrieved = registry.getHandler("mock")
        assertNotNull(retrieved)
        assertEquals("mock", retrieved.type())
    }

    @Test
    @DisplayName("should return null for unregistered handler type")
    fun testGetUnregisteredHandler() {
        val handler = registry.getHandler("unknown")
        assertNull(handler)
    }

    @Test
    @DisplayName("should allow multiple handlers with different types")
    fun testMultipleHandlers() {
        val mockHandler1 = MockTaskHandler()
        val mockHandler2 = MockTaskHandler("http")
        
        registry.register(mockHandler1)
        registry.register(mockHandler2)
        
        assertNotNull(registry.getHandler("mock"))
        assertNotNull(registry.getHandler("http"))
    }

    @Test
    @DisplayName("should override handler when registering same type twice")
    fun testHandlerOverride() {
        val handler1 = MockTaskHandler()
        val handler2 = MockTaskHandler()
        
        registry.register(handler1)
        registry.register(handler2)
        
        val retrieved = registry.getHandler("mock")
        assertNotNull(retrieved)
        // Should be the second handler
        assertEquals(handler2, retrieved)
    }

    private class MockTaskHandler(private val handlerType: String = "mock") : TaskHandler {
        override fun type(): String = handlerType
        override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
            return TaskResult(TaskStatus.SUCCESS)
        }
        override fun cancel() {}
    }
}

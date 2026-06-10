package com.taskforge.handler

/**
 * Registry for TaskHandler implementations.
 * The engine will resolve handlers by task type through this registry.
 */
class TaskHandlerRegistry {
    private val handlers: MutableMap<String, TaskHandler> = mutableMapOf()

    fun register(handler: TaskHandler) {
        handlers[handler.type()] = handler
    }

    fun getHandler(type: String): TaskHandler? = handlers[type]
}

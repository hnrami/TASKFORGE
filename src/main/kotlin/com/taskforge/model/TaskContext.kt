package com.taskforge.model

data class TaskContext(
    val variables: MutableMap<String, Any?> = mutableMapOf()
) {
    fun <T> get(key: String): T? = variables[key] as? T
    fun set(key: String, value: Any?) { variables[key] = value }
    fun snapshot(): Map<String, Any?> = variables.toMap()
}

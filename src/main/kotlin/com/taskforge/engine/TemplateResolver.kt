package com.taskforge.engine

import com.taskforge.exception.TaskForgeException

class TemplateResolver {
    private val reference = Regex("""\{\{([^}]+)}}""")

    fun resolveConfig(config: Map<String, Any?>, values: Map<String, Any?>): Map<String, Any?> =
        config.mapValues { (_, value) -> resolveValue(value, values) }

    fun resolveText(text: String, values: Map<String, Any?>): String =
        reference.replace(text) { match ->
            val key = match.groupValues[1].trim()
            values[key]?.toString() ?: throw TaskForgeException("Unknown output reference: $key")
        }

    private fun resolveValue(value: Any?, values: Map<String, Any?>): Any? = when (value) {
        is String -> resolveText(value, values)
        is Map<*, *> -> value.entries.associate { it.key.toString() to resolveValue(it.value, values) }
        is List<*> -> value.map { resolveValue(it, values) }
        else -> value
    }
}

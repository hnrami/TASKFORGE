package com.taskforge.engine

class ConditionEvaluator(private val resolver: TemplateResolver) {
    fun evaluate(condition: String?, values: Map<String, Any?>): Boolean {
        if (condition.isNullOrBlank()) return true
        val resolved = resolver.resolveText(condition, values).trim()
        val parts = resolved.split("==", limit = 2)
        if (parts.size == 2) return parts[0].trim() == parts[1].trim()
        return resolved.equals("true", ignoreCase = true)
    }
}

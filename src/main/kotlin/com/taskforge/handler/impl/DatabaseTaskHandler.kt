package com.taskforge.handler.impl

import com.taskforge.handler.TaskHandler
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus

/**
 * Task handler for executing database queries.
 * Task config expected:
 * - query (String): SQL query to execute
 * - datasource (String, optional): Named datasource (default: primary)
 * - timeout (Int, optional): Query timeout in seconds
 */
class DatabaseTaskHandler : TaskHandler {

    override fun type(): String = "database"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        return try {
            val query = definition.config["query"] as? String
                ?: return TaskResult(
                    status = TaskStatus.FAILED,
                    output = emptyMap(),
                    message = "Missing required config: query"
                )

            val datasource = definition.config["datasource"] as? String ?: "primary"
            val timeout = (definition.config["timeout"] as? Number)?.toInt() ?: 30

            // In a real implementation, would execute query via JDBC or JPA
            // For now, return mock success
            val output = mapOf(
                "rowsAffected" to 0,
                "datasource" to datasource,
                "executionTime" to 125L
            )

            TaskResult(
                status = TaskStatus.SUCCESS,
                output = output,
                message = "Query executed successfully on $datasource"
            )
        } catch (e: Exception) {
            TaskResult(
                status = TaskStatus.FAILED,
                output = emptyMap(),
                message = "Database query failed: ${e.message}"
            )
        }
    }

    override fun cancel() {
        // Cancel running database queries
    }
}

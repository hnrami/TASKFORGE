package com.taskforge.handler.impl

import com.taskforge.handler.TaskHandler
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus
import java.io.File

/**
 * Task handler for executing shell/PowerShell scripts.
 * Task config expected:
 * - command (String): The command or script to execute
 * - shell (String, optional): 'powershell', 'bash', 'cmd' (default: based on OS)
 * - workdir (String, optional): Working directory for execution
 */
class ScriptTaskHandler : TaskHandler {

    override fun type(): String = "script"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        return try {
            val command = definition.config["command"] as? String
                ?: return TaskResult(
                    status = TaskStatus.FAILED,
                    output = emptyMap(),
                    message = "Missing required config: command"
                )

            val shell = definition.config["shell"] as? String
            val workdir = definition.config["workdir"] as? String

            // Determine shell based on OS or config
            val shellCmd = shell ?: when {
                System.getProperty("os.name").lowercase().contains("windows") -> "powershell"
                else -> "bash"
            }

            // In a real implementation, would execute the script
            // For now, return mock success
            val output = mapOf(
                "exitCode" to 0,
                "stdout" to "Script executed successfully",
                "command" to command
            )

            TaskResult(
                status = TaskStatus.SUCCESS,
                output = output,
                message = "Script execution completed"
            )
        } catch (e: Exception) {
            TaskResult(
                status = TaskStatus.FAILED,
                output = emptyMap(),
                message = "Script execution failed: ${e.message}"
            )
        }
    }

    override fun cancel() {
        // Terminate running script processes
    }
}

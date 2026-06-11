package com.taskforge.handler.impl

import com.taskforge.handler.TaskHandler
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ScriptTaskHandler : TaskHandler {
    private val processes = ConcurrentHashMap.newKeySet<Process>()

    override fun type(): String = "script"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        val command = definition.config["command"] as? String
            ?: return TaskResult(TaskStatus.FAILED, message = "Missing required config: command")
        val shell = definition.config["shell"] as? String ?: defaultShell()
        val processBuilder = ProcessBuilder(shellCommand(shell, command))
        (definition.config["workdir"] as? String)?.let { processBuilder.directory(File(it)) }
        val process = try {
            processBuilder.start()
        } catch (exception: Exception) {
            return TaskResult(TaskStatus.FAILED, message = "Script start failed: ${exception.message}")
        }
        processes.add(process)
        return try {
            val stdout = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readText() }
            val stderr = CompletableFuture.supplyAsync { process.errorStream.bufferedReader().readText() }
            val exitCode = process.waitFor()
            val output = mapOf(
                "stdout" to stdout.join(),
                "stderr" to stderr.join(),
                "exitCode" to exitCode
            )
            if (exitCode == 0) TaskResult(TaskStatus.SUCCESS, output)
            else TaskResult(TaskStatus.FAILED, output, "Script exited with code $exitCode")
        } catch (exception: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            TaskResult(TaskStatus.FAILED, message = "Script execution interrupted")
        } finally {
            processes.remove(process)
        }
    }

    override fun cancel() {
        processes.forEach {
            it.destroy()
            if (it.isAlive) it.destroyForcibly()
        }
    }

    private fun defaultShell(): String =
        if (System.getProperty("os.name").lowercase().contains("windows")) "powershell" else "bash"

    private fun shellCommand(shell: String, command: String): List<String> = when (shell.lowercase()) {
        "powershell", "pwsh" -> listOf(shell, "-NoProfile", "-Command", command)
        "cmd" -> listOf("cmd", "/c", command)
        else -> listOf(shell, "-c", command)
    }
}

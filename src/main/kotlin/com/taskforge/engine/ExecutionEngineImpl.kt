package com.taskforge.engine

import com.taskforge.exception.TaskForgeException
import com.taskforge.handler.TaskHandler
import com.taskforge.handler.TaskHandlerRegistry
import com.taskforge.model.ExecutionStatus
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskExecution
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus
import com.taskforge.model.WorkflowDefinition
import com.taskforge.model.WorkflowExecution
import com.taskforge.repository.ExecutionRepository
import com.taskforge.repository.WorkflowRepository
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class ExecutionEngineImpl(
    private val dagValidator: DagValidator,
    private val dagScheduler: DagSchedulerImpl,
    private val stateManager: StateManager,
    private val handlerRegistry: TaskHandlerRegistry,
    private val workflowRepository: WorkflowRepository,
    private val executionRepository: ExecutionRepository,
    private val templateResolver: TemplateResolver = TemplateResolver(),
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator(templateResolver)
) : ExecutionEngine {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val workflowExecutor = Executors.newCachedThreadPool(daemonThreadFactory("workflow"))
    private val taskExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
        daemonThreadFactory("task")
    )
    private val cancelledExecutions = ConcurrentHashMap.newKeySet<String>()
    private val runningHandlers = ConcurrentHashMap<String, ConcurrentHashMap<String, TaskHandler>>()

    override fun startWorkflow(definition: WorkflowDefinition): WorkflowExecution {
        dagValidator.validate(definition)
        val execution = WorkflowExecution(
            id = UUID.randomUUID().toString(),
            definitionId = definition.id,
            status = ExecutionStatus.RUNNING,
            startedAt = Instant.now(),
            tasks = definition.tasks.map {
                TaskExecution(
                    id = UUID.randomUUID().toString(),
                    taskDefinitionId = it.id,
                    status = TaskStatus.PENDING
                )
            }
        )
        executionRepository.save(execution)
        workflowExecutor.submit { runWorkflow(definition, execution.id) }
        return execution
    }

    override fun cancel(executionId: String): WorkflowExecution {
        val execution = findExecution(executionId)
        cancelledExecutions.add(executionId)
        runningHandlers[executionId]?.values?.forEach { it.cancel() }
        val cancelled = execution.copy(
            status = ExecutionStatus.CANCELLED,
            finishedAt = Instant.now(),
            tasks = execution.tasks.map {
                if (it.status in setOf(TaskStatus.PENDING, TaskStatus.RUNNING, TaskStatus.WAITING_APPROVAL)) {
                    it.copy(status = TaskStatus.CANCELLED, finishedAt = Instant.now())
                } else it
            }
        )
        executionRepository.save(cancelled)
        return cancelled
    }

    override fun resolveApproval(executionId: String, taskId: String, approved: Boolean): WorkflowExecution {
        val execution = findExecution(executionId)
        val approval = execution.tasks.firstOrNull { it.taskDefinitionId == taskId }
            ?: throw TaskForgeException("Task not found in execution: $taskId")
        if (approval.status != TaskStatus.WAITING_APPROVAL) {
            throw TaskForgeException("Task is not waiting for approval: $taskId")
        }
        val result = if (approved) {
            TaskResult(TaskStatus.SUCCESS, message = "Approval accepted")
        } else {
            TaskResult(TaskStatus.FAILED, message = "Approval rejected")
        }
        val updated = execution.copy(tasks = execution.tasks.map {
            if (it.taskDefinitionId == taskId) {
                it.copy(status = result.status, result = result, finishedAt = Instant.now())
            } else it
        })
        executionRepository.save(updated)
        return updated
    }

    private fun runWorkflow(definition: WorkflowDefinition, executionId: String) {
        try {
            while (true) {
                if (executionId in cancelledExecutions) return
                var execution = findExecution(executionId)
                execution = expireApprovals(definition, execution)
                execution = skipBlockedTasks(definition, execution)
                executionRepository.save(execution)

                if (execution.tasks.all { it.status in TERMINAL_TASK_STATUSES }) {
                    finishWorkflow(execution)
                    return
                }

                val outputs = collectOutputs(execution)
                val ready = definition.tasks.filter { definitionTask ->
                    execution.tasks.first { it.taskDefinitionId == definitionTask.id }.status == TaskStatus.PENDING &&
                        definitionTask.dependsOn.all { dependency ->
                            execution.tasks.first { it.taskDefinitionId == dependency }.status == TaskStatus.SUCCESS
                        }
                }

                if (ready.isEmpty()) {
                    Thread.sleep(50)
                    continue
                }

                val skippedByCondition = ready.filterNot { conditionEvaluator.evaluate(it.condition, outputs) }
                if (skippedByCondition.isNotEmpty()) {
                    val skippedIds = skippedByCondition.map { it.id }.toSet()
                    execution = execution.copy(tasks = execution.tasks.map {
                        if (it.taskDefinitionId in skippedIds) {
                            it.copy(status = TaskStatus.SKIPPED, finishedAt = Instant.now())
                        } else it
                    })
                    executionRepository.save(execution)
                }

                val runnable = ready - skippedByCondition.toSet()
                if (runnable.isEmpty()) continue
                val runnableIds = runnable.map { it.id }.toSet()
                execution = findExecution(executionId).copy(tasks = findExecution(executionId).tasks.map {
                    if (it.taskDefinitionId in runnableIds) {
                        it.copy(status = TaskStatus.RUNNING, startedAt = Instant.now())
                    } else it
                })
                executionRepository.save(execution)

                val futures = runnable.associateWith { task ->
                    taskExecutor.submit<TaskExecution> {
                        val current = findExecution(executionId).tasks.first { it.taskDefinitionId == task.id }
                        executeTask(executionId, task, current, outputs)
                    }
                }
                val completed = futures.map { (task, future) ->
                    awaitTask(executionId, task, future)
                }.associateBy { it.taskDefinitionId }

                if (executionId in cancelledExecutions) return
                execution = findExecution(executionId).copy(tasks = findExecution(executionId).tasks.map {
                    completed[it.taskDefinitionId] ?: it
                })
                executionRepository.save(execution)
            }
        } catch (exception: Exception) {
            logger.error("Workflow execution {} failed", executionId, exception)
            val execution = executionRepository.findById(executionId) ?: return
            executionRepository.save(execution.copy(status = ExecutionStatus.FAILED, finishedAt = Instant.now()))
        } finally {
            runningHandlers.remove(executionId)
            cancelledExecutions.remove(executionId)
        }
    }

    private fun executeTask(
        executionId: String,
        task: TaskDefinition,
        execution: TaskExecution,
        outputs: Map<String, Any?>
    ): TaskExecution {
        val handler = handlerRegistry.getHandler(task.type)
            ?: throw TaskForgeException("No handler registered for task type: ${task.type}")
        runningHandlers.computeIfAbsent(executionId) { ConcurrentHashMap() }[task.id] = handler
        val maxAttempts = (task.retryPolicy?.maxAttempts ?: 1).coerceAtLeast(1)
        var result = TaskResult(TaskStatus.FAILED, message = "Task did not execute")
        var attempt = 0
        while (attempt < maxAttempts && executionId !in cancelledExecutions) {
            attempt++
            val resolvedTask = task.copy(config = templateResolver.resolveConfig(task.config, outputs))
            result = try {
                handler.execute(resolvedTask, TaskContext(outputs.toMutableMap()))
            } catch (exception: Exception) {
                TaskResult(TaskStatus.FAILED, message = exception.message, retryable = true)
            }
            if (result.status != TaskStatus.FAILED || !result.retryable || attempt >= maxAttempts) break
            Thread.sleep(task.retryPolicy?.backoffMillis ?: 0)
        }
        runningHandlers[executionId]?.remove(task.id)
        return execution.copy(
            status = result.status,
            attempt = attempt,
            finishedAt = if (result.status == TaskStatus.WAITING_APPROVAL) null else Instant.now(),
            result = result
        )
    }

    private fun awaitTask(
        executionId: String,
        task: TaskDefinition,
        future: Future<TaskExecution>
    ): TaskExecution {
        return try {
            if (task.timeoutSeconds == null) future.get()
            else future.get(task.timeoutSeconds, TimeUnit.SECONDS)
        } catch (exception: TimeoutException) {
            future.cancel(true)
            runningHandlers[executionId]?.remove(task.id)?.cancel()
            findExecution(executionId).tasks.first { it.taskDefinitionId == task.id }.copy(
                status = TaskStatus.FAILED,
                finishedAt = Instant.now(),
                result = TaskResult(TaskStatus.FAILED, message = "Task timed out after ${task.timeoutSeconds} seconds")
            )
        } catch (exception: Exception) {
            val cause = if (exception is ExecutionException) exception.cause ?: exception else exception
            findExecution(executionId).tasks.first { it.taskDefinitionId == task.id }.copy(
                status = TaskStatus.FAILED,
                finishedAt = Instant.now(),
                result = TaskResult(TaskStatus.FAILED, message = cause.message ?: "Task execution failed")
            )
        }
    }

    private fun collectOutputs(execution: WorkflowExecution): Map<String, Any?> = buildMap {
        execution.tasks.filter { it.status == TaskStatus.SUCCESS }.forEach { task ->
            task.result?.output?.forEach { (key, value) -> put("${task.taskDefinitionId}.$key", value) }
        }
    }

    private fun skipBlockedTasks(definition: WorkflowDefinition, execution: WorkflowExecution): WorkflowExecution {
        val statuses = execution.tasks.associate { it.taskDefinitionId to it.status }
        return execution.copy(tasks = execution.tasks.map { taskExecution ->
            val task = definition.tasks.first { it.id == taskExecution.taskDefinitionId }
            if (taskExecution.status == TaskStatus.PENDING &&
                task.dependsOn.any { statuses[it] in setOf(TaskStatus.FAILED, TaskStatus.SKIPPED, TaskStatus.CANCELLED) }
            ) {
                taskExecution.copy(status = TaskStatus.SKIPPED, finishedAt = Instant.now())
            } else taskExecution
        })
    }

    private fun expireApprovals(definition: WorkflowDefinition, execution: WorkflowExecution): WorkflowExecution {
        val now = Instant.now()
        return execution.copy(tasks = execution.tasks.map { taskExecution ->
            val timeout = definition.tasks.first { it.id == taskExecution.taskDefinitionId }.timeoutSeconds
            if (taskExecution.status == TaskStatus.WAITING_APPROVAL && timeout != null &&
                taskExecution.startedAt?.plusSeconds(timeout)?.isBefore(now) == true
            ) {
                taskExecution.copy(
                    status = TaskStatus.FAILED,
                    finishedAt = now,
                    result = TaskResult(TaskStatus.FAILED, message = "Approval timed out after $timeout seconds")
                )
            } else taskExecution
        })
    }

    private fun finishWorkflow(execution: WorkflowExecution) {
        val status = when {
            execution.status == ExecutionStatus.CANCELLED -> ExecutionStatus.CANCELLED
            execution.tasks.any { it.status == TaskStatus.FAILED } -> ExecutionStatus.FAILED
            else -> ExecutionStatus.SUCCESS
        }
        executionRepository.save(execution.copy(status = status, finishedAt = Instant.now()))
    }

    private fun findExecution(id: String): WorkflowExecution =
        executionRepository.findById(id) ?: throw TaskForgeException("Execution not found: $id")

    private companion object {
        val TERMINAL_TASK_STATUSES = setOf(
            TaskStatus.SUCCESS,
            TaskStatus.FAILED,
            TaskStatus.SKIPPED,
            TaskStatus.CANCELLED
        )

        fun daemonThreadFactory(prefix: String): ThreadFactory {
            val sequence = AtomicInteger()
            return ThreadFactory { runnable ->
                Thread(runnable, "taskforge-$prefix-${sequence.incrementAndGet()}").apply { isDaemon = true }
            }
        }
    }
}

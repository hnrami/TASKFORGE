package com.taskforge.config

import com.taskforge.engine.*
import com.taskforge.handler.TaskHandlerRegistry
import com.taskforge.handler.impl.HttpTaskHandler
import com.taskforge.handler.impl.ScriptTaskHandler
import com.taskforge.handler.impl.NotificationTaskHandler
import com.taskforge.handler.impl.DatabaseTaskHandler
import com.taskforge.handler.impl.ApprovalTaskHandler
import com.taskforge.repository.WorkflowRepository
import com.taskforge.repository.ExecutionRepository
import com.taskforge.service.WorkflowService
import com.taskforge.service.ExecutionService
import com.taskforge.service.impl.WorkflowServiceImpl
import com.taskforge.service.impl.ExecutionServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration class for TaskForge beans.
 * Defines all component dependencies and wiring.
 */
@Configuration
class TaskForgeConfiguration {

    @Bean
    fun taskHandlerRegistry(): TaskHandlerRegistry {
        val registry = TaskHandlerRegistry()
        
        // Register all available task handlers
        registry.register(HttpTaskHandler())
        registry.register(ScriptTaskHandler())
        registry.register(NotificationTaskHandler())
        registry.register(DatabaseTaskHandler())
        registry.register(ApprovalTaskHandler())
        
        return registry
    }

    @Bean
    fun dagValidator(handlerRegistry: TaskHandlerRegistry): DagValidator {
        return DagValidatorImpl(handlerRegistry)
    }

    @Bean
    fun dagScheduler(): DagScheduler {
        return DagSchedulerImpl()
    }

    @Bean
    fun stateManager(): StateManager {
        return StateManagerImpl()
    }

    @Bean
    fun executionEngine(
        dagValidator: DagValidator,
        handlerRegistry: TaskHandlerRegistry,
        workflowRepository: WorkflowRepository,
        executionRepository: ExecutionRepository
    ): ExecutionEngine {
        val dagScheduler = DagSchedulerImpl()
        val stateManager = StateManagerImpl()
        
        return ExecutionEngineImpl(
            dagValidator = dagValidator,
            dagScheduler = dagScheduler,
            stateManager = stateManager,
            handlerRegistry = handlerRegistry,
            workflowRepository = workflowRepository,
            executionRepository = executionRepository
        )
    }

    @Bean
    fun workflowService(
        workflowRepository: WorkflowRepository,
        dagValidator: DagValidator
    ): WorkflowService {
        return WorkflowServiceImpl(workflowRepository, dagValidator)
    }

    @Bean
    fun executionService(
        executionEngine: ExecutionEngine,
        workflowRepository: WorkflowRepository,
        executionRepository: ExecutionRepository
    ): ExecutionService {
        return ExecutionServiceImpl(
            executionEngine = executionEngine,
            workflowRepository = workflowRepository,
            executionRepository = executionRepository
        )
    }
}

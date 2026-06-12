package com.taskforge.repository.jpa

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskforge.model.WorkflowExecution
import com.taskforge.repository.ExecutionRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository

@Primary
@Repository
class JpaExecutionRepository(
    private val repository: ExecutionJpaRepository,
    private val objectMapper: ObjectMapper
) : ExecutionRepository {
    override fun save(execution: WorkflowExecution) {
        repository.save(ExecutionEntity(execution.id, objectMapper.writeValueAsString(execution)))
    }

    override fun findById(id: String): WorkflowExecution? =
        repository.findById(id).orElse(null)?.let { objectMapper.readValue(it.payload, WorkflowExecution::class.java) }
}

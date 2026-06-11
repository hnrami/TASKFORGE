package com.taskforge.repository.jpa

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskforge.model.WorkflowDefinition
import com.taskforge.repository.WorkflowRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository

@Primary
@Repository
class JpaWorkflowRepository(
    private val repository: WorkflowJpaRepository,
    private val objectMapper: ObjectMapper
) : WorkflowRepository {
    override fun save(definition: WorkflowDefinition) {
        repository.save(WorkflowEntity(definition.id, objectMapper.writeValueAsString(definition)))
    }

    override fun findById(id: String): WorkflowDefinition? =
        repository.findById(id).orElse(null)?.let { objectMapper.readValue(it.payload, WorkflowDefinition::class.java) }

    override fun findAll(): List<WorkflowDefinition> =
        repository.findAll().map { objectMapper.readValue(it.payload, WorkflowDefinition::class.java) }

    override fun delete(id: String) = repository.deleteById(id)
}

package com.taskforge.repository.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface WorkflowJpaRepository : JpaRepository<WorkflowEntity, String>

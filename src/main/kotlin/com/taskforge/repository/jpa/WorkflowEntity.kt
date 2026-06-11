package com.taskforge.repository.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Lob

@Entity
class WorkflowEntity(
    @Id
    var id: String = "",
    @Lob
    var payload: String = ""
)

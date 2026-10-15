package com.example.tasks.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository
    extends JpaRepository<TaskEntity, UUID>, JpaSpecificationExecutor<TaskEntity> {

  Optional<TaskEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}

package com.example.tasks.web;

import com.example.tasks.persistence.TaskEntity;
import org.springframework.data.domain.Page;

final class TaskMapper {

  private TaskMapper() {}

  static TaskResponse toResponse(TaskEntity task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getDueDate(),
        task.getCreatedAt(),
        task.getUpdatedAt(),
        task.getVersion());
  }

  static PageResponse<TaskResponse> toResponse(Page<TaskEntity> page) {
    return new PageResponse<>(
        page.getContent().stream().map(TaskMapper::toResponse).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}

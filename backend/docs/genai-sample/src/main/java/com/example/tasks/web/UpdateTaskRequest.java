package com.example.tasks.web;

import com.example.tasks.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTaskRequest(
    @NotBlank @Size(max = 120) String title,
    @Size(max = 2000) String description,
    @NotNull TaskStatus status,
    LocalDate dueDate,
    @NotNull Long version) {

  /** Trimmed before validation: the title's limits apply to its trimmed form. */
  public UpdateTaskRequest {
    title = title == null ? null : title.strip();
  }
}

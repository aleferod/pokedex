package com.example.tasks.web;

import com.example.tasks.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
    @NotBlank @Size(max = 120) String title,
    @Size(max = 2000) String description,
    TaskStatus status,
    LocalDate dueDate) {

  /** Trimmed before validation: the title's limits apply to its trimmed form. */
  public CreateTaskRequest {
    title = title == null ? null : title.strip();
  }
}

package com.example.tasks.web;

import com.example.tasks.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    LocalDate dueDate,
    Instant createdAt,
    Instant updatedAt,
    long version) {}

package com.example.tasks.task;

import java.time.LocalDate;

/** Input for replacing a task; {@code version} is the version the client read. */
public record TaskChanges(
    String title, String description, TaskStatus status, LocalDate dueDate, long version) {}

package com.example.tasks.task;

import java.time.LocalDate;

/** Input for creating a task. */
public record NewTask(String title, String description, TaskStatus status, LocalDate dueDate) {}

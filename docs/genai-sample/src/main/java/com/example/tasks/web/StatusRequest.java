package com.example.tasks.web;

import com.example.tasks.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull TaskStatus status) {}

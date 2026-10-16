package com.example.tasks.task;

public class InvalidDueDateException extends RuntimeException {

  public InvalidDueDateException() {
    super("dueDate must not be in the past");
  }
}

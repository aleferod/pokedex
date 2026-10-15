package com.example.tasks.task;

import java.util.UUID;

public class StaleTaskException extends RuntimeException {

  public StaleTaskException(UUID id) {
    super("Task " + id + " was modified by another request");
  }
}

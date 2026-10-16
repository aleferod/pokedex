package com.example.tasks.task;

public class UnknownUserException extends RuntimeException {

  public UnknownUserException(String username) {
    super("Unknown user " + username);
  }
}

package com.ballastlane.archfixtures.fieldinjection;

import org.springframework.beans.factory.annotation.Autowired;

public class FieldInjected {

  @Autowired private Collaborator collaborator;

  public String work() {
    return collaborator.toString();
  }

  public static class Collaborator {}
}

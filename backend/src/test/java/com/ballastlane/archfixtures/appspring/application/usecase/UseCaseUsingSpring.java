package com.ballastlane.archfixtures.appspring.application.usecase;

import org.springframework.util.Assert;

public class UseCaseUsingSpring {

  public void execute(String name) {
    Assert.hasText(name, "name is required");
  }
}

package com.ballastlane.archfixtures.domainapp.domain;

import com.ballastlane.archfixtures.domainapp.application.port.in.SomeUseCase;

public class DomainUsingApplication {

  public void call(SomeUseCase useCase) {
    useCase.execute();
  }
}

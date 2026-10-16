package com.ballastlane.archfixtures.adapterinfra.adapter.in.web;

import com.ballastlane.archfixtures.adapterinfra.infrastructure.AppSettings;

public class ControllerUsingInfrastructure {

  public String greeting(AppSettings settings) {
    return settings.greeting();
  }
}

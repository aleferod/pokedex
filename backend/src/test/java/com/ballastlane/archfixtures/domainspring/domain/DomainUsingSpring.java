package com.ballastlane.archfixtures.domainspring.domain;

import org.springframework.util.StringUtils;

public class DomainUsingSpring {

  public boolean isNamed(String name) {
    return StringUtils.hasText(name);
  }
}

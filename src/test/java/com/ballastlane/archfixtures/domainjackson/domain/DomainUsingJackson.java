package com.ballastlane.archfixtures.domainjackson.domain;

import tools.jackson.databind.ObjectMapper;

public class DomainUsingJackson {

  public String toJson(ObjectMapper mapper) {
    return mapper.writeValueAsString(this);
  }
}

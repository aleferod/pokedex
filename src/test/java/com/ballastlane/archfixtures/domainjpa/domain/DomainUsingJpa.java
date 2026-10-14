package com.ballastlane.archfixtures.domainjpa.domain;

import jakarta.persistence.EntityManager;

public class DomainUsingJpa {

  public void save(EntityManager entityManager) {
    entityManager.persist(this);
  }
}

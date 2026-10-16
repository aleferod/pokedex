package com.ballastlane.archfixtures.adaptercoupling.adapter.in.web;

import com.ballastlane.archfixtures.adaptercoupling.adapter.out.persistence.JpaRepositoryAdapter;

public class ControllerUsingPersistence {

  private final JpaRepositoryAdapter repository;

  public ControllerUsingPersistence(JpaRepositoryAdapter repository) {
    this.repository = repository;
  }

  public int count() {
    return repository.count();
  }
}

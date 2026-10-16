package com.ballastlane.archfixtures.appadapter.application.usecase;

import com.ballastlane.archfixtures.appadapter.adapter.out.persistence.PersistenceAdapter;

public class UseCaseUsingAdapter {

  private final PersistenceAdapter adapter;

  public UseCaseUsingAdapter(PersistenceAdapter adapter) {
    this.adapter = adapter;
  }

  public int count() {
    return adapter.count();
  }
}
